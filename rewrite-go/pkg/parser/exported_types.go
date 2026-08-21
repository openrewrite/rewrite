/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package parser

import (
	"go/ast"
	"go/build"
	"go/importer"
	"go/parser"
	"go/token"
	"go/types"
	"os"
	"path"
	"path/filepath"
	"sort"
	"strings"

	"golang.org/x/mod/modfile"
	"golang.org/x/mod/module"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// ExportedTypes enumerates the public API of one or more Go modules (the
// on-disk dirs in ownArtifacts) into the JavaType model, resolving imported
// symbols against the reference closure. The Go analog of the C#
// AssemblyTypeEnumerator / JS package-exported-types: each own package's exported
// named types map to complete JavaType.Class bodies (members + methods), and a
// package's exported funcs/vars/consts attach to a synthetic class whose FQN is
// the package import path. Types referenced but not defined by the own modules
// come back as FQN-only ShallowClass refs for the caller to resolve. Go is
// structurally typed, so mapNamed leaves Supertype/Interfaces null — kept as-is.
func ExportedTypes(ownArtifacts, references []string) []java.FullyQualified {
	// Augment the caller's references with each own module's OWN declared deps (its go.mod
	// requires, resolved in the module cache) so a package's public API can name cross-module
	// references from the module and its own manifest alone. Anything not defined by the own
	// modules stays a shallow FQN class-ref for the caller to resolve.
	refs := append([]string(nil), references...)
	for _, dir := range ownArtifacts {
		refs = append(refs, ownModuleDepDirs(dir)...)
	}
	refImporter := newReferenceImporter(refs)
	mapper := newTypeMapper()
	// Foreign named types reached from an own type's API mint as FQN-only ShallowClass refs.
	ownPaths := make([]string, 0, len(ownArtifacts))
	for _, dir := range ownArtifacts {
		if mp := moduleImportPath(dir); mp != "" {
			ownPaths = append(ownPaths, mp)
		} else if sp := stdlibImportPath(dir); sp != "" {
			ownPaths = append(ownPaths, sp)
		}
	}
	mapper.ownPkg = func(pkgPath string) bool {
		for _, p := range ownPaths {
			if pkgPath == p || strings.HasPrefix(pkgPath, p+"/") {
				return true
			}
		}
		return false
	}

	var out []java.FullyQualified
	seen := map[string]bool{}
	collect := func(cls *java.JavaTypeClass) {
		if cls == nil || cls.FullyQualifiedName == "" || seen[cls.FullyQualifiedName] {
			return
		}
		seen[cls.FullyQualifiedName] = true
		out = append(out, cls)
	}

	var stdlibImporter types.Importer
	for _, dir := range ownArtifacts {
		modPath := moduleImportPath(dir)
		if modPath == "" {
			// No go.mod: a stdlib package under $GOROOT/src. Load it from source, since
			// Go 1.20+ ships no precompiled archives to import.
			importPath := stdlibImportPath(dir)
			if importPath == "" {
				continue
			}
			if stdlibImporter == nil {
				stdlibImporter = importer.ForCompiler(token.NewFileSet(), "source", nil)
			}
			if pkg, err := stdlibImporter.Import(importPath); err == nil && pkg != nil {
				enumeratePackage(pkg, importPath, mapper, collect)
			}
			continue
		}
		pi := NewProjectImporter(modPath, refImporter)
		pi.SetProjectRoot(dir)
		pkgImports := registerModuleSources(pi, dir, modPath)

		for _, importPath := range pkgImports {
			pkg, err := pi.Import(importPath)
			if err != nil || pkg == nil {
				continue
			}
			enumeratePackage(pkg, importPath, mapper, collect)
		}
	}
	return out
}

// ownModuleDepDirs reads dir/go.mod and returns the module-cache directories of its declared
// requires (direct and indirect), so a module's public API can name cross-module references from
// its own manifest alone. Its own replace/exclude directives are deliberately not applied (Go
// ignores a non-main module's replaces). Missing entries are skipped — the reference simply
// degrades to a shallow FQN class-ref.
func ownModuleDepDirs(dir string) []string {
	data, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		return nil
	}
	f, err := modfile.Parse("go.mod", data, nil)
	if err != nil {
		return nil
	}
	cache := GoModCache()
	if cache == "" {
		return nil
	}
	var dirs []string
	for _, req := range f.Require {
		esc, err := module.EscapePath(req.Mod.Path)
		if err != nil {
			continue
		}
		d := filepath.Join(cache, esc+"@"+req.Mod.Version)
		if fi, err := os.Stat(d); err == nil && fi.IsDir() {
			dirs = append(dirs, d)
		}
	}
	return dirs
}

// GoModCache locates the module cache: $GOMODCACHE, else the first $GOPATH entry's
// pkg/mod, else ~/go/pkg/mod.
func GoModCache() string {
	if v := os.Getenv("GOMODCACHE"); v != "" {
		return v
	}
	gopath := os.Getenv("GOPATH")
	if gopath == "" {
		home, err := os.UserHomeDir()
		if err != nil {
			return ""
		}
		gopath = filepath.Join(home, "go")
	} else if i := strings.IndexByte(gopath, filepath.ListSeparator); i >= 0 {
		gopath = gopath[:i]
	}
	return filepath.Join(gopath, "pkg", "mod")
}

// enumeratePackage walks a type-checked package's exported top-level names,
// mapping named types to full classes and collecting package-level
// funcs/vars/consts onto a synthetic class named for the import path.
func enumeratePackage(pkg *types.Package, importPath string, mapper *typeMapper, collect func(*java.JavaTypeClass)) {
	scope := pkg.Scope()
	var pkgClass *java.JavaTypeClass
	pkgClassOf := func() *java.JavaTypeClass {
		if pkgClass == nil {
			pkgClass = &java.JavaTypeClass{FullyQualifiedName: importPath, Kind: "Class", FlagsBitMap: 1}
		}
		return pkgClass
	}

	for _, name := range scope.Names() {
		if !token.IsExported(name) {
			continue
		}
		switch o := scope.Lookup(name).(type) {
		case *types.TypeName:
			// Go aliases are transparent, so only own-package types get full bodies
			// here; an external alias target (type A = io.Reader) stays a shallow
			// reference via its own FQN and must not be owned by this module.
			if named, ok := types.Unalias(o.Type()).(*types.Named); ok &&
				named.Obj().Pkg() != nil && named.Obj().Pkg().Path() == importPath {
				collect(mapper.mapNamed(named))
			}
		case *types.Func:
			sig, ok := o.Type().(*types.Signature)
			if !ok || sig.Recv() != nil {
				continue
			}
			pc := pkgClassOf()
			pc.Methods = append(pc.Methods, mapper.mapSignature(sig, o.Name(), pc))
		case *types.Var:
			pc := pkgClassOf()
			pc.Members = append(pc.Members, &java.JavaTypeVariable{Name: o.Name(), Owner: pc, Type: mapper.mapType(o.Type())})
		case *types.Const:
			pc := pkgClassOf()
			pc.Members = append(pc.Members, &java.JavaTypeVariable{Name: o.Name(), Owner: pc, Type: mapper.mapType(o.Type())})
		}
	}
	collect(pkgClass)
}

// registerModuleSources reads every non-test .go file under a module dir (build
// context permitting), registers it with the importer, and returns the distinct
// package import paths to enumerate.
func registerModuleSources(pi *ProjectImporter, dir, modPath string) []string {
	buildCtx := goBuildContext()
	seen := map[string]bool{}
	var pkgs []string
	_ = filepath.WalkDir(dir, func(p string, d os.DirEntry, err error) error {
		if err != nil {
			return nil
		}
		if d.IsDir() {
			// A nested go.mod marks a separate module; don't mis-attribute its
			// files to this module's import path.
			if p != dir && (skipDir(d.Name()) || containsGoMod(p)) {
				return filepath.SkipDir
			}
			return nil
		}
		name := d.Name()
		if !strings.HasSuffix(name, ".go") || strings.HasSuffix(name, "_test.go") {
			return nil
		}
		content, rerr := os.ReadFile(p)
		if rerr != nil || !MatchBuildContext(buildCtx, name, string(content)) {
			return nil
		}
		rel, rerr := filepath.Rel(dir, p)
		if rerr != nil {
			return nil
		}
		rel = filepath.ToSlash(rel)
		pi.AddSource(rel, string(content))
		importPath := modPath
		if sub := path.Dir(rel); sub != "" && sub != "." {
			importPath = modPath + "/" + sub
		}
		if !seen[importPath] {
			seen[importPath] = true
			pkgs = append(pkgs, importPath)
		}
		return nil
	})
	return pkgs
}

func skipDir(name string) bool {
	// internal/ packages aren't importable across modules — not part of the
	// public API this table represents.
	return name == "vendor" || name == "testdata" || name == "internal" ||
		strings.HasPrefix(name, ".") || strings.HasPrefix(name, "_")
}

func containsGoMod(dir string) bool {
	_, err := os.Stat(filepath.Join(dir, "go.mod"))
	return err == nil
}

// goBuildContext captures the linux/amd64 gc view, pinned so that table
// content is deterministic across build machines.
func goBuildContext() build.Context {
	ctx := build.Default
	ctx.GOOS = "linux"
	ctx.GOARCH = "amd64"
	ctx.CgoEnabled = false
	ctx.Compiler = "gc"
	return ctx
}

// filterBuildContext drops files excluded by buildCtx's GOOS/GOARCH filename
// suffixes and //go:build constraints, so OS-variant reference files
// (foo_linux.go + foo_windows.go) don't collide with redeclaration errors.
func filterBuildContext(files []projectFile, buildCtx build.Context) []projectFile {
	out := make([]projectFile, 0, len(files))
	for _, f := range files {
		if MatchBuildContext(buildCtx, filepath.Base(f.path), f.content) {
			out = append(out, f)
		}
	}
	return out
}

// stdlibImportPath returns dir's import path relative to its nearest "src" ancestor
// (.../go/src/net/http → net/http), or "" if dir isn't under a src/ tree.
func stdlibImportPath(dir string) string {
	dir = filepath.Clean(dir)
	for p := dir; ; {
		parent := filepath.Dir(p)
		if parent == p {
			return ""
		}
		if filepath.Base(parent) == "src" {
			rel, err := filepath.Rel(parent, dir)
			if err != nil {
				return ""
			}
			return filepath.ToSlash(rel)
		}
		p = parent
	}
}

// moduleImportPath reads the module path from <dir>/go.mod, or "" if absent/unparseable.
func moduleImportPath(dir string) string {
	data, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		return ""
	}
	return modfile.ModulePath(data)
}

// referenceImporter resolves imports against the reference closure — on-disk
// module dirs keyed by their module path — falling back to the stdlib importer.
// It is the ProjectImporter's fallback so an own package's external imports
// resolve to real symbols (giving external param/return types their true FQN)
// instead of collapsing to Unknown.
type referenceImporter struct {
	modDirs  map[string]string
	modPaths []string // longest-first for longest-prefix match
	cache    map[string]*types.Package
	fset     *token.FileSet
	def      types.Importer
	buildCtx build.Context
}

func newReferenceImporter(refs []string) *referenceImporter {
	ri := &referenceImporter{
		modDirs:  map[string]string{},
		cache:    map[string]*types.Package{},
		fset:     token.NewFileSet(),
		def:      importer.Default(),
		buildCtx: goBuildContext(),
	}
	for _, dir := range refs {
		if mp := moduleImportPath(dir); mp != "" {
			ri.modDirs[mp] = dir
			ri.modPaths = append(ri.modPaths, mp)
		}
	}
	sort.Slice(ri.modPaths, func(i, j int) bool { return len(ri.modPaths[i]) > len(ri.modPaths[j]) })
	return ri
}

func (ri *referenceImporter) Import(importPath string) (*types.Package, error) {
	if p, ok := ri.cache[importPath]; ok {
		return p, nil
	}
	for _, mp := range ri.modPaths {
		if importPath != mp && !strings.HasPrefix(importPath, mp+"/") {
			continue
		}
		sub := strings.TrimPrefix(importPath, mp)
		pkgDir := filepath.Join(ri.modDirs[mp], filepath.FromSlash(sub))
		files, err := readGoFilesIn(pkgDir)
		if err != nil {
			break
		}
		files = filterBuildContext(files, ri.buildCtx)
		if len(files) == 0 {
			break
		}
		return ri.check(importPath, files), nil
	}
	return ri.def.Import(importPath)
}

func (ri *referenceImporter) check(importPath string, files []projectFile) *types.Package {
	asts := make([]*ast.File, 0, len(files))
	for _, f := range files {
		a, err := parser.ParseFile(ri.fset, f.path, f.content, 0)
		if err != nil {
			continue
		}
		asts = append(asts, a)
	}
	conf := types.Config{Importer: ri, Error: func(error) {}}
	pkg := types.NewPackage(importPath, "")
	ri.cache[importPath] = pkg // forward-declare to break import cycles
	_ = types.NewChecker(&conf, ri.fset, pkg, &types.Info{}).Files(asts)
	return pkg
}

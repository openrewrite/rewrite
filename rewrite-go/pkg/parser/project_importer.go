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
	"fmt"
	"go/ast"
	"go/build"
	"go/importer"
	"go/parser"
	"go/token"
	"go/types"
	"log"
	"os"
	"path"
	"path/filepath"
	"sort"
	"strings"

	"golang.org/x/mod/module"
)

// ProjectImporter resolves Go imports for a parsed file against five
// layers, in order:
//
//  1. Sibling sources within the same project (registered via AddSource).
//     Yields real *types.Package objects from parsing + type-checking.
//  2. Vendored sources at `<projectRoot>/vendor/<importPath>/*.go`. When
//     a `replace` directive applies, the lookup target is resolved
//     accordingly: local replace (`./` / `../` prefix) walks the local
//     path; module-path replace walks `vendor/<NewPath>/`.
//     Yields real *types.Package objects with full method/field types.
//  3. The module cache, for modules of the resolved build list (registered
//     via AddModule). Yields real *types.Package objects from the module's
//     extracted sources under $GOMODCACHE, which is where a `go build`
//     without a vendor tree reads them from too.
//  4. Modules declared in go.mod's `require` directives (registered via
//     AddRequire). Yields a STUB *types.Package — right path and name,
//     empty scope — so references like `import "github.com/x/y"` make
//     the identifier `y` non-nil even when the module's source isn't
//     present locally.
//  5. The fallback (importer.Default by default), which resolves stdlib
//     packages from GOROOT.
//
// Mirrors the role of MavenProject/JavaSourceSet classpath resolution on
// the Java side: when a recipe parses a Go file inside a project, imports
// of `<modulePath>/<sub>` resolve against that sub-package's parsed
// sources, and dependencies resolve against the on-disk sources a build
// would compile. A module reachable through neither falls back to a
// typed-but-empty stub.
//
// Every on-disk layer is walked lazily, on the Import() call that needs
// it.
type ProjectImporter struct {
	modulePath  string
	projectRoot string // absolute or relative path to the dir containing go.mod
	// sources keyed by full import path (e.g. "example.com/foo/sub").
	sources map[string][]projectFile
	// modules are the resolved build list, keyed by module path, so an import
	// under one resolves against that module's extracted sources.
	modules map[string]cacheModule
	// requires lists module paths declared in go.mod `require` directives.
	// An import path matches when it equals one of these OR is under one
	// of them as a sub-path (require x/y also covers x/y/z imports).
	requires []string
	// replaces maps each old import path to its replacement target.
	// Built by AddReplace from the parsed go.mod replace directives.
	replaces map[string]replaceTarget
	cache    map[string]*types.Package
	// stubs are the paths resolved to a placeholder rather than to real
	// symbols, so a caller can report what a package could not see.
	stubs    map[string]bool
	fset     *token.FileSet
	fallback types.Importer
}

// StubsReachableFrom names the placeholder packages — right path and name,
// empty scope — that importPath either is or reaches through what it imports.
// A type a package cannot see costs its importers the same, however many
// packages back it went missing. Sorted, so one tree comes of one source.
func (p *ProjectImporter) StubsReachableFrom(importPath string) []string {
	// Nothing stubbed is nothing reachable, and it is what a build whose
	// modules all resolved looks like — every package would otherwise walk
	// its whole import graph to find that out.
	if len(p.stubs) == 0 {
		return nil
	}
	var found []string
	seen := make(map[string]bool)
	var walk func(string)
	walk = func(path string) {
		if seen[path] {
			return
		}
		seen[path] = true
		if p.stubs[path] {
			// A stub carries no imports of its own to walk.
			found = append(found, path)
			return
		}
		pkg, ok := p.cache[path]
		if !ok {
			return
		}
		for _, imp := range pkg.Imports() {
			walk(imp.Path())
		}
	}
	walk(importPath)
	sort.Strings(found)
	return found
}

// replaceTarget mirrors a go.mod `replace ... => newPath [newVersion]`
// entry. NewVersion is empty when NewPath is a local filesystem path.
type replaceTarget struct {
	NewPath    string
	NewVersion string
}

type projectFile struct {
	path    string // module-relative, e.g. "main.go" or "sub/sub.go"
	content string
}

// Pass importer.Default() (or nil for the same default) as the stdlib
// fallback. Project root is unset and must be configured via
// SetProjectRoot for vendor walking to find anything.
func NewProjectImporter(modulePath string, fallback types.Importer) *ProjectImporter {
	if fallback == nil {
		fallback = importer.Default()
	}
	return &ProjectImporter{
		modulePath: modulePath,
		sources:    make(map[string][]projectFile),
		modules:    make(map[string]cacheModule),
		replaces:   make(map[string]replaceTarget),
		cache:      make(map[string]*types.Package),
		stubs:      make(map[string]bool),
		fset:       token.NewFileSet(),
		fallback:   fallback,
	}
}

// SetProjectRoot configures the directory the vendor walker scans
// relative to. Without this set, vendor lookups always miss and the
// resolver falls through to the require-stub tier. Pass the directory
// containing the project's go.mod.
func (p *ProjectImporter) SetProjectRoot(root string) {
	p.projectRoot = root
}

// AddReplace registers a go.mod `replace oldPath [oldVersion] => newPath [newVersion]`
// entry. At Import() time, requests for oldPath (or sub-paths under it)
// are redirected to newPath. Local-path replacements (`./` / `../`)
// resolve against the project root; module-path replacements resolve
// against `vendor/<NewPath>/`.
func (p *ProjectImporter) AddReplace(oldPath, newPath, newVersion string) {
	if oldPath == "" || newPath == "" {
		return
	}
	p.replaces[oldPath] = replaceTarget{NewPath: newPath, NewVersion: newVersion}
}

// Imports of this path (or any sub-path under it) that no earlier layer
// satisfies resolve to a stub *types.Package — non-nil, with the right path
// and name, but with an empty scope. Register the module with AddModule too
// for real method/field types.
func (p *ProjectImporter) AddRequire(modulePath string) {
	if modulePath != "" {
		p.requires = append(p.requires, modulePath)
	}
}

// cacheModule is one module of the resolved build list: the import prefix it
// provides, and the coordinate its sources live under.
type cacheModule struct {
	Path      string
	CachePath string
	Version   string
}

// AddModule registers a module of the resolved build list under modulePath,
// the import prefix it provides. cachePath and version are the coordinate its
// sources live under; leave cachePath empty to have any directive registered
// with AddReplace decide it, which is what a caller reading go.mod wants.
func (p *ProjectImporter) AddModule(modulePath, cachePath, version string) {
	if modulePath == "" {
		return
	}
	p.modules[modulePath] = cacheModule{Path: modulePath, CachePath: cachePath, Version: version}
}

// AddSource registers a .go file with the importer. relPath is the file's
// path relative to the module root, e.g. "main.go" or "sub/sub.go". Only
// .go files are indexed; anything else is ignored.
func (p *ProjectImporter) AddSource(relPath, content string) {
	if !strings.HasSuffix(relPath, ".go") {
		return
	}
	dir := path.Dir(relPath)
	importPath := p.modulePath
	if dir != "" && dir != "." {
		importPath = p.modulePath + "/" + dir
	}
	p.sources[importPath] = append(p.sources[importPath], projectFile{path: relPath, content: content})
}

func (p *ProjectImporter) Import(importPath string) (*types.Package, error) {
	if cached, ok := p.cache[importPath]; ok {
		return cached, nil
	}
	if files, ok := p.sources[importPath]; ok {
		pkg, err := p.parsePackage(importPath, files)
		if err != nil {
			return nil, err
		}
		p.cache[importPath] = pkg
		return pkg, nil
	}
	// Vendor walker: real on-disk source resolution. When a replace
	// directive applies, the walker follows it; otherwise it looks for
	// `<projectRoot>/vendor/<importPath>/`. Parse failures are logged
	// and the resolver falls through to the require-stub tier so a
	// broken vendored dep doesn't tank the parent parse (per the C4
	// directive in the eng review).
	if p.projectRoot != "" {
		if pkg := p.walkVendor(importPath); pkg != nil {
			p.cache[importPath] = pkg
			return pkg, nil
		}
	}
	if pkg := p.walkModuleCache(importPath); pkg != nil {
		p.cache[importPath] = pkg
		return pkg, nil
	}
	// Stub-resolve any path declared in go.mod requires (or under one).
	// Real symbols stay missing, but the package object itself is non-nil
	// so identifiers referencing it have a Package type. The package must
	// be marked complete; otherwise the type-checker treats the import
	// as not yet loaded and reports `undefined: <pkgname>` at use sites.
	for _, req := range p.requires {
		if importPath == req || strings.HasPrefix(importPath, req+"/") {
			stub := types.NewPackage(importPath, path.Base(importPath))
			stub.MarkComplete()
			p.cache[importPath] = stub
			p.stubs[importPath] = true
			return stub, nil
		}
	}
	return p.fallback.Import(importPath)
}

// walkVendor attempts to resolve importPath against on-disk sources at
// `<projectRoot>/vendor/<importPath>/` (or the equivalent path under a
// matching `replace` directive). Returns nil if no sources are found or
// if parsing failed (in which case the resolver should fall through to
// the stub tier).
func (p *ProjectImporter) walkVendor(importPath string) *types.Package {
	dir := p.resolveVendorDir(importPath)
	if dir == "" {
		return nil
	}
	files, err := readGoFilesIn(dir)
	if err != nil || len(files) == 0 {
		return nil
	}
	pkg, err := p.parsePackage(importPath, files)
	if err != nil {
		log.Printf("vendor walker: skip %s (parse error: %v) — falling back to stub", importPath, err)
		return nil
	}
	return pkg
}

// walkModuleCache resolves importPath against the extracted sources of the
// build-list module that provides it. Returns nil when no registered module
// covers the path, the module is absent from the cache, or parsing failed.
func (p *ProjectImporter) walkModuleCache(importPath string) *types.Package {
	dir := p.resolveModuleCacheDir(importPath)
	if dir == "" {
		return nil
	}
	files, err := p.readBuildableGoFilesIn(dir)
	if err != nil || len(files) == 0 {
		return nil
	}
	pkg, err := p.parsePackage(importPath, files)
	if err != nil {
		log.Printf("module cache: skip %s (parse error: %v) — falling back to stub", importPath, err)
		return nil
	}
	return pkg
}

// resolveModuleCacheDir maps an import path to the directory holding the
// providing module's sources. A module path is a prefix of every import path it
// provides, and the longest such prefix wins: `example.com/a` and
// `example.com/a/b` can both be modules, and only the longer one provides
// `example.com/a/b/c`.
func (p *ProjectImporter) resolveModuleCacheDir(importPath string) string {
	var best cacheModule
	for path, m := range p.modules {
		if importPath != path && !strings.HasPrefix(importPath, path+"/") {
			continue
		}
		if len(path) > len(best.Path) {
			best = m
		}
	}
	if best.Path == "" {
		return ""
	}
	cachePath, version := p.replacedCoordinate(best)
	var root string
	if isLocalReplace(cachePath) {
		root = cachePath
		if !filepath.IsAbs(root) {
			root = filepath.Join(p.projectRoot, filepath.FromSlash(cachePath))
		}
	} else {
		cache := GoModCache()
		if cache == "" || version == "" {
			return ""
		}
		esc, err := module.EscapePath(cachePath)
		if err != nil {
			return ""
		}
		vesc, err := module.EscapeVersion(version)
		if err != nil {
			return ""
		}
		root = filepath.Join(cache, esc+"@"+vesc)
	}
	return filepath.Join(root, filepath.FromSlash(strings.TrimPrefix(importPath, best.Path)))
}

// replacedCoordinate is where a module's sources live, applying the replace
// directive for a caller that left the coordinate to AddModule.
func (p *ProjectImporter) replacedCoordinate(m cacheModule) (cachePath, version string) {
	if m.CachePath != "" {
		return m.CachePath, m.Version
	}
	if repl, ok := p.replaces[m.Path]; ok {
		return repl.NewPath, repl.NewVersion
	}
	return m.Path, m.Version
}

// readBuildableGoFilesIn is readGoFilesIn restricted to the files this build
// would compile. A module in the cache carries every platform's sources at
// once, and `plat_linux.go` beside `plat_windows.go` redeclares everything.
func (p *ProjectImporter) readBuildableGoFilesIn(dir string) ([]projectFile, error) {
	files, err := readGoFilesIn(dir)
	if err != nil {
		return nil, err
	}
	out := files[:0]
	for _, f := range files {
		if MatchBuildContext(build.Default, filepath.Base(f.path), f.content) {
			out = append(out, f)
		}
	}
	return out, nil
}

// resolveVendorDir maps an import path to the on-disk directory the
// vendor walker should scan. Returns "" when no resolution applies.
//
// Replace-directive resolution honored:
//   - `replace foo => ./local/foo`     → walk `<projectRoot>/local/foo`
//   - `replace foo => bar` (module)    → walk `<projectRoot>/vendor/bar`
//
// Without a matching replace, the default vendor dir is
// `<projectRoot>/vendor/<importPath>`. Sub-package imports
// (`require foo/bar` + `import "foo/bar/sub"`) walk the same way:
// `<projectRoot>/vendor/foo/bar/sub`.
func (p *ProjectImporter) resolveVendorDir(importPath string) string {
	target := importPath
	for old, repl := range p.replaces {
		if importPath == old || strings.HasPrefix(importPath, old+"/") {
			suffix := strings.TrimPrefix(importPath, old)
			if isLocalReplace(repl.NewPath) {
				return filepath.Join(p.projectRoot, filepath.FromSlash(repl.NewPath+suffix))
			}
			target = repl.NewPath + suffix
			break
		}
	}
	return filepath.Join(p.projectRoot, "vendor", filepath.FromSlash(target))
}

func isLocalReplace(newPath string) bool {
	return strings.HasPrefix(newPath, "./") ||
		strings.HasPrefix(newPath, "../") ||
		newPath == "." ||
		filepath.IsAbs(newPath)
}

// readGoFilesIn reads every non-test .go file in dir as a projectFile.
// Returns nil + error if dir doesn't exist or can't be listed; returns
// nil + nil (interpreted as "not vendored") when the dir is empty of .go
// files.
func readGoFilesIn(dir string) ([]projectFile, error) {
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil, err
	}
	var out []projectFile
	for _, e := range entries {
		if e.IsDir() {
			continue
		}
		name := e.Name()
		if !strings.HasSuffix(name, ".go") || strings.HasSuffix(name, "_test.go") {
			continue
		}
		full := filepath.Join(dir, name)
		data, err := os.ReadFile(full)
		if err != nil {
			return nil, err
		}
		out = append(out, projectFile{path: full, content: string(data)})
	}
	return out, nil
}

func (p *ProjectImporter) parsePackage(importPath string, files []projectFile) (*types.Package, error) {
	asts := make([]*ast.File, 0, len(files))
	for _, f := range files {
		a, err := parser.ParseFile(p.fset, f.path, f.content, parser.ParseComments)
		if err != nil {
			return nil, fmt.Errorf("parse %s: %w", f.path, err)
		}
		asts = append(asts, a)
	}
	conf := types.Config{
		// Recurse: a sibling package may itself import another sibling.
		Importer: p,
		// Type errors in sibling code shouldn't break the caller's parse —
		// we still want partial type info.
		Error: func(error) {},
	}
	info := &types.Info{
		Defs: make(map[*ast.Ident]types.Object),
		Uses: make(map[*ast.Ident]types.Object),
	}
	// The package's Path() — here the full import path, e.g. "github.com/x/y"
	// — MUST be the full import path, not just the package name, because
	// mapType sets a method's DeclaringType.FullyQualifiedName from pkg.Path()
	// at type-mapping time. With "y" alone, vendored fmt-like methods would
	// resolve to FQN "y" instead of "github.com/x/y".
	//
	// Forward-declare the package in the cache BEFORE type-checking it. This
	// is the standard go/types cycle-breaking pattern: type-checking calls
	// back into Import for every dependency, so an import cycle
	// (a -> b -> ... -> a, common in deep graphs like entgo.io/ent) would
	// otherwise re-enter parsePackage for a path not yet in the cache and
	// recurse until the goroutine stack overflows. Registering the
	// not-yet-complete package up front lets the cycle resolve to it on
	// re-entry instead; go/types tolerates importing an incomplete package
	// for cycle resolution and completes it once Files returns.
	//
	// conf.Check(importPath, ...) is itself exactly NewPackage + NewChecker +
	// Files; we spell it out only so the package object exists (and is cached)
	// before the recursive type-check begins. The package name is left empty;
	// the checker fills it in from the parsed files, same as conf.Check.
	pkg := types.NewPackage(importPath, "")
	p.cache[importPath] = pkg
	_ = types.NewChecker(&conf, p.fset, pkg, info).Files(asts)
	return pkg, nil
}

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

package golang

import (
	"path"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang/internal"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// RenamePackage renames a Go package across a project.
//
// On every visited compilation unit:
//   - If the file's `package` declaration matches the last segment of
//     OldPackagePath AND the file is in the renamed package, the
//     declaration is rewritten to the last segment of NewPackagePath.
//   - Every import whose path equals OldPackagePath OR is under
//     OldPackagePath as a sub-path is rewritten to the corresponding
//     NewPackagePath path. Sub-paths preserve their suffix:
//     `import "old/foo/sub"` becomes `import "new/foo/sub"` when
//     renaming `old/foo` to `new/foo`.
//
// The recipe is idempotent: re-running it on a file that's already at
// the new package name is a no-op.
type RenamePackage struct {
	recipe.Base
	OldPackagePath string
	NewPackagePath string
}

func (r *RenamePackage) Name() string        { return "org.openrewrite.golang.RenamePackage" }
func (r *RenamePackage) DisplayName() string { return "Rename package" }
func (r *RenamePackage) Description() string {
	return "Rename a Go package across a project — rewrites the `package` declaration in files that own the package, and rewrites import paths in every file that references it."
}

func (r *RenamePackage) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("oldPackagePath", "Old package path", "The fully qualified package path to rename.").
			WithExample("github.com/old/foo").WithValue(r.OldPackagePath),
		recipe.Option("newPackagePath", "New package path", "The fully qualified package path to use instead.").
			WithExample("github.com/new/foo").WithValue(r.NewPackagePath),
	}
}

func (r *RenamePackage) Editor() recipe.TreeVisitor {
	return visitor.Init(&renamePackageVisitor{cfg: r})
}

type renamePackageVisitor struct {
	visitor.GoVisitor
	cfg *RenamePackage
}

func (v *renamePackageVisitor) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*tree.CompilationUnit)
	if v.cfg.OldPackagePath == "" || v.cfg.NewPackagePath == "" {
		return cu
	}

	// Rewrite the package declaration when this file owns the renamed
	// package. The name is only the last path segment, so we compare
	// segment-by-segment.
	oldName := path.Base(v.cfg.OldPackagePath)
	newName := path.Base(v.cfg.NewPackagePath)
	if cu.PackageDecl != nil && cu.PackageDecl.Element != nil &&
		cu.PackageDecl.Element.Name == oldName &&
		v.fileBelongsTo(cu, v.cfg.OldPackagePath) {
		pkg := *cu.PackageDecl
		ident := *pkg.Element
		ident.Name = newName
		pkg.Element = &ident
		cu.PackageDecl = &pkg
	}

	// Rewrite import paths. Match `OldPackagePath` exactly OR as a
	// strict prefix (`OldPackagePath/...`); anything else is left
	// alone. Aliased / blank / dot imports are rewritten the same way.
	if cu.Imports != nil {
		imps := *cu.Imports
		out := make([]tree.RightPadded[*tree.Import], len(imps.Elements))
		for i, rp := range imps.Elements {
			imp := rp.Element
			oldPath := internal.ImportPath(imp)
			newPath := rewritePath(oldPath, v.cfg.OldPackagePath, v.cfg.NewPackagePath)
			if newPath == oldPath {
				out[i] = rp
				continue
			}
			imp = withImportPath(imp, newPath)
			rp.Element = imp
			out[i] = rp
		}
		imps.Elements = out
		cu.Imports = &imps
	}

	return cu
}

// fileBelongsTo reports whether cu lives inside the package at
// candidatePath. The check uses the parsed `GoResolutionResult` marker's
// ModulePath plus the file's source-relative subdirectory: if the file's
// dir under the module equals candidatePath, the file belongs to it.
//
// When module context is unavailable (no `GoResolutionResult` marker),
// the file's location can't be confidently determined, so we report
// false — matching the conservative default: don't rewrite a `package`
// declaration based solely on a name match (the same name might be
// reused across unrelated directories). Tests that exercise this path
// must wrap their sources in `GoProject(...)` with a `GoMod(...)`
// sibling so the marker is propagated.
func (v *renamePackageVisitor) fileBelongsTo(cu *tree.CompilationUnit, candidatePath string) bool {
	modulePath := internal.FindModulePath(cu)
	if modulePath == "" {
		return false
	}
	if !strings.HasPrefix(candidatePath, modulePath) {
		return false
	}
	relCandidate := strings.TrimPrefix(strings.TrimPrefix(candidatePath, modulePath), "/")
	relFile := path.Dir(cu.SourcePath)
	if relFile == "." {
		relFile = ""
	}
	return relFile == relCandidate
}

func rewritePath(p, oldPath, newPath string) string {
	if p == oldPath {
		return newPath
	}
	if strings.HasPrefix(p, oldPath+"/") {
		return newPath + strings.TrimPrefix(p, oldPath)
	}
	return p
}

// withImportPath returns a copy of imp with its Qualid Literal source +
// value updated to the new import path. Preserves Prefix and Markers
// so the printer keeps the surrounding whitespace.
func withImportPath(imp *tree.Import, newPath string) *tree.Import {
	if imp == nil {
		return imp
	}
	c := *imp
	if lit, ok := imp.Qualid.(*tree.Literal); ok {
		ln := *lit
		ln.Value = `"` + newPath + `"`
		ln.Source = `"` + newPath + `"`
		c.Qualid = &ln
	}
	return &c
}

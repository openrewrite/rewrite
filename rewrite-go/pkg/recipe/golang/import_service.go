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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

// ImportService exposes the Go-side import-manipulation primitives as
// composable visitors. Mirrors org.openrewrite.java.service.ImportService.
//
// Recipes get one via recipe.Service:
//
//	svc := recipe.Service[*golang.ImportService](cu)
//	v.DoAfterVisit(svc.AddImportVisitor("fmt", nil, false))
//
// Each method returns a recipe.TreeVisitor configured for the
// requested operation. Visitors can be applied directly via
// `v.Visit(cu, ctx)` OR queued for the after-visit drain via
// `GoVisitor.DoAfterVisit(v)`. The latter is the canonical way to
// compose import side-effects with a main edit:
//
//	func (r *MyRecipe) Editor() recipe.TreeVisitor {
//	    return visitor.Init(&myVisitor{})
//	}
//
//	type myVisitor struct{ visitor.GoVisitor }
//
//	func (v *myVisitor) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
//	    if shouldRewrite(mi) {
//	        // ...rewrite mi...
//	        cu := v.Cursor().FirstEnclosing(reflect.TypeOf((*tree.CompilationUnit)(nil)))
//	        svc := recipe.Service[*golang.ImportService](cu)
//	        v.DoAfterVisit(svc.AddImportVisitor("fmt", nil, false))
//	    }
//	    return mi
//	}
type ImportService struct{}

// AddImportVisitor returns a visitor that adds `import [alias] "packagePath"`
// to the visited compilation unit. No-op if the import is already
// present in a compatible form.
//
//   - alias == nil          → regular import (`import "fmt"`).
//   - alias == "_"          → blank import for init() side-effects.
//   - alias == "."          → dot import.
//   - any other identifier  → aliased import.
//
// When onlyIfReferenced is true, the import is added only when some
// identifier in the file already references the package via type
// attribution. That's the safe default for IDE-style refactors that
// shouldn't introduce dead imports.
func (s *ImportService) AddImportVisitor(packagePath string, alias *string, onlyIfReferenced bool) recipe.TreeVisitor {
	return (&AddImport{
		PackagePath:      packagePath,
		Alias:            alias,
		OnlyIfReferenced: onlyIfReferenced,
	}).Editor()
}

// RemoveImportVisitor returns a visitor that deletes any `import` whose
// path matches packagePath. Aliased / blank / dot forms are all
// removed. Empty import containers are nil-ed out so the printer
// doesn't emit an empty `import ()` block.
func (s *ImportService) RemoveImportVisitor(packagePath string) recipe.TreeVisitor {
	return (&RemoveImport{PackagePath: packagePath}).Editor()
}

// RemoveUnusedImportsVisitor returns a visitor that drops imports
// whose alias is never referenced in the file. Mirrors `goimports -w`.
// Blank (`_`) and dot (`.`) imports are preserved by semantic rule.
func (s *ImportService) RemoveUnusedImportsVisitor() recipe.TreeVisitor {
	return (&RemoveUnusedImports{}).Editor()
}

// OrderImportsVisitor returns a visitor that sorts imports into
// stdlib / third-party / local groups. Local detection uses the
// sibling go.mod's module path (via the GoResolutionResult marker).
func (s *ImportService) OrderImportsVisitor() recipe.TreeVisitor {
	return (&OrderImports{}).Editor()
}

func init() {
	// Register the service factory so callers can do
	// `recipe.Service[*golang.ImportService](cu)`. Stateless — one
	// instance per call is fine.
	recipe.RegisterService[*ImportService](func() any { return &ImportService{} })
}

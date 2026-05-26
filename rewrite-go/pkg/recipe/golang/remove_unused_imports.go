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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang/internal"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// RemoveUnusedImports drops imports whose alias is never referenced by an
// identifier in the file. Mirrors `goimports -w` behavior.
//
// Per the C3 directive: a single identifier walker driven by Type.Owner
// computes the set of referenced packages. Aliased and dot imports
// resolve uniformly through the identifier's type FQN. Blank imports
// (`import _ "path"`) are preserved by semantic rule — they exist for
// their init() side-effects, not for any user-visible reference.
//
// Recipe authors who want to drop blank imports should use RemoveImport
// targeting the specific path.
type RemoveUnusedImports struct {
	recipe.Base
}

func (r *RemoveUnusedImports) Name() string        { return "org.openrewrite.golang.RemoveUnusedImports" }
func (r *RemoveUnusedImports) DisplayName() string { return "Remove unused imports" }
func (r *RemoveUnusedImports) Description() string {
	return "Remove imports for packages that are not referenced by any identifier in the file. Blank (`_`) imports are preserved."
}

func (r *RemoveUnusedImports) Editor() recipe.TreeVisitor {
	return visitor.Init(&removeUnusedImportsVisitor{})
}

type removeUnusedImportsVisitor struct {
	visitor.GoVisitor
}

func (v *removeUnusedImportsVisitor) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*tree.CompilationUnit)
	if cu.Imports == nil || len(cu.Imports.Elements) == 0 {
		return cu
	}
	refs := internal.ReferencedPackages(cu)
	for _, rp := range cu.Imports.Elements {
		imp := rp.Element
		if imp == nil {
			continue
		}
		// Blank imports stay — they exist for init() side-effects.
		if internal.AliasName(imp) == "_" {
			continue
		}
		// Dot imports also stay — referenced packages can't be tracked
		// by FQN because dot-imported names enter the local scope.
		if internal.AliasName(imp) == "." {
			continue
		}
		if !refs[internal.ImportPath(imp)] {
			cu = internal.RemoveFromBlock(cu, imp)
		}
	}
	return cu
}

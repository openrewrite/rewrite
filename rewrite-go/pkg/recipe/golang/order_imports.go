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

// OrderImports normalizes the order of `import` lines into the
// `goimports -w` convention: stdlib first, third-party second, local
// last; within each group entries are sorted alphabetically by import
// path; a blank line separates non-empty groups.
//
// Local imports are detected via the sibling go.mod's
// `GoResolutionResult.ModulePath` marker (attached by `parseProject` and
// the Java parseWithProject path). Without a module marker, every
// non-stdlib import is treated as third-party.
//
// Idempotent: running OrderImports twice yields the same result as once.
type OrderImports struct {
	recipe.Base
}

func (r *OrderImports) Name() string        { return "org.openrewrite.golang.OrderImports" }
func (r *OrderImports) DisplayName() string { return "Order imports" }
func (r *OrderImports) Description() string {
	return "Sort `import` lines into stdlib / third-party / local groups. Within each group, entries are alphabetized; non-empty groups are separated by a blank line. Mirrors `goimports -w`. Local detection uses the sibling go.mod's module path."
}

func (r *OrderImports) Editor() recipe.TreeVisitor {
	return visitor.Init(&orderImportsVisitor{})
}

type orderImportsVisitor struct {
	visitor.GoVisitor
}

func (v *orderImportsVisitor) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*tree.CompilationUnit)
	if cu.Imports == nil || len(cu.Imports.Elements) <= 1 {
		return cu
	}
	modulePath := internal.FindModulePath(cu)
	sorted := internal.SortByGroup(cu.Imports.Elements, modulePath)
	if sameOrder(cu.Imports.Elements, sorted) {
		return cu
	}
	c := *cu
	imps := *c.Imports
	imps.Elements = sorted
	c.Imports = &imps
	return &c
}

func sameOrder(before, after []tree.RightPadded[*tree.Import]) bool {
	if len(before) != len(after) {
		return false
	}
	for i := range before {
		if before[i].Element == nil || after[i].Element == nil {
			return before[i].Element == after[i].Element
		}
		if before[i].Element.ID != after[i].Element.ID {
			return false
		}
	}
	return true
}

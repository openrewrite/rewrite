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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// OrderImports sorts the imports in each `import` declaration: stdlib
// first, third-party second, local last; within each group entries are
// sorted alphabetically by import path; a blank line separates non-empty
// groups. Imports are never moved between declarations.
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
	return "Sort the imports in each `import` declaration into stdlib / third-party / local groups. Within each group, entries are alphabetized; non-empty groups are separated by a blank line. Imports are never moved between declarations. Local detection uses the sibling go.mod's module path."
}

func (r *OrderImports) Editor() recipe.TreeVisitor {
	return visitor.Init(&orderImportsVisitor{})
}

type orderImportsVisitor struct {
	visitor.GoVisitor
}

func (v *orderImportsVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*golang.CompilationUnit)
	if cu.Imports == nil || len(cu.Imports.Elements) <= 1 {
		return cu
	}
	modulePath := internal.FindModulePath(cu)
	sorted := sortEachDeclaration(cu.Imports.Elements, modulePath)
	if sameOrder(cu.Imports.Elements, sorted) {
		return cu
	}
	c := *cu
	imps := *c.Imports
	imps.Elements = sorted
	c.Imports = &imps
	return &c
}

// sortEachDeclaration sorts the imports of each import declaration on their
// own, as gofmt does. The parser flattens every declaration into one list,
// marking each later declaration's first import with an ImportBlock, so
// sorting the list whole would move imports, and the boundaries they carry,
// between declarations.
func sortEachDeclaration(elements []java.RightPadded[*java.Import], modulePath string) []java.RightPadded[*java.Import] {
	out := make([]java.RightPadded[*java.Import], 0, len(elements))
	start := 0
	for i := 1; i <= len(elements); i++ {
		if i < len(elements) && (elements[i].Element == nil ||
			java.FindMarker[golang.ImportBlock](elements[i].Element.Markers) == nil) {
			continue
		}
		out = append(out, sortDeclaration(elements[start:i], modulePath)...)
		start = i
	}
	return out
}

// sortDeclaration sorts one declaration, keeping the ImportBlock marker that
// opens it on whichever import now comes first.
func sortDeclaration(decl []java.RightPadded[*java.Import], modulePath string) []java.RightPadded[*java.Import] {
	if len(decl) <= 1 {
		return decl
	}
	block := java.FindMarker[golang.ImportBlock](decl[0].Element.Markers)
	sorted := internal.SortByGroup(decl, modulePath)
	if block == nil || sorted[0].Element.ID == decl[0].Element.ID {
		return sorted
	}
	for i := range sorted {
		if java.FindMarker[golang.ImportBlock](sorted[i].Element.Markers) != nil {
			imp := *sorted[i].Element
			imp.Markers = withoutImportBlock(imp.Markers)
			sorted[i].Element = &imp
		}
	}
	head := *sorted[0].Element
	head.Markers = java.AddMarker(head.Markers, *block)
	sorted[0].Element = &head
	return sorted
}

func withoutImportBlock(markers java.Markers) java.Markers {
	entries := make([]java.Marker, 0, len(markers.Entries()))
	for _, m := range markers.Entries() {
		if _, ok := m.(golang.ImportBlock); !ok {
			entries = append(entries, m)
		}
	}
	return java.MakeMarkers(markers.GetID(), entries)
}

func sameOrder(before, after []java.RightPadded[*java.Import]) bool {
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

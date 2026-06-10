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

package rpc

import (
	"testing"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// These tests cover CompilationUnit.Imports on the Print round-trip. Java
// doesn't preserve the inner generic type parameter of JContainer on the wire.
// The pre-fix receiver inferred the element type from the payload and raw-cast
// the result to Container[*Import]; an EMPTY imports container infers to
// Container[Expression], which is NOT assertable to Container[*Import] (Go
// generics are invariant), so the cast panicked, aborting the receive mid-stream
// and corrupting the ref table (the "received reference to unknown object"
// cascade on the next Print).
//
// The fix is receiveContainerTyped[T] / coerceRightPaddedTyped[T]: the field's
// statically-known element type T is supplied at the call site, so the container
// is built as Container[T] directly — no inference, no guessing for empty
// containers, no raw cross-instantiation assertion.

// makeImport returns an *Import shaped like a Go import (Qualid is a string Literal).
func makeImport(path string) *java.Import {
	return &java.Import{
		ID: uuid.New(),
		Qualid: &java.Literal{
			ID:     uuid.New(),
			Source: `"` + path + `"`,
			Value:  path,
		},
	}
}

func TestCoerceRightPaddedTyped_PassThrough(t *testing.T) {
	// already-correct variant should pass through with element identity preserved.
	imp := makeImport("fmt")
	var wire any = java.RightPadded[*java.Import]{Element: imp, Markers: java.Markers{}}

	got := coerceRightPaddedTyped[*java.Import](wire)
	if got.Element != imp {
		t.Fatalf("pass-through dropped element: %+v", got)
	}
}

func TestCoerceRightPaddedTyped_FromJVariant(t *testing.T) {
	// given: a RightPadded[java.J] wrapping an *Import — the "element type is
	// any/J" trigger. *Import implements java.J but not Expression, so a
	// type-erased receive can surface this variant.
	imp := makeImport("os")
	var wire any = java.RightPadded[java.J]{Element: imp, After: java.EmptySpace, Markers: java.Markers{}}

	got := coerceRightPaddedTyped[*java.Import](wire)
	if got.Element != imp {
		t.Errorf("element identity lost: want %p, got %p", imp, got.Element)
	}
}

func TestCoerceRightPaddedTyped_NonMatchingElementFallsBack(t *testing.T) {
	// given: a RightPadded whose element does NOT satisfy T (an *Identifier where
	// we want *Import). Coercion must fall back to an element-less padding rather
	// than panic — a stray element should never abort the whole receive.
	var wire any = java.RightPadded[java.Expression]{Element: makeIdent("x"), After: java.EmptySpace, Markers: java.Markers{}}

	got := coerceRightPaddedTyped[*java.Import](wire)
	if got.Element != nil {
		t.Errorf("want nil Element on fallback, got %+v", got.Element)
	}
}

func TestRawCastPanics_ContainerImportFromExpression(t *testing.T) {
	// Pre-fix behavior: VisitCompilationUnit inferred the container type then did
	// `result.(java.Container[*java.Import])`. For an empty imports container the
	// inferred type was Container[Expression]. Lock that panic in as a regression
	// sentinel — receiveContainerTyped[*Import] avoids it by never inferring.
	var wire any = java.Container[java.Expression]{}
	expectPanic(t, "raw cast Container[Expression]->Container[*Import]", func() {
		_ = wire.(java.Container[*java.Import])
	})
}

// ----- Full send->receive round trip through VisitCompilationUnit (Print path) -----

func TestCompilationUnitRoundTrip_EmptyImports(t *testing.T) {
	// given: a CU with a non-nil but EMPTY imports container — this is what
	// triggered the production panic (empty container receives as Expression).
	cuID := uuid.New()
	before := &golang.CompilationUnit{
		ID:      cuID,
		Imports: &java.Container[*java.Import]{Before: java.EmptySpace, Markers: java.Markers{}},
	}
	seed := &golang.CompilationUnit{ID: cuID}

	// when: round-trip must not panic.
	got := roundTripNode(t, before, seed).(*golang.CompilationUnit)

	// then: Imports stays a *Container[*Import].
	if got.Imports == nil {
		t.Fatal("Imports: got nil, want empty *Container[*Import]")
	}
	if len(got.Imports.Elements) != 0 {
		t.Errorf("Imports.Elements: got %d, want 0", len(got.Imports.Elements))
	}
}

func TestCompilationUnitRoundTrip_WithImports(t *testing.T) {
	// given: a CU with two real imports.
	cuID := uuid.New()
	imp1, imp2 := makeImport("fmt"), makeImport("os")
	before := &golang.CompilationUnit{
		ID: cuID,
		Imports: &java.Container[*java.Import]{
			Elements: []java.RightPadded[*java.Import]{
				{Element: imp1, Markers: java.Markers{}},
				{Element: imp2, Markers: java.Markers{}},
			},
		},
	}
	seed := &golang.CompilationUnit{ID: cuID}

	// when
	got := roundTripNode(t, before, seed).(*golang.CompilationUnit)

	// then: both imports survive, typed as *Import.
	if got.Imports == nil {
		t.Fatal("Imports: got nil, want non-nil")
	}
	if len(got.Imports.Elements) != 2 {
		t.Fatalf("Imports.Elements: got %d, want 2", len(got.Imports.Elements))
	}
	gotImp0 := got.Imports.Elements[0].Element
	if gotImp0 == nil {
		t.Fatal("Imports[0]: got nil *Import")
	}
	if lit, ok := gotImp0.Qualid.(*java.Literal); !ok || lit.Value != "fmt" {
		t.Errorf("Imports[0].Qualid: got %+v, want literal \"fmt\"", gotImp0.Qualid)
	}
}

func TestCompilationUnitRoundTrip_NilImports(t *testing.T) {
	// given: a CU with no imports at all (nil container) — must stay nil.
	cuID := uuid.New()
	before := &golang.CompilationUnit{ID: cuID}
	seed := &golang.CompilationUnit{ID: cuID}

	got := roundTripNode(t, before, seed).(*golang.CompilationUnit)
	if got.Imports != nil {
		t.Errorf("Imports: got %+v, want nil", got.Imports)
	}
}

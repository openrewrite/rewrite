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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// These tests exercise the receiver-side coercion helpers that bridge Java's
// type-erased generic padding (Java doesn't preserve the inner type parameter
// of JRightPadded / JLeftPadded / JContainer on the wire) against Go's
// strictly-parameterized RightPadded[T] / LeftPadded[T] / Container[T].
//
// Each scenario below was observed as a real panic in the rewrite-go RPC
// server when running OpenRewrite recipes against Go sources via moderne-cli.
// The "RawCastPanics_…" twins demonstrate what the un-coerced code did
// pre-fix; the "Coerce…" tests demonstrate the same input flowing through
// the coerce helpers without panicking.

// makeIdent returns an *Identifier (which implements Expression only).
func makeIdent(name string) *tree.Identifier {
	return &tree.Identifier{ID: uuid.New(), Name: name}
}

// makeMethodInvocation returns a *MethodInvocation (implements both Statement
// and Expression — Java sometimes ships it inside RightPadded[Expression]
// where the Go side expects RightPadded[Statement] and vice versa).
func makeMethodInvocation() *tree.MethodInvocation {
	return &tree.MethodInvocation{ID: uuid.New(), Name: makeIdent("doThing")}
}

// expectPanic runs fn and fails the test if it does NOT panic. Used to lock
// in the pre-fix raw-cast behavior so regressions surface immediately.
func expectPanic(t *testing.T, label string, fn func()) {
	t.Helper()
	defer func() {
		if r := recover(); r == nil {
			t.Fatalf("%s: expected panic, got none", label)
		}
	}()
	fn()
	t.Fatalf("%s: unreachable — fn returned without panic", label)
}

// ----- Group 1: RightPadded[Statement] vs RightPadded[Expression] -----

func TestCoerceToStatementRP_AcceptsExpressionVariant(t *testing.T) {
	// given: a RightPadded[Expression] wrapping a *MethodInvocation
	// (exactly what Java emits for a for-loop init expression that
	// happens to also be a valid statement).
	mi := makeMethodInvocation()
	var wire any = tree.RightPadded[tree.Expression]{
		Element: mi,
		After:   tree.EmptySpace,
		Markers: tree.Markers{},
	}

	// when: the receiver coerces it to RightPadded[Statement]
	got := coerceToStatementRP(wire)

	// then: the element survives, now typed as Statement
	if got.Element == nil {
		t.Fatal("Element nil after coerce")
	}
	if got.Element.(*tree.MethodInvocation) != mi {
		t.Errorf("Element identity lost: want %p, got %p", mi, got.Element)
	}
}

func TestRawCastPanics_RightPaddedStatementFromExpression(t *testing.T) {
	// Pre-fix behavior: java_receiver.go did `result.(tree.RightPadded[tree.Statement])`.
	// Lock that panic in as a regression sentinel.
	var wire any = tree.RightPadded[tree.Expression]{
		Element: makeMethodInvocation(),
		Markers: tree.Markers{},
	}
	expectPanic(t, "raw cast RP[Expression]->RP[Statement]", func() {
		_ = wire.(tree.RightPadded[tree.Statement])
	})
}

// ----- Group 2: LeftPadded[*Identifier] from LeftPadded[Expression] -----

func TestCoerceLeftPaddedIdent_AcceptsExpressionVariant(t *testing.T) {
	// given: a LeftPadded[Expression] wrapping an *Identifier — the shape
	// Java emits for FieldAccess.name and Import.alias.
	id := makeIdent("Foo")
	var wire any = tree.LeftPadded[tree.Expression]{
		Before:  tree.EmptySpace,
		Element: id,
		Markers: tree.Markers{},
	}

	// when
	got := coerceLeftPaddedIdent(wire)

	// then
	if got.Element != id {
		t.Errorf("Element identity lost: want %p, got %p", id, got.Element)
	}
}

func TestRawCastPanics_LeftPaddedIdentFromExpression(t *testing.T) {
	var wire any = tree.LeftPadded[tree.Expression]{
		Element: makeIdent("Foo"),
		Markers: tree.Markers{},
	}
	expectPanic(t, "raw cast LP[Expression]->LP[*Identifier]", func() {
		_ = wire.(tree.LeftPadded[*tree.Identifier])
	})
}

// ----- Group 3a: ParseAssignOp recognizes Java's source-symbol spellings -----

func TestParseAssignOp(t *testing.T) {
	cases := []struct {
		in   string
		want tree.AssignOp
	}{
		// Go enum names (what AssignOp.String emits)
		{"Equals", tree.AssignOpEquals},
		{"Define", tree.AssignOpDefine},
		// Java source-symbol forms (what JavaSender ships)
		{"=", tree.AssignOpEquals},
		{":=", tree.AssignOpDefine},
		// Unknown spelling: 0 lets the caller fall back deliberately.
		{"<-", 0},
		{"", 0},
	}
	for _, c := range cases {
		if got := tree.ParseAssignOp(c.in); got != c.want {
			t.Errorf("ParseAssignOp(%q): want %v, got %v", c.in, c.want, got)
		}
	}
}

// ----- Group 3b: coerceLeftPaddedAssignOp parses the string form -----

func TestCoerceLeftPaddedAssignOp_FromStringDefine(t *testing.T) {
	// given: Java ships the operator for a `range` loop as LeftPadded[string]{":="}
	// because ParseAssignOp didn't know ":=" before this fix.
	var wire any = tree.LeftPadded[string]{
		Before:  tree.EmptySpace,
		Element: ":=",
		Markers: tree.Markers{},
	}

	// when
	got := coerceLeftPaddedAssignOp(wire)

	// then
	if got.Element != tree.AssignOpDefine {
		t.Errorf("want AssignOpDefine, got %v", got.Element)
	}
}

func TestCoerceLeftPaddedAssignOp_FromStringEquals(t *testing.T) {
	var wire any = tree.LeftPadded[string]{Element: "="}
	if got := coerceLeftPaddedAssignOp(wire); got.Element != tree.AssignOpEquals {
		t.Errorf("want AssignOpEquals, got %v", got.Element)
	}
}

func TestCoerceLeftPaddedAssignOp_FromAlreadyTypedVariant(t *testing.T) {
	// already-correct variant should pass through unchanged.
	var wire any = tree.LeftPadded[tree.AssignOp]{Element: tree.AssignOpDefine}
	if got := coerceLeftPaddedAssignOp(wire); got.Element != tree.AssignOpDefine {
		t.Errorf("pass-through broke: got %v", got.Element)
	}
}

func TestCoerceLeftPaddedAssignOp_UnknownSpellingFallsBack(t *testing.T) {
	var wire any = tree.LeftPadded[string]{Element: "<-"}
	// Defense-in-depth: we'd rather emit a possibly-wrong = than crash the recipe.
	if got := coerceLeftPaddedAssignOp(wire); got.Element != tree.AssignOpEquals {
		t.Errorf("want fallback to AssignOpEquals, got %v", got.Element)
	}
}

func TestRawCastPanics_LeftPaddedAssignOpFromString(t *testing.T) {
	// Pre-fix: VisitForEachControl did `result.(tree.LeftPadded[tree.AssignOp])`
	// on a value that was actually LeftPadded[string].
	var wire any = tree.LeftPadded[string]{Element: ":="}
	expectPanic(t, "raw cast LP[string]->LP[AssignOp]", func() {
		_ = wire.(tree.LeftPadded[tree.AssignOp])
	})
}

// ----- Group 4: Container[Statement] from Container[Expression] -----

func TestCoerceContainerStatement_FromExpressionVariant(t *testing.T) {
	// given: a Container[Expression] of two MethodInvocations — both also
	// satisfy Statement. Java ships VisitCase bodies this way.
	mi1, mi2 := makeMethodInvocation(), makeMethodInvocation()
	var wire any = tree.Container[tree.Expression]{
		Before: tree.EmptySpace,
		Elements: []tree.RightPadded[tree.Expression]{
			{Element: mi1, Markers: tree.Markers{}},
			{Element: mi2, Markers: tree.Markers{}},
		},
		Markers: tree.Markers{},
	}

	// when
	got := coerceContainerStatement(wire)

	// then
	if len(got.Elements) != 2 {
		t.Fatalf("want 2 elements, got %d", len(got.Elements))
	}
	if got.Elements[0].Element.(*tree.MethodInvocation) != mi1 {
		t.Errorf("element[0] identity lost")
	}
	if got.Elements[1].Element.(*tree.MethodInvocation) != mi2 {
		t.Errorf("element[1] identity lost")
	}
}

func TestCoerceContainerExpression_FromStatementVariant(t *testing.T) {
	// given: a Container[Statement] of MethodInvocations — also satisfies Expression.
	mi := makeMethodInvocation()
	var wire any = tree.Container[tree.Statement]{
		Elements: []tree.RightPadded[tree.Statement]{
			{Element: mi, Markers: tree.Markers{}},
		},
	}

	// when
	got := coerceContainerExpression(wire)

	// then
	if len(got.Elements) != 1 || got.Elements[0].Element.(*tree.MethodInvocation) != mi {
		t.Fatalf("coerce dropped element: %+v", got)
	}
}

func TestRawCastPanics_ContainerStatementFromExpression(t *testing.T) {
	var wire any = tree.Container[tree.Expression]{}
	expectPanic(t, "raw cast Container[Expression]->Container[Statement]", func() {
		_ = wire.(tree.Container[tree.Statement])
	})
}

// ----- Group 5: containerFromElements with heterogeneous RP variants -----

func TestContainerFromElements_HeterogeneousElements(t *testing.T) {
	// given: a slice whose first entry is RightPadded[Expression] (used to set
	// the detected variant) but whose subsequent entries are RightPadded[Statement].
	// This is exactly what Java sends for switch-case body containers when the
	// payload includes both expression-statements and pure statements.
	mi1 := makeMethodInvocation()
	mi2 := makeMethodInvocation()
	elements := []any{
		tree.RightPadded[tree.Expression]{Element: mi1, Markers: tree.Markers{}},
		tree.RightPadded[tree.Statement]{Element: mi2, Markers: tree.Markers{}},
	}

	// when: containerFromElements must coerce element 2 to Expression — the
	// pre-fix raw cast panicked here.
	got := containerFromElements(tree.EmptySpace, elements, tree.Markers{})

	// then: a Container[Expression] with both elements preserved.
	cont, ok := got.(tree.Container[tree.Expression])
	if !ok {
		t.Fatalf("want Container[Expression], got %T", got)
	}
	if len(cont.Elements) != 2 {
		t.Fatalf("want 2 elements, got %d", len(cont.Elements))
	}
	if cont.Elements[0].Element.(*tree.MethodInvocation) != mi1 {
		t.Errorf("element[0] identity lost")
	}
	if cont.Elements[1].Element.(*tree.MethodInvocation) != mi2 {
		t.Errorf("element[1] identity lost — coerce dropped the Statement-variant entry")
	}
}

func TestContainerFromElements_HeterogeneousStatementFirst(t *testing.T) {
	// given: opposite ordering — Statement detected first, Expression follows.
	mi1, mi2 := makeMethodInvocation(), makeMethodInvocation()
	elements := []any{
		tree.RightPadded[tree.Statement]{Element: mi1, Markers: tree.Markers{}},
		tree.RightPadded[tree.Expression]{Element: mi2, Markers: tree.Markers{}},
	}

	// when
	got := containerFromElements(tree.EmptySpace, elements, tree.Markers{})

	// then
	cont, ok := got.(tree.Container[tree.Statement])
	if !ok {
		t.Fatalf("want Container[Statement], got %T", got)
	}
	if len(cont.Elements) != 2 {
		t.Fatalf("want 2 elements, got %d", len(cont.Elements))
	}
	if cont.Elements[1].Element.(*tree.MethodInvocation) != mi2 {
		t.Errorf("element[1] identity lost — coerce dropped the Expression-variant entry")
	}
}

func TestRawCastPanics_HeterogeneousRightPadded(t *testing.T) {
	// Pre-fix containerFromElements: detected RP[Expression] from elements[0],
	// then raw-cast elements[1] (an RP[Statement]) to RP[Expression] -> panic.
	var second any = tree.RightPadded[tree.Statement]{Element: makeMethodInvocation()}
	expectPanic(t, "raw cast RP[Statement]->RP[Expression]", func() {
		_ = second.(tree.RightPadded[tree.Expression])
	})
}

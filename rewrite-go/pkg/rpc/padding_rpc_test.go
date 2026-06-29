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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
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
func makeIdent(name string) *java.Identifier {
	return &java.Identifier{ID: uuid.New(), Name: name}
}

// makeMethodInvocation returns a *MethodInvocation (implements both Statement
// and Expression — Java sometimes ships it inside RightPadded[Expression]
// where the Go side expects RightPadded[Statement] and vice versa).
func makeMethodInvocation() *java.MethodInvocation {
	return &java.MethodInvocation{ID: uuid.New(), Name: makeIdent("doThing")}
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
	var wire any = java.RightPadded[java.Expression]{
		Element: mi,
		After:   java.EmptySpace,
		Markers: java.Markers{},
	}

	// when: the receiver coerces it to RightPadded[Statement]
	got := coerceToStatementRP(wire)

	// then: the element survives, now typed as Statement
	if got.Element == nil {
		t.Fatal("Element nil after coerce")
	}
	if got.Element.(*java.MethodInvocation) != mi {
		t.Errorf("Element identity lost: want %p, got %p", mi, got.Element)
	}
}

func TestRawCastPanics_RightPaddedStatementFromExpression(t *testing.T) {
	// Pre-fix behavior: java_receiver.go did `result.(java.RightPadded[java.Statement])`.
	// Lock that panic in as a regression sentinel.
	var wire any = java.RightPadded[java.Expression]{
		Element: makeMethodInvocation(),
		Markers: java.Markers{},
	}
	expectPanic(t, "raw cast RP[Expression]->RP[Statement]", func() {
		_ = wire.(java.RightPadded[java.Statement])
	})
}

// ----- Group 2: LeftPadded[*Identifier] from LeftPadded[Expression] -----

func TestCoerceLeftPaddedIdent_AcceptsExpressionVariant(t *testing.T) {
	// given: a LeftPadded[Expression] wrapping an *Identifier — the shape
	// Java emits for FieldAccess.name and Import.alias.
	id := makeIdent("Foo")
	var wire any = java.LeftPadded[java.Expression]{
		Before:  java.EmptySpace,
		Element: id,
		Markers: java.Markers{},
	}

	// when
	got := coerceLeftPaddedIdent(wire)

	// then
	if got.Element != id {
		t.Errorf("Element identity lost: want %p, got %p", id, got.Element)
	}
}

func TestRawCastPanics_LeftPaddedIdentFromExpression(t *testing.T) {
	var wire any = java.LeftPadded[java.Expression]{
		Element: makeIdent("Foo"),
		Markers: java.Markers{},
	}
	expectPanic(t, "raw cast LP[Expression]->LP[*Identifier]", func() {
		_ = wire.(java.LeftPadded[*java.Identifier])
	})
}

// ----- Group 4: coerceRightPaddedTyped[T] element coercion -----
//
// receiveContainerTyped[T] builds Container[T] by running each received element
// through coerceRightPaddedTyped[T], so these element-level tests lock in the
// guarantees the deleted coerceContainerStatement/coerceContainerExpression/
// containerFromElements heuristics used to provide — without the type inference.

func TestCoerceRightPaddedTyped_StatementFromExpressionVariant(t *testing.T) {
	// given: a RightPadded[Expression] wrapping a *MethodInvocation (also a
	// Statement). Java ships VisitCase bodies and parameter lists this way.
	mi := makeMethodInvocation()
	var wire any = java.RightPadded[java.Expression]{Element: mi, Markers: java.Markers{}}

	// when: a Statement-typed field coerces it.
	got := coerceRightPaddedTyped[java.Statement](wire)

	// then: the element survives, now typed as Statement.
	if got.Element.(*java.MethodInvocation) != mi {
		t.Errorf("element identity lost: want %p, got %p", mi, got.Element)
	}
}

func TestCoerceRightPaddedTyped_ExpressionFromStatementVariant(t *testing.T) {
	// given: a RightPadded[Statement] wrapping a *MethodInvocation (also Expression).
	mi := makeMethodInvocation()
	var wire any = java.RightPadded[java.Statement]{Element: mi, Markers: java.Markers{}}

	// when
	got := coerceRightPaddedTyped[java.Expression](wire)

	// then
	if got.Element.(*java.MethodInvocation) != mi {
		t.Errorf("element identity lost: want %p, got %p", mi, got.Element)
	}
}

// makeReturnStatement returns a *java.Return — implements Statement but NOT Expression.
// Used to exercise mixed Case.Body containers whose elements include statement-only nodes.
func makeReturnStatement() *java.Return {
	return &java.Return{ID: uuid.New()}
}

func TestCoerceRightPaddedTyped_StatementOnlyElementSurvives(t *testing.T) {
	// given: a *Return (Statement-only, NOT an Expression) arriving in a
	// type-erased RightPadded[java.J] — the shape a mixed Case.Body produces.
	// Pre-fix, inferring Expression for the whole container silently dropped this
	// element (truncated Go source after a round trip); a Statement-typed field
	// must preserve it.
	ret := makeReturnStatement()
	var wire any = java.RightPadded[java.J]{Element: ret, Markers: java.Markers{}}

	// when
	got := coerceRightPaddedTyped[java.Statement](wire)

	// then
	if got.Element.(*java.Return) != ret {
		t.Errorf("return statement identity lost — pre-fix this was silently dropped")
	}
}

func TestRawCastPanics_ContainerStatementFromExpression(t *testing.T) {
	// Sentinel for the cross-instantiation panic receiveContainerTyped avoids:
	// an inferred Container[Expression] is not assertable to Container[Statement].
	var wire any = java.Container[java.Expression]{}
	expectPanic(t, "raw cast Container[Expression]->Container[Statement]", func() {
		_ = wire.(java.Container[java.Statement])
	})
}

func TestRawCastPanics_HeterogeneousRightPadded(t *testing.T) {
	// Sentinel: RightPadded[Statement] is not assertable to RightPadded[Expression]
	// — coerceRightPaddedTyped re-wraps instead of asserting.
	var second any = java.RightPadded[java.Statement]{Element: makeMethodInvocation()}
	expectPanic(t, "raw cast RP[Statement]->RP[Expression]", func() {
		_ = second.(java.RightPadded[java.Expression])
	})
}

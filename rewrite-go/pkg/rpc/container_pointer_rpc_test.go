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

// These tests cover the pointer-typed *Container Print round-trip on the NO_CHANGE
// path — the direct continuation of #7831. On NO_CHANGE, q.Receive returns the
// `before` value verbatim. For a nullable *Container[T] field that before-value is
// the *Container[T] pointer, so a raw `result.(java.Container[T])` value-cast panics
// with "interface conversion: ... is *java.Container[...], not java.Container[...]"
// (a pointer-vs-value mismatch, not an element-type mismatch). The fix derefs the
// before-pointer to a value baseline so the NO_CHANGE path returns an assertable
// value Container[T]; receivePointerContainer / receiveContainer centralize that so
// the class can't recur.

// roundTripNodeWithBefore is like roundTripNode but drives the sender with a distinct
// `before` baseline, so fields whose after-value is identical to the before-value emit
// NO_CHANGE messages (the sender diffs each field against q.before). This is what a
// real session produces when a subtree is unchanged since the last GET_OBJECT cycle.
func roundTripNodeWithBefore(t *testing.T, after, before, seed java.Tree) any {
	t.Helper()
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, make(map[uintptr]int))
	// Diff `after` against `before`; identical field pointers/values emit NO_CHANGE.
	sendQ.before = before
	NewGoSender().Visit(after, sendQ)
	sendQ.Flush()

	delivered := false
	recvQ := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return messages
	})
	return NewGoReceiver().Visit(seed, recvQ)
}

func TestParameterizedTypeRoundTrip_TypeParametersNoChange(t *testing.T) {
	// given: a ParameterizedType whose TypeParameters container is unchanged
	// (after and before share the same *Container pointer) but whose Clazz differs,
	// so the container field receives a NO_CHANGE message while the node is a change.
	ptID := uuid.New()
	typeParams := &java.Container[java.Expression]{
		Elements: []java.RightPadded[java.Expression]{
			{Element: makeIdent("string"), Markers: java.Markers{}},
		},
	}
	before := &java.ParameterizedType{
		ID:             ptID,
		Clazz:          makeIdent("List"),
		TypeParameters: typeParams,
	}
	after := &java.ParameterizedType{
		ID:             ptID,
		Clazz:          makeIdent("Map"), // changed so the node itself is a change
		TypeParameters: typeParams,       // SAME pointer -> NO_CHANGE for this field
	}
	// seed mirrors a baseline from a prior receive: TypeParameters is a non-nil
	// *Container, which is the before-value the NO_CHANGE path hands back.
	seed := &java.ParameterizedType{
		ID:             ptID,
		TypeParameters: &java.Container[java.Expression]{Elements: typeParams.Elements},
	}

	// when: the round-trip must not panic on the container field's NO_CHANGE message.
	got := roundTripNodeWithBefore(t, after, before, seed).(*java.ParameterizedType)

	// then: TypeParameters stays a *Container[Expression] with its element intact.
	if got.TypeParameters == nil {
		t.Fatal("TypeParameters: got nil, want non-nil *Container[Expression]")
	}
	if len(got.TypeParameters.Elements) != 1 {
		t.Fatalf("TypeParameters.Elements: got %d, want 1", len(got.TypeParameters.Elements))
	}
	if id, ok := got.TypeParameters.Elements[0].Element.(*java.Identifier); !ok || id.Name != "string" {
		t.Errorf("TypeParameters[0]: got %+v, want Identifier{string}", got.TypeParameters.Elements[0].Element)
	}
}

func TestParameterizedTypeRoundTrip_TypeParametersChange(t *testing.T) {
	// given: a fresh ParameterizedType (Add path) with one type argument.
	ptID := uuid.New()
	before := &java.ParameterizedType{
		ID:    ptID,
		Clazz: makeIdent("List"),
		TypeParameters: &java.Container[java.Expression]{
			Elements: []java.RightPadded[java.Expression]{
				{Element: makeIdent("int"), Markers: java.Markers{}},
			},
		},
	}
	seed := &java.ParameterizedType{ID: ptID}

	got := roundTripNode(t, before, seed).(*java.ParameterizedType)

	if got.TypeParameters == nil {
		t.Fatal("TypeParameters: got nil, want non-nil")
	}
	if len(got.TypeParameters.Elements) != 1 {
		t.Fatalf("TypeParameters.Elements: got %d, want 1", len(got.TypeParameters.Elements))
	}
	if id, ok := got.TypeParameters.Elements[0].Element.(*java.Identifier); !ok || id.Name != "int" {
		t.Errorf("TypeParameters[0]: got %+v, want Identifier{int}", got.TypeParameters.Elements[0].Element)
	}
}

func TestParameterizedTypeRoundTrip_NilTypeParameters(t *testing.T) {
	// given: a ParameterizedType with no type arguments (nil container) — must stay nil.
	ptID := uuid.New()
	before := &java.ParameterizedType{ID: ptID, Clazz: makeIdent("string")}
	seed := &java.ParameterizedType{ID: ptID}

	got := roundTripNode(t, before, seed).(*java.ParameterizedType)
	if got.TypeParameters != nil {
		t.Errorf("TypeParameters: got %+v, want nil", got.TypeParameters)
	}
}

func TestCompositeRoundTrip_ElementsNoChange(t *testing.T) {
	// given: a Go Composite whose Elements is a VALUE Container[Expression]. The
	// value-container NO_CHANGE path returns the value baseline directly; this guards
	// that routing value containers through receiveContainer keeps that behavior.
	compID := uuid.New()
	elements := java.Container[java.Expression]{
		Elements: []java.RightPadded[java.Expression]{
			{Element: makeIdent("a"), Markers: java.Markers{}},
		},
	}
	before := &golang.Composite{
		ID:       compID,
		TypeExpr: makeIdent("T"),
		Elements: elements,
	}
	after := &golang.Composite{
		ID:       compID,
		TypeExpr: makeIdent("U"), // changed so the node is a change
		Elements: elements,       // identical value -> NO_CHANGE for this field
	}
	seed := &golang.Composite{
		ID:       compID,
		Elements: java.Container[java.Expression]{Elements: elements.Elements},
	}

	got := roundTripNodeWithBefore(t, after, before, seed).(*golang.Composite)

	if len(got.Elements.Elements) != 1 {
		t.Fatalf("Elements: got %d, want 1", len(got.Elements.Elements))
	}
	if id, ok := got.Elements.Elements[0].Element.(*java.Identifier); !ok || id.Name != "a" {
		t.Errorf("Elements[0]: got %+v, want Identifier{a}", got.Elements.Elements[0].Element)
	}
}

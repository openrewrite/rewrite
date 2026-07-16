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
	"reflect"
	"testing"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// roundTripNode serializes `before` via GoSender, then feeds the
// emitted RpcObjectData stream into a ReceiveQueue and reads it via
// GoReceiver. The `seed` argument is the empty node skeleton the
// receiver starts from (matching how a real session has a baseline
// from a prior GET_OBJECT cycle).
func roundTripNode(t *testing.T, before java.Tree, seed java.Tree) any {
	t.Helper()
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, NewReferenceMap())
	NewGoSender().Visit(before, sendQ)
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

func TestJavaReceiverPreVisitAppliesWireID(t *testing.T) {
	id := uuid.MustParse("12345678-1111-2222-3333-123456789abc")
	got := roundTripNode(t,
		&java.Identifier{ID: id, Name: "x"},
		&java.Identifier{},
	).(*java.Identifier)

	if got.ID != id {
		t.Errorf("ID: got %s, want %s", got.ID, id)
	}
}

func TestAnnotationRpcRoundTrip_BasicTag(t *testing.T) {
	// Mirror of the `json:"name"` shape the parser will eventually emit
	// for struct field tags.
	annID := uuid.MustParse("aaaaaaaa-1111-2222-3333-aaaaaaaaaaaa")
	typeID := uuid.MustParse("bbbbbbbb-1111-2222-3333-bbbbbbbbbbbb")
	litID := uuid.MustParse("cccccccc-1111-2222-3333-cccccccccccc")
	before := &java.Annotation{
		ID:             annID,
		AnnotationType: &java.Identifier{ID: typeID, Name: "json"},
		Arguments: &java.Container[java.Expression]{
			Elements: []java.RightPadded[java.Expression]{
				{Element: &java.Literal{
					ID:     litID,
					Source: `"name"`,
					Value:  "name",
				}},
			},
		},
	}

	seed := &java.Annotation{ID: annID}
	got := roundTripNode(t, before, seed).(*java.Annotation)

	if got.ID != annID {
		t.Errorf("ID: got %s, want %s", got.ID, annID)
	}
	gotType, ok := got.AnnotationType.(*java.Identifier)
	if !ok {
		t.Fatalf("AnnotationType: got %T, want *Identifier", got.AnnotationType)
	}
	if gotType.Name != "json" {
		t.Errorf("AnnotationType.Name: got %q, want %q", gotType.Name, "json")
	}
	if got.Arguments == nil {
		t.Fatal("Arguments: got nil, want non-nil")
	}
	if len(got.Arguments.Elements) != 1 {
		t.Fatalf("Arguments.Elements: got %d, want 1", len(got.Arguments.Elements))
	}
	gotLit, ok := got.Arguments.Elements[0].Element.(*java.Literal)
	if !ok {
		t.Fatalf("Arguments[0]: got %T, want *Literal", got.Arguments.Elements[0].Element)
	}
	if gotLit.Source != `"name"` {
		t.Errorf("Arguments[0].Source: got %q, want %q", gotLit.Source, `"name"`)
	}
	if v, _ := gotLit.Value.(string); v != "name" {
		t.Errorf("Arguments[0].Value: got %v, want %q", gotLit.Value, "name")
	}
}

func TestAnnotationRpcRoundTrip_NoArguments(t *testing.T) {
	// Bare-args case (Arguments == nil) — what `//go:noinline` will
	// produce. Receiver must produce nil Arguments, not an empty
	// Container.
	annID := uuid.MustParse("dddddddd-1111-2222-3333-dddddddddddd")
	typeID := uuid.MustParse("eeeeeeee-1111-2222-3333-eeeeeeeeeeee")
	before := &java.Annotation{
		ID:             annID,
		AnnotationType: &java.Identifier{ID: typeID, Name: "go:noinline"},
	}

	seed := &java.Annotation{ID: annID}
	got := roundTripNode(t, before, seed).(*java.Annotation)

	if got.Arguments != nil {
		t.Errorf("Arguments: got %+v, want nil", got.Arguments)
	}
	if !reflect.DeepEqual(got.AnnotationType.(*java.Identifier).Name, "go:noinline") {
		t.Errorf("AnnotationType.Name: got %q, want %q", got.AnnotationType.(*java.Identifier).Name, "go:noinline")
	}
}

func TestAnnotationRpcRoundTrip_PrefixPreserved(t *testing.T) {
	annID := uuid.MustParse("ffffffff-1111-2222-3333-ffffffffffff")
	typeID := uuid.MustParse("00000000-1111-2222-3333-000000000000")
	litID := uuid.MustParse("11111111-aaaa-bbbb-cccc-111111111111")
	before := &java.Annotation{
		ID:             annID,
		Prefix:         java.Space{Whitespace: " "},
		AnnotationType: &java.Identifier{ID: typeID, Name: "validate"},
		Arguments: &java.Container[java.Expression]{
			Elements: []java.RightPadded[java.Expression]{
				{Element: &java.Literal{
					ID:     litID,
					Source: `"required"`,
					Value:  "required",
				}},
			},
		},
	}

	seed := &java.Annotation{ID: annID}
	got := roundTripNode(t, before, seed).(*java.Annotation)

	if got.Prefix.Whitespace != " " {
		t.Errorf("Prefix.Whitespace: got %q, want %q", got.Prefix.Whitespace, " ")
	}
}

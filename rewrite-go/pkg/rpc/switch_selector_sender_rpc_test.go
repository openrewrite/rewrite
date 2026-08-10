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

// Drives the GoSender over `before` and returns the emitted wire messages,
// without a receive step.
func serializeNode(t *testing.T, before java.Tree) []RpcObjectData {
	t.Helper()
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, NewReferenceMap())
	NewGoSender().Visit(before, sendQ)
	sendQ.Flush()
	return messages
}

// Verifies that serializing the same switch twice produces identical wire messages,
// covering the deterministic IDs synthesized for the tagless `select {}` / `switch {}`
// path (the ControlParentheses wrapper and its synthetic Empty selector).
func TestSwitchSelectorSend_TaglessDeterministic(t *testing.T) {
	sw := &java.Switch{
		ID:   uuid.New(),
		Body: &java.Block{ID: uuid.New(), End: java.Space{Whitespace: "\n"}},
	}

	first := serializeNode(t, sw)
	second := serializeNode(t, sw)

	if !reflect.DeepEqual(first, second) {
		t.Fatalf("tagless switch serialization is non-deterministic across sends:\nfirst:  %+v\nsecond: %+v", first, second)
	}
}

// Verifies that the space between the selector and the opening `{` survives the
// round trip.
func TestSwitchSelectorRoundTrip_PreservesTagAfterSpace(t *testing.T) {
	swID := uuid.New()
	before := &java.Switch{
		ID:   swID,
		Tag:  &java.RightPadded[java.Expression]{Element: makeIdent("x"), After: java.Space{Whitespace: " "}},
		Body: &java.Block{ID: uuid.New(), End: java.Space{Whitespace: "\n"}},
	}
	seed := &java.Switch{ID: swID}

	got := roundTripNode(t, before, seed).(*java.Switch)

	if got.Tag == nil {
		t.Fatal("Tag: got nil, want the selector")
	}
	if got.Tag.After.Whitespace != " " {
		t.Errorf("Tag.After.Whitespace: got %q, want %q (the space before `{`)", got.Tag.After.Whitespace, " ")
	}
}

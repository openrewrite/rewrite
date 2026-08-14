/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
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

func sendMarkers(m java.Markers) []RpcObjectData {
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, NewReferenceMap())
	SendMarkersCodec(m, sendQ)
	sendQ.Flush()
	return messages
}

func receiveMarkers(messages []RpcObjectData) java.Markers {
	delivered := false
	recvQ := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return messages
	})
	return receiveMarkersCodec(recvQ, java.Markers{})
}

func streamCarries(messages []RpcObjectData, value string) bool {
	for _, m := range messages {
		if s, ok := m.Value.(string); ok && s == value {
			return true
		}
	}
	return false
}

func TestRecipesThatMadeChangesRoundTrip(t *testing.T) {
	displayName := "Change text"
	instanceName := "Change text to `hello`"
	effort := int64(300000)
	markerID := uuid.MustParse("11111111-2222-3333-4444-555555555555")

	before := java.Markers{ID: uuid.New(), Entries: []java.Marker{
		java.RecipesThatMadeChanges{
			Ident: markerID,
			Recipes: [][]java.RecipeIdentity{{
				{Name: "org.openrewrite.text.ChangeText"},
				{
					Name:                               "org.openrewrite.text.FindAndReplace",
					DisplayName:                        &displayName,
					InstanceName:                       &instanceName,
					Options:                            map[string]any{"toText": "hello"},
					EstimatedEffortPerOccurrenceMillis: &effort,
				},
			}},
		},
	}}

	// Hop 1 exercises the receiver against a Java-shaped stream; hop 2 exercises this peer's
	// own sender against what its receiver produced. Markers travel as refs, so both hops go
	// through SendMarkersCodec rather than sending the marker directly.
	firstHop := sendMarkers(before)
	got := receiveMarkers(firstHop)

	secondHop := sendMarkers(got)
	if !streamCarries(secondHop, "org.openrewrite.text.FindAndReplace") {
		t.Fatalf("second hop carried no recipe identity; the marker diffed as NO_CHANGE")
	}
	got = receiveMarkers(secondHop)

	if len(got.Entries) != 1 {
		t.Fatalf("entries: want 1, got %d", len(got.Entries))
	}
	m, ok := got.Entries[0].(java.RecipesThatMadeChanges)
	if !ok {
		t.Fatalf("entry is %T, want java.RecipesThatMadeChanges", got.Entries[0])
	}
	if m.Ident != markerID {
		t.Errorf("id: want %v, got %v", markerID, m.Ident)
	}
	if len(m.Recipes) != 1 || len(m.Recipes[0]) != 2 {
		t.Fatalf("recipes: want one stack of 2, got %v", m.Recipes)
	}

	// A frame carrying only a name must not acquire values from its neighbour.
	if first := m.Recipes[0][0]; first.Name != "org.openrewrite.text.ChangeText" ||
		first.DisplayName != nil || first.InstanceName != nil ||
		first.Options != nil || first.EstimatedEffortPerOccurrenceMillis != nil {
		t.Errorf("first frame: want name only, got %+v", first)
	}

	second := m.Recipes[0][1]
	if second.Name != "org.openrewrite.text.FindAndReplace" {
		t.Errorf("name: want FindAndReplace, got %q", second.Name)
	}
	if second.DisplayName == nil || *second.DisplayName != displayName {
		t.Errorf("displayName: want %q, got %v", displayName, second.DisplayName)
	}
	if second.InstanceName == nil || *second.InstanceName != instanceName {
		t.Errorf("instanceName: want %q, got %v", instanceName, second.InstanceName)
	}
	if second.EstimatedEffortPerOccurrenceMillis == nil || *second.EstimatedEffortPerOccurrenceMillis != effort {
		t.Errorf("effort: want %d, got %v", effort, second.EstimatedEffortPerOccurrenceMillis)
	}
	options, ok := second.Options.(map[string]any)
	if !ok || options["toText"] != "hello" {
		t.Errorf("options: want map with toText=hello, got %#v", second.Options)
	}
}

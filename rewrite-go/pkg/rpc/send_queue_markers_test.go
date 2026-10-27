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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// A node whose Markers was left as the nil *markersData (rather than EmptyMarkers)
// must still be sent as an ADD carrying the empty markers, not diffed nil-against-nil
// into a NO_CHANGE. Otherwise the Java receiver keeps the null its freshly
// instantiated node started with, producing a null J.markers across the whole tree.
func TestSendNilMarkersEmitsAddNotNoChange(t *testing.T) {
	// given
	var messages []RpcObjectData
	q := NewSendQueue(100, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, NewReferenceMap())

	var nilMarkers java.Markers // nil *markersData

	// when
	q.Send(nilMarkers, nil, func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	q.Flush()

	// then
	if len(messages) == 0 {
		t.Fatal("expected at least one message, got none")
	}
	if messages[0].State == NoChange {
		t.Fatalf("nil Markers diffed to NO_CHANGE; Java would keep a null markers. messages=%v", messages)
	}
	if messages[0].State != Add {
		t.Fatalf("expected ADD for a nil (empty) Markers, got state=%v", messages[0].State)
	}
}

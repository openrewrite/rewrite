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
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// Every marker-free node shares EmptyMarkers, so its Markers travel as a ref: sent in
// full once per connection and cited by a one-message ADD afterwards.
func TestEmptyMarkersCrossTheWireOnceAndAreSharedOnReceipt(t *testing.T) {
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, NewReferenceMap())
	sender := NewGoSender()
	sender.Visit(&java.Identifier{ID: uuid.New(), Markers: java.EmptyMarkers, Name: "a"}, sendQ)
	sender.Visit(&java.Identifier{ID: uuid.New(), Markers: java.EmptyMarkers, Name: "b"}, sendQ)
	sendQ.Flush()

	var full []RpcObjectData
	for _, m := range messages {
		if m.ValueType != nil && *m.ValueType == "org.openrewrite.marker.Markers" {
			full = append(full, m)
		}
	}
	require.Len(t, full, 1)
	require.NotNil(t, full[0].Ref)
	hits := 0
	for _, m := range messages {
		if m.State == Add && m.ValueType == nil && m.Value == nil && m.Ref != nil && *m.Ref == *full[0].Ref {
			hits++
		}
	}
	assert.Equal(t, 1, hits, "the second node's Markers should be a ref-only ADD")

	delivered := false
	recvQ := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return messages
	})
	receiver := NewGoReceiver()
	first := receiver.Visit(&java.Identifier{}, recvQ).(*java.Identifier)
	second := receiver.Visit(&java.Identifier{}, recvQ).(*java.Identifier)
	assert.True(t, first.Markers == second.Markers, "received nodes should share one Markers instance")
	assert.Equal(t, java.EmptyMarkers.GetID(), second.Markers.GetID())
}

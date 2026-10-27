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
	"strings"
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestShortCommentFreeSpaceIsSentOnceAsRefAndOthersByValue(t *testing.T) {
	prefixes := []java.Space{
		java.MakeSpace(nil, " "),
		java.MakeSpace(nil, " "),
		java.MakeSpace([]java.Comment{{Multiline: true, Text: " c ", Suffix: " ", Markers: java.EmptyMarkers}}, " "),
		java.MakeSpace(nil, strings.Repeat(" ", 51)),
	}
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, NewReferenceMap())
	sender := NewGoSender()
	for _, p := range prefixes {
		sender.Visit(&java.Identifier{ID: uuid.New(), Prefix: p, Markers: java.EmptyMarkers, Name: "a"}, sendQ)
	}
	sendQ.Flush()

	byValue, defined := 0, 0
	for _, m := range messages {
		if m.ValueType != nil && *m.ValueType == "org.openrewrite.java.tree.Space" {
			if m.Ref == nil {
				byValue++
			} else {
				defined++
			}
		}
	}
	assert.Equal(t, 2, byValue, "the commented and the long Space go by value")
	assert.Equal(t, 1, defined, "the shared Space is defined once")

	delivered := false
	recvQ := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return messages
	})
	receiver := NewGoReceiver()
	var received []java.Space
	for range prefixes {
		received = append(received, receiver.Visit(&java.Identifier{}, recvQ).(*java.Identifier).Prefix)
	}
	assert.True(t, received[0] == received[1])
	require.Len(t, received[2].Comments(), 1)
	assert.Equal(t, " c ", received[2].Comments()[0].Text)
}

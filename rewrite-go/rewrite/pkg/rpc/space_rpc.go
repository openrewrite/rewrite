/*
 * Copyright 2025 the original author or authors.
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
	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/pkg/tree"
)

// sendSpace serializes a Space to the send queue.
// Matches JavaSender.visitSpace field order: comments (list), whitespace.
func sendSpace(s tree.Space, q *SendQueue) {
	// Comments list: id = text + suffix
	q.GetAndSendList(s,
		func(v any) []any {
			sp := v.(tree.Space)
			if sp.Comments == nil {
				return []any{} // must be empty slice, not nil, so Java gets ADD with empty list
			}
			result := make([]any, len(sp.Comments))
			for i, c := range sp.Comments {
				result[i] = c
			}
			return result
		},
		func(v any) any {
			c := v.(tree.Comment)
			return c.Text + c.Suffix
		},
		func(v any) {
			c := v.(tree.Comment)
			// multiline, text, suffix, markers (matching JavaSender.visitSpace comment fields)
			q.GetAndSend(c, func(x any) any { return x.(tree.Comment).Multiline }, nil)
			q.GetAndSend(c, func(x any) any { return x.(tree.Comment).Text }, nil)
			q.GetAndSend(c, func(x any) any { return x.(tree.Comment).Suffix }, nil)
			// Markers - use codec callback to match Java's Markers codec behavior
			q.GetAndSend(c, func(x any) any { return x.(tree.Comment).Markers },
				func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
		})
	// Whitespace
	q.GetAndSend(s, func(v any) any { return v.(tree.Space).Whitespace }, nil)
}

// receiveSpace deserializes a Space from the receive queue.
func receiveSpace(before tree.Space, q *ReceiveQueue) tree.Space {
	// Comments list
	commentsAny := make([]any, len(before.Comments))
	for i, c := range before.Comments {
		commentsAny[i] = c
	}
	afterComments := q.ReceiveList(commentsAny, func(v any) any {
		c := v.(tree.Comment)
		c.Multiline = receiveScalar[bool](q, c.Multiline)
		c.Text = receiveScalar[string](q, c.Text)
		c.Suffix = receiveScalar[string](q, c.Suffix)
		c.Markers = receiveMarkersCodec(q, c.Markers)
		return c
	})

	var comments []tree.Comment
	if afterComments != nil {
		comments = make([]tree.Comment, len(afterComments))
		for i, v := range afterComments {
			comments[i] = v.(tree.Comment)
		}
	}

	whitespace := receiveScalar[string](q, before.Whitespace)
	return tree.Space{Comments: comments, Whitespace: whitespace}
}

// SendMarkersCodec serializes Markers fields like Java's Markers.rpcSend codec.
// Sends: ID (uuid string), then marker entries list as ref.
func SendMarkersCodec(m tree.Markers, q *SendQueue) {
	// ID
	q.GetAndSend(m, func(v any) any { return v.(tree.Markers).ID.String() }, nil)
	// Entries list (as ref) - always send non-nil slice to match Java behavior
	q.GetAndSendListAsRef(m,
		func(v any) []any {
			markers := v.(tree.Markers)
			if markers.Entries == nil {
				return []any{}
			}
			result := make([]any, len(markers.Entries))
			for i, e := range markers.Entries {
				result[i] = e
			}
			return result
		},
		func(v any) any {
			return v.(tree.Marker).ID().String()
		},
		nil) // individual marker codecs would go here
}

// receiveMarkersCodec deserializes Markers fields like Java's Markers.rpcReceive codec.
func receiveMarkersCodec(q *ReceiveQueue, before tree.Markers) tree.Markers {
	// ID
	idStr := receiveScalar[string](q, before.ID.String())
	id := before.ID
	if idStr != "" && idStr != before.ID.String() {
		if parsed, err := uuid.Parse(idStr); err == nil {
			id = parsed
		}
	}
	// Entries list
	var beforeAny []any
	if before.Entries != nil {
		beforeAny = make([]any, len(before.Entries))
		for i, e := range before.Entries {
			beforeAny[i] = e
		}
	}
	afterAny := q.ReceiveList(beforeAny, nil)
	var entries []tree.Marker
	if afterAny != nil {
		entries = make([]tree.Marker, len(afterAny))
		for i, v := range afterAny {
			entries[i] = v.(tree.Marker)
		}
	}
	return tree.Markers{ID: id, Entries: entries}
}

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
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// A nil Markers must still travel as a Markers message, or the Java peer reconstructs null
// markers instead of Markers.EMPTY and every visit of the tree yields a new instance.
func TestNilMarkersStillCrossTheWire(t *testing.T) {
	for name, tree := range map[string]java.Tree{
		"J":          &java.Identifier{ID: uuid.New(), Name: "a"},
		"ParseError": &java.ParseError{Ident: uuid.New(), SourcePath: "a.go"},
		"GoMod":      &golang.GoMod{Ident: uuid.New(), SourcePath: "go.mod"},
		"GoSum":      &golang.GoSum{Ident: uuid.New(), SourcePath: "go.sum"},
	} {
		t.Run(name, func(t *testing.T) {
			// given
			var messages []RpcObjectData
			sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
				messages = append(messages, batch...)
			}, NewReferenceMap())

			// when
			NewGoSender().Visit(tree, sendQ)
			sendQ.Flush()

			// then
			var markerMessages []RpcObjectData
			for _, m := range messages {
				if m.ValueType != nil && *m.ValueType == "org.openrewrite.marker.Markers" {
					markerMessages = append(markerMessages, m)
				}
			}
			require.Len(t, markerMessages, 1)
		})
	}
}

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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestNilAnnotationsCrossTheWireAsAnEmptyList(t *testing.T) {
	id := uuid.New()
	for name, nilAndEmpty := range map[string][2]java.Tree{
		"Identifier": {
			&java.Identifier{ID: id, Name: "a"},
			&java.Identifier{ID: id, Name: "a", Annotations: []java.Tree{}},
		},
		"ParenthesizedTypeTree": {
			&java.ParenthesizedTypeTree{ID: id},
			&java.ParenthesizedTypeTree{ID: id, Annotations: []*java.Annotation{}},
		},
	} {
		t.Run(name, func(t *testing.T) {
			// the Java peer declares these lists non-null
			require.Equal(t, sendTree(nilAndEmpty[1]), sendTree(nilAndEmpty[0]))
		})
	}
}

func TestNilForClausesCrossTheWireAsEmptyLists(t *testing.T) {
	id := uuid.New()
	common := len(sendTree(&java.Empty{ID: id}))

	fields := sendTree(&java.ForControl{ID: id})[common:]

	// init and update are lists around the nullable condition
	require.Equal(t, []State{Add, Change, NoChange, Add, Change}, states(fields))
}

func sendTree(tree java.Tree) []RpcObjectData {
	var messages []RpcObjectData
	q := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, NewReferenceMap())
	NewGoSender().Visit(tree, q)
	q.Flush()
	return messages
}

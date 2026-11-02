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

// A receiver sets a node's prefix and markers through WithPrefix and
// WithMarkers, found by reflection. A node missing either keeps what it had
// and silently drops what the peer sent.
func TestEveryNodeTakesThePrefixAndMarkersItReceives(t *testing.T) {
	prefix := java.MakeSpace(nil, " ")
	markers := java.MakeMarkers(uuid.New(), []java.Marker{java.NewSearchResult("found")})
	nodes := 0
	for javaType, factory := range factories {
		node, ok := factory().(java.J)
		if !ok {
			continue
		}
		nodes++
		t.Run(javaType, func(t *testing.T) {
			withPrefix, ok := withPrefixViaReflection(node, prefix).(java.J)
			require.True(t, ok)
			assert.Equal(t, " ", withPrefix.GetPrefix().Whitespace())

			withMarkers, ok := withMarkersViaReflection(node, markers).(java.J)
			require.True(t, ok)
			assert.Len(t, withMarkers.GetMarkers().Entries(), 1)
		})
	}
	require.Positive(t, nodes)
}

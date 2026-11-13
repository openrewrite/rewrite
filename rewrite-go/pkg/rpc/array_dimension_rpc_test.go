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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestArrayDimensionIndexChange_AppliedOnReceive(t *testing.T) {
	// given
	accessID, dimensionID, indexID := uuid.New(), uuid.New(), uuid.New()
	arr := makeIdent("arr")
	arrayAccess := func(index string) *java.ArrayAccess {
		return &java.ArrayAccess{
			ID:      accessID,
			Indexed: arr,
			Dimension: &java.ArrayDimension{
				ID: dimensionID,
				Index: java.RightPadded[java.Expression]{
					Element: &java.Identifier{ID: indexID, Name: index},
					Markers: java.EmptyMarkers,
				},
			},
		}
	}
	before := arrayAccess("x")
	after := arrayAccess("flag")

	// when
	got := roundTripNodeWithBefore(t, after, before, arrayAccess("x")).(*java.ArrayAccess)

	// then
	index, ok := got.Dimension.Index.Element.(*java.Identifier)
	if !ok {
		t.Fatalf("Index: want *java.Identifier, got %T", got.Dimension.Index.Element)
	}
	if index.Name != "flag" {
		t.Errorf("Index: want flag after CHANGE, got %s", index.Name)
	}
}

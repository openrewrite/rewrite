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

package template

import (
	"reflect"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// semanticMarkers hold what the printer emits as keywords, so two nodes
// carrying different ones are different source however alike their fields
// are: `:=` and `const` live here rather than in the tree.
var semanticMarkers = map[reflect.Type]bool{
	reflect.TypeOf(golang.ShortVarDecl{}):    true,
	reflect.TypeOf(golang.ConstDecl{}):       true,
	reflect.TypeOf(golang.VarKeyword{}):      true,
	reflect.TypeOf(golang.InterfaceMethod{}): true,
	reflect.TypeOf(golang.StructTag{}):       true,
}

func semanticMarkerNames() map[string]bool {
	names := make(map[string]bool, len(semanticMarkers))
	for t := range semanticMarkers {
		names[t.Name()] = true
	}
	return names
}

// matchMarkers compares the semantic markers two nodes carry as sets.
func matchMarkers(pattern, candidate java.Markers) bool {
	p := semanticMarkerSet(pattern)
	q := semanticMarkerSet(candidate)
	if len(p) != len(q) {
		return false
	}
	for t, marker := range p {
		other, ok := q[t]
		if !ok {
			return false
		}
		if !sameMarkerValue(marker, other) {
			return false
		}
	}
	return true
}

func semanticMarkerSet(markers java.Markers) map[reflect.Type]java.Marker {
	var set map[reflect.Type]java.Marker
	for _, m := range markers.Entries {
		t := reflect.TypeOf(m)
		if !semanticMarkers[t] {
			continue
		}
		if set == nil {
			set = make(map[reflect.Type]java.Marker, 1)
		}
		set[t] = m
	}
	return set
}

// A StructTag carries the tag it stands for, so presence alone would read
// `json:"a"` and `json:"b"` as the same field. Every other semantic marker is
// a bare flag, and its presence is all there is to compare.
func sameMarkerValue(a, b java.Marker) bool {
	tag, ok := a.(golang.StructTag)
	if !ok {
		return true
	}
	other := b.(golang.StructTag)
	if tag.Tag == nil || other.Tag == nil {
		return tag.Tag == other.Tag
	}
	return tag.Tag.Source == other.Tag.Source
}

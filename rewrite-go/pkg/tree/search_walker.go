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

package tree

import (
	"reflect"

	"github.com/google/uuid"
)

// CollectSearchResultIDs walks the given tree and returns the IDs of every
// SearchResult and SearchResultMarker found in any node's Markers. The
// returned slice has stable insertion order; duplicates are dropped.
//
// Implementation: reflection-based descent. Every LST node has a `Markers
// Markers` field, plus zero or more child fields that are themselves
// trees (or wrappers like RightPadded / LeftPadded / Container holding
// trees). A visitor-based walker would be cleaner but would require every
// concrete node type to opt in; reflection lets BatchVisit collect search
// markers without touching the 60+ node definitions.
func CollectSearchResultIDs(t Tree) []uuid.UUID {
	w := &searchWalker{seen: make(map[uuid.UUID]struct{})}
	w.walk(reflect.ValueOf(t))
	return w.ids
}

type searchWalker struct {
	ids  []uuid.UUID
	seen map[uuid.UUID]struct{}
}

var (
	treeIface    = reflect.TypeOf((*Tree)(nil)).Elem()
	markersType  = reflect.TypeOf(Markers{})
	uuidType     = reflect.TypeOf(uuid.UUID{})
)

func (w *searchWalker) walk(v reflect.Value) {
	if !v.IsValid() {
		return
	}
	switch v.Kind() {
	case reflect.Ptr, reflect.Interface:
		if v.IsNil() {
			return
		}
		w.walk(v.Elem())
	case reflect.Struct:
		// Check for a Markers field on this struct and harvest search-result IDs.
		if mf := v.FieldByName("Markers"); mf.IsValid() && mf.Type() == markersType {
			w.collectFrom(mf.Interface().(Markers))
		}
		// Descend into every other field. We skip Markers (already handled)
		// and Prefix (a Space — never carries markers we care about).
		for i := 0; i < v.NumField(); i++ {
			f := v.Field(i)
			name := v.Type().Field(i).Name
			if name == "Markers" || name == "Prefix" || name == "ID" {
				continue
			}
			// Skip uuid.UUID values — they're not trees.
			if f.Type() == uuidType {
				continue
			}
			w.walk(f)
		}
	case reflect.Slice, reflect.Array:
		for i := 0; i < v.Len(); i++ {
			w.walk(v.Index(i))
		}
	}
}

func (w *searchWalker) collectFrom(m Markers) {
	for _, marker := range m.Entries {
		var id uuid.UUID
		switch x := marker.(type) {
		case SearchResult:
			id = x.Ident
		case *SearchResult:
			id = x.Ident
		case SearchResultMarker:
			id = x.Ident
		case *SearchResultMarker:
			id = x.Ident
		default:
			continue
		}
		if _, dup := w.seen[id]; dup {
			continue
		}
		w.seen[id] = struct{}{}
		w.ids = append(w.ids, id)
	}
}

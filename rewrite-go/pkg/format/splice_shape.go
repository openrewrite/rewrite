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

package format

import (
	"reflect"
	"sync"
)

// spliceShape is what the splicer needs to know about one LST type. Every
// answer depends only on the type, so each is computed once and shared across
// the walk.
type spliceShape struct {
	kind      reflect.Kind
	isSpace   bool
	isMarkers bool
	isTree    bool
	// carriesSpace reports whether a value of this type can hold a Space at
	// any depth. Values that can't are copied wholesale instead of walked.
	carriesSpace bool
	spaceFields  []int
}

var spliceShapes sync.Map // reflect.Type -> *spliceShape

func shapeOf(t reflect.Type) *spliceShape {
	if cached, ok := spliceShapes.Load(t); ok {
		return cached.(*spliceShape)
	}
	d := &spliceShape{
		kind:      t.Kind(),
		isSpace:   t == spaceType,
		isMarkers: t == markersType,
		isTree:    t.Implements(treeType),
		// LST types are mutually recursive. A type reached again while its own
		// shape is still being computed answers "yes", keeping the walk
		// over-inclusive rather than pruning a branch that does carry Spaces.
		carriesSpace: true,
	}
	spliceShapes.Store(t, d)

	switch {
	case d.isSpace || d.isMarkers:
		d.carriesSpace = true
	case t.Implements(javaTypeType):
		d.carriesSpace = false
	default:
		switch t.Kind() {
		case reflect.Interface:
			// The dynamic type decides, so assume the worst.
			d.carriesSpace = true
		case reflect.Pointer, reflect.Slice:
			d.carriesSpace = shapeOf(t.Elem()).carriesSpace
		case reflect.Struct:
			d.carriesSpace = false
			for i := 0; i < t.NumField(); i++ {
				f := t.Field(i)
				if f.PkgPath != "" {
					continue // unexported fields ride along on the struct copy
				}
				if shapeOf(f.Type).carriesSpace {
					d.spaceFields = append(d.spaceFields, i)
					d.carriesSpace = true
				}
			}
		default:
			d.carriesSpace = false
		}
	}
	return d
}

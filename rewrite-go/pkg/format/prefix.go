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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// getPrefix returns the `Prefix` field of any tree node that carries
// one. Reads via reflection so format visitors can manipulate prefixes
// uniformly across the ~50 concrete LST types without an exhaustive
// type-switch.
//
// Returns the zero Space when the node has no Prefix field (which is
// vanishingly rare — every J-conformant type carries one).
func getPrefix(t tree.Tree) tree.Space {
	if t == nil {
		return tree.Space{}
	}
	rv := reflect.ValueOf(t)
	if rv.Kind() == reflect.Ptr {
		if rv.IsNil() {
			return tree.Space{}
		}
		rv = rv.Elem()
	}
	if rv.Kind() != reflect.Struct {
		return tree.Space{}
	}
	f := rv.FieldByName("Prefix")
	if !f.IsValid() {
		return tree.Space{}
	}
	if s, ok := f.Interface().(tree.Space); ok {
		return s
	}
	return tree.Space{}
}

// withPrefix calls the node's `WithPrefix(Space) <T>` method to produce
// a copy with the given prefix. Returns the original node if it has no
// such method (defensive — every J-conformant type ships one).
func withPrefix[T tree.Tree](t T, prefix tree.Space) T {
	rv := reflect.ValueOf(t)
	m := rv.MethodByName("WithPrefix")
	if !m.IsValid() {
		return t
	}
	results := m.Call([]reflect.Value{reflect.ValueOf(prefix)})
	if len(results) == 0 {
		return t
	}
	if r, ok := results[0].Interface().(T); ok {
		return r
	}
	return t
}

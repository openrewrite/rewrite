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
	"reflect"

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// extractID returns the ID of any AST node, polymorphically via the J
// interface. Used as a list-element ID extractor in
// `q.GetAndSendList(...)`. Non-J values return uuid.Nil.
func extractID(v any) any {
	if j, ok := v.(tree.J); ok {
		return j.GetID()
	}
	return uuid.Nil
}

// withPrefixViaReflection invokes the concrete type's typed
// `WithPrefix(Space) *T` method via reflection and returns the result
// as tree.Tree. Mirrors what rewrite-java's `j.withPrefix(...)` does
// polymorphically — Go can't express that on the J interface because
// the typed method returns the concrete pointer (not J), and Go
// doesn't allow covariant return types on interface methods. Reflection
// is the cleanest way to call it without exposing in-place mutators
// on the public J interface (which would silently bypass the visitor
// framework's pointer-identity change detection).
//
// Used only by JavaReceiver.PreVisit. Returns t unchanged if the type
// doesn't implement WithPrefix (defensive — every J-conformant type
// does).
func withPrefixViaReflection(t tree.Tree, prefix tree.Space) tree.Tree {
	rv := reflect.ValueOf(t)
	m := rv.MethodByName("WithPrefix")
	if !m.IsValid() {
		return t
	}
	results := m.Call([]reflect.Value{reflect.ValueOf(prefix)})
	if len(results) == 0 {
		return t
	}
	if r, ok := results[0].Interface().(tree.Tree); ok {
		return r
	}
	return t
}

// withMarkersViaReflection is the WithMarkers counterpart.
func withMarkersViaReflection(t tree.Tree, markers tree.Markers) tree.Tree {
	rv := reflect.ValueOf(t)
	m := rv.MethodByName("WithMarkers")
	if !m.IsValid() {
		return t
	}
	results := m.Call([]reflect.Value{reflect.ValueOf(markers)})
	if len(results) == 0 {
		return t
	}
	if r, ok := results[0].Interface().(tree.Tree); ok {
		return r
	}
	return t
}

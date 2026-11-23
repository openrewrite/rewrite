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
	"reflect"
	"testing"
)

// value_types.go holds two parallel registries that MUST stay in sync:
//
//   - valueTypeMap (send side): Go type -> Java class name. The sender writes
//     this class name onto the wire for every object it emits.
//   - factories    (receive side): Java class name -> constructor. newObj uses
//     it to instantiate an incoming node when Java sends an ADD for that type.
//
// Anything the Go side can SEND, Java can send BACK (e.g. when a recipe edits
// the tree). So every value type needs a matching factory, or the receive path
// panics with "no factory registered for type: ...". This is exactly how the
// Go$DeclarationBlock node regressed: registered as a value type, but the
// factory was forgotten, so production parsing of grouped var/const blocks
// panicked the moment Java round-tripped the node.
//
// This test enforces the invariant generically — adding any new value type
// without its factory (or with a mismatched factory) fails here, with no
// per-type test needed.
func TestEveryValueTypeHasMatchingFactory(t *testing.T) {
	for goType, javaClassName := range valueTypeMap {
		// given: a registered send-side value type
		factory, ok := factories[javaClassName]
		if !ok {
			// then: a receivable factory must exist for the same class name
			t.Errorf("value type %s (registered as %q) has no RegisterFactory entry; "+
				"newObj will panic when Java sends this node back over the wire",
				goType, javaClassName)
			continue
		}

		// then: the factory must build the very type the sender announced,
		// otherwise the receiver populates the wrong struct
		built := reflect.TypeOf(factory())
		if built != goType {
			t.Errorf("factory for %q builds %s, but it is registered as value type %s",
				javaClassName, built, goType)
		}
	}
}

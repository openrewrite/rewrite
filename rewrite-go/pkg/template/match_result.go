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

package template

import (
	"reflect"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

type MatchResult struct {
	bindings map[string]any // single: java.J, variadic: []java.J
}

// The map is built by the first binding: most comparisons fail before there
// is one, and a match that fails is never read.
func NewMatchResult() *MatchResult {
	return &MatchResult{}
}

// bind stores a single captured value.
func (m *MatchResult) bind(name string, value java.J) {
	if m.bindings == nil {
		m.bindings = make(map[string]any)
	}
	m.bindings[name] = value
}

// Bind associates a capture with the subtree to substitute for it, so a
// template can be instantiated without a pattern match. Returns the receiver,
// for chaining.
func (m *MatchResult) Bind(c *Capture, value java.J) *MatchResult {
	m.bind(c.Name(), value)
	return m
}

// bindList stores a variadic captured list.
func (m *MatchResult) bindList(name string, values []java.J) {
	if m.bindings == nil {
		m.bindings = make(map[string]any)
	}
	m.bindings[name] = values
}

// BindList associates a capture with the subtrees to substitute for it, which
// the placeholder's enclosing list expands to. A run of any length is a
// binding, zero included. Elems builds the argument from the element slices a
// recipe holds. Returns the receiver, for chaining.
func (m *MatchResult) BindList(c *Capture, values []java.J) *MatchResult {
	m.bindList(c.Name(), values)
	return m
}

// Elems concatenates element slices into the []java.J BindList takes. Go
// converts neither []java.Expression nor []java.Statement to that on its own,
// and a recipe holds one of those rather than a []java.J.
func Elems[T java.J](groups ...[]T) []java.J {
	n := 0
	for _, group := range groups {
		n += len(group)
	}
	values := make([]java.J, 0, n)
	for _, group := range groups {
		for _, value := range group {
			values = append(values, value)
		}
	}
	return values
}

// listBinding returns the list bound to name, and whether the binding is a list
// at all. It is the binding's shape, not the capture's declaration, that
// decides how substitution treats a placeholder.
func (m *MatchResult) listBinding(name string) ([]java.J, bool) {
	if m == nil {
		return nil, false
	}
	list, ok := m.bindings[name].([]java.J)
	return list, ok
}

// satisfies reports whether c has a value substitution can use: a run within
// the capture's bounds, or a single subtree that holds a node.
func (m *MatchResult) satisfies(c *Capture) bool {
	if run, ok := m.listBinding(c.Name()); ok {
		return c.allowsCount(len(run))
	}
	return m.Get(c.Name()) != nil
}

func (m *MatchResult) Has(name string) bool {
	if m == nil {
		return false
	}
	_, ok := m.bindings[name]
	return ok
}

func (m *MatchResult) Get(name string) java.J {
	if m == nil {
		return nil
	}
	v, ok := m.bindings[name]
	if !ok {
		return nil
	}
	if j, ok := v.(java.J); ok && !isNilTree(j) {
		return j
	}
	return nil
}

// isNilTree reports a J holding no node. A caller that binds the nil result of
// a helper returning a concrete type leaves an interface that is itself
// non-nil, which every consumer would otherwise dereference.
func isNilTree(j java.J) bool {
	v := reflect.ValueOf(j)
	return v.Kind() == reflect.Ptr && v.IsNil()
}

func (m *MatchResult) GetList(name string) []java.J {
	v, ok := m.bindings[name]
	if !ok {
		return nil
	}
	if list, ok := v.([]java.J); ok {
		return list
	}
	// If a single value was bound, wrap it in a slice.
	if j, ok := v.(java.J); ok {
		return []java.J{j}
	}
	return nil
}

func (m *MatchResult) GetCapture(c *Capture) java.J {
	return m.Get(c.Name())
}

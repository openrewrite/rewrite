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

import "github.com/openrewrite/rewrite/rewrite-go/pkg/tree"

// MatchResult holds the captured AST nodes from a successful pattern match.
type MatchResult struct {
	bindings map[string]any // single: tree.J, variadic: []tree.J
}

// NewMatchResult creates a MatchResult from binding pairs.
func NewMatchResult() *MatchResult {
	return &MatchResult{bindings: make(map[string]any)}
}

// bind stores a single captured value.
func (m *MatchResult) bind(name string, value tree.J) {
	m.bindings[name] = value
}

// bindList stores a variadic captured list.
func (m *MatchResult) bindList(name string, values []tree.J) {
	m.bindings[name] = values
}

// Has returns true if a binding exists for the given capture name.
func (m *MatchResult) Has(name string) bool {
	_, ok := m.bindings[name]
	return ok
}

// Get returns the single captured node for the given name, or nil if not bound.
func (m *MatchResult) Get(name string) tree.J {
	v, ok := m.bindings[name]
	if !ok {
		return nil
	}
	if j, ok := v.(tree.J); ok {
		return j
	}
	return nil
}

// GetList returns the variadic captured nodes for the given name.
func (m *MatchResult) GetList(name string) []tree.J {
	v, ok := m.bindings[name]
	if !ok {
		return nil
	}
	if list, ok := v.([]tree.J); ok {
		return list
	}
	// If a single value was bound, wrap it in a slice.
	if j, ok := v.(tree.J); ok {
		return []tree.J{j}
	}
	return nil
}

// GetCapture returns the captured node for the given Capture, or nil.
func (m *MatchResult) GetCapture(c *Capture) tree.J {
	return m.Get(c.Name())
}

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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// patternComparator performs structural comparison between a pattern tree
// and a candidate tree, binding placeholder identifiers to captured nodes.
type patternComparator struct {
	captures map[string]*Capture
	result   *MatchResult
	cursor   *visitor.Cursor
}

func newPatternComparator(captures map[string]*Capture, cursor *visitor.Cursor) *patternComparator {
	return &patternComparator{
		captures: captures,
		result:   NewMatchResult(),
		cursor:   cursor,
	}
}

// match compares the pattern against the candidate. Returns the MatchResult
// on success, or nil on failure.
func (c *patternComparator) match(pattern, candidate java.J) *MatchResult {
	if c.matchNode(pattern, candidate) {
		return c.result
	}
	return nil
}

func (c *patternComparator) matchNode(pattern, candidate java.J) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}

	// Check if the pattern node is a placeholder identifier.
	if name, isPlaceholder := placeholderName(pattern); isPlaceholder {
		return c.bindCapture(name, candidate)
	}

	// Both nodes must be the same concrete type.
	if reflect.TypeOf(pattern) != reflect.TypeOf(candidate) {
		return false
	}

	// A literal means the text it was written as, and an Empty means only
	// that it is there; neither reads as the fields it holds.
	switch p := pattern.(type) {
	case *java.Literal:
		return p.Source == candidate.(*java.Literal).Source
	case *java.Empty:
		return true
	}
	return c.matchFields(pattern, candidate)
}

// bindCapture binds a captured value, checking for repeated captures
// (which enforce structural equality).
func (c *patternComparator) bindCapture(name string, candidate java.J) bool {
	if c.result.Has(name) {
		// Repeated capture: enforce structural equality with prior binding.
		prev := c.result.Get(name)
		return structurallyEqual(prev, candidate)
	}
	c.result.bind(name, candidate)
	return true
}

// placeholderName returns the capture name j stands for, if j is a placeholder
// identifier. An identifier is an expression, so one written in statement
// position reaches here inside the wrapper the parser puts it in.
func placeholderName(j java.J) (string, bool) {
	if stmt, ok := j.(*golang.ExpressionStatement); ok {
		j = stmt.Expression
	}
	ident, ok := j.(*java.Identifier)
	if !ok {
		return "", false
	}
	return FromPlaceholder(ident.Name)
}

// A comparator built for structural equality carries no captures, so every
// placeholder it meets is an ordinary identifier.
func (c *patternComparator) isVariadic(name string) bool {
	capture, ok := c.captures[name]
	return ok && capture.IsVariadic()
}

// bindRun binds the run a variadic capture absorbed, enforcing structural
// equality with a prior binding the way bindCapture does for a single node.
func (c *patternComparator) bindRun(name string, values []java.J) bool {
	prev, bound := c.result.listBinding(name)
	if !bound {
		c.result.bindList(name, values)
		return true
	}
	if len(prev) != len(values) {
		return false
	}
	for i := range prev {
		if !structurallyEqual(prev[i], values[i]) {
			return false
		}
	}
	return true
}

// structurallyEqual checks if two nodes are structurally equivalent
// (ignoring whitespace). Used for repeated captures.
func structurallyEqual(a, b java.J) bool {
	if a == nil && b == nil {
		return true
	}
	if a == nil || b == nil {
		return false
	}
	// Use a comparator with no captures — if it matches, they're equal.
	cmp := newPatternComparator(nil, nil)
	return cmp.matchNode(a, b)
}

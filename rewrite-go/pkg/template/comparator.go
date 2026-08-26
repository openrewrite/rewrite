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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// patternComparator performs structural comparison between a pattern tree
// and a candidate tree, binding placeholder identifiers to captured nodes.
type patternComparator struct {
	captures map[string]*Capture
	declared map[string]java.JavaType
	result   *MatchResult
	cursor   *visitor.Cursor
	mode     TypeMatchingMode
	variadic bool

	// skipFastPath routes every node through the reflective walk.
	skipFastPath bool

	// tracking records where a missing attribution decided a comparison, for
	// Explain. The path is kept only while it is on.
	tracking          bool
	path              []string
	inconclusive      int
	firstInconclusive []string
}

// allowsDeclaredType holds a capture to the type it was declared with. The
// declaration is the one type comparison an author writes by hand, so the mode
// does not switch it off; it decides only the candidate carrying no
// attribution.
func (c *patternComparator) allowsDeclaredType(name string, candidate java.J) bool {
	want, ok := c.declared[name]
	if !ok {
		return true
	}
	var actual java.JavaType
	if expr, ok := candidate.(java.Expression); ok {
		actual = matcher.TypeOfExpression(expr)
	}
	if actual == nil || java.IsUnknown(actual) {
		if c.tracking {
			c.noteInconclusive()
		}
		return c.mode == TypeMatchingLenient
	}
	return matcher.IsAssignableToType(actual, want)
}

func (c *patternComparator) matchTypeSlot(pattern, candidate java.JavaType) bool {
	if c.mode == TypeMatchingOff {
		return true
	}
	result, conclusive := compareTypes(pattern, candidate, c.mode)
	if !conclusive && c.tracking {
		c.noteInconclusive()
	}
	return result
}

// bindings is the result the comparator fills, built at the first binding:
// most comparisons fail before there is one.
func (c *patternComparator) bindings() *MatchResult {
	if c.result == nil {
		c.result = NewMatchResult()
	}
	return c.result
}

// match compares the pattern against the candidate. Returns the MatchResult
// on success, or nil on failure.
func (c *patternComparator) match(pattern, candidate java.J) *MatchResult {
	if c.matchNode(pattern, candidate) {
		return c.bindings()
	}
	return nil
}

func (c *patternComparator) matchNode(pattern, candidate java.J) bool {
	if pattern == nil || candidate == nil {
		return pattern == nil && candidate == nil
	}

	// Check if the pattern node is a placeholder identifier. A capture binds
	// what it found, parentheses and all.
	if name, isPlaceholder := placeholderName(pattern); isPlaceholder {
		return c.bindCapture(name, candidate)
	}

	// Both nodes must be the same concrete type. Parentheses record how the
	// source was written and the tree already says how it groups, so two
	// kinds that disagree are compared again through them.
	if reflect.TypeOf(pattern) != reflect.TypeOf(candidate) {
		inner, found := unparenthesize(pattern), unparenthesize(candidate)
		if inner == pattern && found == candidate {
			return false
		}
		return c.matchNode(inner, found)
	}

	if !matchMarkers(pattern.GetMarkers(), candidate.GetMarkers()) {
		return false
	}

	// A literal means what it evaluates to and the type it was read as, an
	// Empty means only that it is there, and a resolved package call means the
	// symbol it names; none reads as the fields it holds.
	switch p := pattern.(type) {
	case *java.Literal:
		q := candidate.(*java.Literal)
		return sameLiteralValue(p, q) && c.matchTypeSlot(p.Type, q.Type)
	case *java.Empty:
		return true
	case *java.MethodInvocation:
		if result, handled := c.matchByDeclaringType(p, candidate.(*java.MethodInvocation)); handled {
			return result
		}
	}
	if !c.skipFastPath {
		if result, handled := c.fastMatch(pattern, candidate); handled {
			return result
		}
	}
	return c.matchFields(pattern, candidate)
}

// bindCapture binds a captured value, checking for repeated captures
// (which enforce structural equality).
func (c *patternComparator) bindCapture(name string, candidate java.J) bool {
	if !c.allowsDeclaredType(name, candidate) {
		return false
	}
	if c.result.Has(name) {
		// Repeated capture: enforce structural equality with prior binding.
		prev := c.result.Get(name)
		return structurallyEqual(prev, candidate)
	}
	c.bindings().bind(name, candidate)
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
	for _, value := range values {
		if !c.allowsDeclaredType(name, value) {
			return false
		}
	}
	prev, bound := c.result.listBinding(name)
	if !bound {
		c.bindings().bindList(name, values)
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
	// A comparator with no captures meets no placeholder, so a match is
	// equality.
	return (&patternComparator{mode: TypeMatchingOff}).matchNode(a, b)
}

// unparenthesize reads through the parentheses around a node.
func unparenthesize(j java.J) java.J {
	for {
		switch p := j.(type) {
		case *java.Parentheses:
			j = p.Tree.Element
		case *java.ControlParentheses:
			j = p.Tree.Element
		case *java.ParenthesizedTypeTree:
			if p.Type == nil {
				return j
			}
			j = p.Type.Tree.Element
		default:
			return j
		}
		if j == nil {
			return nil
		}
	}
}

// sameLiteralValue compares what two literals evaluate to, so `0x1` and `1`
// are one literal written twice. A value of another kind is another literal,
// an integer and a float being different constants. Source stands in where
// the parser recorded no value, which is how two of them are told apart.
func sameLiteralValue(a, b *java.Literal) bool {
	if a.Value == nil || b.Value == nil {
		return a.Value == nil && b.Value == nil && a.Source == b.Source
	}
	at := reflect.TypeOf(a.Value)
	if at != reflect.TypeOf(b.Value) {
		return false
	}
	if !at.Comparable() {
		return a.Source == b.Source
	}
	return a.Value == b.Value
}

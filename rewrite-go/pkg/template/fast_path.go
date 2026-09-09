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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// A visitor method calls Match once per node it narrowed to, so the kinds a
// pattern most often lands on read their own fields rather than reflect over
// them. TestFastPathAgreesWithWalk holds these to what the walk answers; the
// only slots it cannot reach are the ones Go source never fills and the RPC
// peer sends — an identifier's annotations, a call's type parameters.
func (c *patternComparator) fastMatch(pattern, candidate java.J) (bool, bool) {
	switch p := pattern.(type) {
	case *java.Identifier:
		q := candidate.(*java.Identifier)
		return p.Name == q.Name &&
			c.matchTypeSlot(p.Type, q.Type) &&
			c.matchTypeSlot(typeOrNil(p.FieldType), typeOrNil(q.FieldType)) &&
			c.matchTrees(p.Annotations, q.Annotations), true

	case *java.MethodInvocation:
		q := candidate.(*java.MethodInvocation)
		return c.matchRightPadded(p.Select, q.Select) &&
			c.matchContainer(p.TypeParameters, q.TypeParameters) &&
			c.matchNode(p.Name, q.Name) &&
			matchList(c, p.Arguments.Elements, q.Arguments.Elements) &&
			c.matchTypeSlot(typeOrNil(p.MethodType), typeOrNil(q.MethodType)), true

	case *java.FieldAccess:
		q := candidate.(*java.FieldAccess)
		return c.matchNode(p.Target, q.Target) &&
			c.matchNode(p.Name.Element, q.Name.Element) &&
			c.matchTypeSlot(p.Type, q.Type), true

	case *java.Binary:
		q := candidate.(*java.Binary)
		return p.Operator.Element == q.Operator.Element &&
			c.matchNode(p.Left, q.Left) &&
			c.matchNode(p.Right, q.Right) &&
			c.matchTypeSlot(p.Type, q.Type), true

	case *java.Block:
		q := candidate.(*java.Block)
		return matchList(c, p.Statements, q.Statements), true
	}
	return false, false
}

// A typed nil in a JavaType slot is an absent type, so it reaches the
// comparison as an untyped one and two of them are equal.
func typeOrNil[T interface {
	comparable
	java.JavaType
}](t T) java.JavaType {
	var absent T
	if t == absent {
		return nil
	}
	return t
}

func (c *patternComparator) matchTrees(pattern, candidate []java.Tree) bool {
	if len(pattern) != len(candidate) {
		return false
	}
	for i := range pattern {
		p, _ := pattern[i].(java.J)
		q, _ := candidate[i].(java.J)
		if !c.matchNode(p, q) {
			return false
		}
	}
	return true
}

func (c *patternComparator) matchRightPadded(pattern, candidate *java.RightPadded[java.Expression]) bool {
	if pattern == nil || candidate == nil {
		return pattern == nil && candidate == nil
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchContainer(pattern, candidate *java.Container[java.Expression]) bool {
	if pattern == nil || candidate == nil {
		return pattern == nil && candidate == nil
	}
	return matchList(c, pattern.Elements, candidate.Elements)
}

// matchList is matchRun over a typed list. Routing it through the reflected
// one costs the hand-written comparisons the speed they exist for, so the two
// are held together by TestFastPathAgreesWithWalkOnVariadicRuns instead.
func matchList[T java.J](c *patternComparator, pattern, candidate []java.RightPadded[T]) bool {
	at, ok := variadicIndex(c, pattern)
	if !ok {
		return false
	}
	if at < 0 {
		return len(pattern) == len(candidate) && matchElements(c, pattern, candidate)
	}

	run := len(candidate) - (len(pattern) - 1)
	// A bound on the slice arithmetic below. allowsCount rejects a
	// negative run first, a capture's minimum never being negative.
	if run < 0 {
		return false
	}
	name, _ := placeholderName(java.J(pattern[at].Element))
	if !c.captures[name].allowsCount(run) {
		return false
	}
	if !matchElements(c, pattern[:at], candidate[:at]) {
		return false
	}
	if !matchElements(c, pattern[at+1:], candidate[at+run:]) {
		return false
	}
	return c.bindRun(name, unwrap(candidate[at:at+run]))
}

func matchElements[T java.J](c *patternComparator, pattern, candidate []java.RightPadded[T]) bool {
	for i := range pattern {
		if !c.matchNode(pattern[i].Element, candidate[i].Element) {
			return false
		}
	}
	return true
}

// variadicIndex returns the position of the pattern's variadic capture, -1 when
// it has none, and ok=false when it has more than one.
func variadicIndex[T java.J](c *patternComparator, pattern []java.RightPadded[T]) (int, bool) {
	if !c.variadic {
		return -1, true
	}
	at := -1
	for i := range pattern {
		name, ok := placeholderName(java.J(pattern[i].Element))
		if !ok || !c.isVariadic(name) {
			continue
		}
		if at >= 0 {
			return 0, false
		}
		at = i
	}
	return at, true
}

func unwrap[T java.J](padded []java.RightPadded[T]) []java.J {
	elements := java.UnwrapRightPadded(padded)
	values := make([]java.J, len(elements))
	for i, e := range elements {
		values[i] = e
	}
	return values
}

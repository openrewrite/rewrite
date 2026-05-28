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
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// SpacesVisitor enforces gofmt's intra-line spacing rules:
//
//   - One space around binary operators (`a + b`, not `a+b`).
//   - No space around unary operators (`!x`, `-y`).
//   - One space after commas in argument/parameter lists
//     (RightPadded.After fields don't get touched here — they precede
//     the comma, not follow it).
//   - One space around `=` and `:=` (Assignment / VariableDeclarator).
//
// Non-trivial whitespace (containing newlines) is preserved on the
// theory that it was authored deliberately. SpacesVisitor only fixes
// "0 spaces" and "2+ spaces" in single-line contexts; multi-line
// expressions are left to the author and the indent visitor.
type SpacesVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
}

// NewSpacesVisitor returns a visitor configured with the given
// stopAfter bound. Pass nil to format the entire visited tree.
func NewSpacesVisitor(stopAfter tree.Tree) *SpacesVisitor {
	return visitor.Init(&SpacesVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
	})
}

func (v *SpacesVisitor) Visit(t tree.Tree, p any) tree.Tree {
	if v.shouldHalt() {
		return t
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

func (v *SpacesVisitor) VisitBinary(bin *tree.Binary, p any) tree.J {
	bin = v.GoVisitor.VisitBinary(bin, p).(*tree.Binary)
	bin.Operator.Before = ensureSingleSpace(bin.Operator.Before)
	bin = bin.WithRight(ensureLeadingSingleSpace(bin.Right))
	return bin
}

func (v *SpacesVisitor) VisitAssignment(a *tree.Assignment, p any) tree.J {
	a = v.GoVisitor.VisitAssignment(a, p).(*tree.Assignment)
	a.Value.Before = ensureSingleSpace(a.Value.Before)
	a.Value.Element = ensureLeadingSingleSpace(a.Value.Element)
	return a
}

func (v *SpacesVisitor) VisitAssignmentOperation(ao *tree.AssignmentOperation, p any) tree.J {
	ao = v.GoVisitor.VisitAssignmentOperation(ao, p).(*tree.AssignmentOperation)
	ao.Operator.Before = ensureSingleSpace(ao.Operator.Before)
	ao.Assignment = ensureLeadingSingleSpace(ao.Assignment)
	return ao
}

func (v *SpacesVisitor) VisitUnary(u *tree.Unary, p any) tree.J {
	u = v.GoVisitor.VisitUnary(u, p).(*tree.Unary)
	u.Operand = clearExpressionLeadingSpace(u.Operand)
	return u
}

// ensureSingleSpace returns the space unchanged if it contains a
// newline (deliberate multi-line layout), otherwise normalizes any
// 0-or-many-spaces to exactly one space.
func ensureSingleSpace(s tree.Space) tree.Space {
	if strings.Contains(s.Whitespace, "\n") {
		return s
	}
	if s.Whitespace == " " {
		return s
	}
	s.Whitespace = " "
	return s
}

// ensureLeadingSingleSpace ensures the leftmost leaf of an Expression
// has a single-space Prefix. Walks the leftmost spine via
// transformLeftmostPrefix so the rule works whether the leading
// whitespace lives directly on the expression (Identifier, Literal,
// Unary, Parentheses) or on a deeper leftmost descendant (Binary's
// Left, FieldAccess's Target, etc.).
func ensureLeadingSingleSpace(e tree.Expression) tree.Expression {
	if e == nil {
		return e
	}
	out := transformLeftmostPrefix(e, ensureSingleSpace)
	if r, ok := out.(tree.Expression); ok {
		return r
	}
	return e
}

func clearExpressionLeadingSpace(e tree.Expression) tree.Expression {
	if e == nil {
		return e
	}
	prefix := getPrefix(e)
	if strings.Contains(prefix.Whitespace, "\n") {
		return e
	}
	if prefix.Whitespace == "" {
		return e
	}
	prefix.Whitespace = ""
	return withPrefix(e, prefix)
}

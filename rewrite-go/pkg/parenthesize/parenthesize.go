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

// Package parenthesize adds the parentheses an expression needs to keep its
// grouping when it sits somewhere that binds tighter than it does. A recipe
// that swaps one expression for another changes how the expression around it
// groups unless it accounts for precedence: replacing `f()` with `a + b` in
// `c * f()` yields `c * a + b`, which multiplies a different thing.
//
// Mirrors org.openrewrite.java.ParenthesizeVisitor.
package parenthesize

import (
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Maybe returns e wrapped in parentheses when the position named by site binds
// tighter than e does, and e unchanged otherwise. site is the node e replaces,
// which supplies both the enclosing expression and the slot e lands in.
func Maybe(e java.Expression, site *visitor.Cursor) java.Expression {
	if !Needed(e, site) {
		return e
	}
	return Wrap(e)
}

// Wrap puts e in parentheses, moving e's own leading whitespace out onto them
// so the result leads the same way e did.
func Wrap(e java.Expression) java.Expression {
	inner, ok := format.WithPrefix(e, java.Space{}).(java.Expression)
	if !ok {
		return e
	}
	return &java.Parentheses{
		ID:     uuid.New(),
		Prefix: format.PrefixOf(e),
		Tree:   java.RightPadded[java.Expression]{Element: inner},
	}
}

// Needed reports whether e would regroup the expression around it if it were
// dropped in at site unparenthesized.
func Needed(e java.Expression, site *visitor.Cursor) bool {
	prec, _, _, isBinary := format.BinaryOperands(e)
	if !isBinary || site == nil || site.Parent() == nil {
		return false
	}
	replaced := site.Value()
	switch parent := site.Parent().Value().(type) {
	case *java.Unary, *golang.Unary:
		return true
	// A primary expression is what these apply to, so an operator inside one
	// has to be grouped away from it.
	case *java.FieldAccess, *java.ArrayAccess, *golang.TypeAssertion:
		return true
	case *java.MethodInvocation:
		return parent.Select != nil && any(parent.Select.Element) == any(replaced)
	default:
		enclosing, isExpr := site.Parent().Value().(java.Expression)
		if !isExpr {
			return false
		}
		outer, left, right, ok := format.BinaryOperands(enclosing)
		if !ok {
			return false
		}
		// Go's binary operators are left-associative, so an equally binding
		// right operand regroups where a left one does not.
		if any(right) == any(replaced) {
			return prec <= outer
		}
		if any(left) == any(replaced) {
			return prec < outer
		}
		return false
	}
}

// Visitor parenthesizes every expression in the tree it visits that would
// otherwise regroup, which is what a recipe wants after synthesizing a tree
// from parts rather than replacing a single node.
type Visitor struct {
	visitor.GoVisitor
}

func NewVisitor() *Visitor {
	return visitor.Init(&Visitor{})
}

func (v *Visitor) Visit(t java.Tree, p any) java.Tree {
	out := v.GoVisitor.Visit(t, p)
	expr, ok := out.(java.Expression)
	if !ok {
		return out
	}
	// The cursor has popped back to the parent by now, and the parent still
	// holds t, so t is the node out is replacing.
	if Needed(expr, visitor.NewCursor(v.Cursor(), t)) {
		return Wrap(expr)
	}
	return out
}

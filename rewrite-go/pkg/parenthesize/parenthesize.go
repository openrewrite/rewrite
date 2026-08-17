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
	if site == nil || site.Parent() == nil {
		return false
	}
	if bracesReadAsABlock(e, site) {
		return true
	}
	replaced := site.Value()
	switch parent := site.Parent().Value().(type) {
	case *java.Unary, *golang.Unary:
		// Two operators in a row run together into one token.
		return needsDelimiting(e)
	// Each of these applies to a primary expression, so anything else in that
	// one slot has to be grouped away from it. The slots beside it — an index,
	// a slice bound, an asserted type — are delimited already.
	case *java.FieldAccess:
		return any(parent.Target) == any(replaced) && needsDelimiting(e)
	case *java.ArrayAccess:
		return any(parent.Indexed) == any(replaced) && needsDelimiting(e)
	case *golang.Slice:
		return any(parent.Indexed) == any(replaced) && needsDelimiting(e)
	case *golang.TypeAssertion:
		return any(parent.Left.Element) == any(replaced) && needsDelimiting(e)
	// The callee slot holds the type a conversion names, which is subject to
	// the same rule.
	case *java.MethodInvocation:
		return parent.Select != nil && any(parent.Select.Element) == any(replaced) && needsDelimiting(e)
	default:
		prec, _, _, isBinary := format.BinaryOperands(e)
		if !isBinary {
			return false
		}
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

// needsDelimiting reports whether e is a form Go's grammar does not admit where
// a primary expression belongs: anything built from operators, and the pointer,
// channel and func type spellings, which a conversion names in that position.
// Slice, array and map types open with a bracket and read unambiguously.
func needsDelimiting(e java.Expression) bool {
	switch e.(type) {
	case *java.Binary, *golang.Binary, *java.Unary, *golang.Unary, *java.TypeCast:
		return true
	case *golang.Channel, *golang.FuncType:
		return true
	}
	return false
}

// bracesReadAsABlock reports whether e is a composite literal written with a
// bare type name, sitting between a control-clause keyword and its block. Go's
// parser takes the literal's opening brace for the block's, so the literal has
// to be delimited.
func bracesReadAsABlock(e java.Expression, site *visitor.Cursor) bool {
	composite, ok := e.(*golang.Composite)
	if !ok || !isBareTypeName(composite.TypeExpr) {
		return false
	}
	child := java.Tree(site.Value())
	for c := site.Parent(); c != nil; c = c.Parent() {
		switch parent := c.Value().(type) {
		case *java.If, *java.Switch, *java.ForLoop, *java.ForEachLoop, *golang.StatementWithInit:
			return true
		// Anything already delimited settles where the brace belongs. A block
		// is the clause's own body, which the keyword is long done with.
		case *java.Parentheses, *golang.Composite, *java.Block, *java.Case, *golang.CommClause:
			return false
		case *java.MethodInvocation:
			if parent.Select == nil || any(parent.Select.Element) != any(child) {
				return false
			}
		case *java.ArrayAccess:
			if any(parent.Indexed) != any(child) {
				return false
			}
		}
		child = c.Value()
	}
	return false
}

// isBareTypeName reports whether a composite literal's type is spelled as a
// name, which is the spelling that collides with a block. One written from a
// slice, map or array type opens with a bracket instead.
func isBareTypeName(typeExpr java.Expression) bool {
	switch typeExpr.(type) {
	case *java.Identifier, *java.FieldAccess:
		return true
	}
	return false
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

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
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// placeAt fits result into the site it replaces. site is the replaced node.
func placeAt(result java.J, site *visitor.Cursor) java.J {
	replaced, ok := site.Value().(java.J)
	if !ok {
		return result
	}
	out := parenthesize(setLeadingPrefix(result, getLeadingPrefix(replaced)), site)
	if formatted, ok := format.AutoFormat(out, nil, nil, site.Parent()).(java.J); ok {
		return formatted
	}
	return out
}

// parenthesize wraps e in parentheses when the position it lands in binds
// tighter than e does, which is what keeps a substitution from changing how the
// surrounding expression groups. site is the node e replaces.
func parenthesize(e java.J, site *visitor.Cursor) java.J {
	expr, ok := e.(java.Expression)
	if !ok || !needsParens(expr, site) {
		return e
	}
	inner := setLeadingPrefix(expr, java.Space{})
	return &java.Parentheses{
		ID:     uuid.New(),
		Prefix: getLeadingPrefix(expr),
		Tree:   java.RightPadded[java.Expression]{Element: inner.(java.Expression)},
	}
}

func needsParens(e java.Expression, site *visitor.Cursor) bool {
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

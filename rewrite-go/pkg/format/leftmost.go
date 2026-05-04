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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// transformLeftmostPrefix walks the leftmost-spine of t and applies f
// to the deepest leaf's Prefix. The "leftmost spine" follows the child
// whose source position is leftmost — Binary's Left, Assignment's
// Variable, FieldAccess's Target, ArrayAccess's Indexed,
// MethodInvocation's Select (or Name when Select is nil). Types whose
// source layout puts the operator/keyword first (Unary, Parentheses,
// TypeCast) carry their leading whitespace on their own Prefix and are
// treated as leaves.
//
// The parser places inter-statement / inter-expression whitespace on
// the leftmost descendant rather than on the enclosing node. Format
// passes that need to manipulate that whitespace (BlankLines'
// block-start strip, Spaces' "single space after `:=`" with a Binary
// operand) reach for it through this helper instead of guessing where
// the parser put it.
func transformLeftmostPrefix(t tree.Tree, f func(tree.Space) tree.Space) tree.Tree {
	if t == nil {
		return nil
	}
	switch n := t.(type) {
	case *tree.Binary:
		if n.Left == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Left, f).(tree.Expression); ok {
			c := *n
			c.Left = updated
			return &c
		}
	case *tree.Assignment:
		if n.Variable == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Variable, f).(tree.Expression); ok {
			c := *n
			c.Variable = updated
			return &c
		}
	case *tree.AssignmentOperation:
		if n.Variable == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Variable, f).(tree.Expression); ok {
			c := *n
			c.Variable = updated
			return &c
		}
	case *tree.FieldAccess:
		if n.Target == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Target, f).(tree.Expression); ok {
			c := *n
			c.Target = updated
			return &c
		}
	case *tree.MethodInvocation:
		c := *n
		if n.Select != nil && n.Select.Element != nil {
			if updated, ok := transformLeftmostPrefix(n.Select.Element, f).(tree.Expression); ok {
				sp := *n.Select
				sp.Element = updated
				c.Select = &sp
				return &c
			}
		} else if n.Name != nil {
			if updated, ok := transformLeftmostPrefix(n.Name, f).(*tree.Identifier); ok {
				c.Name = updated
				return &c
			}
		}
	case *tree.ArrayAccess:
		if n.Indexed == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Indexed, f).(tree.Expression); ok {
			c := *n
			c.Indexed = updated
			return &c
		}
	case *tree.StatementExpression:
		if n.Statement == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Statement, f).(tree.Statement); ok {
			c := *n
			c.Statement = updated
			return &c
		}
	case *tree.Composite:
		if n.TypeExpr == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.TypeExpr, f).(tree.Expression); ok {
			c := *n
			c.TypeExpr = updated
			return &c
		}
	case *tree.ParameterizedType:
		if n.Clazz == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Clazz, f).(tree.Expression); ok {
			c := *n
			c.Clazz = updated
			return &c
		}
	case *tree.IndexList:
		if n.Target == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Target, f).(tree.Expression); ok {
			c := *n
			c.Target = updated
			return &c
		}
	case *tree.Slice:
		if n.Indexed == nil {
			break
		}
		if updated, ok := transformLeftmostPrefix(n.Indexed, f).(tree.Expression); ok {
			c := *n
			c.Indexed = updated
			return &c
		}
	}
	// Leaf — apply f to this node's own Prefix.
	cur := getPrefix(t)
	next := f(cur)
	if next.Whitespace == cur.Whitespace && len(next.Comments) == len(cur.Comments) {
		return t
	}
	return withPrefix(t, next)
}

// setLeftmostPrefix is the "set" form of transformLeftmostPrefix: it
// replaces the leftmost leaf's Prefix with prefix unconditionally.
func setLeftmostPrefix(t tree.Tree, prefix tree.Space) tree.Tree {
	return transformLeftmostPrefix(t, func(_ tree.Space) tree.Space { return prefix })
}

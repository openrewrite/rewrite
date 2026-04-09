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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
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
func (c *patternComparator) match(pattern, candidate tree.J) *MatchResult {
	if c.matchNode(pattern, candidate) {
		return c.result
	}
	return nil
}

// matchNode compares two nodes structurally, handling placeholder binding.
func (c *patternComparator) matchNode(pattern, candidate tree.J) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}

	// Check if the pattern node is a placeholder identifier.
	if ident, ok := pattern.(*tree.Identifier); ok {
		if name, isPlaceholder := FromPlaceholder(ident.Name); isPlaceholder {
			return c.bindCapture(name, candidate)
		}
	}

	// Both nodes must be the same concrete type.
	if reflect.TypeOf(pattern) != reflect.TypeOf(candidate) {
		return false
	}

	return c.matchProperties(pattern, candidate)
}

// bindCapture binds a captured value, checking for repeated captures
// (which enforce structural equality).
func (c *patternComparator) bindCapture(name string, candidate tree.J) bool {
	if c.result.Has(name) {
		// Repeated capture: enforce structural equality with prior binding.
		prev := c.result.Get(name)
		return structurallyEqual(prev, candidate)
	}
	c.result.bind(name, candidate)
	return true
}

// matchProperties dispatches to type-specific comparison.
func (c *patternComparator) matchProperties(pattern, candidate tree.J) bool {
	switch p := pattern.(type) {
	case *tree.Identifier:
		cand := candidate.(*tree.Identifier)
		return p.Name == cand.Name
	case *tree.Literal:
		cand := candidate.(*tree.Literal)
		return p.Source == cand.Source
	case *tree.Binary:
		cand := candidate.(*tree.Binary)
		return c.matchNode(p.Left, cand.Left) &&
			p.Operator.Element == cand.Operator.Element &&
			c.matchNode(p.Right, cand.Right)
	case *tree.Unary:
		cand := candidate.(*tree.Unary)
		return p.Operator.Element == cand.Operator.Element &&
			c.matchNode(p.Operand, cand.Operand)
	case *tree.FieldAccess:
		cand := candidate.(*tree.FieldAccess)
		return c.matchNode(p.Target, cand.Target) &&
			c.matchNode(p.Name.Element, cand.Name.Element)
	case *tree.MethodInvocation:
		cand := candidate.(*tree.MethodInvocation)
		if !c.matchOptionalRightPadded(p.Select, cand.Select) {
			return false
		}
		if !c.matchNode(p.Name, cand.Name) {
			return false
		}
		return c.matchExpressionList(p.Arguments.Elements, cand.Arguments.Elements)
	case *tree.Assignment:
		cand := candidate.(*tree.Assignment)
		return c.matchNode(p.Variable, cand.Variable) &&
			c.matchNode(p.Value.Element, cand.Value.Element)
	case *tree.AssignmentOperation:
		cand := candidate.(*tree.AssignmentOperation)
		return c.matchNode(p.Variable, cand.Variable) &&
			p.Operator.Element == cand.Operator.Element &&
			c.matchNode(p.Assignment, cand.Assignment)
	case *tree.Block:
		cand := candidate.(*tree.Block)
		return c.matchStatementList(p.Statements, cand.Statements)
	case *tree.Return:
		cand := candidate.(*tree.Return)
		return c.matchExpressionRightPaddedList(p.Expressions, cand.Expressions)
	case *tree.If:
		cand := candidate.(*tree.If)
		if !c.matchOptionalRightPaddedStmt(p.Init, cand.Init) {
			return false
		}
		if !c.matchNode(p.Condition, cand.Condition) {
			return false
		}
		if !c.matchNode(p.Then, cand.Then) {
			return false
		}
		return c.matchOptionalRightPaddedJ(p.ElsePart, cand.ElsePart)
	case *tree.Else:
		cand := candidate.(*tree.Else)
		return c.matchNode(cand.Body.Element, p.Body.Element)
	case *tree.MethodDeclaration:
		cand := candidate.(*tree.MethodDeclaration)
		if !c.matchNode(p.Name, cand.Name) {
			return false
		}
		return c.matchOptionalNode(p.Body, cand.Body)
	case *tree.VariableDeclarations:
		cand := candidate.(*tree.VariableDeclarations)
		if !c.matchOptionalExpression(p.TypeExpr, cand.TypeExpr) {
			return false
		}
		if len(p.Variables) != len(cand.Variables) {
			return false
		}
		for i := range p.Variables {
			if !c.matchNode(p.Variables[i].Element, cand.Variables[i].Element) {
				return false
			}
		}
		return true
	case *tree.VariableDeclarator:
		cand := candidate.(*tree.VariableDeclarator)
		if !c.matchNode(p.Name, cand.Name) {
			return false
		}
		if p.Initializer == nil && cand.Initializer == nil {
			return true
		}
		if p.Initializer == nil || cand.Initializer == nil {
			return false
		}
		return c.matchNode(p.Initializer.Element, cand.Initializer.Element)
	case *tree.Parentheses:
		cand := candidate.(*tree.Parentheses)
		return c.matchNode(p.Tree.Element, cand.Tree.Element)
	case *tree.TypeCast:
		cand := candidate.(*tree.TypeCast)
		return c.matchNode(p.Clazz, cand.Clazz) &&
			c.matchNode(p.Expr, cand.Expr)
	case *tree.ControlParentheses:
		cand := candidate.(*tree.ControlParentheses)
		return c.matchNode(p.Tree.Element, cand.Tree.Element)
	case *tree.ArrayAccess:
		cand := candidate.(*tree.ArrayAccess)
		return c.matchNode(p.Indexed, cand.Indexed) &&
			c.matchNode(p.Dimension, cand.Dimension)
	case *tree.ArrayDimension:
		cand := candidate.(*tree.ArrayDimension)
		return c.matchNode(p.Index.Element, cand.Index.Element)
	case *tree.ArrayType:
		cand := candidate.(*tree.ArrayType)
		if !c.matchOptionalExpression(p.Length, cand.Length) {
			return false
		}
		return c.matchNode(p.ElementType, cand.ElementType)
	case *tree.Import:
		cand := candidate.(*tree.Import)
		return c.matchNode(p.Qualid, cand.Qualid)
	case *tree.Empty:
		return true
	case *tree.ForLoop:
		cand := candidate.(*tree.ForLoop)
		return c.matchNode(&p.Control, &cand.Control) &&
			c.matchNode(p.Body, cand.Body)
	case *tree.ForControl:
		cand := candidate.(*tree.ForControl)
		if !c.matchOptionalRightPaddedStmt(p.Init, cand.Init) {
			return false
		}
		if !c.matchOptionalRightPaddedExpr(p.Condition, cand.Condition) {
			return false
		}
		return c.matchOptionalRightPaddedStmt(p.Update, cand.Update)
	case *tree.ForEachLoop:
		cand := candidate.(*tree.ForEachLoop)
		return c.matchNode(&p.Control, &cand.Control) &&
			c.matchNode(p.Body, cand.Body)
	case *tree.ForEachControl:
		cand := candidate.(*tree.ForEachControl)
		if !c.matchOptionalRightPaddedExpr2(p.Key, cand.Key) {
			return false
		}
		if !c.matchOptionalRightPaddedExpr2(p.Value, cand.Value) {
			return false
		}
		return c.matchNode(p.Iterable, cand.Iterable)
	case *tree.Switch:
		cand := candidate.(*tree.Switch)
		if !c.matchOptionalRightPaddedStmt(p.Init, cand.Init) {
			return false
		}
		if !c.matchOptionalRightPaddedExpr(p.Tag, cand.Tag) {
			return false
		}
		return c.matchNode(p.Body, cand.Body)
	case *tree.Case:
		cand := candidate.(*tree.Case)
		if !c.matchExpressionList(p.Expressions.Elements, cand.Expressions.Elements) {
			return false
		}
		return c.matchStatementList(p.Body, cand.Body)
	case *tree.Break:
		cand := candidate.(*tree.Break)
		return c.matchOptionalNode(p.Label, cand.Label)
	case *tree.Continue:
		cand := candidate.(*tree.Continue)
		return c.matchOptionalNode(p.Label, cand.Label)
	case *tree.Label:
		cand := candidate.(*tree.Label)
		return c.matchNode(p.Name.Element, cand.Name.Element) &&
			c.matchNode(p.Statement, cand.Statement)

	// Go-specific nodes
	case *tree.GoStmt:
		cand := candidate.(*tree.GoStmt)
		return c.matchNode(p.Expr, cand.Expr)
	case *tree.Defer:
		cand := candidate.(*tree.Defer)
		return c.matchNode(p.Expr, cand.Expr)
	case *tree.Send:
		cand := candidate.(*tree.Send)
		return c.matchNode(p.Channel, cand.Channel) &&
			c.matchNode(p.Arrow.Element, cand.Arrow.Element)
	case *tree.Goto:
		cand := candidate.(*tree.Goto)
		return c.matchNode(p.Label, cand.Label)
	case *tree.Fallthrough:
		return true
	case *tree.Composite:
		cand := candidate.(*tree.Composite)
		if !c.matchOptionalExpression(p.TypeExpr, cand.TypeExpr) {
			return false
		}
		return c.matchExpressionList(p.Elements.Elements, cand.Elements.Elements)
	case *tree.KeyValue:
		cand := candidate.(*tree.KeyValue)
		return c.matchNode(p.Key, cand.Key) &&
			c.matchNode(p.Value.Element, cand.Value.Element)
	case *tree.Slice:
		cand := candidate.(*tree.Slice)
		if !c.matchNode(p.Indexed, cand.Indexed) {
			return false
		}
		if !c.matchNode(p.Low.Element, cand.Low.Element) {
			return false
		}
		if !c.matchNode(p.High.Element, cand.High.Element) {
			return false
		}
		return c.matchOptionalExpression(p.Max, cand.Max)
	case *tree.MapType:
		cand := candidate.(*tree.MapType)
		return c.matchNode(p.Key.Element, cand.Key.Element) &&
			c.matchNode(p.Value, cand.Value)
	case *tree.Channel:
		cand := candidate.(*tree.Channel)
		return p.Dir == cand.Dir &&
			c.matchNode(p.Value, cand.Value)
	case *tree.FuncType:
		cand := candidate.(*tree.FuncType)
		if !c.matchStatementContainer(p.Parameters, cand.Parameters) {
			return false
		}
		return c.matchOptionalExpression(p.ReturnType, cand.ReturnType)
	case *tree.StructType:
		cand := candidate.(*tree.StructType)
		return c.matchOptionalNode(p.Body, cand.Body)
	case *tree.InterfaceType:
		cand := candidate.(*tree.InterfaceType)
		return c.matchOptionalNode(p.Body, cand.Body)
	case *tree.TypeList:
		cand := candidate.(*tree.TypeList)
		return c.matchStatementContainer(p.Types, cand.Types)
	case *tree.TypeDecl:
		cand := candidate.(*tree.TypeDecl)
		if !c.matchNode(p.Name, cand.Name) {
			return false
		}
		return c.matchOptionalExpression(p.Definition, cand.Definition)
	case *tree.MultiAssignment:
		cand := candidate.(*tree.MultiAssignment)
		if !c.matchExpressionRightPaddedList(p.Variables, cand.Variables) {
			return false
		}
		return c.matchExpressionRightPaddedList(p.Values, cand.Values)
	case *tree.CommClause:
		cand := candidate.(*tree.CommClause)
		if !c.matchOptionalStatement(p.Comm, cand.Comm) {
			return false
		}
		return c.matchStatementList(p.Body, cand.Body)
	case *tree.IndexList:
		cand := candidate.(*tree.IndexList)
		if !c.matchNode(p.Target, cand.Target) {
			return false
		}
		return c.matchExpressionList(p.Indices.Elements, cand.Indices.Elements)
	case *tree.CompilationUnit:
		cand := candidate.(*tree.CompilationUnit)
		return c.matchStatementList(p.Statements, cand.Statements)
	default:
		// Unknown node type — fail the match.
		return false
	}
}

// --- Helper methods for optional and list comparisons ---

func (c *patternComparator) matchOptionalNode(pattern, candidate tree.J) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern, candidate)
}

func (c *patternComparator) matchOptionalExpression(pattern, candidate tree.Expression) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern, candidate)
}

func (c *patternComparator) matchOptionalStatement(pattern, candidate tree.Statement) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern, candidate)
}

func (c *patternComparator) matchOptionalRightPadded(pattern, candidate *tree.RightPadded[tree.Expression]) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchOptionalRightPaddedStmt(pattern, candidate *tree.RightPadded[tree.Statement]) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchOptionalRightPaddedExpr(pattern, candidate *tree.RightPadded[tree.Expression]) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchOptionalRightPaddedExpr2(pattern, candidate *tree.RightPadded[tree.Expression]) bool {
	return c.matchOptionalRightPaddedExpr(pattern, candidate)
}

func (c *patternComparator) matchOptionalRightPaddedJ(pattern, candidate *tree.RightPadded[tree.J]) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchExpressionList(pattern, candidate []tree.RightPadded[tree.Expression]) bool {
	if len(pattern) != len(candidate) {
		return false
	}
	for i := range pattern {
		if !c.matchNode(pattern[i].Element, candidate[i].Element) {
			return false
		}
	}
	return true
}

func (c *patternComparator) matchExpressionRightPaddedList(pattern, candidate []tree.RightPadded[tree.Expression]) bool {
	return c.matchExpressionList(pattern, candidate)
}

func (c *patternComparator) matchStatementList(pattern, candidate []tree.RightPadded[tree.Statement]) bool {
	if len(pattern) != len(candidate) {
		return false
	}
	for i := range pattern {
		if !c.matchNode(pattern[i].Element, candidate[i].Element) {
			return false
		}
	}
	return true
}

func (c *patternComparator) matchStatementContainer(pattern, candidate tree.Container[tree.Statement]) bool {
	return c.matchStatementList(pattern.Elements, candidate.Elements)
}

// structurallyEqual checks if two nodes are structurally equivalent
// (ignoring whitespace). Used for repeated captures.
func structurallyEqual(a, b tree.J) bool {
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

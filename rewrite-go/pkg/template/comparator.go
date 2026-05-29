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

// matchNode compares two nodes structurally, handling placeholder binding.
func (c *patternComparator) matchNode(pattern, candidate java.J) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}

	// Check if the pattern node is a placeholder identifier.
	if ident, ok := pattern.(*java.Identifier); ok {
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
func (c *patternComparator) bindCapture(name string, candidate java.J) bool {
	if c.result.Has(name) {
		// Repeated capture: enforce structural equality with prior binding.
		prev := c.result.Get(name)
		return structurallyEqual(prev, candidate)
	}
	c.result.bind(name, candidate)
	return true
}

// matchProperties dispatches to type-specific comparison.
func (c *patternComparator) matchProperties(pattern, candidate java.J) bool {
	switch p := pattern.(type) {
	case *java.Identifier:
		cand := candidate.(*java.Identifier)
		return p.Name == cand.Name
	case *java.Literal:
		cand := candidate.(*java.Literal)
		return p.Source == cand.Source
	case *java.Binary:
		cand := candidate.(*java.Binary)
		return c.matchNode(p.Left, cand.Left) &&
			p.Operator.Element == cand.Operator.Element &&
			c.matchNode(p.Right, cand.Right)
	case *java.Unary:
		cand := candidate.(*java.Unary)
		return p.Operator.Element == cand.Operator.Element &&
			c.matchNode(p.Operand, cand.Operand)
	case *java.FieldAccess:
		cand := candidate.(*java.FieldAccess)
		return c.matchNode(p.Target, cand.Target) &&
			c.matchNode(p.Name.Element, cand.Name.Element)
	case *java.MethodInvocation:
		cand := candidate.(*java.MethodInvocation)
		if !c.matchOptionalRightPadded(p.Select, cand.Select) {
			return false
		}
		if !c.matchNode(p.Name, cand.Name) {
			return false
		}
		return c.matchExpressionList(p.Arguments.Elements, cand.Arguments.Elements)
	case *java.Assignment:
		cand := candidate.(*java.Assignment)
		return c.matchNode(p.Variable, cand.Variable) &&
			c.matchNode(p.Value.Element, cand.Value.Element)
	case *java.AssignmentOperation:
		cand := candidate.(*java.AssignmentOperation)
		return c.matchNode(p.Variable, cand.Variable) &&
			p.Operator.Element == cand.Operator.Element &&
			c.matchNode(p.Assignment, cand.Assignment)
	case *java.Block:
		cand := candidate.(*java.Block)
		return c.matchStatementList(p.Statements, cand.Statements)
	case *java.Return:
		cand := candidate.(*java.Return)
		return c.matchExpressionRightPaddedList(p.Expressions, cand.Expressions)
	case *java.If:
		cand := candidate.(*java.If)
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
	case *java.Else:
		cand := candidate.(*java.Else)
		return c.matchNode(cand.Body.Element, p.Body.Element)
	case *java.MethodDeclaration:
		cand := candidate.(*java.MethodDeclaration)
		if !c.matchNode(p.Name, cand.Name) {
			return false
		}
		return c.matchOptionalNode(p.Body, cand.Body)
	case *java.VariableDeclarations:
		cand := candidate.(*java.VariableDeclarations)
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
	case *java.VariableDeclarator:
		cand := candidate.(*java.VariableDeclarator)
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
	case *java.Parentheses:
		cand := candidate.(*java.Parentheses)
		return c.matchNode(p.Tree.Element, cand.Tree.Element)
	case *java.TypeCast:
		cand := candidate.(*java.TypeCast)
		return c.matchNode(p.Clazz, cand.Clazz) &&
			c.matchNode(p.Expr, cand.Expr)
	case *java.ControlParentheses:
		cand := candidate.(*java.ControlParentheses)
		return c.matchNode(p.Tree.Element, cand.Tree.Element)
	case *java.ArrayAccess:
		cand := candidate.(*java.ArrayAccess)
		return c.matchNode(p.Indexed, cand.Indexed) &&
			c.matchNode(p.Dimension, cand.Dimension)
	case *java.ArrayDimension:
		cand := candidate.(*java.ArrayDimension)
		return c.matchNode(p.Index.Element, cand.Index.Element)
	case *java.ArrayType:
		cand := candidate.(*java.ArrayType)
		if !c.matchOptionalExpression(p.Length, cand.Length) {
			return false
		}
		return c.matchNode(p.ElementType, cand.ElementType)
	case *java.Import:
		cand := candidate.(*java.Import)
		return c.matchNode(p.Qualid, cand.Qualid)
	case *java.Empty:
		return true
	case *java.ForLoop:
		cand := candidate.(*java.ForLoop)
		return c.matchNode(&p.Control, &cand.Control) &&
			c.matchNode(p.Body, cand.Body)
	case *java.ForControl:
		cand := candidate.(*java.ForControl)
		if !c.matchOptionalRightPaddedStmt(p.Init, cand.Init) {
			return false
		}
		if !c.matchOptionalRightPaddedExpr(p.Condition, cand.Condition) {
			return false
		}
		return c.matchOptionalRightPaddedStmt(p.Update, cand.Update)
	case *java.ForEachLoop:
		cand := candidate.(*java.ForEachLoop)
		return c.matchNode(&p.Control, &cand.Control) &&
			c.matchNode(p.Body, cand.Body)
	case *java.ForEachControl:
		cand := candidate.(*java.ForEachControl)
		if !c.matchOptionalRightPaddedExpr2(p.Key, cand.Key) {
			return false
		}
		if !c.matchOptionalRightPaddedExpr2(p.Value, cand.Value) {
			return false
		}
		return c.matchNode(p.Iterable, cand.Iterable)
	case *java.Switch:
		cand := candidate.(*java.Switch)
		if !c.matchOptionalRightPaddedStmt(p.Init, cand.Init) {
			return false
		}
		if !c.matchOptionalRightPaddedExpr(p.Tag, cand.Tag) {
			return false
		}
		return c.matchNode(p.Body, cand.Body)
	case *java.Case:
		cand := candidate.(*java.Case)
		if !c.matchExpressionList(p.Expressions.Elements, cand.Expressions.Elements) {
			return false
		}
		return c.matchStatementList(p.Body, cand.Body)
	case *java.Break:
		cand := candidate.(*java.Break)
		return c.matchOptionalNode(p.Label, cand.Label)
	case *java.Continue:
		cand := candidate.(*java.Continue)
		return c.matchOptionalNode(p.Label, cand.Label)
	case *java.Label:
		cand := candidate.(*java.Label)
		return c.matchNode(p.Name.Element, cand.Name.Element) &&
			c.matchNode(p.Statement, cand.Statement)

	// Go-specific nodes
	case *golang.GoStmt:
		cand := candidate.(*golang.GoStmt)
		return c.matchNode(p.Expr, cand.Expr)
	case *golang.Defer:
		cand := candidate.(*golang.Defer)
		return c.matchNode(p.Expr, cand.Expr)
	case *golang.Send:
		cand := candidate.(*golang.Send)
		return c.matchNode(p.Channel, cand.Channel) &&
			c.matchNode(p.Arrow.Element, cand.Arrow.Element)
	case *golang.Goto:
		cand := candidate.(*golang.Goto)
		return c.matchNode(p.Label, cand.Label)
	case *golang.Fallthrough:
		return true
	case *golang.Composite:
		cand := candidate.(*golang.Composite)
		if !c.matchOptionalExpression(p.TypeExpr, cand.TypeExpr) {
			return false
		}
		return c.matchExpressionList(p.Elements.Elements, cand.Elements.Elements)
	case *golang.KeyValue:
		cand := candidate.(*golang.KeyValue)
		return c.matchNode(p.Key, cand.Key) &&
			c.matchNode(p.Value.Element, cand.Value.Element)
	case *golang.Slice:
		cand := candidate.(*golang.Slice)
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
	case *golang.MapType:
		cand := candidate.(*golang.MapType)
		return c.matchNode(p.Key.Element, cand.Key.Element) &&
			c.matchNode(p.Value, cand.Value)
	case *golang.Channel:
		cand := candidate.(*golang.Channel)
		return p.Dir == cand.Dir &&
			c.matchNode(p.Value, cand.Value)
	case *golang.FuncType:
		cand := candidate.(*golang.FuncType)
		if !c.matchStatementContainer(p.Parameters, cand.Parameters) {
			return false
		}
		return c.matchOptionalExpression(p.ReturnType, cand.ReturnType)
	case *golang.StructType:
		cand := candidate.(*golang.StructType)
		return c.matchOptionalNode(p.Body, cand.Body)
	case *golang.InterfaceType:
		cand := candidate.(*golang.InterfaceType)
		return c.matchOptionalNode(p.Body, cand.Body)
	case *golang.TypeList:
		cand := candidate.(*golang.TypeList)
		return c.matchStatementContainer(p.Types, cand.Types)
	case *golang.Union:
		cand := candidate.(*golang.Union)
		return c.matchExpressionRightPaddedList(p.Types, cand.Types)
	case *golang.UnderlyingType:
		cand := candidate.(*golang.UnderlyingType)
		return c.matchOptionalExpression(p.Element, cand.Element)
	case *golang.TypeDecl:
		cand := candidate.(*golang.TypeDecl)
		if !c.matchNode(p.Name, cand.Name) {
			return false
		}
		return c.matchOptionalExpression(p.Definition, cand.Definition)
	case *golang.MultiAssignment:
		cand := candidate.(*golang.MultiAssignment)
		if !c.matchExpressionRightPaddedList(p.Variables, cand.Variables) {
			return false
		}
		return c.matchExpressionRightPaddedList(p.Values, cand.Values)
	case *golang.CommClause:
		cand := candidate.(*golang.CommClause)
		if !c.matchOptionalStatement(p.Comm, cand.Comm) {
			return false
		}
		return c.matchStatementList(p.Body, cand.Body)
	case *golang.IndexList:
		cand := candidate.(*golang.IndexList)
		if !c.matchNode(p.Target, cand.Target) {
			return false
		}
		return c.matchExpressionList(p.Indices.Elements, cand.Indices.Elements)
	case *golang.CompilationUnit:
		cand := candidate.(*golang.CompilationUnit)
		return c.matchStatementList(p.Statements, cand.Statements)
	default:
		// Unknown node type — fail the match.
		return false
	}
}

// --- Helper methods for optional and list comparisons ---

func (c *patternComparator) matchOptionalNode(pattern, candidate java.J) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern, candidate)
}

func (c *patternComparator) matchOptionalExpression(pattern, candidate java.Expression) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern, candidate)
}

func (c *patternComparator) matchOptionalStatement(pattern, candidate java.Statement) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern, candidate)
}

func (c *patternComparator) matchOptionalRightPadded(pattern, candidate *java.RightPadded[java.Expression]) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchOptionalRightPaddedStmt(pattern, candidate *java.RightPadded[java.Statement]) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchOptionalRightPaddedExpr(pattern, candidate *java.RightPadded[java.Expression]) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchOptionalRightPaddedExpr2(pattern, candidate *java.RightPadded[java.Expression]) bool {
	return c.matchOptionalRightPaddedExpr(pattern, candidate)
}

func (c *patternComparator) matchOptionalRightPaddedJ(pattern, candidate *java.RightPadded[java.J]) bool {
	if pattern == nil && candidate == nil {
		return true
	}
	if pattern == nil || candidate == nil {
		return false
	}
	return c.matchNode(pattern.Element, candidate.Element)
}

func (c *patternComparator) matchExpressionList(pattern, candidate []java.RightPadded[java.Expression]) bool {
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

func (c *patternComparator) matchExpressionRightPaddedList(pattern, candidate []java.RightPadded[java.Expression]) bool {
	return c.matchExpressionList(pattern, candidate)
}

func (c *patternComparator) matchStatementList(pattern, candidate []java.RightPadded[java.Statement]) bool {
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

func (c *patternComparator) matchStatementContainer(pattern, candidate java.Container[java.Statement]) bool {
	return c.matchStatementList(pattern.Elements, candidate.Elements)
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

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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// BinarySpacingVisitor spaces binary operators the way gofmt does: the tighter
// an operator binds relative to its neighbours, the more likely it loses its
// blanks, so `x*y + z` and `hi-lo` come out alongside `a + b`. What decides it
// is the set of precedences in the expression and how deeply the expression
// nests, which this pass carries down from the statement it starts at.
//
// The rule and the numbers are go/printer's binaryExpr, walkBinary and cutoff.
type BinarySpacingVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
	depth int
}

// NewBinarySpacingVisitor returns a visitor configured with the given
// stopAfter bound. Pass nil to format the entire visited tree.
func NewBinarySpacingVisitor(stopAfter java.Tree) *BinarySpacingVisitor {
	return visitor.Init(&BinarySpacingVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
		depth:            1,
	})
}

func (v *BinarySpacingVisitor) Visit(t java.Tree, p any) java.Tree {
	if v.shouldHalt() {
		return t
	}
	// Every statement starts its expressions at the outermost depth, so a
	// statement reached from inside an expression — a function literal's body —
	// starts over rather than inheriting the depth around it.
	_, isStatement := t.(java.Statement)
	// A call is both a statement and an expression; only something that is
	// purely a statement begins a fresh expression.
	if _, isExpression := t.(java.Expression); isStatement && !isExpression {
		outer := v.depth
		v.depth = 1
		defer func() { v.depth = outer }()
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

func (v *BinarySpacingVisitor) VisitBinary(bin *java.Binary, p any) java.J {
	prec := javaPrecedence(bin.Operator.Element)
	out := *bin
	out.Operator.Before, out.Right = v.spaceOperands(
		operandsOf(bin), prec, bin.Operator.Before, bin.Right)

	depth := v.depth
	out.Left = v.operand(bin.Left, depth+diffPrec(bin.Left, prec))
	out.Right = v.operand(out.Right, depth+1)
	v.depth = depth
	return &out
}

func (v *BinarySpacingVisitor) VisitGoBinary(bin *golang.Binary, p any) java.J {
	// `&^` is the only operator golang.Binary carries, at the tightest level.
	const prec = 5
	out := *bin
	out.Operator.Before, out.Right = v.spaceOperands(
		operandsOf(bin), prec, bin.Operator.Before, bin.Right)

	depth := v.depth
	out.Left = v.operand(bin.Left, depth+diffPrec(bin.Left, prec))
	out.Right = v.operand(out.Right, depth+1)
	v.depth = depth
	return &out
}

// VisitParentheses undoes one level of nesting, since the parentheses already
// group what the depth is standing in for.
func (v *BinarySpacingVisitor) VisitParentheses(parens *java.Parentheses, p any) java.J {
	depth := v.depth
	out := *parens
	out.Tree.Element = v.operand(parens.Tree.Element, reduceDepth(depth))
	v.depth = depth
	return &out
}

// VisitArrayAccess descends into an index one level deeper, matching the
// nesting an index expression introduces in gofmt.
func (v *BinarySpacingVisitor) VisitArrayAccess(access *java.ArrayAccess, p any) java.J {
	out := *access
	depth := v.depth
	out.Indexed = v.operand(access.Indexed, depth)
	if access.Dimension != nil {
		dimension := *access.Dimension
		dimension.Index.Element = v.operand(access.Dimension.Index.Element, depth+1)
		out.Dimension = &dimension
	}
	v.depth = depth
	return &out
}

// VisitGoUnary starts a dereferenced operand at the outermost depth. gofmt
// prints the operand of `*x` as an expression in its own right, while every
// other unary operator carries the depth around it into its operand.
func (v *BinarySpacingVisitor) VisitGoUnary(u *golang.Unary, p any) java.J {
	if u.Operator.Element != golang.Indirection {
		return v.GoVisitor.VisitGoUnary(u, p)
	}
	depth := v.depth
	out := *u
	out.Expression = v.operand(u.Expression, 1)
	v.depth = depth
	return &out
}

// VisitSlice descends into the indices of a slice expression, and spaces the
// colons: gofmt sets them off with blanks when an outermost slice has more than
// one index and at least one of them is a binary expression, so `a[i : j+1]`
// reads apart while `a[:n-1]` stays tight.
func (v *BinarySpacingVisitor) VisitSlice(sl *golang.Slice, p any) java.J {
	depth := v.depth
	out := *sl
	out.Indexed = v.operand(sl.Indexed, 1)

	indices := []java.Expression{sl.Low.Element, sl.High.Element, sl.Max}
	present, binaries := 0, 0
	for _, index := range indices {
		if isPresentIndex(index) {
			present++
			if operandsOf(index).prec > 0 {
				binaries++
			}
		}
	}
	blanks := depth <= 1 && present > 1 && binaries > 0

	out.Low.Element = v.operand(sl.Low.Element, depth+1)
	out.High.Element = v.operand(sl.High.Element, depth+1)
	out.Max = v.operand(sl.Max, depth+1)

	// A colon takes a blank on the side where an index is written, and none
	// where one is omitted.
	out.Low.After = spaceOrNothing(out.Low.After, blanks && isPresentIndex(out.Low.Element))
	if isPresentIndex(out.High.Element) {
		out.High.Element = withPrefix(out.High.Element, spaceOrNothing(getPrefix(out.High.Element), blanks))
		out.High.After = spaceOrNothing(out.High.After, blanks && out.Max != nil)
	}
	if isPresentIndex(out.Max) {
		out.Max = withPrefix(out.Max, spaceOrNothing(getPrefix(out.Max), blanks))
	}
	v.depth = depth
	return &out
}

// isPresentIndex reports whether a slice index is written, since an omitted one
// is modeled as an empty expression rather than as nothing.
func isPresentIndex(e java.Expression) bool {
	if e == nil {
		return false
	}
	_, empty := e.(*java.Empty)
	return !empty
}

// VisitComposite prints its elements at the outermost depth however deeply the
// literal itself sits, which is gofmt's rule for a composite literal.
func (v *BinarySpacingVisitor) VisitComposite(c *golang.Composite, p any) java.J {
	depth := v.depth
	out := *c
	if c.TypeExpr != nil {
		out.TypeExpr = v.operand(c.TypeExpr, depth)
	}
	out.Elements.Elements = v.operandList(c.Elements.Elements, 1)
	v.depth = depth
	return &out
}

// VisitMethodInvocation descends one level for a call that passes more than one
// argument, which is where gofmt starts tightening the operators inside them.
func (v *BinarySpacingVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	depth := v.depth
	if len(mi.Arguments.Elements) > 1 {
		v.depth++
	}
	out := v.GoVisitor.VisitMethodInvocation(mi, p)
	v.depth = depth
	return out
}

// VisitMultiAssignment starts one level in when both sides list more than one
// expression, which is where gofmt tightens the operators inside them.
func (v *BinarySpacingVisitor) VisitMultiAssignment(assign *golang.MultiAssignment, p any) java.J {
	depth := 1
	if len(assign.Variables) > 1 && len(assign.Values) > 1 {
		depth = 2
	}
	out := *assign
	out.Variables = v.operandList(assign.Variables, depth)
	out.Values = v.operandList(assign.Values, depth)
	return &out
}

func (v *BinarySpacingVisitor) operand(e java.Expression, depth int) java.Expression {
	if e == nil {
		return nil
	}
	v.depth = depth
	if out, ok := v.Visit(e, nil).(java.Expression); ok {
		return out
	}
	return e
}

func (v *BinarySpacingVisitor) operandList(elements []java.RightPadded[java.Expression], depth int) []java.RightPadded[java.Expression] {
	outer := v.depth
	out := append([]java.RightPadded[java.Expression](nil), elements...)
	for i := range out {
		out[i].Element = v.operand(out[i].Element, depth)
	}
	v.depth = outer
	return out
}

// spaceOperands decides whether an operator of the given precedence keeps the
// blanks around it, and returns the space before the operator together with the
// right operand carrying the space after it. An operator whose right operand
// starts on another line keeps that break and takes no blank after it.
func (v *BinarySpacingVisitor) spaceOperands(operands binaryOperands, prec int, before java.Space, right java.Expression) (java.Space, java.Expression) {
	blank := prec < cutoff(operands, v.depth)
	before = spaceOrNothing(before, blank)
	if strings.Contains(getPrefix(right).Whitespace, "\n") {
		return before, right
	}
	return before, withPrefix(right, spaceOrNothing(getPrefix(right), blank))
}

func spaceOrNothing(s java.Space, blank bool) java.Space {
	if len(s.Comments) > 0 || strings.Contains(s.Whitespace, "\n") {
		return s
	}
	if blank {
		s.Whitespace = " "
	} else {
		s.Whitespace = ""
	}
	return s
}

// binaryOperands is what cutoff needs to know about one binary expression: the
// operator's precedence and the two operands.
type binaryOperands struct {
	prec        int
	token       string
	left, right java.Expression
}

// BinaryOperands reports how tightly e binds, on go/token.Token.Precedence's
// scale, along with its operands. ok is false for anything that is not a binary
// expression. Callers outside layout need this to decide grouping.
func BinaryOperands(e java.Expression) (prec int, left, right java.Expression, ok bool) {
	ops := operandsOf(e)
	if ops.prec == 0 {
		return 0, nil, nil, false
	}
	return ops.prec, ops.left, ops.right, true
}

func operandsOf(e java.Expression) binaryOperands {
	switch b := e.(type) {
	case *java.Binary:
		op := b.Operator.Element
		return binaryOperands{javaPrecedence(op), javaOperatorToken(op), b.Left, b.Right}
	case *golang.Binary:
		return binaryOperands{5, "&^", b.Left, b.Right}
	}
	return binaryOperands{}
}

// cutoff is the precedence at and above which an operator loses its blanks.
func cutoff(e binaryOperands, depth int) int {
	has4, has5, maxProblem := walkBinary(e)
	if maxProblem > 0 {
		return maxProblem + 1
	}
	if has4 && has5 {
		if depth == 1 {
			return 5
		}
		return 4
	}
	if depth == 1 {
		return 6
	}
	return 4
}

// walkBinary reports which precedence levels appear in an expression, and the
// tightest level at which writing two operators together would read as one
// token — `/*` opening a comment, for one.
func walkBinary(e binaryOperands) (has4, has5 bool, maxProblem int) {
	switch e.prec {
	case 4:
		has4 = true
	case 5:
		has5 = true
	}

	if l := operandsOf(e.left); l.prec > 0 && l.prec >= e.prec {
		h4, h5, mp := walkBinary(l)
		has4, has5 = has4 || h4, has5 || h5
		maxProblem = max(maxProblem, mp)
	}

	if r := operandsOf(e.right); r.prec > 0 && r.prec > e.prec {
		h4, h5, mp := walkBinary(r)
		has4, has5 = has4 || h4, has5 || h5
		maxProblem = max(maxProblem, mp)
	} else if op, ok := unaryOperator(e.right); ok {
		switch e.token + op {
		case "/*", "&&", "&^":
			maxProblem = 5
		case "++", "--":
			maxProblem = max(maxProblem, 4)
		}
	}
	return
}

// unaryOperator reports the token a unary expression writes ahead of its
// operand, which can join the binary operator to its left.
func unaryOperator(e java.Expression) (string, bool) {
	u, ok := e.(*java.Unary)
	if !ok {
		return "", false
	}
	switch u.Operator.Element {
	case java.Positive:
		return "+", true
	case java.Negate:
		return "-", true
	case java.Not:
		return "!", true
	case java.BitwiseNot:
		return "^", true
	case java.Deref:
		return "*", true
	case java.AddressOf:
		return "&", true
	case java.Receive:
		return "<-", true
	}
	return "", false
}

func diffPrec(e java.Expression, prec int) int {
	if operands := operandsOf(e); operands.prec == prec {
		return 0
	}
	return 1
}

func reduceDepth(depth int) int {
	if depth <= 1 {
		return 1
	}
	return depth - 1
}

// javaOperatorToken spells an operator as Go writes it, which decides whether it
// joins the unary operator of the operand to its right.
func javaOperatorToken(op java.BinaryOperator) string {
	switch op {
	case java.LogicalOr, java.Or:
		return "||"
	case java.LogicalAnd, java.And:
		return "&&"
	case java.Equal:
		return "=="
	case java.NotEqual:
		return "!="
	case java.LessThan:
		return "<"
	case java.LessThanOrEqual:
		return "<="
	case java.GreaterThan:
		return ">"
	case java.GreaterThanOrEqual:
		return ">="
	case java.Add:
		return "+"
	case java.Subtract:
		return "-"
	case java.BitwiseOr:
		return "|"
	case java.BitwiseXor:
		return "^"
	case java.Multiply:
		return "*"
	case java.Divide:
		return "/"
	case java.Modulo:
		return "%"
	case java.LeftShift:
		return "<<"
	case java.RightShift:
		return ">>"
	case java.BitwiseAnd:
		return "&"
	case java.AndNot:
		return "&^"
	}
	return ""
}

// javaPrecedence is go/token.Token.Precedence for the operators java.Binary
// carries.
func javaPrecedence(op java.BinaryOperator) int {
	switch op {
	case java.LogicalOr, java.Or:
		return 1
	case java.LogicalAnd, java.And:
		return 2
	case java.Equal, java.NotEqual, java.LessThan, java.LessThanOrEqual, java.GreaterThan, java.GreaterThanOrEqual:
		return 3
	case java.Add, java.Subtract, java.BitwiseOr, java.BitwiseXor:
		return 4
	case java.Multiply, java.Divide, java.Modulo, java.LeftShift, java.RightShift, java.BitwiseAnd, java.AndNot:
		return 5
	}
	return 0
}

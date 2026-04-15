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

package rpc

import (
	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// Receiver can deserialize any AST node.
type Receiver interface {
	Visit(node any, q *ReceiveQueue) any
}

// JavaReceiver deserializes J (shared Java-like) AST nodes from the receive queue.
// Mirrors JavaReceiver.java for J nodes.
type JavaReceiver struct {
	typeReceiver *JavaTypeReceiver
	parent       Receiver // the GoReceiver (or other language receiver) that delegates to us
}

// visitJ dispatches J-node field deserialization (after preVisit has been called by the parent).
func (r *JavaReceiver) visitJ(node any, q *ReceiveQueue) any {
	switch v := node.(type) {
	case *tree.Identifier:
		return r.receiveIdentifier(v, q)
	case *tree.Literal:
		return r.receiveLiteral(v, q)
	case *tree.Binary:
		return r.receiveBinary(v, q)
	case *tree.Block:
		return r.receiveBlock(v, q)
	case *tree.Empty:
		return v
	case *tree.Unary:
		return r.receiveUnary(v, q)
	case *tree.FieldAccess:
		return r.receiveFieldAccess(v, q)
	case *tree.MethodInvocation:
		return r.receiveMethodInvocation(v, q)
	case *tree.Assignment:
		return r.receiveAssignment(v, q)
	case *tree.AssignmentOperation:
		return r.receiveAssignmentOperation(v, q)
	case *tree.MethodDeclaration:
		return r.receiveMethodDeclaration(v, q)
	case *tree.VariableDeclarations:
		return r.receiveVariableDeclarations(v, q)
	case *tree.VariableDeclarator:
		return r.receiveVariableDeclarator(v, q)
	case *tree.Return:
		return r.receiveReturn(v, q)
	case *tree.If:
		return r.receiveIf(v, q)
	case *tree.Else:
		return r.receiveElse(v, q)
	case *tree.ForLoop:
		return r.receiveForLoop(v, q)
	case *tree.ForControl:
		return r.receiveForControl(v, q)
	case *tree.ForEachLoop:
		return r.receiveForEachLoop(v, q)
	case *tree.ForEachControl:
		return r.receiveForEachControl(v, q)
	case *tree.Switch:
		return r.receiveSwitch(v, q)
	case *tree.Case:
		return r.receiveCase(v, q)
	case *tree.Break:
		return r.receiveBreak(v, q)
	case *tree.Continue:
		return r.receiveContinue(v, q)
	case *tree.Label:
		return r.receiveLabel(v, q)
	case *tree.ArrayType:
		return r.receiveArrayType(v, q)
	case *tree.ArrayAccess:
		return r.receiveArrayAccess(v, q)
	case *tree.ParameterizedType:
		return r.receiveParameterizedType(v, q)
	case *tree.ArrayDimension:
		return r.receiveArrayDimension(v, q)
	case *tree.Parentheses:
		return r.receiveParentheses(v, q)
	case *tree.TypeCast:
		return r.receiveTypeCast(v, q)
	case *tree.ControlParentheses:
		return r.receiveControlParentheses(v, q)
	case *tree.Import:
		return r.receiveImport(v, q)
	default:
		return node
	}
}

// receiveType receives a JavaType from the queue with null/Unknown handling.
func (r *JavaReceiver) receiveType(before tree.JavaType, q *ReceiveQueue) tree.JavaType {
	result := q.Receive(before, func(v any) any {
		return r.typeReceiver.Visit(v.(tree.JavaType), q)
	})
	if result == nil {
		return nil
	}
	return result.(tree.JavaType)
}

// --- J nodes ---

func (r *JavaReceiver) receiveIdentifier(id *tree.Identifier, q *ReceiveQueue) *tree.Identifier {
	c := *id // shallow copy to avoid mutating remoteObjects baseline
	id = &c
	// annotations
	beforeAnns := make([]any, len(id.Annotations))
	for i, a := range id.Annotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.parent.Visit(v, q) })
	if afterAnns != nil {
		id.Annotations = make([]tree.Tree, len(afterAnns))
		for i, a := range afterAnns {
			id.Annotations[i] = a.(tree.Tree)
		}
	}
	// simpleName
	id.Name = receiveScalar[string](q, id.Name)
	// type (as ref)
	id.Type = r.receiveType(id.Type, q)
	// fieldType (as ref)
	ft := r.receiveType(id.FieldType, q)
	if ft != nil {
		id.FieldType = ft.(*tree.JavaTypeVariable)
	} else {
		id.FieldType = nil
	}
	return id
}

func (r *JavaReceiver) receiveLiteral(lit *tree.Literal, q *ReceiveQueue) *tree.Literal {
	c := *lit // shallow copy to avoid mutating remoteObjects baseline
	lit = &c
	// value
	lit.Value = q.Receive(lit.Value, nil)
	// valueSource
	lit.Source = receiveScalar[string](q, lit.Source)
	// unicodeEscapes (empty for Go)
	q.ReceiveList(nil, nil)
	// type (as ref)
	lit.Type = r.receiveType(lit.Type, q)
	return lit
}

func (r *JavaReceiver) receiveBinary(b *tree.Binary, q *ReceiveQueue) *tree.Binary {
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	result := q.Receive(b.Left, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		b.Left = result.(tree.Expression)
	}
	if result := q.Receive(b.Operator, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		b.Operator = result.(tree.LeftPadded[tree.BinaryOperator])
	}
	rightResult := q.Receive(b.Right, func(v any) any { return r.parent.Visit(v, q) })
	if rightResult != nil {
		b.Right = rightResult.(tree.Expression)
	}
	b.Type = r.receiveType(b.Type, q)
	return b
}

func (r *JavaReceiver) receiveBlock(b *tree.Block, q *ReceiveQueue) *tree.Block {
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	// static (right-padded) - Java-only field, not stored in Go Block
	q.Receive(nil, func(v any) any { return receiveRightPadded(r.parent, q, v) })
	// statements
	beforeStmts := make([]any, len(b.Statements))
	for i, s := range b.Statements {
		beforeStmts[i] = s
	}
	afterStmts := q.ReceiveList(beforeStmts, func(v any) any { return receiveRightPadded(r.parent, q, v) })
	if afterStmts != nil {
		b.Statements = make([]tree.RightPadded[tree.Statement], len(afterStmts))
		for i, s := range afterStmts {
			b.Statements[i] = coerceToStatementRP(s)
		}
	}
	// end space
	if result := q.Receive(b.End, func(v any) any {
		return receiveSpace(v.(tree.Space), q)
	}); result != nil {
		b.End = result.(tree.Space)
	}
	return b
}

func (r *JavaReceiver) receiveUnary(u *tree.Unary, q *ReceiveQueue) *tree.Unary {
	c := *u // shallow copy to avoid mutating remoteObjects baseline
	u = &c
	if result := q.Receive(u.Operator, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		u.Operator = result.(tree.LeftPadded[tree.UnaryOperator])
	}
	result := q.Receive(u.Operand, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		u.Operand = result.(tree.Expression)
	}
	u.Type = r.receiveType(u.Type, q)
	return u
}

func (r *JavaReceiver) receiveFieldAccess(fa *tree.FieldAccess, q *ReceiveQueue) *tree.FieldAccess {
	c := *fa // shallow copy to avoid mutating remoteObjects baseline
	fa = &c
	result := q.Receive(fa.Target, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		fa.Target = result.(tree.Expression)
	}
	if result := q.Receive(fa.Name, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		fa.Name = result.(tree.LeftPadded[*tree.Identifier])
	}
	fa.Type = r.receiveType(fa.Type, q)
	return fa
}

func (r *JavaReceiver) receiveMethodInvocation(mi *tree.MethodInvocation, q *ReceiveQueue) *tree.MethodInvocation {
	c := *mi // shallow copy to avoid mutating remoteObjects baseline
	mi = &c
	// select
	var beforeSelect any
	if mi.Select != nil {
		beforeSelect = *mi.Select
	}
	if result := q.Receive(beforeSelect, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		rp := coerceToExpressionRP(result)
		mi.Select = &rp
	} else {
		mi.Select = nil
	}
	// typeParameters (nil for Go)
	q.Receive(nil, nil)
	// name
	result := q.Receive(mi.Name, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		mi.Name = result.(*tree.Identifier)
	}
	// arguments
	if result := q.Receive(mi.Arguments, func(v any) any { return receiveContainer(r.parent, q, v) }); result != nil {
		mi.Arguments = result.(tree.Container[tree.Expression])
	}
	// methodType
	mt := r.receiveType(mi.MethodType, q)
	if mt != nil {
		mi.MethodType = mt.(*tree.JavaTypeMethod)
	} else {
		mi.MethodType = nil
	}
	return mi
}

func (r *JavaReceiver) receiveAssignment(a *tree.Assignment, q *ReceiveQueue) *tree.Assignment {
	c := *a // shallow copy to avoid mutating remoteObjects baseline
	a = &c
	result := q.Receive(a.Variable, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		a.Variable = result.(tree.Expression)
	}
	if result := q.Receive(a.Value, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		a.Value = result.(tree.LeftPadded[tree.Expression])
	}
	a.Type = r.receiveType(a.Type, q)
	return a
}

func (r *JavaReceiver) receiveAssignmentOperation(a *tree.AssignmentOperation, q *ReceiveQueue) *tree.AssignmentOperation {
	c := *a // shallow copy to avoid mutating remoteObjects baseline
	a = &c
	result := q.Receive(a.Variable, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		a.Variable = result.(tree.Expression)
	}
	if result := q.Receive(a.Operator, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		a.Operator = result.(tree.LeftPadded[tree.AssignmentOperator])
	}
	assignResult := q.Receive(a.Assignment, func(v any) any { return r.parent.Visit(v, q) })
	if assignResult != nil {
		a.Assignment = assignResult.(tree.Expression)
	}
	a.Type = r.receiveType(a.Type, q)
	return a
}

func (r *JavaReceiver) receiveMethodDeclaration(md *tree.MethodDeclaration, q *ReceiveQueue) *tree.MethodDeclaration {
	c := *md // shallow copy to avoid mutating remoteObjects baseline
	md = &c
	// leadingAnnotations
	q.ReceiveList(nil, nil)
	// modifiers
	q.ReceiveList(nil, nil)
	// typeParameters
	q.Receive(nil, nil)
	// returnTypeExpression
	result := q.Receive(md.ReturnType, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		md.ReturnType = result.(tree.Expression)
	}
	// name annotations
	q.ReceiveList(nil, nil)
	// name
	nameResult := q.Receive(md.Name, func(v any) any { return r.parent.Visit(v, q) })
	if nameResult != nil {
		md.Name = nameResult.(*tree.Identifier)
	}
	// parameters
	if result := q.Receive(md.Parameters, func(v any) any { return receiveContainerAs(r.parent, q, v, ContainerStatement) }); result != nil {
		md.Parameters = result.(tree.Container[tree.Statement])
	}
	// throws
	q.Receive(nil, nil)
	// body
	bodyResult := q.Receive(md.Body, func(v any) any { return r.parent.Visit(v, q) })
	if bodyResult != nil {
		md.Body = bodyResult.(*tree.Block)
	}
	// defaultValue
	q.Receive(nil, nil)
	// methodType
	mt := r.receiveType(md.MethodType, q)
	if mt != nil {
		md.MethodType = mt.(*tree.JavaTypeMethod)
	} else {
		md.MethodType = nil
	}
	return md
}

func (r *JavaReceiver) receiveVariableDeclarations(vd *tree.VariableDeclarations, q *ReceiveQueue) *tree.VariableDeclarations {
	c := *vd // shallow copy to avoid mutating remoteObjects baseline
	vd = &c
	// leadingAnnotations
	q.ReceiveList(nil, nil)
	// modifiers
	q.ReceiveList(nil, nil)
	// typeExpression
	result := q.Receive(vd.TypeExpr, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		vd.TypeExpr = result.(tree.Expression)
	}
	// varargs
	var currentVarargs any
	if vd.Varargs != nil {
		currentVarargs = *vd.Varargs
	}
	varargsResult := q.Receive(currentVarargs, func(v any) any { return receiveSpace(v.(tree.Space), q) })
	if varargsResult != nil {
		sp := varargsResult.(tree.Space)
		vd.Varargs = &sp
	}
	// variables
	beforeVars := make([]any, len(vd.Variables))
	for i, v := range vd.Variables {
		beforeVars[i] = v
	}
	afterVars := q.ReceiveList(beforeVars, func(v any) any { return receiveRightPadded(r.parent, q, v) })
	if afterVars != nil {
		vd.Variables = make([]tree.RightPadded[*tree.VariableDeclarator], len(afterVars))
		for i, v := range afterVars {
			vd.Variables[i] = v.(tree.RightPadded[*tree.VariableDeclarator])
		}
	}
	return vd
}

func (r *JavaReceiver) receiveVariableDeclarator(vd *tree.VariableDeclarator, q *ReceiveQueue) *tree.VariableDeclarator {
	c := *vd // shallow copy to avoid mutating remoteObjects baseline
	vd = &c
	// declarator (name)
	result := q.Receive(vd.Name, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		vd.Name = result.(*tree.Identifier)
	}
	// dimensionsAfterName
	q.ReceiveList(nil, nil)
	// initializer
	var beforeInit any
	if vd.Initializer != nil {
		beforeInit = *vd.Initializer
	}
	if result := q.Receive(beforeInit, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		lp := result.(tree.LeftPadded[tree.Expression])
		vd.Initializer = &lp
	} else {
		vd.Initializer = nil
	}
	// variableType
	q.Receive(nil, nil)
	return vd
}

func (r *JavaReceiver) receiveReturn(ret *tree.Return, q *ReceiveQueue) *tree.Return {
	c := *ret
	ret = &c
	var beforeExpr any
	if len(ret.Expressions) > 0 {
		beforeExpr = ret.Expressions[0].Element
	}
	result := q.Receive(beforeExpr, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		expr := result.(tree.Expression)
		if len(ret.Expressions) > 0 {
			ret.Expressions[0] = tree.RightPadded[tree.Expression]{
				Element: expr,
				After:   ret.Expressions[0].After,
				Markers: ret.Expressions[0].Markers,
			}
		} else {
			ret.Expressions = []tree.RightPadded[tree.Expression]{
				{Element: expr},
			}
		}
	} else if beforeExpr != nil {
		ret.Expressions = nil
	}
	return ret
}

func (r *JavaReceiver) receiveIf(i *tree.If, q *ReceiveQueue) *tree.If {
	c := *i // shallow copy to avoid mutating remoteObjects baseline
	i = &c
	// ifCondition - Java sends ControlParentheses; cache it for future round-trips
	var beforeCP any
	if i.ConditionCP != nil {
		beforeCP = i.ConditionCP
	} else if i.Condition != nil {
		beforeCP = &tree.ControlParentheses{
			ID:      uuid.New(),
			Markers: tree.Markers{ID: uuid.New()},
			Tree:    tree.RightPadded[tree.Expression]{Element: i.Condition},
		}
	}
	cpResult := q.Receive(beforeCP, func(v any) any { return r.parent.Visit(v, q) })
	if cpResult != nil {
		if cp, ok := cpResult.(*tree.ControlParentheses); ok {
			i.ConditionCP = cp
			i.Condition = cp.Tree.Element
		} else {
			i.Condition = cpResult.(tree.Expression)
		}
	}
	// thenPart - Java sends RightPadded<Statement> wrapping the Block
	if thenResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r.parent, q, v) }); thenResult != nil {
		rp := coerceToStatementRP(thenResult)
		if blk, ok := rp.Element.(*tree.Block); ok {
			i.Then = blk
		}
	}
	// elsePart - Java sends Else node, convert to RightPadded
	elseResult := q.Receive(nil, func(v any) any { return r.parent.Visit(v, q) })
	if elseResult != nil {
		el := elseResult.(*tree.Else)
		i.ElsePart = &tree.RightPadded[tree.J]{
			Element: el.Body.Element,
			After:   el.Prefix,
		}
	} else {
		i.ElsePart = nil
	}
	return i
}

func (r *JavaReceiver) receiveElse(el *tree.Else, q *ReceiveQueue) *tree.Else {
	c := *el // shallow copy to avoid mutating remoteObjects baseline
	el = &c
	if result := q.Receive(el.Body, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		el.Body = result.(tree.RightPadded[tree.Statement])
	}
	return el
}

func (r *JavaReceiver) receiveForLoop(f *tree.ForLoop, q *ReceiveQueue) *tree.ForLoop {
	c := *f // shallow copy to avoid mutating remoteObjects baseline
	f = &c
	ctrl := &f.Control
	result := q.Receive(ctrl, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		f.Control = *result.(*tree.ForControl)
	}
	// body - Java sends RightPadded<Statement> wrapping the Block
	if bodyResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r.parent, q, v) }); bodyResult != nil {
		rp := coerceToStatementRP(bodyResult)
		if blk, ok := rp.Element.(*tree.Block); ok {
			f.Body = blk
		}
	}
	return f
}

func (r *JavaReceiver) receiveForControl(fc *tree.ForControl, q *ReceiveQueue) *tree.ForControl {
	c := *fc // shallow copy to avoid mutating remoteObjects baseline
	fc = &c
	// init (list of right-padded)
	var beforeInit []any
	if fc.Init != nil {
		beforeInit = []any{*fc.Init}
	}
	initList := q.ReceiveList(beforeInit, func(v any) any { return receiveRightPadded(r.parent, q, v) })
	if len(initList) > 0 {
		rp := initList[0].(tree.RightPadded[tree.Statement])
		fc.Init = &rp
	} else {
		fc.Init = nil
	}
	// condition (right-padded)
	var beforeCond any
	if fc.Condition != nil {
		beforeCond = *fc.Condition
	}
	if result := q.Receive(beforeCond, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		rp := coerceToExpressionRP(result)
		fc.Condition = &rp
	} else {
		fc.Condition = nil
	}
	// update (list of right-padded)
	var beforeUpdate []any
	if fc.Update != nil {
		beforeUpdate = []any{*fc.Update}
	}
	updateList := q.ReceiveList(beforeUpdate, func(v any) any { return receiveRightPadded(r.parent, q, v) })
	if len(updateList) > 0 {
		rp := updateList[0].(tree.RightPadded[tree.Statement])
		fc.Update = &rp
	} else {
		fc.Update = nil
	}
	return fc
}

func (r *JavaReceiver) receiveForEachLoop(f *tree.ForEachLoop, q *ReceiveQueue) *tree.ForEachLoop {
	c := *f // shallow copy to avoid mutating remoteObjects baseline
	f = &c
	ctrl := &f.Control
	result := q.Receive(ctrl, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		f.Control = *result.(*tree.ForEachControl)
	}
	// body - Java sends RightPadded<Statement> wrapping the Block
	if bodyResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r.parent, q, v) }); bodyResult != nil {
		rp := coerceToStatementRP(bodyResult)
		if blk, ok := rp.Element.(*tree.Block); ok {
			f.Body = blk
		}
	}
	return f
}

func (r *JavaReceiver) receiveForEachControl(fc *tree.ForEachControl, q *ReceiveQueue) *tree.ForEachControl {
	c := *fc // shallow copy to avoid mutating remoteObjects baseline
	fc = &c
	// key (right-padded, nullable)
	var beforeKey any
	if fc.Key != nil {
		beforeKey = *fc.Key
	}
	if result := q.Receive(beforeKey, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		rp := coerceToExpressionRP(result)
		fc.Key = &rp
	} else {
		fc.Key = nil
	}
	// value (right-padded, nullable)
	var beforeValue any
	if fc.Value != nil {
		beforeValue = *fc.Value
	}
	if result := q.Receive(beforeValue, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		rp := coerceToExpressionRP(result)
		fc.Value = &rp
	} else {
		fc.Value = nil
	}
	// operator (left-padded AssignOp as string)
	if result := q.Receive(fc.Operator, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		fc.Operator = result.(tree.LeftPadded[tree.AssignOp])
	}
	// iterable
	result := q.Receive(fc.Iterable, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		fc.Iterable = result.(tree.Expression)
	}
	return fc
}

func (r *JavaReceiver) receiveSwitch(sw *tree.Switch, q *ReceiveQueue) *tree.Switch {
	c := *sw // shallow copy to avoid mutating remoteObjects baseline
	sw = &c
	// selector - Java sends ControlParentheses, extract inner Expression for Tag
	cpResult := q.Receive(nil, func(v any) any { return r.parent.Visit(v, q) })
	if cpResult != nil {
		if cp, ok := cpResult.(*tree.ControlParentheses); ok {
			if _, isEmpty := cp.Tree.Element.(*tree.Empty); !isEmpty {
				sw.Tag = &tree.RightPadded[tree.Expression]{
					Element: cp.Tree.Element,
					After:   cp.Tree.After,
				}
			}
		}
	}
	result := q.Receive(sw.Body, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		sw.Body = result.(*tree.Block)
	}
	return sw
}

func (r *JavaReceiver) receiveCase(cs *tree.Case, q *ReceiveQueue) *tree.Case {
	c := *cs // shallow copy to avoid mutating remoteObjects baseline
	cs = &c
	q.Receive(nil, nil) // type enum
	if result := q.Receive(cs.Expressions, func(v any) any { return receiveContainer(r.parent, q, v) }); result != nil {
		cs.Expressions = result.(tree.Container[tree.Expression])
	}
	// statements - Java sends Container<RightPadded<Statement>>, extract to Go's []RightPadded[Statement]
	if result := q.Receive(nil, func(v any) any { return receiveContainerAs(r.parent, q, v, ContainerStatement) }); result != nil {
		cont := result.(tree.Container[tree.Statement])
		cs.Body = cont.Elements
	}
	q.Receive(nil, nil) // body
	q.Receive(nil, nil) // guard
	return cs
}

func (r *JavaReceiver) receiveBreak(b *tree.Break, q *ReceiveQueue) *tree.Break {
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	result := q.Receive(b.Label, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		b.Label = result.(*tree.Identifier)
	}
	return b
}

func (r *JavaReceiver) receiveContinue(cont *tree.Continue, q *ReceiveQueue) *tree.Continue {
	c := *cont // shallow copy to avoid mutating remoteObjects baseline
	cont = &c
	result := q.Receive(cont.Label, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		cont.Label = result.(*tree.Identifier)
	}
	return cont
}

func (r *JavaReceiver) receiveLabel(l *tree.Label, q *ReceiveQueue) *tree.Label {
	c := *l // shallow copy to avoid mutating remoteObjects baseline
	l = &c
	if result := q.Receive(l.Name, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		l.Name = coerceRightPaddedIdent(result)
	}
	result := q.Receive(l.Statement, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		l.Statement = result.(tree.Statement)
	}
	return l
}

func (r *JavaReceiver) receiveArrayType(at *tree.ArrayType, q *ReceiveQueue) *tree.ArrayType {
	c := *at // shallow copy to avoid mutating remoteObjects baseline
	at = &c
	result := q.Receive(at.ElementType, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		at.ElementType = result.(tree.Expression)
	}
	q.ReceiveList(nil, nil) // annotations
	if result := q.Receive(at.Dimension, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		at.Dimension = result.(tree.LeftPadded[tree.Space])
	}
	at.Type = r.receiveType(at.Type, q)
	return at
}

func (r *JavaReceiver) receiveParameterizedType(pt *tree.ParameterizedType, q *ReceiveQueue) *tree.ParameterizedType {
	c := *pt
	pt = &c
	if result := q.Receive(pt.Clazz, func(v any) any { return r.parent.Visit(v, q) }); result != nil {
		pt.Clazz = result.(tree.Expression)
	}
	if result := q.Receive(pt.TypeParameters, func(v any) any { return receiveContainer(r.parent, q, v) }); result != nil {
		container := result.(tree.Container[tree.Expression])
		pt.TypeParameters = &container
	}
	pt.Type = r.receiveType(pt.Type, q)
	return pt
}

func (r *JavaReceiver) receiveArrayAccess(aa *tree.ArrayAccess, q *ReceiveQueue) *tree.ArrayAccess {
	c := *aa // shallow copy to avoid mutating remoteObjects baseline
	aa = &c
	result := q.Receive(aa.Indexed, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		aa.Indexed = result.(tree.Expression)
	}
	dimResult := q.Receive(aa.Dimension, func(v any) any { return r.parent.Visit(v, q) })
	if dimResult != nil {
		aa.Dimension = dimResult.(*tree.ArrayDimension)
	}
	return aa
}

func (r *JavaReceiver) receiveArrayDimension(ad *tree.ArrayDimension, q *ReceiveQueue) *tree.ArrayDimension {
	c := *ad // shallow copy to avoid mutating remoteObjects baseline
	ad = &c
	if result := q.Receive(ad.Index, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		ad.Index = coerceToExpressionRP(result)
	}
	return ad
}

func (r *JavaReceiver) receiveParentheses(p *tree.Parentheses, q *ReceiveQueue) *tree.Parentheses {
	c := *p // shallow copy to avoid mutating remoteObjects baseline
	p = &c
	if result := q.Receive(p.Tree, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		p.Tree = coerceToExpressionRP(result)
	}
	return p
}

func (r *JavaReceiver) receiveTypeCast(tc *tree.TypeCast, q *ReceiveQueue) *tree.TypeCast {
	c := *tc // shallow copy to avoid mutating remoteObjects baseline
	tc = &c
	result := q.Receive(tc.Clazz, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		tc.Clazz = result.(*tree.ControlParentheses)
	}
	exprResult := q.Receive(tc.Expr, func(v any) any { return r.parent.Visit(v, q) })
	if exprResult != nil {
		tc.Expr = exprResult.(tree.Expression)
	}
	return tc
}

func (r *JavaReceiver) receiveControlParentheses(cp *tree.ControlParentheses, q *ReceiveQueue) *tree.ControlParentheses {
	c := *cp // shallow copy to avoid mutating remoteObjects baseline
	cp = &c
	if result := q.Receive(cp.Tree, func(v any) any { return receiveRightPadded(r.parent, q, v) }); result != nil {
		cp.Tree = coerceToExpressionRP(result)
	}
	return cp
}

func (r *JavaReceiver) receiveImport(imp *tree.Import, q *ReceiveQueue) *tree.Import {
	c := *imp // shallow copy to avoid mutating remoteObjects baseline
	imp = &c
	// static (always false for Go, but must receive full LeftPadded protocol)
	staticBefore := tree.LeftPadded[bool]{Before: tree.EmptySpace, Element: false}
	q.Receive(staticBefore, func(v any) any { return receiveLeftPadded(r.parent, q, v) })
	// qualid (Expression - could be Literal or FieldAccess depending on direction)
	result := q.Receive(imp.Qualid, func(v any) any { return r.parent.Visit(v, q) })
	if result != nil {
		imp.Qualid = result.(tree.Expression)
	}
	// alias
	var beforeAlias any
	if imp.Alias != nil {
		beforeAlias = *imp.Alias
	}
	if result := q.Receive(beforeAlias, func(v any) any { return receiveLeftPadded(r.parent, q, v) }); result != nil {
		lp := result.(tree.LeftPadded[*tree.Identifier])
		imp.Alias = &lp
	} else {
		imp.Alias = nil
	}
	return imp
}

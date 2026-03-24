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
	"fmt"

	"github.com/openrewrite/rewrite/pkg/tree"
)

// GoReceiver deserializes Go AST nodes from the receive queue.
// Mirrors GolangReceiver.java + JavaReceiver.java for J nodes.
type GoReceiver struct {
	typeReceiver JavaTypeReceiver
}

// visit dispatches to the appropriate receive method based on node type.
func (r *GoReceiver) Visit(node any, q *ReceiveQueue) any {
	if node == nil {
		return nil
	}

	// preVisit: receive ID, prefix, markers
	node = r.preVisit(node, q)

	switch v := node.(type) {
	// G nodes
	case *tree.CompilationUnit:
		return r.receiveCompilationUnit(v, q)
	case *tree.GoStmt:
		return r.receiveGoStmt(v, q)
	case *tree.Defer:
		return r.receiveDefer(v, q)
	case *tree.Send:
		return r.receiveSend(v, q)
	case *tree.Goto:
		return r.receiveGoto(v, q)
	case *tree.Fallthrough:
		return v
	case *tree.Composite:
		return r.receiveComposite(v, q)
	case *tree.KeyValue:
		return r.receiveKeyValue(v, q)
	case *tree.Slice:
		return r.receiveSlice(v, q)
	case *tree.MapType:
		return r.receiveMapType(v, q)
	case *tree.Channel:
		return r.receiveChannel(v, q)
	case *tree.FuncType:
		return r.receiveFuncType(v, q)
	case *tree.StructType:
		return r.receiveStructType(v, q)
	case *tree.InterfaceType:
		return r.receiveInterfaceType(v, q)
	case *tree.TypeList:
		return r.receiveTypeList(v, q)
	case *tree.TypeDecl:
		return r.receiveTypeDecl(v, q)
	case *tree.MultiAssignment:
		return r.receiveMultiAssignment(v, q)
	case *tree.CommClause:
		return r.receiveCommClause(v, q)
	case *tree.IndexList:
		return r.receiveIndexList(v, q)

	// J nodes
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
		panic(fmt.Sprintf("GoReceiver: unsupported node type %T", node))
	}
}

func (r *GoReceiver) preVisit(node any, q *ReceiveQueue) any {
	// ID
	q.Receive(extractID(node), nil)
	// Prefix
	q.Receive(nodePrefix(node), func(v any) any {
		return receiveSpace(v.(tree.Space), q)
	})
	// Markers
	q.Receive(nodeMarkers(node), func(v any) any {
		return receiveMarkersCodec(q, v.(tree.Markers))
	})
	return node
}

// receiveType receives a JavaType from the queue with null/Unknown handling.
func (r *GoReceiver) receiveType(before tree.JavaType, q *ReceiveQueue) tree.JavaType {
	result := q.Receive(before, func(v any) any {
		return r.typeReceiver.VisitType(v.(tree.JavaType), q)
	})
	if result == nil {
		return nil
	}
	return result.(tree.JavaType)
}

// --- G nodes ---

func (r *GoReceiver) receiveCompilationUnit(cu *tree.CompilationUnit, q *ReceiveQueue) *tree.CompilationUnit {
	cu.SourcePath = receiveScalar[string](q, cu.SourcePath)
	q.Receive(nil, nil) // charset
	q.Receive(nil, nil) // charsetBomMarked
	q.Receive(nil, nil) // checksum
	q.Receive(nil, nil) // fileAttributes
	// packageDecl
	q.Receive(cu.PackageDecl, func(v any) any { return receiveRightPadded(r, q, v) })
	// imports
	q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	// statements
	q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	// EOF
	cu.EOF = receiveSpace(cu.EOF, q)
	return cu
}

func (r *GoReceiver) receiveGoStmt(gs *tree.GoStmt, q *ReceiveQueue) *tree.GoStmt {
	result := q.Receive(gs.Expr, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		gs.Expr = result.(tree.Expression)
	}
	return gs
}

func (r *GoReceiver) receiveDefer(d *tree.Defer, q *ReceiveQueue) *tree.Defer {
	result := q.Receive(d.Expr, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		d.Expr = result.(tree.Expression)
	}
	return d
}

func (r *GoReceiver) receiveSend(sn *tree.Send, q *ReceiveQueue) *tree.Send {
	result := q.Receive(sn.Channel, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		sn.Channel = result.(tree.Expression)
	}
	q.Receive(sn.Arrow, func(v any) any { return receiveLeftPadded(r, q, v) })
	return sn
}

func (r *GoReceiver) receiveGoto(g *tree.Goto, q *ReceiveQueue) *tree.Goto {
	result := q.Receive(g.Label, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		g.Label = result.(*tree.Identifier)
	}
	return g
}

func (r *GoReceiver) receiveComposite(c *tree.Composite, q *ReceiveQueue) *tree.Composite {
	result := q.Receive(c.TypeExpr, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		c.TypeExpr = result.(tree.Expression)
	}
	q.Receive(c.Elements, func(v any) any { return receiveContainer(r, q, v) })
	return c
}

func (r *GoReceiver) receiveKeyValue(kv *tree.KeyValue, q *ReceiveQueue) *tree.KeyValue {
	result := q.Receive(kv.Key, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		kv.Key = result.(tree.Expression)
	}
	q.Receive(kv.Value, func(v any) any { return receiveLeftPadded(r, q, v) })
	return kv
}

func (r *GoReceiver) receiveSlice(sl *tree.Slice, q *ReceiveQueue) *tree.Slice {
	result := q.Receive(sl.Indexed, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		sl.Indexed = result.(tree.Expression)
	}
	sl.OpenBracket = receiveSpace(sl.OpenBracket, q)
	q.Receive(sl.Low, func(v any) any { return receiveRightPadded(r, q, v) })
	q.Receive(sl.High, func(v any) any { return receiveRightPadded(r, q, v) })
	max := q.Receive(sl.Max, func(v any) any { return r.Visit(v, q) })
	if max != nil {
		sl.Max = max.(tree.Expression)
	}
	sl.CloseBracket = receiveSpace(sl.CloseBracket, q)
	return sl
}

func (r *GoReceiver) receiveMapType(mt *tree.MapType, q *ReceiveQueue) *tree.MapType {
	mt.OpenBracket = receiveSpace(mt.OpenBracket, q)
	q.Receive(mt.Key, func(v any) any { return receiveRightPadded(r, q, v) })
	result := q.Receive(mt.Value, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		mt.Value = result.(tree.Expression)
	}
	return mt
}

func (r *GoReceiver) receiveChannel(ch *tree.Channel, q *ReceiveQueue) *tree.Channel {
	dirStr := receiveScalar[string](q, "")
	switch dirStr {
	case "BIDI":
		ch.Dir = tree.ChanBidi
	case "SEND_ONLY":
		ch.Dir = tree.ChanSendOnly
	case "RECV_ONLY":
		ch.Dir = tree.ChanRecvOnly
	}
	result := q.Receive(ch.Value, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		ch.Value = result.(tree.Expression)
	}
	return ch
}

func (r *GoReceiver) receiveFuncType(ft *tree.FuncType, q *ReceiveQueue) *tree.FuncType {
	q.Receive(ft.Parameters, func(v any) any { return receiveContainer(r, q, v) })
	result := q.Receive(ft.ReturnType, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		ft.ReturnType = result.(tree.Expression)
	}
	return ft
}

func (r *GoReceiver) receiveStructType(st *tree.StructType, q *ReceiveQueue) *tree.StructType {
	result := q.Receive(st.Body, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		st.Body = result.(*tree.Block)
	}
	return st
}

func (r *GoReceiver) receiveInterfaceType(it *tree.InterfaceType, q *ReceiveQueue) *tree.InterfaceType {
	result := q.Receive(it.Body, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		it.Body = result.(*tree.Block)
	}
	return it
}

func (r *GoReceiver) receiveTypeList(tl *tree.TypeList, q *ReceiveQueue) *tree.TypeList {
	q.Receive(tl.Types, func(v any) any { return receiveContainer(r, q, v) })
	return tl
}

func (r *GoReceiver) receiveTypeDecl(td *tree.TypeDecl, q *ReceiveQueue) *tree.TypeDecl {
	result := q.Receive(td.Name, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		td.Name = result.(*tree.Identifier)
	}
	q.Receive(td.Assign, func(v any) any { return receiveLeftPadded(r, q, v) })
	defResult := q.Receive(td.Definition, func(v any) any { return r.Visit(v, q) })
	if defResult != nil {
		td.Definition = defResult.(tree.Expression)
	}
	q.Receive(td.Specs, func(v any) any { return receiveContainer(r, q, v) })
	return td
}

func (r *GoReceiver) receiveMultiAssignment(ma *tree.MultiAssignment, q *ReceiveQueue) *tree.MultiAssignment {
	q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	q.Receive(ma.Operator, func(v any) any { return receiveLeftPadded(r, q, v) })
	q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	return ma
}

func (r *GoReceiver) receiveCommClause(cc *tree.CommClause, q *ReceiveQueue) *tree.CommClause {
	result := q.Receive(cc.Comm, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		cc.Comm = result.(tree.Statement)
	}
	cc.Colon = receiveSpace(cc.Colon, q)
	q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	return cc
}

func (r *GoReceiver) receiveIndexList(il *tree.IndexList, q *ReceiveQueue) *tree.IndexList {
	result := q.Receive(il.Target, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		il.Target = result.(tree.Expression)
	}
	q.Receive(il.Indices, func(v any) any { return receiveContainer(r, q, v) })
	return il
}

// --- J nodes ---

func (r *GoReceiver) receiveIdentifier(id *tree.Identifier, q *ReceiveQueue) *tree.Identifier {
	// annotations
	q.ReceiveList(nil, func(v any) any { return r.Visit(v, q) })
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

func (r *GoReceiver) receiveLiteral(lit *tree.Literal, q *ReceiveQueue) *tree.Literal {
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

func (r *GoReceiver) receiveBinary(b *tree.Binary, q *ReceiveQueue) *tree.Binary {
	result := q.Receive(b.Left, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		b.Left = result.(tree.Expression)
	}
	q.Receive(b.Operator, func(v any) any { return receiveLeftPadded(r, q, v) })
	rightResult := q.Receive(b.Right, func(v any) any { return r.Visit(v, q) })
	if rightResult != nil {
		b.Right = rightResult.(tree.Expression)
	}
	b.Type = r.receiveType(b.Type, q)
	return b
}

func (r *GoReceiver) receiveBlock(b *tree.Block, q *ReceiveQueue) *tree.Block {
	// static (right-padded)
	q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	// statements
	q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	// end space
	b.End = receiveSpace(b.End, q)
	return b
}

func (r *GoReceiver) receiveUnary(u *tree.Unary, q *ReceiveQueue) *tree.Unary {
	q.Receive(u.Operator, func(v any) any { return receiveLeftPadded(r, q, v) })
	result := q.Receive(u.Operand, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		u.Operand = result.(tree.Expression)
	}
	u.Type = r.receiveType(u.Type, q)
	return u
}

func (r *GoReceiver) receiveFieldAccess(fa *tree.FieldAccess, q *ReceiveQueue) *tree.FieldAccess {
	result := q.Receive(fa.Target, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		fa.Target = result.(tree.Expression)
	}
	q.Receive(fa.Name, func(v any) any { return receiveLeftPadded(r, q, v) })
	fa.Type = r.receiveType(fa.Type, q)
	return fa
}

func (r *GoReceiver) receiveMethodInvocation(mi *tree.MethodInvocation, q *ReceiveQueue) *tree.MethodInvocation {
	// select
	q.Receive(mi.Select, func(v any) any { return receiveRightPadded(r, q, v) })
	// typeParameters (nil for Go)
	q.Receive(nil, nil)
	// name
	result := q.Receive(mi.Name, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		mi.Name = result.(*tree.Identifier)
	}
	// arguments
	q.Receive(mi.Arguments, func(v any) any { return receiveContainer(r, q, v) })
	// methodType
	mt := r.receiveType(mi.MethodType, q)
	if mt != nil {
		mi.MethodType = mt.(*tree.JavaTypeMethod)
	} else {
		mi.MethodType = nil
	}
	return mi
}

func (r *GoReceiver) receiveAssignment(a *tree.Assignment, q *ReceiveQueue) *tree.Assignment {
	result := q.Receive(a.Variable, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		a.Variable = result.(tree.Expression)
	}
	q.Receive(a.Value, func(v any) any { return receiveLeftPadded(r, q, v) })
	a.Type = r.receiveType(a.Type, q)
	return a
}

func (r *GoReceiver) receiveAssignmentOperation(a *tree.AssignmentOperation, q *ReceiveQueue) *tree.AssignmentOperation {
	result := q.Receive(a.Variable, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		a.Variable = result.(tree.Expression)
	}
	q.Receive(a.Operator, func(v any) any { return receiveLeftPadded(r, q, v) })
	assignResult := q.Receive(a.Assignment, func(v any) any { return r.Visit(v, q) })
	if assignResult != nil {
		a.Assignment = assignResult.(tree.Expression)
	}
	a.Type = r.receiveType(a.Type, q)
	return a
}

func (r *GoReceiver) receiveMethodDeclaration(md *tree.MethodDeclaration, q *ReceiveQueue) *tree.MethodDeclaration {
	// leadingAnnotations
	q.ReceiveList(nil, nil)
	// modifiers
	q.ReceiveList(nil, nil)
	// typeParameters
	q.Receive(nil, nil)
	// returnTypeExpression
	result := q.Receive(md.ReturnType, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		md.ReturnType = result.(tree.Expression)
	}
	// name annotations
	q.ReceiveList(nil, nil)
	// name
	nameResult := q.Receive(md.Name, func(v any) any { return r.Visit(v, q) })
	if nameResult != nil {
		md.Name = nameResult.(*tree.Identifier)
	}
	// parameters
	q.Receive(md.Parameters, func(v any) any { return receiveContainer(r, q, v) })
	// throws
	q.Receive(nil, nil)
	// body
	bodyResult := q.Receive(md.Body, func(v any) any { return r.Visit(v, q) })
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

func (r *GoReceiver) receiveVariableDeclarations(vd *tree.VariableDeclarations, q *ReceiveQueue) *tree.VariableDeclarations {
	// leadingAnnotations
	q.ReceiveList(nil, nil)
	// modifiers
	q.ReceiveList(nil, nil)
	// typeExpression
	result := q.Receive(vd.TypeExpr, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		vd.TypeExpr = result.(tree.Expression)
	}
	// varargs
	q.Receive(nil, nil)
	// variables
	q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	return vd
}

func (r *GoReceiver) receiveVariableDeclarator(vd *tree.VariableDeclarator, q *ReceiveQueue) *tree.VariableDeclarator {
	// declarator (name)
	result := q.Receive(vd.Name, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		vd.Name = result.(*tree.Identifier)
	}
	// dimensionsAfterName
	q.ReceiveList(nil, nil)
	// initializer
	q.Receive(vd.Initializer, func(v any) any { return receiveLeftPadded(r, q, v) })
	// variableType
	q.Receive(nil, nil)
	return vd
}

func (r *GoReceiver) receiveReturn(ret *tree.Return, q *ReceiveQueue) *tree.Return {
	q.Receive(nil, func(v any) any { return r.Visit(v, q) })
	return ret
}

func (r *GoReceiver) receiveIf(i *tree.If, q *ReceiveQueue) *tree.If {
	// ifCondition - Java sends ControlParentheses, extract inner Expression
	cpResult := q.Receive(nil, func(v any) any { return r.Visit(v, q) })
	if cpResult != nil {
		if cp, ok := cpResult.(*tree.ControlParentheses); ok {
			i.Condition = cp.Tree.Element
		} else {
			i.Condition = cpResult.(tree.Expression)
		}
	}
	// thenPart
	q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	// elsePart - Java sends Else node, convert to RightPadded
	elseResult := q.Receive(nil, func(v any) any { return r.Visit(v, q) })
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

func (r *GoReceiver) receiveElse(el *tree.Else, q *ReceiveQueue) *tree.Else {
	q.Receive(el.Body, func(v any) any { return receiveRightPadded(r, q, v) })
	return el
}

func (r *GoReceiver) receiveForLoop(f *tree.ForLoop, q *ReceiveQueue) *tree.ForLoop {
	ctrl := &f.Control
	result := q.Receive(ctrl, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		f.Control = *result.(*tree.ForControl)
	}
	q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	return f
}

func (r *GoReceiver) receiveForControl(fc *tree.ForControl, q *ReceiveQueue) *tree.ForControl {
	// init (list of right-padded)
	initList := q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	if len(initList) > 0 {
		rp := initList[0].(tree.RightPadded[tree.Statement])
		fc.Init = &rp
	}
	// condition (right-padded)
	q.Receive(fc.Condition, func(v any) any { return receiveRightPadded(r, q, v) })
	// update (list of right-padded)
	updateList := q.ReceiveList(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	if len(updateList) > 0 {
		rp := updateList[0].(tree.RightPadded[tree.Statement])
		fc.Update = &rp
	}
	return fc
}

func (r *GoReceiver) receiveForEachLoop(f *tree.ForEachLoop, q *ReceiveQueue) *tree.ForEachLoop {
	ctrl := &f.Control
	result := q.Receive(ctrl, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		f.Control = *result.(*tree.ForEachControl)
	}
	q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	return f
}

func (r *GoReceiver) receiveForEachControl(fc *tree.ForEachControl, q *ReceiveQueue) *tree.ForEachControl {
	// key (right-padded, nullable)
	q.Receive(fc.Key, func(v any) any { return receiveRightPadded(r, q, v) })
	// value (right-padded, nullable)
	q.Receive(fc.Value, func(v any) any { return receiveRightPadded(r, q, v) })
	// operator (left-padded AssignOp as string)
	q.Receive(fc.Operator, func(v any) any { return receiveLeftPadded(r, q, v) })
	// iterable
	result := q.Receive(fc.Iterable, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		fc.Iterable = result.(tree.Expression)
	}
	return fc
}

func (r *GoReceiver) receiveSwitch(sw *tree.Switch, q *ReceiveQueue) *tree.Switch {
	// selector - Java sends ControlParentheses, extract inner Expression for Tag
	cpResult := q.Receive(nil, func(v any) any { return r.Visit(v, q) })
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
	result := q.Receive(sw.Body, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		sw.Body = result.(*tree.Block)
	}
	return sw
}

func (r *GoReceiver) receiveCase(c *tree.Case, q *ReceiveQueue) *tree.Case {
	q.Receive(nil, nil) // type enum
	q.Receive(c.Expressions, func(v any) any { return receiveContainer(r, q, v) })
	q.Receive(nil, func(v any) any { return receiveContainer(r, q, v) }) // statements
	q.Receive(nil, nil) // body
	q.Receive(nil, nil) // guard
	return c
}

func (r *GoReceiver) receiveBreak(b *tree.Break, q *ReceiveQueue) *tree.Break {
	result := q.Receive(b.Label, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		b.Label = result.(*tree.Identifier)
	}
	return b
}

func (r *GoReceiver) receiveContinue(c *tree.Continue, q *ReceiveQueue) *tree.Continue {
	result := q.Receive(c.Label, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		c.Label = result.(*tree.Identifier)
	}
	return c
}

func (r *GoReceiver) receiveLabel(l *tree.Label, q *ReceiveQueue) *tree.Label {
	q.Receive(l.Name, func(v any) any { return receiveRightPadded(r, q, v) })
	result := q.Receive(l.Statement, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		l.Statement = result.(tree.Statement)
	}
	return l
}

func (r *GoReceiver) receiveArrayType(at *tree.ArrayType, q *ReceiveQueue) *tree.ArrayType {
	result := q.Receive(at.ElementType, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		at.ElementType = result.(tree.Expression)
	}
	q.ReceiveList(nil, nil) // annotations
	q.Receive(at.Dimension, func(v any) any { return receiveLeftPadded(r, q, v) })
	at.Type = r.receiveType(at.Type, q)
	return at
}

func (r *GoReceiver) receiveArrayAccess(aa *tree.ArrayAccess, q *ReceiveQueue) *tree.ArrayAccess {
	result := q.Receive(aa.Indexed, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		aa.Indexed = result.(tree.Expression)
	}
	dimResult := q.Receive(aa.Dimension, func(v any) any { return r.Visit(v, q) })
	if dimResult != nil {
		aa.Dimension = dimResult.(*tree.ArrayDimension)
	}
	return aa
}

func (r *GoReceiver) receiveArrayDimension(ad *tree.ArrayDimension, q *ReceiveQueue) *tree.ArrayDimension {
	q.Receive(ad.Index, func(v any) any { return receiveRightPadded(r, q, v) })
	return ad
}

func (r *GoReceiver) receiveParentheses(p *tree.Parentheses, q *ReceiveQueue) *tree.Parentheses {
	q.Receive(p.Tree, func(v any) any { return receiveRightPadded(r, q, v) })
	return p
}

func (r *GoReceiver) receiveTypeCast(tc *tree.TypeCast, q *ReceiveQueue) *tree.TypeCast {
	result := q.Receive(tc.Clazz, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		tc.Clazz = result.(*tree.ControlParentheses)
	}
	exprResult := q.Receive(tc.Expr, func(v any) any { return r.Visit(v, q) })
	if exprResult != nil {
		tc.Expr = exprResult.(tree.Expression)
	}
	return tc
}

func (r *GoReceiver) receiveControlParentheses(cp *tree.ControlParentheses, q *ReceiveQueue) *tree.ControlParentheses {
	q.Receive(cp.Tree, func(v any) any { return receiveRightPadded(r, q, v) })
	return cp
}

func (r *GoReceiver) receiveImport(imp *tree.Import, q *ReceiveQueue) *tree.Import {
	// static (always false for Go, but must receive full LeftPadded protocol)
	staticBefore := tree.LeftPadded[bool]{Before: tree.EmptySpace, Element: false}
	q.Receive(staticBefore, func(v any) any { return receiveLeftPadded(r, q, v) })
	// qualid (Expression - could be Literal or FieldAccess depending on direction)
	result := q.Receive(imp.Qualid, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		imp.Qualid = result.(tree.Expression)
	}
	// alias
	q.Receive(imp.Alias, func(v any) any { return receiveLeftPadded(r, q, v) })
	return imp
}

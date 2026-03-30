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

package visitor

import "github.com/openrewrite/rewrite/pkg/tree"

// GoVisitor traverses and optionally transforms an OpenRewrite LST.
// Embed GoVisitor in a struct and override visit methods to customize behavior.
// Set Self to the outer struct to enable virtual dispatch.
type GoVisitor struct {
	// Self must point to the outermost embedding struct for virtual dispatch.
	// If nil, dispatches to the default implementations on GoVisitor itself.
	Self interface{}

	cursor *Cursor
}

// Visit dispatches to the appropriate visit method based on the node's concrete type.
func (v *GoVisitor) Visit(t tree.Tree, p any) tree.Tree {
	if t == nil {
		return nil
	}

	v.cursor = NewCursor(v.cursor, t)
	defer func() { v.cursor = v.cursor.parent }()

	switch n := t.(type) {
	case *tree.CompilationUnit:
		return v.self().VisitCompilationUnit(n, p)
	case *tree.Identifier:
		return v.self().VisitIdentifier(n, p)
	case *tree.Literal:
		return v.self().VisitLiteral(n, p)
	case *tree.Binary:
		return v.self().VisitBinary(n, p)
	case *tree.Block:
		return v.self().VisitBlock(n, p)
	case *tree.Return:
		return v.self().VisitReturn(n, p)
	case *tree.If:
		return v.self().VisitIf(n, p)
	case *tree.Assignment:
		return v.self().VisitAssignment(n, p)
	case *tree.MethodDeclaration:
		return v.self().VisitMethodDeclaration(n, p)
	case *tree.FieldAccess:
		return v.self().VisitFieldAccess(n, p)
	case *tree.MethodInvocation:
		return v.self().VisitMethodInvocation(n, p)
	case *tree.VariableDeclarations:
		return v.self().VisitVariableDeclarations(n, p)
	case *tree.VariableDeclarator:
		return v.self().VisitVariableDeclarator(n, p)
	case *tree.Import:
		return v.self().VisitImport(n, p)
	case *tree.Unary:
		return v.self().VisitUnary(n, p)
	case *tree.AssignmentOperation:
		return v.self().VisitAssignmentOperation(n, p)
	case *tree.Switch:
		return v.self().VisitSwitch(n, p)
	case *tree.Case:
		return v.self().VisitCase(n, p)
	case *tree.ForLoop:
		return v.self().VisitForLoop(n, p)
	case *tree.ForControl:
		return v.self().VisitForControl(n, p)
	case *tree.ForEachLoop:
		return v.self().VisitForEachLoop(n, p)
	case *tree.ForEachControl:
		return v.self().VisitForEachControl(n, p)
	case *tree.Break:
		return v.self().VisitBreak(n, p)
	case *tree.Continue:
		return v.self().VisitContinue(n, p)
	case *tree.Label:
		return v.self().VisitLabel(n, p)
	case *tree.GoStmt:
		return v.self().VisitGoStmt(n, p)
	case *tree.Defer:
		return v.self().VisitDefer(n, p)
	case *tree.Send:
		return v.self().VisitSend(n, p)
	case *tree.Goto:
		return v.self().VisitGoto(n, p)
	case *tree.Fallthrough:
		return v.self().VisitFallthrough(n, p)
	case *tree.Empty:
		return v.self().VisitEmpty(n, p)
	case *tree.ArrayType:
		return v.self().VisitArrayType(n, p)
	case *tree.Parentheses:
		return v.self().VisitParentheses(n, p)
	case *tree.TypeCast:
		return v.self().VisitTypeCast(n, p)
	case *tree.ControlParentheses:
		return v.self().VisitControlParentheses(n, p)
	case *tree.ArrayAccess:
		return v.self().VisitArrayAccess(n, p)
	case *tree.IndexList:
		return v.self().VisitIndexList(n, p)
	case *tree.ArrayDimension:
		return v.self().VisitArrayDimension(n, p)
	case *tree.Composite:
		return v.self().VisitComposite(n, p)
	case *tree.KeyValue:
		return v.self().VisitKeyValue(n, p)
	case *tree.Slice:
		return v.self().VisitSlice(n, p)
	case *tree.MapType:
		return v.self().VisitMapType(n, p)
	case *tree.Channel:
		return v.self().VisitChannel(n, p)
	case *tree.FuncType:
		return v.self().VisitFuncType(n, p)
	case *tree.TypeList:
		return v.self().VisitTypeList(n, p)
	case *tree.TypeDecl:
		return v.self().VisitTypeDecl(n, p)
	case *tree.StructType:
		return v.self().VisitStructType(n, p)
	case *tree.InterfaceType:
		return v.self().VisitInterfaceType(n, p)
	case *tree.MultiAssignment:
		return v.self().VisitMultiAssignment(n, p)
	case *tree.CommClause:
		return v.self().VisitCommClause(n, p)
	default:
		return t
	}
}

func (v *GoVisitor) self() VisitorI {
	if v.Self != nil {
		if vi, ok := v.Self.(VisitorI); ok {
			return vi
		}
	}
	return v
}

// VisitorI defines all overridable visit methods.
type VisitorI interface {
	Visit(t tree.Tree, p any) tree.Tree
	VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J
	VisitIdentifier(ident *tree.Identifier, p any) tree.J
	VisitLiteral(lit *tree.Literal, p any) tree.J
	VisitBinary(bin *tree.Binary, p any) tree.J
	VisitBlock(block *tree.Block, p any) tree.J
	VisitReturn(ret *tree.Return, p any) tree.J
	VisitIf(ifStmt *tree.If, p any) tree.J
	VisitAssignment(assign *tree.Assignment, p any) tree.J
	VisitMethodDeclaration(md *tree.MethodDeclaration, p any) tree.J
	VisitFieldAccess(fa *tree.FieldAccess, p any) tree.J
	VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J
	VisitVariableDeclarations(vd *tree.VariableDeclarations, p any) tree.J
	VisitVariableDeclarator(vd *tree.VariableDeclarator, p any) tree.J
	VisitImport(imp *tree.Import, p any) tree.J
	VisitUnary(unary *tree.Unary, p any) tree.J
	VisitAssignmentOperation(ao *tree.AssignmentOperation, p any) tree.J
	VisitSwitch(sw *tree.Switch, p any) tree.J
	VisitCase(c *tree.Case, p any) tree.J
	VisitForLoop(forLoop *tree.ForLoop, p any) tree.J
	VisitForControl(control *tree.ForControl, p any) tree.J
	VisitForEachLoop(forEach *tree.ForEachLoop, p any) tree.J
	VisitForEachControl(control *tree.ForEachControl, p any) tree.J
	VisitBreak(b *tree.Break, p any) tree.J
	VisitContinue(c *tree.Continue, p any) tree.J
	VisitLabel(l *tree.Label, p any) tree.J
	VisitGoStmt(g *tree.GoStmt, p any) tree.J
	VisitDefer(d *tree.Defer, p any) tree.J
	VisitSend(s *tree.Send, p any) tree.J
	VisitGoto(g *tree.Goto, p any) tree.J
	VisitFallthrough(f *tree.Fallthrough, p any) tree.J
	VisitEmpty(empty *tree.Empty, p any) tree.J
	VisitArrayType(at *tree.ArrayType, p any) tree.J
	VisitParentheses(paren *tree.Parentheses, p any) tree.J
	VisitTypeCast(tc *tree.TypeCast, p any) tree.J
	VisitControlParentheses(cp *tree.ControlParentheses, p any) tree.J
	VisitArrayAccess(aa *tree.ArrayAccess, p any) tree.J
	VisitIndexList(il *tree.IndexList, p any) tree.J
	VisitArrayDimension(ad *tree.ArrayDimension, p any) tree.J
	VisitComposite(c *tree.Composite, p any) tree.J
	VisitKeyValue(kv *tree.KeyValue, p any) tree.J
	VisitSlice(s *tree.Slice, p any) tree.J
	VisitMapType(mt *tree.MapType, p any) tree.J
	VisitChannel(ch *tree.Channel, p any) tree.J
	VisitFuncType(ft *tree.FuncType, p any) tree.J
	VisitTypeList(tl *tree.TypeList, p any) tree.J
	VisitTypeDecl(td *tree.TypeDecl, p any) tree.J
	VisitStructType(st *tree.StructType, p any) tree.J
	VisitInterfaceType(it *tree.InterfaceType, p any) tree.J
	VisitMultiAssignment(ma *tree.MultiAssignment, p any) tree.J
	VisitCommClause(cc *tree.CommClause, p any) tree.J
	VisitSpace(space tree.Space, p any) tree.Space
	VisitType(javaType tree.JavaType, p any) tree.JavaType
}

// Ensure GoVisitor satisfies VisitorI.
var _ VisitorI = (*GoVisitor)(nil)

// --- Default visit implementations ---

func (v *GoVisitor) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	cu = cu.WithPrefix(v.self().VisitSpace(cu.Prefix, p))
	cu = cu.WithMarkers(v.visitMarkers(cu.Markers, p))
	cu = cu.WithStatements(visitRightPaddedList(v, cu.Statements, p))
	cu = cu.WithEOF(v.self().VisitSpace(cu.EOF, p))
	return cu
}

func (v *GoVisitor) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	ident = ident.WithPrefix(v.self().VisitSpace(ident.Prefix, p))
	ident = ident.WithMarkers(v.visitMarkers(ident.Markers, p))
	return ident
}

func (v *GoVisitor) VisitLiteral(lit *tree.Literal, p any) tree.J {
	lit = lit.WithPrefix(v.self().VisitSpace(lit.Prefix, p))
	lit = lit.WithMarkers(v.visitMarkers(lit.Markers, p))
	return lit
}

func (v *GoVisitor) VisitBinary(bin *tree.Binary, p any) tree.J {
	bin = bin.WithPrefix(v.self().VisitSpace(bin.Prefix, p))
	bin = bin.WithMarkers(v.visitMarkers(bin.Markers, p))
	bin = bin.WithLeft(visitExpression(v, bin.Left, p))
	bin = bin.WithRight(visitExpression(v, bin.Right, p))
	return bin
}

func (v *GoVisitor) VisitBlock(block *tree.Block, p any) tree.J {
	block = block.WithPrefix(v.self().VisitSpace(block.Prefix, p))
	block = block.WithMarkers(v.visitMarkers(block.Markers, p))
	block = block.WithStatements(visitRightPaddedList(v, block.Statements, p))
	block = block.WithEnd(v.self().VisitSpace(block.End, p))
	return block
}

func (v *GoVisitor) VisitReturn(ret *tree.Return, p any) tree.J {
	ret = ret.WithPrefix(v.self().VisitSpace(ret.Prefix, p))
	ret = ret.WithMarkers(v.visitMarkers(ret.Markers, p))
	return ret
}

func (v *GoVisitor) VisitIf(ifStmt *tree.If, p any) tree.J {
	ifStmt = ifStmt.WithPrefix(v.self().VisitSpace(ifStmt.Prefix, p))
	ifStmt = ifStmt.WithMarkers(v.visitMarkers(ifStmt.Markers, p))
	ifStmt = ifStmt.WithCondition(visitExpression(v, ifStmt.Condition, p))
	ifStmt = ifStmt.WithThen(visitAndCast[*tree.Block](v, ifStmt.Then, p))
	return ifStmt
}

func (v *GoVisitor) VisitAssignment(assign *tree.Assignment, p any) tree.J {
	assign = assign.WithPrefix(v.self().VisitSpace(assign.Prefix, p))
	assign = assign.WithMarkers(v.visitMarkers(assign.Markers, p))
	assign = assign.WithVariable(visitExpression(v, assign.Variable, p))
	return assign
}

func (v *GoVisitor) VisitMethodDeclaration(md *tree.MethodDeclaration, p any) tree.J {
	md = md.WithPrefix(v.self().VisitSpace(md.Prefix, p))
	md = md.WithMarkers(v.visitMarkers(md.Markers, p))
	md = md.WithName(visitAndCast[*tree.Identifier](v, md.Name, p))
	if md.Body != nil {
		md = md.WithBody(visitAndCast[*tree.Block](v, md.Body, p))
	}
	return md
}

func (v *GoVisitor) VisitFieldAccess(fa *tree.FieldAccess, p any) tree.J {
	fa = fa.WithPrefix(v.self().VisitSpace(fa.Prefix, p))
	fa = fa.WithMarkers(v.visitMarkers(fa.Markers, p))
	fa = fa.WithTarget(visitExpression(v, fa.Target, p))
	return fa
}

func (v *GoVisitor) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
	mi = mi.WithPrefix(v.self().VisitSpace(mi.Prefix, p))
	mi = mi.WithMarkers(v.visitMarkers(mi.Markers, p))
	mi = mi.WithName(visitAndCast[*tree.Identifier](v, mi.Name, p))
	return mi
}

func (v *GoVisitor) VisitVariableDeclarations(vd *tree.VariableDeclarations, p any) tree.J {
	vd = vd.WithPrefix(v.self().VisitSpace(vd.Prefix, p))
	vd = vd.WithMarkers(v.visitMarkers(vd.Markers, p))
	return vd
}

func (v *GoVisitor) VisitVariableDeclarator(vd *tree.VariableDeclarator, p any) tree.J {
	vd = vd.WithPrefix(v.self().VisitSpace(vd.Prefix, p))
	vd = vd.WithMarkers(v.visitMarkers(vd.Markers, p))
	vd = vd.WithName(visitAndCast[*tree.Identifier](v, vd.Name, p))
	return vd
}

func (v *GoVisitor) VisitImport(imp *tree.Import, p any) tree.J {
	imp = imp.WithPrefix(v.self().VisitSpace(imp.Prefix, p))
	imp = imp.WithMarkers(v.visitMarkers(imp.Markers, p))
	return imp
}

func (v *GoVisitor) VisitUnary(unary *tree.Unary, p any) tree.J {
	unary = unary.WithPrefix(v.self().VisitSpace(unary.Prefix, p))
	unary = unary.WithMarkers(v.visitMarkers(unary.Markers, p))
	unary = unary.WithOperand(visitExpression(v, unary.Operand, p))
	return unary
}

func (v *GoVisitor) VisitAssignmentOperation(ao *tree.AssignmentOperation, p any) tree.J {
	ao = ao.WithPrefix(v.self().VisitSpace(ao.Prefix, p))
	ao = ao.WithMarkers(v.visitMarkers(ao.Markers, p))
	ao = ao.WithVariable(visitExpression(v, ao.Variable, p))
	return ao
}

func (v *GoVisitor) VisitSwitch(sw *tree.Switch, p any) tree.J {
	sw = sw.WithPrefix(v.self().VisitSpace(sw.Prefix, p))
	sw = sw.WithMarkers(v.visitMarkers(sw.Markers, p))
	sw = sw.WithBody(visitAndCast[*tree.Block](v, sw.Body, p))
	return sw
}

func (v *GoVisitor) VisitCase(c *tree.Case, p any) tree.J {
	c = c.WithPrefix(v.self().VisitSpace(c.Prefix, p))
	c = c.WithMarkers(v.visitMarkers(c.Markers, p))
	return c
}

func (v *GoVisitor) VisitForLoop(forLoop *tree.ForLoop, p any) tree.J {
	forLoop = forLoop.WithPrefix(v.self().VisitSpace(forLoop.Prefix, p))
	forLoop = forLoop.WithMarkers(v.visitMarkers(forLoop.Markers, p))
	forLoop.Control = *visitAndCast[*tree.ForControl](v, &forLoop.Control, p)
	forLoop = forLoop.WithBody(visitAndCast[*tree.Block](v, forLoop.Body, p))
	return forLoop
}

func (v *GoVisitor) VisitForControl(control *tree.ForControl, p any) tree.J {
	control = control.WithPrefix(v.self().VisitSpace(control.Prefix, p))
	control = control.WithMarkers(v.visitMarkers(control.Markers, p))
	return control
}

func (v *GoVisitor) VisitForEachLoop(forEach *tree.ForEachLoop, p any) tree.J {
	forEach = forEach.WithPrefix(v.self().VisitSpace(forEach.Prefix, p))
	forEach = forEach.WithMarkers(v.visitMarkers(forEach.Markers, p))
	forEach = forEach.WithBody(visitAndCast[*tree.Block](v, forEach.Body, p))
	return forEach
}

func (v *GoVisitor) VisitForEachControl(control *tree.ForEachControl, p any) tree.J {
	control = control.WithPrefix(v.self().VisitSpace(control.Prefix, p))
	control = control.WithMarkers(v.visitMarkers(control.Markers, p))
	return control
}

func (v *GoVisitor) VisitBreak(b *tree.Break, p any) tree.J {
	b = b.WithPrefix(v.self().VisitSpace(b.Prefix, p))
	b = b.WithMarkers(v.visitMarkers(b.Markers, p))
	return b
}

func (v *GoVisitor) VisitContinue(c *tree.Continue, p any) tree.J {
	c = c.WithPrefix(v.self().VisitSpace(c.Prefix, p))
	c = c.WithMarkers(v.visitMarkers(c.Markers, p))
	return c
}

func (v *GoVisitor) VisitLabel(l *tree.Label, p any) tree.J {
	l = l.WithPrefix(v.self().VisitSpace(l.Prefix, p))
	l = l.WithMarkers(v.visitMarkers(l.Markers, p))
	return l
}

func (v *GoVisitor) VisitGoStmt(g *tree.GoStmt, p any) tree.J {
	g = g.WithPrefix(v.self().VisitSpace(g.Prefix, p))
	g = g.WithMarkers(v.visitMarkers(g.Markers, p))
	return g
}

func (v *GoVisitor) VisitDefer(d *tree.Defer, p any) tree.J {
	d = d.WithPrefix(v.self().VisitSpace(d.Prefix, p))
	d = d.WithMarkers(v.visitMarkers(d.Markers, p))
	return d
}

func (v *GoVisitor) VisitSend(s *tree.Send, p any) tree.J {
	s = s.WithPrefix(v.self().VisitSpace(s.Prefix, p))
	s = s.WithMarkers(v.visitMarkers(s.Markers, p))
	return s
}

func (v *GoVisitor) VisitGoto(g *tree.Goto, p any) tree.J {
	g = g.WithPrefix(v.self().VisitSpace(g.Prefix, p))
	g = g.WithMarkers(v.visitMarkers(g.Markers, p))
	return g
}

func (v *GoVisitor) VisitFallthrough(f *tree.Fallthrough, p any) tree.J {
	f = f.WithPrefix(v.self().VisitSpace(f.Prefix, p))
	f = f.WithMarkers(v.visitMarkers(f.Markers, p))
	return f
}

func (v *GoVisitor) VisitEmpty(empty *tree.Empty, p any) tree.J {
	empty = empty.WithPrefix(v.self().VisitSpace(empty.Prefix, p))
	return empty
}

func (v *GoVisitor) VisitArrayType(at *tree.ArrayType, p any) tree.J {
	at = at.WithPrefix(v.self().VisitSpace(at.Prefix, p))
	at = at.WithMarkers(v.visitMarkers(at.Markers, p))
	return at
}

func (v *GoVisitor) VisitParentheses(paren *tree.Parentheses, p any) tree.J {
	paren = paren.WithPrefix(v.self().VisitSpace(paren.Prefix, p))
	paren = paren.WithMarkers(v.visitMarkers(paren.Markers, p))
	return paren
}

func (v *GoVisitor) VisitTypeCast(tc *tree.TypeCast, p any) tree.J {
	tc = tc.WithPrefix(v.self().VisitSpace(tc.Prefix, p))
	tc = tc.WithMarkers(v.visitMarkers(tc.Markers, p))
	return tc
}

func (v *GoVisitor) VisitControlParentheses(cp *tree.ControlParentheses, p any) tree.J {
	cp = cp.WithPrefix(v.self().VisitSpace(cp.Prefix, p))
	cp = cp.WithMarkers(v.visitMarkers(cp.Markers, p))
	return cp
}

func (v *GoVisitor) VisitArrayAccess(aa *tree.ArrayAccess, p any) tree.J {
	aa = aa.WithPrefix(v.self().VisitSpace(aa.Prefix, p))
	aa = aa.WithMarkers(v.visitMarkers(aa.Markers, p))
	return aa
}

func (v *GoVisitor) VisitIndexList(il *tree.IndexList, p any) tree.J {
	il = il.WithPrefix(v.self().VisitSpace(il.Prefix, p))
	il = il.WithMarkers(v.visitMarkers(il.Markers, p))
	return il
}

func (v *GoVisitor) VisitArrayDimension(ad *tree.ArrayDimension, p any) tree.J {
	ad = ad.WithPrefix(v.self().VisitSpace(ad.Prefix, p))
	ad = ad.WithMarkers(v.visitMarkers(ad.Markers, p))
	return ad
}

func (v *GoVisitor) VisitComposite(c *tree.Composite, p any) tree.J {
	c = c.WithPrefix(v.self().VisitSpace(c.Prefix, p))
	c = c.WithMarkers(v.visitMarkers(c.Markers, p))
	return c
}

func (v *GoVisitor) VisitKeyValue(kv *tree.KeyValue, p any) tree.J {
	kv = kv.WithPrefix(v.self().VisitSpace(kv.Prefix, p))
	kv = kv.WithMarkers(v.visitMarkers(kv.Markers, p))
	return kv
}

func (v *GoVisitor) VisitSlice(s *tree.Slice, p any) tree.J {
	s = s.WithPrefix(v.self().VisitSpace(s.Prefix, p))
	s = s.WithMarkers(v.visitMarkers(s.Markers, p))
	return s
}

func (v *GoVisitor) VisitMapType(mt *tree.MapType, p any) tree.J {
	mt = mt.WithPrefix(v.self().VisitSpace(mt.Prefix, p))
	mt = mt.WithMarkers(v.visitMarkers(mt.Markers, p))
	return mt
}

func (v *GoVisitor) VisitChannel(ch *tree.Channel, p any) tree.J {
	ch = ch.WithPrefix(v.self().VisitSpace(ch.Prefix, p))
	ch = ch.WithMarkers(v.visitMarkers(ch.Markers, p))
	return ch
}

func (v *GoVisitor) VisitFuncType(ft *tree.FuncType, p any) tree.J {
	ft = ft.WithPrefix(v.self().VisitSpace(ft.Prefix, p))
	ft = ft.WithMarkers(v.visitMarkers(ft.Markers, p))
	return ft
}

func (v *GoVisitor) VisitTypeList(tl *tree.TypeList, p any) tree.J {
	tl = tl.WithPrefix(v.self().VisitSpace(tl.Prefix, p))
	tl = tl.WithMarkers(v.visitMarkers(tl.Markers, p))
	return tl
}

func (v *GoVisitor) VisitTypeDecl(td *tree.TypeDecl, p any) tree.J {
	td = td.WithPrefix(v.self().VisitSpace(td.Prefix, p))
	td = td.WithMarkers(v.visitMarkers(td.Markers, p))
	return td
}

func (v *GoVisitor) VisitStructType(st *tree.StructType, p any) tree.J {
	st = st.WithPrefix(v.self().VisitSpace(st.Prefix, p))
	st = st.WithMarkers(v.visitMarkers(st.Markers, p))
	return st
}

func (v *GoVisitor) VisitInterfaceType(it *tree.InterfaceType, p any) tree.J {
	it = it.WithPrefix(v.self().VisitSpace(it.Prefix, p))
	it = it.WithMarkers(v.visitMarkers(it.Markers, p))
	return it
}

func (v *GoVisitor) VisitMultiAssignment(ma *tree.MultiAssignment, p any) tree.J {
	ma = ma.WithPrefix(v.self().VisitSpace(ma.Prefix, p))
	ma = ma.WithMarkers(v.visitMarkers(ma.Markers, p))
	return ma
}

func (v *GoVisitor) VisitCommClause(cc *tree.CommClause, p any) tree.J {
	cc = cc.WithPrefix(v.self().VisitSpace(cc.Prefix, p))
	cc = cc.WithMarkers(v.visitMarkers(cc.Markers, p))
	return cc
}

func (v *GoVisitor) VisitSpace(space tree.Space, p any) tree.Space {
	return space
}

func (v *GoVisitor) VisitType(javaType tree.JavaType, p any) tree.JavaType {
	return javaType
}

func (v *GoVisitor) visitMarkers(markers tree.Markers, p any) tree.Markers {
	return markers
}

// --- Helper functions ---

func visitAndCast[T tree.Tree](v *GoVisitor, t tree.Tree, p any) T {
	result := v.self().Visit(t, p)
	return result.(T)
}

func visitExpression(v *GoVisitor, expr tree.Expression, p any) tree.Expression {
	result := v.self().Visit(expr, p)
	return result.(tree.Expression)
}

func visitRightPaddedList[T tree.J](v *GoVisitor, list []tree.RightPadded[T], p any) []tree.RightPadded[T] {
	result := make([]tree.RightPadded[T], 0, len(list))
	for _, rp := range list {
		visited := v.self().Visit(rp.Element, p)
		if visited == nil {
			continue
		}
		rp.Element = visited.(T)
		rp.After = v.self().VisitSpace(rp.After, p)
		result = append(result, rp)
	}
	return result
}

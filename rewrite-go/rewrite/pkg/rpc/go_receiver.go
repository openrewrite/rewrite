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

// GoReceiver deserializes Go AST nodes from the receive queue.
// Handles G (Go-specific) nodes and delegates J nodes to JavaReceiver.
type GoReceiver struct {
	java JavaReceiver
}

// NewGoReceiver creates a GoReceiver with its JavaReceiver properly wired.
func NewGoReceiver() *GoReceiver {
	gr := &GoReceiver{}
	gr.java = JavaReceiver{
		typeReceiver: NewJavaTypeReceiver(),
		parent:       gr,
	}
	return gr
}

// Visit dispatches to the appropriate receive method based on node type.
func (r *GoReceiver) Visit(node any, q *ReceiveQueue) any {
	if node == nil {
		return nil
	}

	// ParseError has its own codec — handle before preVisit (no prefix field)
	if pe, ok := node.(*tree.ParseError); ok {
		c := *pe
		return r.receiveParseError(&c, q)
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

	default:
		// Delegate all J nodes to JavaReceiver
		return r.java.visitJ(node, q)
	}
}

func (r *GoReceiver) preVisit(node any, q *ReceiveQueue) any {
	// shallow copy to avoid mutating remoteObjects baseline
	switch n := node.(type) {
	// G nodes
	case *tree.CompilationUnit:
		c := *n; node = &c
	case *tree.GoStmt:
		c := *n; node = &c
	case *tree.Defer:
		c := *n; node = &c
	case *tree.Send:
		c := *n; node = &c
	case *tree.Goto:
		c := *n; node = &c
	case *tree.Fallthrough:
		c := *n; node = &c
	case *tree.Composite:
		c := *n; node = &c
	case *tree.KeyValue:
		c := *n; node = &c
	case *tree.Slice:
		c := *n; node = &c
	case *tree.MapType:
		c := *n; node = &c
	case *tree.Channel:
		c := *n; node = &c
	case *tree.FuncType:
		c := *n; node = &c
	case *tree.StructType:
		c := *n; node = &c
	case *tree.InterfaceType:
		c := *n; node = &c
	case *tree.TypeList:
		c := *n; node = &c
	case *tree.TypeDecl:
		c := *n; node = &c
	case *tree.MultiAssignment:
		c := *n; node = &c
	case *tree.CommClause:
		c := *n; node = &c
	case *tree.IndexList:
		c := *n; node = &c
	// J nodes
	case *tree.Identifier:
		c := *n; node = &c
	case *tree.Literal:
		c := *n; node = &c
	case *tree.Binary:
		c := *n; node = &c
	case *tree.Block:
		c := *n; node = &c
	case *tree.Empty:
		c := *n; node = &c
	case *tree.Unary:
		c := *n; node = &c
	case *tree.FieldAccess:
		c := *n; node = &c
	case *tree.MethodInvocation:
		c := *n; node = &c
	case *tree.Assignment:
		c := *n; node = &c
	case *tree.AssignmentOperation:
		c := *n; node = &c
	case *tree.MethodDeclaration:
		c := *n; node = &c
	case *tree.VariableDeclarations:
		c := *n; node = &c
	case *tree.VariableDeclarator:
		c := *n; node = &c
	case *tree.Return:
		c := *n; node = &c
	case *tree.If:
		c := *n; node = &c
	case *tree.Else:
		c := *n; node = &c
	case *tree.ForLoop:
		c := *n; node = &c
	case *tree.ForControl:
		c := *n; node = &c
	case *tree.ForEachLoop:
		c := *n; node = &c
	case *tree.ForEachControl:
		c := *n; node = &c
	case *tree.Switch:
		c := *n; node = &c
	case *tree.Case:
		c := *n; node = &c
	case *tree.Break:
		c := *n; node = &c
	case *tree.Continue:
		c := *n; node = &c
	case *tree.Label:
		c := *n; node = &c
	case *tree.ArrayType:
		c := *n; node = &c
	case *tree.ArrayAccess:
		c := *n; node = &c
	case *tree.ArrayDimension:
		c := *n; node = &c
	case *tree.Parentheses:
		c := *n; node = &c
	case *tree.TypeCast:
		c := *n; node = &c
	case *tree.ControlParentheses:
		c := *n; node = &c
	case *tree.Import:
		c := *n; node = &c
	}

	// ID
	q.Receive(extractID(node), nil)
	// Prefix
	if result := q.Receive(nodePrefix(node), func(v any) any {
		return receiveSpace(v.(tree.Space), q)
	}); result != nil {
		setPrefix(node, result.(tree.Space))
	}
	// Markers
	if result := q.Receive(nodeMarkers(node), func(v any) any {
		return receiveMarkersCodec(q, v.(tree.Markers))
	}); result != nil {
		setMarkers(node, result.(tree.Markers))
	}
	return node
}

// --- G nodes ---

// receiveParseError deserializes a ParseError matching Java's ParseError.rpcReceive field order:
// id, markers, sourcePath, charsetName, charsetBomMarked, checksum, fileAttributes, text
func (r *GoReceiver) receiveParseError(pe *tree.ParseError, q *ReceiveQueue) *tree.ParseError {
	idStr := receiveScalar[string](q, pe.Ident.String())
	if idStr != "" {
		if parsed, err := uuid.Parse(idStr); err == nil {
			pe.Ident = parsed
		}
	}
	pe.Markers = receiveMarkersCodec(q, pe.Markers)
	pe.SourcePath = receiveScalar[string](q, pe.SourcePath)
	pe.CharsetName = receiveScalar[string](q, pe.CharsetName)
	pe.CharsetBomMarked = receiveScalar[bool](q, pe.CharsetBomMarked)
	q.Receive(nil, nil) // checksum
	q.Receive(nil, nil) // fileAttributes
	pe.Text = receiveScalar[string](q, pe.Text)
	return pe
}

func (r *GoReceiver) receiveCompilationUnit(cu *tree.CompilationUnit, q *ReceiveQueue) *tree.CompilationUnit {
	c := *cu // shallow copy to avoid mutating remoteObjects baseline
	cu = &c
	cu.SourcePath = receiveScalar[string](q, cu.SourcePath)
	q.Receive(nil, nil) // charset
	q.Receive(nil, nil) // charsetBomMarked
	q.Receive(nil, nil) // checksum
	q.Receive(nil, nil) // fileAttributes
	// packageDecl
	var beforePkgDecl any
	if cu.PackageDecl != nil {
		beforePkgDecl = *cu.PackageDecl
	}
	if result := q.Receive(beforePkgDecl, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		rp := coerceRightPaddedIdent(result)
		if rp.Element == nil {
		}
		cu.PackageDecl = &rp
	} else {
		cu.PackageDecl = nil
	}
	// imports (container)
	var beforeImports any
	if cu.Imports != nil {
		beforeImports = *cu.Imports
	}
	if result := q.Receive(beforeImports, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		c := result.(tree.Container[*tree.Import])
		cu.Imports = &c
	} else {
		cu.Imports = nil
	}
	// statements
	beforeStmts := make([]any, len(cu.Statements))
	for i, s := range cu.Statements {
		beforeStmts[i] = s
	}
	afterStmts := q.ReceiveList(beforeStmts, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterStmts != nil {
		cu.Statements = make([]tree.RightPadded[tree.Statement], len(afterStmts))
		for i, s := range afterStmts {
			cu.Statements[i] = coerceToStatementRP(s)
		}
	}
	// EOF
	cu.EOF = receiveSpace(cu.EOF, q)
	return cu
}

func (r *GoReceiver) receiveGoStmt(gs *tree.GoStmt, q *ReceiveQueue) *tree.GoStmt {
	c := *gs // shallow copy to avoid mutating remoteObjects baseline
	gs = &c
	result := q.Receive(gs.Expr, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		gs.Expr = result.(tree.Expression)
	}
	return gs
}

func (r *GoReceiver) receiveDefer(d *tree.Defer, q *ReceiveQueue) *tree.Defer {
	c := *d // shallow copy to avoid mutating remoteObjects baseline
	d = &c
	result := q.Receive(d.Expr, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		d.Expr = result.(tree.Expression)
	}
	return d
}

func (r *GoReceiver) receiveSend(sn *tree.Send, q *ReceiveQueue) *tree.Send {
	c := *sn // shallow copy to avoid mutating remoteObjects baseline
	sn = &c
	result := q.Receive(sn.Channel, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		sn.Channel = result.(tree.Expression)
	}
	if result := q.Receive(sn.Arrow, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		sn.Arrow = result.(tree.LeftPadded[tree.Expression])
	}
	return sn
}

func (r *GoReceiver) receiveGoto(g *tree.Goto, q *ReceiveQueue) *tree.Goto {
	c := *g // shallow copy to avoid mutating remoteObjects baseline
	g = &c
	result := q.Receive(g.Label, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		g.Label = result.(*tree.Identifier)
	}
	return g
}

func (r *GoReceiver) receiveComposite(comp *tree.Composite, q *ReceiveQueue) *tree.Composite {
	c := *comp // shallow copy to avoid mutating remoteObjects baseline
	comp = &c
	result := q.Receive(comp.TypeExpr, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		comp.TypeExpr = result.(tree.Expression)
	}
	if result := q.Receive(comp.Elements, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		comp.Elements = result.(tree.Container[tree.Expression])
	}
	return comp
}

func (r *GoReceiver) receiveKeyValue(kv *tree.KeyValue, q *ReceiveQueue) *tree.KeyValue {
	c := *kv // shallow copy to avoid mutating remoteObjects baseline
	kv = &c
	result := q.Receive(kv.Key, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		kv.Key = result.(tree.Expression)
	}
	if result := q.Receive(kv.Value, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		kv.Value = result.(tree.LeftPadded[tree.Expression])
	}
	return kv
}

func (r *GoReceiver) receiveSlice(sl *tree.Slice, q *ReceiveQueue) *tree.Slice {
	c := *sl // shallow copy to avoid mutating remoteObjects baseline
	sl = &c
	result := q.Receive(sl.Indexed, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		sl.Indexed = result.(tree.Expression)
	}
	sl.OpenBracket = receiveSpace(sl.OpenBracket, q)
	if result := q.Receive(sl.Low, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		sl.Low = coerceToExpressionRP(result)
	}
	if result := q.Receive(sl.High, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		sl.High = coerceToExpressionRP(result)
	}
	max := q.Receive(sl.Max, func(v any) any { return r.Visit(v, q) })
	if max != nil {
		sl.Max = max.(tree.Expression)
	}
	sl.CloseBracket = receiveSpace(sl.CloseBracket, q)
	return sl
}

func (r *GoReceiver) receiveMapType(mt *tree.MapType, q *ReceiveQueue) *tree.MapType {
	c := *mt // shallow copy to avoid mutating remoteObjects baseline
	mt = &c
	mt.OpenBracket = receiveSpace(mt.OpenBracket, q)
	if result := q.Receive(mt.Key, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		mt.Key = coerceToExpressionRP(result)
	}
	result := q.Receive(mt.Value, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		mt.Value = result.(tree.Expression)
	}
	return mt
}

func (r *GoReceiver) receiveChannel(ch *tree.Channel, q *ReceiveQueue) *tree.Channel {
	c := *ch // shallow copy to avoid mutating remoteObjects baseline
	ch = &c
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
	c := *ft // shallow copy to avoid mutating remoteObjects baseline
	ft = &c
	if result := q.Receive(ft.Parameters, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		ft.Parameters = result.(tree.Container[tree.Statement])
	}
	result := q.Receive(ft.ReturnType, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		ft.ReturnType = result.(tree.Expression)
	}
	return ft
}

func (r *GoReceiver) receiveStructType(st *tree.StructType, q *ReceiveQueue) *tree.StructType {
	c := *st // shallow copy to avoid mutating remoteObjects baseline
	st = &c
	result := q.Receive(st.Body, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		st.Body = result.(*tree.Block)
	}
	return st
}

func (r *GoReceiver) receiveInterfaceType(it *tree.InterfaceType, q *ReceiveQueue) *tree.InterfaceType {
	c := *it // shallow copy to avoid mutating remoteObjects baseline
	it = &c
	result := q.Receive(it.Body, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		it.Body = result.(*tree.Block)
	}
	return it
}

func (r *GoReceiver) receiveTypeList(tl *tree.TypeList, q *ReceiveQueue) *tree.TypeList {
	c := *tl // shallow copy to avoid mutating remoteObjects baseline
	tl = &c
	if result := q.Receive(tl.Types, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		tl.Types = result.(tree.Container[tree.Statement])
	}
	return tl
}

func (r *GoReceiver) receiveTypeDecl(td *tree.TypeDecl, q *ReceiveQueue) *tree.TypeDecl {
	c := *td // shallow copy to avoid mutating remoteObjects baseline
	td = &c
	result := q.Receive(td.Name, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		td.Name = result.(*tree.Identifier)
	}
	var beforeAssign any
	if td.Assign != nil {
		beforeAssign = *td.Assign
	}
	if result := q.Receive(beforeAssign, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		lp := result.(tree.LeftPadded[tree.Space])
		td.Assign = &lp
	} else {
		td.Assign = nil
	}
	defResult := q.Receive(td.Definition, func(v any) any { return r.Visit(v, q) })
	if defResult != nil {
		td.Definition = defResult.(tree.Expression)
	}
	var beforeSpecs any
	if td.Specs != nil {
		beforeSpecs = *td.Specs
	}
	if result := q.Receive(beforeSpecs, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		c := result.(tree.Container[tree.Statement])
		td.Specs = &c
	} else {
		td.Specs = nil
	}
	return td
}

func (r *GoReceiver) receiveMultiAssignment(ma *tree.MultiAssignment, q *ReceiveQueue) *tree.MultiAssignment {
	c := *ma // shallow copy to avoid mutating remoteObjects baseline
	ma = &c
	// Variables
	beforeVars := make([]any, len(ma.Variables))
	for i, v := range ma.Variables {
		beforeVars[i] = v
	}
	afterVars := q.ReceiveList(beforeVars, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterVars != nil {
		ma.Variables = make([]tree.RightPadded[tree.Expression], len(afterVars))
		for i, v := range afterVars {
			ma.Variables[i] = coerceToExpressionRP(v)
		}
	}
	// Operator
	if result := q.Receive(ma.Operator, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		ma.Operator = result.(tree.LeftPadded[tree.Space])
	}
	// Values
	beforeVals := make([]any, len(ma.Values))
	for i, v := range ma.Values {
		beforeVals[i] = v
	}
	afterVals := q.ReceiveList(beforeVals, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterVals != nil {
		ma.Values = make([]tree.RightPadded[tree.Expression], len(afterVals))
		for i, v := range afterVals {
			ma.Values[i] = coerceToExpressionRP(v)
		}
	}
	return ma
}

func (r *GoReceiver) receiveCommClause(cc *tree.CommClause, q *ReceiveQueue) *tree.CommClause {
	c := *cc // shallow copy to avoid mutating remoteObjects baseline
	cc = &c
	result := q.Receive(cc.Comm, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		cc.Comm = result.(tree.Statement)
	}
	cc.Colon = receiveSpace(cc.Colon, q)
	// Body
	beforeBody := make([]any, len(cc.Body))
	for i, s := range cc.Body {
		beforeBody[i] = s
	}
	afterBody := q.ReceiveList(beforeBody, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterBody != nil {
		cc.Body = make([]tree.RightPadded[tree.Statement], len(afterBody))
		for i, s := range afterBody {
			cc.Body[i] = coerceToStatementRP(s)
		}
	}
	return cc
}

func (r *GoReceiver) receiveIndexList(il *tree.IndexList, q *ReceiveQueue) *tree.IndexList {
	c := *il // shallow copy to avoid mutating remoteObjects baseline
	il = &c
	result := q.Receive(il.Target, func(v any) any { return r.Visit(v, q) })
	if result != nil {
		il.Target = result.(tree.Expression)
	}
	if result := q.Receive(il.Indices, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		il.Indices = result.(tree.Container[tree.Expression])
	}
	return il
}


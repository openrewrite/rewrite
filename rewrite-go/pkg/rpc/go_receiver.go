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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// GoReceiver deserializes Go AST nodes via the visitor pattern.
// Mirrors org.openrewrite.golang.internal.rpc.GolangReceiver, which
// extends JavaReceiver, which extends JavaVisitor.
//
// GoReceiver embeds JavaReceiver to inherit J-node Visit overrides
// and the PreVisit hook (id/prefix/markers), and adds VisitX
// overrides for G-specific node types. Self is set by visitor.Init
// so the framework's type-switch dispatch routes to the most-derived
// override.
type GoReceiver struct {
	JavaReceiver
}

// NewGoReceiver creates a GoReceiver ready to deserialize trees.
func NewGoReceiver() *GoReceiver {
	gr := &GoReceiver{}
	gr.typeReceiver = NewJavaTypeReceiver()
	return visitor.Init(gr)
}

// Visit overrides the framework dispatch to special-case ParseError —
// it isn't a J node, has no Prefix/Markers, and uses its own codec.
// All other tree types fall through to the framework's switch.
func (r *GoReceiver) Visit(t java.Tree, p any) java.Tree {
	if t == nil {
		return nil
	}
	if pe, ok := t.(*java.ParseError); ok {
		c := *pe
		return r.receiveParseError(&c, p.(*ReceiveQueue))
	}
	if pt, ok := t.(*java.PlainText); ok {
		c := *pt
		return r.receivePlainText(&c, p.(*ReceiveQueue))
	}
	if gm, ok := t.(*golang.GoMod); ok {
		c := *gm
		return receiveGoMod(&c, p.(*ReceiveQueue))
	}
	return r.GoVisitor.Visit(t, p)
}

// --- G nodes ---

// receiveParseError deserializes a ParseError matching Java's ParseError.rpcReceive field order:
// id, markers, sourcePath, charsetName, charsetBomMarked, checksum, fileAttributes, text.
//
// ParseError isn't a J node — no Prefix/Markers handling via PreVisit.
// Special-cased at the top of Visit() instead of dispatched through
// the framework switch.
func (r *GoReceiver) receiveParseError(pe *java.ParseError, q *ReceiveQueue) *java.ParseError {
	idStr := receiveScalar[string](q, pe.Ident.String())
	if idStr != "" {
		if parsed, err := uuid.Parse(idStr); err == nil {
			pe.Ident = parsed
		}
	}
	// markers is a nested object: consume its envelope via q.Receive, then read
	// sub-fields in the onChange (matches JavaReceiver.PreVisit / receivePlainText).
	// A direct receiveMarkersCodec call skips the envelope and desyncs the queue
	// on any marker-bearing ParseError.
	if result := q.Receive(pe.Markers, func(v any) any {
		return receiveMarkersCodec(q, v.(java.Markers))
	}); result != nil {
		if mk, ok := result.(java.Markers); ok {
			pe.Markers = mk
		}
	}
	pe.SourcePath = receiveScalar[string](q, pe.SourcePath)
	pe.CharsetName = receiveScalar[string](q, pe.CharsetName)
	pe.CharsetBomMarked = receiveScalar[bool](q, pe.CharsetBomMarked)
	q.Receive(nil, nil) // checksum
	q.Receive(nil, nil) // fileAttributes
	pe.Text = receiveScalar[string](q, pe.Text)
	return pe
}

// receivePlainText deserializes a PlainText matching the canonical field order
// in org.openrewrite.text.PlainTextRpcCodec (and rewrite-javascript's
// text/rpc.ts): id, markers, sourcePath, charsetName, charsetBomMarked,
// checksum, fileAttributes, text, snippets. The Go side only reads SourcePath
// and Text; checksum / fileAttributes / each snippet's fields are consumed and
// discarded so the wire stays in lockstep.
func (r *GoReceiver) receivePlainText(pt *java.PlainText, q *ReceiveQueue) *java.PlainText {
	idStr := receiveScalar[string](q, pt.Ident.String())
	if idStr != "" {
		if parsed, err := uuid.Parse(idStr); err == nil {
			pt.Ident = parsed
		}
	}
	// markers: a nested object — consume its envelope via q.Receive, then read
	// its sub-fields in the onChange, mirroring JavaReceiver.PreVisit. Calling
	// receiveMarkersCodec directly (as receiveParseError does) skips the
	// envelope message and desyncs the queue on any real, marker-bearing file.
	if result := q.Receive(pt.Markers, func(v any) any {
		return receiveMarkersCodec(q, v.(java.Markers))
	}); result != nil {
		if mk, ok := result.(java.Markers); ok {
			pt.Markers = mk
		}
	}
	pt.SourcePath = receiveScalar[string](q, pt.SourcePath)
	pt.CharsetName = receiveScalar[string](q, pt.CharsetName)
	pt.CharsetBomMarked = receiveScalar[bool](q, pt.CharsetBomMarked)
	// checksum — Checksum.rpcSend sends algorithm (string) + value (byte[]);
	// nullable, so the onChange only runs when present (matches VisitCompilationUnit).
	q.Receive(nil, func(v any) any {
		receiveScalar[string](q, "") // algorithm
		q.Receive(nil, nil)          // value
		return nil
	})
	// fileAttributes — FileAttributes.rpcSend sends 7 sub-fields (3 timestamps,
	// 3 bools, size); populated on files read from disk. The timestamps are
	// java.time.ZonedDateTime leaves (see value_types factories).
	q.Receive(nil, func(v any) any {
		q.Receive(nil, nil) // creationTime
		q.Receive(nil, nil) // lastModifiedTime
		q.Receive(nil, nil) // lastAccessTime
		q.Receive(nil, nil) // isReadable
		q.Receive(nil, nil) // isWritable
		q.Receive(nil, nil) // isExecutable
		q.Receive(nil, nil) // size
		return nil
	})
	pt.Text = receiveScalar[string](q, pt.Text)
	// snippets: a list of {id, markers, text}; empty for files. Consume each
	// element's fields to keep the queue aligned if a recipe ever produced one.
	q.ReceiveList(nil, func(v any) any {
		q.Receive(nil, nil) // snippet id
		q.Receive(nil, nil) // snippet markers
		q.Receive(nil, nil) // snippet text
		return v
	})
	return pt
}

func (r *GoReceiver) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *cu // shallow copy to avoid mutating remoteObjects baseline
	cu = &c
	cu.SourcePath = receiveScalar[string](q, cu.SourcePath)
	q.Receive(nil, nil) // charset
	q.Receive(nil, nil) // charsetBomMarked
	// checksum — Checksum.rpcSend sends: algorithm (string), value (byte[])
	q.Receive(nil, func(v any) any {
		receiveScalar[string](q, "") // algorithm
		q.Receive(nil, nil)          // value
		return nil
	})
	// fileAttributes — FileAttributes.rpcSend sends 7 sub-fields
	q.Receive(nil, func(v any) any {
		q.Receive(nil, nil) // creationTime
		q.Receive(nil, nil) // lastModifiedTime
		q.Receive(nil, nil) // lastAccessTime
		q.Receive(nil, nil) // isReadable
		q.Receive(nil, nil) // isWritable
		q.Receive(nil, nil) // isExecutable
		q.Receive(nil, nil) // size
		return nil
	})
	// packageDecl
	var beforePkgDecl any
	if cu.PackageDecl != nil {
		beforePkgDecl = *cu.PackageDecl
	}
	if result := q.Receive(beforePkgDecl, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		rp := coerceRightPaddedTyped[*java.Identifier](result)
		cu.PackageDecl = &rp
	} else {
		cu.PackageDecl = nil
	}
	// imports (container)
	cu.Imports = receivePointerContainer[*java.Import](r, q, cu.Imports)
	// statements
	beforeStmts := make([]any, len(cu.Statements))
	for i, s := range cu.Statements {
		beforeStmts[i] = s
	}
	afterStmts := q.ReceiveList(beforeStmts, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterStmts != nil {
		cu.Statements = make([]java.RightPadded[java.Statement], len(afterStmts))
		for i, s := range afterStmts {
			cu.Statements[i] = coerceToStatementRP(s)
		}
	}
	// EOF
	cu.EOF = receiveValue(q, cu.EOF, func(e java.Space) any { return receiveSpace(e, q) })
	return cu
}

func (r *GoReceiver) VisitGoStmt(gs *golang.GoStmt, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *gs // shallow copy to avoid mutating remoteObjects baseline
	gs = &c
	gs.Expr = receiveValue(q, gs.Expr, func(e java.Expression) any { return r.Visit(e, q) })
	return gs
}

func (r *GoReceiver) VisitDefer(d *golang.Defer, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *d // shallow copy to avoid mutating remoteObjects baseline
	d = &c
	d.Expr = receiveValue(q, d.Expr, func(e java.Expression) any { return r.Visit(e, q) })
	return d
}

func (r *GoReceiver) VisitSend(sn *golang.Send, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *sn // shallow copy to avoid mutating remoteObjects baseline
	sn = &c
	sn.Channel = receiveValue(q, sn.Channel, func(e java.Expression) any { return r.Visit(e, q) })
	if result := q.Receive(sn.Arrow, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		sn.Arrow = result.(java.LeftPadded[java.Expression])
	}
	return sn
}

func (r *GoReceiver) VisitGoto(g *golang.Goto, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *g // shallow copy to avoid mutating remoteObjects baseline
	g = &c
	g.Label = receiveValue(q, g.Label, func(e *java.Identifier) any { return r.Visit(e, q) })
	return g
}

func (r *GoReceiver) VisitGoUnary(u *golang.Unary, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *u // shallow copy to avoid mutating remoteObjects baseline
	u = &c
	u.Operator = receiveLeftPaddedEnum(r, q, u.Operator, golang.ParseUnaryOperator)
	u.Expression = receiveValue(q, u.Expression, func(e java.Expression) any { return r.Visit(e, q) })
	return u
}

func (r *GoReceiver) VisitGoBinary(b *golang.Binary, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	b.Left = receiveValue(q, b.Left, func(e java.Expression) any { return r.Visit(e, q) })
	b.Operator = receiveLeftPaddedEnum(r, q, b.Operator, golang.ParseBinaryOperator)
	b.Right = receiveValue(q, b.Right, func(e java.Expression) any { return r.Visit(e, q) })
	return b
}

func (r *GoReceiver) VisitGoAssignmentOperation(a *golang.AssignmentOperation, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *a // shallow copy to avoid mutating remoteObjects baseline
	a = &c
	a.Variable = receiveValue(q, a.Variable, func(e java.Expression) any { return r.Visit(e, q) })
	a.Operator = receiveLeftPaddedEnum(r, q, a.Operator, golang.ParseAssignmentOperator)
	a.Assignment = receiveValue(q, a.Assignment, func(e java.Expression) any { return r.Visit(e, q) })
	return a
}

func (r *GoReceiver) VisitGoVariadic(vr *golang.Variadic, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *vr // shallow copy to avoid mutating remoteObjects baseline
	vr = &c
	vr.Element = receiveValue(q, vr.Element, func(e java.Expression) any { return r.Visit(e, q) })
	vr.Dots = receiveValue(q, vr.Dots, func(s java.Space) any { return receiveSpace(s, q) })
	vr.Postfix = receiveScalar[bool](q, vr.Postfix)
	return vr
}

// VisitFallthrough mirrors GolangReceiver.visitFallthrough — the node has no
// payload beyond the framework-handled id/prefix/markers, so this override
// is intentionally a no-op. Present for sender/receiver symmetry.
func (r *GoReceiver) VisitFallthrough(f *golang.Fallthrough, p any) java.J {
	return f
}

func (r *GoReceiver) VisitComposite(comp *golang.Composite, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *comp // shallow copy to avoid mutating remoteObjects baseline
	comp = &c
	comp.TypeExpr = receiveValue(q, comp.TypeExpr, func(e java.Expression) any { return r.Visit(e, q) })
	comp.Elements = receiveContainer[java.Expression](r, q, comp.Elements)
	return comp
}

func (r *GoReceiver) VisitKeyValue(kv *golang.KeyValue, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *kv // shallow copy to avoid mutating remoteObjects baseline
	kv = &c
	kv.Key = receiveValue(q, kv.Key, func(e java.Expression) any { return r.Visit(e, q) })
	if result := q.Receive(kv.Value, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		kv.Value = result.(java.LeftPadded[java.Expression])
	}
	return kv
}

func (r *GoReceiver) VisitGoArrayType(at *golang.ArrayType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *at // shallow copy to avoid mutating remoteObjects baseline
	at = &c
	if result := q.Receive(at.Length, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		at.Length = coerceToExpressionRP(result)
	}
	at.ElementType = receiveValue(q, at.ElementType, func(e java.Expression) any { return r.Visit(e, q) })
	return at
}

func (r *GoReceiver) VisitSlice(sl *golang.Slice, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *sl // shallow copy to avoid mutating remoteObjects baseline
	sl = &c
	sl.Indexed = receiveValue(q, sl.Indexed, func(e java.Expression) any { return r.Visit(e, q) })
	sl.OpenBracket = receiveValue(q, sl.OpenBracket, func(e java.Space) any { return receiveSpace(e, q) })
	if result := q.Receive(sl.Low, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		sl.Low = coerceToExpressionRP(result)
	}
	if result := q.Receive(sl.High, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		sl.High = coerceToExpressionRP(result)
	}
	sl.Max = receiveValue(q, sl.Max, func(e java.Expression) any { return r.Visit(e, q) })
	sl.CloseBracket = receiveValue(q, sl.CloseBracket, func(e java.Space) any { return receiveSpace(e, q) })
	return sl
}

func (r *GoReceiver) VisitMapType(mt *golang.MapType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *mt // shallow copy to avoid mutating remoteObjects baseline
	mt = &c
	mt.OpenBracket = receiveValue(q, mt.OpenBracket, func(e java.Space) any { return receiveSpace(e, q) })
	if result := q.Receive(mt.Key, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		mt.Key = coerceToExpressionRP(result)
	}
	mt.Value = receiveValue(q, mt.Value, func(e java.Expression) any { return r.Visit(e, q) })
	return mt
}

func (r *GoReceiver) VisitStatementExpression(se *golang.StatementExpression, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *se
	se = &c
	se.Statement = receiveValue(q, se.Statement, func(e java.Statement) any { return r.Visit(e, q) })
	return se
}

func (r *GoReceiver) VisitPointerType(pt *golang.PointerType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *pt
	pt = &c
	pt.Elem = receiveValue(q, pt.Elem, func(e java.Expression) any { return r.Visit(e, q) })
	return pt
}

func (r *GoReceiver) VisitChannel(ch *golang.Channel, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ch // shallow copy to avoid mutating remoteObjects baseline
	ch = &c
	dirStr := receiveScalar[string](q, "")
	switch dirStr {
	case "BIDI":
		ch.Dir = golang.ChanBidi
	case "SEND_ONLY":
		ch.Dir = golang.ChanSendOnly
	case "RECV_ONLY":
		ch.Dir = golang.ChanRecvOnly
	}
	ch.Value = receiveValue(q, ch.Value, func(e java.Expression) any { return r.Visit(e, q) })
	return ch
}

func (r *GoReceiver) VisitFuncType(ft *golang.FuncType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ft // shallow copy to avoid mutating remoteObjects baseline
	ft = &c
	ft.Parameters = receiveContainer[java.Statement](r, q, ft.Parameters)
	ft.ReturnType = receiveValue(q, ft.ReturnType, func(e java.Expression) any { return r.Visit(e, q) })
	return ft
}

func (r *GoReceiver) VisitStructType(st *golang.StructType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *st // shallow copy to avoid mutating remoteObjects baseline
	st = &c
	st.Body = receiveValue(q, st.Body, func(e *java.Block) any { return r.Visit(e, q) })
	return st
}

func (r *GoReceiver) VisitInterfaceType(it *golang.InterfaceType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *it // shallow copy to avoid mutating remoteObjects baseline
	it = &c
	it.Body = receiveValue(q, it.Body, func(e *java.Block) any { return r.Visit(e, q) })
	return it
}

func (r *GoReceiver) VisitTypeList(tl *golang.TypeList, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *tl // shallow copy to avoid mutating remoteObjects baseline
	tl = &c
	tl.Types = receiveContainer[java.Statement](r, q, tl.Types)
	return tl
}

func (r *GoReceiver) VisitUnion(u *golang.Union, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *u // shallow copy to avoid mutating remoteObjects baseline
	u = &c
	beforeTypes := make([]any, len(u.Types))
	for i, t := range u.Types {
		beforeTypes[i] = t
	}
	afterTypes := q.ReceiveList(beforeTypes, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterTypes != nil {
		u.Types = make([]java.RightPadded[java.Expression], len(afterTypes))
		for i, t := range afterTypes {
			u.Types[i] = coerceToExpressionRP(t)
		}
	}
	return u
}

func (r *GoReceiver) VisitUnderlyingType(ut *golang.UnderlyingType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ut // shallow copy to avoid mutating remoteObjects baseline
	ut = &c
	ut.Element = receiveValue(q, ut.Element, func(e java.Expression) any { return r.Visit(e, q) })
	return ut
}

func (r *GoReceiver) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *td // shallow copy to avoid mutating remoteObjects baseline
	td = &c
	// leadingAnnotations
	beforeAnns := make([]any, len(td.LeadingAnnotations))
	for i, a := range td.LeadingAnnotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if afterAnns != nil {
		td.LeadingAnnotations = make([]*java.Annotation, 0, len(afterAnns))
		for _, a := range afterAnns {
			if a != nil {
				td.LeadingAnnotations = append(td.LeadingAnnotations, a.(*java.Annotation))
			}
		}
	}
	td.Name = receiveValue(q, td.Name, func(e *java.Identifier) any { return r.Visit(e, q) })
	// typeParameters
	td.TypeParameters = receiveValue(q, td.TypeParameters, func(e *java.TypeParameters) any { return r.Visit(e, q) })
	var beforeAssign any
	if td.Assign != nil {
		beforeAssign = *td.Assign
	}
	if result := q.Receive(beforeAssign, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		lp := result.(java.LeftPadded[java.Space])
		td.Assign = &lp
	} else {
		td.Assign = nil
	}
	td.Definition = receiveValue(q, td.Definition, func(e java.Expression) any { return r.Visit(e, q) })
	td.Specs = receivePointerContainer[java.Statement](r, q, td.Specs)
	return td
}

func (r *GoReceiver) VisitDeclarationBlock(db *golang.DeclarationBlock, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *db // shallow copy to avoid mutating remoteObjects baseline
	db = &c
	// leadingAnnotations
	beforeAnns := make([]any, len(db.LeadingAnnotations))
	for i, a := range db.LeadingAnnotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if afterAnns != nil {
		db.LeadingAnnotations = make([]*java.Annotation, 0, len(afterAnns))
		for _, a := range afterAnns {
			if a != nil {
				db.LeadingAnnotations = append(db.LeadingAnnotations, a.(*java.Annotation))
			}
		}
	}
	// kind
	kindStr := receiveScalar[string](q, "")
	switch kindStr {
	case "CONST":
		db.Kind = golang.DeclConst
	case "VAR":
		db.Kind = golang.DeclVar
	}
	db.Specs = receivePointerContainer[java.Statement](r, q, db.Specs)
	return db
}

func (r *GoReceiver) VisitMultiAssignment(ma *golang.MultiAssignment, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ma // shallow copy to avoid mutating remoteObjects baseline
	ma = &c
	// Variables
	beforeVars := make([]any, len(ma.Variables))
	for i, v := range ma.Variables {
		beforeVars[i] = v
	}
	afterVars := q.ReceiveList(beforeVars, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterVars != nil {
		ma.Variables = make([]java.RightPadded[java.Expression], len(afterVars))
		for i, v := range afterVars {
			ma.Variables[i] = coerceToExpressionRP(v)
		}
	}
	// Operator
	if result := q.Receive(ma.Operator, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		ma.Operator = result.(java.LeftPadded[java.Space])
	}
	// Values
	beforeVals := make([]any, len(ma.Values))
	for i, v := range ma.Values {
		beforeVals[i] = v
	}
	afterVals := q.ReceiveList(beforeVals, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterVals != nil {
		ma.Values = make([]java.RightPadded[java.Expression], len(afterVals))
		for i, v := range afterVals {
			ma.Values[i] = coerceToExpressionRP(v)
		}
	}
	return ma
}

func (r *GoReceiver) VisitGoReturn(ret *golang.Return, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ret // shallow copy to avoid mutating remoteObjects baseline
	ret = &c
	beforeExprs := make([]any, len(ret.Expressions))
	for i, e := range ret.Expressions {
		beforeExprs[i] = e
	}
	afterExprs := q.ReceiveList(beforeExprs, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterExprs != nil {
		ret.Expressions = make([]java.RightPadded[java.Expression], len(afterExprs))
		for i, v := range afterExprs {
			ret.Expressions[i] = coerceToExpressionRP(v)
		}
	}
	return ret
}

func (r *GoReceiver) VisitGoMethodDeclaration(md *golang.MethodDeclaration, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *md // shallow copy to avoid mutating remoteObjects baseline
	md = &c
	md.Receiver = receiveContainer[java.Statement](r, q, md.Receiver)
	md.Declaration = receiveValue(q, md.Declaration, func(e *java.MethodDeclaration) any { return r.Visit(e, q) })
	return md
}

func (r *GoReceiver) VisitStatementWithInit(swi *golang.StatementWithInit, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *swi // shallow copy to avoid mutating remoteObjects baseline
	swi = &c
	if result := q.Receive(swi.Init, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		swi.Init = coerceToStatementRP(result)
	}
	swi.Statement = receiveValue(q, swi.Statement, func(e java.Statement) any { return r.Visit(e, q) })
	return swi
}

func (r *GoReceiver) VisitCommClause(cc *golang.CommClause, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *cc // shallow copy to avoid mutating remoteObjects baseline
	cc = &c
	cc.Comm = receiveValue(q, cc.Comm, func(e java.Statement) any { return r.Visit(e, q) })
	cc.Colon = receiveValue(q, cc.Colon, func(e java.Space) any { return receiveSpace(e, q) })
	// Body
	beforeBody := make([]any, len(cc.Body))
	for i, s := range cc.Body {
		beforeBody[i] = s
	}
	afterBody := q.ReceiveList(beforeBody, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterBody != nil {
		cc.Body = make([]java.RightPadded[java.Statement], len(afterBody))
		for i, s := range afterBody {
			cc.Body[i] = coerceToStatementRP(s)
		}
	}
	return cc
}

func (r *GoReceiver) VisitIndexList(il *golang.IndexList, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *il // shallow copy to avoid mutating remoteObjects baseline
	il = &c
	il.Target = receiveValue(q, il.Target, func(e java.Expression) any { return r.Visit(e, q) })
	il.Indices = receiveContainer[java.Expression](r, q, il.Indices)
	return il
}

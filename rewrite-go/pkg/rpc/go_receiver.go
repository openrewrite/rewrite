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
	pe.Markers = receiveMarkersCodec(q, pe.Markers)
	pe.SourcePath = receiveScalar[string](q, pe.SourcePath)
	pe.CharsetName = receiveScalar[string](q, pe.CharsetName)
	pe.CharsetBomMarked = receiveScalar[bool](q, pe.CharsetBomMarked)
	q.Receive(nil, nil) // checksum
	q.Receive(nil, nil) // fileAttributes
	pe.Text = receiveScalar[string](q, pe.Text)
	return pe
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
	if result := q.Receive(beforeImports, func(v any) any { return receiveContainerTyped[*java.Import](r, q, v) }); result != nil {
		// Safe by construction: the onChange path returns Container[*Import] from
		// receiveContainerTyped, and the NoChange path returns beforeImports, which
		// is the field's own Container[*Import] value (or nil, handled by the else).
		c := result.(java.Container[*java.Import])
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
		cu.Statements = make([]java.RightPadded[java.Statement], len(afterStmts))
		for i, s := range afterStmts {
			cu.Statements[i] = coerceToStatementRP(s)
		}
	}
	// EOF
	if result := q.Receive(cu.EOF, func(v any) any { return receiveSpace(v.(java.Space), q) }); result != nil {
		cu.EOF = result.(java.Space)
	}
	return cu
}

func (r *GoReceiver) VisitGoStmt(gs *golang.GoStmt, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *gs // shallow copy to avoid mutating remoteObjects baseline
	gs = &c
	result := q.Receive(gs.Expr, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		gs.Expr = result.(java.Expression)
	}
	return gs
}

func (r *GoReceiver) VisitDefer(d *golang.Defer, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *d // shallow copy to avoid mutating remoteObjects baseline
	d = &c
	result := q.Receive(d.Expr, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		d.Expr = result.(java.Expression)
	}
	return d
}

func (r *GoReceiver) VisitSend(sn *golang.Send, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *sn // shallow copy to avoid mutating remoteObjects baseline
	sn = &c
	result := q.Receive(sn.Channel, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		sn.Channel = result.(java.Expression)
	}
	if result := q.Receive(sn.Arrow, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		sn.Arrow = result.(java.LeftPadded[java.Expression])
	}
	return sn
}

func (r *GoReceiver) VisitGoto(g *golang.Goto, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *g // shallow copy to avoid mutating remoteObjects baseline
	g = &c
	result := q.Receive(g.Label, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		g.Label = result.(*java.Identifier)
	}
	return g
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
	result := q.Receive(comp.TypeExpr, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		comp.TypeExpr = result.(java.Expression)
	}
	if result := q.Receive(comp.Elements, func(v any) any { return receiveContainerTyped[java.Expression](r, q, v) }); result != nil {
		comp.Elements = result.(java.Container[java.Expression])
	}
	return comp
}

func (r *GoReceiver) VisitKeyValue(kv *golang.KeyValue, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *kv // shallow copy to avoid mutating remoteObjects baseline
	kv = &c
	result := q.Receive(kv.Key, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		kv.Key = result.(java.Expression)
	}
	if result := q.Receive(kv.Value, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		kv.Value = result.(java.LeftPadded[java.Expression])
	}
	return kv
}

func (r *GoReceiver) VisitSlice(sl *golang.Slice, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *sl // shallow copy to avoid mutating remoteObjects baseline
	sl = &c
	result := q.Receive(sl.Indexed, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		sl.Indexed = result.(java.Expression)
	}
	if result := q.Receive(sl.OpenBracket, func(v any) any { return receiveSpace(v.(java.Space), q) }); result != nil {
		sl.OpenBracket = result.(java.Space)
	}
	if result := q.Receive(sl.Low, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		sl.Low = coerceToExpressionRP(result)
	}
	if result := q.Receive(sl.High, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		sl.High = coerceToExpressionRP(result)
	}
	max := q.Receive(sl.Max, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if max != nil {
		sl.Max = max.(java.Expression)
	}
	if result := q.Receive(sl.CloseBracket, func(v any) any { return receiveSpace(v.(java.Space), q) }); result != nil {
		sl.CloseBracket = result.(java.Space)
	}
	return sl
}

func (r *GoReceiver) VisitMapType(mt *golang.MapType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *mt // shallow copy to avoid mutating remoteObjects baseline
	mt = &c
	if result := q.Receive(mt.OpenBracket, func(v any) any { return receiveSpace(v.(java.Space), q) }); result != nil {
		mt.OpenBracket = result.(java.Space)
	}
	if result := q.Receive(mt.Key, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		mt.Key = coerceToExpressionRP(result)
	}
	result := q.Receive(mt.Value, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		mt.Value = result.(java.Expression)
	}
	return mt
}

func (r *GoReceiver) VisitStatementExpression(se *golang.StatementExpression, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *se
	se = &c
	result := q.Receive(se.Statement, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		se.Statement = result.(java.Statement)
	}
	return se
}

func (r *GoReceiver) VisitPointerType(pt *golang.PointerType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *pt
	pt = &c
	result := q.Receive(pt.Elem, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		pt.Elem = result.(java.Expression)
	}
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
	result := q.Receive(ch.Value, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		ch.Value = result.(java.Expression)
	}
	return ch
}

func (r *GoReceiver) VisitFuncType(ft *golang.FuncType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ft // shallow copy to avoid mutating remoteObjects baseline
	ft = &c
	if result := q.Receive(ft.Parameters, func(v any) any { return receiveContainerTyped[java.Statement](r, q, v) }); result != nil {
		ft.Parameters = result.(java.Container[java.Statement])
	}
	result := q.Receive(ft.ReturnType, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		ft.ReturnType = result.(java.Expression)
	}
	return ft
}

func (r *GoReceiver) VisitStructType(st *golang.StructType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *st // shallow copy to avoid mutating remoteObjects baseline
	st = &c
	result := q.Receive(st.Body, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		st.Body = result.(*java.Block)
	}
	return st
}

func (r *GoReceiver) VisitInterfaceType(it *golang.InterfaceType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *it // shallow copy to avoid mutating remoteObjects baseline
	it = &c
	result := q.Receive(it.Body, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		it.Body = result.(*java.Block)
	}
	return it
}

func (r *GoReceiver) VisitTypeList(tl *golang.TypeList, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *tl // shallow copy to avoid mutating remoteObjects baseline
	tl = &c
	if result := q.Receive(tl.Types, func(v any) any { return receiveContainerTyped[java.Statement](r, q, v) }); result != nil {
		tl.Types = result.(java.Container[java.Statement])
	}
	return tl
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
	result := q.Receive(td.Name, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		td.Name = result.(*java.Identifier)
	}
	// typeParameters
	if tpResult := q.Receive(td.TypeParameters, func(v any) any { return r.Visit(v.(java.Tree), q) }); tpResult != nil {
		td.TypeParameters = tpResult.(*java.TypeParameters)
	} else {
		td.TypeParameters = nil
	}
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
	defResult := q.Receive(td.Definition, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if defResult != nil {
		td.Definition = defResult.(java.Expression)
	}
	var beforeSpecs any
	if td.Specs != nil {
		beforeSpecs = *td.Specs
	}
	if result := q.Receive(beforeSpecs, func(v any) any { return receiveContainerTyped[java.Statement](r, q, v) }); result != nil {
		c := result.(java.Container[java.Statement])
		td.Specs = &c
	} else {
		td.Specs = nil
	}
	return td
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

func (r *GoReceiver) VisitCommClause(cc *golang.CommClause, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *cc // shallow copy to avoid mutating remoteObjects baseline
	cc = &c
	result := q.Receive(cc.Comm, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		cc.Comm = result.(java.Statement)
	}
	if result := q.Receive(cc.Colon, func(v any) any { return receiveSpace(v.(java.Space), q) }); result != nil {
		cc.Colon = result.(java.Space)
	}
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
	result := q.Receive(il.Target, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if result != nil {
		il.Target = result.(java.Expression)
	}
	if result := q.Receive(il.Indices, func(v any) any { return receiveContainerTyped[java.Expression](r, q, v) }); result != nil {
		il.Indices = result.(java.Container[java.Expression])
	}
	return il
}

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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// GoSender serializes Go AST nodes via the visitor pattern. Mirrors
// org.openrewrite.golang.internal.rpc.GolangSender, which extends
// JavaSender, which extends JavaVisitor.
//
// GoSender embeds JavaSender to inherit J-node Visit overrides + the
// PreVisit hook, and adds VisitX overrides for the G-specific node
// types. The framework's type-switch dispatch in visitor.GoVisitor.Visit
// routes to the most-derived override via the Self field set by
// visitor.Init.
type GoSender struct {
	JavaSender
}

// NewGoSender creates a GoSender ready to serialize trees. Sets Self
// so the framework's dispatch routes to G-node and J-node overrides
// across the embedding chain.
func NewGoSender() *GoSender {
	gs := &GoSender{}
	gs.typeSender = NewJavaTypeSender()
	return visitor.Init(gs)
}

// Visit overrides the framework dispatch to special-case ParseError —
// it isn't a J node, has no Prefix/Markers, and uses its own codec.
// All other tree types fall through to the framework's switch.
func (s *GoSender) Visit(t java.Tree, p any) java.Tree {
	if t == nil {
		return nil
	}
	if pe, ok := t.(*java.ParseError); ok {
		s.sendParseError(pe, p.(*SendQueue))
		return pe
	}
	if gm, ok := t.(*golang.GoMod); ok {
		sendGoMod(gm, p.(*SendQueue))
		return gm
	}
	return s.GoVisitor.Visit(t, p)
}

// --- G nodes ---

func (s *GoSender) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(cu, func(v any) any { return v.(*golang.CompilationUnit).SourcePath }, nil)
	// charset - Go doesn't track this, send empty/default
	q.GetAndSend(cu, func(_ any) any { return "UTF-8" }, nil)
	// charsetBomMarked
	q.GetAndSend(cu, func(_ any) any { return false }, nil)
	// checksum
	q.GetAndSend(cu, func(_ any) any { return nil }, nil)
	// fileAttributes
	q.GetAndSend(cu, func(_ any) any { return nil }, nil)
	// packageDecl (right-padded) — dereference pointer so sendRightPadded gets a value type
	q.GetAndSend(cu, func(v any) any {
		pd := v.(*golang.CompilationUnit).PackageDecl
		if pd == nil {
			return nil
		}
		return *pd
	}, func(v any) { sendRightPadded(s, v, q) })
	// imports (container)
	q.GetAndSend(cu, func(v any) any {
		c := v.(*golang.CompilationUnit)
		if c.Imports == nil {
			return nil
		}
		return *c.Imports
	}, func(v any) { sendContainer(s, v, q) })
	// statements (list of right-padded)
	q.GetAndSendList(cu,
		func(v any) []any {
			stmts := v.(*golang.CompilationUnit).Statements
			result := make([]any, len(stmts))
			for i, s := range stmts {
				result[i] = s
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	// EOF space
	q.GetAndSend(cu, func(v any) any { return v.(*golang.CompilationUnit).EOF },
		func(v any) { sendSpace(v.(java.Space), q) })
	return cu
}

func (s *GoSender) VisitGoStmt(gs *golang.GoStmt, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(gs, func(v any) any { return v.(*golang.GoStmt).Expr },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return gs
}

func (s *GoSender) VisitDefer(d *golang.Defer, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(d, func(v any) any { return v.(*golang.Defer).Expr },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return d
}

func (s *GoSender) VisitSend(sn *golang.Send, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(sn, func(v any) any { return v.(*golang.Send).Channel },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(sn, func(v any) any { return v.(*golang.Send).Arrow },
		func(v any) { sendLeftPadded(s, v, q) })
	return sn
}

func (s *GoSender) VisitGoto(g *golang.Goto, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(g, func(v any) any { return v.(*golang.Goto).Label },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return g
}

func (s *GoSender) VisitGoUnary(u *golang.Unary, p any) java.J {
	q := p.(*SendQueue)
	// Send the operator as its faithful Java enum-constant name (Go.Unary.Type).
	q.GetAndSend(u, func(v any) any {
		op := v.(*golang.Unary).Operator
		return java.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(u, func(v any) any { return v.(*golang.Unary).Expression },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return u
}

func (s *GoSender) VisitGoBinary(b *golang.Binary, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(b, func(v any) any { return v.(*golang.Binary).Left },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(b, func(v any) any {
		op := v.(*golang.Binary).Operator
		return java.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(b, func(v any) any { return v.(*golang.Binary).Right },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return b
}

func (s *GoSender) VisitGoAssignmentOperation(a *golang.AssignmentOperation, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return v.(*golang.AssignmentOperation).Variable },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(a, func(v any) any {
		op := v.(*golang.AssignmentOperation).Operator
		return java.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(a, func(v any) any { return v.(*golang.AssignmentOperation).Assignment },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return a
}

func (s *GoSender) VisitGoVariadic(vr *golang.Variadic, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(vr, func(v any) any { return v.(*golang.Variadic).Element },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(vr, func(v any) any { return v.(*golang.Variadic).Dots },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(vr, func(v any) any { return v.(*golang.Variadic).Postfix }, nil)
	return vr
}

// VisitFallthrough mirrors GolangSender.visitFallthrough — the node has no
// payload beyond the framework-handled id/prefix/markers, so this override
// is intentionally a no-op. Present for sender/receiver symmetry.
func (s *GoSender) VisitFallthrough(f *golang.Fallthrough, p any) java.J {
	return f
}

func (s *GoSender) VisitComposite(c *golang.Composite, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(c, func(v any) any { return v.(*golang.Composite).TypeExpr },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(c, func(v any) any { return v.(*golang.Composite).Elements },
		func(v any) { sendContainer(s, v, q) })
	return c
}

func (s *GoSender) VisitKeyValue(kv *golang.KeyValue, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(kv, func(v any) any { return v.(*golang.KeyValue).Key },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(kv, func(v any) any { return v.(*golang.KeyValue).Value },
		func(v any) { sendLeftPadded(s, v, q) })
	return kv
}

func (s *GoSender) VisitSlice(sl *golang.Slice, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(sl, func(v any) any { return v.(*golang.Slice).Indexed },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(sl, func(v any) any { return v.(*golang.Slice).OpenBracket },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(sl, func(v any) any { return v.(*golang.Slice).Low },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(sl, func(v any) any { return v.(*golang.Slice).High },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(sl, func(v any) any { return v.(*golang.Slice).Max },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(sl, func(v any) any { return v.(*golang.Slice).CloseBracket },
		func(v any) { sendSpace(v.(java.Space), q) })
	return sl
}

func (s *GoSender) VisitMapType(mt *golang.MapType, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(mt, func(v any) any { return v.(*golang.MapType).OpenBracket },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(mt, func(v any) any { return v.(*golang.MapType).Key },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(mt, func(v any) any { return v.(*golang.MapType).Value },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return mt
}

func (s *GoSender) VisitStatementExpression(se *golang.StatementExpression, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(se, func(v any) any { return v.(*golang.StatementExpression).Statement },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return se
}

func (s *GoSender) VisitPointerType(pt *golang.PointerType, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(pt, func(v any) any { return v.(*golang.PointerType).Elem },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return pt
}

func (s *GoSender) VisitChannel(ch *golang.Channel, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(ch, func(v any) any {
		switch v.(*golang.Channel).Dir {
		case golang.ChanBidi:
			return "BIDI"
		case golang.ChanSendOnly:
			return "SEND_ONLY"
		case golang.ChanRecvOnly:
			return "RECV_ONLY"
		default:
			return "BIDI"
		}
	}, nil)
	q.GetAndSend(ch, func(v any) any { return v.(*golang.Channel).Value },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return ch
}

func (s *GoSender) VisitFuncType(ft *golang.FuncType, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(ft, func(v any) any { return v.(*golang.FuncType).Parameters },
		func(v any) { sendContainer(s, v, q) })
	q.GetAndSend(ft, func(v any) any { return v.(*golang.FuncType).ReturnType },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return ft
}

func (s *GoSender) VisitStructType(st *golang.StructType, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(st, func(v any) any { return v.(*golang.StructType).Body },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return st
}

func (s *GoSender) VisitInterfaceType(it *golang.InterfaceType, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(it, func(v any) any { return v.(*golang.InterfaceType).Body },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return it
}

func (s *GoSender) VisitTypeList(tl *golang.TypeList, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(tl, func(v any) any { return v.(*golang.TypeList).Types },
		func(v any) { sendContainer(s, v, q) })
	return tl
}

func (s *GoSender) VisitUnion(u *golang.Union, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSendList(u,
		func(v any) []any {
			types := v.(*golang.Union).Types
			result := make([]any, len(types))
			for i, t := range types {
				result[i] = t
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	return u
}

func (s *GoSender) VisitUnderlyingType(ut *golang.UnderlyingType, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(ut, func(v any) any { return v.(*golang.UnderlyingType).Element },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return ut
}

func (s *GoSender) VisitTypeDecl(td *golang.TypeDecl, p any) java.J {
	q := p.(*SendQueue)
	// leadingAnnotations (`//go:` directives modeled as J.Annotation)
	q.GetAndSendList(td,
		func(v any) []any {
			anns := v.(*golang.TypeDecl).LeadingAnnotations
			result := make([]any, len(anns))
			for i, a := range anns {
				result[i] = a
			}
			return result
		},
		func(v any) any { return extractID(v) },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(td, func(v any) any { return v.(*golang.TypeDecl).Name },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// typeParameters (`[T any]` declaration-site generics; nil for non-generic types)
	q.GetAndSend(td, func(v any) any { return v.(*golang.TypeDecl).TypeParameters },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// Assign — dereference pointer so sendLeftPadded gets a value type
	q.GetAndSend(td, func(v any) any {
		a := v.(*golang.TypeDecl).Assign
		if a == nil {
			return nil
		}
		return *a
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(td, func(v any) any { return v.(*golang.TypeDecl).Definition },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// Specs — dereference pointer so sendContainer gets a value type
	q.GetAndSend(td, func(v any) any {
		sp := v.(*golang.TypeDecl).Specs
		if sp == nil {
			return nil
		}
		return *sp
	}, func(v any) { sendContainer(s, v, q) })
	return td
}

func (s *GoSender) VisitMultiAssignment(ma *golang.MultiAssignment, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSendList(ma,
		func(v any) []any {
			vars := v.(*golang.MultiAssignment).Variables
			result := make([]any, len(vars))
			for i, e := range vars {
				result[i] = e
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(ma, func(v any) any { return v.(*golang.MultiAssignment).Operator },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSendList(ma,
		func(v any) []any {
			vals := v.(*golang.MultiAssignment).Values
			result := make([]any, len(vals))
			for i, e := range vals {
				result[i] = e
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	return ma
}

func (s *GoSender) VisitCommClause(cc *golang.CommClause, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(cc, func(v any) any { return v.(*golang.CommClause).Comm },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(cc, func(v any) any { return v.(*golang.CommClause).Colon },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSendList(cc,
		func(v any) []any {
			body := v.(*golang.CommClause).Body
			result := make([]any, len(body))
			for i, e := range body {
				result[i] = e
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	return cc
}

// sendParseError serializes a ParseError matching Java's ParseError.rpcSend field order:
// id, markers, sourcePath, charsetName, charsetBomMarked, checksum, fileAttributes, text
//
// ParseError isn't a J node — no Prefix/Markers handling via PreVisit.
// Special-cased at the top of Visit() instead of dispatched through
// the framework switch.
func (s *GoSender) sendParseError(pe *java.ParseError, q *SendQueue) {
	q.GetAndSend(pe, func(v any) any { return v.(*java.ParseError).Ident.String() }, nil)
	q.GetAndSend(pe, func(v any) any { return v.(*java.ParseError).Markers },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	q.GetAndSend(pe, func(v any) any { return v.(*java.ParseError).SourcePath }, nil)
	q.GetAndSend(pe, func(v any) any { return v.(*java.ParseError).CharsetName }, nil)
	q.GetAndSend(pe, func(v any) any { return v.(*java.ParseError).CharsetBomMarked }, nil)
	q.GetAndSend(pe, func(_ any) any { return nil }, nil) // checksum
	q.GetAndSend(pe, func(_ any) any { return nil }, nil) // fileAttributes
	q.GetAndSend(pe, func(v any) any { return v.(*java.ParseError).Text }, nil)
}

func (s *GoSender) VisitIndexList(il *golang.IndexList, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(il, func(v any) any { return v.(*golang.IndexList).Target },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(il, func(v any) any { return v.(*golang.IndexList).Indices },
		func(v any) { sendContainer(s, v, q) })
	return il
}

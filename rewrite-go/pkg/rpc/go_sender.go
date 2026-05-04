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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
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
func (s *GoSender) Visit(t tree.Tree, p any) tree.Tree {
	if t == nil {
		return nil
	}
	if pe, ok := t.(*tree.ParseError); ok {
		s.sendParseError(pe, p.(*SendQueue))
		return pe
	}
	return s.GoVisitor.Visit(t, p)
}

// --- G nodes ---

func (s *GoSender) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(cu, func(v any) any { return v.(*tree.CompilationUnit).SourcePath }, nil)
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
		pd := v.(*tree.CompilationUnit).PackageDecl
		if pd == nil {
			return nil
		}
		return *pd
	}, func(v any) { sendRightPadded(s, v, q) })
	// imports (container)
	q.GetAndSend(cu, func(v any) any {
		c := v.(*tree.CompilationUnit)
		if c.Imports == nil {
			return nil
		}
		return *c.Imports
	}, func(v any) { sendContainer(s, v, q) })
	// statements (list of right-padded)
	q.GetAndSendList(cu,
		func(v any) []any {
			stmts := v.(*tree.CompilationUnit).Statements
			result := make([]any, len(stmts))
			for i, s := range stmts {
				result[i] = s
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	// EOF space
	q.GetAndSend(cu, func(v any) any { return v.(*tree.CompilationUnit).EOF },
		func(v any) { sendSpace(v.(tree.Space), q) })
	return cu
}

func (s *GoSender) VisitGoStmt(gs *tree.GoStmt, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(gs, func(v any) any { return v.(*tree.GoStmt).Expr },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return gs
}

func (s *GoSender) VisitDefer(d *tree.Defer, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(d, func(v any) any { return v.(*tree.Defer).Expr },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return d
}

func (s *GoSender) VisitSend(sn *tree.Send, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(sn, func(v any) any { return v.(*tree.Send).Channel },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(sn, func(v any) any { return v.(*tree.Send).Arrow },
		func(v any) { sendLeftPadded(s, v, q) })
	return sn
}

func (s *GoSender) VisitGoto(g *tree.Goto, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(g, func(v any) any { return v.(*tree.Goto).Label },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return g
}

func (s *GoSender) VisitComposite(c *tree.Composite, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(c, func(v any) any { return v.(*tree.Composite).TypeExpr },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(c, func(v any) any { return v.(*tree.Composite).Elements },
		func(v any) { sendContainer(s, v, q) })
	return c
}

func (s *GoSender) VisitKeyValue(kv *tree.KeyValue, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(kv, func(v any) any { return v.(*tree.KeyValue).Key },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(kv, func(v any) any { return v.(*tree.KeyValue).Value },
		func(v any) { sendLeftPadded(s, v, q) })
	return kv
}

func (s *GoSender) VisitSlice(sl *tree.Slice, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).Indexed },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).OpenBracket },
		func(v any) { sendSpace(v.(tree.Space), q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).Low },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).High },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).Max },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).CloseBracket },
		func(v any) { sendSpace(v.(tree.Space), q) })
	return sl
}

func (s *GoSender) VisitMapType(mt *tree.MapType, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(mt, func(v any) any { return v.(*tree.MapType).OpenBracket },
		func(v any) { sendSpace(v.(tree.Space), q) })
	q.GetAndSend(mt, func(v any) any { return v.(*tree.MapType).Key },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(mt, func(v any) any { return v.(*tree.MapType).Value },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return mt
}

func (s *GoSender) VisitStatementExpression(se *tree.StatementExpression, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(se, func(v any) any { return v.(*tree.StatementExpression).Statement },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return se
}

func (s *GoSender) VisitPointerType(pt *tree.PointerType, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(pt, func(v any) any { return v.(*tree.PointerType).Elem },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return pt
}

func (s *GoSender) VisitChannel(ch *tree.Channel, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(ch, func(v any) any {
		switch v.(*tree.Channel).Dir {
		case tree.ChanBidi:
			return "BIDI"
		case tree.ChanSendOnly:
			return "SEND_ONLY"
		case tree.ChanRecvOnly:
			return "RECV_ONLY"
		default:
			return "BIDI"
		}
	}, nil)
	q.GetAndSend(ch, func(v any) any { return v.(*tree.Channel).Value },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return ch
}

func (s *GoSender) VisitFuncType(ft *tree.FuncType, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(ft, func(v any) any { return v.(*tree.FuncType).Parameters },
		func(v any) { sendContainer(s, v, q) })
	q.GetAndSend(ft, func(v any) any { return v.(*tree.FuncType).ReturnType },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return ft
}

func (s *GoSender) VisitStructType(st *tree.StructType, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(st, func(v any) any { return v.(*tree.StructType).Body },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return st
}

func (s *GoSender) VisitInterfaceType(it *tree.InterfaceType, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(it, func(v any) any { return v.(*tree.InterfaceType).Body },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return it
}

func (s *GoSender) VisitTypeList(tl *tree.TypeList, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(tl, func(v any) any { return v.(*tree.TypeList).Types },
		func(v any) { sendContainer(s, v, q) })
	return tl
}

func (s *GoSender) VisitTypeDecl(td *tree.TypeDecl, p any) tree.J {
	q := p.(*SendQueue)
	// leadingAnnotations (`//go:` directives modeled as J.Annotation)
	q.GetAndSendList(td,
		func(v any) []any {
			anns := v.(*tree.TypeDecl).LeadingAnnotations
			result := make([]any, len(anns))
			for i, a := range anns {
				result[i] = a
			}
			return result
		},
		func(v any) any { return extractID(v) },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(td, func(v any) any { return v.(*tree.TypeDecl).Name },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// Assign — dereference pointer so sendLeftPadded gets a value type
	q.GetAndSend(td, func(v any) any {
		a := v.(*tree.TypeDecl).Assign
		if a == nil { return nil }
		return *a
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(td, func(v any) any { return v.(*tree.TypeDecl).Definition },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// Specs — dereference pointer so sendContainer gets a value type
	q.GetAndSend(td, func(v any) any {
		sp := v.(*tree.TypeDecl).Specs
		if sp == nil { return nil }
		return *sp
	}, func(v any) { sendContainer(s, v, q) })
	return td
}

func (s *GoSender) VisitMultiAssignment(ma *tree.MultiAssignment, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSendList(ma,
		func(v any) []any {
			vars := v.(*tree.MultiAssignment).Variables
			result := make([]any, len(vars))
			for i, e := range vars {
				result[i] = e
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(ma, func(v any) any { return v.(*tree.MultiAssignment).Operator },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSendList(ma,
		func(v any) []any {
			vals := v.(*tree.MultiAssignment).Values
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

func (s *GoSender) VisitCommClause(cc *tree.CommClause, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(cc, func(v any) any { return v.(*tree.CommClause).Comm },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(cc, func(v any) any { return v.(*tree.CommClause).Colon },
		func(v any) { sendSpace(v.(tree.Space), q) })
	q.GetAndSendList(cc,
		func(v any) []any {
			body := v.(*tree.CommClause).Body
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
func (s *GoSender) sendParseError(pe *tree.ParseError, q *SendQueue) {
	q.GetAndSend(pe, func(v any) any { return v.(*tree.ParseError).Ident.String() }, nil)
	q.GetAndSend(pe, func(v any) any { return v.(*tree.ParseError).Markers },
		func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
	q.GetAndSend(pe, func(v any) any { return v.(*tree.ParseError).SourcePath }, nil)
	q.GetAndSend(pe, func(v any) any { return v.(*tree.ParseError).CharsetName }, nil)
	q.GetAndSend(pe, func(v any) any { return v.(*tree.ParseError).CharsetBomMarked }, nil)
	q.GetAndSend(pe, func(_ any) any { return nil }, nil) // checksum
	q.GetAndSend(pe, func(_ any) any { return nil }, nil) // fileAttributes
	q.GetAndSend(pe, func(v any) any { return v.(*tree.ParseError).Text }, nil)
}

func (s *GoSender) VisitIndexList(il *tree.IndexList, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(il, func(v any) any { return v.(*tree.IndexList).Target },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(il, func(v any) any { return v.(*tree.IndexList).Indices },
		func(v any) { sendContainer(s, v, q) })
	return il
}


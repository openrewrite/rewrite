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
)

// GoSender serializes Go AST nodes into the send queue.
// Handles G (Go-specific) nodes and delegates J nodes to JavaSender.
type GoSender struct {
	java JavaSender
}

// NewGoSender creates a GoSender with its JavaSender properly wired.
func NewGoSender() *GoSender {
	gs := &GoSender{}
	gs.java = JavaSender{
		typeSender: NewJavaTypeSender(),
		parent:     gs,
	}
	return gs
}

// Visit dispatches to the appropriate send method based on node type.
func (s *GoSender) Visit(node any, q *SendQueue) {
	if node == nil {
		return
	}

	// ParseError has its own codec — handle before preVisit (no prefix field)
	if pe, ok := node.(*tree.ParseError); ok {
		s.sendParseError(pe, q)
		return
	}

	// preVisit: send ID, prefix, markers
	s.preVisit(node, q)

	switch v := node.(type) {
	// G nodes (Go-specific)
	case *tree.CompilationUnit:
		s.sendCompilationUnit(v, q)
	case *tree.GoStmt:
		s.sendGoStmt(v, q)
	case *tree.Defer:
		s.sendDefer(v, q)
	case *tree.Send:
		s.sendSend(v, q)
	case *tree.Goto:
		s.sendGoto(v, q)
	case *tree.Fallthrough:
		// No fields
	case *tree.Composite:
		s.sendComposite(v, q)
	case *tree.KeyValue:
		s.sendKeyValue(v, q)
	case *tree.Slice:
		s.sendSlice(v, q)
	case *tree.MapType:
		s.sendMapType(v, q)
	case *tree.StatementExpression:
		s.sendStatementExpression(v, q)
	case *tree.PointerType:
		s.sendPointerType(v, q)
	case *tree.Channel:
		s.sendChannel(v, q)
	case *tree.FuncType:
		s.sendFuncType(v, q)
	case *tree.StructType:
		s.sendStructType(v, q)
	case *tree.InterfaceType:
		s.sendInterfaceType(v, q)
	case *tree.TypeList:
		s.sendTypeList(v, q)
	case *tree.TypeDecl:
		s.sendTypeDecl(v, q)
	case *tree.MultiAssignment:
		s.sendMultiAssignment(v, q)
	case *tree.CommClause:
		s.sendCommClause(v, q)
	case *tree.IndexList:
		s.sendIndexList(v, q)

	default:
		// Delegate all J nodes to JavaSender
		s.java.visitJ(node, q)
	}
}

func (s *GoSender) preVisit(node any, q *SendQueue) {
	q.GetAndSend(node, nodeID, nil)
	q.GetAndSend(node, nodePrefix, func(v any) { sendSpace(v.(tree.Space), q) })
	q.GetAndSend(node, nodeMarkers, func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
}

// --- G nodes ---

func (s *GoSender) sendCompilationUnit(cu *tree.CompilationUnit, q *SendQueue) {
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
}

func (s *GoSender) sendGoStmt(gs *tree.GoStmt, q *SendQueue) {
	q.GetAndSend(gs, func(v any) any { return v.(*tree.GoStmt).Expr },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendDefer(d *tree.Defer, q *SendQueue) {
	q.GetAndSend(d, func(v any) any { return v.(*tree.Defer).Expr },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendSend(sn *tree.Send, q *SendQueue) {
	q.GetAndSend(sn, func(v any) any { return v.(*tree.Send).Channel },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(sn, func(v any) any { return v.(*tree.Send).Arrow },
		func(v any) { sendLeftPadded(s, v, q) })
}

func (s *GoSender) sendGoto(g *tree.Goto, q *SendQueue) {
	q.GetAndSend(g, func(v any) any { return v.(*tree.Goto).Label },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendComposite(c *tree.Composite, q *SendQueue) {
	q.GetAndSend(c, func(v any) any { return v.(*tree.Composite).TypeExpr },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(c, func(v any) any { return v.(*tree.Composite).Elements },
		func(v any) { sendContainer(s, v, q) })
}

func (s *GoSender) sendKeyValue(kv *tree.KeyValue, q *SendQueue) {
	q.GetAndSend(kv, func(v any) any { return v.(*tree.KeyValue).Key },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(kv, func(v any) any { return v.(*tree.KeyValue).Value },
		func(v any) { sendLeftPadded(s, v, q) })
}

func (s *GoSender) sendSlice(sl *tree.Slice, q *SendQueue) {
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).Indexed },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).OpenBracket },
		func(v any) { sendSpace(v.(tree.Space), q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).Low },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).High },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).Max },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(sl, func(v any) any { return v.(*tree.Slice).CloseBracket },
		func(v any) { sendSpace(v.(tree.Space), q) })
}

func (s *GoSender) sendMapType(mt *tree.MapType, q *SendQueue) {
	q.GetAndSend(mt, func(v any) any { return v.(*tree.MapType).OpenBracket },
		func(v any) { sendSpace(v.(tree.Space), q) })
	q.GetAndSend(mt, func(v any) any { return v.(*tree.MapType).Key },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(mt, func(v any) any { return v.(*tree.MapType).Value },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendStatementExpression(se *tree.StatementExpression, q *SendQueue) {
	q.GetAndSend(se, func(v any) any { return v.(*tree.StatementExpression).Statement },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendPointerType(pt *tree.PointerType, q *SendQueue) {
	q.GetAndSend(pt, func(v any) any { return v.(*tree.PointerType).Elem },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendChannel(ch *tree.Channel, q *SendQueue) {
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
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendFuncType(ft *tree.FuncType, q *SendQueue) {
	q.GetAndSend(ft, func(v any) any { return v.(*tree.FuncType).Parameters },
		func(v any) { sendContainer(s, v, q) })
	q.GetAndSend(ft, func(v any) any { return v.(*tree.FuncType).ReturnType },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendStructType(st *tree.StructType, q *SendQueue) {
	q.GetAndSend(st, func(v any) any { return v.(*tree.StructType).Body },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendInterfaceType(it *tree.InterfaceType, q *SendQueue) {
	q.GetAndSend(it, func(v any) any { return v.(*tree.InterfaceType).Body },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendTypeList(tl *tree.TypeList, q *SendQueue) {
	q.GetAndSend(tl, func(v any) any { return v.(*tree.TypeList).Types },
		func(v any) { sendContainer(s, v, q) })
}

func (s *GoSender) sendTypeDecl(td *tree.TypeDecl, q *SendQueue) {
	q.GetAndSend(td, func(v any) any { return v.(*tree.TypeDecl).Name },
		func(v any) { s.Visit(v, q) })
	// Assign — dereference pointer so sendLeftPadded gets a value type
	q.GetAndSend(td, func(v any) any {
		a := v.(*tree.TypeDecl).Assign
		if a == nil { return nil }
		return *a
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(td, func(v any) any { return v.(*tree.TypeDecl).Definition },
		func(v any) { s.Visit(v, q) })
	// Specs — dereference pointer so sendContainer gets a value type
	q.GetAndSend(td, func(v any) any {
		sp := v.(*tree.TypeDecl).Specs
		if sp == nil { return nil }
		return *sp
	}, func(v any) { sendContainer(s, v, q) })
}

func (s *GoSender) sendMultiAssignment(ma *tree.MultiAssignment, q *SendQueue) {
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
}

func (s *GoSender) sendCommClause(cc *tree.CommClause, q *SendQueue) {
	q.GetAndSend(cc, func(v any) any { return v.(*tree.CommClause).Comm },
		func(v any) { s.Visit(v, q) })
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
}

// sendParseError serializes a ParseError matching Java's ParseError.rpcSend field order:
// id, markers, sourcePath, charsetName, charsetBomMarked, checksum, fileAttributes, text
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

func (s *GoSender) sendIndexList(il *tree.IndexList, q *SendQueue) {
	q.GetAndSend(il, func(v any) any { return v.(*tree.IndexList).Target },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(il, func(v any) any { return v.(*tree.IndexList).Indices },
		func(v any) { sendContainer(s, v, q) })
}


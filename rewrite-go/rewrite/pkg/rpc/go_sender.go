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

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/pkg/tree"
)

// GoSender serializes Go AST nodes into the send queue.
// Mirrors GolangSender.java + JavaSender.java for J nodes.
type GoSender struct {
	typeSender JavaTypeSender
}

// visit dispatches to the appropriate send method based on node type.
func (s *GoSender) Visit(node any, q *SendQueue) {
	if node == nil {
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

	// J nodes (shared Java-like nodes)
	case *tree.Identifier:
		s.sendIdentifier(v, q)
	case *tree.Literal:
		s.sendLiteral(v, q)
	case *tree.Binary:
		s.sendBinary(v, q)
	case *tree.Block:
		s.sendBlock(v, q)
	case *tree.Return:
		s.sendReturn(v, q)
	case *tree.If:
		s.sendIf(v, q)
	case *tree.Else:
		s.sendElse(v, q)
	case *tree.Assignment:
		s.sendAssignment(v, q)
	case *tree.AssignmentOperation:
		s.sendAssignmentOperation(v, q)
	case *tree.MethodDeclaration:
		s.sendMethodDeclaration(v, q)
	case *tree.ForLoop:
		s.sendForLoop(v, q)
	case *tree.ForControl:
		s.sendForControl(v, q)
	case *tree.ForEachLoop:
		s.sendForEachLoop(v, q)
	case *tree.ForEachControl:
		s.sendForEachControl(v, q)
	case *tree.Switch:
		s.sendSwitch(v, q)
	case *tree.Case:
		s.sendCase(v, q)
	case *tree.Break:
		s.sendBreak(v, q)
	case *tree.Continue:
		s.sendContinue(v, q)
	case *tree.Label:
		s.sendLabel(v, q)
	case *tree.Empty:
		// No fields
	case *tree.Unary:
		s.sendUnary(v, q)
	case *tree.FieldAccess:
		s.sendFieldAccess(v, q)
	case *tree.MethodInvocation:
		s.sendMethodInvocation(v, q)
	case *tree.VariableDeclarations:
		s.sendVariableDeclarations(v, q)
	case *tree.VariableDeclarator:
		s.sendVariableDeclarator(v, q)
	case *tree.ArrayType:
		s.sendArrayType(v, q)
	case *tree.ArrayAccess:
		s.sendArrayAccess(v, q)
	case *tree.ArrayDimension:
		s.sendArrayDimension(v, q)
	case *tree.Parentheses:
		s.sendParentheses(v, q)
	case *tree.TypeCast:
		s.sendTypeCast(v, q)
	case *tree.ControlParentheses:
		s.sendControlParentheses(v, q)
	case *tree.Import:
		s.sendImport(v, q)
	default:
		panic(fmt.Sprintf("GoSender: unsupported node type %T", node))
	}
}

func (s *GoSender) preVisit(node any, q *SendQueue) {
	q.GetAndSend(node, nodeID, nil)
	q.GetAndSend(node, nodePrefix, func(v any) { sendSpace(v.(tree.Space), q) })
	q.GetAndSend(node, nodeMarkers, func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
}

// visitType sends a JavaType through the type sender with null/Unknown handling.
func (s *GoSender) visitType(t tree.JavaType, q *SendQueue) {
	if isNilValue(t) {
		return
	}
	if _, ok := t.(*tree.JavaTypeUnknown); ok {
		return
	}
	s.typeSender.VisitType(t, q)
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
	// packageDecl (right-padded)
	q.GetAndSend(cu, func(v any) any { return v.(*tree.CompilationUnit).PackageDecl },
		func(v any) { sendRightPadded(s, v, q) })
	// imports (list of right-padded)
	q.GetAndSendList(cu,
		func(v any) []any {
			c := v.(*tree.CompilationUnit)
			if c.Imports == nil {
				return nil
			}
			result := make([]any, len(c.Imports.Elements))
			for i, e := range c.Imports.Elements {
				result[i] = e
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
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
	q.GetAndSend(td, func(v any) any { return v.(*tree.TypeDecl).Assign },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(td, func(v any) any { return v.(*tree.TypeDecl).Definition },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(td, func(v any) any { return v.(*tree.TypeDecl).Specs },
		func(v any) { sendContainer(s, v, q) })
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

func (s *GoSender) sendIndexList(il *tree.IndexList, q *SendQueue) {
	q.GetAndSend(il, func(v any) any { return v.(*tree.IndexList).Target },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(il, func(v any) any { return v.(*tree.IndexList).Indices },
		func(v any) { sendContainer(s, v, q) })
}

// --- J nodes ---

func (s *GoSender) sendIdentifier(id *tree.Identifier, q *SendQueue) {
	// annotations (list)
	q.GetAndSendList(id,
		func(v any) []any {
			annots := v.(*tree.Identifier).Annotations
			if annots == nil {
				return nil
			}
			result := make([]any, len(annots))
			for i, a := range annots {
				result[i] = a
			}
			return result
		},
		func(v any) any { return extractID(v) },
		func(v any) { s.Visit(v, q) })
	// simpleName
	q.GetAndSend(id, func(v any) any { return v.(*tree.Identifier).Name }, nil)
	// type (as ref)
	q.GetAndSend(id, func(v any) any { return AsRef(v.(*tree.Identifier).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	// fieldType (as ref)
	q.GetAndSend(id, func(v any) any { return AsRef(v.(*tree.Identifier).FieldType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendLiteral(lit *tree.Literal, q *SendQueue) {
	// value
	q.GetAndSend(lit, func(v any) any { return v.(*tree.Literal).Value }, nil)
	// valueSource (source text)
	q.GetAndSend(lit, func(v any) any { return v.(*tree.Literal).Source }, nil)
	// unicodeEscapes (empty for Go)
	q.GetAndSendList(lit, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// type (as ref)
	q.GetAndSend(lit, func(v any) any { return AsRef(v.(*tree.Literal).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendBinary(b *tree.Binary, q *SendQueue) {
	q.GetAndSend(b, func(v any) any { return v.(*tree.Binary).Left },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(b, func(v any) any {
		op := v.(*tree.Binary).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(b, func(v any) any { return v.(*tree.Binary).Right },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(b, func(v any) any { return AsRef(v.(*tree.Binary).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendBlock(b *tree.Block, q *SendQueue) {
	// static (right-padded bool) - Java's JRightPadded<Boolean> with element=false
	// Send manually since Go doesn't have RightPadded[bool]
	sendRightPaddedBool(false, tree.EmptySpace, tree.Markers{}, q)
	// statements
	q.GetAndSendList(b,
		func(v any) []any {
			stmts := v.(*tree.Block).Statements
			result := make([]any, len(stmts))
			for i, stmt := range stmts {
				result[i] = stmt
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	// end space
	q.GetAndSend(b, func(v any) any { return v.(*tree.Block).End },
		func(v any) { sendSpace(v.(tree.Space), q) })
}

func (s *GoSender) sendReturn(r *tree.Return, q *SendQueue) {
	// Java's J.Return has a single expression; Go has multiple
	// The first expression maps to J.Return.expression
	q.GetAndSend(r, func(v any) any {
		exprs := v.(*tree.Return).Expressions
		if len(exprs) > 0 {
			return exprs[0].Element
		}
		return nil
	}, func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendIf(i *tree.If, q *SendQueue) {
	// ifCondition - wrap raw condition in ControlParentheses for Java's J.If model
	q.GetAndSend(i, func(v any) any {
		cond := v.(*tree.If).Condition
		return &tree.ControlParentheses{
			ID:      uuid.New(),
			Markers: tree.Markers{ID: uuid.New()},
			Tree:    tree.RightPadded[tree.Expression]{Element: cond, After: tree.EmptySpace},
		}
	}, func(v any) { s.Visit(v, q) })
	// thenPart (right-padded)
	q.GetAndSend(i, func(v any) any {
		return tree.RightPadded[tree.Statement]{
			Element: v.(*tree.If).Then,
			After:   tree.EmptySpace,
		}
	}, func(v any) { sendRightPadded(s, v, q) })
	// elsePart - wrap in Else node for Java's J.If.Else model
	q.GetAndSend(i, func(v any) any {
		ep := v.(*tree.If).ElsePart
		if ep == nil {
			return nil
		}
		return &tree.Else{
			ID:      uuid.New(),
			Prefix:  ep.After,
			Markers: tree.Markers{ID: uuid.New()},
			Body:    tree.RightPadded[tree.Statement]{Element: ep.Element.(tree.Statement), After: tree.EmptySpace},
		}
	}, func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendElse(el *tree.Else, q *SendQueue) {
	// body (right-padded Statement)
	q.GetAndSend(el, func(v any) any { return v.(*tree.Else).Body },
		func(v any) { sendRightPadded(s, v, q) })
}

func (s *GoSender) sendAssignment(a *tree.Assignment, q *SendQueue) {
	q.GetAndSend(a, func(v any) any { return v.(*tree.Assignment).Variable },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(a, func(v any) any { return v.(*tree.Assignment).Value },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*tree.Assignment).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendAssignmentOperation(a *tree.AssignmentOperation, q *SendQueue) {
	q.GetAndSend(a, func(v any) any { return v.(*tree.AssignmentOperation).Variable },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(a, func(v any) any {
		op := v.(*tree.AssignmentOperation).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(a, func(v any) any { return v.(*tree.AssignmentOperation).Assignment },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*tree.AssignmentOperation).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendMethodDeclaration(md *tree.MethodDeclaration, q *SendQueue) {
	// Go's MethodDeclaration maps to parts of Java's MethodDeclaration
	// Java sends: leadingAnnotations, modifiers, typeParameters, returnTypeExpression,
	//   name annotations, name, parameters, throws, body, defaultValue, methodType
	// Go: receiver, name, parameters, returnType, body, methodType

	// leadingAnnotations (empty for Go)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// modifiers (empty for Go)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// typeParameters (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// returnTypeExpression
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).ReturnType },
		func(v any) { s.Visit(v, q) })
	// name annotations (empty)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Name },
		func(v any) { s.Visit(v, q) })
	// parameters (container)
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Parameters },
		func(v any) { sendContainer(s, v, q) })
	// throws (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// body
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Body },
		func(v any) { s.Visit(v, q) })
	// defaultValue (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// methodType (as ref)
	q.GetAndSend(md, func(v any) any { return AsRef(v.(*tree.MethodDeclaration).MethodType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendForLoop(f *tree.ForLoop, q *SendQueue) {
	q.GetAndSend(f, func(v any) any {
		ctrl := v.(*tree.ForLoop).Control
		return &ctrl
	}, func(v any) { s.Visit(v, q) })
	q.GetAndSend(f, func(v any) any {
		return tree.RightPadded[tree.Statement]{Element: v.(*tree.ForLoop).Body, After: tree.EmptySpace}
	}, func(v any) { sendRightPadded(s, v, q) })
}

func (s *GoSender) sendForControl(fc *tree.ForControl, q *SendQueue) {
	// init (list of right-padded)
	q.GetAndSendList(fc,
		func(v any) []any {
			init := v.(*tree.ForControl).Init
			if init == nil {
				return nil
			}
			return []any{*init}
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	// condition (right-padded)
	q.GetAndSend(fc, func(v any) any { return v.(*tree.ForControl).Condition },
		func(v any) { sendRightPadded(s, v, q) })
	// update (list of right-padded)
	q.GetAndSendList(fc,
		func(v any) []any {
			update := v.(*tree.ForControl).Update
			if update == nil {
				return nil
			}
			return []any{*update}
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
}

func (s *GoSender) sendForEachLoop(f *tree.ForEachLoop, q *SendQueue) {
	q.GetAndSend(f, func(v any) any {
		ctrl := v.(*tree.ForEachLoop).Control
		return &ctrl
	}, func(v any) { s.Visit(v, q) })
	q.GetAndSend(f, func(v any) any {
		return tree.RightPadded[tree.Statement]{Element: v.(*tree.ForEachLoop).Body, After: tree.EmptySpace}
	}, func(v any) { sendRightPadded(s, v, q) })
}

func (s *GoSender) sendForEachControl(fc *tree.ForEachControl, q *SendQueue) {
	// Go sends: key (right-padded), value (right-padded), operator (left-padded string), iterable
	// Java GolangReceiver override reads this format
	q.GetAndSend(fc, func(v any) any { return v.(*tree.ForEachControl).Key },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any { return v.(*tree.ForEachControl).Value },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any {
		op := v.(*tree.ForEachControl).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any { return v.(*tree.ForEachControl).Iterable },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendSwitch(sw *tree.Switch, q *SendQueue) {
	// selector - wrap tag in ControlParentheses for Java's J.Switch model
	q.GetAndSend(sw, func(v any) any {
		tag := v.(*tree.Switch).Tag
		var inner tree.Expression
		if tag != nil {
			inner = tag.Element
		} else {
			// Tagless switch: use Empty as the expression
			inner = &tree.Empty{ID: uuid.New()}
		}
		return &tree.ControlParentheses{
			ID:      uuid.New(),
			Markers: tree.Markers{ID: uuid.New()},
			Tree:    tree.RightPadded[tree.Expression]{Element: inner, After: tree.EmptySpace},
		}
	}, func(v any) { s.Visit(v, q) })
	// cases (Block)
	q.GetAndSend(sw, func(v any) any { return v.(*tree.Switch).Body },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendCase(c *tree.Case, q *SendQueue) {
	// type (enum value)
	q.GetAndSend(c, func(_ any) any { return "Statement" }, nil)
	// caseLabels (container)
	q.GetAndSend(c, func(v any) any { return v.(*tree.Case).Expressions },
		func(v any) { sendContainer(s, v, q) })
	// statements (container)
	q.GetAndSend(c, func(v any) any {
		body := v.(*tree.Case).Body
		result := make([]tree.RightPadded[tree.Statement], len(body))
		copy(result, body)
		return tree.Container[tree.Statement]{Elements: result}
	}, func(v any) { sendContainer(s, v, q) })
	// body (right-padded, nil for Go-style case)
	q.GetAndSend(c, func(_ any) any { return nil }, nil)
	// guard (nil for Go)
	q.GetAndSend(c, func(_ any) any { return nil }, nil)
}

func (s *GoSender) sendBreak(b *tree.Break, q *SendQueue) {
	q.GetAndSend(b, func(v any) any { return v.(*tree.Break).Label },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendContinue(c *tree.Continue, q *SendQueue) {
	q.GetAndSend(c, func(v any) any { return v.(*tree.Continue).Label },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendLabel(l *tree.Label, q *SendQueue) {
	q.GetAndSend(l, func(v any) any { return v.(*tree.Label).Name },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(l, func(v any) any { return v.(*tree.Label).Statement },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendUnary(u *tree.Unary, q *SendQueue) {
	q.GetAndSend(u, func(v any) any {
		op := v.(*tree.Unary).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(u, func(v any) any { return v.(*tree.Unary).Operand },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(u, func(v any) any { return AsRef(v.(*tree.Unary).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendFieldAccess(fa *tree.FieldAccess, q *SendQueue) {
	q.GetAndSend(fa, func(v any) any { return v.(*tree.FieldAccess).Target },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(fa, func(v any) any { return v.(*tree.FieldAccess).Name },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(fa, func(v any) any { return AsRef(v.(*tree.FieldAccess).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendMethodInvocation(mi *tree.MethodInvocation, q *SendQueue) {
	// select (right-padded, nullable)
	q.GetAndSend(mi, func(v any) any { return v.(*tree.MethodInvocation).Select },
		func(v any) { sendRightPadded(s, v, q) })
	// typeParameters (nil for Go)
	q.GetAndSend(mi, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(mi, func(v any) any { return v.(*tree.MethodInvocation).Name },
		func(v any) { s.Visit(v, q) })
	// arguments (container)
	q.GetAndSend(mi, func(v any) any { return v.(*tree.MethodInvocation).Arguments },
		func(v any) { sendContainer(s, v, q) })
	// methodType (as ref)
	q.GetAndSend(mi, func(v any) any { return AsRef(v.(*tree.MethodInvocation).MethodType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *GoSender) sendVariableDeclarations(vd *tree.VariableDeclarations, q *SendQueue) {
	// leadingAnnotations (empty)
	q.GetAndSendList(vd, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// modifiers (empty)
	q.GetAndSendList(vd, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// typeExpression
	q.GetAndSend(vd, func(v any) any { return v.(*tree.VariableDeclarations).TypeExpr },
		func(v any) { s.Visit(v, q) })
	// varargs (nil for Go)
	q.GetAndSend(vd, func(_ any) any { return nil }, nil)
	// variables (list of right-padded NamedVariable)
	q.GetAndSendList(vd,
		func(v any) []any {
			vars := v.(*tree.VariableDeclarations).Variables
			result := make([]any, len(vars))
			for i, d := range vars {
				result[i] = d
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
}

func (s *GoSender) sendVariableDeclarator(vd *tree.VariableDeclarator, q *SendQueue) {
	// Java's NamedVariable: declarator (Identifier), dimensionsAfterName, initializer, variableType
	// Go: Name, Initializer
	q.GetAndSend(vd, func(v any) any { return v.(*tree.VariableDeclarator).Name },
		func(v any) { s.Visit(v, q) })
	// dimensionsAfterName (empty for Go)
	q.GetAndSendList(vd, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// initializer (left-padded, nullable)
	q.GetAndSend(vd, func(v any) any { return v.(*tree.VariableDeclarator).Initializer },
		func(v any) { sendLeftPadded(s, v, q) })
	// variableType (as ref) - not yet on Go VariableDeclarator
	q.GetAndSend(vd, func(_ any) any { return nil }, nil)
}

func (s *GoSender) sendArrayType(at *tree.ArrayType, q *SendQueue) {
	// elementType
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).ElementType },
		func(v any) { s.Visit(v, q) })
	// annotations (empty for Go)
	q.GetAndSendList(at, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// dimension (left-padded)
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).Dimension },
		func(v any) { sendLeftPadded(s, v, q) })
	// type
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).Type }, nil)
}

func (s *GoSender) sendArrayAccess(aa *tree.ArrayAccess, q *SendQueue) {
	q.GetAndSend(aa, func(v any) any { return v.(*tree.ArrayAccess).Indexed },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(aa, func(v any) any { return v.(*tree.ArrayAccess).Dimension },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendArrayDimension(ad *tree.ArrayDimension, q *SendQueue) {
	q.GetAndSend(ad, func(v any) any { return ad.Index },
		func(v any) { sendRightPadded(s, v, q) })
}

func (s *GoSender) sendParentheses(p *tree.Parentheses, q *SendQueue) {
	q.GetAndSend(p, func(v any) any { return v.(*tree.Parentheses).Tree },
		func(v any) { sendRightPadded(s, v, q) })
}

func (s *GoSender) sendTypeCast(tc *tree.TypeCast, q *SendQueue) {
	q.GetAndSend(tc, func(v any) any { return v.(*tree.TypeCast).Clazz },
		func(v any) { s.Visit(v, q) })
	q.GetAndSend(tc, func(v any) any { return v.(*tree.TypeCast).Expr },
		func(v any) { s.Visit(v, q) })
}

func (s *GoSender) sendControlParentheses(cp *tree.ControlParentheses, q *SendQueue) {
	q.GetAndSend(cp, func(v any) any { return v.(*tree.ControlParentheses).Tree },
		func(v any) { sendRightPadded(s, v, q) })
}

func (s *GoSender) sendImport(imp *tree.Import, q *SendQueue) {
	// Java Import: static (left-padded), qualid, alias (left-padded)
	// Static is always false for Go
	q.GetAndSend(imp, func(_ any) any {
		return tree.LeftPadded[bool]{Before: tree.EmptySpace, Element: false}
	}, func(v any) { sendLeftPadded(s, v, q) })
	// qualid
	q.GetAndSend(imp, func(v any) any { return v.(*tree.Import).Qualid },
		func(v any) { s.Visit(v, q) })
	// alias
	q.GetAndSend(imp, func(v any) any { return v.(*tree.Import).Alias },
		func(v any) { sendLeftPadded(s, v, q) })
}

// Node accessor helpers

func nodeID(v any) any {
	id := extractID(v)
	if id == nil {
		return nil
	}
	// Convert uuid.UUID to string for JSON serialization
	if u, ok := id.(uuid.UUID); ok {
		return u.String()
	}
	return id
}

func nodePrefix(v any) any {
	switch n := v.(type) {
	case *tree.CompilationUnit:
		return n.Prefix
	case *tree.GoStmt:
		return n.Prefix
	case *tree.Defer:
		return n.Prefix
	case *tree.Send:
		return n.Prefix
	case *tree.Goto:
		return n.Prefix
	case *tree.Fallthrough:
		return n.Prefix
	case *tree.Composite:
		return n.Prefix
	case *tree.KeyValue:
		return n.Prefix
	case *tree.Slice:
		return n.Prefix
	case *tree.MapType:
		return n.Prefix
	case *tree.Channel:
		return n.Prefix
	case *tree.FuncType:
		return n.Prefix
	case *tree.StructType:
		return n.Prefix
	case *tree.InterfaceType:
		return n.Prefix
	case *tree.TypeList:
		return n.Prefix
	case *tree.TypeDecl:
		return n.Prefix
	case *tree.MultiAssignment:
		return n.Prefix
	case *tree.CommClause:
		return n.Prefix
	case *tree.IndexList:
		return n.Prefix
	case *tree.Identifier:
		return n.Prefix
	case *tree.Literal:
		return n.Prefix
	case *tree.Binary:
		return n.Prefix
	case *tree.Block:
		return n.Prefix
	case *tree.Return:
		return n.Prefix
	case *tree.If:
		return n.Prefix
	case *tree.Else:
		return n.Prefix
	case *tree.Assignment:
		return n.Prefix
	case *tree.AssignmentOperation:
		return n.Prefix
	case *tree.MethodDeclaration:
		return n.Prefix
	case *tree.ForLoop:
		return n.Prefix
	case *tree.ForControl:
		return n.Prefix
	case *tree.ForEachLoop:
		return n.Prefix
	case *tree.ForEachControl:
		return n.Prefix
	case *tree.Switch:
		return n.Prefix
	case *tree.Case:
		return n.Prefix
	case *tree.Break:
		return n.Prefix
	case *tree.Continue:
		return n.Prefix
	case *tree.Label:
		return n.Prefix
	case *tree.Empty:
		return n.Prefix
	case *tree.Unary:
		return n.Prefix
	case *tree.FieldAccess:
		return n.Prefix
	case *tree.MethodInvocation:
		return n.Prefix
	case *tree.VariableDeclarations:
		return n.Prefix
	case *tree.VariableDeclarator:
		return n.Prefix
	case *tree.ArrayType:
		return n.Prefix
	case *tree.ArrayAccess:
		return n.Prefix
	case *tree.ArrayDimension:
		return n.Prefix
	case *tree.Parentheses:
		return n.Prefix
	case *tree.TypeCast:
		return n.Prefix
	case *tree.ControlParentheses:
		return n.Prefix
	case *tree.Import:
		return n.Prefix
	default:
		return tree.EmptySpace
	}
}

func nodeMarkers(v any) any {
	switch n := v.(type) {
	case *tree.CompilationUnit:
		return n.Markers
	case *tree.GoStmt:
		return n.Markers
	case *tree.Defer:
		return n.Markers
	case *tree.Send:
		return n.Markers
	case *tree.Goto:
		return n.Markers
	case *tree.Fallthrough:
		return n.Markers
	case *tree.Composite:
		return n.Markers
	case *tree.KeyValue:
		return n.Markers
	case *tree.Slice:
		return n.Markers
	case *tree.MapType:
		return n.Markers
	case *tree.Channel:
		return n.Markers
	case *tree.FuncType:
		return n.Markers
	case *tree.StructType:
		return n.Markers
	case *tree.InterfaceType:
		return n.Markers
	case *tree.TypeList:
		return n.Markers
	case *tree.TypeDecl:
		return n.Markers
	case *tree.MultiAssignment:
		return n.Markers
	case *tree.CommClause:
		return n.Markers
	case *tree.IndexList:
		return n.Markers
	case *tree.Identifier:
		return n.Markers
	case *tree.Literal:
		return n.Markers
	case *tree.Binary:
		return n.Markers
	case *tree.Block:
		return n.Markers
	case *tree.Return:
		return n.Markers
	case *tree.If:
		return n.Markers
	case *tree.Else:
		return n.Markers
	case *tree.Assignment:
		return n.Markers
	case *tree.AssignmentOperation:
		return n.Markers
	case *tree.MethodDeclaration:
		return n.Markers
	case *tree.ForLoop:
		return n.Markers
	case *tree.ForControl:
		return n.Markers
	case *tree.ForEachLoop:
		return n.Markers
	case *tree.ForEachControl:
		return n.Markers
	case *tree.Switch:
		return n.Markers
	case *tree.Case:
		return n.Markers
	case *tree.Break:
		return n.Markers
	case *tree.Continue:
		return n.Markers
	case *tree.Label:
		return n.Markers
	case *tree.Empty:
		return n.Markers
	case *tree.Unary:
		return n.Markers
	case *tree.FieldAccess:
		return n.Markers
	case *tree.MethodInvocation:
		return n.Markers
	case *tree.VariableDeclarations:
		return n.Markers
	case *tree.VariableDeclarator:
		return n.Markers
	case *tree.ArrayType:
		return n.Markers
	case *tree.ArrayAccess:
		return n.Markers
	case *tree.ArrayDimension:
		return n.Markers
	case *tree.Parentheses:
		return n.Markers
	case *tree.TypeCast:
		return n.Markers
	case *tree.ControlParentheses:
		return n.Markers
	case *tree.Import:
		return n.Markers
	default:
		return tree.Markers{}
	}
}

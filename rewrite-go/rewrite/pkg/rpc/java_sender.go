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
	"github.com/openrewrite/rewrite/pkg/tree"
)

// Sender can serialize any AST node.
type Sender interface {
	Visit(node any, q *SendQueue)
}

// JavaSender serializes J (shared Java-like) AST nodes into the send queue.
// Mirrors JavaSender.java for J nodes.
type JavaSender struct {
	typeSender *JavaTypeSender
	parent     Sender // the GoSender (or other language sender) that delegates to us
}

// visitJ dispatches J-node field serialization (after preVisit has been called by the parent).
func (s *JavaSender) visitJ(node any, q *SendQueue) {
	switch v := node.(type) {
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
	}
}

// visitType sends a JavaType through the type sender with null/Unknown handling.
func (s *JavaSender) visitType(t tree.JavaType, q *SendQueue) {
	if isNilValue(t) {
		return
	}
	if _, ok := t.(*tree.JavaTypeUnknown); ok {
		return
	}
	s.typeSender.Visit(t, q)
}

// --- J nodes ---

func (s *JavaSender) sendIdentifier(id *tree.Identifier, q *SendQueue) {
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
		func(v any) { s.parent.Visit(v, q) })
	// simpleName
	q.GetAndSend(id, func(v any) any { return v.(*tree.Identifier).Name }, nil)
	// type (as ref)
	q.GetAndSend(id, func(v any) any { return AsRef(v.(*tree.Identifier).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	// fieldType (as ref)
	q.GetAndSend(id, func(v any) any { return AsRef(v.(*tree.Identifier).FieldType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *JavaSender) sendLiteral(lit *tree.Literal, q *SendQueue) {
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

func (s *JavaSender) sendBinary(b *tree.Binary, q *SendQueue) {
	q.GetAndSend(b, func(v any) any { return v.(*tree.Binary).Left },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(b, func(v any) any {
		op := v.(*tree.Binary).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s.parent, v, q) })
	q.GetAndSend(b, func(v any) any { return v.(*tree.Binary).Right },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(b, func(v any) any { return AsRef(v.(*tree.Binary).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *JavaSender) sendBlock(b *tree.Block, q *SendQueue) {
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
		func(v any) { sendRightPadded(s.parent, v, q) })
	// end space
	q.GetAndSend(b, func(v any) any { return v.(*tree.Block).End },
		func(v any) { sendSpace(v.(tree.Space), q) })
}

func (s *JavaSender) sendReturn(r *tree.Return, q *SendQueue) {
	// Java's J.Return has a single expression; Go has multiple
	// The first expression maps to J.Return.expression
	q.GetAndSend(r, func(v any) any {
		exprs := v.(*tree.Return).Expressions
		if len(exprs) > 0 {
			return exprs[0].Element
		}
		return nil
	}, func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendIf(i *tree.If, q *SendQueue) {
	// ifCondition - wrap raw condition in ControlParentheses for Java's J.If model
	q.GetAndSend(i, func(v any) any {
		cond := v.(*tree.If).Condition
		return &tree.ControlParentheses{
			ID:      uuid.New(),
			Markers: tree.Markers{ID: uuid.New()},
			Tree:    tree.RightPadded[tree.Expression]{Element: cond, After: tree.EmptySpace},
		}
	}, func(v any) { s.parent.Visit(v, q) })
	// thenPart (right-padded)
	q.GetAndSend(i, func(v any) any {
		return tree.RightPadded[tree.Statement]{
			Element: v.(*tree.If).Then,
			After:   tree.EmptySpace,
		}
	}, func(v any) { sendRightPadded(s.parent, v, q) })
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
	}, func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendElse(el *tree.Else, q *SendQueue) {
	// body (right-padded Statement)
	q.GetAndSend(el, func(v any) any { return v.(*tree.Else).Body },
		func(v any) { sendRightPadded(s.parent, v, q) })
}

func (s *JavaSender) sendAssignment(a *tree.Assignment, q *SendQueue) {
	q.GetAndSend(a, func(v any) any { return v.(*tree.Assignment).Variable },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(a, func(v any) any { return v.(*tree.Assignment).Value },
		func(v any) { sendLeftPadded(s.parent, v, q) })
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*tree.Assignment).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *JavaSender) sendAssignmentOperation(a *tree.AssignmentOperation, q *SendQueue) {
	q.GetAndSend(a, func(v any) any { return v.(*tree.AssignmentOperation).Variable },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(a, func(v any) any {
		op := v.(*tree.AssignmentOperation).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s.parent, v, q) })
	q.GetAndSend(a, func(v any) any { return v.(*tree.AssignmentOperation).Assignment },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*tree.AssignmentOperation).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *JavaSender) sendMethodDeclaration(md *tree.MethodDeclaration, q *SendQueue) {
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
		func(v any) { s.parent.Visit(v, q) })
	// name annotations (empty)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Name },
		func(v any) { s.parent.Visit(v, q) })
	// parameters (container)
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Parameters },
		func(v any) { sendContainer(s.parent, v, q) })
	// throws (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// body
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Body },
		func(v any) { s.parent.Visit(v, q) })
	// defaultValue (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// methodType (as ref)
	q.GetAndSend(md, func(v any) any { return AsRef(v.(*tree.MethodDeclaration).MethodType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *JavaSender) sendForLoop(f *tree.ForLoop, q *SendQueue) {
	q.GetAndSend(f, func(v any) any {
		ctrl := v.(*tree.ForLoop).Control
		return &ctrl
	}, func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(f, func(v any) any {
		return tree.RightPadded[tree.Statement]{Element: v.(*tree.ForLoop).Body, After: tree.EmptySpace}
	}, func(v any) { sendRightPadded(s.parent, v, q) })
}

func (s *JavaSender) sendForControl(fc *tree.ForControl, q *SendQueue) {
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
		func(v any) { sendRightPadded(s.parent, v, q) })
	// condition (right-padded) — dereference pointer
	q.GetAndSend(fc, func(v any) any {
		cond := v.(*tree.ForControl).Condition
		if cond == nil { return nil }
		return *cond
	}, func(v any) { sendRightPadded(s.parent, v, q) })
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
		func(v any) { sendRightPadded(s.parent, v, q) })
}

func (s *JavaSender) sendForEachLoop(f *tree.ForEachLoop, q *SendQueue) {
	q.GetAndSend(f, func(v any) any {
		ctrl := v.(*tree.ForEachLoop).Control
		return &ctrl
	}, func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(f, func(v any) any {
		return tree.RightPadded[tree.Statement]{Element: v.(*tree.ForEachLoop).Body, After: tree.EmptySpace}
	}, func(v any) { sendRightPadded(s.parent, v, q) })
}

func (s *JavaSender) sendForEachControl(fc *tree.ForEachControl, q *SendQueue) {
	// Go sends: key (right-padded), value (right-padded), operator (left-padded string), iterable
	// Java GolangReceiver override reads this format
	q.GetAndSend(fc, func(v any) any {
		k := v.(*tree.ForEachControl).Key
		if k == nil { return nil }
		return *k
	}, func(v any) { sendRightPadded(s.parent, v, q) })
	q.GetAndSend(fc, func(v any) any {
		val := v.(*tree.ForEachControl).Value
		if val == nil { return nil }
		return *val
	}, func(v any) { sendRightPadded(s.parent, v, q) })
	q.GetAndSend(fc, func(v any) any {
		op := v.(*tree.ForEachControl).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s.parent, v, q) })
	q.GetAndSend(fc, func(v any) any { return v.(*tree.ForEachControl).Iterable },
		func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendSwitch(sw *tree.Switch, q *SendQueue) {
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
	}, func(v any) { s.parent.Visit(v, q) })
	// cases (Block)
	q.GetAndSend(sw, func(v any) any { return v.(*tree.Switch).Body },
		func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendCase(c *tree.Case, q *SendQueue) {
	// type (enum value)
	q.GetAndSend(c, func(_ any) any { return "Statement" }, nil)
	// caseLabels (container)
	q.GetAndSend(c, func(v any) any { return v.(*tree.Case).Expressions },
		func(v any) { sendContainer(s.parent, v, q) })
	// statements (container)
	q.GetAndSend(c, func(v any) any {
		body := v.(*tree.Case).Body
		result := make([]tree.RightPadded[tree.Statement], len(body))
		copy(result, body)
		return tree.Container[tree.Statement]{Elements: result}
	}, func(v any) { sendContainer(s.parent, v, q) })
	// body (right-padded, nil for Go-style case)
	q.GetAndSend(c, func(_ any) any { return nil }, nil)
	// guard (nil for Go)
	q.GetAndSend(c, func(_ any) any { return nil }, nil)
}

func (s *JavaSender) sendBreak(b *tree.Break, q *SendQueue) {
	q.GetAndSend(b, func(v any) any { return v.(*tree.Break).Label },
		func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendContinue(c *tree.Continue, q *SendQueue) {
	q.GetAndSend(c, func(v any) any { return v.(*tree.Continue).Label },
		func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendLabel(l *tree.Label, q *SendQueue) {
	q.GetAndSend(l, func(v any) any { return v.(*tree.Label).Name },
		func(v any) { sendRightPadded(s.parent, v, q) })
	q.GetAndSend(l, func(v any) any { return v.(*tree.Label).Statement },
		func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendUnary(u *tree.Unary, q *SendQueue) {
	q.GetAndSend(u, func(v any) any {
		op := v.(*tree.Unary).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s.parent, v, q) })
	q.GetAndSend(u, func(v any) any { return v.(*tree.Unary).Operand },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(u, func(v any) any { return AsRef(v.(*tree.Unary).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *JavaSender) sendFieldAccess(fa *tree.FieldAccess, q *SendQueue) {
	q.GetAndSend(fa, func(v any) any { return v.(*tree.FieldAccess).Target },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(fa, func(v any) any { return v.(*tree.FieldAccess).Name },
		func(v any) { sendLeftPadded(s.parent, v, q) })
	q.GetAndSend(fa, func(v any) any { return AsRef(v.(*tree.FieldAccess).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *JavaSender) sendMethodInvocation(mi *tree.MethodInvocation, q *SendQueue) {
	// select (right-padded, nullable) — dereference pointer
	q.GetAndSend(mi, func(v any) any {
		sel := v.(*tree.MethodInvocation).Select
		if sel == nil { return nil }
		return *sel
	}, func(v any) { sendRightPadded(s.parent, v, q) })
	// typeParameters (nil for Go)
	q.GetAndSend(mi, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(mi, func(v any) any { return v.(*tree.MethodInvocation).Name },
		func(v any) { s.parent.Visit(v, q) })
	// arguments (container)
	q.GetAndSend(mi, func(v any) any { return v.(*tree.MethodInvocation).Arguments },
		func(v any) { sendContainer(s.parent, v, q) })
	// methodType (as ref)
	q.GetAndSend(mi, func(v any) any { return AsRef(v.(*tree.MethodInvocation).MethodType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
}

func (s *JavaSender) sendVariableDeclarations(vd *tree.VariableDeclarations, q *SendQueue) {
	// leadingAnnotations (empty -- Go has no annotations)
	q.GetAndSendList(vd, func(_ any) []any { return []any{} }, func(_ any) any { return nil }, nil)
	// modifiers (empty -- Go has no modifiers)
	q.GetAndSendList(vd, func(_ any) []any { return []any{} }, func(_ any) any { return nil }, nil)
	// typeExpression
	q.GetAndSend(vd, func(v any) any { return v.(*tree.VariableDeclarations).TypeExpr },
		func(v any) { s.parent.Visit(v, q) })
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
		func(v any) { sendRightPadded(s.parent, v, q) })
}

func (s *JavaSender) sendVariableDeclarator(vd *tree.VariableDeclarator, q *SendQueue) {
	// Java's NamedVariable: declarator (Identifier), dimensionsAfterName, initializer, variableType
	// Go: Name, Initializer
	q.GetAndSend(vd, func(v any) any { return v.(*tree.VariableDeclarator).Name },
		func(v any) { s.parent.Visit(v, q) })
	// dimensionsAfterName (empty for Go)
	q.GetAndSendList(vd, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// initializer (left-padded, nullable) — dereference pointer
	q.GetAndSend(vd, func(v any) any {
		init := v.(*tree.VariableDeclarator).Initializer
		if init == nil { return nil }
		return *init
	}, func(v any) { sendLeftPadded(s.parent, v, q) })
	// variableType (as ref) - not yet on Go VariableDeclarator
	q.GetAndSend(vd, func(_ any) any { return nil }, nil)
}

func (s *JavaSender) sendArrayType(at *tree.ArrayType, q *SendQueue) {
	// elementType
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).ElementType },
		func(v any) { s.parent.Visit(v, q) })
	// annotations (empty for Go)
	q.GetAndSendList(at, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// dimension (left-padded)
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).Dimension },
		func(v any) { sendLeftPadded(s.parent, v, q) })
	// type
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).Type }, nil)
}

func (s *JavaSender) sendArrayAccess(aa *tree.ArrayAccess, q *SendQueue) {
	q.GetAndSend(aa, func(v any) any { return v.(*tree.ArrayAccess).Indexed },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(aa, func(v any) any { return v.(*tree.ArrayAccess).Dimension },
		func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendArrayDimension(ad *tree.ArrayDimension, q *SendQueue) {
	q.GetAndSend(ad, func(v any) any { return ad.Index },
		func(v any) { sendRightPadded(s.parent, v, q) })
}

func (s *JavaSender) sendParentheses(p *tree.Parentheses, q *SendQueue) {
	q.GetAndSend(p, func(v any) any { return v.(*tree.Parentheses).Tree },
		func(v any) { sendRightPadded(s.parent, v, q) })
}

func (s *JavaSender) sendTypeCast(tc *tree.TypeCast, q *SendQueue) {
	q.GetAndSend(tc, func(v any) any { return v.(*tree.TypeCast).Clazz },
		func(v any) { s.parent.Visit(v, q) })
	q.GetAndSend(tc, func(v any) any { return v.(*tree.TypeCast).Expr },
		func(v any) { s.parent.Visit(v, q) })
}

func (s *JavaSender) sendControlParentheses(cp *tree.ControlParentheses, q *SendQueue) {
	q.GetAndSend(cp, func(v any) any { return v.(*tree.ControlParentheses).Tree },
		func(v any) { sendRightPadded(s.parent, v, q) })
}

func (s *JavaSender) sendImport(imp *tree.Import, q *SendQueue) {
	// Java Import: static (left-padded), qualid, alias (left-padded)
	// Static is always false for Go
	q.GetAndSend(imp, func(_ any) any {
		return tree.LeftPadded[bool]{Before: tree.EmptySpace, Element: false}
	}, func(v any) { sendLeftPadded(s.parent, v, q) })
	// qualid
	q.GetAndSend(imp, func(v any) any { return v.(*tree.Import).Qualid },
		func(v any) { s.parent.Visit(v, q) })
	// alias — dereference pointer
	q.GetAndSend(imp, func(v any) any {
		a := v.(*tree.Import).Alias
		if a == nil { return nil }
		return *a
	}, func(v any) { sendLeftPadded(s.parent, v, q) })
}

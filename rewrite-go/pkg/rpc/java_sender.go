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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Sender serializes a tree node into a SendQueue. Implementations are
// visitor.VisitorI-conformant so they slot into the standard visitor
// dispatch — mirrors rewrite-java's JavaSender, which extends
// JavaVisitor<RpcSendQueue>.
type Sender interface {
	Visit(t java.Tree, p any) java.Tree
}

// JavaSender serializes J (shared Java-like) AST nodes via the visitor
// pattern. Mirrors org.openrewrite.java.internal.rpc.JavaSender.
//
// Architecture: JavaSender embeds visitor.GoVisitor so the framework's
// type-switch dispatch routes calls into JavaSender's VisitX
// overrides. PreVisit handles the cross-cutting fields (id, prefix,
// markers) once per node, mirroring Java's JavaVisitor.preVisit.
//
// Language-specific senders (GoSender) embed JavaSender and override
// the additional G-node Visit methods on top.
type JavaSender struct {
	visitor.GoVisitor
	typeSender *JavaTypeSender
}

// PreVisit serializes the cross-cutting fields of every J node:
// id, prefix, markers. Called by the framework before the
// type-specific VisitX dispatch. ParseError isn't a J node and is
// special-cased at the GoSender layer.
//
// Field access goes through the polymorphic J-interface methods
// (GetID / GetPrefix / GetMarkers), mirroring rewrite-java's
// JavaVisitor.preVisit pattern.
func (s *JavaSender) PreVisit(t java.Tree, p any) java.Tree {
	j, ok := t.(java.J)
	if !ok {
		return t
	}
	q := p.(*SendQueue)
	q.GetAndSend(t, func(v any) any { return v.(java.J).GetID().String() }, nil)
	q.GetAndSend(t, func(v any) any { return v.(java.J).GetPrefix() },
		func(v any) { sendSpace(v.(java.Space), q) })
	q.GetAndSend(t, func(v any) any { return v.(java.J).GetMarkers() },
		func(v any) { SendMarkersCodec(v.(java.Markers), q) })
	_ = j
	return t
}

// visitType sends a JavaType through the type sender with null/Unknown handling.
func (s *JavaSender) visitType(t java.JavaType, q *SendQueue) {
	if isNilValue(t) {
		return
	}
	if _, ok := t.(*java.JavaTypeUnknown); ok {
		return
	}
	s.typeSender.Visit(t, q)
}

// --- J nodes ---

func (s *JavaSender) VisitIdentifier(id *java.Identifier, p any) java.J {
	q := p.(*SendQueue)
	// annotations (list)
	q.GetAndSendList(id,
		func(v any) []any {
			annots := v.(*java.Identifier).Annotations
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
		func(v any) { s.Visit(v.(java.Tree), q) })
	// simpleName
	q.GetAndSend(id, func(v any) any { return v.(*java.Identifier).Name }, nil)
	// type (as ref)
	q.GetAndSend(id, func(v any) any { return AsRef(v.(*java.Identifier).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	// fieldType (as ref)
	q.GetAndSend(id, func(v any) any { return AsRef(v.(*java.Identifier).FieldType) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return id
}

func (s *JavaSender) VisitLiteral(lit *java.Literal, p any) java.J {
	q := p.(*SendQueue)
	// value
	q.GetAndSend(lit, func(v any) any { return v.(*java.Literal).Value }, nil)
	// valueSource (source text)
	q.GetAndSend(lit, func(v any) any { return v.(*java.Literal).Source }, nil)
	// unicodeEscapes (empty for Go)
	q.GetAndSendList(lit, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// type (as ref)
	q.GetAndSend(lit, func(v any) any { return AsRef(v.(*java.Literal).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return lit
}

func (s *JavaSender) VisitBinary(b *java.Binary, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(b, func(v any) any { return v.(*java.Binary).Left },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(b, func(v any) any {
		op := v.(*java.Binary).Operator
		return java.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(b, func(v any) any { return v.(*java.Binary).Right },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(b, func(v any) any { return AsRef(v.(*java.Binary).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return b
}

func (s *JavaSender) VisitBlock(b *java.Block, p any) java.J {
	q := p.(*SendQueue)
	// static (right-padded bool) - Java's JRightPadded<Boolean> with element=false
	// Send manually since Go doesn't have RightPadded[bool]
	sendRightPaddedBool(false, java.EmptySpace, java.Markers{}, q)
	// statements
	q.GetAndSendList(b,
		func(v any) []any {
			stmts := v.(*java.Block).Statements
			result := make([]any, len(stmts))
			for i, stmt := range stmts {
				result[i] = stmt
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	// end space
	q.GetAndSend(b, func(v any) any { return v.(*java.Block).End },
		func(v any) { sendSpace(v.(java.Space), q) })
	return b
}

func (s *JavaSender) VisitReturn(r *java.Return, p any) java.J {
	q := p.(*SendQueue)
	// Java's J.Return has a single expression; Go has multiple
	// The first expression maps to J.Return.expression
	q.GetAndSend(r, func(v any) any {
		exprs := v.(*java.Return).Expressions
		if len(exprs) > 0 {
			return exprs[0].Element
		}
		return nil
	}, func(v any) { s.Visit(v.(java.Tree), q) })
	return r
}

func (s *JavaSender) VisitIf(i *java.If, p any) java.J {
	q := p.(*SendQueue)
	// ifCondition - reuse cached ControlParentheses if available, otherwise create new
	q.GetAndSend(i, func(v any) any {
		ifNode := v.(*java.If)
		if ifNode.ConditionCP != nil {
			cp := *ifNode.ConditionCP
			cp.Tree = java.RightPadded[java.Expression]{Element: ifNode.Condition, After: cp.Tree.After}
			return &cp
		}
		return &java.ControlParentheses{
			ID:      uuid.New(),
			Markers: java.Markers{ID: uuid.New()},
			Tree:    java.RightPadded[java.Expression]{Element: ifNode.Condition, After: java.EmptySpace},
		}
	}, func(v any) { s.Visit(v.(java.Tree), q) })
	// thenPart (right-padded)
	q.GetAndSend(i, func(v any) any {
		return java.RightPadded[java.Statement]{
			Element: v.(*java.If).Then,
			After:   java.EmptySpace,
		}
	}, func(v any) { sendRightPadded(s, v, q) })
	// elsePart - wrap in Else node for Java's J.If.Else model
	q.GetAndSend(i, func(v any) any {
		ep := v.(*java.If).ElsePart
		if ep == nil {
			return nil
		}
		return &java.Else{
			ID:      uuid.New(),
			Prefix:  ep.After,
			Markers: java.Markers{ID: uuid.New()},
			Body:    java.RightPadded[java.Statement]{Element: ep.Element.(java.Statement), After: java.EmptySpace},
		}
	}, func(v any) { s.Visit(v.(java.Tree), q) })
	return i
}

func (s *JavaSender) VisitElse(el *java.Else, p any) java.J {
	q := p.(*SendQueue)
	// body (right-padded Statement)
	q.GetAndSend(el, func(v any) any { return v.(*java.Else).Body },
		func(v any) { sendRightPadded(s, v, q) })
	return el
}

func (s *JavaSender) VisitAssignment(a *java.Assignment, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return v.(*java.Assignment).Variable },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(a, func(v any) any { return v.(*java.Assignment).Value },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*java.Assignment).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return a
}

func (s *JavaSender) VisitAssignmentOperation(a *java.AssignmentOperation, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return v.(*java.AssignmentOperation).Variable },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(a, func(v any) any {
		op := v.(*java.AssignmentOperation).Operator
		return java.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(a, func(v any) any { return v.(*java.AssignmentOperation).Assignment },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*java.AssignmentOperation).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return a
}

func (s *JavaSender) VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J {
	q := p.(*SendQueue)
	// Go's MethodDeclaration maps to parts of Java's MethodDeclaration
	// Java sends: leadingAnnotations, modifiers, typeParameters, returnTypeExpression,
	//   name annotations, name, parameters, dimensionsAfterName, throws, body, defaultValue, methodType
	// Go: receiver, name, parameters, returnType, body, methodType

	// leadingAnnotations (`//go:` directives modeled as J.Annotation)
	q.GetAndSendList(md,
		func(v any) []any {
			anns := v.(*java.MethodDeclaration).LeadingAnnotations
			result := make([]any, len(anns))
			for i, a := range anns {
				result[i] = a
			}
			return result
		},
		func(v any) any { return extractID(v) },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// modifiers (empty for Go)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// typeParameters (`[T any]` declaration-site generics; nil for non-generic funcs)
	q.GetAndSend(md, func(v any) any { return v.(*java.MethodDeclaration).TypeParameters },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// returnTypeExpression
	q.GetAndSend(md, func(v any) any { return v.(*java.MethodDeclaration).ReturnType },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// name annotations (empty)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(md, func(v any) any { return v.(*java.MethodDeclaration).Name },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// parameters (container)
	q.GetAndSend(md, func(v any) any { return v.(*java.MethodDeclaration).Parameters },
		func(v any) { sendContainer(s, v, q) })
	// dimensionsAfterName (empty for Go — no C-style array method returns)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// throws (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// body
	q.GetAndSend(md, func(v any) any { return v.(*java.MethodDeclaration).Body },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// defaultValue (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// methodType (as ref)
	q.GetAndSend(md, func(v any) any { return AsRef(v.(*java.MethodDeclaration).MethodType) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return md
}

func (s *JavaSender) VisitTypeParameters(tps *java.TypeParameters, p any) java.J {
	q := p.(*SendQueue)
	// annotations (empty for Go)
	q.GetAndSendList(tps, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// typeParameters (list of right-padded J$TypeParameter)
	q.GetAndSendList(tps,
		func(v any) []any {
			elems := v.(*java.TypeParameters).TypeParameters
			result := make([]any, len(elems))
			for i, e := range elems {
				result[i] = e
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	return tps
}

func (s *JavaSender) VisitTypeParameter(tp *java.TypeParameter, p any) java.J {
	q := p.(*SendQueue)
	// annotations (empty for Go)
	q.GetAndSendList(tp, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// modifiers (empty for Go)
	q.GetAndSendList(tp, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(tp, func(v any) any { return v.(*java.TypeParameter).Name },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// bounds (container; nil when the parameter shares a sibling's constraint).
	// Send the value Container, not the *Container, since the padding
	// accessors only recognize value Container types.
	q.GetAndSend(tp, func(v any) any {
		b := v.(*java.TypeParameter).Bounds
		if b == nil {
			return nil
		}
		return *b
	}, func(v any) { sendContainer(s, v, q) })
	return tp
}

func (s *JavaSender) VisitForLoop(f *java.ForLoop, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(f, func(v any) any {
		ctrl := v.(*java.ForLoop).Control
		return &ctrl
	}, func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(f, func(v any) any {
		return java.RightPadded[java.Statement]{Element: v.(*java.ForLoop).Body, After: java.EmptySpace}
	}, func(v any) { sendRightPadded(s, v, q) })
	return f
}

func (s *JavaSender) VisitForControl(fc *java.ForControl, p any) java.J {
	q := p.(*SendQueue)
	// init (list of right-padded)
	q.GetAndSendList(fc,
		func(v any) []any {
			init := v.(*java.ForControl).Init
			if init == nil {
				return nil
			}
			return []any{*init}
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	// condition (right-padded) — dereference pointer
	q.GetAndSend(fc, func(v any) any {
		cond := v.(*java.ForControl).Condition
		if cond == nil {
			return nil
		}
		return *cond
	}, func(v any) { sendRightPadded(s, v, q) })
	// update (list of right-padded)
	q.GetAndSendList(fc,
		func(v any) []any {
			update := v.(*java.ForControl).Update
			if update == nil {
				return nil
			}
			return []any{*update}
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	return fc
}

func (s *JavaSender) VisitForEachLoop(f *java.ForEachLoop, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(f, func(v any) any {
		ctrl := v.(*java.ForEachLoop).Control
		return &ctrl
	}, func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(f, func(v any) any {
		return java.RightPadded[java.Statement]{Element: v.(*java.ForEachLoop).Body, After: java.EmptySpace}
	}, func(v any) { sendRightPadded(s, v, q) })
	return f
}

func (s *JavaSender) VisitForEachControl(fc *java.ForEachControl, p any) java.J {
	q := p.(*SendQueue)
	// Go sends: key (right-padded), value (right-padded), operator (left-padded string), iterable
	// Java GolangReceiver override reads this format
	q.GetAndSend(fc, func(v any) any {
		k := v.(*java.ForEachControl).Key
		if k == nil {
			return nil
		}
		return *k
	}, func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any {
		val := v.(*java.ForEachControl).Value
		if val == nil {
			return nil
		}
		return *val
	}, func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any {
		op := v.(*java.ForEachControl).Operator
		return java.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any { return v.(*java.ForEachControl).Iterable },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return fc
}

func (s *JavaSender) VisitSwitch(sw *java.Switch, p any) java.J {
	q := p.(*SendQueue)
	// selector - wrap tag in ControlParentheses for Java's J.Switch model
	q.GetAndSend(sw, func(v any) any {
		tag := v.(*java.Switch).Tag
		var inner java.Expression
		if tag != nil {
			inner = tag.Element
		} else {
			// Tagless switch: use Empty as the expression
			inner = &java.Empty{ID: uuid.New()}
		}
		return &java.ControlParentheses{
			ID:      uuid.New(),
			Markers: java.Markers{ID: uuid.New()},
			Tree:    java.RightPadded[java.Expression]{Element: inner, After: java.EmptySpace},
		}
	}, func(v any) { s.Visit(v.(java.Tree), q) })
	// cases (Block)
	q.GetAndSend(sw, func(v any) any { return v.(*java.Switch).Body },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return sw
}

func (s *JavaSender) VisitCase(c *java.Case, p any) java.J {
	q := p.(*SendQueue)
	// type (enum value)
	q.GetAndSend(c, func(_ any) any { return "Statement" }, nil)
	// caseLabels (container)
	q.GetAndSend(c, func(v any) any { return v.(*java.Case).Expressions },
		func(v any) { sendContainer(s, v, q) })
	// statements (container)
	q.GetAndSend(c, func(v any) any {
		body := v.(*java.Case).Body
		result := make([]java.RightPadded[java.Statement], len(body))
		copy(result, body)
		return java.Container[java.Statement]{Elements: result}
	}, func(v any) { sendContainer(s, v, q) })
	// body (right-padded, nil for Go-style case)
	q.GetAndSend(c, func(_ any) any { return nil }, nil)
	// guard (nil for Go)
	q.GetAndSend(c, func(_ any) any { return nil }, nil)
	return c
}

func (s *JavaSender) VisitBreak(b *java.Break, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(b, func(v any) any { return v.(*java.Break).Label },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return b
}

func (s *JavaSender) VisitContinue(c *java.Continue, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(c, func(v any) any { return v.(*java.Continue).Label },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return c
}

func (s *JavaSender) VisitLabel(l *java.Label, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(l, func(v any) any { return v.(*java.Label).Name },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(l, func(v any) any { return v.(*java.Label).Statement },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return l
}

// sendAnnotation matches JavaSender.visitAnnotation field order:
// annotationType, then nullable arguments container.
func (s *JavaSender) VisitAnnotation(ann *java.Annotation, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(ann, func(v any) any { return v.(*java.Annotation).AnnotationType },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(ann, func(v any) any {
		args := v.(*java.Annotation).Arguments
		if args == nil {
			return nil
		}
		return *args
	}, func(v any) { sendContainer(s, v, q) })
	return ann
}

func (s *JavaSender) VisitUnary(u *java.Unary, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(u, func(v any) any {
		op := v.(*java.Unary).Operator
		return java.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(u, func(v any) any { return v.(*java.Unary).Operand },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(u, func(v any) any { return AsRef(v.(*java.Unary).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return u
}

func (s *JavaSender) VisitFieldAccess(fa *java.FieldAccess, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(fa, func(v any) any { return v.(*java.FieldAccess).Target },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(fa, func(v any) any { return v.(*java.FieldAccess).Name },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(fa, func(v any) any { return AsRef(v.(*java.FieldAccess).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return fa
}

func (s *JavaSender) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	q := p.(*SendQueue)
	// select (right-padded, nullable) — dereference pointer
	q.GetAndSend(mi, func(v any) any {
		sel := v.(*java.MethodInvocation).Select
		if sel == nil {
			return nil
		}
		return *sel
	}, func(v any) { sendRightPadded(s, v, q) })
	// typeParameters (nil for Go)
	q.GetAndSend(mi, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(mi, func(v any) any { return v.(*java.MethodInvocation).Name },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// arguments (container)
	q.GetAndSend(mi, func(v any) any { return v.(*java.MethodInvocation).Arguments },
		func(v any) { sendContainer(s, v, q) })
	// methodType (as ref)
	q.GetAndSend(mi, func(v any) any { return AsRef(v.(*java.MethodInvocation).MethodType) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return mi
}

func (s *JavaSender) VisitVariableDeclarations(vd *java.VariableDeclarations, p any) java.J {
	q := p.(*SendQueue)
	// leadingAnnotations (struct field tags + `//go:` directives,
	// modeled as J.Annotation per the Java contract)
	q.GetAndSendList(vd,
		func(v any) []any {
			anns := v.(*java.VariableDeclarations).LeadingAnnotations
			result := make([]any, len(anns))
			for i, a := range anns {
				result[i] = a
			}
			return result
		},
		func(v any) any { return extractID(v) },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// modifiers (empty -- Go has no modifiers)
	q.GetAndSendList(vd, func(_ any) []any { return []any{} }, func(_ any) any { return nil }, nil)
	// typeExpression
	q.GetAndSend(vd, func(v any) any { return v.(*java.VariableDeclarations).TypeExpr },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// varargs
	q.GetAndSend(vd, func(v any) any {
		va := v.(*java.VariableDeclarations).Varargs
		if va != nil {
			return *va
		}
		return nil
	}, func(v any) { sendSpace(v.(java.Space), q) })
	// variables (list of right-padded NamedVariable)
	q.GetAndSendList(vd,
		func(v any) []any {
			vars := v.(*java.VariableDeclarations).Variables
			result := make([]any, len(vars))
			for i, d := range vars {
				result[i] = d
			}
			return result
		},
		func(v any) any { return containerElementID(v) },
		func(v any) { sendRightPadded(s, v, q) })
	return vd
}

func (s *JavaSender) VisitVariableDeclarator(vd *java.VariableDeclarator, p any) java.J {
	q := p.(*SendQueue)
	// Java's NamedVariable: declarator (Identifier), dimensionsAfterName, initializer, variableType
	// Go: Name, Initializer
	q.GetAndSend(vd, func(v any) any { return v.(*java.VariableDeclarator).Name },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// dimensionsAfterName (empty for Go)
	q.GetAndSendList(vd, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// initializer (left-padded, nullable) — dereference pointer
	q.GetAndSend(vd, func(v any) any {
		init := v.(*java.VariableDeclarator).Initializer
		if init == nil {
			return nil
		}
		return *init
	}, func(v any) { sendLeftPadded(s, v, q) })
	// variableType (as ref) - not yet on Go VariableDeclarator
	q.GetAndSend(vd, func(_ any) any { return nil }, nil)
	return vd
}

func (s *JavaSender) VisitArrayType(at *java.ArrayType, p any) java.J {
	q := p.(*SendQueue)
	// elementType
	q.GetAndSend(at, func(v any) any { return v.(*java.ArrayType).ElementType },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// annotations (empty for Go)
	q.GetAndSendList(at, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// dimension (left-padded)
	q.GetAndSend(at, func(v any) any { return v.(*java.ArrayType).Dimension },
		func(v any) { sendLeftPadded(s, v, q) })
	// type
	q.GetAndSend(at, func(v any) any { return v.(*java.ArrayType).Type }, nil)
	return at
}

func (s *JavaSender) VisitArrayAccess(aa *java.ArrayAccess, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(aa, func(v any) any { return v.(*java.ArrayAccess).Indexed },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(aa, func(v any) any { return v.(*java.ArrayAccess).Dimension },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return aa
}

func (s *JavaSender) VisitParameterizedType(pt *java.ParameterizedType, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(pt, func(v any) any { return v.(*java.ParameterizedType).Clazz },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// TypeParameters is a *Container; dereference so sendContainer (containerElements
	// et al.) sees a value Container rather than the pointer (which sends as empty).
	q.GetAndSend(pt, func(v any) any {
		tp := v.(*java.ParameterizedType).TypeParameters
		if tp == nil {
			return nil
		}
		return *tp
	}, func(v any) { sendContainer(s, v, q) })
	q.GetAndSend(pt, func(v any) any { return AsRef(v.(*java.ParameterizedType).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(java.JavaType), q) })
	return pt
}

func (s *JavaSender) VisitArrayDimension(ad *java.ArrayDimension, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(ad, func(v any) any { return ad.Index },
		func(v any) { sendRightPadded(s, v, q) })
	return ad
}

func (s *JavaSender) VisitParentheses(parens *java.Parentheses, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(parens, func(v any) any { return v.(*java.Parentheses).Tree },
		func(v any) { sendRightPadded(s, v, q) })
	return parens
}

func (s *JavaSender) VisitTypeCast(tc *java.TypeCast, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(tc, func(v any) any { return v.(*java.TypeCast).Clazz },
		func(v any) { s.Visit(v.(java.Tree), q) })
	q.GetAndSend(tc, func(v any) any { return v.(*java.TypeCast).Expr },
		func(v any) { s.Visit(v.(java.Tree), q) })
	return tc
}

func (s *JavaSender) VisitControlParentheses(cp *java.ControlParentheses, p any) java.J {
	q := p.(*SendQueue)
	q.GetAndSend(cp, func(v any) any { return v.(*java.ControlParentheses).Tree },
		func(v any) { sendRightPadded(s, v, q) })
	return cp
}

func (s *JavaSender) VisitImport(imp *java.Import, p any) java.J {
	q := p.(*SendQueue)
	// Java Import: static (left-padded), qualid, alias (left-padded)
	// Static is always false for Go
	q.GetAndSend(imp, func(_ any) any {
		return java.LeftPadded[bool]{Before: java.EmptySpace, Element: false}
	}, func(v any) { sendLeftPadded(s, v, q) })
	// qualid
	q.GetAndSend(imp, func(v any) any { return v.(*java.Import).Qualid },
		func(v any) { s.Visit(v.(java.Tree), q) })
	// alias — dereference pointer
	q.GetAndSend(imp, func(v any) any {
		a := v.(*java.Import).Alias
		if a == nil {
			return nil
		}
		return *a
	}, func(v any) { sendLeftPadded(s, v, q) })
	return imp
}

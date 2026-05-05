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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Sender serializes a tree node into a SendQueue. Implementations are
// visitor.VisitorI-conformant so they slot into the standard visitor
// dispatch — mirrors rewrite-java's JavaSender, which extends
// JavaVisitor<RpcSendQueue>.
type Sender interface {
	Visit(t tree.Tree, p any) tree.Tree
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
func (s *JavaSender) PreVisit(t tree.Tree, p any) tree.Tree {
	j, ok := t.(tree.J)
	if !ok {
		return t
	}
	q := p.(*SendQueue)
	q.GetAndSend(t, func(v any) any { return v.(tree.J).GetID().String() }, nil)
	q.GetAndSend(t, func(v any) any { return v.(tree.J).GetPrefix() },
		func(v any) { sendSpace(v.(tree.Space), q) })
	q.GetAndSend(t, func(v any) any { return v.(tree.J).GetMarkers() },
		func(v any) { SendMarkersCodec(v.(tree.Markers), q) })
	_ = j
	return t
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

func (s *JavaSender) VisitIdentifier(id *tree.Identifier, p any) tree.J {
	q := p.(*SendQueue)
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
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// simpleName
	q.GetAndSend(id, func(v any) any { return v.(*tree.Identifier).Name }, nil)
	// type (as ref)
	q.GetAndSend(id, func(v any) any { return AsRef(v.(*tree.Identifier).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	// fieldType (as ref)
	q.GetAndSend(id, func(v any) any { return AsRef(v.(*tree.Identifier).FieldType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return id
}

func (s *JavaSender) VisitLiteral(lit *tree.Literal, p any) tree.J {
	q := p.(*SendQueue)
	// value
	q.GetAndSend(lit, func(v any) any { return v.(*tree.Literal).Value }, nil)
	// valueSource (source text)
	q.GetAndSend(lit, func(v any) any { return v.(*tree.Literal).Source }, nil)
	// unicodeEscapes (empty for Go)
	q.GetAndSendList(lit, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// type (as ref)
	q.GetAndSend(lit, func(v any) any { return AsRef(v.(*tree.Literal).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return lit
}

func (s *JavaSender) VisitBinary(b *tree.Binary, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(b, func(v any) any { return v.(*tree.Binary).Left },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(b, func(v any) any {
		op := v.(*tree.Binary).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(b, func(v any) any { return v.(*tree.Binary).Right },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(b, func(v any) any { return AsRef(v.(*tree.Binary).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return b
}

func (s *JavaSender) VisitBlock(b *tree.Block, p any) tree.J {
	q := p.(*SendQueue)
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
	return b
}

func (s *JavaSender) VisitReturn(r *tree.Return, p any) tree.J {
	q := p.(*SendQueue)
	// Java's J.Return has a single expression; Go has multiple
	// The first expression maps to J.Return.expression
	q.GetAndSend(r, func(v any) any {
		exprs := v.(*tree.Return).Expressions
		if len(exprs) > 0 {
			return exprs[0].Element
		}
		return nil
	}, func(v any) { s.Visit(v.(tree.Tree), q) })
	return r
}

func (s *JavaSender) VisitIf(i *tree.If, p any) tree.J {
	q := p.(*SendQueue)
	// ifCondition - reuse cached ControlParentheses if available, otherwise create new
	q.GetAndSend(i, func(v any) any {
		ifNode := v.(*tree.If)
		if ifNode.ConditionCP != nil {
			cp := *ifNode.ConditionCP
			cp.Tree = tree.RightPadded[tree.Expression]{Element: ifNode.Condition, After: cp.Tree.After}
			return &cp
		}
		return &tree.ControlParentheses{
			ID:      uuid.New(),
			Markers: tree.Markers{ID: uuid.New()},
			Tree:    tree.RightPadded[tree.Expression]{Element: ifNode.Condition, After: tree.EmptySpace},
		}
	}, func(v any) { s.Visit(v.(tree.Tree), q) })
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
	}, func(v any) { s.Visit(v.(tree.Tree), q) })
	return i
}

func (s *JavaSender) VisitElse(el *tree.Else, p any) tree.J {
	q := p.(*SendQueue)
	// body (right-padded Statement)
	q.GetAndSend(el, func(v any) any { return v.(*tree.Else).Body },
		func(v any) { sendRightPadded(s, v, q) })
	return el
}

func (s *JavaSender) VisitAssignment(a *tree.Assignment, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return v.(*tree.Assignment).Variable },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(a, func(v any) any { return v.(*tree.Assignment).Value },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*tree.Assignment).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return a
}

func (s *JavaSender) VisitAssignmentOperation(a *tree.AssignmentOperation, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return v.(*tree.AssignmentOperation).Variable },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(a, func(v any) any {
		op := v.(*tree.AssignmentOperation).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(a, func(v any) any { return v.(*tree.AssignmentOperation).Assignment },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*tree.AssignmentOperation).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return a
}

func (s *JavaSender) VisitMethodDeclaration(md *tree.MethodDeclaration, p any) tree.J {
	q := p.(*SendQueue)
	// Go's MethodDeclaration maps to parts of Java's MethodDeclaration
	// Java sends: leadingAnnotations, modifiers, typeParameters, returnTypeExpression,
	//   name annotations, name, parameters, throws, body, defaultValue, methodType
	// Go: receiver, name, parameters, returnType, body, methodType

	// leadingAnnotations (`//go:` directives modeled as J.Annotation)
	q.GetAndSendList(md,
		func(v any) []any {
			anns := v.(*tree.MethodDeclaration).LeadingAnnotations
			result := make([]any, len(anns))
			for i, a := range anns {
				result[i] = a
			}
			return result
		},
		func(v any) any { return extractID(v) },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// modifiers (empty for Go)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// typeParameters (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// returnTypeExpression
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).ReturnType },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// name annotations (empty)
	q.GetAndSendList(md, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Name },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// parameters (container)
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Parameters },
		func(v any) { sendContainer(s, v, q) })
	// throws (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// body
	q.GetAndSend(md, func(v any) any { return v.(*tree.MethodDeclaration).Body },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// defaultValue (nil for Go)
	q.GetAndSend(md, func(_ any) any { return nil }, nil)
	// methodType (as ref)
	q.GetAndSend(md, func(v any) any { return AsRef(v.(*tree.MethodDeclaration).MethodType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return md
}

func (s *JavaSender) VisitForLoop(f *tree.ForLoop, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(f, func(v any) any {
		ctrl := v.(*tree.ForLoop).Control
		return &ctrl
	}, func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(f, func(v any) any {
		return tree.RightPadded[tree.Statement]{Element: v.(*tree.ForLoop).Body, After: tree.EmptySpace}
	}, func(v any) { sendRightPadded(s, v, q) })
	return f
}

func (s *JavaSender) VisitForControl(fc *tree.ForControl, p any) tree.J {
	q := p.(*SendQueue)
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
	// condition (right-padded) — dereference pointer
	q.GetAndSend(fc, func(v any) any {
		cond := v.(*tree.ForControl).Condition
		if cond == nil { return nil }
		return *cond
	}, func(v any) { sendRightPadded(s, v, q) })
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
	return fc
}

func (s *JavaSender) VisitForEachLoop(f *tree.ForEachLoop, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(f, func(v any) any {
		ctrl := v.(*tree.ForEachLoop).Control
		return &ctrl
	}, func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(f, func(v any) any {
		return tree.RightPadded[tree.Statement]{Element: v.(*tree.ForEachLoop).Body, After: tree.EmptySpace}
	}, func(v any) { sendRightPadded(s, v, q) })
	return f
}

func (s *JavaSender) VisitForEachControl(fc *tree.ForEachControl, p any) tree.J {
	q := p.(*SendQueue)
	// Go sends: key (right-padded), value (right-padded), operator (left-padded string), iterable
	// Java GolangReceiver override reads this format
	q.GetAndSend(fc, func(v any) any {
		k := v.(*tree.ForEachControl).Key
		if k == nil { return nil }
		return *k
	}, func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any {
		val := v.(*tree.ForEachControl).Value
		if val == nil { return nil }
		return *val
	}, func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any {
		op := v.(*tree.ForEachControl).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(fc, func(v any) any { return v.(*tree.ForEachControl).Iterable },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return fc
}

func (s *JavaSender) VisitSwitch(sw *tree.Switch, p any) tree.J {
	q := p.(*SendQueue)
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
	}, func(v any) { s.Visit(v.(tree.Tree), q) })
	// cases (Block)
	q.GetAndSend(sw, func(v any) any { return v.(*tree.Switch).Body },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return sw
}

func (s *JavaSender) VisitCase(c *tree.Case, p any) tree.J {
	q := p.(*SendQueue)
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
	return c
}

func (s *JavaSender) VisitBreak(b *tree.Break, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(b, func(v any) any { return v.(*tree.Break).Label },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return b
}

func (s *JavaSender) VisitContinue(c *tree.Continue, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(c, func(v any) any { return v.(*tree.Continue).Label },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return c
}

func (s *JavaSender) VisitLabel(l *tree.Label, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(l, func(v any) any { return v.(*tree.Label).Name },
		func(v any) { sendRightPadded(s, v, q) })
	q.GetAndSend(l, func(v any) any { return v.(*tree.Label).Statement },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return l
}

// sendAnnotation matches JavaSender.visitAnnotation field order:
// annotationType, then nullable arguments container.
func (s *JavaSender) VisitAnnotation(ann *tree.Annotation, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(ann, func(v any) any { return v.(*tree.Annotation).AnnotationType },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(ann, func(v any) any {
		args := v.(*tree.Annotation).Arguments
		if args == nil {
			return nil
		}
		return *args
	}, func(v any) { sendContainer(s, v, q) })
	return ann
}

func (s *JavaSender) VisitUnary(u *tree.Unary, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(u, func(v any) any {
		op := v.(*tree.Unary).Operator
		return tree.LeftPadded[string]{Before: op.Before, Element: op.Element.String(), Markers: op.Markers}
	}, func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(u, func(v any) any { return v.(*tree.Unary).Operand },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(u, func(v any) any { return AsRef(v.(*tree.Unary).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return u
}

func (s *JavaSender) VisitFieldAccess(fa *tree.FieldAccess, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(fa, func(v any) any { return v.(*tree.FieldAccess).Target },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(fa, func(v any) any { return v.(*tree.FieldAccess).Name },
		func(v any) { sendLeftPadded(s, v, q) })
	q.GetAndSend(fa, func(v any) any { return AsRef(v.(*tree.FieldAccess).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return fa
}

func (s *JavaSender) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
	q := p.(*SendQueue)
	// select (right-padded, nullable) — dereference pointer
	q.GetAndSend(mi, func(v any) any {
		sel := v.(*tree.MethodInvocation).Select
		if sel == nil { return nil }
		return *sel
	}, func(v any) { sendRightPadded(s, v, q) })
	// typeParameters (nil for Go)
	q.GetAndSend(mi, func(_ any) any { return nil }, nil)
	// name
	q.GetAndSend(mi, func(v any) any { return v.(*tree.MethodInvocation).Name },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// arguments (container)
	q.GetAndSend(mi, func(v any) any { return v.(*tree.MethodInvocation).Arguments },
		func(v any) { sendContainer(s, v, q) })
	// methodType (as ref)
	q.GetAndSend(mi, func(v any) any { return AsRef(v.(*tree.MethodInvocation).MethodType) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return mi
}

func (s *JavaSender) VisitVariableDeclarations(vd *tree.VariableDeclarations, p any) tree.J {
	q := p.(*SendQueue)
	// leadingAnnotations (struct field tags + `//go:` directives,
	// modeled as J.Annotation per the Java contract)
	q.GetAndSendList(vd,
		func(v any) []any {
			anns := v.(*tree.VariableDeclarations).LeadingAnnotations
			result := make([]any, len(anns))
			for i, a := range anns {
				result[i] = a
			}
			return result
		},
		func(v any) any { return extractID(v) },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// modifiers (empty -- Go has no modifiers)
	q.GetAndSendList(vd, func(_ any) []any { return []any{} }, func(_ any) any { return nil }, nil)
	// typeExpression
	q.GetAndSend(vd, func(v any) any { return v.(*tree.VariableDeclarations).TypeExpr },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// varargs
	q.GetAndSend(vd, func(v any) any {
		va := v.(*tree.VariableDeclarations).Varargs
		if va != nil {
			return *va
		}
		return nil
	}, func(v any) { sendSpace(v.(tree.Space), q) })
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
	return vd
}

func (s *JavaSender) VisitVariableDeclarator(vd *tree.VariableDeclarator, p any) tree.J {
	q := p.(*SendQueue)
	// Java's NamedVariable: declarator (Identifier), dimensionsAfterName, initializer, variableType
	// Go: Name, Initializer
	q.GetAndSend(vd, func(v any) any { return v.(*tree.VariableDeclarator).Name },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// dimensionsAfterName (empty for Go)
	q.GetAndSendList(vd, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// initializer (left-padded, nullable) — dereference pointer
	q.GetAndSend(vd, func(v any) any {
		init := v.(*tree.VariableDeclarator).Initializer
		if init == nil { return nil }
		return *init
	}, func(v any) { sendLeftPadded(s, v, q) })
	// variableType (as ref) - not yet on Go VariableDeclarator
	q.GetAndSend(vd, func(_ any) any { return nil }, nil)
	return vd
}

func (s *JavaSender) VisitArrayType(at *tree.ArrayType, p any) tree.J {
	q := p.(*SendQueue)
	// elementType
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).ElementType },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// annotations (empty for Go)
	q.GetAndSendList(at, func(_ any) []any { return nil }, func(_ any) any { return nil }, nil)
	// dimension (left-padded)
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).Dimension },
		func(v any) { sendLeftPadded(s, v, q) })
	// type
	q.GetAndSend(at, func(v any) any { return v.(*tree.ArrayType).Type }, nil)
	return at
}

func (s *JavaSender) VisitArrayAccess(aa *tree.ArrayAccess, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(aa, func(v any) any { return v.(*tree.ArrayAccess).Indexed },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(aa, func(v any) any { return v.(*tree.ArrayAccess).Dimension },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return aa
}

func (s *JavaSender) VisitParameterizedType(pt *tree.ParameterizedType, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(pt, func(v any) any { return v.(*tree.ParameterizedType).Clazz },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(pt, func(v any) any { return v.(*tree.ParameterizedType).TypeParameters },
		func(v any) { sendContainer(s, v, q) })
	q.GetAndSend(pt, func(v any) any { return AsRef(v.(*tree.ParameterizedType).Type) },
		func(v any) { s.visitType(GetValueNonNull(v).(tree.JavaType), q) })
	return pt
}

func (s *JavaSender) VisitArrayDimension(ad *tree.ArrayDimension, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(ad, func(v any) any { return ad.Index },
		func(v any) { sendRightPadded(s, v, q) })
	return ad
}

func (s *JavaSender) VisitParentheses(parens *tree.Parentheses, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(parens, func(v any) any { return v.(*tree.Parentheses).Tree },
		func(v any) { sendRightPadded(s, v, q) })
	return parens
}

func (s *JavaSender) VisitTypeCast(tc *tree.TypeCast, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(tc, func(v any) any { return v.(*tree.TypeCast).Clazz },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	q.GetAndSend(tc, func(v any) any { return v.(*tree.TypeCast).Expr },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	return tc
}

func (s *JavaSender) VisitControlParentheses(cp *tree.ControlParentheses, p any) tree.J {
	q := p.(*SendQueue)
	q.GetAndSend(cp, func(v any) any { return v.(*tree.ControlParentheses).Tree },
		func(v any) { sendRightPadded(s, v, q) })
	return cp
}

func (s *JavaSender) VisitImport(imp *tree.Import, p any) tree.J {
	q := p.(*SendQueue)
	// Java Import: static (left-padded), qualid, alias (left-padded)
	// Static is always false for Go
	q.GetAndSend(imp, func(_ any) any {
		return tree.LeftPadded[bool]{Before: tree.EmptySpace, Element: false}
	}, func(v any) { sendLeftPadded(s, v, q) })
	// qualid
	q.GetAndSend(imp, func(v any) any { return v.(*tree.Import).Qualid },
		func(v any) { s.Visit(v.(tree.Tree), q) })
	// alias — dereference pointer
	q.GetAndSend(imp, func(v any) any {
		a := v.(*tree.Import).Alias
		if a == nil { return nil }
		return *a
	}, func(v any) { sendLeftPadded(s, v, q) })
	return imp
}

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

// Receiver deserializes a tree node from a ReceiveQueue. Mirrors the
// visitor.VisitorI signature so receivers slot into the framework's
// dispatch — same shape as rewrite-java's JavaReceiver, which extends
// JavaVisitor<RpcReceiveQueue>.
type Receiver interface {
	Visit(t java.Tree, p any) java.Tree
}

// JavaReceiver deserializes J (shared Java-like) AST nodes via the
// visitor pattern. Mirrors org.openrewrite.java.internal.rpc.JavaReceiver.
//
// JavaReceiver embeds visitor.GoVisitor; the framework's type-switch
// dispatch routes calls to JavaReceiver's VisitX overrides via the
// Self field. PreVisit handles the cross-cutting fields (id, prefix,
// markers) once per node, mirroring Java's JavaVisitor.preVisit.
type JavaReceiver struct {
	visitor.GoVisitor
	typeReceiver *JavaTypeReceiver
}

// PreVisit deserializes the cross-cutting fields of every J node:
// id, prefix, markers. Returns a (possibly new) instance of t with
// those fields populated from the queue. ParseError isn't a J node
// and is special-cased at the GoReceiver layer.
//
// Each field is updated via the immutable typed wither (WithPrefix /
// WithMarkers, called polymorphically via reflection) so the input
// tree is never mutated and the visitor framework's pointer-identity
// change detection sees the right thing — same input pointer means
// nothing changed; different output pointer means something changed.
// Mirrors rewrite-java's `j.withPrefix(...).withMarkers(...)` chain.
func (r *JavaReceiver) PreVisit(t java.Tree, p any) java.Tree {
	j, IsJ := t.(java.J)
	if !IsJ {
		return t
	}
	q := p.(*ReceiveQueue)
	if result := q.Receive(j.GetID().String(), nil); result != nil {
		if idStr, ok := result.(string); ok && idStr != "" {
			if id, err := uuid.Parse(idStr); err == nil {
				t = j.WithID(id)
			}
		}
	}
	if result := q.Receive(j.GetPrefix(), func(v any) any {
		return receiveSpace(v.(java.Space), q)
	}); result != nil {
		t = withPrefixViaReflection(t, result.(java.Space))
	}
	if result := q.Receive(j.GetMarkers(), func(v any) any {
		return receiveMarkersCodec(q, v.(java.Markers))
	}); result != nil {
		t = withMarkersViaReflection(t, result.(java.Markers))
	}
	return t
}

// receiveType receives a JavaType from the queue with null/Unknown handling.
func (r *JavaReceiver) receiveType(before java.JavaType, q *ReceiveQueue) java.JavaType {
	result := q.Receive(before, func(v any) any {
		return r.typeReceiver.Visit(v.(java.JavaType), q)
	})
	if result == nil {
		return nil
	}
	return result.(java.JavaType)
}

// --- J nodes ---

func (r *JavaReceiver) VisitIdentifier(id *java.Identifier, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *id // shallow copy to avoid mutating remoteObjects baseline
	id = &c
	// annotations
	beforeAnns := make([]any, len(id.Annotations))
	for i, a := range id.Annotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if afterAnns != nil {
		id.Annotations = make([]java.Tree, len(afterAnns))
		for i, a := range afterAnns {
			id.Annotations[i] = a.(java.Tree)
		}
	}
	// simpleName
	id.Name = receiveScalar[string](q, id.Name)
	// type (as ref)
	id.Type = r.receiveType(id.Type, q)
	// fieldType (as ref)
	ft := r.receiveType(id.FieldType, q)
	if ft != nil {
		id.FieldType = ft.(*java.JavaTypeVariable)
	} else {
		id.FieldType = nil
	}
	return id
}

func (r *JavaReceiver) VisitLiteral(lit *java.Literal, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *lit // shallow copy to avoid mutating remoteObjects baseline
	lit = &c
	// value
	lit.Value = q.Receive(lit.Value, nil)
	// valueSource
	lit.Source = receiveScalar[string](q, lit.Source)
	// unicodeEscapes (empty for Go)
	q.ReceiveList(nil, nil)
	// type (as ref)
	lit.Type = r.receiveType(lit.Type, q)
	return lit
}

func (r *JavaReceiver) VisitBinary(b *java.Binary, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	b.Left = receiveValue(q, b.Left, func(e java.Expression) any { return r.Visit(e, q) })
	b.Operator = receiveLeftPaddedEnum(r, q, b.Operator, java.ParseBinaryOperator)
	b.Right = receiveValue(q, b.Right, func(e java.Expression) any { return r.Visit(e, q) })
	b.Type = r.receiveType(b.Type, q)
	return b
}

func (r *JavaReceiver) VisitBlock(b *java.Block, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	// static (right-padded) - Java-only field, not stored in Go Block
	q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) })
	// statements
	beforeStmts := make([]any, len(b.Statements))
	for i, s := range b.Statements {
		beforeStmts[i] = s
	}
	afterStmts := q.ReceiveList(beforeStmts, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterStmts != nil {
		b.Statements = make([]java.RightPadded[java.Statement], len(afterStmts))
		for i, s := range afterStmts {
			b.Statements[i] = coerceToStatementRP(s)
		}
	}
	// end space
	b.End = receiveValue(q, b.End, func(e java.Space) any { return receiveSpace(e, q) })
	return b
}

// receiveAnnotation matches JavaReceiver.visitAnnotation field order:
// annotationType, then nullable arguments container.
func (r *JavaReceiver) VisitAnnotation(ann *java.Annotation, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ann // shallow copy to avoid mutating remoteObjects baseline
	ann = &c
	ann.AnnotationType = receiveValue(q, ann.AnnotationType, func(e java.Expression) any { return r.Visit(e, q) })
	ann.Arguments = receivePointerContainer[java.Expression](r, q, ann.Arguments)
	return ann
}

func (r *JavaReceiver) VisitUnary(u *java.Unary, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *u // shallow copy to avoid mutating remoteObjects baseline
	u = &c
	u.Operator = receiveLeftPaddedEnum(r, q, u.Operator, java.ParseUnaryOperator)
	u.Operand = receiveValue(q, u.Operand, func(e java.Expression) any { return r.Visit(e, q) })
	u.Type = r.receiveType(u.Type, q)
	return u
}

func (r *JavaReceiver) VisitFieldAccess(fa *java.FieldAccess, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *fa // shallow copy to avoid mutating remoteObjects baseline
	fa = &c
	fa.Target = receiveValue(q, fa.Target, func(e java.Expression) any { return r.Visit(e, q) })
	if result := q.Receive(fa.Name, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		fa.Name = coerceLeftPaddedIdent(result)
	}
	fa.Type = r.receiveType(fa.Type, q)
	return fa
}

func (r *JavaReceiver) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *mi // shallow copy to avoid mutating remoteObjects baseline
	mi = &c
	// select
	var beforeSelect any
	if mi.Select != nil {
		beforeSelect = *mi.Select
	}
	if result := q.Receive(beforeSelect, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		rp := coerceToExpressionRP(result)
		mi.Select = &rp
	} else {
		mi.Select = nil
	}
	// typeParameters (explicit call-site type args; nullable container)
	mi.TypeParameters = receivePointerContainer[java.Expression](r, q, mi.TypeParameters)
	// name
	mi.Name = receiveValue(q, mi.Name, func(e *java.Identifier) any { return r.Visit(e, q) })
	// arguments
	mi.Arguments = receiveContainer[java.Expression](r, q, mi.Arguments)
	// methodType
	mt := r.receiveType(mi.MethodType, q)
	if mt != nil {
		mi.MethodType = mt.(*java.JavaTypeMethod)
	} else {
		mi.MethodType = nil
	}
	return mi
}

func (r *JavaReceiver) VisitAssignment(a *java.Assignment, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *a // shallow copy to avoid mutating remoteObjects baseline
	a = &c
	a.Variable = receiveValue(q, a.Variable, func(e java.Expression) any { return r.Visit(e, q) })
	if result := q.Receive(a.Value, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		a.Value = result.(java.LeftPadded[java.Expression])
	}
	a.Type = r.receiveType(a.Type, q)
	return a
}

func (r *JavaReceiver) VisitAssignmentOperation(a *java.AssignmentOperation, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *a // shallow copy to avoid mutating remoteObjects baseline
	a = &c
	a.Variable = receiveValue(q, a.Variable, func(e java.Expression) any { return r.Visit(e, q) })
	a.Operator = receiveLeftPaddedEnum(r, q, a.Operator, java.ParseAssignmentOperator)
	a.Assignment = receiveValue(q, a.Assignment, func(e java.Expression) any { return r.Visit(e, q) })
	a.Type = r.receiveType(a.Type, q)
	return a
}

func (r *JavaReceiver) VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *md // shallow copy to avoid mutating remoteObjects baseline
	md = &c
	// leadingAnnotations
	beforeAnns := make([]any, len(md.LeadingAnnotations))
	for i, a := range md.LeadingAnnotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if afterAnns != nil {
		md.LeadingAnnotations = make([]*java.Annotation, 0, len(afterAnns))
		for _, a := range afterAnns {
			if a != nil {
				md.LeadingAnnotations = append(md.LeadingAnnotations, a.(*java.Annotation))
			}
		}
	}
	// modifiers
	q.ReceiveList(nil, nil)
	// typeParameters
	md.TypeParameters = receiveValue(q, md.TypeParameters, func(e *java.TypeParameters) any { return r.Visit(e, q) })
	// returnTypeExpression
	md.ReturnType = receiveValue(q, md.ReturnType, func(e java.Expression) any { return r.Visit(e, q) })
	// name annotations
	q.ReceiveList(nil, nil)
	// name
	md.Name = receiveValue(q, md.Name, func(e *java.Identifier) any { return r.Visit(e, q) })
	// parameters
	md.Parameters = receiveContainer[java.Statement](r, q, md.Parameters)
	// dimensionsAfterName (empty for Go — no C-style array method returns)
	q.ReceiveList(nil, nil)
	// throws
	q.Receive(nil, nil)
	// body
	md.Body = receiveValue(q, md.Body, func(e *java.Block) any { return r.Visit(e, q) })
	// defaultValue
	q.Receive(nil, nil)
	// methodType
	mt := r.receiveType(md.MethodType, q)
	if mt != nil {
		md.MethodType = mt.(*java.JavaTypeMethod)
	} else {
		md.MethodType = nil
	}
	return md
}

func (r *JavaReceiver) VisitTypeParameters(tps *java.TypeParameters, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *tps // shallow copy to avoid mutating remoteObjects baseline
	tps = &c
	// annotations
	q.ReceiveList(nil, nil)
	// typeParameters (list of right-padded J$TypeParameter)
	beforeElems := make([]any, len(tps.TypeParameters))
	for i, e := range tps.TypeParameters {
		beforeElems[i] = e
	}
	afterElems := q.ReceiveList(beforeElems, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterElems != nil {
		tps.TypeParameters = make([]java.RightPadded[java.J], len(afterElems))
		for i, e := range afterElems {
			tps.TypeParameters[i] = e.(java.RightPadded[java.J])
		}
	}
	return tps
}

func (r *JavaReceiver) VisitTypeParameter(tp *java.TypeParameter, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *tp // shallow copy to avoid mutating remoteObjects baseline
	tp = &c
	// annotations
	q.ReceiveList(nil, nil)
	// modifiers
	q.ReceiveList(nil, nil)
	// name
	tp.Name = receiveValue(q, tp.Name, func(e java.Expression) any { return r.Visit(e, q) })
	// bounds (container; nil when the parameter shares a sibling's constraint)
	tp.Bounds = receivePointerContainer[java.Expression](r, q, tp.Bounds)
	return tp
}

func (r *JavaReceiver) VisitVariableDeclarations(vd *java.VariableDeclarations, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *vd // shallow copy to avoid mutating remoteObjects baseline
	vd = &c
	// leadingAnnotations
	beforeAnns := make([]any, len(vd.LeadingAnnotations))
	for i, a := range vd.LeadingAnnotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.Visit(v.(java.Tree), q) })
	if afterAnns != nil {
		vd.LeadingAnnotations = make([]*java.Annotation, 0, len(afterAnns))
		for _, a := range afterAnns {
			if a != nil {
				vd.LeadingAnnotations = append(vd.LeadingAnnotations, a.(*java.Annotation))
			}
		}
	}
	// modifiers
	q.ReceiveList(nil, nil)
	// typeExpression
	vd.TypeExpr = receiveValue(q, vd.TypeExpr, func(e java.Expression) any { return r.Visit(e, q) })
	// varargs
	var currentVarargs any
	if vd.Varargs != nil {
		currentVarargs = *vd.Varargs
	}
	if varargsResult := q.Receive(currentVarargs, func(v any) any { return receiveSpace(v.(java.Space), q) }); varargsResult != nil {
		sp := varargsResult.(java.Space)
		vd.Varargs = &sp
	}
	// variables
	beforeVars := make([]any, len(vd.Variables))
	for i, v := range vd.Variables {
		beforeVars[i] = v
	}
	afterVars := q.ReceiveList(beforeVars, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterVars != nil {
		vd.Variables = make([]java.RightPadded[*java.VariableDeclarator], len(afterVars))
		for i, v := range afterVars {
			vd.Variables[i] = v.(java.RightPadded[*java.VariableDeclarator])
		}
	}
	return vd
}

func (r *JavaReceiver) VisitVariableDeclarator(vd *java.VariableDeclarator, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *vd // shallow copy to avoid mutating remoteObjects baseline
	vd = &c
	// declarator (name)
	vd.Name = receiveValue(q, vd.Name, func(e *java.Identifier) any { return r.Visit(e, q) })
	// dimensionsAfterName
	q.ReceiveList(nil, nil)
	// initializer
	var beforeInit any
	if vd.Initializer != nil {
		beforeInit = *vd.Initializer
	}
	if result := q.Receive(beforeInit, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		lp := result.(java.LeftPadded[java.Expression])
		vd.Initializer = &lp
	} else {
		vd.Initializer = nil
	}
	// variableType
	q.Receive(nil, nil)
	return vd
}

func (r *JavaReceiver) VisitReturn(ret *java.Return, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ret
	ret = &c
	ret.Expression = receiveValue(q, ret.Expression, func(e java.Expression) any { return r.Visit(e, q) })
	return ret
}

func (r *JavaReceiver) VisitIf(i *java.If, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *i // shallow copy to avoid mutating remoteObjects baseline
	i = &c
	// ifCondition - Java sends a ControlParentheses; store it directly (matches J.If)
	if cpResult := q.Receive(i.Condition, func(v any) any { return r.Visit(v.(java.Tree), q) }); cpResult != nil {
		i.Condition = cpResult.(*java.ControlParentheses)
	}
	// thenPart - Java sends RightPadded<Statement> wrapping the Block
	if thenResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) }); thenResult != nil {
		rp := coerceToStatementRP(thenResult)
		if blk, ok := rp.Element.(*java.Block); ok {
			i.Then = blk
		}
	}
	// elsePart - Java sends Else node, convert to RightPadded
	if elseResult := q.Receive(nil, func(v any) any { return r.Visit(v.(java.Tree), q) }); elseResult != nil {
		el := elseResult.(*java.Else)
		i.ElsePart = &java.RightPadded[java.J]{
			Element: el.Body.Element,
			After:   el.Prefix,
		}
	} else {
		i.ElsePart = nil
	}
	return i
}

func (r *JavaReceiver) VisitElse(el *java.Else, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *el // shallow copy to avoid mutating remoteObjects baseline
	el = &c
	if result := q.Receive(el.Body, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		el.Body = coerceToStatementRP(result)
	}
	return el
}

func (r *JavaReceiver) VisitForLoop(f *java.ForLoop, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *f // shallow copy to avoid mutating remoteObjects baseline
	f = &c
	ctrl := &f.Control
	if result := q.Receive(ctrl, func(v any) any { return r.Visit(v.(java.Tree), q) }); result != nil {
		f.Control = *result.(*java.ForControl)
	}
	// body - Java sends RightPadded<Statement> wrapping the Block
	if bodyResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) }); bodyResult != nil {
		rp := coerceToStatementRP(bodyResult)
		if blk, ok := rp.Element.(*java.Block); ok {
			f.Body = blk
		}
	}
	return f
}

func (r *JavaReceiver) VisitForControl(fc *java.ForControl, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *fc // shallow copy to avoid mutating remoteObjects baseline
	fc = &c
	// init (list of right-padded)
	var beforeInit []any
	if fc.Init != nil {
		beforeInit = []any{*fc.Init}
	}
	initList := q.ReceiveList(beforeInit, func(v any) any { return receiveRightPadded(r, q, v) })
	if len(initList) > 0 {
		rp := coerceToStatementRP(initList[0])
		fc.Init = &rp
	} else {
		fc.Init = nil
	}
	// condition (right-padded)
	var beforeCond any
	if fc.Condition != nil {
		beforeCond = *fc.Condition
	}
	if result := q.Receive(beforeCond, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		rp := coerceToExpressionRP(result)
		fc.Condition = &rp
	} else {
		fc.Condition = nil
	}
	// update (list of right-padded)
	var beforeUpdate []any
	if fc.Update != nil {
		beforeUpdate = []any{*fc.Update}
	}
	updateList := q.ReceiveList(beforeUpdate, func(v any) any { return receiveRightPadded(r, q, v) })
	if len(updateList) > 0 {
		rp := coerceToStatementRP(updateList[0])
		fc.Update = &rp
	} else {
		fc.Update = nil
	}
	return fc
}

func (r *JavaReceiver) VisitForEachLoop(f *java.ForEachLoop, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *f // shallow copy to avoid mutating remoteObjects baseline
	f = &c
	ctrl := &f.Control
	if result := q.Receive(ctrl, func(v any) any { return r.Visit(v.(java.Tree), q) }); result != nil {
		f.Control = *result.(*java.ForEachControl)
	}
	// body - Java sends RightPadded<Statement> wrapping the Block
	if bodyResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) }); bodyResult != nil {
		rp := coerceToStatementRP(bodyResult)
		if blk, ok := rp.Element.(*java.Block); ok {
			f.Body = blk
		}
	}
	return f
}

func (r *JavaReceiver) VisitForEachControl(fc *java.ForEachControl, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *fc // shallow copy to avoid mutating remoteObjects baseline
	fc = &c
	// key (right-padded, nullable)
	var beforeKey any
	if fc.Key != nil {
		beforeKey = *fc.Key
	}
	if result := q.Receive(beforeKey, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		rp := coerceToExpressionRP(result)
		fc.Key = &rp
	} else {
		fc.Key = nil
	}
	// value (right-padded, nullable)
	var beforeValue any
	if fc.Value != nil {
		beforeValue = *fc.Value
	}
	if result := q.Receive(beforeValue, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		rp := coerceToExpressionRP(result)
		fc.Value = &rp
	} else {
		fc.Value = nil
	}
	// operator (left-padded AssignOp enum)
	fc.Operator = receiveLeftPaddedEnum(r, q, fc.Operator, parseAssignOpDefaulting)
	// iterable
	fc.Iterable = receiveValue(q, fc.Iterable, func(e java.Expression) any { return r.Visit(e, q) })
	return fc
}

func (r *JavaReceiver) VisitSwitch(sw *java.Switch, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *sw // shallow copy to avoid mutating remoteObjects baseline
	sw = &c
	// selector - Java sends ControlParentheses, extract inner Expression for Tag
	if cpResult := q.Receive(nil, func(v any) any { return r.Visit(v.(java.Tree), q) }); cpResult != nil {
		if cp, ok := cpResult.(*java.ControlParentheses); ok {
			if _, isEmpty := cp.Tree.Element.(*java.Empty); !isEmpty {
				sw.Tag = &java.RightPadded[java.Expression]{
					Element: cp.Tree.Element,
					After:   cp.Tree.After,
				}
			}
		}
	}
	sw.Body = receiveValue(q, sw.Body, func(e *java.Block) any { return r.Visit(e, q) })
	return sw
}

func (r *JavaReceiver) VisitCase(cs *java.Case, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *cs // shallow copy to avoid mutating remoteObjects baseline
	cs = &c
	q.Receive(nil, nil) // type enum
	cs.Expressions = receiveContainer[java.Expression](r, q, cs.Expressions)
	// statements - Java sends Container<RightPadded<Statement>>, extract to Go's []RightPadded[Statement]
	if result := q.Receive(nil, func(v any) any { return receiveContainerTyped[java.Statement](r, q, v) }); result != nil {
		cont := result.(java.Container[java.Statement])
		cs.Body = cont.Elements
	}
	q.Receive(nil, nil) // body
	q.Receive(nil, nil) // guard
	return cs
}

func (r *JavaReceiver) VisitBreak(b *java.Break, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	b.Label = receiveValue(q, b.Label, func(e *java.Identifier) any { return r.Visit(e, q) })
	return b
}

func (r *JavaReceiver) VisitContinue(cont *java.Continue, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *cont // shallow copy to avoid mutating remoteObjects baseline
	cont = &c
	cont.Label = receiveValue(q, cont.Label, func(e *java.Identifier) any { return r.Visit(e, q) })
	return cont
}

func (r *JavaReceiver) VisitLabel(l *java.Label, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *l // shallow copy to avoid mutating remoteObjects baseline
	l = &c
	if result := q.Receive(l.Name, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		l.Name = coerceRightPaddedTyped[*java.Identifier](result)
	}
	l.Statement = receiveValue(q, l.Statement, func(e java.Statement) any { return r.Visit(e, q) })
	return l
}

func (r *JavaReceiver) VisitArrayType(at *java.ArrayType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *at // shallow copy to avoid mutating remoteObjects baseline
	at = &c
	at.ElementType = receiveValue(q, at.ElementType, func(e java.Expression) any { return r.Visit(e, q) })
	q.ReceiveList(nil, nil) // annotations
	if result := q.Receive(at.Dimension, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		at.Dimension = result.(java.LeftPadded[java.Space])
	}
	at.Type = r.receiveType(at.Type, q)
	return at
}

func (r *JavaReceiver) VisitParameterizedType(pt *java.ParameterizedType, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *pt
	pt = &c
	pt.Clazz = receiveValue(q, pt.Clazz, func(e java.Expression) any { return r.Visit(e, q) })
	pt.TypeParameters = receivePointerContainer[java.Expression](r, q, pt.TypeParameters)
	pt.Type = r.receiveType(pt.Type, q)
	return pt
}

func (r *JavaReceiver) VisitArrayAccess(aa *java.ArrayAccess, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *aa // shallow copy to avoid mutating remoteObjects baseline
	aa = &c
	aa.Indexed = receiveValue(q, aa.Indexed, func(e java.Expression) any { return r.Visit(e, q) })
	aa.Dimension = receiveValue(q, aa.Dimension, func(e *java.ArrayDimension) any { return r.Visit(e, q) })
	return aa
}

func (r *JavaReceiver) VisitArrayDimension(ad *java.ArrayDimension, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *ad // shallow copy to avoid mutating remoteObjects baseline
	ad = &c
	if result := q.Receive(ad.Index, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		ad.Index = coerceToExpressionRP(result)
	}
	return ad
}

func (r *JavaReceiver) VisitParentheses(parens *java.Parentheses, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *parens // shallow copy to avoid mutating remoteObjects baseline
	parens = &c
	if result := q.Receive(parens.Tree, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		parens.Tree = coerceToExpressionRP(result)
	}
	return parens
}

func (r *JavaReceiver) VisitTypeCast(tc *java.TypeCast, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *tc // shallow copy to avoid mutating remoteObjects baseline
	tc = &c
	tc.Clazz = receiveValue(q, tc.Clazz, func(e *java.ControlParentheses) any { return r.Visit(e, q) })
	tc.Expr = receiveValue(q, tc.Expr, func(e java.Expression) any { return r.Visit(e, q) })
	return tc
}

func (r *JavaReceiver) VisitControlParentheses(cp *java.ControlParentheses, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *cp // shallow copy to avoid mutating remoteObjects baseline
	cp = &c
	if result := q.Receive(cp.Tree, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		cp.Tree = coerceToExpressionRP(result)
	}
	return cp
}

func (r *JavaReceiver) VisitImport(imp *java.Import, p any) java.J {
	q := p.(*ReceiveQueue)
	c := *imp // shallow copy to avoid mutating remoteObjects baseline
	imp = &c
	// static (always false for Go, but must receive full LeftPadded protocol)
	staticBefore := java.LeftPadded[bool]{Before: java.EmptySpace, Element: false}
	q.Receive(staticBefore, func(v any) any { return receiveLeftPadded(r, q, v) })
	// qualid (Expression - could be Literal or FieldAccess depending on direction)
	imp.Qualid = receiveValue(q, imp.Qualid, func(e java.Expression) any { return r.Visit(e, q) })
	// alias
	var beforeAlias any
	if imp.Alias != nil {
		beforeAlias = *imp.Alias
	}
	if result := q.Receive(beforeAlias, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		lp := coerceLeftPaddedIdent(result)
		imp.Alias = &lp
	} else {
		imp.Alias = nil
	}
	return imp
}

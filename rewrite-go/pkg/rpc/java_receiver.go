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

// Receiver deserializes a tree node from a ReceiveQueue. Mirrors the
// visitor.VisitorI signature so receivers slot into the framework's
// dispatch — same shape as rewrite-java's JavaReceiver, which extends
// JavaVisitor<RpcReceiveQueue>.
type Receiver interface {
	Visit(t tree.Tree, p any) tree.Tree
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
func (r *JavaReceiver) PreVisit(t tree.Tree, p any) tree.Tree {
	j, isJ := t.(tree.J)
	if !isJ {
		return t
	}
	q := p.(*ReceiveQueue)
	q.Receive(j.GetID().String(), nil)
	if result := q.Receive(j.GetPrefix(), func(v any) any {
		return receiveSpace(v.(tree.Space), q)
	}); result != nil {
		t = withPrefixViaReflection(t, result.(tree.Space))
	}
	if result := q.Receive(j.GetMarkers(), func(v any) any {
		return receiveMarkersCodec(q, v.(tree.Markers))
	}); result != nil {
		t = withMarkersViaReflection(t, result.(tree.Markers))
	}
	return t
}

// receiveType receives a JavaType from the queue with null/Unknown handling.
func (r *JavaReceiver) receiveType(before tree.JavaType, q *ReceiveQueue) tree.JavaType {
	result := q.Receive(before, func(v any) any {
		return r.typeReceiver.Visit(v.(tree.JavaType), q)
	})
	if result == nil {
		return nil
	}
	return result.(tree.JavaType)
}

// --- J nodes ---

func (r *JavaReceiver) VisitIdentifier(id *tree.Identifier, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *id // shallow copy to avoid mutating remoteObjects baseline
	id = &c
	// annotations
	beforeAnns := make([]any, len(id.Annotations))
	for i, a := range id.Annotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if afterAnns != nil {
		id.Annotations = make([]tree.Tree, len(afterAnns))
		for i, a := range afterAnns {
			id.Annotations[i] = a.(tree.Tree)
		}
	}
	// simpleName
	id.Name = receiveScalar[string](q, id.Name)
	// type (as ref)
	id.Type = r.receiveType(id.Type, q)
	// fieldType (as ref)
	ft := r.receiveType(id.FieldType, q)
	if ft != nil {
		id.FieldType = ft.(*tree.JavaTypeVariable)
	} else {
		id.FieldType = nil
	}
	return id
}

func (r *JavaReceiver) VisitLiteral(lit *tree.Literal, p any) tree.J {
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

func (r *JavaReceiver) VisitBinary(b *tree.Binary, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	result := q.Receive(b.Left, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		b.Left = result.(tree.Expression)
	}
	if result := q.Receive(b.Operator, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		b.Operator = result.(tree.LeftPadded[tree.BinaryOperator])
	}
	rightResult := q.Receive(b.Right, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if rightResult != nil {
		b.Right = rightResult.(tree.Expression)
	}
	b.Type = r.receiveType(b.Type, q)
	return b
}

func (r *JavaReceiver) VisitBlock(b *tree.Block, p any) tree.J {
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
		b.Statements = make([]tree.RightPadded[tree.Statement], len(afterStmts))
		for i, s := range afterStmts {
			b.Statements[i] = coerceToStatementRP(s)
		}
	}
	// end space
	if result := q.Receive(b.End, func(v any) any {
		return receiveSpace(v.(tree.Space), q)
	}); result != nil {
		b.End = result.(tree.Space)
	}
	return b
}

// receiveAnnotation matches JavaReceiver.visitAnnotation field order:
// annotationType, then nullable arguments container.
func (r *JavaReceiver) VisitAnnotation(ann *tree.Annotation, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *ann // shallow copy to avoid mutating remoteObjects baseline
	ann = &c
	if result := q.Receive(ann.AnnotationType, func(v any) any { return r.Visit(v.(tree.Tree), q) }); result != nil {
		ann.AnnotationType = result.(tree.Expression)
	}
	var beforeArgs any
	if ann.Arguments != nil {
		beforeArgs = *ann.Arguments
	}
	if result := q.Receive(beforeArgs, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		container := result.(tree.Container[tree.Expression])
		ann.Arguments = &container
	} else {
		ann.Arguments = nil
	}
	return ann
}

func (r *JavaReceiver) VisitUnary(u *tree.Unary, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *u // shallow copy to avoid mutating remoteObjects baseline
	u = &c
	if result := q.Receive(u.Operator, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		u.Operator = result.(tree.LeftPadded[tree.UnaryOperator])
	}
	result := q.Receive(u.Operand, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		u.Operand = result.(tree.Expression)
	}
	u.Type = r.receiveType(u.Type, q)
	return u
}

func (r *JavaReceiver) VisitFieldAccess(fa *tree.FieldAccess, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *fa // shallow copy to avoid mutating remoteObjects baseline
	fa = &c
	result := q.Receive(fa.Target, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		fa.Target = result.(tree.Expression)
	}
	if result := q.Receive(fa.Name, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		fa.Name = result.(tree.LeftPadded[*tree.Identifier])
	}
	fa.Type = r.receiveType(fa.Type, q)
	return fa
}

func (r *JavaReceiver) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
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
	// typeParameters (nil for Go)
	q.Receive(nil, nil)
	// name
	result := q.Receive(mi.Name, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		mi.Name = result.(*tree.Identifier)
	}
	// arguments
	if result := q.Receive(mi.Arguments, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		mi.Arguments = result.(tree.Container[tree.Expression])
	}
	// methodType
	mt := r.receiveType(mi.MethodType, q)
	if mt != nil {
		mi.MethodType = mt.(*tree.JavaTypeMethod)
	} else {
		mi.MethodType = nil
	}
	return mi
}

func (r *JavaReceiver) VisitAssignment(a *tree.Assignment, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *a // shallow copy to avoid mutating remoteObjects baseline
	a = &c
	result := q.Receive(a.Variable, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		a.Variable = result.(tree.Expression)
	}
	if result := q.Receive(a.Value, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		a.Value = result.(tree.LeftPadded[tree.Expression])
	}
	a.Type = r.receiveType(a.Type, q)
	return a
}

func (r *JavaReceiver) VisitAssignmentOperation(a *tree.AssignmentOperation, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *a // shallow copy to avoid mutating remoteObjects baseline
	a = &c
	result := q.Receive(a.Variable, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		a.Variable = result.(tree.Expression)
	}
	if result := q.Receive(a.Operator, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		a.Operator = result.(tree.LeftPadded[tree.AssignmentOperator])
	}
	assignResult := q.Receive(a.Assignment, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if assignResult != nil {
		a.Assignment = assignResult.(tree.Expression)
	}
	a.Type = r.receiveType(a.Type, q)
	return a
}

func (r *JavaReceiver) VisitMethodDeclaration(md *tree.MethodDeclaration, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *md // shallow copy to avoid mutating remoteObjects baseline
	md = &c
	// leadingAnnotations
	beforeAnns := make([]any, len(md.LeadingAnnotations))
	for i, a := range md.LeadingAnnotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if afterAnns != nil {
		md.LeadingAnnotations = make([]*tree.Annotation, 0, len(afterAnns))
		for _, a := range afterAnns {
			if a != nil {
				md.LeadingAnnotations = append(md.LeadingAnnotations, a.(*tree.Annotation))
			}
		}
	}
	// modifiers
	q.ReceiveList(nil, nil)
	// typeParameters
	q.Receive(nil, nil)
	// returnTypeExpression
	result := q.Receive(md.ReturnType, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		md.ReturnType = result.(tree.Expression)
	}
	// name annotations
	q.ReceiveList(nil, nil)
	// name
	nameResult := q.Receive(md.Name, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if nameResult != nil {
		md.Name = nameResult.(*tree.Identifier)
	}
	// parameters
	if result := q.Receive(md.Parameters, func(v any) any { return receiveContainerAs(r, q, v, ContainerStatement) }); result != nil {
		md.Parameters = result.(tree.Container[tree.Statement])
	}
	// throws
	q.Receive(nil, nil)
	// body
	bodyResult := q.Receive(md.Body, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if bodyResult != nil {
		md.Body = bodyResult.(*tree.Block)
	}
	// defaultValue
	q.Receive(nil, nil)
	// methodType
	mt := r.receiveType(md.MethodType, q)
	if mt != nil {
		md.MethodType = mt.(*tree.JavaTypeMethod)
	} else {
		md.MethodType = nil
	}
	return md
}

func (r *JavaReceiver) VisitVariableDeclarations(vd *tree.VariableDeclarations, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *vd // shallow copy to avoid mutating remoteObjects baseline
	vd = &c
	// leadingAnnotations
	beforeAnns := make([]any, len(vd.LeadingAnnotations))
	for i, a := range vd.LeadingAnnotations {
		beforeAnns[i] = a
	}
	afterAnns := q.ReceiveList(beforeAnns, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if afterAnns != nil {
		vd.LeadingAnnotations = make([]*tree.Annotation, 0, len(afterAnns))
		for _, a := range afterAnns {
			if a != nil {
				vd.LeadingAnnotations = append(vd.LeadingAnnotations, a.(*tree.Annotation))
			}
		}
	}
	// modifiers
	q.ReceiveList(nil, nil)
	// typeExpression
	result := q.Receive(vd.TypeExpr, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		vd.TypeExpr = result.(tree.Expression)
	}
	// varargs
	var currentVarargs any
	if vd.Varargs != nil {
		currentVarargs = *vd.Varargs
	}
	varargsResult := q.Receive(currentVarargs, func(v any) any { return receiveSpace(v.(tree.Space), q) })
	if varargsResult != nil {
		sp := varargsResult.(tree.Space)
		vd.Varargs = &sp
	}
	// variables
	beforeVars := make([]any, len(vd.Variables))
	for i, v := range vd.Variables {
		beforeVars[i] = v
	}
	afterVars := q.ReceiveList(beforeVars, func(v any) any { return receiveRightPadded(r, q, v) })
	if afterVars != nil {
		vd.Variables = make([]tree.RightPadded[*tree.VariableDeclarator], len(afterVars))
		for i, v := range afterVars {
			vd.Variables[i] = v.(tree.RightPadded[*tree.VariableDeclarator])
		}
	}
	return vd
}

func (r *JavaReceiver) VisitVariableDeclarator(vd *tree.VariableDeclarator, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *vd // shallow copy to avoid mutating remoteObjects baseline
	vd = &c
	// declarator (name)
	result := q.Receive(vd.Name, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		vd.Name = result.(*tree.Identifier)
	}
	// dimensionsAfterName
	q.ReceiveList(nil, nil)
	// initializer
	var beforeInit any
	if vd.Initializer != nil {
		beforeInit = *vd.Initializer
	}
	if result := q.Receive(beforeInit, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		lp := result.(tree.LeftPadded[tree.Expression])
		vd.Initializer = &lp
	} else {
		vd.Initializer = nil
	}
	// variableType
	q.Receive(nil, nil)
	return vd
}

func (r *JavaReceiver) VisitReturn(ret *tree.Return, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *ret
	ret = &c
	var beforeExpr any
	if len(ret.Expressions) > 0 {
		beforeExpr = ret.Expressions[0].Element
	}
	result := q.Receive(beforeExpr, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		expr := result.(tree.Expression)
		if len(ret.Expressions) > 0 {
			ret.Expressions[0] = tree.RightPadded[tree.Expression]{
				Element: expr,
				After:   ret.Expressions[0].After,
				Markers: ret.Expressions[0].Markers,
			}
		} else {
			ret.Expressions = []tree.RightPadded[tree.Expression]{
				{Element: expr},
			}
		}
	} else if beforeExpr != nil {
		ret.Expressions = nil
	}
	return ret
}

func (r *JavaReceiver) VisitIf(i *tree.If, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *i // shallow copy to avoid mutating remoteObjects baseline
	i = &c
	// ifCondition - Java sends ControlParentheses; cache it for future round-trips
	var beforeCP any
	if i.ConditionCP != nil {
		beforeCP = i.ConditionCP
	} else if i.Condition != nil {
		beforeCP = &tree.ControlParentheses{
			ID:      uuid.New(),
			Markers: tree.Markers{ID: uuid.New()},
			Tree:    tree.RightPadded[tree.Expression]{Element: i.Condition},
		}
	}
	cpResult := q.Receive(beforeCP, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if cpResult != nil {
		if cp, ok := cpResult.(*tree.ControlParentheses); ok {
			i.ConditionCP = cp
			i.Condition = cp.Tree.Element
		} else {
			i.Condition = cpResult.(tree.Expression)
		}
	}
	// thenPart - Java sends RightPadded<Statement> wrapping the Block
	if thenResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) }); thenResult != nil {
		rp := coerceToStatementRP(thenResult)
		if blk, ok := rp.Element.(*tree.Block); ok {
			i.Then = blk
		}
	}
	// elsePart - Java sends Else node, convert to RightPadded
	elseResult := q.Receive(nil, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if elseResult != nil {
		el := elseResult.(*tree.Else)
		i.ElsePart = &tree.RightPadded[tree.J]{
			Element: el.Body.Element,
			After:   el.Prefix,
		}
	} else {
		i.ElsePart = nil
	}
	return i
}

func (r *JavaReceiver) VisitElse(el *tree.Else, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *el // shallow copy to avoid mutating remoteObjects baseline
	el = &c
	if result := q.Receive(el.Body, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		el.Body = result.(tree.RightPadded[tree.Statement])
	}
	return el
}

func (r *JavaReceiver) VisitForLoop(f *tree.ForLoop, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *f // shallow copy to avoid mutating remoteObjects baseline
	f = &c
	ctrl := &f.Control
	result := q.Receive(ctrl, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		f.Control = *result.(*tree.ForControl)
	}
	// body - Java sends RightPadded<Statement> wrapping the Block
	if bodyResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) }); bodyResult != nil {
		rp := coerceToStatementRP(bodyResult)
		if blk, ok := rp.Element.(*tree.Block); ok {
			f.Body = blk
		}
	}
	return f
}

func (r *JavaReceiver) VisitForControl(fc *tree.ForControl, p any) tree.J {
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
		rp := initList[0].(tree.RightPadded[tree.Statement])
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
		rp := updateList[0].(tree.RightPadded[tree.Statement])
		fc.Update = &rp
	} else {
		fc.Update = nil
	}
	return fc
}

func (r *JavaReceiver) VisitForEachLoop(f *tree.ForEachLoop, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *f // shallow copy to avoid mutating remoteObjects baseline
	f = &c
	ctrl := &f.Control
	result := q.Receive(ctrl, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		f.Control = *result.(*tree.ForEachControl)
	}
	// body - Java sends RightPadded<Statement> wrapping the Block
	if bodyResult := q.Receive(nil, func(v any) any { return receiveRightPadded(r, q, v) }); bodyResult != nil {
		rp := coerceToStatementRP(bodyResult)
		if blk, ok := rp.Element.(*tree.Block); ok {
			f.Body = blk
		}
	}
	return f
}

func (r *JavaReceiver) VisitForEachControl(fc *tree.ForEachControl, p any) tree.J {
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
	// operator (left-padded AssignOp as string)
	if result := q.Receive(fc.Operator, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		fc.Operator = result.(tree.LeftPadded[tree.AssignOp])
	}
	// iterable
	result := q.Receive(fc.Iterable, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		fc.Iterable = result.(tree.Expression)
	}
	return fc
}

func (r *JavaReceiver) VisitSwitch(sw *tree.Switch, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *sw // shallow copy to avoid mutating remoteObjects baseline
	sw = &c
	// selector - Java sends ControlParentheses, extract inner Expression for Tag
	cpResult := q.Receive(nil, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if cpResult != nil {
		if cp, ok := cpResult.(*tree.ControlParentheses); ok {
			if _, isEmpty := cp.Tree.Element.(*tree.Empty); !isEmpty {
				sw.Tag = &tree.RightPadded[tree.Expression]{
					Element: cp.Tree.Element,
					After:   cp.Tree.After,
				}
			}
		}
	}
	result := q.Receive(sw.Body, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		sw.Body = result.(*tree.Block)
	}
	return sw
}

func (r *JavaReceiver) VisitCase(cs *tree.Case, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *cs // shallow copy to avoid mutating remoteObjects baseline
	cs = &c
	q.Receive(nil, nil) // type enum
	if result := q.Receive(cs.Expressions, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		cs.Expressions = result.(tree.Container[tree.Expression])
	}
	// statements - Java sends Container<RightPadded<Statement>>, extract to Go's []RightPadded[Statement]
	if result := q.Receive(nil, func(v any) any { return receiveContainerAs(r, q, v, ContainerStatement) }); result != nil {
		cont := result.(tree.Container[tree.Statement])
		cs.Body = cont.Elements
	}
	q.Receive(nil, nil) // body
	q.Receive(nil, nil) // guard
	return cs
}

func (r *JavaReceiver) VisitBreak(b *tree.Break, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *b // shallow copy to avoid mutating remoteObjects baseline
	b = &c
	result := q.Receive(b.Label, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		b.Label = result.(*tree.Identifier)
	}
	return b
}

func (r *JavaReceiver) VisitContinue(cont *tree.Continue, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *cont // shallow copy to avoid mutating remoteObjects baseline
	cont = &c
	result := q.Receive(cont.Label, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		cont.Label = result.(*tree.Identifier)
	}
	return cont
}

func (r *JavaReceiver) VisitLabel(l *tree.Label, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *l // shallow copy to avoid mutating remoteObjects baseline
	l = &c
	if result := q.Receive(l.Name, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		l.Name = coerceRightPaddedIdent(result)
	}
	result := q.Receive(l.Statement, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		l.Statement = result.(tree.Statement)
	}
	return l
}

func (r *JavaReceiver) VisitArrayType(at *tree.ArrayType, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *at // shallow copy to avoid mutating remoteObjects baseline
	at = &c
	result := q.Receive(at.ElementType, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		at.ElementType = result.(tree.Expression)
	}
	q.ReceiveList(nil, nil) // annotations
	if result := q.Receive(at.Dimension, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		at.Dimension = result.(tree.LeftPadded[tree.Space])
	}
	at.Type = r.receiveType(at.Type, q)
	return at
}

func (r *JavaReceiver) VisitParameterizedType(pt *tree.ParameterizedType, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *pt
	pt = &c
	if result := q.Receive(pt.Clazz, func(v any) any { return r.Visit(v.(tree.Tree), q) }); result != nil {
		pt.Clazz = result.(tree.Expression)
	}
	if result := q.Receive(pt.TypeParameters, func(v any) any { return receiveContainer(r, q, v) }); result != nil {
		container := result.(tree.Container[tree.Expression])
		pt.TypeParameters = &container
	}
	pt.Type = r.receiveType(pt.Type, q)
	return pt
}

func (r *JavaReceiver) VisitArrayAccess(aa *tree.ArrayAccess, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *aa // shallow copy to avoid mutating remoteObjects baseline
	aa = &c
	result := q.Receive(aa.Indexed, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		aa.Indexed = result.(tree.Expression)
	}
	dimResult := q.Receive(aa.Dimension, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if dimResult != nil {
		aa.Dimension = dimResult.(*tree.ArrayDimension)
	}
	return aa
}

func (r *JavaReceiver) VisitArrayDimension(ad *tree.ArrayDimension, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *ad // shallow copy to avoid mutating remoteObjects baseline
	ad = &c
	if result := q.Receive(ad.Index, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		ad.Index = coerceToExpressionRP(result)
	}
	return ad
}

func (r *JavaReceiver) VisitParentheses(parens *tree.Parentheses, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *parens // shallow copy to avoid mutating remoteObjects baseline
	parens = &c
	if result := q.Receive(parens.Tree, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		parens.Tree = coerceToExpressionRP(result)
	}
	return parens
}

func (r *JavaReceiver) VisitTypeCast(tc *tree.TypeCast, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *tc // shallow copy to avoid mutating remoteObjects baseline
	tc = &c
	result := q.Receive(tc.Clazz, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		tc.Clazz = result.(*tree.ControlParentheses)
	}
	exprResult := q.Receive(tc.Expr, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if exprResult != nil {
		tc.Expr = exprResult.(tree.Expression)
	}
	return tc
}

func (r *JavaReceiver) VisitControlParentheses(cp *tree.ControlParentheses, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *cp // shallow copy to avoid mutating remoteObjects baseline
	cp = &c
	if result := q.Receive(cp.Tree, func(v any) any { return receiveRightPadded(r, q, v) }); result != nil {
		cp.Tree = coerceToExpressionRP(result)
	}
	return cp
}

func (r *JavaReceiver) VisitImport(imp *tree.Import, p any) tree.J {
	q := p.(*ReceiveQueue)
	c := *imp // shallow copy to avoid mutating remoteObjects baseline
	imp = &c
	// static (always false for Go, but must receive full LeftPadded protocol)
	staticBefore := tree.LeftPadded[bool]{Before: tree.EmptySpace, Element: false}
	q.Receive(staticBefore, func(v any) any { return receiveLeftPadded(r, q, v) })
	// qualid (Expression - could be Literal or FieldAccess depending on direction)
	result := q.Receive(imp.Qualid, func(v any) any { return r.Visit(v.(tree.Tree), q) })
	if result != nil {
		imp.Qualid = result.(tree.Expression)
	}
	// alias
	var beforeAlias any
	if imp.Alias != nil {
		beforeAlias = *imp.Alias
	}
	if result := q.Receive(beforeAlias, func(v any) any { return receiveLeftPadded(r, q, v) }); result != nil {
		lp := result.(tree.LeftPadded[*tree.Identifier])
		imp.Alias = &lp
	} else {
		imp.Alias = nil
	}
	return imp
}

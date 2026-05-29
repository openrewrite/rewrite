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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// JavaTypeReceiver deserializes JavaType objects from the receive queue.
// Field ordering MUST match JavaTypeReceiver.java exactly.
// Embeds JavaTypeVisitor for dispatch; override methods deserialize each type.
type JavaTypeReceiver struct {
	visitor.JavaTypeVisitor
}

// NewJavaTypeReceiver creates a JavaTypeReceiver with virtual dispatch wired.
func NewJavaTypeReceiver() *JavaTypeReceiver {
	r := &JavaTypeReceiver{}
	r.Self = r // wire virtual dispatch
	return r
}

// VisitAnnotation mirrors JavaTypeReceiver.visitAnnotation
func (r *JavaTypeReceiver) VisitAnnotation(a *java.JavaTypeAnnotation, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	a.Type = receiveAsFullyQualified(r, q, a.Type)
	a.Values = r.receiveAnnotationElementValueList(q, a.Values)
	return a
}

func (r *JavaTypeReceiver) receiveAnnotationElementValueList(
	q *ReceiveQueue, before []java.JavaTypeAnnotationElementValue,
) []java.JavaTypeAnnotationElementValue {
	beforeAny := elementValueSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.receiveAnnotationElementValue(q, v.(java.JavaTypeAnnotationElementValue))
	})
	if afterAny == nil {
		return nil
	}
	out := make([]java.JavaTypeAnnotationElementValue, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(java.JavaTypeAnnotationElementValue)
	}
	return out
}

func (r *JavaTypeReceiver) receiveAnnotationElementValue(
	q *ReceiveQueue, v java.JavaTypeAnnotationElementValue,
) java.JavaTypeAnnotationElementValue {
	switch ev := v.(type) {
	case *java.JavaTypeAnnotationArrayElementValue:
		ev.Element = receiveAsType[java.JavaType](r, q, ev.Element)
		// Constant values arrive as whatever the JSON-RPC layer deserialized;
		// numeric subtype and char/string distinctions are not preserved.
		ev.ConstantValues = q.ReceiveList(ev.ConstantValues, nil)
		ev.ReferenceValues = receiveTypeList(r, q, ev.ReferenceValues)
		return ev
	case *java.JavaTypeAnnotationSingleElementValue:
		ev.Element = receiveAsType[java.JavaType](r, q, ev.Element)
		ev.ConstantValue = q.Receive(ev.ConstantValue, nil)
		ev.ReferenceValue = receiveAsType[java.JavaType](r, q, ev.ReferenceValue)
		return ev
	default:
		return v
	}
}

// VisitMultiCatch mirrors JavaTypeReceiver.visitMultiCatch
func (r *JavaTypeReceiver) VisitMultiCatch(mc *java.JavaTypeMultiCatch, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	mc.ThrowableTypes = receiveTypeList(r, q, mc.ThrowableTypes)
	return mc
}

// VisitIntersection mirrors JavaTypeReceiver.visitIntersection
func (r *JavaTypeReceiver) VisitIntersection(is *java.JavaTypeIntersection, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	is.Bounds = receiveTypeList(r, q, is.Bounds)
	return is
}

// VisitClass mirrors JavaTypeReceiver.visitClass field order:
// flagsBitMap, kind, fullyQualifiedName, typeParameters, supertype, owningClass,
// annotations, interfaces, members, methods
func (r *JavaTypeReceiver) VisitClass(c *java.JavaTypeClass, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	r.receiveClassFields(c, q)
	return c
}

// VisitShallowClass mirrors VisitClass — Java's ShallowClass extends Class
// and uses the same field set; the discriminator is the wire valueType.
func (r *JavaTypeReceiver) VisitShallowClass(sc *java.JavaTypeShallowClass, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	r.receiveClassFields(&sc.JavaTypeClass, q)
	return sc
}

func (r *JavaTypeReceiver) receiveClassFields(c *java.JavaTypeClass, q *ReceiveQueue) {
	c.FlagsBitMap = receiveScalar[int64](q, c.FlagsBitMap)
	c.Kind = receiveScalar[string](q, c.Kind)
	c.FullyQualifiedName = receiveScalar[string](q, c.FullyQualifiedName)
	c.TypeParameters = receiveTypeList(r, q, c.TypeParameters)
	c.Supertype = receiveAsFullyQualified(r, q, c.Supertype)
	c.OwningClass = receiveAsFullyQualified(r, q, c.OwningClass)
	c.Annotations = receiveClassList(r, q, c.Annotations)
	c.Interfaces = receiveClassList(r, q, c.Interfaces)
	c.Members = receiveVariableList(r, q, c.Members)
	c.Methods = receiveMethodList(r, q, c.Methods)
}

// VisitParameterized mirrors JavaTypeReceiver.visitParameterized
func (r *JavaTypeReceiver) VisitParameterized(pt *java.JavaTypeParameterized, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	pt.Type = receiveAsFullyQualified(r, q, pt.Type)
	pt.TypeParameters = receiveTypeList(r, q, pt.TypeParameters)
	return pt
}

// VisitGenericTypeVariable mirrors JavaTypeReceiver.visitGenericTypeVariable
func (r *JavaTypeReceiver) VisitGenericTypeVariable(g *java.JavaTypeGenericTypeVariable, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	g.Name = receiveScalar[string](q, g.Name)
	g.Variance = receiveScalar[string](q, g.Variance)
	g.Bounds = receiveTypeList(r, q, g.Bounds)
	return g
}

// VisitArray mirrors JavaTypeReceiver.visitArray
func (r *JavaTypeReceiver) VisitArray(a *java.JavaTypeArray, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	a.ElemType = receiveAsType[java.JavaType](r, q, a.ElemType)
	a.Annotations = receiveClassList(r, q, a.Annotations)
	return a
}

// VisitPrimitive mirrors JavaTypeReceiver.visitPrimitive
func (r *JavaTypeReceiver) VisitPrimitive(pr *java.JavaTypePrimitive, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	pr.Keyword = receiveScalar[string](q, pr.Keyword)
	return pr
}

// VisitMethod mirrors JavaTypeReceiver.visitMethod field order:
// declaringType, name, flagsBitMap, returnType, parameterNames, parameterTypes,
// thrownExceptions, annotations, defaultValue, declaredFormalTypeNames
func (r *JavaTypeReceiver) VisitMethod(m *java.JavaTypeMethod, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	m.DeclaringType = receiveAsFullyQualified(r, q, m.DeclaringType)
	m.Name = receiveScalar[string](q, m.Name)
	m.FlagsBitMap = receiveScalar[int64](q, m.FlagsBitMap)
	m.ReturnType = receiveAsType[java.JavaType](r, q, m.ReturnType)
	m.ParameterNames = receiveStringList(q, m.ParameterNames)
	m.ParameterTypes = receiveTypeList(r, q, m.ParameterTypes)
	m.ThrownExceptions = receiveTypeList(r, q, m.ThrownExceptions)
	m.Annotations = receiveClassList(r, q, m.Annotations)
	m.DefaultValue = receiveStringList(q, m.DefaultValue)
	m.DeclaredFormalTypeNames = receiveStringList(q, m.DeclaredFormalTypeNames)
	return m
}

// VisitVariable mirrors JavaTypeReceiver.visitVariable
func (r *JavaTypeReceiver) VisitVariable(v *java.JavaTypeVariable, p any) java.JavaType {
	q := p.(*ReceiveQueue)
	v.Name = receiveScalar[string](q, v.Name)
	v.Owner = receiveAsType[java.JavaType](r, q, v.Owner)
	v.Type = receiveAsType[java.JavaType](r, q, v.Type)
	v.Annotations = receiveClassList(r, q, v.Annotations)
	return v
}

// receiveScalar receives a simple value from the queue.
func receiveScalar[T any](q *ReceiveQueue, before T) T {
	result := q.Receive(before, nil)
	if result == nil {
		var zero T
		return zero
	}
	// Handle numeric type conversions from JSON
	return convertTo[T](result)
}

// receiveValue receives a field that needs a deserialization closure, returning the
// typed value directly (the zero value — nil for pointer/interface T — on delete) so
// call sites are a single branch-free assignment. This is the free-function analog of
// the generic q.receive(before, onChange) in the Java/TS/Python receivers (Go forbids
// type-parameterized methods).
//
// onChange receives the prior value already typed as T (receiveValue does the inbound
// cast once) and returns the deserialized value; its result is narrowed back to T here,
// so closure bodies need no casts, e.g.:
//
//	gs.Expr = receiveValue(q, gs.Expr, func(e java.Expression) any { return r.Visit(e, q) })
//	b.End   = receiveValue(q, b.End,   func(s java.Space) any { return receiveSpace(s, q) })
//
// Semantics mirror Java's RpcReceiveQueue.receive: NO_CHANGE returns `before`, DELETE
// returns the zero value (nil for the pointer/interface T of nullable fields — Java's
// `return null`), ADD/CHANGE returns the deserialized value. q.Receive yields nil only
// on DELETE or a nil `before`, so the zero return never produces a bogus empty value for
// the mandatory value-typed fields (which are never deleted).
func receiveValue[T any](q *ReceiveQueue, before T, onChange func(T) any) T {
	result := q.Receive(before, func(v any) any { return onChange(v.(T)) })
	if result == nil {
		var zero T
		return zero
	}
	return result.(T)
}

// convertTo converts a value to the desired type, handling JSON number conversions.
func convertTo[T any](v any) T {
	if t, ok := v.(T); ok {
		return t
	}
	// Handle float64 -> int64 conversion (common with JSON)
	var zero T
	switch any(zero).(type) {
	case int64:
		switch n := v.(type) {
		case float64:
			return any(int64(n)).(T)
		case int:
			return any(int64(n)).(T)
		}
	case string:
		if s, ok := v.(string); ok {
			return any(s).(T)
		}
	}
	return v.(T)
}

// receiveAsType receives a ref-tracked type from the queue.
func receiveAsType[T java.JavaType](r *JavaTypeReceiver, q *ReceiveQueue, before T) T {
	result := q.Receive(before, func(v any) any {
		return r.Visit(v.(java.JavaType), q)
	})
	if result == nil {
		var zero T
		return zero
	}
	if typed, ok := result.(T); ok {
		return typed
	}
	// Incompatible type (e.g., JavaTypeUnknown where JavaTypeClass expected)
	var zero T
	return zero
}

// receiveTypeList receives a list of JavaTypes from the queue.
func receiveTypeList(r *JavaTypeReceiver, q *ReceiveQueue, before []java.JavaType) []java.JavaType {
	beforeAny := javaTypeSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.Visit(v.(java.JavaType), q)
	})
	if afterAny == nil {
		return nil
	}
	result := make([]java.JavaType, len(afterAny))
	for i, v := range afterAny {
		result[i] = v.(java.JavaType)
	}
	return result
}

// receiveClassList receives a list of FullyQualified class types from the
// queue. The element type is widened (vs. concrete *JavaTypeClass) so
// that JavaType$ShallowClass payloads keep their wire identity through
// the round-trip.
func receiveClassList(r *JavaTypeReceiver, q *ReceiveQueue, before []java.FullyQualified) []java.FullyQualified {
	beforeAny := classSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.Visit(v.(java.JavaType), q)
	})
	if afterAny == nil {
		return nil
	}
	result := make([]java.FullyQualified, len(afterAny))
	for i, v := range afterAny {
		if fq, ok := v.(java.FullyQualified); ok {
			result[i] = fq
		}
		// Unknown / non-fully-qualified types are skipped (nil in the list)
	}
	return result
}

// receiveAsFullyQualified receives a class-typed JavaType (Class or
// ShallowClass) and preserves its concrete identity.
func receiveAsFullyQualified(r *JavaTypeReceiver, q *ReceiveQueue, before java.FullyQualified) java.FullyQualified {
	var beforeAny any
	if before != nil {
		beforeAny = before
	}
	result := q.Receive(beforeAny, func(v any) any {
		return r.Visit(v.(java.JavaType), q)
	})
	if result == nil {
		return nil
	}
	if fq, ok := result.(java.FullyQualified); ok {
		return fq
	}
	return nil
}

// receiveVariableList receives a list of *JavaTypeVariable from the queue.
func receiveVariableList(r *JavaTypeReceiver, q *ReceiveQueue, before []*java.JavaTypeVariable) []*java.JavaTypeVariable {
	beforeAny := variableSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.Visit(v.(java.JavaType), q)
	})
	if afterAny == nil {
		return nil
	}
	result := make([]*java.JavaTypeVariable, len(afterAny))
	for i, v := range afterAny {
		result[i] = v.(*java.JavaTypeVariable)
	}
	return result
}

// receiveMethodList receives a list of *JavaTypeMethod from the queue.
func receiveMethodList(r *JavaTypeReceiver, q *ReceiveQueue, before []*java.JavaTypeMethod) []*java.JavaTypeMethod {
	beforeAny := methodSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.Visit(v.(java.JavaType), q)
	})
	if afterAny == nil {
		return nil
	}
	result := make([]*java.JavaTypeMethod, len(afterAny))
	for i, v := range afterAny {
		result[i] = v.(*java.JavaTypeMethod)
	}
	return result
}

// receiveStringList receives a list of strings from the queue.
func receiveStringList(q *ReceiveQueue, before []string) []string {
	beforeAny := stringSlice(before)
	afterAny := q.ReceiveList(beforeAny, nil)
	if afterAny == nil {
		return nil
	}
	result := make([]string, len(afterAny))
	for i, v := range afterAny {
		result[i] = v.(string)
	}
	return result
}

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
	"github.com/openrewrite/rewrite/pkg/tree"
)

// JavaTypeReceiver deserializes JavaType objects from the receive queue.
// Field ordering MUST match JavaTypeReceiver.java exactly.
type JavaTypeReceiver struct{}

// VisitType dispatches to the appropriate visit method based on concrete type.
func (r *JavaTypeReceiver) VisitType(before tree.JavaType, q *ReceiveQueue) tree.JavaType {
	if before == nil {
		return nil
	}
	switch v := before.(type) {
	case *tree.JavaTypeClass:
		return r.visitClass(v, q)
	case *tree.JavaTypeParameterized:
		return r.visitParameterized(v, q)
	case *tree.JavaTypeGenericTypeVariable:
		return r.visitGenericTypeVariable(v, q)
	case *tree.JavaTypeArray:
		return r.visitArray(v, q)
	case *tree.JavaTypePrimitive:
		return r.visitPrimitive(v, q)
	case *tree.JavaTypeMethod:
		return r.visitMethod(v, q)
	case *tree.JavaTypeVariable:
		return r.visitVariable(v, q)
	case *tree.JavaTypeAnnotation:
		return r.visitAnnotation(v, q)
	case *tree.JavaTypeMultiCatch:
		return r.visitMultiCatch(v, q)
	case *tree.JavaTypeIntersection:
		return r.visitIntersection(v, q)
	case *tree.JavaTypeUnknown:
		return before
	default:
		return before
	}
}

// visitAnnotation mirrors JavaTypeReceiver.visitAnnotation
func (r *JavaTypeReceiver) visitAnnotation(a *tree.JavaTypeAnnotation, q *ReceiveQueue) *tree.JavaTypeAnnotation {
	a.Type = receiveAsType[*tree.JavaTypeClass](r, q, a.Type)
	return a
}

// visitMultiCatch mirrors JavaTypeReceiver.visitMultiCatch
func (r *JavaTypeReceiver) visitMultiCatch(mc *tree.JavaTypeMultiCatch, q *ReceiveQueue) *tree.JavaTypeMultiCatch {
	mc.ThrowableTypes = receiveTypeList(r, q, mc.ThrowableTypes)
	return mc
}

// visitIntersection mirrors JavaTypeReceiver.visitIntersection
func (r *JavaTypeReceiver) visitIntersection(is *tree.JavaTypeIntersection, q *ReceiveQueue) *tree.JavaTypeIntersection {
	is.Bounds = receiveTypeList(r, q, is.Bounds)
	return is
}

// visitClass mirrors JavaTypeReceiver.visitClass field order:
// flagsBitMap, kind, fullyQualifiedName, typeParameters, supertype, owningClass,
// annotations, interfaces, members, methods
func (r *JavaTypeReceiver) visitClass(c *tree.JavaTypeClass, q *ReceiveQueue) *tree.JavaTypeClass {
	c.FlagsBitMap = receiveScalar[int64](q, c.FlagsBitMap)
	c.Kind = receiveScalar[string](q, c.Kind)
	c.FullyQualifiedName = receiveScalar[string](q, c.FullyQualifiedName)
	c.TypeParameters = receiveTypeList(r, q, c.TypeParameters)
	c.Supertype = receiveAsType[*tree.JavaTypeClass](r, q, c.Supertype)
	c.OwningClass = receiveAsType[*tree.JavaTypeClass](r, q, c.OwningClass)
	c.Annotations = receiveClassList(r, q, c.Annotations)
	c.Interfaces = receiveClassList(r, q, c.Interfaces)
	c.Members = receiveVariableList(r, q, c.Members)
	c.Methods = receiveMethodList(r, q, c.Methods)
	return c
}

// visitParameterized mirrors JavaTypeReceiver.visitParameterized
func (r *JavaTypeReceiver) visitParameterized(p *tree.JavaTypeParameterized, q *ReceiveQueue) *tree.JavaTypeParameterized {
	p.Type = receiveAsType[*tree.JavaTypeClass](r, q, p.Type)
	p.TypeParameters = receiveTypeList(r, q, p.TypeParameters)
	return p
}

// visitGenericTypeVariable mirrors JavaTypeReceiver.visitGenericTypeVariable
func (r *JavaTypeReceiver) visitGenericTypeVariable(g *tree.JavaTypeGenericTypeVariable, q *ReceiveQueue) *tree.JavaTypeGenericTypeVariable {
	g.Name = receiveScalar[string](q, g.Name)
	g.Variance = receiveScalar[string](q, g.Variance)
	g.Bounds = receiveTypeList(r, q, g.Bounds)
	return g
}

// visitArray mirrors JavaTypeReceiver.visitArray
func (r *JavaTypeReceiver) visitArray(a *tree.JavaTypeArray, q *ReceiveQueue) *tree.JavaTypeArray {
	a.ElemType = receiveAsType[tree.JavaType](r, q, a.ElemType)
	a.Annotations = receiveClassList(r, q, a.Annotations)
	return a
}

// visitPrimitive mirrors JavaTypeReceiver.visitPrimitive
func (r *JavaTypeReceiver) visitPrimitive(p *tree.JavaTypePrimitive, q *ReceiveQueue) *tree.JavaTypePrimitive {
	p.Keyword = receiveScalar[string](q, p.Keyword)
	return p
}

// visitMethod mirrors JavaTypeReceiver.visitMethod field order:
// declaringType, name, flagsBitMap, returnType, parameterNames, parameterTypes,
// thrownExceptions, annotations, defaultValue, declaredFormalTypeNames
func (r *JavaTypeReceiver) visitMethod(m *tree.JavaTypeMethod, q *ReceiveQueue) *tree.JavaTypeMethod {
	m.DeclaringType = receiveAsType[*tree.JavaTypeClass](r, q, m.DeclaringType)
	m.Name = receiveScalar[string](q, m.Name)
	m.FlagsBitMap = receiveScalar[int64](q, m.FlagsBitMap)
	m.ReturnType = receiveAsType[tree.JavaType](r, q, m.ReturnType)
	m.ParameterNames = receiveStringList(q, m.ParameterNames)
	m.ParameterTypes = receiveTypeList(r, q, m.ParameterTypes)
	m.ThrownExceptions = receiveTypeList(r, q, m.ThrownExceptions)
	m.Annotations = receiveClassList(r, q, m.Annotations)
	m.DefaultValue = receiveStringList(q, m.DefaultValue)
	m.DeclaredFormalTypeNames = receiveStringList(q, m.DeclaredFormalTypeNames)
	return m
}

// visitVariable mirrors JavaTypeReceiver.visitVariable
func (r *JavaTypeReceiver) visitVariable(v *tree.JavaTypeVariable, q *ReceiveQueue) *tree.JavaTypeVariable {
	v.Name = receiveScalar[string](q, v.Name)
	v.Owner = receiveAsType[tree.JavaType](r, q, v.Owner)
	v.Type = receiveAsType[tree.JavaType](r, q, v.Type)
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
func receiveAsType[T tree.JavaType](r *JavaTypeReceiver, q *ReceiveQueue, before T) T {
	result := q.Receive(before, func(v any) any {
		return r.VisitType(v.(tree.JavaType), q)
	})
	if result == nil {
		var zero T
		return zero
	}
	return result.(T)
}

// receiveTypeList receives a list of JavaTypes from the queue.
func receiveTypeList(r *JavaTypeReceiver, q *ReceiveQueue, before []tree.JavaType) []tree.JavaType {
	beforeAny := javaTypeSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.VisitType(v.(tree.JavaType), q)
	})
	if afterAny == nil {
		return nil
	}
	result := make([]tree.JavaType, len(afterAny))
	for i, v := range afterAny {
		result[i] = v.(tree.JavaType)
	}
	return result
}

// receiveClassList receives a list of *JavaTypeClass from the queue.
func receiveClassList(r *JavaTypeReceiver, q *ReceiveQueue, before []*tree.JavaTypeClass) []*tree.JavaTypeClass {
	beforeAny := classSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.VisitType(v.(tree.JavaType), q)
	})
	if afterAny == nil {
		return nil
	}
	result := make([]*tree.JavaTypeClass, len(afterAny))
	for i, v := range afterAny {
		result[i] = v.(*tree.JavaTypeClass)
	}
	return result
}

// receiveVariableList receives a list of *JavaTypeVariable from the queue.
func receiveVariableList(r *JavaTypeReceiver, q *ReceiveQueue, before []*tree.JavaTypeVariable) []*tree.JavaTypeVariable {
	beforeAny := variableSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.VisitType(v.(tree.JavaType), q)
	})
	if afterAny == nil {
		return nil
	}
	result := make([]*tree.JavaTypeVariable, len(afterAny))
	for i, v := range afterAny {
		result[i] = v.(*tree.JavaTypeVariable)
	}
	return result
}

// receiveMethodList receives a list of *JavaTypeMethod from the queue.
func receiveMethodList(r *JavaTypeReceiver, q *ReceiveQueue, before []*tree.JavaTypeMethod) []*tree.JavaTypeMethod {
	beforeAny := methodSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		return r.VisitType(v.(tree.JavaType), q)
	})
	if afterAny == nil {
		return nil
	}
	result := make([]*tree.JavaTypeMethod, len(afterAny))
	for i, v := range afterAny {
		result[i] = v.(*tree.JavaTypeMethod)
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

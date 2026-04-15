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

// JavaTypeSender serializes JavaType objects into the send queue.
// Field ordering MUST match JavaTypeSender.java exactly.
// Embeds JavaTypeVisitor for dispatch; override methods serialize each type.
type JavaTypeSender struct {
	visitor.JavaTypeVisitor
}

// NewJavaTypeSender creates a JavaTypeSender with virtual dispatch wired.
func NewJavaTypeSender() *JavaTypeSender {
	s := &JavaTypeSender{}
	s.Self = s // wire virtual dispatch
	return s
}

// VisitAnnotation matches JavaTypeSender.visitAnnotation
func (s *JavaTypeSender) VisitAnnotation(a *tree.JavaTypeAnnotation, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*tree.JavaTypeAnnotation).Type) },
		func(v any) { s.Visit(GetValueNonNull(v).(tree.JavaType), q) })
	return a
}

// VisitMultiCatch matches JavaTypeSender.visitMultiCatch
func (s *JavaTypeSender) VisitMultiCatch(mc *tree.JavaTypeMultiCatch, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSendListAsRef(mc,
		func(v any) []any { return javaTypeSlice(v.(*tree.JavaTypeMultiCatch).ThrowableTypes) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	return mc
}

// VisitIntersection matches JavaTypeSender.visitIntersection
func (s *JavaTypeSender) VisitIntersection(is *tree.JavaTypeIntersection, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSendListAsRef(is,
		func(v any) []any { return javaTypeSlice(v.(*tree.JavaTypeIntersection).Bounds) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	return is
}

// VisitClass matches JavaTypeSender.visitClass field order exactly:
// flagsBitMap, kind, fullyQualifiedName, typeParameters, supertype, owningClass,
// annotations, interfaces, members, methods
func (s *JavaTypeSender) VisitClass(c *tree.JavaTypeClass, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(c, func(v any) any { return v.(*tree.JavaTypeClass).FlagsBitMap }, nil)
	q.GetAndSend(c, func(v any) any { return v.(*tree.JavaTypeClass).Kind }, nil)
	q.GetAndSend(c, func(v any) any { return v.(*tree.JavaTypeClass).FullyQualifiedName }, nil)
	q.GetAndSendListAsRef(c,
		func(v any) []any { return javaTypeSlice(v.(*tree.JavaTypeClass).TypeParameters) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	q.GetAndSend(c, func(v any) any { return AsRef(v.(*tree.JavaTypeClass).Supertype) },
		func(v any) { s.Visit(GetValueNonNull(v).(tree.JavaType), q) })
	q.GetAndSend(c, func(v any) any { return AsRef(v.(*tree.JavaTypeClass).OwningClass) },
		func(v any) { s.Visit(GetValueNonNull(v).(tree.JavaType), q) })
	q.GetAndSendListAsRef(c,
		func(v any) []any { return classSlice(v.(*tree.JavaTypeClass).Annotations) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	q.GetAndSendListAsRef(c,
		func(v any) []any { return classSlice(v.(*tree.JavaTypeClass).Interfaces) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	q.GetAndSendListAsRef(c,
		func(v any) []any { return variableSlice(v.(*tree.JavaTypeClass).Members) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	q.GetAndSendListAsRef(c,
		func(v any) []any { return methodSlice(v.(*tree.JavaTypeClass).Methods) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	return c
}

// VisitParameterized matches JavaTypeSender.visitParameterized
func (s *JavaTypeSender) VisitParameterized(pt *tree.JavaTypeParameterized, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(pt, func(v any) any { return AsRef(v.(*tree.JavaTypeParameterized).Type) },
		func(v any) { s.Visit(GetValueNonNull(v).(tree.JavaType), q) })
	q.GetAndSendListAsRef(pt,
		func(v any) []any { return javaTypeSlice(v.(*tree.JavaTypeParameterized).TypeParameters) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	return pt
}

// VisitGenericTypeVariable matches JavaTypeSender.visitGenericTypeVariable
func (s *JavaTypeSender) VisitGenericTypeVariable(g *tree.JavaTypeGenericTypeVariable, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(g, func(v any) any { return v.(*tree.JavaTypeGenericTypeVariable).Name }, nil)
	q.GetAndSend(g, func(v any) any { return v.(*tree.JavaTypeGenericTypeVariable).Variance }, nil)
	q.GetAndSendListAsRef(g,
		func(v any) []any { return javaTypeSlice(v.(*tree.JavaTypeGenericTypeVariable).Bounds) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	return g
}

// VisitArray matches JavaTypeSender.visitArray
func (s *JavaTypeSender) VisitArray(a *tree.JavaTypeArray, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*tree.JavaTypeArray).ElemType) },
		func(v any) { s.Visit(GetValueNonNull(v).(tree.JavaType), q) })
	q.GetAndSendListAsRef(a,
		func(v any) []any { return classSlice(v.(*tree.JavaTypeArray).Annotations) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	return a
}

// VisitPrimitive matches JavaTypeSender.visitPrimitive
func (s *JavaTypeSender) VisitPrimitive(pr *tree.JavaTypePrimitive, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(pr, func(v any) any { return v.(*tree.JavaTypePrimitive).Keyword }, nil)
	return pr
}

// VisitMethod matches JavaTypeSender.visitMethod field order exactly:
// declaringType, name, flagsBitMap, returnType, parameterNames, parameterTypes,
// thrownExceptions, annotations, defaultValue, declaredFormalTypeNames
func (s *JavaTypeSender) VisitMethod(m *tree.JavaTypeMethod, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(m, func(v any) any { return AsRef(v.(*tree.JavaTypeMethod).DeclaringType) },
		func(v any) { s.Visit(GetValueNonNull(v).(tree.JavaType), q) })
	q.GetAndSend(m, func(v any) any { return v.(*tree.JavaTypeMethod).Name }, nil)
	q.GetAndSend(m, func(v any) any { return v.(*tree.JavaTypeMethod).FlagsBitMap }, nil)
	q.GetAndSend(m, func(v any) any { return AsRef(v.(*tree.JavaTypeMethod).ReturnType) },
		func(v any) { s.Visit(GetValueNonNull(v).(tree.JavaType), q) })
	q.GetAndSendList(m,
		func(v any) []any { return stringSlice(v.(*tree.JavaTypeMethod).ParameterNames) },
		func(v any) any { return v },
		nil)
	q.GetAndSendListAsRef(m,
		func(v any) []any { return javaTypeSlice(v.(*tree.JavaTypeMethod).ParameterTypes) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	q.GetAndSendListAsRef(m,
		func(v any) []any { return javaTypeSlice(v.(*tree.JavaTypeMethod).ThrownExceptions) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	q.GetAndSendListAsRef(m,
		func(v any) []any { return classSlice(v.(*tree.JavaTypeMethod).Annotations) },
		func(v any) any { return tree.TypeSignature(v.(tree.JavaType)) },
		func(v any) { s.Visit(v.(tree.JavaType), q) })
	q.GetAndSendList(m,
		func(v any) []any { return stringSlice(v.(*tree.JavaTypeMethod).DefaultValue) },
		func(v any) any { return v },
		nil)
	q.GetAndSendList(m,
		func(v any) []any { return stringSlice(v.(*tree.JavaTypeMethod).DeclaredFormalTypeNames) },
		func(v any) any { return v },
		nil)
	return m
}

// VisitVariable matches JavaTypeSender.visitVariable
func (s *JavaTypeSender) VisitVariable(v *tree.JavaTypeVariable, p any) tree.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(v, func(x any) any { return x.(*tree.JavaTypeVariable).Name }, nil)
	q.GetAndSend(v, func(x any) any { return AsRef(x.(*tree.JavaTypeVariable).Owner) },
		func(x any) { s.Visit(GetValueNonNull(x).(tree.JavaType), q) })
	q.GetAndSend(v, func(x any) any { return AsRef(x.(*tree.JavaTypeVariable).Type) },
		func(x any) { s.Visit(GetValueNonNull(x).(tree.JavaType), q) })
	q.GetAndSendListAsRef(v,
		func(x any) []any { return classSlice(x.(*tree.JavaTypeVariable).Annotations) },
		func(x any) any { return tree.TypeSignature(x.(tree.JavaType)) },
		func(x any) { s.Visit(x.(tree.JavaType), q) })
	return v
}

// Helper functions to convert typed slices to []any

func javaTypeSlice(types []tree.JavaType) []any {
	if types == nil {
		return nil
	}
	result := make([]any, len(types))
	for i, t := range types {
		result[i] = t
	}
	return result
}

func classSlice(classes []*tree.JavaTypeClass) []any {
	if classes == nil {
		return nil
	}
	result := make([]any, len(classes))
	for i, c := range classes {
		result[i] = c
	}
	return result
}

func variableSlice(vars []*tree.JavaTypeVariable) []any {
	if vars == nil {
		return nil
	}
	result := make([]any, len(vars))
	for i, v := range vars {
		result[i] = v
	}
	return result
}

func methodSlice(methods []*tree.JavaTypeMethod) []any {
	if methods == nil {
		return nil
	}
	result := make([]any, len(methods))
	for i, m := range methods {
		result[i] = m
	}
	return result
}

func stringSlice(strs []string) []any {
	if strs == nil {
		return nil
	}
	result := make([]any, len(strs))
	for i, s := range strs {
		result[i] = s
	}
	return result
}

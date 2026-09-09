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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Field ordering MUST match JavaTypeSender.java exactly.
// Embeds JavaTypeVisitor for dispatch; override methods serialize each type.
type JavaTypeSender struct {
	visitor.JavaTypeVisitor
}

func NewJavaTypeSender() *JavaTypeSender {
	s := &JavaTypeSender{}
	s.Self = s // wire virtual dispatch
	return s
}

func (s *JavaTypeSender) VisitAnnotation(a *java.JavaTypeAnnotation, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*java.JavaTypeAnnotation).Type) },
		func(v any) { s.Visit(GetValueNonNull(v).(java.JavaType), q) })
	q.GetAndSendListAsRef(a,
		func(v any) []any { return elementValueSlice(v.(*java.JavaTypeAnnotation).Values) },
		func(v any) any { return java.TypeSignature(v.(java.JavaTypeAnnotationElementValue).GetElement()) },
		func(v any) { s.visitAnnotationElementValue(v.(java.JavaTypeAnnotationElementValue), q) })
	return a
}

func elementValueSlice(in []java.JavaTypeAnnotationElementValue) []any {
	if in == nil {
		return nil
	}
	out := make([]any, len(in))
	for i, v := range in {
		out[i] = v
	}
	return out
}

func (s *JavaTypeSender) visitAnnotationElementValue(v java.JavaTypeAnnotationElementValue, q *SendQueue) {
	q.GetAndSend(v,
		func(x any) any { return AsRef(x.(java.JavaTypeAnnotationElementValue).GetElement()) },
		func(t any) { s.Visit(GetValueNonNull(t).(java.JavaType), q) })
	switch ev := v.(type) {
	case *java.JavaTypeAnnotationArrayElementValue:
		// Constant values are sent as raw JSON-native values; numeric subtype
		// (int/long/float/double) and char/string distinctions are not preserved.
		q.GetAndSendList(ev,
			func(x any) []any {
				return x.(*java.JavaTypeAnnotationArrayElementValue).ConstantValues
			},
			func(x any) any {
				if x == nil {
					return "null"
				}
				return fmt.Sprintf("%v", x)
			},
			nil)
		q.GetAndSendListAsRef(ev,
			func(x any) []any {
				return javaTypeSlice(x.(*java.JavaTypeAnnotationArrayElementValue).ReferenceValues)
			},
			func(x any) any { return java.TypeSignature(x.(java.JavaType)) },
			func(t any) { s.Visit(t.(java.JavaType), q) })
	case *java.JavaTypeAnnotationSingleElementValue:
		q.GetAndSend(ev,
			func(x any) any {
				return x.(*java.JavaTypeAnnotationSingleElementValue).ConstantValue
			},
			nil)
		q.GetAndSend(ev,
			func(x any) any { return AsRef(x.(*java.JavaTypeAnnotationSingleElementValue).ReferenceValue) },
			func(t any) { s.Visit(GetValueNonNull(t).(java.JavaType), q) })
	}
}

func (s *JavaTypeSender) VisitMultiCatch(mc *java.JavaTypeMultiCatch, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSendListAsRef(mc,
		func(v any) []any { return javaTypeSlice(v.(*java.JavaTypeMultiCatch).ThrowableTypes) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	return mc
}

func (s *JavaTypeSender) VisitIntersection(is *java.JavaTypeIntersection, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSendListAsRef(is,
		func(v any) []any { return javaTypeSlice(v.(*java.JavaTypeIntersection).Bounds) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	return is
}

// flagsBitMap, kind, fullyQualifiedName, typeParameters, supertype, owningClass,
// annotations, interfaces, members, methods
func (s *JavaTypeSender) VisitClass(c *java.JavaTypeClass, p any) java.JavaType {
	s.visitClassFields(c, p.(*SendQueue))
	return c
}

// VisitShallowClass walks the same field set as VisitClass — Java's
// ShallowClass extends Class and uses the same wire shape; only the
// outgoing valueType discriminator differs (handled by valueTypeMap).
func (s *JavaTypeSender) VisitShallowClass(sc *java.JavaTypeShallowClass, p any) java.JavaType {
	s.visitClassFields(sc, p.(*SendQueue))
	return sc
}

// toClassFields normalizes a Class-or-ShallowClass any to the embedded
// *JavaTypeClass that holds the actual fields. Returns nil for any other
// shape so callers can no-op on type-mismatched `before` states.
func toClassFields(v any) *java.JavaTypeClass {
	switch c := v.(type) {
	case *java.JavaTypeClass:
		return c
	case *java.JavaTypeShallowClass:
		return &c.JavaTypeClass
	}
	return nil
}

// visitClassFields walks the wire-shape fields shared by JavaType$Class
// and JavaType$ShallowClass. The `parent` argument is whichever outer
// pointer the visitor was invoked with (either *JavaTypeClass or
// *JavaTypeShallowClass); the closures call toClassFields so the
// `before` value supplied by q.GetAndSend works for either type.
func (s *JavaTypeSender) visitClassFields(parent any, q *SendQueue) {
	q.GetAndSend(parent, func(v any) any { return toClassFields(v).FlagsBitMap }, nil)
	q.GetAndSend(parent, func(v any) any { return toClassFields(v).Kind }, nil)
	q.GetAndSend(parent, func(v any) any { return toClassFields(v).FullyQualifiedName }, nil)
	q.GetAndSendListAsRef(parent,
		func(v any) []any { return javaTypeSlice(toClassFields(v).TypeParameters) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	q.GetAndSend(parent, func(v any) any { return AsRef(toClassFields(v).Supertype) },
		func(v any) { s.Visit(GetValueNonNull(v).(java.JavaType), q) })
	q.GetAndSend(parent, func(v any) any { return AsRef(toClassFields(v).OwningClass) },
		func(v any) { s.Visit(GetValueNonNull(v).(java.JavaType), q) })
	q.GetAndSendListAsRef(parent,
		func(v any) []any { return classSlice(toClassFields(v).Annotations) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	q.GetAndSendListAsRef(parent,
		func(v any) []any { return classSlice(toClassFields(v).Interfaces) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	q.GetAndSendListAsRef(parent,
		func(v any) []any { return variableSlice(toClassFields(v).Members) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	q.GetAndSendListAsRef(parent,
		func(v any) []any { return methodSlice(toClassFields(v).Methods) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
}

func (s *JavaTypeSender) VisitParameterized(pt *java.JavaTypeParameterized, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(pt, func(v any) any { return AsRef(v.(*java.JavaTypeParameterized).Type) },
		func(v any) { s.Visit(GetValueNonNull(v).(java.JavaType), q) })
	q.GetAndSendListAsRef(pt,
		func(v any) []any { return javaTypeSlice(v.(*java.JavaTypeParameterized).TypeParameters) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	return pt
}

func (s *JavaTypeSender) VisitGenericTypeVariable(g *java.JavaTypeGenericTypeVariable, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(g, func(v any) any { return v.(*java.JavaTypeGenericTypeVariable).Name }, nil)
	q.GetAndSend(g, func(v any) any { return v.(*java.JavaTypeGenericTypeVariable).Variance }, nil)
	q.GetAndSendListAsRef(g,
		func(v any) []any { return javaTypeSlice(v.(*java.JavaTypeGenericTypeVariable).Bounds) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	return g
}

func (s *JavaTypeSender) VisitArray(a *java.JavaTypeArray, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(a, func(v any) any { return AsRef(v.(*java.JavaTypeArray).ElemType) },
		func(v any) { s.Visit(GetValueNonNull(v).(java.JavaType), q) })
	q.GetAndSendListAsRef(a,
		func(v any) []any { return classSlice(v.(*java.JavaTypeArray).Annotations) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	return a
}

func (s *JavaTypeSender) VisitPrimitive(pr *java.JavaTypePrimitive, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(pr, func(v any) any { return v.(*java.JavaTypePrimitive).Keyword }, nil)
	return pr
}

// declaringType, name, flagsBitMap, returnType, parameterNames, parameterTypes,
// thrownExceptions, annotations, defaultValue, declaredFormalTypeNames
func (s *JavaTypeSender) VisitMethod(m *java.JavaTypeMethod, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(m, func(v any) any { return AsRef(v.(*java.JavaTypeMethod).DeclaringType) },
		func(v any) { s.Visit(GetValueNonNull(v).(java.JavaType), q) })
	q.GetAndSend(m, func(v any) any { return v.(*java.JavaTypeMethod).Name }, nil)
	q.GetAndSend(m, func(v any) any { return v.(*java.JavaTypeMethod).FlagsBitMap }, nil)
	q.GetAndSend(m, func(v any) any { return AsRef(v.(*java.JavaTypeMethod).ReturnType) },
		func(v any) { s.Visit(GetValueNonNull(v).(java.JavaType), q) })
	q.GetAndSendList(m,
		func(v any) []any { return stringSlice(v.(*java.JavaTypeMethod).ParameterNames) },
		func(v any) any { return v },
		nil)
	q.GetAndSendListAsRef(m,
		func(v any) []any { return javaTypeSlice(v.(*java.JavaTypeMethod).ParameterTypes) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	q.GetAndSendListAsRef(m,
		func(v any) []any { return javaTypeSlice(v.(*java.JavaTypeMethod).ThrownExceptions) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	q.GetAndSendListAsRef(m,
		func(v any) []any { return classSlice(v.(*java.JavaTypeMethod).Annotations) },
		func(v any) any { return java.TypeSignature(v.(java.JavaType)) },
		func(v any) { s.Visit(v.(java.JavaType), q) })
	q.GetAndSendList(m,
		func(v any) []any { return stringSlice(v.(*java.JavaTypeMethod).DefaultValue) },
		func(v any) any { return v },
		nil)
	q.GetAndSendList(m,
		func(v any) []any { return stringSlice(v.(*java.JavaTypeMethod).DeclaredFormalTypeNames) },
		func(v any) any { return v },
		nil)
	return m
}

func (s *JavaTypeSender) VisitVariable(v *java.JavaTypeVariable, p any) java.JavaType {
	q := p.(*SendQueue)
	q.GetAndSend(v, func(x any) any { return x.(*java.JavaTypeVariable).Name }, nil)
	q.GetAndSend(v, func(x any) any { return AsRef(x.(*java.JavaTypeVariable).Owner) },
		func(x any) { s.Visit(GetValueNonNull(x).(java.JavaType), q) })
	q.GetAndSend(v, func(x any) any { return AsRef(x.(*java.JavaTypeVariable).Type) },
		func(x any) { s.Visit(GetValueNonNull(x).(java.JavaType), q) })
	q.GetAndSendListAsRef(v,
		func(x any) []any { return classSlice(x.(*java.JavaTypeVariable).Annotations) },
		func(x any) any { return java.TypeSignature(x.(java.JavaType)) },
		func(x any) { s.Visit(x.(java.JavaType), q) })
	return v
}

func javaTypeSlice(types []java.JavaType) []any {
	if types == nil {
		return nil
	}
	result := make([]any, len(types))
	for i, t := range types {
		result[i] = t
	}
	return result
}

func classSlice(classes []java.FullyQualified) []any {
	if classes == nil {
		return nil
	}
	result := make([]any, len(classes))
	for i, c := range classes {
		result[i] = c
	}
	return result
}

func variableSlice(vars []*java.JavaTypeVariable) []any {
	if vars == nil {
		return nil
	}
	result := make([]any, len(vars))
	for i, v := range vars {
		result[i] = v
	}
	return result
}

func methodSlice(methods []*java.JavaTypeMethod) []any {
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

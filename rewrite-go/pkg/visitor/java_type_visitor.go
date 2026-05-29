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

package visitor

import "github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"

// JavaTypeVisitor traverses and optionally transforms JavaType instances.
// Mirrors org.openrewrite.java.JavaTypeVisitor in Java.
// Embed in a struct and override visit methods to customize behavior.
type JavaTypeVisitor struct {
	// Self must point to the outermost embedding struct for virtual dispatch.
	Self JavaTypeVisitorI
}

// JavaTypeVisitorI defines all overridable visit methods for JavaType.
type JavaTypeVisitorI interface {
	Visit(javaType java.JavaType, p any) java.JavaType
	VisitAnnotation(annotation *java.JavaTypeAnnotation, p any) java.JavaType
	VisitArray(array *java.JavaTypeArray, p any) java.JavaType
	VisitClass(class_ *java.JavaTypeClass, p any) java.JavaType
	VisitShallowClass(shallow *java.JavaTypeShallowClass, p any) java.JavaType
	VisitGenericTypeVariable(generic *java.JavaTypeGenericTypeVariable, p any) java.JavaType
	VisitIntersection(intersection *java.JavaTypeIntersection, p any) java.JavaType
	VisitMethod(method *java.JavaTypeMethod, p any) java.JavaType
	VisitMultiCatch(multiCatch *java.JavaTypeMultiCatch, p any) java.JavaType
	VisitParameterized(parameterized *java.JavaTypeParameterized, p any) java.JavaType
	VisitPrimitive(primitive *java.JavaTypePrimitive, p any) java.JavaType
	VisitUnknown(unknown *java.JavaTypeUnknown, p any) java.JavaType
	VisitVariable(variable *java.JavaTypeVariable, p any) java.JavaType
}

func (v *JavaTypeVisitor) self() JavaTypeVisitorI {
	if v.Self != nil {
		return v.Self
	}
	return v
}

// Visit dispatches to the appropriate visit method based on concrete type.
func (v *JavaTypeVisitor) Visit(javaType java.JavaType, p any) java.JavaType {
	if javaType == nil {
		return nil
	}

	switch t := javaType.(type) {
	case *java.JavaTypeAnnotation:
		return v.self().VisitAnnotation(t, p)
	case *java.JavaTypeArray:
		return v.self().VisitArray(t, p)
	case *java.JavaTypeShallowClass:
		return v.self().VisitShallowClass(t, p)
	case *java.JavaTypeClass:
		return v.self().VisitClass(t, p)
	case *java.JavaTypeGenericTypeVariable:
		return v.self().VisitGenericTypeVariable(t, p)
	case *java.JavaTypeIntersection:
		return v.self().VisitIntersection(t, p)
	case *java.JavaTypeMethod:
		return v.self().VisitMethod(t, p)
	case *java.JavaTypeMultiCatch:
		return v.self().VisitMultiCatch(t, p)
	case *java.JavaTypeParameterized:
		return v.self().VisitParameterized(t, p)
	case *java.JavaTypePrimitive:
		return v.self().VisitPrimitive(t, p)
	case *java.JavaTypeUnknown:
		return v.self().VisitUnknown(t, p)
	case *java.JavaTypeVariable:
		return v.self().VisitVariable(t, p)
	default:
		return javaType
	}
}

// VisitList visits a list of JavaTypes.
func (v *JavaTypeVisitor) VisitList(javaTypes []java.JavaType, p any) []java.JavaType {
	if javaTypes == nil {
		return nil
	}
	result := make([]java.JavaType, len(javaTypes))
	for i, jt := range javaTypes {
		result[i] = v.self().Visit(jt, p)
	}
	return result
}

// Default implementations — return the type unchanged.

func (v *JavaTypeVisitor) VisitAnnotation(annotation *java.JavaTypeAnnotation, p any) java.JavaType {
	return annotation
}

func (v *JavaTypeVisitor) VisitArray(array *java.JavaTypeArray, p any) java.JavaType {
	return array
}

func (v *JavaTypeVisitor) VisitClass(class_ *java.JavaTypeClass, p any) java.JavaType {
	return class_
}

func (v *JavaTypeVisitor) VisitShallowClass(shallow *java.JavaTypeShallowClass, p any) java.JavaType {
	return shallow
}

func (v *JavaTypeVisitor) VisitGenericTypeVariable(generic *java.JavaTypeGenericTypeVariable, p any) java.JavaType {
	return generic
}

func (v *JavaTypeVisitor) VisitIntersection(intersection *java.JavaTypeIntersection, p any) java.JavaType {
	return intersection
}

func (v *JavaTypeVisitor) VisitMethod(method *java.JavaTypeMethod, p any) java.JavaType {
	return method
}

func (v *JavaTypeVisitor) VisitMultiCatch(multiCatch *java.JavaTypeMultiCatch, p any) java.JavaType {
	return multiCatch
}

func (v *JavaTypeVisitor) VisitParameterized(parameterized *java.JavaTypeParameterized, p any) java.JavaType {
	return parameterized
}

func (v *JavaTypeVisitor) VisitPrimitive(primitive *java.JavaTypePrimitive, p any) java.JavaType {
	return primitive
}

func (v *JavaTypeVisitor) VisitUnknown(unknown *java.JavaTypeUnknown, p any) java.JavaType {
	return unknown
}

func (v *JavaTypeVisitor) VisitVariable(variable *java.JavaTypeVariable, p any) java.JavaType {
	return variable
}

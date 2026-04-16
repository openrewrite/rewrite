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

import "github.com/openrewrite/rewrite/rewrite-go/pkg/tree"

// JavaTypeVisitor traverses and optionally transforms JavaType instances.
// Mirrors org.openrewrite.java.JavaTypeVisitor in Java.
// Embed in a struct and override visit methods to customize behavior.
type JavaTypeVisitor struct {
	// Self must point to the outermost embedding struct for virtual dispatch.
	Self JavaTypeVisitorI
}

// JavaTypeVisitorI defines all overridable visit methods for JavaType.
type JavaTypeVisitorI interface {
	Visit(javaType tree.JavaType, p any) tree.JavaType
	VisitAnnotation(annotation *tree.JavaTypeAnnotation, p any) tree.JavaType
	VisitArray(array *tree.JavaTypeArray, p any) tree.JavaType
	VisitClass(class_ *tree.JavaTypeClass, p any) tree.JavaType
	VisitGenericTypeVariable(generic *tree.JavaTypeGenericTypeVariable, p any) tree.JavaType
	VisitIntersection(intersection *tree.JavaTypeIntersection, p any) tree.JavaType
	VisitMethod(method *tree.JavaTypeMethod, p any) tree.JavaType
	VisitMultiCatch(multiCatch *tree.JavaTypeMultiCatch, p any) tree.JavaType
	VisitParameterized(parameterized *tree.JavaTypeParameterized, p any) tree.JavaType
	VisitPrimitive(primitive *tree.JavaTypePrimitive, p any) tree.JavaType
	VisitUnknown(unknown *tree.JavaTypeUnknown, p any) tree.JavaType
	VisitVariable(variable *tree.JavaTypeVariable, p any) tree.JavaType
}

func (v *JavaTypeVisitor) self() JavaTypeVisitorI {
	if v.Self != nil {
		return v.Self
	}
	return v
}

// Visit dispatches to the appropriate visit method based on concrete type.
func (v *JavaTypeVisitor) Visit(javaType tree.JavaType, p any) tree.JavaType {
	if javaType == nil {
		return nil
	}

	switch t := javaType.(type) {
	case *tree.JavaTypeAnnotation:
		return v.self().VisitAnnotation(t, p)
	case *tree.JavaTypeArray:
		return v.self().VisitArray(t, p)
	case *tree.JavaTypeClass:
		return v.self().VisitClass(t, p)
	case *tree.JavaTypeGenericTypeVariable:
		return v.self().VisitGenericTypeVariable(t, p)
	case *tree.JavaTypeIntersection:
		return v.self().VisitIntersection(t, p)
	case *tree.JavaTypeMethod:
		return v.self().VisitMethod(t, p)
	case *tree.JavaTypeMultiCatch:
		return v.self().VisitMultiCatch(t, p)
	case *tree.JavaTypeParameterized:
		return v.self().VisitParameterized(t, p)
	case *tree.JavaTypePrimitive:
		return v.self().VisitPrimitive(t, p)
	case *tree.JavaTypeUnknown:
		return v.self().VisitUnknown(t, p)
	case *tree.JavaTypeVariable:
		return v.self().VisitVariable(t, p)
	default:
		return javaType
	}
}

// VisitList visits a list of JavaTypes.
func (v *JavaTypeVisitor) VisitList(javaTypes []tree.JavaType, p any) []tree.JavaType {
	if javaTypes == nil {
		return nil
	}
	result := make([]tree.JavaType, len(javaTypes))
	for i, jt := range javaTypes {
		result[i] = v.self().Visit(jt, p)
	}
	return result
}

// Default implementations — return the type unchanged.

func (v *JavaTypeVisitor) VisitAnnotation(annotation *tree.JavaTypeAnnotation, p any) tree.JavaType {
	return annotation
}

func (v *JavaTypeVisitor) VisitArray(array *tree.JavaTypeArray, p any) tree.JavaType {
	return array
}

func (v *JavaTypeVisitor) VisitClass(class_ *tree.JavaTypeClass, p any) tree.JavaType {
	return class_
}

func (v *JavaTypeVisitor) VisitGenericTypeVariable(generic *tree.JavaTypeGenericTypeVariable, p any) tree.JavaType {
	return generic
}

func (v *JavaTypeVisitor) VisitIntersection(intersection *tree.JavaTypeIntersection, p any) tree.JavaType {
	return intersection
}

func (v *JavaTypeVisitor) VisitMethod(method *tree.JavaTypeMethod, p any) tree.JavaType {
	return method
}

func (v *JavaTypeVisitor) VisitMultiCatch(multiCatch *tree.JavaTypeMultiCatch, p any) tree.JavaType {
	return multiCatch
}

func (v *JavaTypeVisitor) VisitParameterized(parameterized *tree.JavaTypeParameterized, p any) tree.JavaType {
	return parameterized
}

func (v *JavaTypeVisitor) VisitPrimitive(primitive *tree.JavaTypePrimitive, p any) tree.JavaType {
	return primitive
}

func (v *JavaTypeVisitor) VisitUnknown(unknown *tree.JavaTypeUnknown, p any) tree.JavaType {
	return unknown
}

func (v *JavaTypeVisitor) VisitVariable(variable *tree.JavaTypeVariable, p any) tree.JavaType {
	return variable
}

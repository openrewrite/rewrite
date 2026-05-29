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
	"reflect"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func init() {
	// Register JavaType factories for RPC deserialization
	RegisterFactory(java.JavaTypeClassKind, func() any { return &java.JavaTypeClass{} })
	RegisterFactory(java.JavaTypeShallowClassKind, func() any { return &java.JavaTypeShallowClass{} })
	RegisterFactory(java.JavaTypeParameterizedKind, func() any { return &java.JavaTypeParameterized{} })
	RegisterFactory(java.JavaTypeGenericTypeVariableKind, func() any { return &java.JavaTypeGenericTypeVariable{} })
	RegisterFactory(java.JavaTypeArrayKind, func() any { return &java.JavaTypeArray{} })
	RegisterFactory(java.JavaTypePrimitiveKind, func() any { return &java.JavaTypePrimitive{} })
	RegisterFactory(java.JavaTypeMethodKind, func() any { return &java.JavaTypeMethod{} })
	RegisterFactory(java.JavaTypeVariableKind, func() any { return &java.JavaTypeVariable{} })
	RegisterFactory(java.JavaTypeAnnotationKind, func() any { return &java.JavaTypeAnnotation{} })
	RegisterFactory(java.JavaTypeAnnotationSingleElementValueKind, func() any { return &java.JavaTypeAnnotationSingleElementValue{} })
	RegisterFactory(java.JavaTypeAnnotationArrayElementValueKind, func() any { return &java.JavaTypeAnnotationArrayElementValue{} })
	RegisterFactory(java.JavaTypeMultiCatchKind, func() any { return &java.JavaTypeMultiCatch{} })
	RegisterFactory(java.JavaTypeIntersectionKind, func() any { return &java.JavaTypeIntersection{} })
	RegisterFactory(java.JavaTypeUnknownKind, func() any { return java.UnknownType })

	// Register Go types -> Java class name mappings for send queue
	RegisterValueType(reflect.TypeOf(&java.JavaTypeClass{}), java.JavaTypeClassKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeShallowClass{}), java.JavaTypeShallowClassKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeParameterized{}), java.JavaTypeParameterizedKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeGenericTypeVariable{}), java.JavaTypeGenericTypeVariableKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeArray{}), java.JavaTypeArrayKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypePrimitive{}), java.JavaTypePrimitiveKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeMethod{}), java.JavaTypeMethodKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeVariable{}), java.JavaTypeVariableKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeAnnotation{}), java.JavaTypeAnnotationKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeAnnotationSingleElementValue{}), java.JavaTypeAnnotationSingleElementValueKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeAnnotationArrayElementValue{}), java.JavaTypeAnnotationArrayElementValueKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeMultiCatch{}), java.JavaTypeMultiCatchKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeIntersection{}), java.JavaTypeIntersectionKind)
	RegisterValueType(reflect.TypeOf(&java.JavaTypeUnknown{}), java.JavaTypeUnknownKind)
}

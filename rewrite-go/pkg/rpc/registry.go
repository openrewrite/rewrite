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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

func init() {
	// Register JavaType factories for RPC deserialization
	RegisterFactory(tree.JavaTypeClassKind, func() any { return &tree.JavaTypeClass{} })
	RegisterFactory(tree.JavaTypeShallowClassKind, func() any { return &tree.JavaTypeClass{} })
	RegisterFactory(tree.JavaTypeParameterizedKind, func() any { return &tree.JavaTypeParameterized{} })
	RegisterFactory(tree.JavaTypeGenericTypeVariableKind, func() any { return &tree.JavaTypeGenericTypeVariable{} })
	RegisterFactory(tree.JavaTypeArrayKind, func() any { return &tree.JavaTypeArray{} })
	RegisterFactory(tree.JavaTypePrimitiveKind, func() any { return &tree.JavaTypePrimitive{} })
	RegisterFactory(tree.JavaTypeMethodKind, func() any { return &tree.JavaTypeMethod{} })
	RegisterFactory(tree.JavaTypeVariableKind, func() any { return &tree.JavaTypeVariable{} })
	RegisterFactory(tree.JavaTypeAnnotationKind, func() any { return &tree.JavaTypeAnnotation{} })
	RegisterFactory(tree.JavaTypeAnnotationSingleElementValueKind, func() any { return &tree.JavaTypeAnnotationSingleElementValue{} })
	RegisterFactory(tree.JavaTypeAnnotationArrayElementValueKind, func() any { return &tree.JavaTypeAnnotationArrayElementValue{} })
	RegisterFactory(tree.JavaTypeMultiCatchKind, func() any { return &tree.JavaTypeMultiCatch{} })
	RegisterFactory(tree.JavaTypeIntersectionKind, func() any { return &tree.JavaTypeIntersection{} })
	RegisterFactory(tree.JavaTypeUnknownKind, func() any { return tree.UnknownType })

	// Register Go types -> Java class name mappings for send queue
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeClass{}), tree.JavaTypeClassKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeParameterized{}), tree.JavaTypeParameterizedKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeGenericTypeVariable{}), tree.JavaTypeGenericTypeVariableKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeArray{}), tree.JavaTypeArrayKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypePrimitive{}), tree.JavaTypePrimitiveKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeMethod{}), tree.JavaTypeMethodKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeVariable{}), tree.JavaTypeVariableKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeAnnotation{}), tree.JavaTypeAnnotationKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeAnnotationSingleElementValue{}), tree.JavaTypeAnnotationSingleElementValueKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeAnnotationArrayElementValue{}), tree.JavaTypeAnnotationArrayElementValueKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeMultiCatch{}), tree.JavaTypeMultiCatchKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeIntersection{}), tree.JavaTypeIntersectionKind)
	RegisterValueType(reflect.TypeOf(&tree.JavaTypeUnknown{}), tree.JavaTypeUnknownKind)
}

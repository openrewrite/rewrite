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

package tree

import "fmt"

// JavaType is the interface for all Java type representations used for type attribution.
type JavaType interface {
	isJavaType()
}

// FullyQualified is a JavaType with a fully qualified name.
type FullyQualified interface {
	JavaType
	GetFullyQualifiedName() string
}

// Java class name constants for RPC deserialization.
const (
	JavaTypeClassKind              = "org.openrewrite.java.tree.JavaType$Class"
	JavaTypeShallowClassKind       = "org.openrewrite.java.tree.JavaType$ShallowClass"
	JavaTypeParameterizedKind      = "org.openrewrite.java.tree.JavaType$Parameterized"
	JavaTypeGenericTypeVariableKind = "org.openrewrite.java.tree.JavaType$GenericTypeVariable"
	JavaTypeArrayKind              = "org.openrewrite.java.tree.JavaType$Array"
	JavaTypePrimitiveKind          = "org.openrewrite.java.tree.JavaType$Primitive"
	JavaTypeMethodKind             = "org.openrewrite.java.tree.JavaType$Method"
	JavaTypeVariableKind           = "org.openrewrite.java.tree.JavaType$Variable"
	JavaTypeAnnotationKind         = "org.openrewrite.java.tree.JavaType$Annotation"
	JavaTypeMultiCatchKind         = "org.openrewrite.java.tree.JavaType$MultiCatch"
	JavaTypeIntersectionKind       = "org.openrewrite.java.tree.JavaType$Intersection"
	JavaTypeUnknownKind            = "org.openrewrite.java.tree.JavaType$Unknown"
)

// JavaTypePrimitive represents a primitive type like int, boolean, etc.
type JavaTypePrimitive struct {
	Keyword string
}

func (*JavaTypePrimitive) isJavaType() {}

// JavaTypeClass represents a class, interface, enum, or annotation type.
type JavaTypeClass struct {
	FlagsBitMap        int64
	Kind               string
	FullyQualifiedName string
	TypeParameters     []JavaType
	Supertype          *JavaTypeClass
	OwningClass        *JavaTypeClass
	Annotations        []*JavaTypeClass
	Interfaces         []*JavaTypeClass
	Members            []*JavaTypeVariable
	Methods            []*JavaTypeMethod
}

func (*JavaTypeClass) isJavaType() {}

func (c *JavaTypeClass) GetFullyQualifiedName() string {
	return c.FullyQualifiedName
}

// JavaTypeParameterized represents a parameterized type like List<String>.
type JavaTypeParameterized struct {
	Type           *JavaTypeClass
	TypeParameters []JavaType
}

func (*JavaTypeParameterized) isJavaType() {}

func (p *JavaTypeParameterized) GetFullyQualifiedName() string {
	if p.Type != nil {
		return p.Type.FullyQualifiedName
	}
	return ""
}

// JavaTypeGenericTypeVariable represents a generic type variable like T extends Comparable.
type JavaTypeGenericTypeVariable struct {
	Name     string
	Variance string
	Bounds   []JavaType
}

func (*JavaTypeGenericTypeVariable) isJavaType() {}

// JavaTypeArray represents an array type.
type JavaTypeArray struct {
	ElemType    JavaType
	Annotations []*JavaTypeClass
}

func (*JavaTypeArray) isJavaType() {}

// JavaTypeMethod represents a method type signature.
type JavaTypeMethod struct {
	DeclaringType          *JavaTypeClass
	Name                   string
	FlagsBitMap            int64
	ReturnType             JavaType
	ParameterNames         []string
	ParameterTypes         []JavaType
	ThrownExceptions       []JavaType
	Annotations            []*JavaTypeClass
	DefaultValue           []string
	DeclaredFormalTypeNames []string
}

func (*JavaTypeMethod) isJavaType() {}

// JavaTypeVariable represents a variable type.
type JavaTypeVariable struct {
	Name        string
	Owner       JavaType
	Type        JavaType
	Annotations []*JavaTypeClass
}

func (*JavaTypeVariable) isJavaType() {}

// JavaTypeAnnotation represents an annotation type reference.
type JavaTypeAnnotation struct {
	Type *JavaTypeClass
}

func (*JavaTypeAnnotation) isJavaType() {}

// JavaTypeMultiCatch represents a multi-catch type (e.g., IOException | SQLException).
type JavaTypeMultiCatch struct {
	ThrowableTypes []JavaType
}

func (*JavaTypeMultiCatch) isJavaType() {}

// JavaTypeIntersection represents an intersection type (e.g., Serializable & Comparable).
type JavaTypeIntersection struct {
	Bounds []JavaType
}

func (*JavaTypeIntersection) isJavaType() {}

// JavaTypeUnknown represents an unknown or unresolved type.
type JavaTypeUnknown struct{}

func (*JavaTypeUnknown) isJavaType() {}

// UnknownType is the singleton instance of JavaTypeUnknown.
var UnknownType = &JavaTypeUnknown{}

// TypeSignature computes a string signature for a JavaType, used for list identity
// tracking in the RPC protocol. Mirrors DefaultJavaTypeSignatureBuilder in Java.
func TypeSignature(t JavaType) string {
	if t == nil {
		return ""
	}
	switch v := t.(type) {
	case *JavaTypePrimitive:
		return v.Keyword
	case *JavaTypeClass:
		return v.FullyQualifiedName
	case *JavaTypeParameterized:
		sig := ""
		if v.Type != nil {
			sig = v.Type.FullyQualifiedName
		}
		sig += "<"
		for i, tp := range v.TypeParameters {
			if i > 0 {
				sig += ", "
			}
			sig += TypeSignature(tp)
		}
		sig += ">"
		return sig
	case *JavaTypeGenericTypeVariable:
		return v.Name
	case *JavaTypeArray:
		return TypeSignature(v.ElemType) + "[]"
	case *JavaTypeMethod:
		declSig := ""
		if v.DeclaringType != nil {
			declSig = v.DeclaringType.FullyQualifiedName
		}
		return fmt.Sprintf("%s{name=%s,return=%s,parameters=%s}",
			declSig, v.Name, TypeSignature(v.ReturnType), typeListSignature(v.ParameterTypes))
	case *JavaTypeVariable:
		ownerSig := TypeSignature(v.Owner)
		return fmt.Sprintf("%s{name=%s,type=%s}", ownerSig, v.Name, TypeSignature(v.Type))
	case *JavaTypeAnnotation:
		if v.Type != nil {
			return "@" + v.Type.FullyQualifiedName
		}
		return "@"
	case *JavaTypeMultiCatch:
		return typeListSignature(v.ThrowableTypes)
	case *JavaTypeIntersection:
		return typeListSignature(v.Bounds)
	case *JavaTypeUnknown:
		return "*"
	default:
		return ""
	}
}

func typeListSignature(types []JavaType) string {
	sig := "["
	for i, t := range types {
		if i > 0 {
			sig += ", "
		}
		sig += TypeSignature(t)
	}
	sig += "]"
	return sig
}

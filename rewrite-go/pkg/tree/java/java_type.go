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

package java

import "fmt"

type JavaType interface {
	isJavaType()
}

type FullyQualified interface {
	JavaType
	GetFullyQualifiedName() string
}

// Java class name constants for RPC deserialization.
const (
	JavaTypeClassKind                        = "org.openrewrite.java.tree.JavaType$Class"
	JavaTypeShallowClassKind                 = "org.openrewrite.java.tree.JavaType$ShallowClass"
	JavaTypeParameterizedKind                = "org.openrewrite.java.tree.JavaType$Parameterized"
	JavaTypeGenericTypeVariableKind          = "org.openrewrite.java.tree.JavaType$GenericTypeVariable"
	JavaTypeArrayKind                        = "org.openrewrite.java.tree.JavaType$Array"
	JavaTypePrimitiveKind                    = "org.openrewrite.java.tree.JavaType$Primitive"
	JavaTypeMethodKind                       = "org.openrewrite.java.tree.JavaType$Method"
	JavaTypeVariableKind                     = "org.openrewrite.java.tree.JavaType$Variable"
	JavaTypeAnnotationKind                   = "org.openrewrite.java.tree.JavaType$Annotation"
	JavaTypeAnnotationSingleElementValueKind = "org.openrewrite.java.tree.JavaType$Annotation$SingleElementValue"
	JavaTypeAnnotationArrayElementValueKind  = "org.openrewrite.java.tree.JavaType$Annotation$ArrayElementValue"
	JavaTypeMultiCatchKind                   = "org.openrewrite.java.tree.JavaType$MultiCatch"
	JavaTypeIntersectionKind                 = "org.openrewrite.java.tree.JavaType$Intersection"
	JavaTypeUnknownKind                      = "org.openrewrite.java.tree.JavaType$Unknown"
)

type JavaTypePrimitive struct {
	Keyword string
}

func (*JavaTypePrimitive) isJavaType() {}

type JavaTypeClass struct {
	FlagsBitMap        int64
	Kind               string
	FullyQualifiedName string
	TypeParameters     []JavaType
	Supertype          FullyQualified
	OwningClass        FullyQualified
	Annotations        []FullyQualified
	Interfaces         []FullyQualified
	Members            []*JavaTypeVariable
	Methods            []*JavaTypeMethod
}

func (*JavaTypeClass) isJavaType() {}

func (c *JavaTypeClass) GetFullyQualifiedName() string {
	if c == nil {
		return ""
	}
	return c.FullyQualifiedName
}

// JavaTypeShallowClass mirrors Java's JavaType.ShallowClass — a Class
// instance that carries minimal metadata (kind, FQN, owning class) and
// otherwise behaves identically to JavaTypeClass. The wire format
// preserves the distinction; on the Go side the embedded JavaTypeClass
// supplies all fields and accessors.
type JavaTypeShallowClass struct {
	JavaTypeClass
}

func (*JavaTypeShallowClass) isJavaType() {}

type JavaTypeParameterized struct {
	Type           FullyQualified
	TypeParameters []JavaType
}

func (*JavaTypeParameterized) isJavaType() {}

func (p *JavaTypeParameterized) GetFullyQualifiedName() string {
	if p.Type != nil {
		return p.Type.GetFullyQualifiedName()
	}
	return ""
}

type JavaTypeGenericTypeVariable struct {
	Name     string
	Variance string
	Bounds   []JavaType
}

func (*JavaTypeGenericTypeVariable) isJavaType() {}

type JavaTypeArray struct {
	ElemType    JavaType
	Annotations []FullyQualified
}

func (*JavaTypeArray) isJavaType() {}

type JavaTypeMethod struct {
	DeclaringType           FullyQualified
	Name                    string
	FlagsBitMap             int64
	ReturnType              JavaType
	ParameterNames          []string
	ParameterTypes          []JavaType
	ThrownExceptions        []JavaType
	Annotations             []FullyQualified
	DefaultValue            []string
	DeclaredFormalTypeNames []string
}

func (*JavaTypeMethod) isJavaType() {}

type JavaTypeVariable struct {
	Name        string
	Owner       JavaType
	Type        JavaType
	Annotations []FullyQualified
}

func (*JavaTypeVariable) isJavaType() {}

type JavaTypeAnnotation struct {
	Type   FullyQualified
	Values []JavaTypeAnnotationElementValue
}

func (*JavaTypeAnnotation) isJavaType() {}

// One of *JavaTypeAnnotationSingleElementValue or *JavaTypeAnnotationArrayElementValue.
type JavaTypeAnnotationElementValue interface {
	isJavaTypeAnnotationElementValue()
	GetElement() JavaType
}

// JavaTypeAnnotationSingleElementValue is a single annotation element value (one
// constant or one reference). Java parser puts class literals and enum constants
// in ReferenceValue; only String/Number/Boolean/Character live in ConstantValue.
type JavaTypeAnnotationSingleElementValue struct {
	Element        JavaType
	ConstantValue  any
	ReferenceValue JavaType
}

func (*JavaTypeAnnotationSingleElementValue) isJavaTypeAnnotationElementValue() {}

func (s *JavaTypeAnnotationSingleElementValue) GetElement() JavaType { return s.Element }

type JavaTypeAnnotationArrayElementValue struct {
	Element         JavaType
	ConstantValues  []any
	ReferenceValues []JavaType
}

func (*JavaTypeAnnotationArrayElementValue) isJavaTypeAnnotationElementValue() {}

func (a *JavaTypeAnnotationArrayElementValue) GetElement() JavaType { return a.Element }

type JavaTypeMultiCatch struct {
	ThrowableTypes []JavaType
}

func (*JavaTypeMultiCatch) isJavaType() {}

type JavaTypeIntersection struct {
	Bounds []JavaType
}

func (*JavaTypeIntersection) isJavaType() {}

type JavaTypeUnknown struct{}

func (*JavaTypeUnknown) isJavaType() {}

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
	case *JavaTypeShallowClass:
		return v.FullyQualifiedName
	case *JavaTypeClass:
		return v.FullyQualifiedName
	case *JavaTypeParameterized:
		sig := ""
		if v.Type != nil {
			sig = v.Type.GetFullyQualifiedName()
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
			declSig = v.DeclaringType.GetFullyQualifiedName()
		}
		return fmt.Sprintf("%s{name=%s,return=%s,parameters=%s}",
			declSig, v.Name, TypeSignature(v.ReturnType), typeListSignature(v.ParameterTypes))
	case *JavaTypeVariable:
		ownerSig := TypeSignature(v.Owner)
		return fmt.Sprintf("%s{name=%s,type=%s}", ownerSig, v.Name, TypeSignature(v.Type))
	case *JavaTypeAnnotation:
		if v.Type != nil {
			return "@" + v.Type.GetFullyQualifiedName()
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

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

package matcher

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// GetFullyQualifiedName extracts the FQN from a JavaType.
// Returns "" for nil, unknown, or types without a FQN.
func GetFullyQualifiedName(t java.JavaType) string {
	if t == nil {
		return ""
	}
	switch v := t.(type) {
	case *java.JavaTypeClass:
		return v.FullyQualifiedName
	case *java.JavaTypeParameterized:
		if v.Type != nil {
			return v.Type.GetFullyQualifiedName()
		}
	case *java.JavaTypePrimitive:
		return v.Keyword
	case *java.JavaTypeArray:
		return GetFullyQualifiedName(v.ElemType) + "[]"
	case java.FullyQualified:
		return v.GetFullyQualifiedName()
	}
	return ""
}

func IsOfClassType(t java.JavaType, fqn string) bool {
	return GetFullyQualifiedName(t) == fqn
}

// IsAssignableTo checks if the type is assignable to the target FQN.
// For Go, this means the type IS the target, or the type implements
// an interface with that FQN (structural typing).
func IsAssignableTo(t java.JavaType, fqn string) bool {
	if t == nil {
		return false
	}
	return isAssignableToFQN(t, fqn, make(map[java.JavaType]bool))
}

func isAssignableToFQN(t java.JavaType, fqn string, visited map[java.JavaType]bool) bool {
	if visited[t] {
		return false
	}
	visited[t] = true

	if GetFullyQualifiedName(t) == fqn {
		return true
	}

	switch v := t.(type) {
	case *java.JavaTypeClass:
		// Check interfaces
		for _, iface := range v.Interfaces {
			if isAssignableToFQN(iface, fqn, visited) {
				return true
			}
		}
		// Check supertype (not common in Go, but supported by the model)
		if v.Supertype != nil {
			if isAssignableToFQN(v.Supertype, fqn, visited) {
				return true
			}
		}
	case *java.JavaTypeParameterized:
		if v.Type != nil {
			return isAssignableToFQN(v.Type, fqn, visited)
		}
	}
	return false
}

// Implements checks if the type implements the given interface FQN.
// Unlike IsAssignableTo, this returns false if the type IS the interface.
func Implements(t java.JavaType, interfaceFQN string) bool {
	if t == nil {
		return false
	}
	// Must not be the interface itself
	if GetFullyQualifiedName(t) == interfaceFQN {
		return false
	}
	return IsAssignableTo(t, interfaceFQN)
}

func IsError(t java.JavaType) bool {
	return IsOfClassType(t, "error")
}

// Classification is a lookup on the Go type name a basic type is attributed
// under. An untyped constant is its own type (`const c = 1` is an untyped int,
// not an int) and answers the same questions as its typed counterpart.
var (
	goIntegerTypes = map[string]bool{
		"int": true, "int8": true, "int16": true, "int32": true, "int64": true,
		"uint": true, "uint8": true, "uint16": true, "uint32": true, "uint64": true,
		"uintptr": true, "byte": true, "rune": true,
		"untyped int": true, "untyped rune": true,
	}
	goFractionalTypes = map[string]bool{
		"float32": true, "float64": true, "complex64": true, "complex128": true,
		"untyped float": true, "untyped complex": true,
	}
)

func IsString(t java.JavaType) bool {
	switch GetFullyQualifiedName(t) {
	case "string", "untyped string":
		return true
	}
	return false
}

func IsNumeric(t java.JavaType) bool {
	return IsInt(t) || goFractionalTypes[GetFullyQualifiedName(t)]
}

func IsInt(t java.JavaType) bool {
	return goIntegerTypes[GetFullyQualifiedName(t)]
}

func IsBool(t java.JavaType) bool {
	switch GetFullyQualifiedName(t) {
	case "bool", "untyped bool":
		return true
	}
	return false
}

// IsSameGoType reports whether two attributed types are the same Go type.
// `byte` and `uint8` are one type spelled two ways, as are `rune` and `int32`;
// attribution keeps each spelling, so identity compares canonical names.
func IsSameGoType(a, b java.JavaType) bool {
	if a == nil || b == nil {
		return false
	}
	return canonicalGoType(java.TypeSignature(a)) == canonicalGoType(java.TypeSignature(b))
}

func canonicalGoType(signature string) string {
	switch signature {
	case "byte":
		return "uint8"
	case "rune":
		return "int32"
	}
	return signature
}

// AsClass safely casts a JavaType to *JavaTypeClass, returning nil if not a
// class. A JavaTypeShallowClass is unwrapped to its embedded JavaTypeClass
// — callers that need to distinguish ShallowClass from Class should type
// switch on the original JavaType, not on this accessor's result.
func AsClass(t java.JavaType) *java.JavaTypeClass {
	switch v := t.(type) {
	case *java.JavaTypeClass:
		return v
	case *java.JavaTypeShallowClass:
		return &v.JavaTypeClass
	case *java.JavaTypeParameterized:
		if c, ok := v.Type.(*java.JavaTypeClass); ok {
			return c
		}
		if sc, ok := v.Type.(*java.JavaTypeShallowClass); ok {
			return &sc.JavaTypeClass
		}
	}
	return nil
}

// AsMethod safely casts a JavaType to *JavaTypeMethod, returning nil if not a method.
func AsMethod(t java.JavaType) *java.JavaTypeMethod {
	if m, ok := t.(*java.JavaTypeMethod); ok {
		return m
	}
	return nil
}

// AsArray safely casts a JavaType to *JavaTypeArray, returning nil if not an array.
func AsArray(t java.JavaType) *java.JavaTypeArray {
	if a, ok := t.(*java.JavaTypeArray); ok {
		return a
	}
	return nil
}

// TypeOfExpression extracts the JavaType from an expression node.
func TypeOfExpression(expr java.Expression) java.JavaType {
	if expr == nil {
		return nil
	}
	switch n := expr.(type) {
	case *java.Identifier:
		return n.Type
	case *java.Literal:
		return n.Type
	case *java.Binary:
		return n.Type
	case *java.Unary:
		return n.Type
	case *java.FieldAccess:
		return n.Type
	case *golang.TypeAssertion:
		if n.Type != nil {
			return n.Type
		}
		if n.AssertedType != nil {
			return TypeOfExpression(n.AssertedType.Tree.Element)
		}
	case *java.TypeCast:
		if n.Clazz != nil {
			return TypeOfExpression(n.Clazz.Tree.Element)
		}
	case *java.ArrayAccess:
		return n.Type
	case *java.Parentheses:
		return TypeOfExpression(n.Tree.Element)
	case *java.ControlParentheses:
		return TypeOfExpression(n.Tree.Element)
	case *java.MethodInvocation:
		if n.MethodType != nil {
			return n.MethodType.ReturnType
		}
	case *java.Assignment:
		return n.Type
	case *java.AssignmentOperation:
		return n.Type
	}
	return nil
}

// DeclaringTypeFQN extracts the declaring type's FQN from a MethodInvocation.
// For `fmt.Println(...)`, this returns "fmt" (the package path).
// For `t.Sub(...)`, this returns the type of the receiver.
func DeclaringTypeFQN(mi *java.MethodInvocation) string {
	if mi.MethodType != nil && mi.MethodType.DeclaringType != nil {
		return mi.MethodType.DeclaringType.GetFullyQualifiedName()
	}
	// Fallback: infer from Select expression
	if mi.Select != nil {
		if ident, ok := mi.Select.Element.(*java.Identifier); ok {
			// Package-qualified call: fmt.Println -> "fmt"
			return ident.Name
		}
		// Method call on a typed receiver: try to get the type
		t := TypeOfExpression(mi.Select.Element)
		return GetFullyQualifiedName(t)
	}
	return ""
}

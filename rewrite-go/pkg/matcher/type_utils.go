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
		return java.FQNOf(v)
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

func IsString(t java.JavaType) bool {
	if t == nil {
		return false
	}
	if p, ok := t.(*java.JavaTypePrimitive); ok {
		return p.Keyword == "String" || p.Keyword == "string"
	}
	return IsOfClassType(t, "string")
}

func IsNumeric(t java.JavaType) bool {
	if t == nil {
		return false
	}
	if p, ok := t.(*java.JavaTypePrimitive); ok {
		switch p.Keyword {
		case "int", "long", "short", "byte", "float", "double", "char":
			return true
		}
	}
	// Go's unsigned integers have no Java primitive; they map to named types.
	switch GetFullyQualifiedName(t) {
	case "uint", "uint8", "uint16", "uint32", "uint64", "uintptr":
		return true
	}
	return false
}

func IsInt(t java.JavaType) bool {
	if p, ok := t.(*java.JavaTypePrimitive); ok {
		switch p.Keyword {
		case "int", "long", "short", "byte":
			return true
		}
	}
	// Go's unsigned integers have no Java primitive; they map to named types.
	switch GetFullyQualifiedName(t) {
	case "uint", "uint8", "uint16", "uint32", "uint64", "uintptr":
		return true
	}
	return false
}

func IsBool(t java.JavaType) bool {
	if p, ok := t.(*java.JavaTypePrimitive); ok {
		return p.Keyword == "boolean" || p.Keyword == "bool"
	}
	return IsOfClassType(t, "bool")
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
	case *java.ArrayType:
		return n.Type
	case *java.ParameterizedType:
		return n.Type
	case *java.Parentheses:
		return TypeOfExpression(n.Tree.Element)
	case *java.ControlParentheses:
		return TypeOfExpression(n.Tree.Element)
	case *java.MethodInvocation:
		if n.MethodType != nil {
			return n.MethodType.ReturnType
		}
		// A conversion has no callee to carry a signature; its value is of
		// the type being converted to. Name holds that type, down to the
		// `Duration` of `time.Duration(x)` — Select is only the package.
		// A conversion to an unnamed type (`[]byte(s)`) has no Name, and
		// parks its type expression in Select instead.
		if java.FindMarker[golang.Conversion](n.Markers) != nil {
			if n.Name != nil && n.Name.Type != nil {
				return n.Name.Type
			}
			if n.Select != nil {
				return TypeOfExpression(n.Select.Element)
			}
		}
	case *golang.Composite:
		return n.Type
	case *golang.Unary:
		return n.Type
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
//
// Every name comes from the type system, so an unresolved receiver yields "":
// a local variable named `os` cannot read back as the `os` package.
func DeclaringTypeFQN(mi *java.MethodInvocation) string {
	if IsResolved(mi) {
		return mi.MethodType.DeclaringType.GetFullyQualifiedName()
	}
	if mi.Select != nil {
		return GetFullyQualifiedName(TypeOfExpression(mi.Select.Element))
	}
	return ""
}

// IsResolved reports whether the type checker resolved the call to a method of
// a known type. A false here and a non-empty DeclaringTypeFQN can coexist: an
// import whose symbols failed to load still names its package.
func IsResolved(mi *java.MethodInvocation) bool {
	return mi.MethodType != nil && mi.MethodType.DeclaringType != nil &&
		!java.IsUnknown(mi.MethodType.DeclaringType)
}

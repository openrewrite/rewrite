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

import "github.com/openrewrite/rewrite/rewrite-go/pkg/tree"

// GetFullyQualifiedName extracts the FQN from a JavaType.
// Returns "" for nil, unknown, or types without a FQN.
func GetFullyQualifiedName(t tree.JavaType) string {
	if t == nil {
		return ""
	}
	switch v := t.(type) {
	case *tree.JavaTypeClass:
		return v.FullyQualifiedName
	case *tree.JavaTypeParameterized:
		if v.Type != nil {
			return v.Type.FullyQualifiedName
		}
	case *tree.JavaTypePrimitive:
		return v.Keyword
	case *tree.JavaTypeArray:
		return GetFullyQualifiedName(v.ElemType) + "[]"
	case tree.FullyQualified:
		return v.GetFullyQualifiedName()
	}
	return ""
}

// IsOfClassType checks if the type has the exact fully qualified name.
func IsOfClassType(t tree.JavaType, fqn string) bool {
	return GetFullyQualifiedName(t) == fqn
}

// IsAssignableTo checks if the type is assignable to the target FQN.
// For Go, this means the type IS the target, or the type implements
// an interface with that FQN (structural typing).
func IsAssignableTo(t tree.JavaType, fqn string) bool {
	if t == nil {
		return false
	}
	return isAssignableToFQN(t, fqn, make(map[tree.JavaType]bool))
}

func isAssignableToFQN(t tree.JavaType, fqn string, visited map[tree.JavaType]bool) bool {
	if visited[t] {
		return false
	}
	visited[t] = true

	if GetFullyQualifiedName(t) == fqn {
		return true
	}

	switch v := t.(type) {
	case *tree.JavaTypeClass:
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
	case *tree.JavaTypeParameterized:
		if v.Type != nil {
			return isAssignableToFQN(v.Type, fqn, visited)
		}
	}
	return false
}

// Implements checks if the type implements the given interface FQN.
// Unlike IsAssignableTo, this returns false if the type IS the interface.
func Implements(t tree.JavaType, interfaceFQN string) bool {
	if t == nil {
		return false
	}
	// Must not be the interface itself
	if GetFullyQualifiedName(t) == interfaceFQN {
		return false
	}
	return IsAssignableTo(t, interfaceFQN)
}

// IsError checks if the type is the Go built-in `error` interface.
func IsError(t tree.JavaType) bool {
	return IsOfClassType(t, "error")
}

// IsString checks if the type is the Go `string` type.
func IsString(t tree.JavaType) bool {
	if t == nil {
		return false
	}
	if p, ok := t.(*tree.JavaTypePrimitive); ok {
		return p.Keyword == "String" || p.Keyword == "string"
	}
	return IsOfClassType(t, "string")
}

// IsNumeric checks if the type is a numeric type (int, float, etc.).
func IsNumeric(t tree.JavaType) bool {
	if t == nil {
		return false
	}
	if p, ok := t.(*tree.JavaTypePrimitive); ok {
		switch p.Keyword {
		case "int", "long", "short", "byte", "float", "double", "char":
			return true
		}
	}
	fqn := GetFullyQualifiedName(t)
	switch fqn {
	case "int", "int8", "int16", "int32", "int64",
		"uint", "uint8", "uint16", "uint32", "uint64", "uintptr",
		"float32", "float64", "byte", "rune":
		return true
	}
	return false
}

// IsBool checks if the type is the Go `bool` type.
func IsBool(t tree.JavaType) bool {
	if p, ok := t.(*tree.JavaTypePrimitive); ok {
		return p.Keyword == "boolean" || p.Keyword == "bool"
	}
	return IsOfClassType(t, "bool")
}

// AsClass safely casts a JavaType to *JavaTypeClass, returning nil if not a class.
func AsClass(t tree.JavaType) *tree.JavaTypeClass {
	if c, ok := t.(*tree.JavaTypeClass); ok {
		return c
	}
	if p, ok := t.(*tree.JavaTypeParameterized); ok {
		return p.Type
	}
	return nil
}

// AsMethod safely casts a JavaType to *JavaTypeMethod, returning nil if not a method.
func AsMethod(t tree.JavaType) *tree.JavaTypeMethod {
	if m, ok := t.(*tree.JavaTypeMethod); ok {
		return m
	}
	return nil
}

// AsArray safely casts a JavaType to *JavaTypeArray, returning nil if not an array.
func AsArray(t tree.JavaType) *tree.JavaTypeArray {
	if a, ok := t.(*tree.JavaTypeArray); ok {
		return a
	}
	return nil
}

// TypeOfExpression extracts the JavaType from an expression node.
func TypeOfExpression(expr tree.Expression) tree.JavaType {
	if expr == nil {
		return nil
	}
	switch n := expr.(type) {
	case *tree.Identifier:
		return n.Type
	case *tree.Literal:
		return n.Type
	case *tree.Binary:
		return n.Type
	case *tree.Unary:
		return n.Type
	case *tree.FieldAccess:
		return n.Type
	case *tree.TypeCast:
		return n.Type
	case *tree.ArrayAccess:
		return n.Type
	case *tree.Parentheses:
		return n.Type
	case *tree.MethodInvocation:
		if n.MethodType != nil {
			return n.MethodType.ReturnType
		}
	case *tree.Assignment:
		return n.Type
	case *tree.AssignmentOperation:
		return n.Type
	}
	return nil
}

// DeclaringTypeFQN extracts the declaring type's FQN from a MethodInvocation.
// For `fmt.Println(...)`, this returns "fmt" (the package path).
// For `t.Sub(...)`, this returns the type of the receiver.
func DeclaringTypeFQN(mi *tree.MethodInvocation) string {
	if mi.MethodType != nil && mi.MethodType.DeclaringType != nil {
		return mi.MethodType.DeclaringType.FullyQualifiedName
	}
	// Fallback: infer from Select expression
	if mi.Select != nil {
		if ident, ok := mi.Select.Element.(*tree.Identifier); ok {
			// Package-qualified call: fmt.Println -> "fmt"
			return ident.Name
		}
		// Method call on a typed receiver: try to get the type
		t := TypeOfExpression(mi.Select.Element)
		return GetFullyQualifiedName(t)
	}
	return ""
}

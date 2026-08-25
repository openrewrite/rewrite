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

// IsAssignableToType reports whether a value of type from may be used where to
// is wanted. Go satisfies an interface by carrying its methods and declares it
// nowhere, so an interface target is answered from the two method sets.
func IsAssignableToType(from, to java.JavaType) bool {
	if from == nil || to == nil || java.IsUnknown(from) || java.IsUnknown(to) {
		return false
	}
	if IsSameGoType(from, to) {
		return true
	}
	// A literal's keyword names a class of Go types, so it answers to any of
	// them by name — and, carrying no method set, to no interface but the
	// empty one.
	if (isLiteralKeyword(from) || isLiteralKeyword(to)) && SharesGoTypeName(from, to) {
		return true
	}
	// An interface is the only target a Go value reaches without being it,
	// whether by carrying its methods or by a relation a peer model recorded.
	if iface := AsClass(to); iface != nil && iface.Kind == "Interface" {
		fqn := GetFullyQualifiedName(to)
		return hasMethodSet(from, iface.Methods) || (fqn != "" && IsAssignableTo(from, fqn))
	}
	return false
}

// hasMethodSet reports whether from carries every method of an interface. The
// empty one asks for nothing, which is what makes `any` a target every
// attributed type reaches.
func hasMethodSet(from java.JavaType, methods []*java.JavaTypeMethod) bool {
	if len(methods) == 0 {
		return true
	}
	cls := AsClass(from)
	if cls == nil {
		return false
	}
	for _, want := range methods {
		if !hasMethod(cls, want) {
			return false
		}
	}
	return true
}

func hasMethod(cls *java.JavaTypeClass, want *java.JavaTypeMethod) bool {
	for _, have := range cls.Methods {
		if have.Name == want.Name && sameSignature(have, want) {
			return true
		}
	}
	return false
}

func sameSignature(a, b *java.JavaTypeMethod) bool {
	if len(a.ParameterTypes) != len(b.ParameterTypes) {
		return false
	}
	for i := range a.ParameterTypes {
		if !IsSameGoType(a.ParameterTypes[i], b.ParameterTypes[i]) {
			return false
		}
	}
	return IsSameGoType(a.ReturnType, b.ReturnType)
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
	goStringTypes = map[string]bool{"string": true, "untyped string": true}
	goBoolTypes   = map[string]bool{"bool": true, "untyped bool": true}
)

// literalKeywordNames are the Go type names a literal's JavaType.Primitive
// keyword stands for — several each, since Java has fewer keywords than Go has
// types. See doc/recipe-authoring.md: The names types carry.
var literalKeywordNames = map[string][]string{
	"String":  {"string", "untyped string"},
	"boolean": {"bool", "untyped bool"},
	"byte":    {"byte", "int8"},
	"short":   {"int16"},
	"int":     {"int", "int32", "untyped int"},
	"long":    {"int64"},
	"char":    {"rune", "untyped rune"},
	"float":   {"float32"},
	"double":  {"float64", "untyped float"},
}

// GoTypeNames are the Go type names an attributed type answers to: one for a
// type named after it, and the class a literal's keyword stands for.
func GoTypeNames(t java.JavaType) []string {
	if p, ok := t.(*java.JavaTypePrimitive); ok {
		if names, ok := literalKeywordNames[p.Keyword]; ok {
			return names
		}
	}
	return []string{GetFullyQualifiedName(t)}
}

// SharesGoTypeName reports whether two attributed types name a Go type in
// common, a literal `1` sharing one with an `int` and with an `int32`.
func SharesGoTypeName(a, b java.JavaType) bool {
	others := GoTypeNames(b)
	for _, name := range GoTypeNames(a) {
		for _, other := range others {
			if name != "" && name == other {
				return true
			}
		}
	}
	return false
}

func anyGoTypeName(t java.JavaType, in map[string]bool) bool {
	for _, name := range GoTypeNames(t) {
		if in[name] {
			return true
		}
	}
	return false
}

func IsString(t java.JavaType) bool {
	return anyGoTypeName(t, goStringTypes)
}

func IsNumeric(t java.JavaType) bool {
	return IsInt(t) || anyGoTypeName(t, goFractionalTypes)
}

func IsInt(t java.JavaType) bool {
	return anyGoTypeName(t, goIntegerTypes)
}

func IsBool(t java.JavaType) bool {
	return anyGoTypeName(t, goBoolTypes)
}

// IsSameGoType reports whether two attributed types are the same Go type.
// `byte` and `uint8` are one type spelled two ways, as are `rune` and `int32`;
// attribution keeps each spelling, so identity compares canonical names. A
// literal's keyword names a class of Go types rather than one, so it answers
// false — `int` and `int32` are both the `int` keyword.
func IsSameGoType(a, b java.JavaType) bool {
	if a == nil || b == nil || isLiteralKeyword(a) || isLiteralKeyword(b) {
		return false
	}
	return canonicalGoType(java.TypeSignature(a)) == canonicalGoType(java.TypeSignature(b))
}

func isLiteralKeyword(t java.JavaType) bool {
	p, ok := t.(*java.JavaTypePrimitive)
	if !ok {
		return false
	}
	_, named := literalKeywordNames[p.Keyword]
	return named
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
	case *java.ArrayType:
		return n.Type
	case *java.ParameterizedType:
		return n.Type
	case *java.Parentheses:
		return TypeOfExpression(n.Tree.Element)
	case *java.ParenthesizedTypeTree:
		return TypeOfExpression(n.Type.Tree.Element)
	case *java.ControlParentheses:
		return TypeOfExpression(n.Tree.Element)
	// A pointer carries the type it points to; the type mapper draws no
	// distinction between `T` and `*T`.
	case *golang.PointerType:
		return TypeOfExpression(n.Elem)
	case *java.MethodInvocation:
		if n.MethodType != nil {
			return n.MethodType.ReturnType
		}
	case *golang.Composite:
		return n.Type
	// A composite type spelling holds no type slot of its own, so its type is
	// the one its parts spell out — the same shape the type mapper builds for
	// the Go type they name.
	case *golang.MapType:
		return &java.JavaTypeParameterized{
			Type:           &java.JavaTypeClass{FullyQualifiedName: "map", Kind: "Class"},
			TypeParameters: []java.JavaType{TypeOfExpression(n.Key.Element), TypeOfExpression(n.Value)},
		}
	case *golang.Channel:
		return &java.JavaTypeParameterized{
			Type:           &java.JavaTypeClass{FullyQualifiedName: channelName(n.Dir), Kind: "Class"},
			TypeParameters: []java.JavaType{TypeOfExpression(n.Value)},
		}
	case *golang.ArrayType:
		return &java.JavaTypeArray{ElemType: TypeOfExpression(n.ElementType)}
	case *golang.Unary:
		return n.Type
	case *java.Assignment:
		return n.Type
	case *java.AssignmentOperation:
		return n.Type
	}
	return nil
}

func channelName(dir golang.ChanDir) string {
	switch dir {
	case golang.ChanSendOnly:
		return "chan<-"
	case golang.ChanRecvOnly:
		return "<-chan"
	}
	return "chan"
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

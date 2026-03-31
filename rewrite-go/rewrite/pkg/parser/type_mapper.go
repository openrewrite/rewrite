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

package parser

import (
	"go/types"
	"strings"
	"unicode"

	"github.com/openrewrite/rewrite/pkg/tree"
)

// typeMapper converts go/types.Type to OpenRewrite JavaType equivalents.
// It caches results to avoid creating duplicate type objects.
type typeMapper struct {
	cache map[types.Type]tree.JavaType
	// namedCache deduplicates by *types.Named pointer, preventing infinite recursion
	// when types reference each other.
	namedCache map[*types.Named]*tree.JavaTypeClass
}

func newTypeMapper() *typeMapper {
	return &typeMapper{
		cache:      make(map[types.Type]tree.JavaType),
		namedCache: make(map[*types.Named]*tree.JavaTypeClass),
	}
}

// mapType converts a go/types.Type to a tree.JavaType.
func (m *typeMapper) mapType(t types.Type) tree.JavaType {
	if t == nil {
		return nil
	}
	if cached, ok := m.cache[t]; ok {
		return cached
	}

	result := m.doMapType(t)
	if result != nil {
		m.cache[t] = result
	}
	return result
}

func (m *typeMapper) doMapType(t types.Type) tree.JavaType {
	switch v := t.(type) {
	case *types.Basic:
		return m.mapBasic(v)
	case *types.Named:
		return m.mapNamed(v)
	case *types.Pointer:
		// Go pointers are transparent for refactoring — unwrap to pointee type
		return m.mapType(v.Elem())
	case *types.Slice:
		return &tree.JavaTypeArray{ElemType: m.mapType(v.Elem())}
	case *types.Array:
		return &tree.JavaTypeArray{ElemType: m.mapType(v.Elem())}
	case *types.Map:
		return m.mapMapType(v)
	case *types.Chan:
		return m.mapChanType(v)
	case *types.Signature:
		return m.mapSignature(v, "", nil)
	case *types.Interface:
		return m.mapInterface(v, "")
	case *types.Struct:
		return m.mapStruct(v, "")
	case *types.TypeParam:
		return &tree.JavaTypeGenericTypeVariable{
			Name:     v.Obj().Name(),
			Variance: "INVARIANT",
		}
	case *types.Tuple:
		// Tuples are decomposed by callers; should not appear here directly
		return tree.UnknownType
	case *types.Union:
		// Union types from type constraints
		var bounds []tree.JavaType
		for i := 0; i < v.Len(); i++ {
			bounds = append(bounds, m.mapType(v.Term(i).Type()))
		}
		return &tree.JavaTypeIntersection{Bounds: bounds}
	default:
		return tree.UnknownType
	}
}

// mapBasic maps Go basic types to JavaTypePrimitive.
func (m *typeMapper) mapBasic(b *types.Basic) tree.JavaType {
	// Handle aliases by name first (Byte=Uint8, Rune=Int32 share constant values)
	switch b.Name() {
	case "byte":
		return &tree.JavaTypePrimitive{Keyword: "byte"}
	case "rune":
		return &tree.JavaTypePrimitive{Keyword: "char"}
	}

	switch b.Kind() {
	case types.Bool, types.UntypedBool:
		return &tree.JavaTypePrimitive{Keyword: "boolean"}
	case types.Int, types.Int8, types.Int16, types.Int32, types.Int64,
		types.Uint, types.Uint8, types.Uint16, types.Uint32, types.Uint64,
		types.Uintptr, types.UntypedInt:
		return &tree.JavaTypePrimitive{Keyword: "int"}
	case types.Float32, types.Float64, types.UntypedFloat:
		return &tree.JavaTypePrimitive{Keyword: "double"}
	case types.Complex64, types.Complex128, types.UntypedComplex:
		return &tree.JavaTypePrimitive{Keyword: "double"}
	case types.String, types.UntypedString:
		return &tree.JavaTypePrimitive{Keyword: "String"}
	case types.UntypedRune:
		return &tree.JavaTypePrimitive{Keyword: "char"}
	case types.UntypedNil:
		return &tree.JavaTypePrimitive{Keyword: "void"}
	default:
		return tree.UnknownType
	}
}

// mapNamed maps a named type (struct, interface, etc.) to JavaTypeClass.
func (m *typeMapper) mapNamed(named *types.Named) *tree.JavaTypeClass {
	// Check for already-created class (handles circular references)
	if cached, ok := m.namedCache[named]; ok {
		return cached
	}

	obj := named.Obj()
	fqn := fullyQualifiedName(obj)

	cls := &tree.JavaTypeClass{
		FullyQualifiedName: fqn,
		FlagsBitMap:        flagsForObject(obj),
	}
	// Cache early to break cycles
	m.namedCache[named] = cls
	m.cache[named] = cls

	// Map the underlying type to determine kind and populate members/methods
	underlying := named.Underlying()
	switch u := underlying.(type) {
	case *types.Struct:
		cls.Kind = "Class"
		cls.Members = m.structMembers(u)
	case *types.Interface:
		cls.Kind = "Interface"
		cls.Methods = m.interfaceMethods(u)
	default:
		// Named type wrapping a basic type (e.g., type MyInt int)
		cls.Kind = "Class"
	}

	// Map type parameters (generics)
	tparams := named.TypeParams()
	if tparams != nil && tparams.Len() > 0 {
		for i := 0; i < tparams.Len(); i++ {
			cls.TypeParameters = append(cls.TypeParameters, m.mapType(tparams.At(i)))
		}
	}

	// Map methods
	for i := 0; i < named.NumMethods(); i++ {
		method := named.Method(i)
		sig := method.Type().(*types.Signature)
		mt := m.mapSignature(sig, method.Name(), cls)
		cls.Methods = append(cls.Methods, mt)
	}

	// Map implemented interfaces
	for i := 0; i < named.NumMethods(); i++ {
		// Go uses structural interface satisfaction — we don't enumerate interfaces here.
		// Interface relationships would require global analysis.
		break
	}

	return cls
}

// mapSignature maps a function signature to JavaTypeMethod.
func (m *typeMapper) mapSignature(sig *types.Signature, name string, declaringType *tree.JavaTypeClass) *tree.JavaTypeMethod {
	mt := &tree.JavaTypeMethod{
		Name:          name,
		DeclaringType: declaringType,
		FlagsBitMap:   flagsForExported(name),
	}

	// Return type
	results := sig.Results()
	if results.Len() == 0 {
		mt.ReturnType = &tree.JavaTypePrimitive{Keyword: "void"}
	} else if results.Len() == 1 {
		mt.ReturnType = m.mapType(results.At(0).Type())
	} else {
		// Multiple return values — use a tuple-like representation
		// Map as Unknown since Java doesn't have multi-return
		mt.ReturnType = tree.UnknownType
	}

	// Parameters
	params := sig.Params()
	for i := 0; i < params.Len(); i++ {
		p := params.At(i)
		mt.ParameterNames = append(mt.ParameterNames, p.Name())
		mt.ParameterTypes = append(mt.ParameterTypes, m.mapType(p.Type()))
	}

	return mt
}

// mapInterface maps an anonymous interface type.
func (m *typeMapper) mapInterface(iface *types.Interface, fqn string) *tree.JavaTypeClass {
	cls := &tree.JavaTypeClass{
		FullyQualifiedName: fqn,
		Kind:               "Interface",
	}
	cls.Methods = m.interfaceMethods(iface)
	return cls
}

// mapStruct maps an anonymous struct type.
func (m *typeMapper) mapStruct(s *types.Struct, fqn string) *tree.JavaTypeClass {
	cls := &tree.JavaTypeClass{
		FullyQualifiedName: fqn,
		Kind:               "Class",
	}
	cls.Members = m.structMembers(s)
	return cls
}

// mapMapType maps a Go map type to a parameterized class.
func (m *typeMapper) mapMapType(mt *types.Map) tree.JavaType {
	return &tree.JavaTypeParameterized{
		Type: &tree.JavaTypeClass{
			FullyQualifiedName: "map",
			Kind:               "Class",
		},
		TypeParameters: []tree.JavaType{
			m.mapType(mt.Key()),
			m.mapType(mt.Elem()),
		},
	}
}

// mapChanType maps a Go channel type to a parameterized class.
func (m *typeMapper) mapChanType(ch *types.Chan) tree.JavaType {
	var fqn string
	switch ch.Dir() {
	case types.SendRecv:
		fqn = "chan"
	case types.SendOnly:
		fqn = "chan<-"
	case types.RecvOnly:
		fqn = "<-chan"
	}
	return &tree.JavaTypeParameterized{
		Type: &tree.JavaTypeClass{
			FullyQualifiedName: fqn,
			Kind:               "Class",
		},
		TypeParameters: []tree.JavaType{m.mapType(ch.Elem())},
	}
}

// structMembers extracts fields from a struct as JavaTypeVariable.
func (m *typeMapper) structMembers(s *types.Struct) []*tree.JavaTypeVariable {
	var members []*tree.JavaTypeVariable
	for i := 0; i < s.NumFields(); i++ {
		f := s.Field(i)
		members = append(members, &tree.JavaTypeVariable{
			Name:        f.Name(),
			Type:        m.mapType(f.Type()),
			Annotations: nil,
		})
	}
	return members
}

// interfaceMethods extracts methods from an interface as JavaTypeMethod.
func (m *typeMapper) interfaceMethods(iface *types.Interface) []*tree.JavaTypeMethod {
	var methods []*tree.JavaTypeMethod
	for i := 0; i < iface.NumMethods(); i++ {
		method := iface.Method(i)
		sig := method.Type().(*types.Signature)
		mt := m.mapSignature(sig, method.Name(), nil)
		methods = append(methods, mt)
	}
	return methods
}

// fullyQualifiedName returns the fully qualified name for a Go type object.
// Format: "pkgpath.TypeName" (e.g., "fmt.Stringer", "main.MyStruct")
func fullyQualifiedName(obj *types.TypeName) string {
	pkg := obj.Pkg()
	if pkg == nil {
		// Built-in types (error, etc.)
		return obj.Name()
	}
	return pkg.Path() + "." + obj.Name()
}

// flagsForObject returns the Java-compatible flags bitmask for a Go type object.
// Bit 0 (value 1) = Public (exported in Go)
func flagsForObject(obj *types.TypeName) int64 {
	return flagsForExported(obj.Name())
}

// flagsForExported returns Public (1) if the name starts with an uppercase letter.
func flagsForExported(name string) int64 {
	if len(name) > 0 && unicode.IsUpper(rune(name[0])) {
		return 1 // Java Flag.Public
	}
	return 2 // Java Flag.Private
}

// mapObject maps a types.Object (from Defs/Uses) to a JavaType for identifiers.
func (m *typeMapper) mapObject(obj types.Object) tree.JavaType {
	if obj == nil {
		return nil
	}
	return m.mapType(obj.Type())
}

// mapObjectToVariable maps a types.Object to a JavaTypeVariable (for field-like identifiers).
func (m *typeMapper) mapObjectToVariable(obj types.Object) *tree.JavaTypeVariable {
	if obj == nil {
		return nil
	}
	switch o := obj.(type) {
	case *types.Var:
		ownerType := m.ownerType(o)
		return &tree.JavaTypeVariable{
			Name:  o.Name(),
			Owner: ownerType,
			Type:  m.mapType(o.Type()),
		}
	default:
		return nil
	}
}

// ownerType returns the JavaType of the object's owner (declaring type).
func (m *typeMapper) ownerType(v *types.Var) tree.JavaType {
	if !v.IsField() {
		return nil
	}
	// For struct fields, the owner is the parent type.
	// go/types doesn't directly expose the parent; we return nil.
	// The owner will be inferred from context during tree construction.
	return nil
}

// mapMethodObject maps a types.Func to a JavaTypeMethod.
func (m *typeMapper) mapMethodObject(fn *types.Func) *tree.JavaTypeMethod {
	if fn == nil {
		return nil
	}
	sig := fn.Type().(*types.Signature)

	var declaringType *tree.JavaTypeClass
	recv := sig.Recv()
	if recv != nil {
		// Method receiver — the declaring type is the receiver's type
		recvType := recv.Type()
		// Unwrap pointer
		if ptr, ok := recvType.(*types.Pointer); ok {
			recvType = ptr.Elem()
		}
		if named, ok := recvType.(*types.Named); ok {
			declaringType = m.mapNamed(named)
		}
	} else {
		// Package-level function
		pkg := fn.Pkg()
		if pkg != nil {
			declaringType = &tree.JavaTypeClass{
				FullyQualifiedName: pkg.Path(),
				Kind:               "Class",
			}
		}
	}

	return m.mapSignature(sig, fn.Name(), declaringType)
}

// mapSelection maps a types.Selection (field or method selection via ".") to a JavaType.
func (m *typeMapper) mapSelection(sel *types.Selection) tree.JavaType {
	if sel == nil {
		return nil
	}
	return m.mapType(sel.Type())
}

// mapSelectionToMethod maps a method selection to a JavaTypeMethod.
func (m *typeMapper) mapSelectionToMethod(sel *types.Selection) *tree.JavaTypeMethod {
	if sel == nil || sel.Kind() != types.MethodVal {
		return nil
	}
	fn, ok := sel.Obj().(*types.Func)
	if !ok {
		return nil
	}
	return m.mapMethodObject(fn)
}

// isUnresolved returns true if we should not expect to map the given object
// because type-checking did not resolve it (e.g., missing imports).
func isUnresolved(obj types.Object) bool {
	return obj == nil
}

// isBlankIdent returns true if the identifier is "_".
func isBlankIdent(name string) bool {
	return strings.TrimSpace(name) == "_"
}

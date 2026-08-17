/*
 * Copyright 2026 the original author or authors.
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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// basicTypeFQNs pairs every *types.Basic go/types can hand the mapper with the
// name it must be attributed under. The checker resolves `byte` and `rune` to
// their own Basic instances, even for an inferred `for _, c := range s`.
var basicTypeFQNs = []struct {
	typ  *types.Basic
	want string
}{
	{types.Typ[types.Bool], "bool"},
	{types.Typ[types.Int], "int"},
	{types.Typ[types.Int8], "int8"},
	{types.Typ[types.Int16], "int16"},
	{types.Typ[types.Int32], "int32"},
	{types.Typ[types.Int64], "int64"},
	{types.Typ[types.Uint], "uint"},
	{types.Typ[types.Uint8], "uint8"},
	{types.Typ[types.Uint16], "uint16"},
	{types.Typ[types.Uint32], "uint32"},
	{types.Typ[types.Uint64], "uint64"},
	{types.Typ[types.Uintptr], "uintptr"},
	{types.Typ[types.Float32], "float32"},
	{types.Typ[types.Float64], "float64"},
	{types.Typ[types.Complex64], "complex64"},
	{types.Typ[types.Complex128], "complex128"},
	{types.Typ[types.String], "string"},
	{types.Typ[types.UnsafePointer], "unsafe.Pointer"},
	{types.Typ[types.UntypedBool], "untyped bool"},
	{types.Typ[types.UntypedInt], "untyped int"},
	{types.Typ[types.UntypedRune], "untyped rune"},
	{types.Typ[types.UntypedFloat], "untyped float"},
	{types.Typ[types.UntypedComplex], "untyped complex"},
	{types.Typ[types.UntypedString], "untyped string"},
	{types.Typ[types.UntypedNil], "untyped nil"},
	{universeBasic("byte"), "byte"},
	{universeBasic("rune"), "rune"},
}

func universeBasic(name string) *types.Basic {
	return types.Universe.Lookup(name).Type().(*types.Basic)
}

func TestMapBasicNamesEveryTypeAfterItsGoType(t *testing.T) {
	m := newTypeMapper()
	for _, tc := range basicTypeFQNs {
		mapped := m.mapType(tc.typ)
		fq, ok := mapped.(java.FullyQualified)
		require.Truef(t, ok, "%s mapped to %T, want a FullyQualified type", tc.typ, mapped)
		assert.Equalf(t, tc.want, fq.GetFullyQualifiedName(), "attributed name of %s", tc.typ)
	}
}

func TestMapBasicGivesEveryTypeItsOwnName(t *testing.T) {
	m := newTypeMapper()
	seen := map[string]string{}
	for _, tc := range basicTypeFQNs {
		fqn := java.TypeSignature(m.mapType(tc.typ))
		if prior, collides := seen[fqn]; collides {
			t.Errorf("%s and %s both attribute as %q", prior, tc.typ, fqn)
		}
		seen[fqn] = tc.typ.String()
	}
}

// Java's JavaType.Primitive is a closed enum, and JavaTypeReceiver.visitPrimitive
// throws on a keyword outside it, so a basic type may only be attributed as a
// primitive when it names one of these.
func TestMapBasicEmitsNoPrimitiveJavaWouldReject(t *testing.T) {
	javaKeywords := map[string]bool{
		"boolean": true, "byte": true, "char": true, "double": true,
		"float": true, "int": true, "long": true, "short": true,
		"void": true, "String": true, "null": true, "": true,
	}
	m := newTypeMapper()
	for _, tc := range basicTypeFQNs {
		if prim, ok := m.mapType(tc.typ).(*java.JavaTypePrimitive); ok {
			assert.Truef(t, javaKeywords[prim.Keyword],
				"%s attributed as primitive %q, which JavaTypeReceiver.visitPrimitive rejects", tc.typ, prim.Keyword)
		}
	}
}

// A type the checker could not resolve carries no name to attribute it under.
func TestMapBasicLeavesInvalidTypeUnknown(t *testing.T) {
	assert.Equal(t, java.UnknownType, newTypeMapper().mapType(types.Typ[types.Invalid]))
}

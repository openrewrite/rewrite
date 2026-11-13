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

package test

import (
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Declares one variable per Go basic type, named after the type it holds, so a
// walk over the identifiers reads back what each type was attributed as.
const everyBasicType = `package main

import "unsafe"

const (
	untypedBool    = true
	untypedInt     = 42
	untypedRune    = 'a'
	untypedFloat   = 1.5
	untypedComplex = 1i
	untypedString  = "s"
)

func main() {
	var bool_ bool
	var string_ string
	var int_ int
	var int8_ int8
	var int16_ int16
	var int32_ int32
	var int64_ int64
	var uint_ uint
	var uint8_ uint8
	var uint16_ uint16
	var uint32_ uint32
	var uint64_ uint64
	var uintptr_ uintptr
	var float32_ float32
	var float64_ float64
	var complex64_ complex64
	var complex128_ complex128
	var byte_ byte
	var rune_ rune
	var pointer_ unsafe.Pointer
	_, _, _, _, _, _, _ = bool_, string_, int_, int8_, int16_, int32_, int64_
	_, _, _, _, _, _ = uint_, uint8_, uint16_, uint32_, uint64_, uintptr_
	_, _, _, _ = float32_, float64_, complex64_, complex128_
	_, _, _ = byte_, rune_, pointer_
	_, _, _, _, _, _ = untypedBool, untypedInt, untypedRune, untypedFloat, untypedComplex, untypedString
}
`

func TestBasicTypeAttribution(t *testing.T) {
	fqns := basicTypeFQNs(t)

	for name, want := range map[string]string{
		"bool_":       "bool",
		"string_":     "string",
		"int_":        "int",
		"int8_":       "int8",
		"int16_":      "int16",
		"int32_":      "int32",
		"int64_":      "int64",
		"uint_":       "uint",
		"uint8_":      "uint8",
		"uint16_":     "uint16",
		"uint32_":     "uint32",
		"uint64_":     "uint64",
		"uintptr_":    "uintptr",
		"float32_":    "float32",
		"float64_":    "float64",
		"complex64_":  "complex64",
		"complex128_": "complex128",
		"byte_":       "byte",
		"rune_":       "rune",
		"pointer_":    "unsafe.Pointer",

		"untypedBool":    "untyped bool",
		"untypedInt":     "untyped int",
		"untypedRune":    "untyped rune",
		"untypedFloat":   "untyped float",
		"untypedComplex": "untyped complex",
		"untypedString":  "untyped string",
	} {
		got, ok := fqns[name]
		require.Truef(t, ok, "no type attribution for %s", name)
		assert.Equalf(t, want, got, "attributed name of %s", name)
	}
}

// The pairs a recipe reads to decide whether `int(x)` or `byte(x)` converts
// anything.
func TestBasicTypeCollisionsStayDistinct(t *testing.T) {
	fqns := basicTypeFQNs(t)

	for _, pair := range [][2]string{
		{"int_", "int32_"},
		{"int_", "untypedInt"},
		{"byte_", "int8_"},
		{"float64_", "complex128_"},
		{"complex64_", "complex128_"},
		{"rune_", "untypedRune"},
		{"string_", "untypedString"},
		{"bool_", "untypedBool"},
	} {
		left, right := fqns[pair[0]], fqns[pair[1]]
		require.NotEmptyf(t, left, "no type attribution for %s", pair[0])
		require.NotEmptyf(t, right, "no type attribution for %s", pair[1])
		assert.NotEqualf(t, left, right, "%s and %s must not share an attributed type", pair[0], pair[1])
	}
}

// basicTypeFQNs parses everyBasicType and maps each declared name to the name
// of the type it was attributed.
func basicTypeFQNs(t *testing.T) map[string]string {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("test.go", everyBasicType)
	require.NoError(t, err)

	v := visitor.Init(&typeCollector{identTypes: make(map[string]java.JavaType)})
	v.Visit(cu, nil)

	fqns := make(map[string]string, len(v.identTypes))
	for name, typ := range v.identTypes {
		if fq, ok := typ.(java.FullyQualified); ok {
			fqns[name] = fq.GetFullyQualifiedName()
		}
	}
	return fqns
}

// A type alias is transparent, so it resolves to its target type rather than
// regressing to JavaTypeUnknown.
func TestTypeAliasAttribution(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("test.go", `package main

type MyInt = int

func main() {
	b := MyInt(1)
	_ = b
}
`)
	require.NoError(t, err)
	ExpectType(t, cu, "b", "int")
}

// The predeclared `any` alias resolves to the empty interface type rather than
// JavaTypeUnknown.
func TestAnyAttribution(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("test.go", `package main

func main() {
	var a any = 1
	_ = a
}
`)
	require.NoError(t, err)

	v := visitor.Init(&typeCollector{identTypes: make(map[string]java.JavaType)})
	v.Visit(cu, nil)

	typ, ok := v.identTypes["a"]
	require.True(t, ok, "no type attribution for a")
	cls, ok := typ.(*java.JavaTypeClass)
	require.Truef(t, ok, "any: type is %T, want *JavaTypeClass (empty interface)", typ)
	assert.Equalf(t, "Interface", cls.Kind, "any: Kind = %q, want %q", cls.Kind, "Interface")
}

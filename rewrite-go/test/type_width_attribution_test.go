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

// Each Go integer width must be attributed distinctly: widths Java can express
// map to the matching primitive keyword, and unsigned widths (which have no Java
// primitive) map to a synthetic named type keyed by their Go name.
func TestIntegerWidthAttribution(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("test.go", `package main

func main() {
	i := 1
	i8 := int8(1)
	i16 := int16(1)
	i32 := int32(1)
	i64 := int64(1)
	u := uint(1)
	u8 := uint8(1)
	u16 := uint16(1)
	u32 := uint32(1)
	u64 := uint64(1)
	up := uintptr(1)
	f32 := float32(1)
	f64 := float64(1)
	_ = i
	_ = i8
	_ = i16
	_ = i32
	_ = i64
	_ = u
	_ = u8
	_ = u16
	_ = u32
	_ = u64
	_ = up
	_ = f32
	_ = f64
}
`)
	require.NoError(t, err)

	// Signed widths and floats map to distinct primitive keywords.
	ExpectPrimitiveType(t, cu, "i", "int")
	ExpectPrimitiveType(t, cu, "i8", "byte")
	ExpectPrimitiveType(t, cu, "i16", "short")
	ExpectPrimitiveType(t, cu, "i32", "int")
	ExpectPrimitiveType(t, cu, "i64", "long")
	ExpectPrimitiveType(t, cu, "f32", "float")
	ExpectPrimitiveType(t, cu, "f64", "double")

	// Unsigned widths have no Java primitive, so they become named types.
	ExpectType(t, cu, "u", "uint")
	ExpectType(t, cu, "u8", "uint8")
	ExpectType(t, cu, "u16", "uint16")
	ExpectType(t, cu, "u32", "uint32")
	ExpectType(t, cu, "u64", "uint64")
	ExpectType(t, cu, "up", "uintptr")
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
	ExpectPrimitiveType(t, cu, "b", "int")
}

// A value of type unsafe.Pointer has no Java primitive, so it maps to a named
// type rather than JavaTypeUnknown.
func TestUnsafePointerAttribution(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("test.go", `package main

import "unsafe"

func main() {
	var c unsafe.Pointer
	_ = c
}
`)
	require.NoError(t, err)
	ExpectType(t, cu, "c", "unsafe.Pointer")
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

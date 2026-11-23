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

package rpc

import (
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func TestJavaTypeFlagsRoundTrip(t *testing.T) {
	const classFlags = 1 | 1<<4 // Public, Final
	// Bit 20 is not a Java flag. C# sets it on extension methods.
	const methodFlags = 1 | 1<<43 | 1<<20    // Public, Default
	const variableFlags = 1<<1 | 1<<3 | 1<<4 // Private, Static, Final

	cls := &java.JavaTypeClass{FlagsBitMap: classFlags, Kind: "Class", FullyQualifiedName: "example.Foo"}
	cls.Methods = []*java.JavaTypeMethod{{FlagsBitMap: methodFlags, DeclaringType: cls, Name: "Bar",
		ReturnType: &java.JavaTypePrimitive{Keyword: "void"}}}
	cls.Members = []*java.JavaTypeVariable{{FlagsBitMap: variableFlags, Name: "baz", Owner: cls,
		Type: &java.JavaTypePrimitive{Keyword: "int"}}}
	id := uuid.MustParse("12345678-1111-2222-3333-123456789abc")

	got := roundTripNode(t, &java.Identifier{ID: id, Name: "x", Type: cls}, &java.Identifier{ID: id}).(*java.Identifier)

	received, ok := got.Type.(*java.JavaTypeClass)
	require.Truef(t, ok, "Type: got %T, want *JavaTypeClass", got.Type)
	assert.Equal(t, int64(classFlags), received.FlagsBitMap)
	assert.Equal(t, int64(methodFlags), received.Methods[0].FlagsBitMap)
	assert.Equal(t, int64(variableFlags), received.Members[0].FlagsBitMap)
}

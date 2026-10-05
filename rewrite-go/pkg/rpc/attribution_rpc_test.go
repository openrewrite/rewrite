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

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	rwtest "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func roundTripSource(t *testing.T, src string) java.Tree {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("f.go", src)
	require.NoError(t, err, "parse")
	seed := &golang.CompilationUnit{ID: cu.ID}
	return roundTripNode(t, cu, seed).(java.Tree)
}

func collectRoundTripped(t *testing.T, src string) *rwtest.TypedNodes {
	t.Helper()
	return rwtest.CollectTypedNodes(roundTripSource(t, src))
}

func TestCompositeTypeSurvivesRpcRoundTrip(t *testing.T) {
	c := collectRoundTripped(t, "package main\n\nimport \"crypto/tls\"\n\nfunc f() {\n\t_ = []tls.Config{{InsecureSkipVerify: true}}\n}\n")

	require.Len(t, c.Composites, 2)
	assert.Equal(t, "crypto/tls.Config[]", matcher.GetFullyQualifiedName(c.Composites[0].Type))
	assert.Equal(t, "crypto/tls.Config", matcher.GetFullyQualifiedName(c.Composites[1].Type),
		"the elided inner literal has only its own type slot to carry the type")
}

func TestConversionAndBuiltinSurviveRpcRoundTrip(t *testing.T) {
	c := collectRoundTripped(t, "package main\n\nfunc f(b []byte) {\n\t_ = string(b)\n\t_ = len(b)\n}\n")

	require.Len(t, c.Conversions, 1, "conversion lost on round-trip")
	assert.Equal(t, "string", matcher.GetFullyQualifiedName(matcher.TypeOfExpression(c.Conversions[0])))

	var builtins int
	for _, mi := range c.Invocations {
		if java.FindMarker[golang.Builtin](mi.Markers) != nil {
			builtins++
		}
	}
	assert.Equal(t, 1, builtins, "Builtin marker lost on round-trip")
}

func TestBuiltinMethodTypeSurvivesRpcRoundTrip(t *testing.T) {
	c := collectRoundTripped(t, "package main\n\nfunc f(b []byte) {\n\t_ = len(b)\n}\n")

	var lens *java.MethodInvocation
	for _, mi := range c.Invocations {
		if mi.Name != nil && mi.Name.Name == "len" {
			lens = mi
		}
	}
	require.NotNil(t, lens, "len call lost on round-trip")
	require.NotNil(t, lens.MethodType, "len lost its MethodType on round-trip")
	assert.Equal(t, "builtin", matcher.GetFullyQualifiedName(lens.MethodType.DeclaringType))
	assert.Equal(t, "int", matcher.GetFullyQualifiedName(lens.MethodType.ReturnType))
}

func TestAnyTypeSurvivesRpcRoundTrip(t *testing.T) {
	c := collectRoundTripped(t, "package main\n\nfunc f(a any) {\n\t_ = []any{a}\n}\n")

	require.Len(t, c.Composites, 1)
	assert.Equal(t, "any[]", matcher.GetFullyQualifiedName(c.Composites[0].Type))
}

func TestTypeAssertionSurvivesRpcRoundTrip(t *testing.T) {
	c := collectRoundTripped(t, "package main\n\nfunc f(a any) {\n\ts, ok := a.(string)\n\t_, _ = s, ok\n}\n")

	require.Len(t, c.TypeAssertions, 1)
	assert.Equal(t, "string", matcher.GetFullyQualifiedName(c.TypeAssertions[0].Type))
}

func TestResultTypesSurviveRpcRoundTrip(t *testing.T) {
	c := collectRoundTripped(t, "package main\n\ntype Box[T any] struct{ v T }\n\nfunc f(xs []int, n int, p *int, b Box[string]) {\n\t_ = xs[0]\n\t_ = -n\n\t_ = *p\n}\n")

	require.Len(t, c.Unaries, 1)
	assert.Equal(t, "int", matcher.GetFullyQualifiedName(c.Unaries[0].Type))
	require.Len(t, c.GoUnaries, 1)
	assert.Equal(t, "int", matcher.GetFullyQualifiedName(c.GoUnaries[0].Type))
	require.Len(t, c.ParameterizedTypes, 1)
	assert.Equal(t, "main.Box", matcher.GetFullyQualifiedName(c.ParameterizedTypes[0].Type))

	// The J.ArrayAccess codec every RPC peer shares carries indexed and
	// dimension only, so the type an index resolves to is Go-side state.
	// Widening it is a lockstep change across all peers.
	require.Len(t, c.ArrayAccesses, 1)
	assert.Nil(t, c.ArrayAccesses[0].Type)
}

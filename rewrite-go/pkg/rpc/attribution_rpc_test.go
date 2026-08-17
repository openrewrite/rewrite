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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func roundTripSource(t *testing.T, src string) java.Tree {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("f.go", src)
	require.NoError(t, err, "parse")
	seed := &golang.CompilationUnit{ID: cu.ID}
	return roundTripNode(t, cu, seed).(java.Tree)
}

type attributionCollector struct {
	visitor.GoVisitor
	invocations []*java.MethodInvocation
	composites  []*golang.Composite
}

func (v *attributionCollector) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	v.invocations = append(v.invocations, mi)
	return v.GoVisitor.VisitMethodInvocation(mi, p)
}

func (v *attributionCollector) VisitComposite(c *golang.Composite, p any) java.J {
	v.composites = append(v.composites, c)
	return v.GoVisitor.VisitComposite(c, p)
}

func collectRoundTripped(t *testing.T, src string) *attributionCollector {
	t.Helper()
	c := visitor.Init(&attributionCollector{})
	c.Visit(roundTripSource(t, src), nil)
	return c
}

func TestCompositeTypeSurvivesRpcRoundTrip(t *testing.T) {
	c := collectRoundTripped(t, "package main\n\nimport \"crypto/tls\"\n\nfunc f() {\n\t_ = []tls.Config{{InsecureSkipVerify: true}}\n}\n")

	require.Len(t, c.composites, 2)
	assert.Equal(t, "crypto/tls.Config[]", matcher.GetFullyQualifiedName(c.composites[0].Type))
	assert.Equal(t, "crypto/tls.Config", matcher.GetFullyQualifiedName(c.composites[1].Type),
		"the elided inner literal has only its own type slot to carry the type")
}

func TestConversionAndBuiltinMarkersSurviveRpcRoundTrip(t *testing.T) {
	c := collectRoundTripped(t, "package main\n\nfunc f(b []byte) {\n\t_ = string(b)\n\t_ = len(b)\n}\n")

	var conversions, builtins int
	for _, mi := range c.invocations {
		if java.FindMarker[golang.Conversion](mi.Markers) != nil {
			conversions++
		}
		if java.FindMarker[golang.Builtin](mi.Markers) != nil {
			builtins++
		}
	}
	assert.Equal(t, 1, conversions, "Conversion marker lost on round-trip")
	assert.Equal(t, 1, builtins, "Builtin marker lost on round-trip")
}

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

package parser_test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func parseGo(t *testing.T, src string) java.Tree {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("g.go", src)
	require.NoError(t, err, "parse")
	return cu
}

type nodeCollector struct {
	visitor.GoVisitor
	calls       []*java.MethodInvocation
	conversions []*java.TypeCast
}

func (c *nodeCollector) PreVisit(t java.Tree, p any) java.Tree {
	switch n := t.(type) {
	case *java.MethodInvocation:
		c.calls = append(c.calls, n)
	case *java.TypeCast:
		c.conversions = append(c.conversions, n)
	}
	return t
}

func collect(t *testing.T, src string) *nodeCollector {
	t.Helper()
	c := visitor.Init(&nodeCollector{})
	c.Visit(parseGo(t, src), nil)
	return c
}

func (c *nodeCollector) call(t *testing.T, name string) *java.MethodInvocation {
	t.Helper()
	for _, mi := range c.calls {
		if mi.Name != nil && mi.Name.Name == name {
			return mi
		}
	}
	t.Fatalf("no call to %q in the tree", name)
	return nil
}

func (c *nodeCollector) conversion(t *testing.T) *java.TypeCast {
	t.Helper()
	require.Len(t, c.conversions, 1, "expected exactly one conversion in the tree")
	return c.conversions[0]
}

const conversionSource = "package main\n\nimport \"time\"\n\n" +
	"func f(n int64) {\n\t_ = time.Duration(n)\n\t_ = time.Now()\n}\n"

func TestConversionIsNotAMethodInvocation(t *testing.T) {
	c := collect(t, conversionSource)

	// `time.Duration(n)` converts; it invokes no method for a pattern to name.
	assert.Len(t, c.calls, 1)
	assert.Equal(t, "Now", c.calls[0].Name.Name)
	assert.True(t, matcher.NewMethodMatcher("time Now(..)").Matches(c.calls[0]))
	require.Len(t, c.conversions, 1)
}

func TestConversionCarriesTheTypeItNames(t *testing.T) {
	for _, tc := range []struct{ conversion, want string }{
		{"time.Duration(n)", "time.Duration"},
		{"MyInt(3)", "main.MyInt"},
		{"string(b)", "String"},
		{"[]byte(s)", "byte[]"},
	} {
		t.Run(tc.conversion, func(t *testing.T) {
			src := "package main\n\nimport \"time\"\n\ntype MyInt int\n\n" +
				"func f(n int64, b []byte, s string) {\n\t_ = " + tc.conversion + "\n}\n"

			got := matcher.TypeOfExpression(collect(t, src).conversion(t))

			assert.Equal(t, tc.want, matcher.GetFullyQualifiedName(got))
		})
	}
}

func TestConversionToAnUnnamedTypeRoundTrips(t *testing.T) {
	for _, conversion := range []string{
		"[]byte(s)",
		"(*T)(p)",
		"(func())(fn)",
		"(chan int)(ch)",
		"(map[string]int)(m)",
		"[4]byte(b4)",
		"any(s)",
	} {
		t.Run(conversion, func(t *testing.T) {
			src := "package main\n\ntype T struct{}\n\n" +
				"func f(s string, p *T, fn func(), ch chan int, m map[string]int, b4 [4]byte) {\n" +
				"\t_ = " + conversion + "\n}\n"

			cu := parseGo(t, src)

			assert.Equal(t, src, printer.Print(cu))
			require.Len(t, collect(t, src).conversions, 1, "not read as a conversion")
		})
	}
}

func TestConversionSpacingRoundTrips(t *testing.T) {
	for _, conversion := range []string{
		"time.Duration(n)",
		"time.Duration (n)",
		"time.Duration( n )",
		"time.Duration(\n\t\tn,\n\t)",
		"time.Duration( /* c */ n /* d */ )",
		"time . Duration(n)",
	} {
		t.Run(conversion, func(t *testing.T) {
			src := "package main\n\nimport \"time\"\n\nfunc f(n int64) {\n\t_ = " + conversion + "\n}\n"

			assert.Equal(t, src, printer.Print(parseGo(t, src)))
		})
	}
}

func TestCallKeepsItsInvocationShape(t *testing.T) {
	src := "package main\n\nimport \"time\"\n\nfunc f() {\n\t_ = time.Now()\n}\n"

	c := collect(t, src)

	assert.Empty(t, c.conversions)
	require.NotNil(t, c.call(t, "Now").MethodType)
}

func TestConversionInAPrimarySlotRoundTrips(t *testing.T) {
	src := "package main\n\ntype T struct{ n int }\n\n" +
		"func f(v T, b []byte) {\n\t_ = T(v).n\n\t_ = []byte(b)[0]\n\t_ = string(b)[1:2]\n}\n"

	assert.Equal(t, src, printer.Print(parseGo(t, src)))
}

func TestConversionOperandIsTheExpressionSlot(t *testing.T) {
	src := "package main\n\nfunc f(b []byte) {\n\t_ = string(b)\n}\n"

	operand, ok := collect(t, src).conversion(t).Expr.(*java.Identifier)

	require.True(t, ok)
	assert.Equal(t, "b", operand.Name)
}

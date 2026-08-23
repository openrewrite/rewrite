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

package template_test

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func matchesExpr(t *testing.T, pattern, candidate string) bool {
	t.Helper()
	return template.Expression(pattern).Build().Matches(exprOf(t, candidate), nil)
}

func TestParenthesesDoNotChangeWhatAnExpressionIs(t *testing.T) {
	for _, c := range [][2]string{
		{"true", "(true)"},
		{"(true)", "true"},
		{"1", "(1)"},
		{"a + b", "(a + b)"},
		{"((a))", "a"},
		{"f(1)", "f((1))"},
		{"(a) + (b)", "a + b"},
	} {
		require.True(t, matchesExpr(t, c[0], c[1]), "%q against %q", c[0], c[1])
	}
}

// The tree says how an expression groups, so reading through parentheses
// leaves two different groupings different.
func TestParenthesesStillRecordPrecedence(t *testing.T) {
	require.False(t, matchesExpr(t, "(a + b) * c", "a + b*c"))
	require.False(t, matchesExpr(t, "a + b*c", "(a + b) * c"))
}

func TestALiteralIsWhatItEvaluatesTo(t *testing.T) {
	for _, c := range [][2]string{
		{"1", "0x1"},
		{"0x1", "1"},
		{"1", "0b1"},
		{`"x"`, "`x`"},
		{`"x"`, `"\x78"`},
		{"f(1)", "f(0x1)"},
		{"1000", "1_000"},
	} {
		require.True(t, matchesExpr(t, c[0], c[1]), "%q against %q", c[0], c[1])
	}
}

// An integer and a float are different constants, whatever they round to.
func TestALiteralOfAnotherKindIsAnotherLiteral(t *testing.T) {
	require.False(t, matchesExpr(t, "1", "1.0"))
	require.False(t, matchesExpr(t, "1", "2"))
	require.False(t, matchesExpr(t, `"x"`, `"y"`))
}

// A capture takes what it found, so the replacement keeps the parentheses the
// source was written with.
func TestACaptureBindsWhatTheSourceWrote(t *testing.T) {
	x := template.Expr("x")
	pat := template.Expression(fmt.Sprintf("f(%s)", x)).Captures(x).Build()
	match := pat.Match(exprOf(t, "f((a + b))"), nil)
	require.NotNil(t, match)
	_, parenthesized := match.Get("x").(*java.Parentheses)
	require.True(t, parenthesized, "got %T", match.Get("x"))
}

// A capture written twice holds the two to the same expression, which reads
// through parentheses and literal spelling like any other comparison.
func TestARepeatedCaptureComparesSemantically(t *testing.T) {
	x := template.Expr("x")
	pat := template.Expression(fmt.Sprintf("%s + %s", x, x)).Captures(x).Build()
	require.True(t, pat.Matches(exprOf(t, "1 + 0x1"), nil))
	require.True(t, pat.Matches(exprOf(t, "a + (a)"), nil))
	require.False(t, pat.Matches(exprOf(t, "1 + 2"), nil))
}

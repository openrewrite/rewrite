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
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
)

func benchMatch(b *testing.B, pattern string, kind template.ScaffoldKind, candidate fixture) {
	pat := patternFor(fixture{kind: kind, code: pattern})
	node := candidateFor(b, candidate)
	pat.Matches(node, nil) // parse the pattern outside the timed loop
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		pat.Matches(node, nil)
	}
}

func BenchmarkMatchHit(b *testing.B) {
	benchMatch(b, `f(1)`, template.ScaffoldExpression, fixture{name: "call", kind: template.ScaffoldExpression, code: `f(1)`})
}

// A candidate of another kind is rejected on its concrete type, before any
// field is read.
func BenchmarkMatchMiss(b *testing.B) {
	benchMatch(b, `f(1)`, template.ScaffoldExpression, fixture{name: "binary", kind: template.ScaffoldExpression, code: `a + b`})
}

// A visitor method has already narrowed by kind, so most candidates reaching
// a match differ somewhere inside rather than at the root.
func BenchmarkMatchSameKindMiss(b *testing.B) {
	benchMatch(b, `f(1)`, template.ScaffoldExpression, fixture{name: "callOtherName", kind: template.ScaffoldExpression, code: `g(1)`})
}

func BenchmarkMatchSameKindMissDeepArg(b *testing.B) {
	benchMatch(b, `f(a.b.c(1), 2)`, template.ScaffoldExpression, fixture{name: "callNear", kind: template.ScaffoldExpression, code: `f(a.b.c(1), 3)`})
}

func BenchmarkMatchDeep(b *testing.B) {
	body := strings.Repeat("g(1)\n", 20)
	code := "func F() {\n" + body + "}"
	benchMatch(b, code, template.ScaffoldTopLevel, fixture{name: "deep", kind: template.ScaffoldTopLevel, code: code})
}

// A recipe applies a template once per rewritten node, so the context block
// behind it is parsed and type-checked on that path or not at all.
func BenchmarkApplyWithContext(b *testing.B) {
	tmpl := template.ExpressionTemplate("Wrap(w)").
		Context("type Wrapped struct{ V int }", "func Wrap(v Wrapped) Wrapped { return v }", "var w Wrapped").
		Build()
	tmpl.Apply(nil, nil)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		tmpl.Apply(nil, nil)
	}
}

// A template with many placeholders, applied the way a recipe applies one:
// repeatedly, to every node it rewrote.
func BenchmarkApplyWithCaptures(b *testing.B) {
	caps := []*template.Capture{
		template.Expr("a"), template.Expr("b"), template.Expr("c"), template.Expr("d"),
	}
	tmpl := template.ExpressionTemplate(
		fmt.Sprintf("f(%s, g(%s, %s), h(%s))", caps[0], caps[1], caps[2], caps[3])).
		Captures(caps...).Build()
	values := template.NewMatchResult()
	for _, c := range caps {
		values.Bind(c, template.Expression("1").Build().Tree(b))
	}
	tmpl.Apply(nil, values)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		tmpl.Apply(nil, values)
	}
}

// The same comparisons through the walk alone, which is what the numbers in
// PARITY-AUDIT.md compare the hand-written cases against.
func BenchmarkWalkSameKindMiss(b *testing.B) {
	benchWalk(b, `f(1)`, fixture{kind: template.ScaffoldExpression, code: `g(1)`})
}

func BenchmarkWalkSameKindMissDeepArg(b *testing.B) {
	benchWalk(b, `f(a.b.c(1), 2)`, fixture{kind: template.ScaffoldExpression, code: `f(a.b.c(1), 3)`})
}

func benchWalk(b *testing.B, pattern string, candidate fixture) {
	pat := patternFor(fixture{kind: template.ScaffoldExpression, code: pattern})
	node := candidateFor(b, candidate)
	pat.MatchesViaWalk(node, nil)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		pat.MatchesViaWalk(node, nil)
	}
}

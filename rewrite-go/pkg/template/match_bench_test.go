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
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func benchCandidate(b *testing.B, f fixture) java.J {
	b.Helper()
	t := &testing.T{}
	return candidateFor(t, f)
}

func benchMatch(b *testing.B, pattern string, kind template.ScaffoldKind, candidate fixture) {
	pat := patternFor(fixture{kind: kind, code: pattern})
	node := benchCandidate(b, candidate)
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

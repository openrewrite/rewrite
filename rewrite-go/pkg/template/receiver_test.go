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
)

const bufSrc = `package a

import "bytes"

type H struct{ B bytes.Buffer }

func f(one, two bytes.Buffer, h H) {
	one.WriteString("x")
	two.WriteString("x")
	h.B.WriteString("x")
}
`

func TestReceiverCaptureBinds(t *testing.T) {
	for _, mode := range allModes {
		b := template.Expr("b").WithType("bytes.Buffer")
		pat := template.Expression(fmt.Sprintf(`%s.WriteString("x")`, b)).
			Captures(b).Imports("bytes").Context("var one bytes.Buffer").TypeMatching(mode).Build()
		m := pat.Match(firstCall(t, bufSrc), nil)
		require.NotNil(t, m, "mode %v", mode)
		require.NotNil(t, m.Get("b"), "mode %v: receiver capture unbound", mode)
	}
}

// Only a package qualifier is interchangeable with another spelling of itself.
func TestOneReceiverIsNotAnother(t *testing.T) {
	for _, mode := range allModes {
		pat := template.Expression(`one.WriteString("x")`).
			Imports("bytes").Context("var one bytes.Buffer", "var two bytes.Buffer").
			TypeMatching(mode).Build()
		var got []bool
		for _, c := range allCalls(t, bufSrc) {
			got = append(got, pat.Matches(c, nil))
		}
		require.Equal(t, []bool{true, false, false}, got, "mode %v: [one, two, h.B]", mode)
	}
}

var allModes = []template.TypeMatchingMode{
	template.TypeMatchingOff, template.TypeMatchingLenient, template.TypeMatchingStrict,
}

// The distinctness corpus is capture-free, so it compares structure alone.
func TestFastPathAgreesWithWalkOnCaptures(t *testing.T) {
	for _, mode := range allModes {
		for _, tc := range []struct {
			name string
			code func(*template.Capture) string
			caps func() *template.Capture
		}{
			{"receiver", func(c *template.Capture) string { return fmt.Sprintf(`%s.WriteString("x")`, c) },
				func() *template.Capture { return template.Expr("c").WithType("bytes.Buffer") }},
			{"argument", func(c *template.Capture) string { return fmt.Sprintf(`one.WriteString(%s)`, c) },
				func() *template.Capture { return template.Expr("c") }},
			{"wholeCall", func(c *template.Capture) string { return fmt.Sprintf(`%s`, c) },
				func() *template.Capture { return template.Expr("c") }},
		} {
			c := tc.caps()
			pat := template.Expression(tc.code(c)).Captures(c).
				Imports("bytes").Context("var one bytes.Buffer").TypeMatching(mode).Build()
			for i, cand := range allCalls(t, bufSrc) {
				fast, walk := pat.Match(cand, nil), pat.MatchViaWalk(cand, nil)
				require.Equal(t, walk == nil, fast == nil,
					"%s mode %v call %d: fast and walk disagree on matching", tc.name, mode, i)
				if walk != nil {
					require.Equal(t, walk.Bindings(), fast.Bindings(),
						"%s mode %v call %d: fast and walk bound differently", tc.name, mode, i)
				}
			}
		}
	}
}

func TestPackageCallComparesItsTypeArguments(t *testing.T) {
	cand := firstCall(t, `package a

import "slices"

func f(x []string) { _ = slices.Clip[[]string](x) }
`)
	for _, mode := range allModes {
		a := template.Expr("a")
		other := template.Expression(fmt.Sprintf("slices.Clip[[]int](%s)", a)).
			Captures(a).Imports("slices").TypeMatching(mode).Build()
		require.False(t, other.Matches(cand, nil), "mode %v: differing type arguments", mode)

		same := template.Expression(fmt.Sprintf("slices.Clip[[]string](%s)", a)).
			Captures(a).Imports("slices").TypeMatching(mode).Build()
		require.True(t, same.Matches(cand, nil), "mode %v: matching type arguments", mode)
	}
}

// Two variables of one name are told apart by the type they hold.
func TestSameNameDifferentTypeIsNotTheSameVariable(t *testing.T) {
	cand := firstCall(t, `package a

import "bytes"

func f() {
	var v bytes.Reader
	g(v)
}

func g(any) {}
`)
	pat := template.Expression("g(v)").
		Imports("bytes").Context("var v bytes.Buffer", "func g(any) {}").
		TypeMatching(template.TypeMatchingStrict).Build()
	require.False(t, pat.Matches(cand, nil), "a bytes.Reader named v is not a bytes.Buffer named v")
}

// Nothing in the capture-free corpus reaches either variadic implementation.
func TestFastPathAgreesWithWalkOnVariadicRuns(t *testing.T) {
	src := `package a

func f(a, b, c int) {
	g()
	g(a)
	g(a, b)
	g(a, b, c)
}

func g(...int) {}
`
	for _, mode := range allModes {
		for _, bounds := range [][2]int{{0, -1}, {1, -1}, {1, 2}, {2, 2}} {
			args := template.Expr("args").Variadic(bounds[0], bounds[1])
			// The second shape puts a fixed argument ahead of the run, so a
			// candidate shorter than the pattern leaves the run negative.
			for _, code := range []string{"g(%s)", "g(a, %s)"} {
				pat := template.Expression(fmt.Sprintf(code, args)).
					Captures(args).Context("func g(...int) {}", "var a int").
					TypeMatching(mode).Build()
				for i, cand := range allCalls(t, src) {
					fast, walk := pat.Match(cand, nil), pat.MatchViaWalk(cand, nil)
					require.Equal(t, walk == nil, fast == nil,
						"mode %v bounds %v %q call %d: fast and walk disagree", mode, bounds, code, i)
					if walk != nil {
						require.Equal(t, len(walk.GetList("args")), len(fast.GetList("args")),
							"mode %v bounds %v %q call %d: absorbed different runs", mode, bounds, code, i)
					}
				}
			}
		}
	}
}

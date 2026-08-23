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

// A capture in the receiver binds what the receiver was, under every mode.
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

// Two receivers of one type are two receivers. Only a package qualifier is
// interchangeable with another spelling of itself.
func TestOneReceiverIsNotAnother(t *testing.T) {
	for _, mode := range allModes {
		pat := template.Expression(`one.WriteString("x")`).
			Imports("bytes").Context("var one bytes.Buffer", "var two bytes.Buffer").
			TypeMatching(mode).Build()
		var got []bool
		for _, c := range allCalls(t, bufSrc) {
			got = append(got, pat.Matches(c, nil))
		}
		// Strict also refuses `one`: the pattern declares its own, in its own
		// package, and a variable is the one it was declared as.
		want := []bool{mode != template.TypeMatchingStrict, false, false}
		require.Equal(t, want, got, "mode %v: [one, two, h.B]", mode)
	}
}

var allModes = []template.TypeMatchingMode{
	template.TypeMatchingOff, template.TypeMatchingLenient, template.TypeMatchingStrict,
}

// The distinctness corpus is capture-free, so it compares the fast path and
// the walk on structure alone. These carry captures in each position the
// hand-written comparisons read, and compare what was bound as well.
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

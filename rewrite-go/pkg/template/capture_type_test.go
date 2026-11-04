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
	"sync"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// errorArgs names one operand of each kind a capture declared `error` has to
// tell apart — the interface itself, concrete types carrying its method, and
// types carrying neither — beside the source that produces it.
var errorArgs = []struct{ name, expr string }{
	{"err", "err"},
	{"io.EOF", "io.EOF"},
	{"ErrFoo", "ErrFoo"},
	{"string", "s"},
	{"int", "n"},
	{"literal", "1"},
	{"Plain", "Plain{}"},
	{"MyErrValue", "MyErr{}"},
	{"Outer", "Outer{}"},
	{"Wrapped", "Wrapped{}"},
}

const errorFixture = `package a

import "io"

type MyErr struct{}

func (e *MyErr) Error() string { return "x" }

type Plain struct{}

type Inner struct{}

func (i Inner) Error() string { return "y" }

type Outer struct{ Inner }

type Wrapped struct{ error }

var ErrFoo = &MyErr{}

func f(err error, s *string, n int) {
%s}

func take(v any) {}
`

// errorFixtureCalls parses the fixture once for the tests below to read from.
var errorFixtureCalls = sync.OnceValues(func() ([]*java.MethodInvocation, error) {
	var calls strings.Builder
	for _, arg := range errorArgs {
		fmt.Fprintf(&calls, "\ttake(%s)\n", arg.expr)
	}
	cu, err := parser.NewGoParser().Parse("a.go", fmt.Sprintf(errorFixture, calls.String()))
	if err != nil {
		return nil, err
	}
	return nodesOf[*java.MethodInvocation](cu), nil
})

// takeArg returns the call whose argument errorArgs named, so an assertion
// states the operand it means and adding one renumbers nothing.
func takeArg(t testing.TB, name string) java.J {
	t.Helper()
	calls, err := errorFixtureCalls()
	require.NoError(t, err)
	for i, arg := range errorArgs {
		if arg.name == name {
			return calls[i]
		}
	}
	t.Fatalf("errorFixture has no argument named %q", name)
	return nil
}

func takePattern(typeName string) *template.GoPattern {
	return takePatternMode(typeName, template.TypeMatchingOff)
}

func takePatternMode(typeName string, mode template.TypeMatchingMode) *template.GoPattern {
	arg := template.Expr("v").WithType(typeName)
	return template.Expression(fmt.Sprintf("take(%s)", arg)).
		Captures(arg).TypeMatching(mode).Build()
}

// unattributedArg is one candidate under two questions: whether it matches,
// and whether Explain calls the refusal inconclusive.
const unattributedArg = `package a

import "example.invalid/nowhere"

func f() { take(nowhere.Thing) }

func take(v any) {}
`

func TestCaptureTypeIsEnforcedWithoutTypeMatching(t *testing.T) {
	pat := takePattern("error")
	require.True(t, pat.Matches(takeArg(t, "err"), nil))
	require.False(t, pat.Matches(takeArg(t, "string"), nil))
}

// The shape the `prefer_errors_is` recipe family matches.
func TestCaptureTypeConstrainsANilComparison(t *testing.T) {
	e := template.Expr("e").WithType("error")
	pat := template.Expression(fmt.Sprintf("%s == nil", e)).Captures(e).Build()
	binaries := nodesOf[*java.Binary](parseSource(t, `package a

func f(err error, s *string) {
	_ = err == nil
	_ = s == nil
}
`))
	require.Len(t, binaries, 2)
	require.True(t, pat.Matches(binaries[0], nil), "err == nil")
	require.False(t, pat.Matches(binaries[1], nil), "s == nil")
}

func TestCaptureTypeAcceptsAConcreteImplementation(t *testing.T) {
	pat := takePattern("error")
	require.True(t, pat.Matches(takeArg(t, "io.EOF"), nil))
	require.True(t, pat.Matches(takeArg(t, "ErrFoo"), nil), "*MyErr carries Error() string")
	require.False(t, pat.Matches(takeArg(t, "Plain"), nil), "Plain carries no Error()")
}

func TestCaptureTypeAcceptsABasicTypeEitherWayItIsCarried(t *testing.T) {
	pat := takePattern("int")
	require.True(t, pat.Matches(takeArg(t, "int"), nil))
	require.True(t, pat.Matches(takeArg(t, "literal"), nil))
	require.False(t, pat.Matches(takeArg(t, "err"), nil))
}

func TestCaptureTypeFollowsTheModeOnAnUnattributedCandidate(t *testing.T) {
	for _, mode := range allModes {
		require.Equal(t, mode == template.TypeMatchingLenient,
			takePatternMode("error", mode).Matches(firstCall(t, unattributedArg), nil), "mode %v", mode)
	}
}

func TestCaptureTypeReportsAnUnattributedCandidateAsInconclusive(t *testing.T) {
	why := takePattern("error").Explain(firstCall(t, unattributedArg), nil)
	require.False(t, why.Matched)
	require.Equal(t, 1, why.InconclusiveTypes)
}

func TestCaptureTypeOnANonExpressionCapturePanics(t *testing.T) {
	require.Panics(t, func() { template.Stmt("s").WithType("int") })
	require.Panics(t, func() { template.Ident("i").WithType("int") })
	require.Panics(t, func() { template.TypeExpr("t").WithType("int") })
}

func TestCaptureTypeThatDoesNotResolveIsAParseError(t *testing.T) {
	_, err := takePattern("nowhere.Missing").TreeOrError()
	require.ErrorContains(t, err, "nowhere.Missing")
	require.ErrorContains(t, err, "v")
}

func TestCaptureTypeConstrainsEveryElementOfAVariadicRun(t *testing.T) {
	args := template.Expr("args").WithType("error").Variadic(1, -1)
	pat := template.Expression(fmt.Sprintf("join(%s)", args)).Captures(args).Build()
	calls := allCalls(t, `package a

func f(a error, b error, n int) {
	join(a, b)
	join(a, n)
}

func join(v ...any) {}
`)
	require.Len(t, calls, 2)
	require.True(t, pat.Matches(calls[0], nil))
	require.False(t, pat.Matches(calls[1], nil))
}

func TestCaptureTypeConstrainsABarePlaceholderPattern(t *testing.T) {
	c := template.Expr("v").WithType("error")
	pat := template.Expression(c.String()).Captures(c).Build()
	for name, want := range map[string]bool{
		"err": true, "io.EOF": true, "ErrFoo": true, "MyErrValue": true,
		"string": false, "int": false, "literal": false, "Plain": false,
		"Outer": false, "Wrapped": false,
	} {
		arg := takeArg(t, name).(*java.MethodInvocation).Arguments.Elements[0].Element
		require.Equal(t, want, pat.Matches(arg, nil), name)
		require.Equal(t, want, pat.MatchViaWalk(arg, nil) != nil, "%s via walk", name)
	}
}

func TestCaptureTypeComparesTypeArguments(t *testing.T) {
	src := `package a

func f(good map[string]int, bad map[int][]byte, ci chan int, cs chan string) {
	take(good)
	take(bad)
	take(ci)
	take(cs)
}

func take(v any) {}
`
	for declared, want := range map[string][]bool{
		"map[string]int": {true, false, false, false},
		"chan int":       {false, false, true, false},
	} {
		pat := takePattern(declared)
		for i, call := range allCalls(t, src) {
			require.Equal(t, want[i], pat.Matches(call, nil), "%s against call %d", declared, i)
		}
	}
}

func TestCaptureTypeAcceptsEveryAttributedValueAsAny(t *testing.T) {
	pat := takePattern("any")
	for _, arg := range errorArgs {
		require.True(t, pat.Matches(takeArg(t, arg.name), nil), arg.name)
	}
}

// Two limits the attributed model imposes, pinned so a model that grows past
// them fails here. See doc/recipe-authoring.md: Typed captures.
func TestCaptureTypeMissesAMethodPromotedFromAnEmbeddedField(t *testing.T) {
	pat := takePattern("error")
	require.False(t, pat.Matches(takeArg(t, "Outer"), nil), "Outer embeds Inner")
	require.False(t, pat.Matches(takeArg(t, "Wrapped"), nil), "Wrapped embeds error itself")
}

func TestCaptureTypeReadsAPointerAsItsPointee(t *testing.T) {
	require.True(t, takePattern("error").Matches(takeArg(t, "MyErrValue"), nil), "Error() is on *MyErr")
}

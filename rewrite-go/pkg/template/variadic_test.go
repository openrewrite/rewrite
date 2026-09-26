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

package template

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// findCall returns the first call of the named function in src.
func findCall(t *testing.T, src, name string) *java.MethodInvocation {
	t.Helper()
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	require.NoError(t, err)

	var found java.J
	visitor.Init(&callFinder{target: name, found: &found}).Visit(cu, nil)
	require.NotNilf(t, found, "could not find call to %q in:\n%s", name, src)
	return found.(*java.MethodInvocation)
}

// callSource wraps calls in a compilable file so the arity varies but nothing else does.
func callSource(calls ...string) string {
	src := "package main\n\nfunc f(xs ...int) int { return 0 }\n"
	for i, call := range calls {
		src += fmt.Sprintf("\nvar v%d = %s\n", i, call)
	}
	return src
}

func TestMatchVariadicArgumentsOfAnyArity(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	pat := Expression(fmt.Sprintf("f(%s)", args)).Captures(args).Build()

	for _, tc := range []struct {
		call string
		want int
	}{
		{"f()", 0},
		{"f(1)", 1},
		{"f(1, 2, 3)", 3},
	} {
		t.Run(tc.call, func(t *testing.T) {
			call := findCall(t, callSource(tc.call), "f")
			result := pat.Match(call, nil)
			require.NotNilf(t, result, "one pattern should match %s", tc.call)
			assert.Len(t, result.GetList("args"), tc.want)
		})
	}
}

func TestMatchVariadicHonoursMinAndMaxCount(t *testing.T) {
	args := Expr("args").Variadic(1, 2)
	pat := Expression(fmt.Sprintf("f(%s)", args)).Captures(args).Build()

	assert.Nil(t, pat.Match(findCall(t, callSource("f()"), "f"), nil), "0 args is below minCount 1")
	assert.NotNil(t, pat.Match(findCall(t, callSource("f(1)"), "f"), nil))
	assert.NotNil(t, pat.Match(findCall(t, callSource("f(1, 2)"), "f"), nil))
	assert.Nil(t, pat.Match(findCall(t, callSource("f(1, 2, 3)"), "f"), nil), "3 args is above maxCount 2")
}

func TestMatchVariadicBetweenFixedCaptures(t *testing.T) {
	first, rest, last := Expr("first"), Expr("rest").Variadic(0, -1), Expr("last")
	pat := Expression(fmt.Sprintf("f(%s, %s, %s)", first, rest, last)).
		Captures(first, rest, last).
		Build()

	// The one variadic makes the split arithmetic, so the trailing fixed
	// capture binds the last argument rather than being starved by a greedy run.
	result := pat.Match(findCall(t, callSource("f(1, 2, 3, 4)"), "f"), nil)
	require.NotNil(t, result)
	assert.Equal(t, "1", result.Get("first").(*java.Literal).Source)
	assert.Equal(t, "4", result.Get("last").(*java.Literal).Source)
	middle := result.GetList("rest")
	require.Len(t, middle, 2)
	assert.Equal(t, "2", middle[0].(*java.Literal).Source)
	assert.Equal(t, "3", middle[1].(*java.Literal).Source)

	// Two fixed captures need two arguments of their own.
	assert.NotNil(t, pat.Match(findCall(t, callSource("f(1, 2)"), "f"), nil))
	assert.Nil(t, pat.Match(findCall(t, callSource("f(1)"), "f"), nil))
}

func TestMatchRejectsTwoVariadicsInOneList(t *testing.T) {
	a, b := Expr("a").Variadic(0, -1), Expr("b").Variadic(0, -1)
	pat := Expression(fmt.Sprintf("f(%s, %s)", a, b)).Captures(a, b).Build()

	assert.Nil(t, pat.Match(findCall(t, callSource("f(1, 2)"), "f"), nil),
		"a list splits at one variadic; two leave the split undetermined")
}

func TestMatchRepeatedVariadicCaptureNeedsEqualRuns(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	pat := Expression(fmt.Sprintf("f(%s) + f(%s)", args, args)).Captures(args).Build()

	src := func(expr string) string {
		return "package main\n\nfunc f(xs ...int) int { return 0 }\n\nvar v = " + expr + "\n"
	}
	p := parser.NewGoParser()
	match := func(expr string) *MatchResult {
		cu, err := p.Parse("test.go", src(expr))
		require.NoError(t, err)
		var found java.J
		visitor.Init(&binaryFinder{found: &found}).Visit(cu, nil)
		require.NotNil(t, found)
		return pat.Match(found, nil)
	}

	assert.NotNil(t, match("f(1, 2) + f(1, 2)"))
	assert.Nil(t, match("f(1, 2) + f(1)"), "a repeated capture binds one run, not two")
	assert.Nil(t, match("f(1, 2) + f(1, 3)"))
}

func TestApplyFailsWhenABoundNodeCannotSitInTheList(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	tmpl := ExpressionTemplate(fmt.Sprintf("f(%s)", args)).Captures(args).Build()

	// A return statement is no expression, so it cannot become an argument.
	values := NewMatchResult().BindList(args, []java.J{&java.Return{}})
	assert.Nil(t, tmpl.Apply(nil, values))
}

func TestRewriteExpandsVariadicArguments(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	before := Expression(fmt.Sprintf("before(%s)", args)).Captures(args).Build()
	after := ExpressionTemplate(fmt.Sprintf("after(%s)", args)).Captures(args).Build()

	r := &rewriteRecipeWithVisitor{visitor: Rewrite(before, after)}
	test.NewRecipeSpec().WithRecipe(r).RewriteRun(t,
		test.Golang(`
			package main

			func before(xs ...int) int { return 0 }
			func after(xs ...int) int  { return 0 }

			var a = before()
			var b = before(1)
			var c = before(1, 2, 3)
		`, `
			package main

			func before(xs ...int) int { return 0 }
			func after(xs ...int) int  { return 0 }

			var a = after()
			var b = after(1)
			var c = after(1, 2, 3)
		`),
	)
}

func TestRewriteMixesVariadicWithFixedCaptures(t *testing.T) {
	recv, args := Expr("recv"), Expr("args").Variadic(0, -1)
	before := Expression(fmt.Sprintf("before(%s, %s)", recv, args)).Captures(recv, args).Build()
	after := ExpressionTemplate(fmt.Sprintf("after(%s, %s)", args, recv)).Captures(recv, args).Build()

	r := &rewriteRecipeWithVisitor{visitor: Rewrite(before, after)}
	test.NewRecipeSpec().WithRecipe(r).RewriteRun(t,
		test.Golang(`
			package main

			func before(xs ...int) int { return 0 }
			func after(xs ...int) int  { return 0 }

			var a = before(1)
			var b = before(1, 2, 3)
		`, `
			package main

			func before(xs ...int) int { return 0 }
			func after(xs ...int) int  { return 0 }

			var a = after(1)
			var b = after(2, 3, 1)
		`),
	)
}

func TestRewriteExpandsVariadicStatements(t *testing.T) {
	body := Stmt("body").Variadic(0, -1)
	before := StatementPattern(fmt.Sprintf("if ok {\n%s\n}", body)).Captures(body).Build()
	after := StatementTemplate(fmt.Sprintf("if !ok {\n%s\n}", body)).Captures(body).Build()

	r := &rewriteRecipeWithVisitor{visitor: Rewrite(before, after)}
	test.NewRecipeSpec().WithRecipe(r).RewriteRun(t,
		test.Golang(`
			package main

			func f(ok bool) {
				if ok {
					g()
					g()
				}
			}

			func g() {}
		`, `
			package main

			func f(ok bool) {
				if !ok {
					g()
					g()
				}
			}

			func g() {}
		`),
	)
}

func TestInstantiateWithHandBoundList(t *testing.T) {
	recv, args := Expr("recv"), Expr("args").Variadic(0, -1)
	tmpl := ExpressionTemplate(fmt.Sprintf("assert.Equal(%s, %s)", recv, args)).
		Captures(recv, args).
		Build()

	call := tmpl.Instantiate(NewMatchResult().
		Bind(recv, &java.Identifier{Name: "t"}).
		BindList(args, []java.J{
			&java.Literal{Source: "want"},
			&java.Literal{Source: "got"},
		}))

	require.NotNil(t, call)
	assert.Equal(t, "assert.Equal(t, want, got)", printer.Print(call))
}

func TestInstantiateWithEmptyList(t *testing.T) {
	recv, args := Expr("recv"), Expr("args").Variadic(0, -1)
	tmpl := ExpressionTemplate(fmt.Sprintf("assert.True(%s, %s)", recv, args)).
		Captures(recv, args).
		Build()

	call := tmpl.Instantiate(NewMatchResult().
		Bind(recv, &java.Identifier{Name: "t"}).
		BindList(args, nil))

	require.NotNil(t, call)
	assert.Equal(t, "assert.True(t)", printer.Print(call))
}

func TestInstantiateHonoursBoundsOnAHandBoundRun(t *testing.T) {
	args := Expr("args").Variadic(1, 2)
	tmpl := ExpressionTemplate(fmt.Sprintf("f(%s)", args)).Captures(args).Build()

	run := func(n int) []java.J {
		values := make([]java.J, n)
		for i := range values {
			values[i] = &java.Literal{Source: "1"}
		}
		return values
	}

	assert.Nil(t, tmpl.Instantiate(NewMatchResult().BindList(args, run(0))), "below minCount 1")
	assert.NotNil(t, tmpl.Instantiate(NewMatchResult().BindList(args, run(1))))
	assert.NotNil(t, tmpl.Instantiate(NewMatchResult().BindList(args, run(2))))
	assert.Nil(t, tmpl.Instantiate(NewMatchResult().BindList(args, run(3))), "above maxCount 2")
}

func TestElemsConcatenatesElementSlices(t *testing.T) {
	recv := &java.Identifier{Name: "t"}
	coreArgs := []java.Expression{&java.Identifier{Name: "want"}, &java.Identifier{Name: "got"}}
	msgArgs := []java.Expression{&java.Literal{Source: `"boom"`}}

	args := Expr("args").Variadic(0, -1)
	tmpl := ExpressionTemplate(fmt.Sprintf("assert.Equal(%s)", args)).Captures(args).Build()

	call := tmpl.Instantiate(NewMatchResult().
		BindList(args, Elems([]java.Expression{recv}, coreArgs, msgArgs)))

	require.NotNil(t, call)
	assert.Equal(t, `assert.Equal(t, want, got, "boom")`, printer.Print(call))
}

func TestElemsWidensStatements(t *testing.T) {
	stmts := []java.Statement{&java.Return{}, &java.Return{}}
	assert.Len(t, Elems(stmts), 2)
	assert.Empty(t, Elems[java.Expression]())
}

func TestInstantiateRefusesUnboundVariadicCapture(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	tmpl := ExpressionTemplate(fmt.Sprintf("errors.New(%s)", args)).
		Captures(args).
		Imports("errors").
		Build()

	assert.Nil(t, tmpl.Instantiate(NewMatchResult()))
}

func TestInstantiateSplicedListNodesGetFreshIDs(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	tmpl := ExpressionTemplate(fmt.Sprintf("f(%s)", args)).Captures(args).Build()

	lit := &java.Literal{Source: "1"}
	call := tmpl.Instantiate(NewMatchResult().BindList(args, []java.J{lit, lit}))

	require.NotNil(t, call)
	mi, ok := call.(*java.MethodInvocation)
	require.Truef(t, ok, "expected *java.MethodInvocation, got %T", call)
	require.Len(t, mi.Arguments.Elements, 2)
	first, second := mi.Arguments.Elements[0].Element.GetID(), mi.Arguments.Elements[1].Element.GetID()
	assert.NotEqual(t, lit.ID, first, "the bound node stays usable where it came from")
	assert.NotEqual(t, first, second, "one node bound twice must not yield two of the same ID")
}

func TestApplyFailsForVariadicOutsideListPosition(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	tmpl := ExpressionTemplate(fmt.Sprintf("%s + 1", args)).Captures(args).Build()

	values := NewMatchResult().BindList(args, []java.J{&java.Literal{Source: "2"}})
	assert.Nil(t, tmpl.Apply(nil, values),
		"a list has nowhere to expand outside a list position")
}

func TestApplyFailsRatherThanEmitAPlaceholder(t *testing.T) {
	bound, unbound := Expr("bound"), Expr("unbound")
	tmpl := ExpressionTemplate(fmt.Sprintf("f(%s, %s)", bound, unbound)).
		Captures(bound, unbound).
		Build()

	values := NewMatchResult().Bind(bound, &java.Literal{Source: "1"})
	assert.Nil(t, tmpl.Apply(nil, values), "an unbound capture has no substitute to emit")
}

func TestVariadicRejectsInvalidBounds(t *testing.T) {
	assert.Panics(t, func() { Expr("x").Variadic(-1, 3) })
	assert.Panics(t, func() { Expr("x").Variadic(3, 2) })
	assert.NotPanics(t, func() { Expr("x").Variadic(3, -1) })
}

type callFinder struct {
	visitor.GoVisitor
	target string
	found  *java.J
}

func (v *callFinder) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	if *v.found == nil && mi.Name != nil && mi.Name.Name == v.target {
		*v.found = mi
	}
	return v.GoVisitor.VisitMethodInvocation(mi, p)
}

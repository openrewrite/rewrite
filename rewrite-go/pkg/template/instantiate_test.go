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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// findLiteral returns the first literal in src whose Source matches want.
func findLiteral(t *testing.T, src, want string) *java.Literal {
	t.Helper()
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	require.NoError(t, err)

	var found java.J
	visitor.Init(&literalFinder{target: want, found: &found}).Visit(cu, nil)
	require.NotNilf(t, found, "could not find literal %s in:\n%s", want, src)
	return found.(*java.Literal)
}

// findIdentifier returns the last identifier in src named want, which in these
// test sources is the use site rather than the declaration.
func findIdentifier(t *testing.T, src, want string) *java.Identifier {
	t.Helper()
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	require.NoError(t, err)

	var found java.J
	visitor.Init(&lastIdentFinder{target: want, found: &found}).Visit(cu, nil)
	require.NotNilf(t, found, "could not find identifier %q in:\n%s", want, src)
	return found.(*java.Identifier)
}

func TestInstantiateTopLevelWithSplicedSubtree(t *testing.T) {
	msgLit := findLiteral(t, `package main

import "errors"

func f() error {
	return errors.New("not found")
}
`, `"not found"`)
	require.NotNil(t, msgLit.Type, "precondition: parsed literal should carry a type")

	name := Ident("name")
	msg := Expr("msg").WithType("string")
	tmpl := TopLevelTemplate(fmt.Sprintf("var %s = errors.New(%s)", name, msg)).
		Captures(name, msg).
		Imports("errors").
		Build()

	decl := tmpl.Instantiate(NewMatchResult().
		Bind(name, &java.Identifier{Name: "ErrNotFound"}).
		Bind(msg, msgLit))

	require.NotNil(t, decl, "Instantiate should produce a detached node")
	assert.Equal(t, `var ErrNotFound = errors.New("not found")`, printer.Print(decl))

	vd, ok := decl.(*java.VariableDeclarations)
	require.Truef(t, ok, "expected *java.VariableDeclarations, got %T", decl)

	mi, ok := vd.Variables[0].Element.Initializer.Element.(*java.MethodInvocation)
	require.Truef(t, ok, "expected *java.MethodInvocation initializer, got %T", vd.Variables[0].Element.Initializer.Element)

	// The template's own call is attributed by go/types, not by hand.
	require.NotNil(t, mi.MethodType, "errors.New should be attributed")
	assert.Equal(t, "New", mi.MethodType.Name)
	require.NotNil(t, mi.MethodType.DeclaringType)
	assert.Equal(t, "errors", mi.MethodType.DeclaringType.GetFullyQualifiedName())

	spliced, ok := mi.Arguments.Elements[0].Element.(*java.Literal)
	require.Truef(t, ok, "expected spliced *java.Literal, got %T", mi.Arguments.Elements[0].Element)
	assert.Equal(t, msgLit.Source, spliced.Source)
	assert.Same(t, msgLit.Type, spliced.Type, "spliced node should keep its type attribution")
	assert.NotEqual(t, msgLit.ID, spliced.ID, "the bound node stays usable where it came from")
}

func TestInstantiateSplicedIdentifierKeepsType(t *testing.T) {
	errIdent := findIdentifier(t, `package main

import "errors"

func f() error {
	err := errors.New("boom")
	return err
}
`, "err")
	require.NotNil(t, errIdent.Type, "precondition: parsed identifier should carry a type")

	errCap := Expr("err").WithType("error")
	tmpl := ExpressionTemplate(fmt.Sprintf(`fmt.Errorf("ctx: %%w", %s)`, errCap)).
		Captures(errCap).
		Imports("fmt").
		Build()

	expr := tmpl.Instantiate(NewMatchResult().Bind(errCap, errIdent))
	require.NotNil(t, expr)
	assert.Equal(t, `fmt.Errorf("ctx: %w", err)`, printer.Print(expr))

	mi, ok := expr.(*java.MethodInvocation)
	require.Truef(t, ok, "expected *java.MethodInvocation, got %T", expr)
	require.NotNil(t, mi.MethodType, "fmt.Errorf should be attributed")

	spliced, ok := mi.Arguments.Elements[1].Element.(*java.Identifier)
	require.Truef(t, ok, "expected spliced *java.Identifier, got %T", mi.Arguments.Elements[1].Element)
	assert.Same(t, errIdent.Type, spliced.Type, "spliced identifier should keep its type attribution")
	assert.NotEqual(t, errIdent.ID, spliced.ID, "the bound node stays usable where it came from")
}

func TestInstantiateReusesTemplateWithFreshIDs(t *testing.T) {
	name := Ident("name")
	tmpl := TopLevelTemplate(fmt.Sprintf(`var %s = errors.New("x")`, name)).
		Captures(name).
		Imports("errors").
		Build()

	first := tmpl.Instantiate(NewMatchResult().Bind(name, &java.Identifier{Name: "ErrOne"}))
	second := tmpl.Instantiate(NewMatchResult().Bind(name, &java.Identifier{Name: "ErrTwo"}))

	require.NotNil(t, first)
	require.NotNil(t, second)
	assert.Equal(t, `var ErrOne = errors.New("x")`, printer.Print(first))
	assert.Equal(t, `var ErrTwo = errors.New("x")`, printer.Print(second))
	assert.NotEqual(t, first.(*java.VariableDeclarations).ID, second.(*java.VariableDeclarations).ID,
		"instantiations of one template must not share node IDs")
}

func TestInstantiateWithoutCaptures(t *testing.T) {
	tmpl := TopLevelTemplate(`var ErrNotFound = errors.New("not found")`).Imports("errors").Build()

	decl := tmpl.Instantiate(nil)
	require.NotNil(t, decl)
	assert.Equal(t, `var ErrNotFound = errors.New("not found")`, printer.Print(decl))
}

func TestInstantiateRejectsUnboundCapture(t *testing.T) {
	name := Ident("name")
	msg := Expr("msg").WithType("string")
	tmpl := TopLevelTemplate(fmt.Sprintf("var %s = errors.New(%s)", name, msg)).
		Captures(name, msg).
		Imports("errors").
		Build()

	assert.Nil(t, tmpl.Instantiate(NewMatchResult().Bind(name, &java.Identifier{Name: "ErrNotFound"})))
	assert.Nil(t, tmpl.Instantiate(nil))
	// A nil binding satisfies Has, so the guard has to read the value itself.
	assert.Nil(t, tmpl.Instantiate(NewMatchResult().
		Bind(name, &java.Identifier{Name: "ErrNotFound"}).
		Bind(msg, nil)))
}

func TestInstantiateStripsScaffoldPrefixAcrossNodeKinds(t *testing.T) {
	for _, code := range []string{
		"&Foo{}", "*p", "<-ch", "a &^ b", "func() {}", "[]int{1}",
		"x + 1", "f(1)", "m[k]", "x.(T)", "map[string]int{}", "-n", "!ok",
	} {
		t.Run(code, func(t *testing.T) {
			out := ExpressionTemplate(code).Build().Instantiate(nil)
			require.NotNil(t, out)
			assert.Equalf(t, java.EmptySpace, out.GetPrefix(),
				"%T carries a scaffold prefix; setPrefix needs a case for it", out)
		})
	}
}

func TestInstantiateLeavesBoundSubtreeUsable(t *testing.T) {
	msgLit := findLiteral(t, "package main\n\nvar m = \"reused\"\n", `"reused"`)

	msg := Expr("msg").WithType("string")
	tmpl := TopLevelTemplate(fmt.Sprintf("var E = errors.New(%s)", msg)).
		Captures(msg).
		Imports("errors").
		Build()

	first := tmpl.Instantiate(NewMatchResult().Bind(msg, msgLit))
	second := tmpl.Instantiate(NewMatchResult().Bind(msg, msgLit))
	require.NotNil(t, first)
	require.NotNil(t, second)

	firstArg := literalArgOf(t, first)
	secondArg := literalArgOf(t, second)
	assert.NotEqual(t, msgLit.ID, firstArg.ID)
	assert.NotEqual(t, firstArg.ID, secondArg.ID,
		"two instantiations sharing a binding must not share node IDs")
}

func literalArgOf(t *testing.T, decl java.J) *java.Literal {
	t.Helper()
	vd, ok := decl.(*java.VariableDeclarations)
	require.Truef(t, ok, "expected *java.VariableDeclarations, got %T", decl)
	mi, ok := vd.Variables[0].Element.Initializer.Element.(*java.MethodInvocation)
	require.Truef(t, ok, "expected *java.MethodInvocation, got %T", vd.Variables[0].Element.Initializer.Element)
	lit, ok := mi.Arguments.Elements[0].Element.(*java.Literal)
	require.Truef(t, ok, "expected *java.Literal argument, got %T", mi.Arguments.Elements[0].Element)
	return lit
}

func TestBindIsChainableAndReadable(t *testing.T) {
	a, b := Expr("a"), Expr("b")
	lit := &java.Literal{Source: "1"}
	ident := &java.Identifier{Name: "x"}

	m := NewMatchResult().Bind(a, lit).Bind(b, ident)

	assert.Same(t, lit, m.GetCapture(a))
	assert.Same(t, ident, m.GetCapture(b))
	assert.True(t, m.Has("a"))
	assert.False(t, m.Has("c"))
}

type literalFinder struct {
	visitor.GoVisitor
	target string
	found  *java.J
}

func (v *literalFinder) VisitLiteral(lit *java.Literal, p any) java.J {
	if *v.found == nil && lit.Source == v.target {
		*v.found = lit
	}
	return v.GoVisitor.VisitLiteral(lit, p)
}

type lastIdentFinder struct {
	visitor.GoVisitor
	target string
	found  *java.J
}

func (v *lastIdentFinder) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if ident.Name == v.target {
		*v.found = ident
	}
	return v.GoVisitor.VisitIdentifier(ident, p)
}

func TestInstantiateTreatsATypedNilBindingAsUnbound(t *testing.T) {
	cap := Expr("v")
	tmpl := ExpressionTemplate(fmt.Sprintf(`fmt.Sprintf("%%s", %s)`, cap)).
		Captures(cap).Imports("fmt").Build()

	m := NewMatchResult().Bind(cap, (*java.Literal)(nil))

	assert.Nil(t, tmpl.Instantiate(m))
}

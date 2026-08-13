/*
 * Copyright 2025 the original author or authors.
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

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func TestScaffoldExpression(t *testing.T) {
	source, _ := buildScaffold("1 + 2", nil, nil, ScaffoldExpression)
	require.NotEqual(t, "", source)

	p := parser.NewGoParser()
	_, err := p.Parse("test.go", source)
	if err != nil {
		t.Fatalf("scaffold should parse: %v\nsource:\n%s", err, source)
	}
}

func TestScaffoldStatement(t *testing.T) {
	source, _ := buildScaffold("x = 1", nil, nil, ScaffoldStatement)
	require.NotEqual(t, "", source)

	p := parser.NewGoParser()
	_, err := p.Parse("test.go", source)
	if err != nil {
		t.Fatalf("scaffold should parse: %v\nsource:\n%s", err, source)
	}
}

func TestScaffoldWithCaptures(t *testing.T) {
	caps := captureMap([]*Capture{Expr("x")})
	source, count := buildScaffold(fmt.Sprintf("%s + 1", Expr("x")), caps, nil, ScaffoldExpression)
	assert.Equal(t, 1, count)

	p := parser.NewGoParser()
	_, err := p.Parse("test.go", source)
	if err != nil {
		t.Fatalf("scaffold with captures should parse: %v\nsource:\n%s", err, source)
	}
}

func TestParseScaffoldExpression(t *testing.T) {
	node, err := parseScaffold("1 + 2", nil, nil, ScaffoldExpression)
	require.NoError(t, err)
	require.NotNil(t, node)
	bin, ok := node.(*java.Binary)
	if !ok {
		t.Fatalf("expected *java.Binary, got %T", node)
	}
	require.False(t, bin.Left == nil || bin.Right == nil)
}

func TestPatternMatchIdentifier(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", "package main\n\nvar y = x\n")
	require.NoError(t, err)

	pat := Expression("x").Build()

	var found java.J
	v := visitor.Init(&identFinder{target: "x", found: &found})
	v.Visit(cu, nil)

	require.NotNil(t, found)

	result := pat.Match(found, nil)
	assert.NotNil(t, result)
}

func TestPatternNoMatch(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", "package main\n\nvar z = y\n")
	require.NoError(t, err)

	pat := Expression("x").Build()

	var found java.J
	v := visitor.Init(&identFinder{target: "y", found: &found})
	v.Visit(cu, nil)

	require.NotNil(t, found)

	result := pat.Match(found, nil)
	assert.Nil(t, result)
}

func TestPatternMatchWithCapture(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", "package main\n\nvar x = 1 + 2\n")
	require.NoError(t, err)

	expr := Expr("expr")
	pat := Expression(fmt.Sprintf("%s + 2", expr)).
		Captures(expr).
		Build()

	var found java.J
	v := visitor.Init(&binaryFinder{found: &found})
	v.Visit(cu, nil)

	require.NotNil(t, found)

	result := pat.Match(found, nil)
	require.NotNil(t, result)

	captured := result.Get("expr")
	require.NotNil(t, captured)
	lit, ok := captured.(*java.Literal)
	if !ok {
		t.Fatalf("expected captured value to be *java.Literal, got %T", captured)
	}
	assert.Equal(t, "1", lit.Source)
}

func TestRewriteVisitor(t *testing.T) {
	pat := Expression("x").Build()
	tmpl := ExpressionTemplate("y").Build()
	rewriter := Rewrite(pat, tmpl)

	r := &rewriteRecipeWithVisitor{visitor: rewriter}
	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = x
		`, `
			package main

			var a = y
		`),
	)
}

func TestRewriteBinaryExpression(t *testing.T) {
	pat := Expression("1 + 2").Build()
	tmpl := ExpressionTemplate("3").Build()
	rewriter := Rewrite(pat, tmpl)

	r := &rewriteRecipeWithVisitor{visitor: rewriter}
	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = 1 + 2
		`, `
			package main

			var a = 3
		`),
	)
}

func TestRewriteWithCapture(t *testing.T) {
	expr := Expr("expr")
	pat := Expression(fmt.Sprintf("%s + 0", expr)).
		Captures(expr).
		Build()
	tmpl := ExpressionTemplate(fmt.Sprintf("%s", expr)).
		Captures(expr).
		Build()
	rewriter := Rewrite(pat, tmpl)

	r := &rewriteRecipeWithVisitor{visitor: rewriter}
	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = x + 0
		`, `
			package main

			var a = x
		`),
	)
}

func TestPatternNoChangeWhenNoMatch(t *testing.T) {
	pat := Expression("1 + 2").Build()
	tmpl := ExpressionTemplate("99").Build()
	rewriter := Rewrite(pat, tmpl)

	r := &rewriteRecipeWithVisitor{visitor: rewriter}
	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = 3 + 4
		`),
	)
}

func TestRewritePreservesFormatting(t *testing.T) {
	pat := Expression("x").Build()
	tmpl := ExpressionTemplate("y").Build()
	rewriter := Rewrite(pat, tmpl)

	src := "package main\n\nvar a = x\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	require.NoError(t, err)

	ctx := recipe.NewExecutionContext()
	result := rewriter.Visit(cu, ctx)
	require.NotNil(t, result)

	actual := printer.Print(result)
	expected := "package main\n\nvar a = y\n"
	assert.Equal(t, expected, actual)
}

func TestPatternMatchGoUnary(t *testing.T) {
	// given a Go-specific unary (address-of) expression `&b` in the source
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", "package main\n\nfunc f(b int) { g(&b) }\n")
	require.NoError(t, err)

	var found java.J
	v := visitor.Init(&goUnaryFinder{found: &found})
	v.Visit(cu, nil)
	require.NotNil(t, found)

	// when matching a pattern that captures the operand of `&<expr>`
	expr := Expr("expr")
	pat := Expression(fmt.Sprintf("&%s", expr)).
		Captures(expr).
		Build()

	// then the pattern matches (regression: golang.Unary had no comparator case)
	result := pat.Match(found, nil)
	require.NotNil(t, result)
	captured := result.Get("expr")
	require.NotNil(t, captured)
	ident, ok := captured.(*java.Identifier)
	if !ok || ident.Name != "b" {
		t.Fatalf("expected captured identifier 'b', got %T %v", captured, captured)
	}
}

type rewriteRecipeWithVisitor struct {
	recipe.Base
	visitor recipe.TreeVisitor
}

func (r *rewriteRecipeWithVisitor) Name() string               { return "test.Rewrite" }
func (r *rewriteRecipeWithVisitor) DisplayName() string        { return "Test Rewrite" }
func (r *rewriteRecipeWithVisitor) Description() string        { return "Test rewrite recipe" }
func (r *rewriteRecipeWithVisitor) Editor() recipe.TreeVisitor { return r.visitor }

type identFinder struct {
	visitor.GoVisitor
	target string
	found  *java.J
}

func (v *identFinder) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if *v.found == nil && ident.Name == v.target {
		*v.found = ident
	}
	return v.GoVisitor.VisitIdentifier(ident, p)
}

type goUnaryFinder struct {
	visitor.GoVisitor
	found *java.J
}

func (v *goUnaryFinder) VisitGoUnary(u *golang.Unary, p any) java.J {
	if *v.found == nil {
		*v.found = u
	}
	return v.GoVisitor.VisitGoUnary(u, p)
}

type binaryFinder struct {
	visitor.GoVisitor
	found *java.J
}

func (v *binaryFinder) VisitBinary(bin *java.Binary, p any) java.J {
	if *v.found == nil {
		*v.found = bin
	}
	return v.GoVisitor.VisitBinary(bin, p)
}

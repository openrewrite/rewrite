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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func TestScaffoldExpression(t *testing.T) {
	source, _ := buildScaffold("1 + 2", nil, nil, ScaffoldExpression)
	if source == "" {
		t.Fatal("expected non-empty scaffold source")
	}

	p := parser.NewGoParser()
	_, err := p.Parse("test.go", source)
	if err != nil {
		t.Fatalf("scaffold should parse: %v\nsource:\n%s", err, source)
	}
}

func TestScaffoldStatement(t *testing.T) {
	source, _ := buildScaffold("x = 1", nil, nil, ScaffoldStatement)
	if source == "" {
		t.Fatal("expected non-empty scaffold source")
	}

	p := parser.NewGoParser()
	_, err := p.Parse("test.go", source)
	if err != nil {
		t.Fatalf("scaffold should parse: %v\nsource:\n%s", err, source)
	}
}

func TestScaffoldWithCaptures(t *testing.T) {
	caps := captureMap([]*Capture{Expr("x")})
	source, count := buildScaffold(fmt.Sprintf("%s + 1", Expr("x")), caps, nil, ScaffoldExpression)
	if count != 1 {
		t.Errorf("expected preamble count 1, got %d", count)
	}

	p := parser.NewGoParser()
	_, err := p.Parse("test.go", source)
	if err != nil {
		t.Fatalf("scaffold with captures should parse: %v\nsource:\n%s", err, source)
	}
}

func TestParseScaffoldExpression(t *testing.T) {
	node, err := parseScaffold("1 + 2", nil, nil, ScaffoldExpression)
	if err != nil {
		t.Fatalf("parseScaffold error: %v", err)
	}
	if node == nil {
		t.Fatal("expected non-nil node")
	}
	bin, ok := node.(*tree.Binary)
	if !ok {
		t.Fatalf("expected *tree.Binary, got %T", node)
	}
	if bin.Left == nil || bin.Right == nil {
		t.Fatal("binary should have left and right")
	}
}

func TestPatternMatchIdentifier(t *testing.T) {
	// Parse source containing identifier "x"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", "package main\n\nvar y = x\n")
	if err != nil {
		t.Fatal(err)
	}

	// Build a pattern that matches identifier "x"
	pat := Expression("x").Build()

	// Find the identifier "x" in the parsed tree
	var found tree.J
	v := visitor.Init(&identFinder{target: "x", found: &found})
	v.Visit(cu, nil)

	if found == nil {
		t.Fatal("could not find identifier 'x' in parsed tree")
	}

	result := pat.Match(found, nil)
	if result == nil {
		t.Error("pattern should match identifier 'x'")
	}
}

func TestPatternNoMatch(t *testing.T) {
	// Parse source containing identifier "y"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", "package main\n\nvar z = y\n")
	if err != nil {
		t.Fatal(err)
	}

	// Build a pattern that matches identifier "x"
	pat := Expression("x").Build()

	// Find identifier "y" in the tree
	var found tree.J
	v := visitor.Init(&identFinder{target: "y", found: &found})
	v.Visit(cu, nil)

	if found == nil {
		t.Fatal("could not find identifier 'y'")
	}

	result := pat.Match(found, nil)
	if result != nil {
		t.Error("pattern 'x' should not match identifier 'y'")
	}
}

func TestPatternMatchWithCapture(t *testing.T) {
	// Parse: 1 + 2
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", "package main\n\nvar x = 1 + 2\n")
	if err != nil {
		t.Fatal(err)
	}

	// Pattern: <expr> + 2
	expr := Expr("expr")
	pat := Expression(fmt.Sprintf("%s + 2", expr)).
		Captures(expr).
		Build()

	// Find the binary expression
	var found tree.J
	v := visitor.Init(&binaryFinder{found: &found})
	v.Visit(cu, nil)

	if found == nil {
		t.Fatal("could not find binary expression")
	}

	result := pat.Match(found, nil)
	if result == nil {
		t.Fatal("pattern should match binary expression")
	}

	captured := result.Get("expr")
	if captured == nil {
		t.Fatal("expected capture 'expr' to be bound")
	}
	lit, ok := captured.(*tree.Literal)
	if !ok {
		t.Fatalf("expected captured value to be *tree.Literal, got %T", captured)
	}
	if lit.Source != "1" {
		t.Errorf("expected captured literal source '1', got %q", lit.Source)
	}
}

func TestRewriteVisitor(t *testing.T) {
	// Recipe that rewrites `x` identifiers to `y`
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
	// Recipe that rewrites `1 + 2` to `3`
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
	// Recipe that rewrites `<expr> + 0` to `<expr>`
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
	// Pattern that matches `1 + 2` should not change `3 + 4`
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
	// Rewriting should preserve the original node's prefix (whitespace).
	pat := Expression("x").Build()
	tmpl := ExpressionTemplate("y").Build()
	rewriter := Rewrite(pat, tmpl)

	src := "package main\n\nvar a = x\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	if err != nil {
		t.Fatal(err)
	}

	ctx := recipe.NewExecutionContext()
	result := rewriter.Visit(cu, ctx)
	if result == nil {
		t.Fatal("expected non-nil result")
	}

	actual := printer.Print(result)
	expected := "package main\n\nvar a = y\n"
	if actual != expected {
		t.Errorf("formatting not preserved\nexpected: %q\nactual:   %q", expected, actual)
	}
}

// --- Test helpers ---

type rewriteRecipeWithVisitor struct {
	recipe.Base
	visitor recipe.TreeVisitor
}

func (r *rewriteRecipeWithVisitor) Name() string                { return "test.Rewrite" }
func (r *rewriteRecipeWithVisitor) DisplayName() string         { return "Test Rewrite" }
func (r *rewriteRecipeWithVisitor) Description() string         { return "Test rewrite recipe" }
func (r *rewriteRecipeWithVisitor) Editor() recipe.TreeVisitor  { return r.visitor }

// identFinder walks the tree to find the first Identifier with the given name.
type identFinder struct {
	visitor.GoVisitor
	target string
	found  *tree.J
}

func (v *identFinder) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	if *v.found == nil && ident.Name == v.target {
		*v.found = ident
	}
	return v.GoVisitor.VisitIdentifier(ident, p)
}

// binaryFinder walks the tree to find the first Binary expression.
type binaryFinder struct {
	visitor.GoVisitor
	found *tree.J
}

func (v *binaryFinder) VisitBinary(bin *tree.Binary, p any) tree.J {
	if *v.found == nil {
		*v.found = bin
	}
	return v.GoVisitor.VisitBinary(bin, p)
}

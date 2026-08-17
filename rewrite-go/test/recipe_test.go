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

package test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type findFoo struct {
	recipe.Base
}

func (r *findFoo) Name() string        { return "org.openrewrite.golang.test.FindFoo" }
func (r *findFoo) DisplayName() string { return "Find foo identifiers" }
func (r *findFoo) Description() string {
	return "Marks all identifiers named `foo` with a search result."
}

func (r *findFoo) Editor() recipe.TreeVisitor {
	return visitor.Init(&findFooVisitor{})
}

type findFooVisitor struct {
	visitor.GoVisitor
}

func (v *findFooVisitor) VisitIdentifier(ident *java.Identifier, p any) java.J {
	ident = v.GoVisitor.VisitIdentifier(ident, p).(*java.Identifier)
	if ident.Name == "foo" {
		ident = ident.WithMarkers(java.FoundSearchResult(ident.Markers, "found foo"))
	}
	return ident
}

type renameFooToBar struct {
	recipe.Base
}

func (r *renameFooToBar) Name() string        { return "org.openrewrite.golang.test.RenameFooToBar" }
func (r *renameFooToBar) DisplayName() string { return "Rename foo to bar" }
func (r *renameFooToBar) Description() string { return "Renames all identifiers named `foo` to `bar`." }

func (r *renameFooToBar) Editor() recipe.TreeVisitor {
	return visitor.Init(&renameFooToBarVisitor{})
}

type renameFooToBarVisitor struct {
	visitor.GoVisitor
}

func (v *renameFooToBarVisitor) VisitIdentifier(ident *java.Identifier, p any) java.J {
	ident = v.GoVisitor.VisitIdentifier(ident, p).(*java.Identifier)
	if ident.Name == "foo" {
		ident = ident.WithName("bar")
	}
	return ident
}

func TestRecipeRename(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&renameFooToBar{})
	spec.RewriteRun(t,
		test.Golang(`
			package main

			func foo() {
			}
		`, `
			package main

			func bar() {
			}
		`),
	)
}

func TestRecipeNoChange(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&renameFooToBar{})
	spec.RewriteRun(t,
		test.Golang(`
			package main

			func hello() {
			}
		`),
	)
}

// TestSearchRecipeViaRewriteRun verifies that RewriteRun prints SearchResult
// markers as /*~~(...)~~>*/ comments by default, so search-style recipes can
// be tested with the same convention used in Java and TypeScript.
func TestSearchRecipeViaRewriteRun(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&findFoo{})
	spec.RewriteRun(t,
		test.GolangRaw(
			"package main\n\nfunc foo() {\n}\n",
			"package main\n\nfunc /*~~(found foo)~~>*/foo() {\n}\n",
		),
	)
}

// TestSearchRecipeWithSanitizedMarkerPrinter verifies that
// WithMarkerPrinter(SanitizedMarkerPrinter) lets tests opt out of marker
// rendering — matching Java's spec.markerPrinter(SANITIZED).
func TestSearchRecipeWithSanitizedMarkerPrinter(t *testing.T) {
	spec := test.NewRecipeSpec().
		WithRecipe(&findFoo{}).
		WithMarkerPrinter(printer.SanitizedMarkerPrinter)
	spec.RewriteRun(t,
		test.GolangRaw(
			"package main\n\nfunc foo() {\n}\n",
			"package main\n\nfunc foo() {\n}\n",
		),
	)
}

func TestSearchRecipeWithMarkerPrinting(t *testing.T) {
	r := &findFoo{}
	editor := r.Editor()

	src := "package main\n\nfunc foo() {\n}\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	require.NoError(t, err)

	ctx := recipe.NewExecutionContext()
	result := editor.Visit(cu, ctx)

	// Print with default marker printer — should show search result comment
	output := printer.PrintWithMarkers(result, printer.DefaultMarkerPrinter)
	expected := "package main\n\nfunc /*~~(found foo)~~>*/foo() {\n}\n"
	assert.Equal(t, expected, output, "marker output mismatch")

	// Print without markers — should be original source
	plain := printer.Print(result)
	assert.Equal(t, plain, src, "plain print should match original source")

	// Print with sanitized printer — should strip markers
	sanitized := printer.PrintWithMarkers(result, printer.SanitizedMarkerPrinter)
	assert.Equal(t, sanitized, src, "sanitized print should match original source")
}

func TestRecipeDescriptor(t *testing.T) {
	r := &findFoo{}
	desc := recipe.Describe(r)

	assert.Equalf(t, "org.openrewrite.golang.test.FindFoo", desc.Name, "expected name %q", "org.openrewrite.golang.test.FindFoo")
	assert.Equalf(t, "Find foo identifiers", desc.DisplayName, "expected displayName %q", "Find foo identifiers")
	if desc.EstimatedEffortPerOccurrence != 5*time.Minute {
		t.Errorf("expected 5 minute default effort, got %v", desc.EstimatedEffortPerOccurrence)
	}
}

func TestRegistryActivate(t *testing.T) {
	reg := recipe.NewRegistry()

	// Each module provides an Activate function
	activateSearch := func(r *recipe.Registry) {
		golang := recipe.CategoryDescriptor{DisplayName: "Go"}
		search := recipe.CategoryDescriptor{DisplayName: "Search"}
		r.Register(&findFoo{}, golang, search)
	}
	activateRefactoring := func(r *recipe.Registry) {
		golang := recipe.CategoryDescriptor{DisplayName: "Go"}
		r.Register(&renameFooToBar{}, golang)
	}

	reg.Activate(activateSearch, activateRefactoring)

	found, ok := reg.FindRecipe("org.openrewrite.golang.test.FindFoo")
	require.True(t, ok, "expected to find FindFoo recipe")
	assert.Equalf(t, "Find foo identifiers", found.Descriptor.DisplayName, "expected displayName %q", "Find foo identifiers")

	// All recipes
	all := reg.AllRecipes()
	assert.Len(t, all, 2, "expected 2 recipes")

	// Categories
	cats := reg.Categories()
	require.Len(t, cats, 1, "expected 1 top-level category")
	assert.Equalf(t, "Go", cats[0].DisplayName, "expected top-level category 'Go', got %q", cats[0].DisplayName)
	assert.Lenf(t, cats[0].Recipes, 1, "expected 1 recipe directly in Go category, got %d", len(cats[0].Recipes))
	require.Lenf(t, cats[0].Subcategories, 1, "expected 1 subcategory in Go, got %d", len(cats[0].Subcategories))
	assert.Equalf(t, "Search", cats[0].Subcategories[0].DisplayName, "expected subcategory 'Search', got %q", cats[0].Subcategories[0].DisplayName)
}

func TestRegistryReflectConstructor(t *testing.T) {
	reg := recipe.NewRegistry()

	reg.Activate(func(r *recipe.Registry) {
		r.Register(&renameFooToBar{}, recipe.CategoryDescriptor{DisplayName: "Go"})
	})

	found, ok := reg.FindRecipe("org.openrewrite.golang.test.RenameFooToBar")
	require.True(t, ok, "expected to find recipe")

	// Constructor auto-derived from prototype via reflection
	instance := found.Constructor(nil)
	assert.Equal(t, "org.openrewrite.golang.test.RenameFooToBar", instance.Name(), "unexpected name")
}

func TestFencedMarkerPrinting(t *testing.T) {
	r := &findFoo{}
	editor := r.Editor()

	src := "package main\n\nfunc foo() {\n}\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	require.NoError(t, err)

	ctx := recipe.NewExecutionContext()
	result := editor.Visit(cu, ctx)

	// Print with fenced printer — should show {{uuid}} delimiters
	output := printer.PrintWithMarkers(result, printer.FencedMarkerPrinter)
	assert.NotEqual(t, output, src, "expected fenced markers in output, but output is unchanged")
	// Fenced output should contain UUID-style markers
	if len(output) <= len(src) {
		t.Error("fenced output should be longer than original source")
	}
}

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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// --- Sample search recipe that marks identifiers named "foo" ---

type findFoo struct {
	recipe.Base
}

func (r *findFoo) Name() string        { return "org.openrewrite.golang.test.FindFoo" }
func (r *findFoo) DisplayName() string { return "Find foo identifiers" }
func (r *findFoo) Description() string { return "Marks all identifiers named `foo` with a search result." }

func (r *findFoo) Editor() recipe.TreeVisitor {
	return visitor.Init(&findFooVisitor{})
}

type findFooVisitor struct {
	visitor.GoVisitor
}

func (v *findFooVisitor) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	ident = v.GoVisitor.VisitIdentifier(ident, p).(*tree.Identifier)
	if ident.Name == "foo" {
		ident = ident.WithMarkers(tree.FoundSearchResult(ident.Markers, "found foo"))
	}
	return ident
}

// --- Sample refactoring recipe that renames "foo" to "bar" ---

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

func (v *renameFooToBarVisitor) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	ident = v.GoVisitor.VisitIdentifier(ident, p).(*tree.Identifier)
	if ident.Name == "foo" {
		ident = ident.WithName("bar")
	}
	return ident
}

// --- Tests ---

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

func TestSearchRecipeWithMarkerPrinting(t *testing.T) {
	r := &findFoo{}
	editor := r.Editor()

	src := "package main\n\nfunc foo() {\n}\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	if err != nil {
		t.Fatal(err)
	}

	ctx := recipe.NewExecutionContext()
	result := editor.Visit(cu, ctx)

	// Print with default marker printer — should show search result comment
	output := printer.PrintWithMarkers(result, printer.DefaultMarkerPrinter)
	expected := "package main\n\nfunc /*~~(found foo)~~>*/foo() {\n}\n"
	if output != expected {
		t.Errorf("marker output mismatch\nexpected: %q\nactual:   %q", expected, output)
	}

	// Print without markers — should be original source
	plain := printer.Print(result)
	if plain != src {
		t.Errorf("plain print should match original source\nexpected: %q\nactual:   %q", src, plain)
	}

	// Print with sanitized printer — should strip markers
	sanitized := printer.PrintWithMarkers(result, printer.SanitizedMarkerPrinter)
	if sanitized != src {
		t.Errorf("sanitized print should match original source\nexpected: %q\nactual:   %q", src, sanitized)
	}
}

func TestRecipeDescriptor(t *testing.T) {
	r := &findFoo{}
	desc := recipe.Describe(r)

	if desc.Name != "org.openrewrite.golang.test.FindFoo" {
		t.Errorf("expected name %q, got %q", "org.openrewrite.golang.test.FindFoo", desc.Name)
	}
	if desc.DisplayName != "Find foo identifiers" {
		t.Errorf("expected displayName %q, got %q", "Find foo identifiers", desc.DisplayName)
	}
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

	// Find by name
	found, ok := reg.FindRecipe("org.openrewrite.golang.test.FindFoo")
	if !ok {
		t.Fatal("expected to find FindFoo recipe")
	}
	if found.Descriptor.DisplayName != "Find foo identifiers" {
		t.Errorf("expected displayName %q, got %q", "Find foo identifiers", found.Descriptor.DisplayName)
	}

	// All recipes
	all := reg.AllRecipes()
	if len(all) != 2 {
		t.Errorf("expected 2 recipes, got %d", len(all))
	}

	// Categories
	cats := reg.Categories()
	if len(cats) != 1 {
		t.Fatalf("expected 1 top-level category, got %d", len(cats))
	}
	if cats[0].DisplayName != "Go" {
		t.Errorf("expected top-level category 'Go', got %q", cats[0].DisplayName)
	}
	if len(cats[0].Recipes) != 1 {
		t.Errorf("expected 1 recipe directly in Go category, got %d", len(cats[0].Recipes))
	}
	if len(cats[0].Subcategories) != 1 {
		t.Fatalf("expected 1 subcategory in Go, got %d", len(cats[0].Subcategories))
	}
	if cats[0].Subcategories[0].DisplayName != "Search" {
		t.Errorf("expected subcategory 'Search', got %q", cats[0].Subcategories[0].DisplayName)
	}
}

func TestRegistryReflectConstructor(t *testing.T) {
	reg := recipe.NewRegistry()

	reg.Activate(func(r *recipe.Registry) {
		r.Register(&renameFooToBar{}, recipe.CategoryDescriptor{DisplayName: "Go"})
	})

	found, ok := reg.FindRecipe("org.openrewrite.golang.test.RenameFooToBar")
	if !ok {
		t.Fatal("expected to find recipe")
	}

	// Constructor auto-derived from prototype via reflection
	instance := found.Constructor(nil)
	if instance.Name() != "org.openrewrite.golang.test.RenameFooToBar" {
		t.Errorf("unexpected name: %s", instance.Name())
	}
}

func TestFencedMarkerPrinting(t *testing.T) {
	r := &findFoo{}
	editor := r.Editor()

	src := "package main\n\nfunc foo() {\n}\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	if err != nil {
		t.Fatal(err)
	}

	ctx := recipe.NewExecutionContext()
	result := editor.Visit(cu, ctx)

	// Print with fenced printer — should show {{uuid}} delimiters
	output := printer.PrintWithMarkers(result, printer.FencedMarkerPrinter)
	if output == src {
		t.Error("expected fenced markers in output, but output is unchanged")
	}
	// Fenced output should contain UUID-style markers
	if len(output) <= len(src) {
		t.Error("fenced output should be longer than original source")
	}
}

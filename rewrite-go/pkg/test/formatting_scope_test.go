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

package test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// renameAndFormat renames a to b, and formats in the way given.
type renameAndFormat struct {
	recipe.Base
	name string
	how  string
}

func (r *renameAndFormat) Name() string {
	if r.name != "" {
		return r.name
	}
	return "org.openrewrite.golang.test.RenameAndFormat"
}
func (r *renameAndFormat) DisplayName() string { return "Rename and format" }
func (r *renameAndFormat) Description() string { return "Renames `a` to `b`, and formats." }

func (r *renameAndFormat) Editor() recipe.TreeVisitor {
	return visitor.Init(&formattingRenamer{how: r.how})
}

type formattingRenamer struct {
	visitor.GoVisitor
	how string
}

func (v *formattingRenamer) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	renamed := v.GoVisitor.VisitCompilationUnit(cu, p)
	if v.how != "file" || renamed == java.J(cu) {
		return renamed
	}
	return format.AutoFormat(renamed, p, nil, nil).(java.J)
}

func (v *formattingRenamer) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if ident.Name != "a" {
		return ident
	}
	renamed := ident.WithName("b")
	switch v.how {
	case "after visit":
		v.DoAfterVisit(format.NewAutoFormatVisitor(nil))
	case "stop after":
		v.DoAfterVisit(format.NewAutoFormatVisitor(renamed))
	case "element":
		return format.MaybeAutoFormat(ident, renamed, p, nil, v.Cursor().Parent()).(java.J)
	}
	return renamed
}

const declaresA = `
	package main

	func a() {}
`

const declaresB = `
	package main

	func b() {}
`

func TestFormattingTheWholeFileInAnAfterVisitFails(t *testing.T) {
	// given a recipe that renames a function and then formats the whole file
	spec := NewRecipeSpec().WithRecipe(&renameAndFormat{how: "after visit"})

	// when it runs
	got := failures(t, spec, Golang(declaresA, declaresB))

	// then the run fails, naming how the recipe formatted and what
	assert.Equal(t, []string{
		"Format only what the recipe changed, not the whole file around it. These calls auto-formatted a " +
			"whole source file that existed before the run, with no stopAfter:\n" +
			"  A visitor's DoAfterVisit(format.NewAutoFormatVisitor(nil)) ran on\n" +
			"    all of test.go\n" +
			"That rewrites the whitespace of code the recipe did not change. Test inputs are usually formatted " +
			"already, so expected outputs rarely show it, but on real code the diff will carry unrelated " +
			"reformatting. Instead:\n" +
			"  - format the element you created or replaced: format.MaybeAutoFormat(original, changed, p, nil, " +
			"v.Cursor().Parent()) on that element, or format.AutoFormat(newElement, p, nil, cursor) with a cursor " +
			"on its parent\n" +
			"  - when a new element only formats right inside its container, format a copy of the container that " +
			"holds just the new element, and add the formatted element to the original\n" +
			"  - when you remove an element, give its prefix to the element that takes its place\n" +
			"  - a template from pkg/template already formats what it inserts, so drop the extra call\n" +
			"Recipes in a format package are exempt. Any other recipe whose purpose is formatting opts out with " +
			"spec.CheckMinimalFormatting = false.",
	}, got)
}

func TestAutoFormatOfTheWholeFileFails(t *testing.T) {
	// given a recipe that calls AutoFormat on the file it changed
	spec := NewRecipeSpec().WithRecipe(&renameAndFormat{how: "file"})

	// when it runs
	got := failures(t, spec, Golang(declaresA, declaresB))

	// then the call is named
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "  test.(*formattingRenamer).VisitCompilationUnit(formatting_scope_test.go:")
	assert.Contains(t, got[0], ") called format.AutoFormat on\n    all of test.go\n")
}

func TestFormattingOnlyTheChangedElementPasses(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&renameAndFormat{how: "element"}).
		RewriteRun(t, Golang(declaresA, declaresB))
}

func TestFormattingThatStopsAfterTheChangePasses(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&renameAndFormat{how: "stop after"}).
		RewriteRun(t, Golang(declaresA, declaresB))
}

// autoFormat is a recipe whose own visitor is the formatter.
type autoFormat struct {
	recipe.Base
}

func (r *autoFormat) Name() string               { return "org.openrewrite.golang.test.AutoFormatAll" }
func (r *autoFormat) DisplayName() string        { return "Auto-format" }
func (r *autoFormat) Description() string        { return "Formats every source file." }
func (r *autoFormat) Editor() recipe.TreeVisitor { return format.NewAutoFormatVisitor(nil) }

func TestRecipeWhoseVisitorIsAFormatterPasses(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&recipeList{recipes: []recipe.Recipe{&renameIdentifiers{rename: renameAToB}, &autoFormat{}}}).
		RewriteRun(t, Golang(declaresA, declaresB))
}

func TestRecipeInAFormatPackagePasses(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&renameAndFormat{name: "org.openrewrite.golang.format.RenameAndFormat", how: "after visit"}).
		RewriteRun(t, Golang(declaresA, declaresB))
}

// bumpGoAndFormat raises the go directive of a go.mod to 1.23, and formats the file.
type bumpGoAndFormat struct {
	recipe.Base
}

func (r *bumpGoAndFormat) Name() string        { return "org.openrewrite.golang.test.BumpGoAndFormat" }
func (r *bumpGoAndFormat) DisplayName() string { return "Bump go and format" }
func (r *bumpGoAndFormat) Description() string { return "Raises go to 1.23, and formats." }

func (r *bumpGoAndFormat) Editor() recipe.TreeVisitor {
	return visitor.Init(&goDirectiveBumper{})
}

type goDirectiveBumper struct {
	visitor.GoVisitor
}

func (v *goDirectiveBumper) VisitGoModDirective(d *golang.GoModDirective, _ any) java.Tree {
	if d.Keyword != "go" || len(d.Values) != 1 || d.Values[0].Text != "1.22" {
		return d
	}
	bumped := *d.Values[0]
	bumped.Text = "1.23"
	c := *d
	c.Values = []*golang.GoModValue{&bumped}
	v.DoAfterVisit(format.NewAutoFormatVisitor(nil))
	return &c
}

func TestFormattingAWholeGoModPasses(t *testing.T) {
	// the formatter lays out Go source files only
	NewRecipeSpec().
		WithRecipe(&bumpGoAndFormat{}).
		RewriteRun(t, GoMod(`
			module example.com/app

			go 1.22
		`, `
			module example.com/app

			go 1.23
		`))
}

func TestMinimalFormattingCheckCanBeTurnedOff(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&renameAndFormat{how: "after visit"})
	spec.CheckMinimalFormatting = false
	spec.RewriteRun(t, Golang(declaresA, declaresB))
}

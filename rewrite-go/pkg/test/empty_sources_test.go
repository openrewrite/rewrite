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
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// renameFirstFunction renames the function a file declares first to first,
// trusting that there is one unless checked, and counts in declaringNone the
// files it meets that declare none.
type renameFirstFunction struct {
	recipe.Base
	checked       bool
	declaringNone *int
}

func (r *renameFirstFunction) Name() string {
	return "org.openrewrite.golang.test.RenameFirstFunction"
}
func (r *renameFirstFunction) DisplayName() string { return "Rename the first function" }
func (r *renameFirstFunction) Description() string {
	return "Renames the function a file declares first to `first`."
}

func (r *renameFirstFunction) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("checked", "Checked", "Check that the file declares a function.").WithValue(r.checked),
	}
}

func (r *renameFirstFunction) Editor() recipe.TreeVisitor {
	return visitor.Init(&firstFunctionRenamer{recipe: r})
}

type firstFunctionRenamer struct {
	visitor.GoVisitor
	recipe *renameFirstFunction
}

func (v *firstFunctionRenamer) VisitCompilationUnit(cu *golang.CompilationUnit, _ any) java.J {
	if len(cu.Statements) == 0 && v.recipe.declaringNone != nil {
		*v.recipe.declaringNone++
	}
	if v.recipe.checked && len(cu.Statements) == 0 {
		return cu
	}
	fn := cu.Statements[0].Element.(*java.MethodDeclaration)
	if fn.Name.Name == "first" {
		return cu
	}
	statements := append([]java.RightPadded[java.Statement]{}, cu.Statements...)
	statements[0].Element = fn.WithName(fn.Name.WithName("first"))
	return cu.WithStatements(statements)
}

// reportFirstDirective reports the verb of the first directive of a go.mod,
// trusting that there is one.
type reportFirstDirective struct {
	recipe.Base
}

func (r *reportFirstDirective) Name() string {
	return "org.openrewrite.golang.test.ReportFirstDirective"
}
func (r *reportFirstDirective) DisplayName() string { return "Report the first directive" }
func (r *reportFirstDirective) Description() string {
	return "Reports the verb of the first directive of a go.mod."
}

func (r *reportFirstDirective) Editor() recipe.TreeVisitor {
	return visitor.Init(&firstDirectiveReporter{})
}

type firstDirectiveReporter struct {
	visitor.GoVisitor
}

func (v *firstDirectiveReporter) VisitGoMod(gm *golang.GoMod, _ any) java.Tree {
	if java.HasMarker[java.SearchResult](gm.Markers) {
		return gm
	}
	d := gm.Statements[0].Element.(*golang.GoModDirective)
	return gm.WithMarkers(java.FoundSearchResult(gm.Markers, "first: "+d.Keyword))
}

func TestRecipeThatPanicsOnAFileWithOnlyAPackageClauseFails(t *testing.T) {
	forgetProbes(t)
	// given a recipe that trusts a file to declare a function
	spec := NewRecipeSpec().WithRecipe(&renameFirstFunction{})

	// when it runs on a file of a project that does
	got := failures(t, spec, GoProject("app", Golang(`
		package main

		func a() {}
	`, `
		package main

		func first() {}
	`)))

	// then the run fails, naming the file the recipe panicked on and where
	require.Len(t, got, 1)
	assert.True(t, strings.HasPrefix(got[0], "Check for the structure the recipe edits before using it, so that "+
		"it handles an empty file or leaves it unchanged instead of panicking.\n"+
		"org.openrewrite.golang.test.RenameFirstFunction panicked when run on a golang.CompilationUnit at test.go "+
		"holding only `package main` with the markers of the test's source (golang.GoProject):\n"+
		"    runtime error: index out of range [0] with length 0\n"+
		"        at test.(*firstFunctionRenamer).VisitCompilationUnit(empty_sources_test.go:"), got[0])
	assert.True(t, strings.HasSuffix(got[0], "  - don't recover from the panic to make this pass\n"+
		"Then add a test with such a source, such as Golang(\"package main\"), GoMod(\"\") or GoSum(\"\"), "+
		"that states the intended result."), got[0])
}

func TestRecipeThatChecksTheStructurePasses(t *testing.T) {
	forgetProbes(t)
	NewRecipeSpec().
		WithRecipe(&renameFirstFunction{checked: true}).
		RewriteRun(t, Golang(`
			package main

			func a() {}
		`, `
			package main

			func first() {}
		`))
}

func TestRecipeThatPanicsOnAnEmptyGoModFails(t *testing.T) {
	forgetProbes(t)
	// given a recipe that trusts a go.mod to hold a directive
	spec := NewRecipeSpec().WithRecipe(&reportFirstDirective{})

	// when it runs on one that does
	got := failures(t, spec, GoMod(`
		module example.com/app

		go 1.22
	`, `
		/*~~(first: module)~~>*/module example.com/app

		go 1.22
	`))

	// then the empty go.mod it panics on is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "org.openrewrite.golang.test.ReportFirstDirective panicked when run on an empty "+
		"golang.GoMod at go.mod:\n"+
		"    runtime error: index out of range [0] with length 0\n"+
		"        at test.(*firstDirectiveReporter).VisitGoMod(empty_sources_test.go:")
}

func TestRecipeThatChangesNothingIsNotRunOnEmptySources(t *testing.T) {
	forgetProbes(t)
	NewRecipeSpec().
		WithRecipe(&renameFirstFunction{}).
		RewriteRun(t, Golang(`
			package main

			func first() {}
		`))
}

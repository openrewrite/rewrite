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
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type recipeList struct {
	recipe.Base
	recipes []recipe.Recipe
}

func (r *recipeList) Name() string                { return "org.openrewrite.golang.test.RecipeList" }
func (r *recipeList) DisplayName() string         { return "Recipe list" }
func (r *recipeList) Description() string         { return "Runs its recipes in order." }
func (r *recipeList) RecipeList() []recipe.Recipe { return r.recipes }

func renameAToB(_ *identifierRenamer, ident *java.Identifier) *java.Identifier {
	if ident.Name == "a" {
		return ident.WithName("b")
	}
	return ident
}

type reportSawA struct {
	recipe.ScanningBase
}

type sawA struct {
	saw bool
}

func (r *reportSawA) Name() string        { return "org.openrewrite.golang.test.ReportSawA" }
func (r *reportSawA) DisplayName() string { return "Report whether `a` was scanned" }
func (r *reportSawA) Description() string {
	return "Marks every source file with whether the scanner saw an identifier `a`."
}

func (r *reportSawA) InitialValue(*recipe.ExecutionContext) any { return &sawA{} }

func (r *reportSawA) Scanner(acc any) recipe.TreeVisitor {
	return visitor.Init(&sawAScanner{acc: acc.(*sawA)})
}

func (r *reportSawA) EditorWithData(acc any) recipe.TreeVisitor {
	return visitor.Init(&searchResultOnce{description: func(any) string {
		return fmt.Sprintf("saw a: %t", acc.(*sawA).saw)
	}})
}

type sawAScanner struct {
	visitor.GoVisitor
	acc *sawA
}

func (v *sawAScanner) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if ident.Name == "a" {
		v.acc.saw = true
	}
	return ident
}

type searchResultOnce struct {
	visitor.GoVisitor
	description func(p any) string
}

func (v *searchResultOnce) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	if java.HasMarker[java.SearchResult](cu.Markers) {
		return cu
	}
	return cu.WithMarkers(java.FoundSearchResult(cu.Markers, v.description(p)))
}

type tally struct {
	recipe.Base
	count, report bool
}

const tallyKey = "org.openrewrite.golang.test.tally"

func (r *tally) Name() string        { return "org.openrewrite.golang.test.Tally" }
func (r *tally) DisplayName() string { return "Tally source files" }
func (r *tally) Description() string { return "Counts source files on the execution context." }

func (r *tally) Editor() recipe.TreeVisitor {
	return visitor.Init(&tallier{recipe: r})
}

type tallier struct {
	visitor.GoVisitor
	recipe *tally
}

func (v *tallier) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	if java.HasMarker[java.SearchResult](cu.Markers) {
		return cu
	}
	ctx := p.(*recipe.ExecutionContext)
	n, _ := ctx.GetMessage(tallyKey)
	count, _ := n.(int)
	if v.recipe.count {
		count++
		ctx.PutMessage(tallyKey, count)
	}
	if v.recipe.report {
		return cu.WithMarkers(java.FoundSearchResult(cu.Markers, fmt.Sprintf("tally %d", count)))
	}
	return cu
}

type deleteFile struct {
	recipe.Base
	path string
}

func (r *deleteFile) Name() string        { return "org.openrewrite.golang.test.DeleteFile" }
func (r *deleteFile) DisplayName() string { return "Delete a file" }
func (r *deleteFile) Description() string { return "Deletes the source file at a path." }

func (r *deleteFile) Editor() recipe.TreeVisitor {
	return visitor.Init(&fileDeleter{path: r.path})
}

type fileDeleter struct {
	visitor.GoVisitor
	path string
}

func (v *fileDeleter) VisitCompilationUnit(cu *golang.CompilationUnit, _ any) java.J {
	if cu.SourcePath == v.path {
		return nil
	}
	return cu
}

func TestScannerSeesTheSourcesBeforeAnyRecipeEditsThem(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&recipeList{recipes: []recipe.Recipe{
			&renameIdentifiers{rename: renameAToB},
			&reportSawA{},
		}}).
		RewriteRun(t, Golang(`
			package main

			func a() {}
		`, `
			/*~~(saw a: true)~~>*/package main

			func b() {}
		`))
}

func TestExecutionContextIsSharedAcrossSourceFiles(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&tally{count: true, report: true}).
		RewriteRun(t,
			Golang(`
				package main
			`, `
				/*~~(tally 1)~~>*/package main
			`).WithPath("a.go"),
			Golang(`
				package main
			`, `
				/*~~(tally 2)~~>*/package main
			`).WithPath("b.go"),
		)
}

func TestExecutionContextIsSharedAcrossRecipes(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&recipeList{recipes: []recipe.Recipe{
			&tally{count: true},
			&tally{report: true},
		}}).
		RewriteRun(t, Golang(`
			package main
		`, `
			/*~~(tally 1)~~>*/package main
		`))
}

func TestRecipeDeletingASourceFile(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&deleteFile{path: "a.go"}).
		RewriteRun(t,
			Golang(`
				package main
			`).WithPath("a.go").ExpectDeleted(),
			Golang(`
				package main
			`).WithPath("b.go"),
		)
}

func TestLaterRecipesDoNotEditADeletedSourceFile(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&recipeList{recipes: []recipe.Recipe{
			&deleteFile{path: "a.go"},
			&tally{count: true, report: true},
		}}).
		RewriteRun(t,
			Golang(`
				package main
			`).WithPath("a.go").ExpectDeleted(),
			Golang(`
				package main
			`, `
				/*~~(tally 1)~~>*/package main
			`).WithPath("b.go"),
		)
}

func TestDeletingASourceFileNotExpectedToChangeFails(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&deleteFile{path: "a.go"})

	// when
	got := failures(t, spec, Golang(`
		package main
	`).WithPath("a.go"))

	// then
	assert.Equal(t, []string{`The recipe deleted a source file "a.go" that was not expected to change`}, got)
}

func TestDeletingASourceFileExpectedToChangeFails(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&deleteFile{path: "a.go"})

	// when
	got := failures(t, spec, Golang(`
		package main
	`, `
		package app
	`).WithPath("a.go"))

	// then
	assert.Equal(t, []string{`The recipe deleted a source file "a.go" but should have changed it instead`}, got)
}

func TestNotDeletingASourceFileExpectedToBeDeletedFails(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&deleteFile{path: "b.go"})

	// when
	got := failures(t, spec, Golang(`
		package main
	`).WithPath("a.go").ExpectDeleted())

	// then
	assert.Equal(t, []string{`Expected the recipe to delete "a.go"`}, got)
}

const keepBKey = "org.openrewrite.golang.test.keepB"

type renameBToCUnlessKept struct {
	recipe.Base
}

func (r *renameBToCUnlessKept) Name() string {
	return "org.openrewrite.golang.test.RenameBToCUnlessKept"
}
func (r *renameBToCUnlessKept) DisplayName() string {
	return "Rename `b` to `c` unless told to keep it"
}
func (r *renameBToCUnlessKept) Description() string {
	return "Renames `b` to `c` unless the execution context says to keep `b`."
}

func (r *renameBToCUnlessKept) Editor() recipe.TreeVisitor {
	return visitor.Init(&bToCRenamer{})
}

type bToCRenamer struct {
	visitor.GoVisitor
}

func (v *bToCRenamer) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if _, keep := p.(*recipe.ExecutionContext).GetMessage(keepBKey); keep || ident.Name != "b" {
		return ident
	}
	return ident.WithName("c")
}

func TestFreshParseRunSeesTheMessagesTheExecutionContextStartedWith(t *testing.T) {
	ctx := recipe.NewExecutionContext()
	ctx.PutMessage(keepBKey, true)
	NewRecipeSpec().
		WithRecipe(&recipeList{recipes: []recipe.Recipe{
			&renameIdentifiers{rename: renameAToB},
			&renameBToCUnlessKept{},
		}}).
		WithExecutionContext(ctx).
		RewriteRun(t, Golang(`
			package main

			func a() {}
		`, `
			package main

			func b() {}
		`))
}

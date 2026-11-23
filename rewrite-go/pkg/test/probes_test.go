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
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// forgetProbes lets t probe as though no other test had, restoring what the
// process probed when t ends.
func forgetProbes(t *testing.T) {
	saved := probes
	probes = newProbeLog()
	t.Cleanup(func() { probes = saved })
}

func firstFunctionA() SourceSpec {
	return Golang(declaresA, `
		package main

		func first() {}
	`)
}

func TestProbeThatFailedIsNotRunAgain(t *testing.T) {
	forgetProbes(t)
	// given a recipe that failed its probe of an empty file in an earlier test
	spec := NewRecipeSpec().WithRecipe(&renameFirstFunction{})
	require.Len(t, failures(t, spec, firstFunctionA()), 1)

	// when another test runs it
	got := failures(t, spec, firstFunctionA())

	// then that test does not probe it again
	assert.Empty(t, got)
}

func TestProbeThatPassedIsNotRunAgain(t *testing.T) {
	forgetProbes(t)
	// given a recipe that passed its probe of an empty file in an earlier test
	declaringNone := 0
	spec := NewRecipeSpec().WithRecipe(&renameFirstFunction{checked: true, declaringNone: &declaringNone})
	spec.RewriteRun(t, firstFunctionA())
	require.Equal(t, 1, declaringNone)

	// when another test runs it
	spec.RewriteRun(t, firstFunctionA())

	// then that test does not run it on an empty file again
	assert.Equal(t, 1, declaringNone)
}

func TestProbeOfAMissingFileThatFailedIsNotRunAgain(t *testing.T) {
	forgetProbes(t)
	// given a scanning recipe that failed its probe without go.mod in an earlier test
	spec := NewRecipeSpec().WithRecipe(&reportInternalImports{})
	require.Len(t, failures(t, spec, internalImportsProject()), 1)

	// when another test runs it
	got := failures(t, spec, internalImportsProject())

	// then that test does not probe it again
	assert.Empty(t, got)
}

func TestRecipeIsProbedAfterTheAfterRecipeCallbacks(t *testing.T) {
	forgetProbes(t)
	// given a recipe that counts the files it meets that declare nothing, which only its probe gives it
	declaringNone := 0
	spec := NewRecipeSpec().WithRecipe(&renameFirstFunction{checked: true, declaringNone: &declaringNone})

	// when a test with an AfterRecipe callback runs it
	seenByAfterRecipe := -1
	spec.RewriteRun(t, firstFunctionA().WithAfterRecipe(func(*testing.T, *golang.CompilationUnit) {
		seenByAfterRecipe = declaringNone
	}))

	// then the callback runs before the probe
	assert.Equal(t, 0, seenByAfterRecipe)
	assert.Equal(t, 1, declaringNone)
}

func TestRecipeWithOtherOptionsIsProbedAgain(t *testing.T) {
	forgetProbes(t)
	// given a recipe that passed its probe of an empty file in an earlier test
	NewRecipeSpec().WithRecipe(&renameFirstFunction{checked: true}).RewriteRun(t, firstFunctionA())

	// when another test runs it with another option value
	got := failures(t, NewRecipeSpec().WithRecipe(&renameFirstFunction{}), firstFunctionA())

	// then that test probes it, as another recipe
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "org.openrewrite.golang.test.RenameFirstFunction panicked when run on a "+
		"golang.CompilationUnit at test.go holding only `package main`")
}

func TestRecipeListOfOtherRecipesIsProbedAgain(t *testing.T) {
	forgetProbes(t)
	// given a recipe list that passed its probe of an empty file in an earlier test
	NewRecipeSpec().WithRecipe(&recipeList{recipes: []recipe.Recipe{&renameFirstFunction{checked: true}}}).
		RewriteRun(t, firstFunctionA())

	// when another test runs a list of the same name, of the recipe with another option value
	got := failures(t, NewRecipeSpec().WithRecipe(&recipeList{recipes: []recipe.Recipe{&renameFirstFunction{}}}),
		firstFunctionA())

	// then that test probes it, as another recipe
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "org.openrewrite.golang.test.RecipeList panicked when run on a "+
		"golang.CompilationUnit at test.go holding only `package main`")
}

func TestRecipeIsProbedAgainOnAnotherKindOfSource(t *testing.T) {
	forgetProbes(t)
	// given a recipe that passed its probe of an empty go.mod in an earlier test
	spec := NewRecipeSpec().WithRecipe(&recipeList{recipes: []recipe.Recipe{
		&reportInternalImports{checked: true}, &renameFirstFunction{},
	}})
	require.Empty(t, failures(t, spec, GoMod(`
		module example.com/app
	`, `
		/*~~(example.com/app)~~>*/module example.com/app
	`)))

	// when another test runs it on a Go file
	got := failures(t, spec, firstFunctionA())

	// then that test probes it on an empty Go file
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "org.openrewrite.golang.test.RecipeList panicked when run on a "+
		"golang.CompilationUnit at test.go holding only `package main`")
}

func TestRecipeIsNotProbedOnceItsProbesTookTwentySeconds(t *testing.T) {
	forgetProbes(t)
	// given a recipe whose probes have taken more than 20 seconds, with any options
	probes.charge(&renameFirstFunction{checked: true}, 21*time.Second)

	// when a test runs it on a file its probe of an empty file fails on
	got := failures(t, NewRecipeSpec().WithRecipe(&renameFirstFunction{}), firstFunctionA())

	// then it is not probed
	assert.Empty(t, got)
}

func TestProbesCountTowardTheTimeCap(t *testing.T) {
	forgetProbes(t)
	// given a recipe whose probes have taken exactly 20 seconds
	probes.charge(&renameFirstFunction{checked: true}, probeTimeCap)

	// when a test probes it once more
	NewRecipeSpec().WithRecipe(&renameFirstFunction{checked: true}).RewriteRun(t, firstFunctionA())

	// then a later test of it, on a file its probe of an empty file fails on, does not probe it
	assert.Empty(t, failures(t, NewRecipeSpec().WithRecipe(&renameFirstFunction{}), firstFunctionA()))
}

func TestCompositeIsCappedByTheRecipesItRuns(t *testing.T) {
	forgetProbes(t)
	// given a recipe list of one recipe whose probes have taken more than 20 seconds
	probes.charge(&recipeList{recipes: []recipe.Recipe{&renameFirstFunction{}}}, 21*time.Second)

	// when lists of the same name run that recipe and another
	same := failures(t, NewRecipeSpec().WithRecipe(&recipeList{recipes: []recipe.Recipe{
		&renameFirstFunction{},
	}}), firstFunctionA())
	other := failures(t, NewRecipeSpec().WithRecipe(&recipeList{recipes: []recipe.Recipe{
		&reportFirstDirective{},
	}}), GoMod(`
		module example.com/app
	`, `
		/*~~(first: module)~~>*/module example.com/app
	`))

	// then only the list of the other recipe is probed
	assert.Empty(t, same)
	assert.Len(t, other, 1)
}

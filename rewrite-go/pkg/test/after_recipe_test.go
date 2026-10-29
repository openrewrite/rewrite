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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func functionName(t *testing.T, cu *golang.CompilationUnit) string {
	t.Helper()
	require.NotEmpty(t, cu.Statements)
	fn, ok := cu.Statements[0].Element.(*java.MethodDeclaration)
	require.Truef(t, ok, "expected a function declaration, got %T", cu.Statements[0].Element)
	return fn.Name.Name
}

func TestAfterRecipeSeesTheRecipeResult(t *testing.T) {
	called := false
	NewRecipeSpec().
		WithRecipe(&renameIdentifiers{rename: oneStepAlong}).
		WithCycles(1).
		RewriteRun(t, Golang(`
			package main

			func a() {}
		`, `
			package main

			func b() {}
		`).WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
			called = true
			assert.Equal(t, "b", functionName(t, cu))
		}))
	assert.True(t, called, "AfterRecipe was not called")
}

func TestAfterRecipeSeesTheParsedSourceWithoutARecipe(t *testing.T) {
	called := false
	NewRecipeSpec().RewriteRun(t, Golang(`
		package main

		func a() {}
	`).WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
		called = true
		assert.Equal(t, "a", functionName(t, cu))
	}))
	assert.True(t, called, "AfterRecipe was not called")
}

func TestAfterRecipeSeesAGeneratedSource(t *testing.T) {
	called := false
	NewRecipeSpec().
		WithRecipe(&generateAlways{}).
		WithCycles(1).
		RewriteRun(t, Generated("tools.go", "package app\n").WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
			called = true
			assert.Equal(t, "tools.go", cu.SourcePath)
		}))
	assert.True(t, called, "AfterRecipe was not called")
}

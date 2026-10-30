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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type visitedFile struct {
	Path string
}

var visitedFiles = recipe.NewDataTable[visitedFile](
	"org.openrewrite.golang.test.VisitedFiles",
	"Visited files",
	"The source files a recipe visited.",
	[]recipe.ColumnDescriptor{{Name: "path", DisplayName: "Path", Description: "The path of the source file."}},
)

var unwrittenFiles = recipe.NewDataTable[visitedFile](
	"org.openrewrite.golang.test.UnwrittenFiles",
	"Unwritten files",
	"A data table no recipe writes to.",
	[]recipe.ColumnDescriptor{{Name: "path", DisplayName: "Path", Description: "The path of the source file."}},
)

type recordVisits struct {
	recipe.Base
}

func (r *recordVisits) Name() string        { return "org.openrewrite.golang.test.RecordVisits" }
func (r *recordVisits) DisplayName() string { return "Record visits" }
func (r *recordVisits) Description() string { return "Writes a row for every source file it visits." }

func (r *recordVisits) Editor() recipe.TreeVisitor {
	return visitor.Init(&visitRecorder{})
}

type visitRecorder struct {
	visitor.GoVisitor
}

func (v *visitRecorder) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	visitedFiles.InsertRow(p.(*recipe.ExecutionContext), visitedFile{Path: cu.SourcePath})
	if java.HasMarker[java.SearchResult](cu.Markers) {
		return cu
	}
	return cu.WithMarkers(java.FoundSearchResult(cu.Markers, "visited"))
}

var visitedSource = Golang(`
	package main
`, `
	/*~~(visited)~~>*/package main
`).WithPath("a.go")

func TestDataTableHoldsTheRowsOfTheFirstCycle(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recordVisits{})
	ExpectDataTable(spec, visitedFiles, func(t *testing.T, rows []visitedFile) {
		assert.Equal(t, []visitedFile{{Path: "a.go"}}, rows)
	})
	spec.RewriteRun(t, visitedSource)
}

func TestDataTableRowsAreLeftOnTheExecutionContext(t *testing.T) {
	// given
	ctx := recipe.NewExecutionContext()

	// when
	NewRecipeSpec().WithRecipe(&recordVisits{}).WithExecutionContext(ctx).RewriteRun(t, visitedSource)

	// then
	store, ok := ctx.GetMessage(recipe.DataTableStoreKey)
	require.True(t, ok)
	assert.Equal(t, []any{visitedFile{Path: "a.go"}}, store.(recipe.DataTableStore).GetRows(visitedFiles.Descriptor().Name, ""))
}

func TestDataTableTheRecipeWritesNoRowsToFails(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&recordVisits{})
	ExpectDataTable(spec, unwrittenFiles, func(*testing.T, []visitedFile) {})

	// when
	got := failures(t, spec, visitedSource)

	// then
	assert.Equal(t, []string{"No data table named org.openrewrite.golang.test.UnwrittenFiles\n" +
		"Found data tables: org.openrewrite.golang.test.VisitedFiles"}, got)
}

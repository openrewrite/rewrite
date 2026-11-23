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
	"sort"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

type dataTableExpectation struct {
	table recipe.DataTableLike
	check func(t *testing.T, rows []any)
}

// ExpectDataTable asserts on the rows the first cycle of each subsequent
// RewriteRun of spec writes to table, failing the run when the recipe wrote
// none. Mirrors RewriteTest's spec.dataTable(...) on the Java side.
//
// Example:
//
//	spec := test.NewRecipeSpec().WithRecipe(&MyRecipe{})
//	test.ExpectDataTable(spec, mypkg.Findings, func(t *testing.T, rows []mypkg.FindingsRow) {
//	    assert.Len(t, rows, 1)
//	})
//	spec.RewriteRun(t, ...)
func ExpectDataTable[Row any](spec *RecipeSpec, table *recipe.DataTable[Row], allRows func(t *testing.T, rows []Row)) *RecipeSpec {
	spec.dataTables = append(spec.dataTables, dataTableExpectation{
		table: table,
		check: func(t *testing.T, rows []any) {
			typed := make([]Row, len(rows))
			for i, r := range rows {
				typed[i] = r.(Row)
			}
			allRows(t, typed)
		},
	})
	return spec
}

func (spec *RecipeSpec) expectDataTablesWritten(t testing.TB, store recipe.DataTableStore) {
	t.Helper()
	for _, e := range spec.dataTables {
		if len(rowsOf(store, e.table)) > 0 {
			continue
		}
		message := "No data table named " + e.table.Descriptor().Name
		if found := dataTableNames(store); len(found) > 0 {
			message += "\nFound data tables: " + strings.Join(found, ", ")
		}
		t.Error(message)
	}
}

func dataTableNames(store recipe.DataTableStore) []string {
	if store == nil {
		return nil
	}
	seen := map[string]bool{}
	var names []string
	for _, dt := range store.GetDataTables() {
		if n := dt.Descriptor().Name; !seen[n] {
			seen[n] = true
			names = append(names, n)
		}
	}
	sort.Strings(names)
	return names
}

func (spec *RecipeSpec) checkDataTables(t *testing.T, store recipe.DataTableStore) {
	t.Helper()
	for _, e := range spec.dataTables {
		e.check(t, rowsOf(store, e.table))
	}
}

func rowsOf(store recipe.DataTableStore, table recipe.DataTableLike) []any {
	if store == nil {
		return nil
	}
	return store.GetRows(table.Descriptor().Name, table.Group())
}

// discardingStore drops the rows of the cycles after the first, which write no
// data tables, as in Java.
type discardingStore struct{}

func (discardingStore) InsertRow(recipe.DataTableLike, *recipe.ExecutionContext, any) {}
func (discardingStore) GetRows(string, string) []any                                  { return nil }
func (discardingStore) GetDataTables() []recipe.DataTableLike                         { return nil }

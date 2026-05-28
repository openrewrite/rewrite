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
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

type findingsRow struct {
	File  string
	Count int
}

var findings = recipe.NewDataTable[findingsRow](
	"org.example.MyRecipe.Findings",
	"My findings",
	"What MyRecipe found",
	[]recipe.ColumnDescriptor{
		{Name: "File", DisplayName: "File", Description: "The file"},
		{Name: "Count", DisplayName: "Count", Description: "Number of hits", Type: "Integer"},
	},
)

func TestInMemoryDataTableStoreRoundTrip(t *testing.T) {
	ctx := recipe.NewExecutionContext()
	ctx.PutMessage(recipe.DataTableStoreKey, recipe.NewInMemoryDataTableStore())

	findings.InsertRow(ctx, findingsRow{File: "a.go", Count: 3})
	findings.InsertRow(ctx, findingsRow{File: "b.go", Count: 1})

	store, _ := ctx.GetMessage(recipe.DataTableStoreKey)
	rows := store.(recipe.DataTableStore).GetRows("org.example.MyRecipe.Findings", "")
	if len(rows) != 2 {
		t.Fatalf("expected 2 rows, got %d", len(rows))
	}
	if r, ok := rows[0].(findingsRow); !ok || r.File != "a.go" || r.Count != 3 {
		t.Fatalf("unexpected row 0: %+v", rows[0])
	}
}

func TestDataTableLazyStoreCreation(t *testing.T) {
	ctx := recipe.NewExecutionContext()
	// No store installed — InsertRow should create an InMemory one.
	findings.InsertRow(ctx, findingsRow{File: "c.go", Count: 7})

	store, ok := ctx.GetMessage(recipe.DataTableStoreKey)
	if !ok {
		t.Fatal("expected store to be lazily created")
	}
	if _, ok := store.(*recipe.InMemoryDataTableStore); !ok {
		t.Fatalf("expected InMemoryDataTableStore, got %T", store)
	}
	rows := store.(recipe.DataTableStore).GetRows("org.example.MyRecipe.Findings", "")
	if len(rows) != 1 {
		t.Fatalf("expected 1 row, got %d", len(rows))
	}
}

func TestCsvDataTableStoreWritesFile(t *testing.T) {
	dir := t.TempDir()
	store, err := recipe.NewCsvDataTableStore(dir)
	if err != nil {
		t.Fatal(err)
	}
	defer store.Close()

	ctx := recipe.NewExecutionContext()
	ctx.PutMessage(recipe.DataTableStoreKey, store)

	findings.InsertRow(ctx, findingsRow{File: "x.go", Count: 5})
	findings.InsertRow(ctx, findingsRow{File: "y,go", Count: 9}) // tests CSV escaping
	store.Close()

	entries, err := os.ReadDir(dir)
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 1 {
		t.Fatalf("expected 1 CSV file, got %d", len(entries))
	}
	csvPath := filepath.Join(dir, entries[0].Name())
	data, err := os.ReadFile(csvPath)
	if err != nil {
		t.Fatal(err)
	}
	got := string(data)
	for _, want := range []string{
		"# @name org.example.MyRecipe.Findings",
		"File,Count",
		"x.go,5",
		`"y,go",9`, // comma in field forces quoting
	} {
		if !strings.Contains(got, want) {
			t.Errorf("CSV missing expected line %q; got:\n%s", want, got)
		}
	}
}

func TestSanitizeScope(t *testing.T) {
	got := recipe.SanitizeScope("org.openrewrite.Foo$Bar")
	// lowercase + non-alnum→dash + 4-char hash suffix (matching JS spec)
	if !strings.HasPrefix(got, "org-openrewrite-foo-bar-") {
		t.Errorf("unexpected sanitized value: %s", got)
	}
}

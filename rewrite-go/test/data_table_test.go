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

func TestCsvDataTableStoreWritesPrefixAndSuffixColumns(t *testing.T) {
	dir := t.TempDir()
	store, err := recipe.NewCsvDataTableStoreWithColumns(dir,
		[]recipe.ColumnValue{{Name: "repositoryPath", Value: "acme/widgets"}},
		[]recipe.ColumnValue{{Name: "organization", Value: "acme"}},
	)
	if err != nil {
		t.Fatal(err)
	}
	defer store.Close()

	ctx := recipe.NewExecutionContext()
	ctx.PutMessage(recipe.DataTableStoreKey, store)

	findings.InsertRow(ctx, findingsRow{File: "x.go", Count: 5})

	entries, err := os.ReadDir(dir)
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 1 {
		t.Fatalf("expected 1 CSV file, got %d", len(entries))
	}
	data, err := os.ReadFile(filepath.Join(dir, entries[0].Name()))
	if err != nil {
		t.Fatal(err)
	}
	got := string(data)
	for _, want := range []string{
		"repositoryPath,File,Count,organization",
		"acme/widgets,x.go,5,acme",
	} {
		if !strings.Contains(got, want) {
			t.Errorf("CSV missing expected line %q; got:\n%s", want, got)
		}
	}
}

func TestCsvDataTableStoresShareOneFileWithSingleHeader(t *testing.T) {
	dir := t.TempDir()
	prefix := []recipe.ColumnValue{{Name: "repositoryPath", Value: "acme/widgets"}}

	// Two stores model the Java host and an RPC runtime sharing one file.
	store1, err := recipe.NewCsvDataTableStoreWithColumns(dir, prefix, nil)
	if err != nil {
		t.Fatal(err)
	}
	store2, err := recipe.NewCsvDataTableStoreWithColumns(dir, prefix, nil)
	if err != nil {
		t.Fatal(err)
	}

	ctx1 := recipe.NewExecutionContext()
	ctx1.PutMessage(recipe.DataTableStoreKey, store1)
	findings.InsertRow(ctx1, findingsRow{File: "a.go", Count: 1})

	ctx2 := recipe.NewExecutionContext()
	ctx2.PutMessage(recipe.DataTableStoreKey, store2)
	findings.InsertRow(ctx2, findingsRow{File: "b.go", Count: 2})

	entries, err := os.ReadDir(dir)
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 1 {
		t.Fatalf("expected the two stores to share 1 CSV file, got %d", len(entries))
	}
	data, err := os.ReadFile(filepath.Join(dir, entries[0].Name()))
	if err != nil {
		t.Fatal(err)
	}
	got := string(data)
	if n := strings.Count(got, "# @name "); n != 1 {
		t.Errorf("expected exactly 1 comment preamble, got %d; content:\n%s", n, got)
	}
	if n := strings.Count(got, "repositoryPath,File,Count"); n != 1 {
		t.Errorf("expected exactly 1 header row, got %d; content:\n%s", n, got)
	}
	for _, want := range []string{"acme/widgets,a.go,1", "acme/widgets,b.go,2"} {
		if !strings.Contains(got, want) {
			t.Errorf("CSV missing expected row %q; got:\n%s", want, got)
		}
	}
}

func TestSanitizeScope(t *testing.T) {
	got := recipe.SanitizeScope("org.openrewrite.Foo$Bar")
	// lowercase + non-alnum→dash + 4-char hash suffix (matching JS spec)
	if !strings.HasPrefix(got, "org-openrewrite-foo-bar-") {
		t.Errorf("unexpected sanitized value: %s", got)
	}
	// Byte-identical to Java's CsvDataTableStore.sanitize; verified with `shasum -a 256`.
	if want := "org-openrewrite-foo-bar-fa3f"; got != want {
		t.Errorf("SanitizeScope cross-language mismatch: got %q, want %q", got, want)
	}
}

// fileKeyOf returns the on-disk file name (minus ".csv") produced for dt — the
// only way to exercise the unexported fileKey end-to-end.
func fileKeyOf(t *testing.T, dt *recipe.DataTable[findingsRow]) string {
	t.Helper()
	dir := t.TempDir()
	store, err := recipe.NewCsvDataTableStore(dir)
	if err != nil {
		t.Fatal(err)
	}
	ctx := recipe.NewExecutionContext()
	ctx.PutMessage(recipe.DataTableStoreKey, store)
	dt.InsertRow(ctx, findingsRow{File: "a.go", Count: 1})

	entries, err := os.ReadDir(dir)
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 1 {
		t.Fatalf("expected exactly 1 CSV file, got %d", len(entries))
	}
	return strings.TrimSuffix(entries[0].Name(), ".csv")
}

// TestFileKeyMatchesJavaScheme verifies a table shared by a Java and a Go
// recipe resolves to the same on-disk file name (expected strings = what
// Java's CsvDataTableStore.fileKey produces).
func TestFileKeyMatchesJavaScheme(t *testing.T) {
	const name = "org.openrewrite.table.TextMatches"
	const displayName = "Text matches"
	cols := []recipe.ColumnDescriptor{{Name: "File", DisplayName: "File", Description: "The file"}}
	newTable := func() *recipe.DataTable[findingsRow] {
		return recipe.NewDataTable[findingsRow](name, displayName, "desc", cols)
	}

	// A DEFAULT table must yield the bare FQN — the Java host writes it there.
	t.Run("default table -> bare FQN", func(t *testing.T) {
		got := fileKeyOf(t, newTable())
		if got != name {
			t.Errorf("default table fileKey = %q, want bare FQN %q", got, name)
		}
	})

	t.Run("custom instance name -> name--sanitize(instanceName)", func(t *testing.T) {
		dt := newTable()
		dt.SetInstanceName("Custom run")
		got := fileKeyOf(t, dt)
		want := name + "--custom-run-6251"
		if want != name+"--"+recipe.SanitizeScope("Custom run") {
			t.Fatalf("test setup: literal want %q out of sync with SanitizeScope", want)
		}
		if got != want {
			t.Errorf("custom-instance fileKey = %q, want %q", got, want)
		}
	})

	t.Run("grouped table -> name--sanitize(group)", func(t *testing.T) {
		dt := newTable()
		dt.SetGroup("team-alpha")
		got := fileKeyOf(t, dt)
		want := name + "--team-alpha-29c4"
		if want != name+"--"+recipe.SanitizeScope("team-alpha") {
			t.Fatalf("test setup: literal want %q out of sync with SanitizeScope", want)
		}
		if got != want {
			t.Errorf("grouped fileKey = %q, want %q", got, want)
		}
	})

	t.Run("group == name -> bare FQN", func(t *testing.T) {
		dt := newTable()
		dt.SetGroup(name)
		got := fileKeyOf(t, dt)
		if got != name {
			t.Errorf("group==name fileKey = %q, want bare FQN %q", got, name)
		}
	})
}

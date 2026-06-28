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

package main

import (
	"encoding/json"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

func TestParseOrderedColumnsPreservesOrder(t *testing.T) {
	raw := json.RawMessage(`{"zeta":"1","alpha":"2","mike":"3"}`)
	cols, err := parseOrderedColumns(raw)
	if err != nil {
		t.Fatal(err)
	}
	want := []recipe.ColumnValue{
		{Name: "zeta", Value: "1"},
		{Name: "alpha", Value: "2"},
		{Name: "mike", Value: "3"},
	}
	if len(cols) != len(want) {
		t.Fatalf("expected %d columns, got %d: %+v", len(want), len(cols), cols)
	}
	for i := range want {
		if cols[i] != want[i] {
			t.Errorf("column %d = %+v, want %+v", i, cols[i], want[i])
		}
	}
}

func TestParseOrderedColumnsEmptyAndNull(t *testing.T) {
	for _, raw := range []json.RawMessage{nil, json.RawMessage("null"), json.RawMessage("{}")} {
		cols, err := parseOrderedColumns(raw)
		if err != nil {
			t.Fatalf("unexpected error for %q: %v", string(raw), err)
		}
		if len(cols) != 0 {
			t.Errorf("expected no columns for %q, got %+v", string(raw), cols)
		}
	}
}

func TestToDataTableStoreKinds(t *testing.T) {
	dir := t.TempDir()

	csv := &csvDataTableStore{
		OutputDir:     dir,
		PrefixColumns: json.RawMessage(`{"repositoryPath":"acme/widgets"}`),
		SuffixColumns: json.RawMessage(`{"organization":"acme"}`),
	}
	store, err := csv.toDataTableStore()
	if err != nil {
		t.Fatal(err)
	}
	if _, ok := store.(*recipe.CsvDataTableStore); !ok {
		t.Fatalf("CSV kind: expected *CsvDataTableStore, got %T", store)
	}

	store, err = (noOpDataTableStore{}).toDataTableStore()
	if err != nil {
		t.Fatal(err)
	}
	if _, ok := store.(*recipe.InMemoryDataTableStore); !ok {
		t.Fatalf("NOOP kind: expected *InMemoryDataTableStore, got %T", store)
	}

	store, err = (&csvDataTableStore{}).toDataTableStore()
	if err != nil {
		t.Fatal(err)
	}
	if _, ok := store.(*recipe.InMemoryDataTableStore); !ok {
		t.Fatalf("under-specified CSV: expected *InMemoryDataTableStore, got %T", store)
	}
}

func TestParseSetDataTableStoreSelectsVariantByKind(t *testing.T) {
	csv, err := parseSetDataTableStore(json.RawMessage(
		`{"kind":"CSV","outputDir":"/tmp/dt","prefixColumns":{"repositoryOrigin":"acme"}}`))
	if err != nil {
		t.Fatal(err)
	}
	c, ok := csv.(*csvDataTableStore)
	if !ok {
		t.Fatalf("CSV: expected *csvDataTableStore, got %T", csv)
	}
	if c.OutputDir != "/tmp/dt" {
		t.Errorf("OutputDir = %q, want /tmp/dt", c.OutputDir)
	}

	noop, err := parseSetDataTableStore(json.RawMessage(`{"kind":"NOOP"}`))
	if err != nil {
		t.Fatal(err)
	}
	if _, ok := noop.(noOpDataTableStore); !ok {
		t.Fatalf("NOOP: expected noOpDataTableStore, got %T", noop)
	}
}

func TestHandleSetDataTableStoreInstallsConfiguredStore(t *testing.T) {
	dir := t.TempDir()
	s := &server{}

	params, _ := json.Marshal(map[string]string{"kind": "CSV", "outputDir": dir})
	result, rpcErr := s.handleSetDataTableStore(params)
	if rpcErr != nil {
		t.Fatalf("unexpected rpc error: %+v", rpcErr)
	}
	if result != true {
		t.Errorf("expected result true, got %v", result)
	}
	if _, ok := s.configuredDataTableStore.(*recipe.CsvDataTableStore); !ok {
		t.Fatalf("expected configured *CsvDataTableStore, got %T", s.configuredDataTableStore)
	}

	ctx := recipe.NewExecutionContext()
	s.installDataTableStore(ctx)
	installed, ok := ctx.GetMessage(recipe.DataTableStoreKey)
	if !ok {
		t.Fatal("expected a store installed on the ctx")
	}
	if installed != s.configuredDataTableStore {
		t.Errorf("installed store %p != configured store %p", installed, s.configuredDataTableStore)
	}
}

func TestInstallDataTableStoreNoopWhenUnconfigured(t *testing.T) {
	s := &server{}
	ctx := recipe.NewExecutionContext()
	s.installDataTableStore(ctx)
	if _, ok := ctx.GetMessage(recipe.DataTableStoreKey); ok {
		t.Error("expected no store installed when host configured none")
	}
}

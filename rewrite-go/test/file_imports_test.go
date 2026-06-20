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
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
)

// TestFileImports verifies imports-only extraction works regardless of build
// constraints (the file here is windows-gated) and handles aliased, blank,
// and grouped imports.
func TestFileImports(t *testing.T) {
	src := "//go:build windows\n\n" +
		"package sys\n\n" +
		"import (\n" +
		"\t\"fmt\"\n" +
		"\t_ \"embed\"\n" +
		"\talias \"golang.org/x/sys/windows\"\n" +
		")\n\n" +
		"import \"strings\"\n"

	got := parser.FileImports(src)
	sort.Strings(got)
	want := []string{"embed", "fmt", "golang.org/x/sys/windows", "strings"}
	if len(got) != len(want) {
		t.Fatalf("got %v, want %v", got, want)
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("got %v, want %v", got, want)
		}
	}
}

// TestFileImportsMalformed returns nil (not a panic) for unparseable source.
func TestFileImportsMalformed(t *testing.T) {
	if got := parser.FileImports("package @@@ broken"); got != nil {
		t.Errorf("expected nil for malformed source, got %v", got)
	}
}

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

//go:build parityaudit

package printer_test

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
)

// TestPrinterCorpus walks every .go file under
// test/printer-corpus/ and asserts parse → print byte-equality. Each
// failure is a printer bug to fix in pkg/printer/go_printer.go.
//
// Gated behind the `parityaudit` build tag (see Makefile target
// `make parity`). Default test runs skip this entirely so corpus
// triage doesn't block CI.
func TestPrinterCorpus(t *testing.T) {
	root := findCorpusRoot(t)

	var fixtures []string
	if err := filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			return nil
		}
		if strings.HasSuffix(path, ".go") {
			fixtures = append(fixtures, path)
		}
		return nil
	}); err != nil {
		t.Fatalf("walk corpus: %v", err)
	}
	if len(fixtures) == 0 {
		t.Fatalf("no .go fixtures found under %s", root)
	}

	for _, f := range fixtures {
		f := f
		name, _ := filepath.Rel(root, f)
		t.Run(name, func(t *testing.T) {
			content, err := os.ReadFile(f)
			if err != nil {
				t.Fatalf("read: %v", err)
			}
			cu, err := parser.NewGoParser().Parse(filepath.Base(f), string(content))
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			printed := printer.Print(cu)
			if printed != string(content) {
				t.Errorf("byte-equality failed\n--- expected ---\n%s\n--- actual ---\n%s",
					string(content), printed)
			}
		})
	}
}

// findCorpusRoot resolves test/testdata/printer-corpus relative to this
// test file. The corpus lives under testdata/ so the standard Go tooling
// (`go test ./...`) skips it — Go treats `testdata/` as a magic
// directory that's ignored when discovering packages, even though our
// fixtures are valid `.go` files.
func findCorpusRoot(t *testing.T) string {
	t.Helper()
	// pkg/printer/parity_test.go → ../../test/testdata/printer-corpus
	candidates := []string{
		filepath.Join("..", "..", "test", "testdata", "printer-corpus"),
	}
	for _, c := range candidates {
		if info, err := os.Stat(c); err == nil && info.IsDir() {
			return c
		}
	}
	t.Fatalf("printer-corpus directory not found relative to %s", mustGetwd(t))
	return ""
}

func mustGetwd(t *testing.T) string {
	t.Helper()
	wd, err := os.Getwd()
	if err != nil {
		t.Fatalf("getwd: %v", err)
	}
	return wd
}

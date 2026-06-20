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

package golang

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TestScannerHarvestsPlainTextGoImports verifies the scan phase reads imports
// out of a build-excluded `.go` file that the CLI represented as PlainText
// (e.g. a `//go:build windows` file on Linux), so go mod tidy doesn't prune a
// platform-only dependency it can no longer "see".
func TestScannerHarvestsPlainTextGoImports(t *testing.T) {
	r := &GoModTidy{}
	acc := r.InitialValue(nil).(*tidyAcc)
	scan := r.Scanner(acc)

	scan.Visit(&java.PlainText{
		SourcePath: "internal/sys_windows.go",
		Text:       "//go:build windows\n\npackage sys\n\nimport (\n\t\"golang.org/x/sys/windows\"\n\t\"fmt\"\n)\n",
	}, nil)

	if !acc.rawImports["golang.org/x/sys/windows"] {
		t.Errorf("expected windows-only import to be harvested from PlainText; got %v", acc.rawImports)
	}
	if !acc.rawImports["fmt"] {
		t.Errorf("expected fmt import to be harvested from PlainText; got %v", acc.rawImports)
	}
}

// TestScannerIgnoresNonGoPlainText verifies non-.go PlainText files (README,
// go.sum, …) are left alone — only `.go` text is parsed for imports.
func TestScannerIgnoresNonGoPlainText(t *testing.T) {
	r := &GoModTidy{}
	acc := r.InitialValue(nil).(*tidyAcc)
	scan := r.Scanner(acc)

	scan.Visit(&java.PlainText{
		SourcePath: "README.md",
		Text:       "import \"this is not go\"\n",
	}, nil)

	if len(acc.rawImports) != 0 {
		t.Errorf("expected no imports harvested from a non-.go PlainText; got %v", acc.rawImports)
	}
}

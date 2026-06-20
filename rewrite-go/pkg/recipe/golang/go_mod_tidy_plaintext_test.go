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

func hasImport(imps []string, want string) bool {
	for _, i := range imps {
		if i == want {
			return true
		}
	}
	return false
}

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

	imps := acc.fileImports["internal/sys_windows.go"]
	if !hasImport(imps, "golang.org/x/sys/windows") {
		t.Errorf("expected windows-only import to be harvested from PlainText; got %v", imps)
	}
	if !hasImport(imps, "fmt") {
		t.Errorf("expected fmt import to be harvested from PlainText; got %v", imps)
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

	if len(acc.fileImports) != 0 {
		t.Errorf("expected no imports harvested from a non-.go PlainText; got %v", acc.fileImports)
	}
}

// TestOwnedImportsScopesByModule verifies a nested module's files are NOT
// attributed to the root module — the prometheus `internal/tools` regression,
// where a nested `//go:build tools` file's import leaked into the root go.mod
// and was misclassified as a direct dependency.
func TestOwnedImportsScopesByModule(t *testing.T) {
	acc := &tidyAcc{
		goModDirs:        map[string]bool{"": true, "internal/tools": true},
		modulePathByDir:  map[string]string{"": "example.com/root", "internal/tools": "example.com/root/internal/tools"},
		requireModsByDir: map[string]map[string]bool{},
		fileImports: map[string][]string{
			"main.go":                   {"github.com/spf13/cobra"},
			"internal/tools/tools.go":   {"github.com/grpc-ecosystem/grpc-gateway/v2/protoc-gen-grpc-gateway"},
			"internal/helper/helper.go": {"github.com/root/own"}, // belongs to root (no nested go.mod here)
		},
	}
	ed := &goModTidyEditor{acc: acc}

	root := ed.ownedImports("")
	if !hasImport(root, "github.com/spf13/cobra") || !hasImport(root, "github.com/root/own") {
		t.Errorf("root module should own main.go and internal/helper imports; got %v", root)
	}
	if hasImport(root, "github.com/grpc-ecosystem/grpc-gateway/v2/protoc-gen-grpc-gateway") {
		t.Errorf("root module must NOT own the nested internal/tools import; got %v", root)
	}

	tools := ed.ownedImports("internal/tools")
	if !hasImport(tools, "github.com/grpc-ecosystem/grpc-gateway/v2/protoc-gen-grpc-gateway") {
		t.Errorf("internal/tools module should own tools.go import; got %v", tools)
	}
	if hasImport(tools, "github.com/spf13/cobra") {
		t.Errorf("internal/tools module must NOT own root imports; got %v", tools)
	}
}

/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
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
	"os"
	"path/filepath"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// TestParseProjectRelativizesGoModSourcePath pins down that a GoMod LST
// produced by ParseProject carries a project-root-relative SourcePath, just
// like compilation units do. The Java side reads SourcePath off the
// serialized object (not the response item), so the object's own field must
// be relativized — not just the response item's.
func TestParseProjectRelativizesGoModSourcePath(t *testing.T) {
	// given
	s, _ := newTestServer(t)
	projectDir := t.TempDir()
	writeFile(t, filepath.Join(projectDir, "go.mod"), "module example.com/foo\n\ngo 1.22\n")
	writeFile(t, filepath.Join(projectDir, "main.go"), "package main\n\nfunc main() {}\n")

	relativeTo := projectDir
	params, err := json.Marshal(parseProjectRequest{
		ProjectPath: projectDir,
		RelativeTo:  &relativeTo,
	})
	if err != nil {
		t.Fatalf("marshal params: %v", err)
	}

	// when
	if _, rpcErr := s.handleParseProject(params); rpcErr != nil {
		t.Fatalf("handleParseProject: %v", rpcErr.Message)
	}

	// then
	var gm *golang.GoMod
	for _, obj := range s.localObjects {
		if g, ok := obj.(*golang.GoMod); ok {
			gm = g
			break
		}
	}
	if gm == nil {
		t.Fatal("expected a GoMod object to be produced")
	}
	if gm.SourcePath != "go.mod" {
		t.Fatalf("expected GoMod SourcePath relativized to %q, got %q", "go.mod", gm.SourcePath)
	}
}

func writeFile(t *testing.T, path, content string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
		t.Fatalf("mkdir: %v", err)
	}
	if err := os.WriteFile(path, []byte(content), 0644); err != nil {
		t.Fatalf("write %s: %v", path, err)
	}
}

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

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
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
	require.NoError(t, err)

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
	require.NotNil(t, gm)
	require.Equal(t, "go.mod", gm.SourcePath)
}

func writeFile(t *testing.T, path, content string) {
	t.Helper()
	require.NoError(t, os.MkdirAll(filepath.Dir(path), 0755))
	if err := os.WriteFile(path, []byte(content), 0644); err != nil {
		t.Fatalf("write %s: %v", path, err)
	}
}

// TestParseProjectRelativizesGoResolutionResultPath pins down that the GoResolutionResult
// marker riding on a GoMod carries a project-root-relative Path, like every other path on
// the tree. It was built from the absolute path the directory walk produced while the
// GoMod's own SourcePath was relativized just below, so a recipe correlating the two
// silently matched nothing and fell back to unqualified results.
func TestParseProjectRelativizesGoResolutionResultPath(t *testing.T) {
	// given
	s, _ := newTestServer(t)
	projectDir := t.TempDir()
	writeFile(t, filepath.Join(projectDir, "go.mod"), "module example.com/foo\n\ngo 1.22\n")
	writeFile(t, filepath.Join(projectDir, "main.go"), "package main\n\nfunc main() {}\n")
	writeFile(t, filepath.Join(projectDir, "go.sum"), "")
	writeFile(t, filepath.Join(projectDir, "examples", "go.mod"), "module example.com/foo/examples\n\ngo 1.22\n")
	writeFile(t, filepath.Join(projectDir, "examples", "demo.go"), "package examples\n")

	relativeTo := projectDir
	params, err := json.Marshal(parseProjectRequest{
		ProjectPath: projectDir,
		RelativeTo:  &relativeTo,
	})
	require.NoError(t, err)

	// when
	if _, rpcErr := s.handleParseProject(params); rpcErr != nil {
		t.Fatalf("handleParseProject: %v", rpcErr.Message)
	}

	// then
	onGoMod, onGoSum := 0, 0
	for _, obj := range s.localObjects {
		switch sf := obj.(type) {
		case *golang.GoMod:
			for _, mrr := range resolutionResults(sf.Markers.Entries) {
				onGoMod++
				assertMarkerPath(t, mrr.Path, sf.SourcePath, sf.SourcePath)
			}
		case *golang.GoSum:
			for _, mrr := range resolutionResults(sf.Markers.Entries) {
				onGoSum++
				assertMarkerPath(t, mrr.Path, filepath.Join(filepath.Dir(sf.SourcePath), "go.mod"), sf.SourcePath)
			}
		}
	}
	require.False(t, onGoMod != 2 || onGoSum != 1)
}

func resolutionResults(entries []java.Marker) []golang.GoResolutionResult {
	var found []golang.GoResolutionResult
	for _, entry := range entries {
		if mrr, ok := entry.(golang.GoResolutionResult); ok {
			found = append(found, mrr)
		}
	}
	return found
}

func assertMarkerPath(t *testing.T, got, want, attachedTo string) {
	t.Helper()
	if got != want {
		t.Errorf("marker on %q has Path %q, want %q", attachedTo, got, want)
	}
}

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
	"os/exec"
	"path/filepath"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// Reproduces https://github.com/openrewrite/rewrite/issues/8475
func TestParseProjectExcludesGitIgnored(t *testing.T) {
	if _, err := exec.LookPath("git"); err != nil {
		t.Skip("git not on PATH")
	}

	// given: a git repo that ignores build/, with generated Go files at the root
	// and nested, a force-added source under an ignored path, and a plain source
	s, _ := newTestServer(t)
	projectDir := t.TempDir()
	writeFile(t, filepath.Join(projectDir, ".gitignore"), "build/\n")
	writeFile(t, filepath.Join(projectDir, "go.mod"), "module example.com/m\n\ngo 1.22\n")
	writeFile(t, filepath.Join(projectDir, "probe.go"), "package m\n")
	writeFile(t, filepath.Join(projectDir, "build", "generated", "rootbuild.go"), "package generated\n")
	writeFile(t, filepath.Join(projectDir, "sub", "build", "generated", "subbuild.go"), "package generated\n")
	writeFile(t, filepath.Join(projectDir, "pkg", "build", "legit.go"), "package build\n")

	gitInit(t, projectDir)
	git(t, projectDir, "add", ".gitignore", "go.mod", "probe.go")
	git(t, projectDir, "add", "-f", filepath.Join("pkg", "build", "legit.go"))
	git(t, projectDir, "commit", "-qm", "init")

	relativeTo := projectDir
	params, err := json.Marshal(parseProjectRequest{ProjectPath: projectDir, RelativeTo: &relativeTo})
	if err != nil {
		t.Fatalf("marshal params: %v", err)
	}

	// when
	if _, rpcErr := s.handleParseProject(params); rpcErr != nil {
		t.Fatalf("handleParseProject: %v", rpcErr.Message)
	}

	// then
	parsed := map[string]bool{}
	for _, obj := range s.localObjects {
		if cu, ok := obj.(*golang.CompilationUnit); ok {
			parsed[filepath.ToSlash(cu.SourcePath)] = true
		}
	}
	for _, want := range []string{"probe.go", "pkg/build/legit.go"} {
		if !parsed[want] {
			t.Errorf("expected %q to be parsed, but it was not", want)
		}
	}
	for _, unwanted := range []string{"build/generated/rootbuild.go", "sub/build/generated/subbuild.go"} {
		if parsed[unwanted] {
			t.Errorf("expected gitignored %q to be excluded, but it was parsed", unwanted)
		}
	}
}

// TestFilterGitIgnoredFailsOpen pins the defensive behavior: with no git binary
// on PATH, and outside a git work tree, every candidate path is returned
// unchanged rather than dropped.
func TestFilterGitIgnoredFailsOpen(t *testing.T) {
	dir := t.TempDir()
	paths := []string{
		filepath.Join(dir, "a.go"),
		filepath.Join(dir, "build", "gen.go"),
	}

	// given: a real git binary but projectPath is not a work tree
	if _, err := exec.LookPath("git"); err == nil {
		if got := filterGitIgnored(dir, paths); len(got) != len(paths) {
			t.Errorf("not a git repo: expected all %d paths kept, got %d: %v", len(paths), len(got), got)
		}
	}

	// given: git is not resolvable on PATH at all
	t.Setenv("PATH", "")
	if got := filterGitIgnored(dir, paths); len(got) != len(paths) {
		t.Errorf("no git binary: expected all %d paths kept, got %d: %v", len(paths), len(got), got)
	}
}

func gitInit(t *testing.T, dir string) {
	t.Helper()
	git(t, dir, "init", "-q")
	git(t, dir, "config", "user.email", "test@example.com")
	git(t, dir, "config", "user.name", "test")
}

func git(t *testing.T, dir string, args ...string) {
	t.Helper()
	cmd := exec.Command("git", args...)
	cmd.Dir = dir
	if out, err := cmd.CombinedOutput(); err != nil {
		t.Fatalf("git %v: %v\n%s", args, err, out)
	}
}

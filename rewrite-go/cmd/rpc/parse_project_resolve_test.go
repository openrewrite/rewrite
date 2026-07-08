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
	"os/exec"
	"path/filepath"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// TestParseProjectResolvesModuleGraph pins the parse-time resolution wiring:
// handleParseProject must run the go toolchain against the module dir and land
// the resolved build list + package->module map on the GoResolutionResult
// marker. Uses a stdlib-only module so resolution needs no network.
func TestParseProjectResolvesModuleGraph(t *testing.T) {
	if _, err := exec.LookPath("go"); err != nil {
		t.Skip("go toolchain not on PATH")
	}

	// given: a dependency-free module on disk
	s, _ := newTestServer(t)
	projectDir := t.TempDir()
	writeFile(t, filepath.Join(projectDir, "go.mod"), "module example.com/foo\n\ngo 1.21\n")
	writeFile(t, filepath.Join(projectDir, "main.go"),
		"package main\n\nimport \"fmt\"\n\nfunc main() { fmt.Println(\"hi\") }\n")

	relativeTo := projectDir
	params, err := json.Marshal(parseProjectRequest{ProjectPath: projectDir, RelativeTo: &relativeTo})
	if err != nil {
		t.Fatalf("marshal params: %v", err)
	}

	// when
	if _, rpcErr := s.handleParseProject(params); rpcErr != nil {
		t.Fatalf("handleParseProject: %v", rpcErr.Message)
	}

	// then: the GoResolutionResult marker carries toolchain-resolved data
	mrr := findGoResolutionResult(t, s)

	var main *golang.GoResolvedDependency
	for i := range mrr.ResolvedDependencies {
		if mrr.ResolvedDependencies[i].ModulePath == "example.com/foo" {
			main = &mrr.ResolvedDependencies[i]
		}
	}
	if main == nil {
		t.Fatalf("main module missing from resolved build list: %+v", mrr.ResolvedDependencies)
	}
	if !main.Main {
		t.Errorf("main module should have Main=true: %+v", main)
	}

	var sawStdlib, sawMainPkg bool
	for _, p := range mrr.PackageModules {
		if p.ImportPath == "fmt" && p.Standard {
			sawStdlib = true
		}
		if p.ImportPath == "example.com/foo" && p.ModulePath == "example.com/foo" {
			sawMainPkg = true
		}
	}
	if !sawStdlib {
		t.Errorf("expected stdlib package fmt (Standard) in PackageModules: %+v", mrr.PackageModules)
	}
	if !sawMainPkg {
		t.Errorf("expected the main package mapped to its module in PackageModules: %+v", mrr.PackageModules)
	}
}

func findGoResolutionResult(t *testing.T, s *server) golang.GoResolutionResult {
	t.Helper()
	for _, obj := range s.localObjects {
		gm, ok := obj.(*golang.GoMod)
		if !ok {
			continue
		}
		for _, m := range gm.Markers.Entries {
			if mrr, ok := m.(golang.GoResolutionResult); ok {
				return mrr
			}
		}
	}
	t.Fatal("no GoResolutionResult marker found on any produced GoMod")
	return golang.GoResolutionResult{}
}

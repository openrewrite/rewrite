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
	"strings"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
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
	require.NoError(t, err, "marshal params")

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
	require.NotNilf(t, main, "main module missing from resolved build list: %+v", mrr.ResolvedDependencies)
	assert.True(t, main.Main, "main module should have Main=true")

	var sawStdlib, sawMainPkg bool
	for _, p := range mrr.PackageModules {
		if p.ImportPath == "fmt" && p.Standard {
			sawStdlib = true
		}
		if p.ImportPath == "example.com/foo" && p.ModulePath == "example.com/foo" {
			sawMainPkg = true
		}
	}
	assert.Truef(t, sawStdlib, "expected stdlib package fmt (Standard) in PackageModules: %+v", mrr.PackageModules)
	assert.Truef(t, sawMainPkg, "expected the main package mapped to its module in PackageModules: %+v", mrr.PackageModules)
}

func TestParseProjectResolvesTestOnlyDependency(t *testing.T) {
	if _, err := exec.LookPath("go"); err != nil {
		t.Skip("go toolchain not on PATH")
	}

	// given: a module whose only use of example.com/bar is from a _test.go file
	s, _ := newTestServer(t)
	projectDir := t.TempDir()
	writeFile(t, filepath.Join(projectDir, "go.mod"),
		"module example.com/foo\n\ngo 1.21\n\nrequire example.com/bar v0.0.0\n\nreplace example.com/bar => ./bar\n")
	writeFile(t, filepath.Join(projectDir, "main.go"),
		"package main\n\nimport \"fmt\"\n\nfunc main() { fmt.Println(\"hi\") }\n")
	writeFile(t, filepath.Join(projectDir, "main_test.go"),
		"package main\n\nimport (\n\t\"testing\"\n\n\t\"example.com/bar\"\n)\n\nfunc TestBar(t *testing.T) { _ = bar.Hello() }\n")
	writeFile(t, filepath.Join(projectDir, "bar", "go.mod"), "module example.com/bar\n\ngo 1.21\n")
	writeFile(t, filepath.Join(projectDir, "bar", "bar.go"),
		"package bar\n\nfunc Hello() string { return \"hi\" }\n")

	relativeTo := projectDir
	params, err := json.Marshal(parseProjectRequest{ProjectPath: projectDir, RelativeTo: &relativeTo})
	require.NoError(t, err, "marshal params")

	// when
	if _, rpcErr := s.handleParseProject(params); rpcErr != nil {
		t.Fatalf("handleParseProject: %v", rpcErr.Message)
	}

	// then: the test-only dependency is mapped to its providing module
	mrr := findGoResolutionResult(t, s)

	var sawTestDep bool
	for _, p := range mrr.PackageModules {
		if p.ImportPath == "example.com/bar" && p.ModulePath == "example.com/bar" {
			sawTestDep = true
		}
	}
	assert.Truef(t, sawTestDep, "expected test-only dependency example.com/bar in PackageModules: %+v", mrr.PackageModules)
}

// TestParseProjectDegradesToGoSumOnlyWhenModuleUnresolvable pins Defect 2 from
// moderneinc/customer-requests#3150: when the toolchain cannot resolve the build
// list (here forced with GOPROXY=off, mimicking a worker behind an unreachable
// proxy), the parser must fall back to go.sum but say so — set ResolutionStatus
// to GO_SUM_ONLY and attach a warning — rather than silently emitting a partial,
// stale dependency set that looks like a successful resolution.
func TestParseProjectDegradesToGoSumOnlyWhenModuleUnresolvable(t *testing.T) {
	if _, err := exec.LookPath("go"); err != nil {
		t.Skip("go toolchain not on PATH")
	}
	// given: every module fetch fails (no network, no cache entry for this module)
	t.Setenv("GOPROXY", "off")
	s, _ := newTestServer(t)
	projectDir := t.TempDir()
	writeFile(t, filepath.Join(projectDir, "go.mod"),
		"module example.com/foo\n\ngo 1.21\n\nrequire example.com/does/not/exist/xyz v1.2.3\n")
	writeFile(t, filepath.Join(projectDir, "main.go"), "package main\n\nfunc main() {}\n")
	writeFile(t, filepath.Join(projectDir, "go.sum"),
		"example.com/does/not/exist/xyz v1.2.3 h1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\n"+
			"example.com/does/not/exist/xyz v1.2.3/go.mod h1:BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=\n")

	relativeTo := projectDir
	params, err := json.Marshal(parseProjectRequest{ProjectPath: projectDir, RelativeTo: &relativeTo})
	require.NoError(t, err, "marshal params")

	// when
	if _, rpcErr := s.handleParseProject(params); rpcErr != nil {
		t.Fatalf("handleParseProject: %v", rpcErr.Message)
	}

	// then: the marker reports the degradation instead of a false-clean resolution
	mrr := findGoResolutionResult(t, s)
	assert.Equalf(t, golang.GoResolutionGoSumOnly, mrr.ResolutionStatus,
		"expected go.sum-only fallback, got %q", mrr.ResolutionStatus)
	// go.sum still supplies the dependency (with its hashes), but this is the
	// go.sum-recorded set, not the toolchain-selected build list
	require.NotNilf(t, mrr.FindResolved("example.com/does/not/exist/xyz"),
		"go.sum-derived dependency missing: %+v", mrr.ResolvedDependencies)

	// and the degradation is visible as a warning, not just a server log line
	gm := findGoMod(t, s)
	assert.Truef(t, hasGoSumOnlyWarning(gm),
		"expected a Markup.Warn about go.sum-only resolution: %+v", gm.Markers.Entries)
}

func findGoMod(t *testing.T, s *server) *golang.GoMod {
	t.Helper()
	for _, obj := range s.localObjects {
		if gm, ok := obj.(*golang.GoMod); ok {
			return gm
		}
	}
	t.Fatal("no GoMod produced")
	return nil
}

func hasGoSumOnlyWarning(gm *golang.GoMod) bool {
	for _, m := range gm.Markers.Entries {
		gmk, ok := m.(java.GenericMarker)
		if !ok || gmk.JavaType != "org.openrewrite.marker.Markup$Warn" {
			continue
		}
		if msg, ok := gmk.Data["message"].(string); ok && strings.Contains(msg, "go.sum alone") {
			return true
		}
	}
	return false
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

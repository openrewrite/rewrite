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

package parser

import (
	"os"
	"os/exec"
	"path/filepath"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

func TestMergeResolvedDependencies(t *testing.T) {
	// given: go.sum hashes and a toolchain-resolved build list that overlap on one module
	fromSum := []golang.GoResolvedDependency{
		{ModulePath: "github.com/a/b", Version: "v1.0.0", ModuleHash: "h1:mod=", GoModHash: "h1:gomod="},
		{ModulePath: "github.com/a/b", Version: "v0.9.0", ModuleHash: "h1:stale="}, // superseded version
	}
	fromList := []golang.GoResolvedDependency{
		{ModulePath: "github.com/a/b", Version: "v1.0.0", Indirect: true, ModuleGoVersion: "1.20",
			Deps: []golang.GoModuleRef{{ModulePath: "github.com/c/d", Version: "v2.0.0"}}},
	}

	// when
	merged := MergeResolvedDependencies(fromSum, fromList)

	// then: the build-list node is authoritative and inherits the go.sum hashes
	if len(merged) != 2 {
		t.Fatalf("want 2 entries (1 build-list + 1 stale sum), got %d: %+v", len(merged), merged)
	}
	got := merged[0]
	if got.ModulePath != "github.com/a/b" || got.Version != "v1.0.0" {
		t.Fatalf("first entry should be the selected build-list node, got %+v", got)
	}
	if !got.Indirect || got.ModuleGoVersion != "1.20" || len(got.Deps) != 1 {
		t.Errorf("build-list metadata not preserved: %+v", got)
	}
	if got.ModuleHash != "h1:mod=" || got.GoModHash != "h1:gomod=" {
		t.Errorf("go.sum hashes not inherited onto build-list node: %+v", got)
	}
	// and: the superseded go.sum row is preserved
	if merged[1].Version != "v0.9.0" || merged[1].ModuleHash != "h1:stale=" {
		t.Errorf("stale go.sum row should be preserved, got %+v", merged[1])
	}
}

func TestMergeResolvedDependenciesEmpty(t *testing.T) {
	// given / when: no build list (resolution unavailable) leaves go.sum rows untouched
	fromSum := []golang.GoResolvedDependency{{ModulePath: "x", Version: "v1", ModuleHash: "h1:x="}}

	// then
	merged := MergeResolvedDependencies(fromSum, nil)
	if len(merged) != 1 || merged[0].ModuleHash != "h1:x=" {
		t.Fatalf("go.sum-only merge should pass through, got %+v", merged)
	}
	if got := MergeResolvedDependencies(nil, nil); len(got) != 0 {
		t.Errorf("empty inputs should yield empty, got %+v", got)
	}
}

func TestAttachEdges(t *testing.T) {
	// given
	mods := []golang.GoResolvedDependency{
		{ModulePath: "example.com/main"},
		{ModulePath: "github.com/a/b", Version: "v1.0.0"},
	}
	edges := []moduleEdge{
		{fromPath: "example.com/main", toPath: "github.com/a/b", toVersion: "v1.0.0"},
		{fromPath: "github.com/gone", toPath: "github.com/x/y", toVersion: "v1"}, // unknown source: dropped
	}

	// when
	attachEdges(mods, edges)

	// then
	if len(mods[0].Deps) != 1 || mods[0].Deps[0].ModulePath != "github.com/a/b" {
		t.Fatalf("edge not attached to source node: %+v", mods[0].Deps)
	}
	if len(mods[1].Deps) != 0 {
		t.Errorf("node with no outgoing edges should have no Deps: %+v", mods[1].Deps)
	}
}

func TestSplitModuleVersion(t *testing.T) {
	// given / when / then
	if p, v := splitModuleVersion("github.com/a/b@v1.2.3"); p != "github.com/a/b" || v != "v1.2.3" {
		t.Errorf("got %q %q", p, v)
	}
	if p, v := splitModuleVersion("example.com/main"); p != "example.com/main" || v != "" {
		t.Errorf("main module (no version) got %q %q", p, v)
	}
}

// TestResolveModuleGraphStdlibOnly exercises the real toolchain path offline:
// a module importing only the standard library resolves with no network access.
func TestResolveModuleGraphStdlibOnly(t *testing.T) {
	if _, err := exec.LookPath("go"); err != nil {
		t.Skip("go toolchain not on PATH")
	}
	// given: a minimal, dependency-free module on disk
	dir := t.TempDir()
	writeFile(t, dir, "go.mod", "module example.com/testmod\n\ngo 1.21\n")
	writeFile(t, dir, "main.go", "package main\n\nimport \"fmt\"\n\nfunc main() { fmt.Println(\"hi\") }\n")

	// when
	mods, pkgs, err := ResolveModuleGraph(dir)
	if err != nil {
		t.Fatalf("resolve failed: %v", err)
	}

	// then: the build list contains the main module
	var main *golang.GoResolvedDependency
	for i := range mods {
		if mods[i].ModulePath == "example.com/testmod" {
			main = &mods[i]
		}
	}
	if main == nil {
		t.Fatalf("main module missing from build list: %+v", mods)
	}
	if !main.Main {
		t.Errorf("main module should have Main=true: %+v", main)
	}

	// and: the package map classifies stdlib and the main package
	var sawStdlib, sawMain bool
	for _, p := range pkgs {
		if p.ImportPath == "fmt" && p.Standard {
			sawStdlib = true
		}
		if p.ImportPath == "example.com/testmod" && p.ModulePath == "example.com/testmod" {
			sawMain = true
		}
	}
	if !sawStdlib {
		t.Errorf("expected stdlib package fmt with Standard=true in %+v", pkgs)
	}
	if !sawMain {
		t.Errorf("expected the main package mapped to its module in %+v", pkgs)
	}
}

func writeFile(t *testing.T, dir, name, content string) {
	t.Helper()
	if err := os.WriteFile(filepath.Join(dir, name), []byte(content), 0o644); err != nil {
		t.Fatalf("write %s: %v", name, err)
	}
}

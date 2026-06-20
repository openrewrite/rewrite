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

package modgraph

import (
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// TestTidyRequireSetViaProxy validates the full TidyRequireSet path (NeededModules
// + go>=1.17 pruning-completeness roots) against real `go mod tidy`, fetching
// everything from the proxy. Each case asserts the computed direct/indirect set
// EXACTLY matches go.mod — proving the pruning pass adds genuinely-needed
// test-transitive roots without over-including the test clusters that the version
// gate must exclude.
func TestTidyRequireSetViaProxy(t *testing.T) {
	if testing.Short() {
		t.Skip("needs network + the go toolchain")
	}
	cases := []struct {
		name, modPath, goMod, mainGo, testGo string
	}{
		{
			// No pruning-completeness extras: indirect == import-reachable only.
			// Guards against over-inclusion through the full Tidy path.
			name:    "no_extras",
			modPath: "example.com/noextras",
			goMod:   "module example.com/noextras\n\ngo 1.25.0\n\nrequire (\n\tgithub.com/grafana/pyroscope-go v1.2.8\n\tgolang.org/x/mod v0.35.0\n)\n",
			mainGo:  "package main\n\nimport (\n\t_ \"github.com/grafana/pyroscope-go\"\n\t_ \"golang.org/x/mod/modfile\"\n)\n\nfunc main() {}\n",
		},
		{
			// testify pulls yaml.v3, whose TEST imports gopkg.in/check.v1 ->
			// kr/text: a classic pruning-completeness indirect that import-only
			// resolution misses but `go mod tidy` records.
			name:    "test_transitive",
			modPath: "example.com/testtrans",
			goMod:   "module example.com/testtrans\n\ngo 1.25.0\n\nrequire github.com/stretchr/testify v1.9.0\n",
			mainGo:  "package main\n\nimport _ \"github.com/stretchr/testify/assert\"\n\nfunc main() {}\n",
		},
		{
			// gin genuinely EXERCISES the pruning-completeness promotion: it adds
			// kr/text (via gopkg.in/check.v1) and go.uber.org/mock (via a
			// dependency package's test) as indirect roots — modules under-
			// selected by the pruned graph that the in-memory MVS must promote.
			name:    "gin_pruning_completeness",
			modPath: "example.com/ginapp",
			goMod:   "module example.com/ginapp\n\ngo 1.25.0\n\nrequire github.com/gin-gonic/gin v1.10.0\n",
			mainGo:  "package main\n\nimport _ \"github.com/gin-gonic/gin\"\n\nfunc main() {}\n",
		},
		{
			// conc shape: testify is imported by a TEST of the main module (not
			// main code), with the older testify v1.8.1. `go mod tidy` does NOT
			// record kr/text here (the pruned graph already selects it correctly),
			// so the version gate must NOT over-promote it. Guards the conc 0/1
			// regression.
			name:    "test_in_test_file",
			modPath: "example.com/conctest",
			goMod:   "module example.com/conctest\n\ngo 1.20\n\nrequire github.com/stretchr/testify v1.8.1\n",
			mainGo:  "package conctest\n",
			testGo:  "package conctest\n\nimport (\n\t\"testing\"\n\n\t\"github.com/stretchr/testify/assert\"\n)\n\nfunc TestX(t *testing.T) { assert.Equal(t, 1, 1) }\n",
		},
		{
			// The exact conc go.mod, which exercises the DEPTH-ORDERING rule:
			// check.v1 -> kr/pretty -> kr/text. `go mod tidy` promotes kr/pretty
			// (under-selected) but NOT kr/text (kr/pretty's go.mod pins it once
			// kr/pretty is a root). A non-frontier-ordered pass over-promotes
			// kr/text; this guards that the BFS pins it first.
			name:    "conc_depth_ordering",
			modPath: "github.com/sourcegraph/conc",
			goMod:   "module github.com/sourcegraph/conc\n\ngo 1.20\n\nrequire github.com/stretchr/testify v1.8.1\n\nrequire (\n\tgithub.com/davecgh/go-spew v1.1.1 // indirect\n\tgithub.com/kr/pretty v0.3.0 // indirect\n\tgithub.com/pmezard/go-difflib v1.0.0 // indirect\n\tgithub.com/rogpeppe/go-internal v1.9.0 // indirect\n\tgopkg.in/check.v1 v1.0.0-20190902080502-41f04d3bba15 // indirect\n\tgopkg.in/yaml.v3 v3.0.1 // indirect\n)\n",
			mainGo:  "package conc\n",
			testGo:  "package conc\n\nimport (\n\t\"testing\"\n\n\t\"github.com/stretchr/testify/assert\"\n)\n\nfunc TestX(t *testing.T) { assert.Equal(t, 1, 1) }\n",
		},
	}

	httpGet := func(url string) ([]byte, int, error) {
		resp, err := http.Get(url)
		if err != nil {
			return nil, 0, err
		}
		defer resp.Body.Close()
		b, _ := io.ReadAll(resp.Body)
		return b, resp.StatusCode, nil
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			dir := t.TempDir()
			write(t, dir, "go.mod", tc.goMod)
			write(t, dir, "main.go", tc.mainGo)
			if tc.testGo != "" {
				write(t, dir, "main_test.go", tc.testGo)
			}
			runGo(t, dir, "mod", "tidy")
			wantDirect, wantIndirect := goldenRequires(t, dir)
			mainImports := scanMainImports(t, dir)

			tidied, err := os.ReadFile(filepath.Join(dir, "go.mod"))
			if err != nil {
				t.Fatal(err)
			}
			src := ProxySource(strings.TrimSpace(runGo(t, dir, "env", "GOPROXY")), httpGet)
			res, err := Resolve(tidied, src)
			if err != nil {
				t.Fatalf("Resolve: %v", err)
			}
			rs := TidyRequireSet(mainImports, tc.modPath, string(tidied), res, src, true)
			if !rs.Complete {
				t.Fatalf("expected complete TidyRequireSet; got Complete=false")
			}
			if d := diffSet(wantDirect, keys(rs.Direct)); d != "" {
				t.Errorf("direct mismatch:\n%s", d)
			}
			if d := diffSet(wantIndirect, keys(rs.Indirect)); d != "" {
				t.Errorf("indirect mismatch:\n%s", d)
			}
			if !t.Failed() {
				t.Logf("OK: TidyRequireSet indirect=%v matches go mod tidy", keys(rs.Indirect))
			}
		})
	}
}

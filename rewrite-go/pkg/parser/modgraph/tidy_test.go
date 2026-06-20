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
		name, modPath, goMod, mainGo string
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

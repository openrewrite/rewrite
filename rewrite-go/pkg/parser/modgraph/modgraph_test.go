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
	goparser "go/parser"
	"go/token"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strings"
	"testing"

	"golang.org/x/mod/modfile"
)

// TestResolveViaProxy validates that the resolver computes the same build list
// when fetching every dependency go.mod from a real GOPROXY (no module cache
// reads) as the toolchain's own `go list -m all`. This is the production path:
// the proxy fetch is injected (here a direct HTTP client; in the CLI it routes
// through OpenRewrite's HttpSender over RPC).
func TestResolveViaProxy(t *testing.T) {
	if testing.Short() {
		t.Skip("needs network + the go toolchain")
	}
	dir := t.TempDir()
	write(t, dir, "go.mod", "module example.com/proxytest\n\ngo 1.25.0\n\nrequire (\n\tgithub.com/grafana/pyroscope-go v1.2.8\n\tgolang.org/x/mod v0.35.0\n)\n")
	write(t, dir, "main.go", "package main\n\nimport (\n\t_ \"github.com/grafana/pyroscope-go\"\n\t_ \"golang.org/x/mod/modfile\"\n)\n\nfunc main() {}\n")
	runGo(t, dir, "mod", "tidy")
	golden := listModVersions(t, dir)

	mainGoMod, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		t.Fatal(err)
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
	src := ProxySource(strings.TrimSpace(runGo(t, dir, "env", "GOPROXY")), httpGet)

	res, err := Resolve(mainGoMod, src)
	if err != nil {
		t.Fatalf("Resolve: %v", err)
	}
	if !res.Complete {
		t.Errorf("expected complete proxy resolution; got Complete=false")
	}

	ours := map[string]string{}
	for _, m := range res.BuildList {
		if !m.Main {
			ours[m.Path] = m.Version
		}
	}
	for path, gver := range golden {
		if path == "example.com/proxytest" {
			continue
		}
		if over, ok := ours[path]; !ok {
			t.Errorf("proxy build list missing module %s (golden %s)", path, gver)
		} else if over != gver {
			t.Errorf("version mismatch for %s: proxy=%s, go list=%s", path, over, gver)
		}
	}
	for path := range ours {
		if _, ok := golden[path]; !ok {
			t.Errorf("proxy build list has extra module %s (not in `go list -m all`)", path)
		}
	}
	if !t.Failed() {
		t.Logf("OK: proxy-fetched build list (%d modules) matches `go list -m all`", len(ours))
	}
}

// TestNeededViaProxy is the capstone: with NO module cache, the resolver fetches
// every dependency go.mod AND every needed package's source zip from the real
// GOPROXY, and computes the same direct/indirect require set as `go mod tidy`.
// This is the "clean clone, no go tooling" path the CLI needs.
func TestNeededViaProxy(t *testing.T) {
	if testing.Short() {
		t.Skip("needs network + the go toolchain")
	}
	dir := t.TempDir()
	write(t, dir, "go.mod", "module example.com/needproxy\n\ngo 1.25.0\n\nrequire (\n\tgithub.com/grafana/pyroscope-go v1.2.8\n\tgolang.org/x/mod v0.35.0\n)\n")
	write(t, dir, "main.go", "package main\n\nimport (\n\t_ \"github.com/grafana/pyroscope-go\"\n\t_ \"golang.org/x/mod/modfile\"\n)\n\nfunc main() {}\n")
	runGo(t, dir, "mod", "tidy")
	wantDirect, wantIndirect := goldenRequires(t, dir)
	mainImports := scanMainImports(t, dir)

	mainGoMod, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		t.Fatal(err)
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
	src := ProxySource(strings.TrimSpace(runGo(t, dir, "env", "GOPROXY")), httpGet)

	res, err := Resolve(mainGoMod, src)
	if err != nil {
		t.Fatalf("Resolve: %v", err)
	}
	rs := NeededModules(mainImports, "example.com/needproxy", res, src, true)
	if !rs.Complete {
		t.Errorf("expected complete proxy require-set resolution; got Complete=false")
	}
	if d := diffSet(wantDirect, keys(rs.Direct)); d != "" {
		t.Errorf("direct require set mismatch (proxy):\n%s", d)
	}
	if d := diffSet(wantIndirect, keys(rs.Indirect)); d != "" {
		t.Errorf("indirect require set mismatch (proxy):\n%s", d)
	}
	if !t.Failed() {
		t.Logf("OK: proxy-only require set direct=%v indirect=%v matches go mod tidy", keys(rs.Direct), keys(rs.Indirect))
	}
}

// TestResolveMatchesToolchain validates the pure-Go resolver against the real
// `go` toolchain: the resolver's selected versions must agree with
// `go list -m all`, and every edge `go mod graph` reports must be present in
// the resolver's graph. The toolchain is used only as the GOLDEN here — the
// resolver itself never execs or hits the network.
func TestResolveMatchesToolchain(t *testing.T) {
	if testing.Short() {
		t.Skip("needs the go toolchain + module cache/network")
	}
	dir := t.TempDir()
	write(t, dir, "go.mod", "module example.com/graphtest\n\ngo 1.25.0\n\nrequire (\n\tgithub.com/grafana/pyroscope-go v1.2.8\n\tgolang.org/x/mod v0.35.0\n)\n")
	write(t, dir, "main.go", "package main\n\nimport (\n\t_ \"github.com/grafana/pyroscope-go\"\n\t_ \"golang.org/x/mod/modfile\"\n)\n\nfunc main() {}\n")

	// Populate the cache + go.sum so `go list -m all` works.
	runGo(t, dir, "mod", "tidy")

	gomodcache := strings.TrimSpace(runGo(t, dir, "env", "GOMODCACHE"))
	mainGoMod, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		t.Fatal(err)
	}

	res, err := Resolve(mainGoMod, CacheSource(gomodcache))
	if err != nil {
		t.Fatalf("Resolve: %v", err)
	}
	if !res.Complete {
		t.Errorf("expected complete resolution from a populated cache; got Complete=false")
	}

	// 1. Concrete known edge: pyroscope-go pulls klauspost/compress as indirect.
	if !hasEdge(res, "github.com/grafana/pyroscope-go", "v1.2.8", "github.com/klauspost/compress", "v1.17.8", true) {
		t.Errorf("missing expected edge pyroscope-go@v1.2.8 -> klauspost/compress@v1.17.8 (indirect)")
	}

	// 2. Build-list versions must agree with `go list -m all`.
	golden := listModVersions(t, dir)
	ours := map[string]string{}
	for _, m := range res.BuildList {
		ours[m.Path] = m.Version
	}
	for path, gver := range golden {
		if path == "example.com/graphtest" {
			continue
		}
		if over, ok := ours[path]; !ok {
			t.Errorf("build list missing module %s (golden %s)", path, gver)
		} else if over != gver {
			t.Errorf("version mismatch for %s: resolver=%s, go list=%s", path, over, gver)
		}
	}

	// 3. Every edge `go mod graph` reports must be in our graph.
	ourEdges := edgeSet(res)
	var missing int
	for _, g := range graphEdges(t, dir) {
		if !ourEdges[g] {
			missing++
			if missing <= 10 {
				t.Errorf("edge from `go mod graph` not in resolver graph: %s", g)
			}
		}
	}
	if missing == 0 {
		t.Logf("OK: build list (%d modules) and all `go mod graph` edges reproduced", len(res.BuildList))
	}

	// 4. go.sum material: every build-list module has a go.mod hash; modules
	// whose packages are imported (zip downloaded) also have a zip hash. This
	// mirrors go.sum, which records a /go.mod hash for the whole build list
	// and an h1: zip hash only for modules that are actually built.
	for _, m := range res.BuildList {
		if m.Main {
			continue
		}
		if !strings.HasPrefix(m.GoModHash, "h1:") {
			t.Errorf("module %s@%s missing h1 GoModHash", m.Path, m.Version)
		}
		if m.ModuleHash != "" && !strings.HasPrefix(m.ModuleHash, "h1:") {
			t.Errorf("module %s@%s has malformed ModuleHash %q", m.Path, m.Version, m.ModuleHash)
		}
	}
	// The directly-imported modules must have a zip hash.
	for _, p := range []string{"github.com/grafana/pyroscope-go", "golang.org/x/mod"} {
		found := false
		for _, m := range res.BuildList {
			if m.Path == p && strings.HasPrefix(m.ModuleHash, "h1:") {
				found = true
			}
		}
		if !found {
			t.Errorf("imported module %s should have a zip ModuleHash", p)
		}
	}

	// 5. The package-import-graph require set must match real `go mod tidy`'s
	// go.mod requires exactly (paths + direct/indirect classification).
	mainImports := scanMainImports(t, dir)
	rs := NeededModules(mainImports, "example.com/graphtest", res, CacheSource(gomodcache), true)
	if !rs.Complete {
		t.Errorf("expected complete require-set resolution; got Complete=false")
	}
	wantDirect, wantIndirect := goldenRequires(t, dir)
	if d := diffSet(wantDirect, keys(rs.Direct)); d != "" {
		t.Errorf("direct require set mismatch:\n%s", d)
	}
	if d := diffSet(wantIndirect, keys(rs.Indirect)); d != "" {
		t.Errorf("indirect require set mismatch:\n%s", d)
	}
	if t.Failed() {
		t.Logf("resolver direct=%v indirect=%v", keys(rs.Direct), keys(rs.Indirect))
	}
}

// scanMainImports returns the union of import paths across all .go files in
// the main module (mirrors what the recipe scan phase collects).
func scanMainImports(t *testing.T, dir string) []string {
	t.Helper()
	set := map[string]bool{}
	fset := token.NewFileSet()
	err := filepath.WalkDir(dir, func(path string, d os.DirEntry, err error) error {
		if err != nil || d.IsDir() || !strings.HasSuffix(path, ".go") {
			return err
		}
		f, perr := goparser.ParseFile(fset, path, nil, goparser.ImportsOnly)
		if perr != nil {
			return nil
		}
		for _, spec := range f.Imports {
			set[strings.Trim(spec.Path.Value, "\"`")] = true
		}
		return nil
	})
	if err != nil {
		t.Fatal(err)
	}
	return keys(setToMap(set))
}

// goldenRequires parses the real (post-tidy) go.mod and returns the direct and
// indirect module paths it declares.
func goldenRequires(t *testing.T, dir string) (direct, indirect []string) {
	t.Helper()
	b, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		t.Fatal(err)
	}
	mf, err := modfile.Parse("go.mod", b, nil)
	if err != nil {
		t.Fatal(err)
	}
	for _, r := range mf.Require {
		if r.Indirect {
			indirect = append(indirect, r.Mod.Path)
		} else {
			direct = append(direct, r.Mod.Path)
		}
	}
	return direct, indirect
}

func keys[V any](m map[string]V) []string {
	out := make([]string, 0, len(m))
	for k := range m {
		out = append(out, k)
	}
	sort.Strings(out)
	return out
}

func setToMap(s map[string]bool) map[string]bool { return s }

func diffSet(want, got []string) string {
	w := map[string]bool{}
	for _, x := range want {
		w[x] = true
	}
	g := map[string]bool{}
	for _, x := range got {
		g[x] = true
	}
	var msg []string
	for _, x := range want {
		if !g[x] {
			msg = append(msg, "  missing (tidy has, resolver lacks): "+x)
		}
	}
	for _, x := range got {
		if !w[x] {
			msg = append(msg, "  extra (resolver has, tidy lacks): "+x)
		}
	}
	return strings.Join(msg, "\n")
}

func hasEdge(res Result, fp, fv, tp, tv string, indirect bool) bool {
	for _, e := range res.Graph {
		if e.FromPath == fp && e.FromVersion == fv && e.ToPath == tp && e.ToVersion == tv && e.Indirect == indirect {
			return true
		}
	}
	return false
}

// edgeSet renders edges in `go mod graph` textual form ("from to", where a
// version-less node is the main module).
func edgeSet(res Result) map[string]bool {
	s := map[string]bool{}
	for _, e := range res.Graph {
		s[node(e.FromPath, e.FromVersion)+" "+node(e.ToPath, e.ToVersion)] = true
	}
	return s
}

func node(path, version string) string {
	if version == "" {
		return path
	}
	return path + "@" + version
}

func listModVersions(t *testing.T, dir string) map[string]string {
	out := runGo(t, dir, "list", "-m", "-f", "{{.Path}} {{.Version}}", "all")
	m := map[string]string{}
	for _, line := range strings.Split(strings.TrimSpace(out), "\n") {
		f := strings.Fields(line)
		if len(f) == 2 {
			m[f[0]] = f[1]
		} else if len(f) == 1 {
			m[f[0]] = ""
		}
	}
	return m
}

func graphEdges(t *testing.T, dir string) []string {
	out := runGo(t, dir, "mod", "graph")
	var edges []string
	for _, line := range strings.Split(strings.TrimSpace(out), "\n") {
		if line == "" {
			continue
		}
		// `go mod graph` emits synthetic `go@x` / `toolchain@x` pseudo-nodes
		// for the go-directive requirement; these are not modules.
		if f := strings.Fields(line); len(f) == 2 {
			if isPseudoNode(f[0]) || isPseudoNode(f[1]) {
				continue
			}
		}
		edges = append(edges, line)
	}
	return edges
}

func isPseudoNode(n string) bool {
	return n == "go" || n == "toolchain" ||
		strings.HasPrefix(n, "go@") || strings.HasPrefix(n, "toolchain@")
}

func runGo(t *testing.T, dir string, args ...string) string {
	t.Helper()
	cmd := exec.Command("go", args...)
	cmd.Dir = dir
	cmd.Env = append(os.Environ(), "GOFLAGS=-mod=mod")
	out, err := cmd.CombinedOutput()
	if err != nil {
		t.Fatalf("go %s: %v\n%s", strings.Join(args, " "), err, out)
	}
	return string(out)
}

func write(t *testing.T, dir, name, content string) {
	t.Helper()
	if err := os.WriteFile(filepath.Join(dir, name), []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

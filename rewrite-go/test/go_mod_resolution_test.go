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

package test

import (
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser/modgraph"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	golangrecipe "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// TestRefreshModelRecipeTime exercises the Maven-style model refresh at recipe
// time: with a ModSource installed in the ExecutionContext (as the RPC server
// does), RefreshModel re-parses the declared go.mod and re-resolves the build
// list, and the UpdateGoModModel visitor swaps the GoResolutionResult marker.
// This is how a dependency-mutating recipe keeps the model current for the next
// recipe in the run.
func TestRefreshModelRecipeTime(t *testing.T) {
	if testing.Short() {
		t.Skip("needs the go toolchain + module cache")
	}
	dir := t.TempDir()
	mustWrite(t, dir, "go.mod", "module example.com/rt\n\ngo 1.25.0\n\nrequire (\n\tgithub.com/grafana/pyroscope-go v1.2.8\n\tgolang.org/x/mod v0.35.0\n)\n")
	mustWrite(t, dir, "main.go", "package main\n\nimport (\n\t_ \"github.com/grafana/pyroscope-go\"\n\t_ \"golang.org/x/mod/modfile\"\n)\n\nfunc main() {}\n")
	mustGo(t, dir, "mod", "tidy")
	gomodcache := strings.TrimSpace(mustGo(t, dir, "env", "GOMODCACHE"))
	content, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		t.Fatal(err)
	}
	gm, err := parser.ParseGoModFile("go.mod", string(content))
	if err != nil {
		t.Fatal(err)
	}

	ctx := recipe.NewExecutionContext()
	golangrecipe.SetModSource(ctx, modgraph.CacheSource(gomodcache))

	// RefreshModel: declared requires + resolved build list.
	marker, ok := golangrecipe.RefreshModel(gm, ctx)
	if !ok {
		t.Fatal("RefreshModel returned ok=false")
	}
	if len(marker.BuildList) == 0 {
		t.Errorf("expected a non-empty resolved build list")
	}
	direct, indirect := requireSets(marker)
	for _, p := range []string{"github.com/grafana/pyroscope-go", "golang.org/x/mod"} {
		if !direct[p] {
			t.Errorf("expected %s declared as a direct require", p)
		}
	}
	for _, p := range []string{"github.com/grafana/pyroscope-go/godeltaprof", "github.com/klauspost/compress"} {
		if !indirect[p] {
			t.Errorf("expected %s declared as an indirect require", p)
		}
	}

	// UpdateGoModModel (the doAfterVisit-scheduled refresh) swaps the marker.
	res := golangrecipe.UpdateGoModModel().Visit(gm, ctx)
	gm2, ok := res.(*golang.GoMod)
	if !ok {
		t.Fatalf("UpdateGoModModel returned %T", res)
	}
	rr := golangrecipe.GetResolutionResult(gm2)
	if rr == nil || len(rr.BuildList) == 0 {
		t.Errorf("expected UpdateGoModModel to attach a resolved marker")
	}

	// Without a ModSource the declared model still refreshes, but the resolved
	// build list is empty (no network/cache access configured).
	m2, ok := golangrecipe.RefreshModel(gm, recipe.NewExecutionContext())
	if !ok {
		t.Errorf("expected declared refresh to succeed without a source")
	}
	if len(m2.BuildList) != 0 {
		t.Errorf("expected no build list without a ModSource, got %d", len(m2.BuildList))
	}
}

func requireSets(m golang.GoResolutionResult) (direct, indirect map[string]bool) {
	direct, indirect = map[string]bool{}, map[string]bool{}
	for _, r := range m.Requires {
		if r.Indirect {
			indirect[r.ModulePath] = true
		} else {
			direct[r.ModulePath] = true
		}
	}
	return direct, indirect
}

func mustWrite(t *testing.T, dir, name, content string) {
	t.Helper()
	if err := os.WriteFile(filepath.Join(dir, name), []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

func mustGo(t *testing.T, dir string, args ...string) string {
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

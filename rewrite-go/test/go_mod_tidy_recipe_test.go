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
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	golangrecipe "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// runTidy drives GoModTidy's scan->edit flow the way the RPC engine does:
// scan every .go file plus the go.mod into the accumulator, then run the
// editor over the go.mod. Returns the printed go.mod.
func runTidy(t *testing.T, goMod string, goFiles map[string]string) string {
	t.Helper()
	r := &golangrecipe.GoModTidy{}
	ctx := recipe.NewExecutionContext()
	acc := r.InitialValue(ctx)

	scanner := r.Scanner(acc)
	p := parser.NewGoParser()
	for path, content := range goFiles {
		cu, err := p.Parse(path, content)
		if err != nil {
			t.Fatalf("parse %s: %v", path, err)
		}
		scanner.Visit(cu, ctx)
	}
	gm, err := parser.ParseGoModFile("go.mod", goMod)
	if err != nil {
		t.Fatalf("parse go.mod: %v", err)
	}
	scanner.Visit(gm, ctx)

	editor := r.EditorWithData(acc)
	res := editor.Visit(gm, ctx)
	return printer.PrintGoMod(res.(*golang.GoMod))
}

// Perturbation: a directly-imported module wrongly marked `// indirect`
// should have the comment removed.
func TestTidyRemovesWrongIndirect(t *testing.T) {
	before := "module example.com/foo\n\ngo 1.21\n\nrequire github.com/a/b v1.0.0 // indirect\n"
	want := "module example.com/foo\n\ngo 1.21\n\nrequire github.com/a/b v1.0.0\n"
	goFiles := map[string]string{
		"main.go": "package main\n\nimport \"github.com/a/b\"\n\nfunc main() { _ = b.X }\n",
	}
	if got := runTidy(t, before, goFiles); got != want {
		t.Errorf("\nwant: %q\ngot:  %q", want, got)
	}
}

// Perturbation: a module not imported anywhere, missing its `// indirect`
// marker, should get one added.
func TestTidyAddsMissingIndirect(t *testing.T) {
	// go >= 1.17: tidy splits the directly-imported dep into its own require
	// and the unused one into a separate indirect require.
	before := "module example.com/foo\n\ngo 1.21\n\nrequire (\n\tgithub.com/a/b v1.0.0\n\tgithub.com/c/d v1.5.0\n)\n"
	want := "module example.com/foo\n\ngo 1.21\n\nrequire github.com/a/b v1.0.0\n\nrequire github.com/c/d v1.5.0 // indirect\n"
	goFiles := map[string]string{
		"main.go": "package main\n\nimport \"github.com/a/b\"\n\nfunc main() { _ = b.X }\n",
	}
	if got := runTidy(t, before, goFiles); got != want {
		t.Errorf("\nwant: %q\ngot:  %q", want, got)
	}
}

// Perturbation: out-of-order require entries should be sorted by module path.
func TestTidySortsRequireBlock(t *testing.T) {
	before := "module example.com/foo\n\ngo 1.21\n\nrequire (\n\tgithub.com/c/d v1.5.0\n\tgithub.com/a/b v1.0.0\n)\n"
	want := "module example.com/foo\n\ngo 1.21\n\nrequire (\n\tgithub.com/a/b v1.0.0\n\tgithub.com/c/d v1.5.0\n)\n"
	goFiles := map[string]string{
		"main.go": "package main\n\nimport (\n\t\"github.com/a/b\"\n\t\"github.com/c/d\"\n)\n\nfunc main() { _, _ = b.X, d.Y }\n",
	}
	if got := runTidy(t, before, goFiles); got != want {
		t.Errorf("\nwant: %q\ngot:  %q", want, got)
	}
}

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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// TestGoProjectTagsGoSiblingsButNotMod confirms the Go-side test harness
// matches the Java-side Assertions.goProject(...) shape: a project wrapper
// tags every .go sibling with a tree.GoProject marker, and the go.mod
// sibling round-trips verbatim (Go-side go.mod parsing is a follow-up).
func TestGoProjectTagsGoSiblingsButNotMod(t *testing.T) {
	goSrc := test.Golang(`
		package main

		func main() {}
	`)
	goSrc.AfterRecipe = func(t *testing.T, cu *tree.CompilationUnit) {
		t.Helper()
		project, ok := findGoProject(cu.Markers)
		if !ok {
			t.Fatal("expected GoProject marker on .go file but none was attached")
		}
		if project.ProjectName != "foo" {
			t.Fatalf("expected GoProject name=%q, got %q", "foo", project.ProjectName)
		}
	}

	spec := test.NewRecipeSpec()
	spec.RewriteRun(t,
		test.GoProject("foo",
			test.GoMod(`
				module example.com/foo

				go 1.22
			`),
			goSrc,
		),
	)
}

// TestGoProjectMixesWithBareGolangSpecs confirms a single RewriteRun call
// can take both project wrappers and bare Golang(...) sources side-by-side.
// The bare source carries no GoProject marker.
func TestGoProjectMixesWithBareGolangSpecs(t *testing.T) {
	wrapped := test.Golang(`
		package main

		func main() {}
	`)
	wrapped.AfterRecipe = func(t *testing.T, cu *tree.CompilationUnit) {
		t.Helper()
		if _, ok := findGoProject(cu.Markers); !ok {
			t.Fatal("wrapped source should carry GoProject marker")
		}
	}

	bare := test.Golang(`
		package main

		func main() {}
	`)
	bare.AfterRecipe = func(t *testing.T, cu *tree.CompilationUnit) {
		t.Helper()
		if _, ok := findGoProject(cu.Markers); ok {
			t.Fatal("bare source should NOT carry GoProject marker")
		}
	}

	spec := test.NewRecipeSpec()
	spec.RewriteRun(t,
		test.GoProject("foo", wrapped),
		bare,
	)
}

// findGoProject is a small lookup helper that mirrors what real recipes
// would do: scan a tree's Markers for a GoProject and return it.
func findGoProject(m tree.Markers) (tree.GoProject, bool) {
	for _, e := range m.Entries {
		if p, ok := e.(tree.GoProject); ok {
			return p, true
		}
	}
	return tree.GoProject{}, false
}

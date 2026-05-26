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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

func TestProjectImporterStubsRequiredModule(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/foo", nil)
	pi.AddRequire("github.com/x/y")

	pkg, err := pi.Import("github.com/x/y")
	if err != nil {
		t.Fatalf("Import returned error: %v", err)
	}
	if pkg == nil {
		t.Fatal("expected stub package, got nil")
	}
	if pkg.Path() != "github.com/x/y" {
		t.Errorf("Path: want %q, got %q", "github.com/x/y", pkg.Path())
	}
	if pkg.Name() != "y" {
		t.Errorf("Name: want %q, got %q", "y", pkg.Name())
	}
}

func TestProjectImporterStubMatchesSubPath(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/foo", nil)
	pi.AddRequire("github.com/x/y")

	// `import "github.com/x/y/sub"` should also stub-resolve, because the
	// require covers the whole module subtree.
	pkg, err := pi.Import("github.com/x/y/sub")
	if err != nil {
		t.Fatalf("Import returned error: %v", err)
	}
	if pkg == nil {
		t.Fatal("expected stub package for sub-path, got nil")
	}
	if pkg.Name() != "sub" {
		t.Errorf("Name: want %q, got %q", "sub", pkg.Name())
	}
}

func TestProjectImporterUnknownPathFallsThroughToError(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/foo", nil)
	pi.AddRequire("github.com/x/y")

	// A module not in requires and not stdlib should not stub-resolve —
	// importer.Default() returns an error for it.
	if _, err := pi.Import("github.com/never/heard/of"); err == nil {
		t.Errorf("expected error for unrequired non-stdlib path, got success")
	}
}

func TestGoProjectThirdPartyImportResolves(t *testing.T) {
	mainSrc := test.Golang(`
		package main

		import "github.com/x/y"

		func main() { _ = y.Hello() }
	`).WithPath("main.go")
	mainSrc.AfterRecipe = func(t *testing.T, cu *tree.CompilationUnit) {
		t.Helper()
		// `y` references the imported package; with the stub in place the
		// identifier should now have a non-nil Type. Without require-driven
		// stubbing this would be nil.
		ids := collectIdentTypes(cu)
		if ids["y"] == nil {
			t.Errorf("expected `y` import identifier to have a non-nil Type via the require stub; got nil")
		}
	}

	spec := test.NewRecipeSpec()
	spec.RewriteRun(t,
		test.GoProject("foo",
			test.GoMod(`
				module example.com/foo

				go 1.22

				require github.com/x/y v1.2.3
			`),
			mainSrc,
		),
	)
}

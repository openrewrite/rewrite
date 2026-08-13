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

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// identTypeWalker collects each visited Identifier's Type into a map.
type identTypeWalker struct {
	visitor.GoVisitor
	types map[string]java.JavaType
}

func (v *identTypeWalker) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if ident.Type != nil {
		v.types[ident.Name] = ident.Type
	}
	return ident
}

// TestProjectImporterResolvesIntraProjectImport directly exercises the
// ProjectImporter without going through the test harness: a sub-package
// is registered, then the importer is asked for it and we inspect the
// resulting *types.Package.
func TestProjectImporterResolvesIntraProjectImport(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/foo", nil)
	pi.AddSource("sub/sub.go", "package sub\n\nfunc Hello() string { return \"hi\" }\n")

	pkg, err := pi.Import("example.com/foo/sub")
	require.NoError(t, err, "Import returned error")
	require.NotNil(t, pkg, "Import returned nil package")
	assert.Equalf(t, "sub", pkg.Name(), "package name: want %q", "sub")
	if hello := pkg.Scope().Lookup("Hello"); hello == nil {
		t.Fatal("expected sub.Hello to be defined in the resolved package")
	}
}

// TestProjectImporterFallsBackToStdlib confirms that paths the importer
// doesn't have sources for fall through to importer.Default() so stdlib
// imports keep working.
func TestProjectImporterFallsBackToStdlib(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/foo", nil)
	pkg, err := pi.Import("fmt")
	require.NoError(t, err, "stdlib fallback failed")
	require.False(t, pkg == nil || pkg.Name() != "fmt", "expected pkg=fmt")
}

// TestGoProjectWiresImporterIntoHarness is the integration assertion: a
// project with go.mod + sub-package + main importing the sub gets type
// attribution on the import.
func TestGoProjectWiresImporterIntoHarness(t *testing.T) {
	mainSrc := test.Golang(`
		package main

		import "example.com/foo/sub"

		func main() { _ = sub.Hello() }
	`).WithPath("main.go")
	mainSrc.AfterRecipe = func(t *testing.T, cu *golang.CompilationUnit) {
		t.Helper()
		// Find an Identifier referencing "sub" or "Hello" and assert its
		// Type came back resolved (non-nil). Without the project importer,
		// these would all be nil because importer.Default() doesn't know
		// about example.com/foo/sub.
		identTypes := collectIdentTypes(cu)
		assert.NotNil(t,identTypes["sub"], "expected `sub` identifier in main.go to have a resolved Type, got nil")
		assert.NotNil(t,identTypes["Hello"], "expected `Hello` identifier in main.go to have a resolved Type, got nil")
	}

	spec := test.NewRecipeSpec()
	spec.RewriteRun(t,
		test.GoProject("foo",
			test.GoMod(`
				module example.com/foo

				go 1.22
			`),
			test.Golang(`
				package sub

				func Hello() string { return "hi" }
			`).WithPath("sub/sub.go"),
			mainSrc,
		),
	)
}

// TestGoProjectWithAbsolutePathsResolvesSiblingPackageImports documents the
// production ParseProject shape without writing files to disk. ParseProject
// discovers absolute file paths, but ProjectImporter must receive
// module-relative paths so sibling package imports are indexed under the
// module import path.
func TestGoProjectWithAbsolutePathsResolvesSiblingPackageImports(t *testing.T) {
	mainSrc := test.Golang(`
		package main

		import "example.com/foo/sub"

		func main() { _ = sub.Hello() }
	`).WithPath("/workspace/main.go")
	mainSrc.AfterRecipe = func(t *testing.T, cu *golang.CompilationUnit) {
		t.Helper()
		test.ExpectMethodType(t, cu, "Hello", "example.com/foo/sub")
	}

	spec := test.NewRecipeSpec()
	spec.RewriteRun(t,
		test.GoProject("foo",
			test.GoMod(`
				module example.com/foo

				go 1.22
			`),
			test.Golang(`
				package sub

				func Hello() string { return "hi" }
			`).WithPath("/workspace/sub/sub.go"),
			mainSrc,
		),
	)
}

// collectIdentTypes walks the tree and returns a map of identifier name
// → its Type (whichever last assignment wins; sufficient for these tests).
func collectIdentTypes(cu *golang.CompilationUnit) map[string]java.JavaType {
	out := map[string]java.JavaType{}
	collector := &identTypeWalker{types: out}
	visitor.Init(collector)
	collector.Visit(cu, nil)
	return out
}

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
	"fmt"
	"sort"
	"strings"
	"testing"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type importAccumulator struct {
	paths map[string]bool
}

type collectExternalImports struct {
	recipe.ScanningBase
}

func (r *collectExternalImports) Name() string {
	return "org.openrewrite.golang.test.CollectExternalImports"
}
func (r *collectExternalImports) DisplayName() string { return "Collect external imports onto go.mod" }
func (r *collectExternalImports) Description() string {
	return "Scans every .go file for external imports and records them as a SearchResult on go.mod."
}

func (r *collectExternalImports) InitialValue(*recipe.ExecutionContext) any {
	return &importAccumulator{paths: map[string]bool{}}
}

func (r *collectExternalImports) Scanner(acc any) recipe.TreeVisitor {
	return visitor.Init(&externalImportScanner{acc: acc.(*importAccumulator)})
}

func (r *collectExternalImports) EditorWithData(acc any) recipe.TreeVisitor {
	return visitor.Init(&goModAnnotator{acc: acc.(*importAccumulator)})
}

type externalImportScanner struct {
	visitor.GoVisitor
	acc *importAccumulator
}

func (v *externalImportScanner) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	if cu.Imports != nil {
		for _, rp := range cu.Imports.Elements {
			path := importPath(rp.Element)
			if isExternalImport(path) {
				v.acc.paths[path] = true
			}
		}
	}
	return cu
}

func importPath(imp *java.Import) string {
	if imp == nil {
		return ""
	}
	lit, ok := imp.Qualid.(*java.Literal)
	if !ok || lit == nil {
		return ""
	}
	return strings.Trim(lit.Source, "\"`")
}

type goModAnnotator struct {
	visitor.GoVisitor
	acc *importAccumulator
}

func (v *goModAnnotator) VisitGoMod(gm *golang.GoMod, p any) java.Tree {
	if len(v.acc.paths) == 0 {
		return gm
	}
	paths := make([]string, 0, len(v.acc.paths))
	for path := range v.acc.paths {
		paths = append(paths, path)
	}
	sort.Strings(paths)
	desc := fmt.Sprintf("external imports: %s", strings.Join(paths, ", "))
	return gm.WithMarkers(java.AddMarker(gm.Markers, java.SearchResult{Ident: uuid.New(), Description: desc}))
}

func isExternalImport(path string) bool {
	if path == "" {
		return false
	}
	first := path
	if i := strings.IndexByte(path, '/'); i >= 0 {
		first = path[:i]
	}
	return strings.Contains(first, ".")
}

func TestScanningRecipeAnnotatesGoModFromSources(t *testing.T) {
	// given a project whose .go files import two external modules
	spec := NewRecipeSpec().WithRecipe(&collectExternalImports{})

	// when the scanning recipe runs across all sources
	// then the go.mod carries a SearchResult listing the scanned imports
	spec.RewriteRun(t,
		GoProject("app",
			GoMod(
				`
					module example.com/app

					go 1.22

					require (
						example.com/a v1.0.0
						example.com/b v1.2.0
					)
				`,
				`
					/*~~(external imports: example.com/a/thing, example.com/b/other)~~>*/module example.com/app

					go 1.22

					require (
						example.com/a v1.0.0
						example.com/b v1.2.0
					)
				`,
			),
			Golang(`
				package app

				import (
					"fmt"

					"example.com/a/thing"
				)

				func A() { fmt.Println(thing.X) }
			`),
			Golang(`
				package app

				import "example.com/b/other"

				func B() { other.Y() }
			`),
		),
	)
}

type generateBuildTag struct {
	recipe.ScanningBase
}

func (r *generateBuildTag) Name() string        { return "org.openrewrite.golang.test.GenerateBuildTag" }
func (r *generateBuildTag) DisplayName() string { return "Generate a build-tag file" }
func (r *generateBuildTag) Description() string {
	return "Generates tools.go when the project imports any external module."
}

func (r *generateBuildTag) InitialValue(*recipe.ExecutionContext) any {
	return &importAccumulator{paths: map[string]bool{}}
}

func (r *generateBuildTag) Scanner(acc any) recipe.TreeVisitor {
	return visitor.Init(&externalImportScanner{acc: acc.(*importAccumulator)})
}

func (r *generateBuildTag) Generate(acc any, _ *recipe.ExecutionContext) []java.Tree {
	if len(acc.(*importAccumulator).paths) == 0 {
		return nil
	}
	cu, err := parser.NewGoParser().Parse("tools.go", "package app\n")
	if err != nil {
		return nil
	}
	return []java.Tree{cu}
}

func TestScanningRecipeGeneratesFile(t *testing.T) {
	// given a project that imports an external module
	spec := NewRecipeSpec().WithRecipe(&generateBuildTag{})

	// when the scanning recipe runs
	// then the harness surfaces the generated tools.go for assertion
	spec.RewriteRun(t,
		GoProject("app",
			GoMod(`
				module example.com/app

				go 1.22

				require example.com/a v1.0.0
			`),
			Golang(`
				package app

				import "example.com/a/thing"

				func A() { thing.X() }
			`),
		),
		Generated("tools.go", "package app\n"),
	)
}

func TestScanningRecipeNoMatchesLeavesGoModUnchanged(t *testing.T) {
	// given .go files that import only the standard library
	spec := NewRecipeSpec().WithRecipe(&collectExternalImports{})

	// when the scanning recipe runs
	// then go.mod is left untouched (no After state)
	spec.RewriteRun(t,
		GoProject("app",
			GoMod(`
				module example.com/app

				go 1.22
			`),
			Golang(`
				package app

				import "fmt"

				func A() { fmt.Println("hi") }
			`),
		),
	)
}

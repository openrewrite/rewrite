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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// --- Sample refactoring recipe that rewrites the `go` directive version ---

type changeGoVersion struct {
	recipe.Base
	NewVersion string
}

func (r *changeGoVersion) Name() string        { return "org.openrewrite.golang.test.ChangeGoVersion" }
func (r *changeGoVersion) DisplayName() string { return "Change the go directive version" }
func (r *changeGoVersion) Description() string { return "Rewrites the `go` directive to a new version." }

func (r *changeGoVersion) Editor() recipe.TreeVisitor {
	return visitor.Init(&changeGoVersionVisitor{newVersion: r.NewVersion})
}

type changeGoVersionVisitor struct {
	visitor.GoVisitor
	newVersion string
}

func (v *changeGoVersionVisitor) VisitGoModDirective(d *golang.GoModDirective, p any) java.Tree {
	d = v.GoVisitor.VisitGoModDirective(d, p).(*golang.GoModDirective)
	if d.Keyword == "go" && len(d.Values) == 1 {
		d = d.WithValues([]*golang.GoModValue{d.Values[0].WithText(v.newVersion)})
	}
	return d
}

// --- Sample search recipe that marks the module path ---

type findModulePath struct {
	recipe.Base
}

func (r *findModulePath) Name() string        { return "org.openrewrite.golang.test.FindModulePath" }
func (r *findModulePath) DisplayName() string { return "Find the module path" }
func (r *findModulePath) Description() string {
	return "Marks the `module` directive's path with a search result."
}

func (r *findModulePath) Editor() recipe.TreeVisitor {
	return visitor.Init(&findModulePathVisitor{})
}

type findModulePathVisitor struct {
	visitor.GoVisitor
}

func (v *findModulePathVisitor) VisitGoModDirective(d *golang.GoModDirective, p any) java.Tree {
	d = v.GoVisitor.VisitGoModDirective(d, p).(*golang.GoModDirective)
	if d.Keyword == "module" && len(d.Values) > 0 {
		marked := d.Values[0].WithMarkers(java.FoundSearchResult(d.Values[0].Markers, "found module"))
		d = d.WithValues(append([]*golang.GoModValue{marked}, d.Values[1:]...))
	}
	return d
}

// --- Tests ---

func TestGoModRecipeSetGoVersion(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&changeGoVersion{NewVersion: "1.22"})
	spec.RewriteRun(t,
		test.GoMod(`
			module example.com/foo

			go 1.21
		`, `
			module example.com/foo

			go 1.22
		`),
	)
}

func TestGoModRecipeNoChange(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&changeGoVersion{NewVersion: "1.22"})
	spec.RewriteRun(t,
		test.GoMod(`
			module example.com/bar

			go 1.22
		`),
	)
}

// TestGoModRecipeInBlock confirms a recipe can reach version tokens inside a
// factored require block, not just top-level directives.
func TestGoModRecipeBumpsAcrossWhitespace(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&changeGoVersion{NewVersion: "1.23"})
	spec.RewriteRun(t,
		test.GoMod(`
			module example.com/foo

			go 1.21

			require (
				github.com/foo/bar v1.0.0
				github.com/baz/qux v1.5.0 // indirect
			)
		`, `
			module example.com/foo

			go 1.23

			require (
				github.com/foo/bar v1.0.0
				github.com/baz/qux v1.5.0 // indirect
			)
		`),
	)
}

// TestGoModSearchRecipe verifies SearchResult markers render as /*~~(...)~~>*/
// in go.mod output, matching the .go convention.
func TestGoModSearchRecipe(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&findModulePath{})
	spec.RewriteRun(t,
		test.GoMod(
			"module example.com/foo\n\ngo 1.21\n",
			"module /*~~(found module)~~>*/example.com/foo\n\ngo 1.21\n",
		),
	)
}

// TestGoModRecipeInProject runs a recipe over a project mixing a go.mod and a
// .go file: the go.mod is transformed while the unrelated .go file is left
// untouched.
func TestGoModRecipeInProject(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&changeGoVersion{NewVersion: "1.23"})
	spec.RewriteRun(t,
		test.GoProject("foo",
			test.GoMod(`
				module example.com/foo

				go 1.21
			`, `
				module example.com/foo

				go 1.23
			`),
			test.Golang(`
				package main

				func main() {
				}
			`),
		),
	)
}

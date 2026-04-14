/*
 * Copyright 2025 the original author or authors.
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

package template

import (
	"fmt"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

func TestDetectScaffoldKind(t *testing.T) {
	tests := []struct {
		code string
		want ScaffoldKind
	}{
		{"x + 1", ScaffoldExpression},
		{"fmt.Println(x)", ScaffoldExpression},
		{"if x > 0 { }", ScaffoldStatement},
		{"return x", ScaffoldStatement},
		{"for i := 0; i < n; i++ { }", ScaffoldStatement},
		{"func foo() { }", ScaffoldTopLevel},
		{"type Foo struct{}", ScaffoldTopLevel},
		{"var x = 1", ScaffoldTopLevel},
	}
	for _, tt := range tests {
		got := detectScaffoldKind(tt.code)
		if got != tt.want {
			t.Errorf("detectScaffoldKind(%q) = %d, want %d", tt.code, got, tt.want)
		}
	}
}

func TestNewRecipeSimpleRewrite(t *testing.T) {
	// Replace identifier `x` with `y`
	r := NewRecipe(
		RecipeName("test.ReplaceXWithY"),
		WithDisplayName("Replace x with y"),
		WithBefore(`x`),
		WithAfter(`y`),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = x
		`, `
			package main

			var a = y
		`),
	)
}

func TestNewRecipeWithCapture(t *testing.T) {
	// Replace `expr + 0` with `expr`
	expr := Expr("expr")
	r := NewRecipe(
		RecipeName("test.AddZero"),
		WithDisplayName("Remove addition of zero"),
		WithBefore(fmt.Sprintf(`%s + 0`, expr)),
		WithAfter(fmt.Sprintf(`%s`, expr)),
		WithCaptures(expr),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = x + 0
		`, `
			package main

			var a = x
		`),
	)
}

func TestNewRecipeNoChangeWhenNoMatch(t *testing.T) {
	r := NewRecipe(
		RecipeName("test.AddZero"),
		WithDisplayName("Remove addition of zero"),
		WithBefore(`x + 0`),
		WithAfter(`x`),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = x + 1
		`),
	)
}

func TestNewRecipeMultipleBefores(t *testing.T) {
	// Replace both `1 + 2` and `2 + 1` with `3`
	r := NewRecipe(
		RecipeName("test.OnePlusTwo"),
		WithDisplayName("Simplify 1+2"),
		WithBefore(`1 + 2`),
		WithBefore(`2 + 1`),
		WithAfter(`3`),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)

	// First alternative matches
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = 1 + 2
		`, `
			package main

			var a = 3
		`),
	)
}

func TestNewRecipeMultipleBeforeSecondMatches(t *testing.T) {
	r := NewRecipe(
		RecipeName("test.OnePlusTwo"),
		WithDisplayName("Simplify 1+2"),
		WithBefore(`1 + 2`),
		WithBefore(`2 + 1`),
		WithAfter(`3`),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)

	// Second alternative matches
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = 2 + 1
		`, `
			package main

			var a = 3
		`),
	)
}

func TestNewRecipeWithImports(t *testing.T) {
	// Replace `fmt.Sprintf("%d", n)` with `strconv.Itoa(n)`
	n := Expr("n")
	r := NewRecipe(
		RecipeName("test.SprintfToItoa"),
		WithDisplayName("Use strconv.Itoa"),
		WithBefore(fmt.Sprintf(`fmt.Sprintf("%%d", %s)`, n), Imports("fmt")),
		WithAfter(fmt.Sprintf(`strconv.Itoa(%s)`, n), Imports("strconv")),
		WithCaptures(n),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			import "fmt"

			func f(n int) string {
				return fmt.Sprintf("%d", n)
			}
		`, `
			package main

			import "fmt"

			func f(n int) string {
				return strconv.Itoa(n)
			}
		`),
	)
}

func TestNewRecipeMetadata(t *testing.T) {
	r := NewRecipe(
		RecipeName("org.openrewrite.golang.MyRecipe"),
		WithDisplayName("My Recipe"),
		WithDescription("Does something useful."),
		WithTags("cleanup", "style"),
		WithBefore(`x`),
		WithAfter(`y`),
	)

	if r.Name() != "org.openrewrite.golang.MyRecipe" {
		t.Errorf("Name() = %q", r.Name())
	}
	if r.DisplayName() != "My Recipe" {
		t.Errorf("DisplayName() = %q", r.DisplayName())
	}
	if r.Description() != "Does something useful." {
		t.Errorf("Description() = %q", r.Description())
	}
	if len(r.Tags()) != 2 || r.Tags()[0] != "cleanup" {
		t.Errorf("Tags() = %v", r.Tags())
	}
}

type myRecipe struct {
	TemplateRecipe
}

func (r *myRecipe) Name() string        { return "test.MyRecipe" }
func (r *myRecipe) DisplayName() string  { return "My Recipe" }
func (r *myRecipe) Description() string  { return "Test recipe" }

func TestTemplateRecipeStruct(t *testing.T) {
	expr := Expr("expr")
	r := &myRecipe{}
	r.InitTemplate(
		WithBefore(fmt.Sprintf(`%s + 0`, expr)),
		WithAfter(fmt.Sprintf(`%s`, expr)),
		WithCaptures(expr),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var a = x + 0
		`, `
			package main

			var a = x
		`),
	)
}

func TestNewRecipePreservesFormatting(t *testing.T) {
	expr := Expr("expr")
	r := NewRecipe(
		RecipeName("test.Identity"),
		WithDisplayName("Replace x with x"),
		WithBefore(fmt.Sprintf(`%s + 0`, expr)),
		WithAfter(fmt.Sprintf(`%s`, expr)),
		WithCaptures(expr),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			var longVariableName = someFunction() + 0
		`, `
			package main

			var longVariableName = someFunction()
		`),
	)
}

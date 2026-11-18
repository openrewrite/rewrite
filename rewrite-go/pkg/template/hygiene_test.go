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

package template

import (
	"fmt"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

var hygieneArg = Expr("arg")

func replacingTwice(after string, afterOpts ...BeforeOption) *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.ReplaceTwice"),
		WithDisplayName("Replace twice"),
		WithBefore(fmt.Sprintf(`twice(%s)`, hygieneArg)),
		WithAfter(after, afterOpts...),
		WithCaptures(hygieneArg),
	))
}

func TestTemplateLocalDoesNotCaptureANameTheCaptureRefersTo(t *testing.T) {
	replacingTwice(fmt.Sprintf(`func() int { v := 2; return v * %s }()`, hygieneArg)).RewriteRun(t,
		test.Golang(`
			package main

			func f(v int) int {
				return twice(v)
			}
		`, `
			package main

			func f(v int) int {
				return func() int { v1 := 2; return v1 * v }()
			}
		`),
	)
}

func TestTemplateParameterDoesNotCaptureANameTheCaptureRefersTo(t *testing.T) {
	replacingTwice(fmt.Sprintf(`apply(func(n int) int { return n * %s })`, hygieneArg)).RewriteRun(t,
		test.Golang(`
			package main

			func f(n int) int {
				return twice(n + 1)
			}
		`, `
			package main

			func f(n int) int {
				return apply(func(n1 int) int { return n1 * (n + 1) })
			}
		`),
	)
}

func TestTemplateBlockLocalDoesNotCaptureANameTheCaptureRefersTo(t *testing.T) {
	replacingTwice(fmt.Sprintf("{\nerr := prepare()\nuse(err, %s)\n}", hygieneArg), asStatement).RewriteRun(t,
		test.Golang(`
			package main

			func f(err error) {
				twice(err)
			}
		`, `
			package main

			func f(err error) {
				{
					err1 := prepare()
					use(err1, err)
				}
			}
		`),
	)
}

func TestTemplateLocalKeepsItsNameWhereTheCaptureDoesNotReferToIt(t *testing.T) {
	replacingTwice(fmt.Sprintf(`func() int { v := 2; return v * %s }()`, hygieneArg)).RewriteRun(t,
		test.Golang(`
			package main

			func f(w int) int {
				return twice(w)
			}
		`, `
			package main

			func f(w int) int {
				return func() int { v := 2; return v * w }()
			}
		`),
	)
}

func TestTemplateLocalRenameLeavesAFieldOfTheSameNameAlone(t *testing.T) {
	replacingTwice(fmt.Sprintf(`func() P { v := 2; return P{v: v * %s} }()`, hygieneArg)).RewriteRun(t,
		test.Golang(`
			package main

			type P struct{ v int }

			func f(v int) P {
				return twice(v)
			}
		`, `
			package main

			type P struct{ v int }

			func f(v int) P {
				return func() P { v1 := 2; return P{v: v1 * v} }()
			}
		`),
	)
}

// The declaration is in scope at the site, where code after it may use it.
func TestTemplateKeepsTheNameOfADeclarationItLeadsWith(t *testing.T) {
	replacingTwice(fmt.Sprintf(`v := %s * 2`, hygieneArg)).RewriteRun(t,
		test.Golang(`
			package main

			func f(v int) {
				twice(v)
			}
		`, `
			package main

			func f(v int) {
				v := v * 2
			}
		`),
	)
}

// The body refers to the loop variable the pattern matched, which the template
// declares again.
func TestTemplateKeepsALoopVariableTheCapturedBodyRefersTo(t *testing.T) {
	xs, body := Expr("xs"), Stmt("body").Variadic(0, -1)
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.InlineAll"),
		WithDisplayName("Inline all"),
		WithBefore(fmt.Sprintf("for _, item := range all(%s) {\n%s\n}", xs, body)),
		WithAfter(fmt.Sprintf("for _, item := range %s {\n%s\n}", xs, body)),
		WithCaptures(xs, body),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f(items []int) {
				for _, item := range all(items) {
					use(item)
				}
			}
		`, `
			package main

			func f(items []int) {
				for _, item := range items {
					use(item)
				}
			}
		`),
	)
}

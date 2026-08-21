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

// The after templates below carry deliberately wrong indentation so that
// output at the target's indent can only come from the formatter.

func TestTemplateIndentsMultilineStatement(t *testing.T) {
	r := NewRecipe(
		RecipeName("test.IndentMultilineStatement"),
		WithDisplayName("Replace legacy() with a loop"),
		WithBefore(`legacy()`),
		WithAfter("for i := 0; i < 3; i++ {\n\t\t\t\t\tmodern()\n\t\t\t\t}"),
		AsStatement(),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			func legacy() {}
			func modern() {}

			func f() {
				if true {
					legacy()
				}
			}
		`, `
			package main

			func legacy() {}
			func modern() {}

			func f() {
				if true {
					for i := 0; i < 3; i++ {
						modern()
					}
				}
			}
		`),
	)
}

// Splicing one match rebuilds the tree around the others, which is where a
// later match is at risk of being stranded at the template's own indent.
func TestTemplateIndentsEveryMatchInFile(t *testing.T) {
	r := NewRecipe(
		RecipeName("test.IndentEveryMatch"),
		WithDisplayName("Replace legacy() with a loop"),
		WithBefore(`legacy()`),
		WithAfter("for i := 0; i < 3; i++ {\n\t\t\t\t\tmodern()\n\t\t\t\t}"),
		AsStatement(),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			func legacy() {}
			func modern() {}

			func shallow() {
				legacy()
			}

			func deep() {
				if true {
					if true {
						legacy()
					}
				}
			}
		`, `
			package main

			func legacy() {}
			func modern() {}

			func shallow() {
				for i := 0; i < 3; i++ {
					modern()
				}
			}

			func deep() {
				if true {
					if true {
						for i := 0; i < 3; i++ {
							modern()
						}
					}
				}
			}
		`),
	)
}

// The doubled spaces in untouched() are what gofmt would collapse if layout
// were not bounded to the spliced subtree.
func TestTemplateLeavesUntouchedCodeAlone(t *testing.T) {
	r := NewRecipe(
		RecipeName("test.BoundedFormatting"),
		WithDisplayName("Replace legacy() with a loop"),
		WithBefore(`legacy()`),
		WithAfter("for i := 0; i < 3; i++ {\n\t\t\t\t\tmodern()\n\t\t\t\t}"),
		AsStatement(),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			func legacy() {}
			func modern() {}

			func untouched() {
				x  :=  1
				_  =  x
			}

			func f() {
				legacy()
			}
		`, `
			package main

			func legacy() {}
			func modern() {}

			func untouched() {
				x  :=  1
				_  =  x
			}

			func f() {
				for i := 0; i < 3; i++ {
					modern()
				}
			}
		`),
	)
}

func TestTemplateExpressionLeavesLayoutAlone(t *testing.T) {
	expr := Expr("expr")
	r := NewRecipe(
		RecipeName("test.ExpressionNoDiff"),
		WithDisplayName("Remove addition of zero"),
		WithBefore(fmt.Sprintf(`%s + 0`, expr)),
		WithAfter(fmt.Sprintf(`%s`, expr)),
		WithCaptures(expr),
	)

	spec := test.NewRecipeSpec().WithRecipe(r)
	spec.RewriteRun(t,
		test.Golang(`
			package main

			func f(x int) int {
				return x + 0
			}
		`, `
			package main

			func f(x int) int {
				return x
			}
		`),
	)
}

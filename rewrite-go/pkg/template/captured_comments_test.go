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

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

var commented = Expr("commented")

func renamingOldToNeu(after string) *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.RenameOld"),
		WithDisplayName("Rename old"),
		WithBefore(fmt.Sprintf(`old(%s)`, commented)),
		WithAfter(after),
		WithCaptures(commented),
	))
}

func TestTemplateKeepsABlockCommentLeadingACapture(t *testing.T) {
	renamingOldToNeu(fmt.Sprintf(`neu(%s)`, commented)).RewriteRun(t,
		test.Golang(`
			package main

			var a = old( /* keep */ 1)
		`, `
			package main

			var a = neu(/* keep */ 1)
		`),
	)
}

func TestTemplateKeepsALineCommentLeadingACaptureOnItsOwnLine(t *testing.T) {
	body := Stmt("body")
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.UnwrapIfTrue"),
		WithDisplayName("Unwrap if true"),
		WithBefore("if true {\n"+body.String()+"\n}"),
		WithAfter(body.String(), asStatement),
		WithCaptures(body),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				if true {
					// why g
					g()
				}
			}
		`, `
			package main

			func f() {
				// why g
				g()
			}
		`),
	)
}

func TestTemplateKeepsCommentsLeadingEachStatementOfARun(t *testing.T) {
	body := Stmt("body").Variadic(0, -1)
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.UnwrapIfTrue"),
		WithDisplayName("Unwrap if true"),
		WithBefore("if true {\n"+body.String()+"\n}"),
		WithAfter("{\n"+body.String()+"\n}", asStatement),
		WithCaptures(body),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f(x int) {
				if true {
					// first
					x = 2
					// second
					x = 3
				}
			}
		`, `
			package main

			func f(x int) {
				{
					// first
					x = 2
					// second
					x = 3
				}
			}
		`),
	)
}

func TestTemplateKeepsACommentLeadingAnArgumentOfARun(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.RenameOld"),
		WithDisplayName("Rename old"),
		WithBefore(fmt.Sprintf(`old(%s)`, args)),
		WithAfter(fmt.Sprintf(`neu(%s)`, args)),
		WithCaptures(args),
	)).RewriteRun(t,
		test.Golang(`
			package main

			var a = old(1, /* two */ 2)
		`, `
			package main

			var a = neu(1, /* two */ 2)
		`),
	)
}

// The line break ending the comment would end the return statement before the
// value it returns.
func TestTemplateLeavesALineCommentBehindWhereItWouldEndTheStatement(t *testing.T) {
	renamingOldToNeu(`return `+commented.String()).RewriteRun(t,
		test.Golang(`
			package main

			func f() int {
				old(
					// one
					1)
			}
		`, `
			package main

			func f() int {
				return 1
			}
		`),
	)
}

func TestTemplateKeepsItsOwnLeadingComment(t *testing.T) {
	renamingOldToNeu(fmt.Sprintf(`/* migrated */ neu(%s)`, commented)).RewriteRun(t,
		test.Golang(`
			package main

			var a = old(1)
		`, `
			package main

			var a = /* migrated */ neu(1)
		`),
	)
}

func TestTemplateKeepsTheReplacedNodesCommentAheadOfItsOwn(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.RenameOld"),
		WithDisplayName("Rename old"),
		WithBefore(fmt.Sprintf(`old(%s)`, commented)),
		WithAfter(fmt.Sprintf("// migrated\nneu(%s)", commented), asStatement),
		WithCaptures(commented),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				// why
				old(1)
			}
		`, `
			package main

			func f() {
				// why
				// migrated
				neu(1)
			}
		`),
	)
}

func TestInstantiateKeepsTheTemplatesLeadingComment(t *testing.T) {
	tmpl := StatementTemplate("// migrated\nneu()").Build()

	got := tmpl.Instantiate(nil)

	require.NotNil(t, got)
	assert.Equal(t, "// migrated\nneu()", printer.Print(got))
}

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

func TestStatementTemplateOfTwoStatementsDropsNeither(t *testing.T) {
	x := Expr("x")
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.SplitCall"),
		WithDisplayName("Split call"),
		WithBefore(fmt.Sprintf(`old(%s)`, x)),
		WithAfter(fmt.Sprintf("a(%s)\nb(%s)", x, x), asStatement),
		WithCaptures(x),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				old(1)
			}
		`),
	)
}

func TestStatementTemplateOfTwoStatementsInABlockKeepsBoth(t *testing.T) {
	x := Expr("x")
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.SplitCall"),
		WithDisplayName("Split call"),
		WithBefore(fmt.Sprintf(`old(%s)`, x)),
		WithAfter(fmt.Sprintf("{\na(%s)\nb(%s)\n}", x, x), asStatement),
		WithCaptures(x),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				old(1)
			}
		`, `
			package main

			func f() {
				{
					a(1)
					b(1)
				}
			}
		`),
	)
}

func TestStatementPatternOfTwoStatementsDoesNotMatchTheFirstAlone(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.MergeCalls"),
		WithDisplayName("Merge calls"),
		WithBefore("a()\nb()", asStatement),
		WithAfter("c()", asStatement),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				a()
				z()
			}
		`),
	)
}

func TestStatementPatternOfTwoStatementsReportsWhyItCannotMatch(t *testing.T) {
	pat := StatementPattern("a()\nb()").Build()
	tmpl := StatementTemplate("a()").Build()

	why := pat.Explain(tmpl.Instantiate(nil), nil)

	require.Error(t, why.ParseError)
	assert.Contains(t, why.ParseError.Error(), "2 statements")
}

func TestTopLevelTemplateOfTwoDeclarationsDropsNeither(t *testing.T) {
	tmpl := TopLevelTemplate("func a() {}\n\nfunc b() {}").Build()

	assert.Nil(t, tmpl.Instantiate(nil))
}

func TestTopLevelTemplateOfOneDeclarationStillInstantiates(t *testing.T) {
	tmpl := TopLevelTemplate("func a() {}").Context("func helper() {}").Build()

	got := tmpl.Instantiate(nil)

	require.NotNil(t, got)
	assert.Equal(t, "func a() {}", printer.Print(got))
}

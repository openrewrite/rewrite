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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

var slotTarget, slotMember = Expr("target"), Expr("member")

func TestTemplateLeavesAnExpressionOutOfASelectorsMember(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.InlineGet"),
		WithDisplayName("Inline get"),
		WithBefore(fmt.Sprintf(`get(%s, %s)`, slotTarget, slotMember)),
		WithAfter(fmt.Sprintf(`%s.%s`, slotTarget, slotMember)),
		WithCaptures(slotTarget, slotMember),
	)).RewriteRun(t,
		test.Golang(`
			package main

			var a = get(obj, b + c)
		`),
	)
}

func TestTemplateStillSubstitutesAnIdentifierIntoASelectorsMember(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.InlineGet"),
		WithDisplayName("Inline get"),
		WithBefore(fmt.Sprintf(`get(%s, %s)`, slotTarget, slotMember)),
		WithAfter(fmt.Sprintf(`%s.%s`, slotTarget, slotMember)),
		WithCaptures(slotTarget, slotMember),
	)).RewriteRun(t,
		test.Golang(`
			package main

			var a = get(obj, field)
		`, `
			package main

			var a = obj.field
		`),
	)
}

func TestTemplateLeavesAnExpressionOutOfADeclaredName(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.Declare"),
		WithDisplayName("Declare"),
		WithBefore(fmt.Sprintf(`declare(%s)`, slotTarget)),
		WithAfter(fmt.Sprintf(`var %s int`, slotTarget), asStatement),
		WithCaptures(slotTarget),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				declare(a.b)
			}
		`),
	)
}

func TestInstantiateLeavesAnExpressionOutOfAFunctionName(t *testing.T) {
	tmpl := TopLevelTemplate(fmt.Sprintf(`func %s() {}`, slotTarget)).Captures(slotTarget).Build()
	sum := ExpressionTemplate(`a + b`).Build().Instantiate(nil)

	got := tmpl.Instantiate(NewMatchResult().Bind(slotTarget, sum))

	assert.Nil(t, got)
}

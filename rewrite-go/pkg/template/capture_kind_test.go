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

func matchesArgumentOf(t *testing.T, c *Capture, src string) bool {
	t.Helper()
	pat := Expression(fmt.Sprintf(`declare(%s)`, c)).Captures(c).Build()
	return pat.Matches(Expression(src).Build().Tree(t), nil)
}

func TestNameCaptureBindsAnIdentifierOnly(t *testing.T) {
	name := Ident("name")

	assert.True(t, matchesArgumentOf(t, name, `declare(counter)`))
	assert.False(t, matchesArgumentOf(t, name, `declare(a + b)`))
	assert.False(t, matchesArgumentOf(t, name, `declare(a.b)`))
	assert.False(t, matchesArgumentOf(t, name, `declare(1)`))
}

func TestTypeCaptureBindsNoMereExpression(t *testing.T) {
	typ := TypeExpr("typ")

	assert.True(t, matchesArgumentOf(t, typ, `declare(Thing)`))
	assert.True(t, matchesArgumentOf(t, typ, `declare(pkg.Thing)`))
	assert.True(t, matchesArgumentOf(t, typ, `declare([]int)`))
	assert.True(t, matchesArgumentOf(t, typ, `declare(map[string]int)`))
	assert.False(t, matchesArgumentOf(t, typ, `declare(1 + 2)`))
	assert.False(t, matchesArgumentOf(t, typ, `declare(1)`))
	assert.False(t, matchesArgumentOf(t, typ, `declare(f())`))
	assert.False(t, matchesArgumentOf(t, typ, `declare(Thing{})`))
}

func TestTypeCaptureLeavesAnExpressionArgumentAlone(t *testing.T) {
	typ := TypeExpr("typ")
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.MakeSlice"),
		WithDisplayName("Make slice"),
		WithBefore(fmt.Sprintf(`mk(%s)`, typ)),
		WithAfter(fmt.Sprintf(`[]%s{}`, typ)),
		WithCaptures(typ),
	)).RewriteRun(t,
		test.Golang(`
			package main

			var a = mk(1 + 2)
		`),
	)
}

func TestTypeCaptureLeavesAPointerOutOfACompositeLiteralsType(t *testing.T) {
	typ := TypeExpr("typ")
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.ZeroLiteral"),
		WithDisplayName("Zero literal"),
		WithBefore(fmt.Sprintf(`zero[%s]()`, typ)),
		WithAfter(fmt.Sprintf(`%s{}`, typ)),
		WithCaptures(typ),
	)).RewriteRun(t,
		test.Golang(`
			package main

			type S struct{}

			var a = zero[*S]()
		`),
	)
}

func TestTypeCaptureStillNamesACompositeLiteralsType(t *testing.T) {
	typ := TypeExpr("typ")
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.ZeroLiteral"),
		WithDisplayName("Zero literal"),
		WithBefore(fmt.Sprintf(`zero[%s]()`, typ)),
		WithAfter(fmt.Sprintf(`%s{}`, typ)),
		WithCaptures(typ),
	)).RewriteRun(t,
		test.Golang(`
			package main

			type S struct{}

			var a = zero[S]()
		`, `
			package main

			type S struct{}

			var a = S{}
		`),
	)
}

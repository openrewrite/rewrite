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

var headerLeft, headerRight = Expr("l"), Expr("r")

func inliningEq() *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.InlineEq"),
		WithDisplayName("Inline eq"),
		WithBefore(fmt.Sprintf(`eq(%s, %s)`, headerLeft, headerRight)),
		WithAfter(fmt.Sprintf(`%s == %s`, headerLeft, headerRight)),
		WithCaptures(headerLeft, headerRight),
	))
}

func TestTemplateCapturedCompositeInIfHeaderIsParenthesized(t *testing.T) {
	inliningEq().RewriteRun(t,
		test.Golang(`
			package main

			type T struct{}

			func f(v T) {
				if eq(v, T{}) {
				}
			}
		`, `
			package main

			type T struct{}

			func f(v T) {
				if v == (T{}) {
				}
			}
		`),
	)
}

func TestTemplateCapturedCompositeInForHeaderIsParenthesized(t *testing.T) {
	inliningEq().RewriteRun(t,
		test.Golang(`
			package main

			type T struct{}

			func f(v T) {
				for eq(v, T{}) {
				}
			}
		`, `
			package main

			type T struct{}

			func f(v T) {
				for v == (T{}) {
				}
			}
		`),
	)
}

func TestTemplateCapturedCompositeDelimitedInAHeaderIsLeftAlone(t *testing.T) {
	inliningEq().RewriteRun(t,
		test.Golang(`
			package main

			type T struct{}

			func f(v T) {
				for !eq(v, T{}) {
				}
			}
		`, `
			package main

			type T struct{}

			func f(v T) {
				for !(v == T{}) {
				}
			}
		`),
	)
}

func TestTemplateCapturedCompositeOutsideAHeaderIsLeftAlone(t *testing.T) {
	inliningEq().RewriteRun(t,
		test.Golang(`
			package main

			type T struct{}

			func f(v T) bool {
				return eq(v, T{})
			}
		`, `
			package main

			type T struct{}

			func f(v T) bool {
				return v == T{}
			}
		`),
	)
}

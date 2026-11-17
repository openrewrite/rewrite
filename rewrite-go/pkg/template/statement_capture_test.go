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
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

var capturedBody = Stmt("body")

func unwrappingIfTrue(after string) *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.UnwrapIfTrue"),
		WithDisplayName("Unwrap if true"),
		WithBefore("if true {\n"+capturedBody.String()+"\n}"),
		WithAfter(after, func(s *beforeSpec) {
			k := ScaffoldStatement
			s.kind = &k
		}),
		WithCaptures(capturedBody),
	))
}

func TestTemplateStatementCaptureBoundToACall(t *testing.T) {
	unwrappingIfTrue(capturedBody.String()).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				if true {
					g()
				}
			}
		`, `
			package main

			func f() {
				g()
			}
		`),
	)
}

func TestTemplateStatementCaptureBoundToAnAssignment(t *testing.T) {
	unwrappingIfTrue("{\n"+capturedBody.String()+"\n}").RewriteRun(t,
		test.Golang(`
			package main

			func f(x int) {
				if true {
					x = 2
				}
			}
		`, `
			package main

			func f(x int) {
				{
					x = 2
				}
			}
		`),
	)
}

func TestTemplateStatementCaptureBoundToAnIf(t *testing.T) {
	unwrappingIfTrue(capturedBody.String()).RewriteRun(t,
		test.Golang(`
			package main

			func f(x int) {
				if true {
					if x > 0 {
						g()
					}
				}
			}
		`, `
			package main

			func f(x int) {
				if x > 0 {
					g()
				}
			}
		`),
	)
}

func TestTemplateStatementCaptureBoundToAnExpressionStatement(t *testing.T) {
	unwrappingIfTrue(capturedBody.String()).RewriteRun(t,
		test.Golang(`
			package main

			func f(x any) {
				if true {
					x.(int)
				}
			}
		`, `
			package main

			func f(x any) {
				x.(int)
			}
		`),
	)
}

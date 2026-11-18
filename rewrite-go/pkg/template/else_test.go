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

func TestTemplateUnwrappingAnElseIfKeepsTheElseABlock(t *testing.T) {
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

			func f(a bool, x int) {
				if a {
					x = 1
				} else if true {
					x = 2
				}
			}
		`, `
			package main

			func f(a bool, x int) {
				if a {
					x = 1
				} else {
					x = 2
				}
			}
		`),
	)
}

func TestTemplateReplacingAnElseIfWithACallKeepsTheElseABlock(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.ReplaceElseIf"),
		WithDisplayName("Replace else if"),
		WithBefore("if done() {\n}", asStatement),
		WithAfter("finish()", asStatement),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f(a bool) {
				if a {
				} else if done() {
				}
			}
		`, `
			package main

			func f(a bool) {
				if a {
				} else {
					finish()
				}
			}
		`),
	)
}

func TestTemplateReplacingAnElseIfWithAnIfKeepsTheElseIf(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.ReplaceElseIf"),
		WithDisplayName("Replace else if"),
		WithBefore("if done() {\n}", asStatement),
		WithAfter("if finished() {\n}", asStatement),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f(a bool) {
				if a {
				} else if done() {
				}
			}
		`, `
			package main

			func f(a bool) {
				if a {
				} else if finished() {
				}
			}
		`),
	)
}

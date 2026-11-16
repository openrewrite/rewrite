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

var grammarArg = Expr("arg")

func grammarRecipe(before, after string, caps ...*Capture) *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.Grammar"),
		WithDisplayName("Grammar"),
		WithBefore(before),
		WithAfter(after),
		WithCaptures(caps...),
	))
}

func TestTemplateLeavesASpreadOutOfAnAssignment(t *testing.T) {
	grammarRecipe(fmt.Sprintf(`wrap(%s)`, grammarArg), grammarArg.String(), grammarArg).RewriteRun(t,
		test.Golang(`
			package main

			func wrap(xs ...int) int { return 0 }

			func f(xs []int) {
				_ = wrap(xs...)
			}
		`),
	)
}

func TestTemplateLeavesASpreadOutOfAnOperand(t *testing.T) {
	grammarRecipe(fmt.Sprintf(`wrap(%s)`, grammarArg), fmt.Sprintf(`inc(%s + 1)`, grammarArg), grammarArg).RewriteRun(t,
		test.Golang(`
			package main

			func wrap(xs ...int) int { return 0 }

			func f(xs []int) {
				_ = wrap(xs...)
			}
		`),
	)
}

func TestTemplateLeavesASpreadOutOfAnArgumentBeforeTheLast(t *testing.T) {
	args := Expr("args").Variadic(0, -1)
	grammarRecipe(fmt.Sprintf(`old(%s)`, args), fmt.Sprintf(`neu(%s, 0)`, args), args).RewriteRun(t,
		test.Golang(`
			package main

			func f(xs []int) {
				old(xs...)
			}
		`),
	)
}

func TestTemplateStillPassesASpreadOnAsTheLastArgument(t *testing.T) {
	grammarRecipe(fmt.Sprintf(`old(%s)`, grammarArg), fmt.Sprintf(`neu(0, %s)`, grammarArg), grammarArg).RewriteRun(t,
		test.Golang(`
			package main

			func f(xs []int) {
				old(xs...)
			}
		`, `
			package main

			func f(xs []int) {
				neu(0, xs...)
			}
		`),
	)
}

func TestTemplateLeavesASelectorOutOfTheLeftOfAShortVarDecl(t *testing.T) {
	grammarRecipe(fmt.Sprintf(`declare(%s)`, grammarArg), fmt.Sprintf(`%s := 1`, grammarArg), grammarArg).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				declare(a.b)
			}
		`),
	)
}

func TestTemplateLeavesANonCommunicationOutOfASelectCase(t *testing.T) {
	grammarRecipe(fmt.Sprintf(`<-%s`, grammarArg), fmt.Sprintf(`take(%s)`, grammarArg), grammarArg).RewriteRun(t,
		test.Golang(`
			package main

			func f(ch chan int) {
				select {
				case <-ch:
				}
			}
		`),
	)
}

func TestTemplateStillReplacesAReceiveInASelectCase(t *testing.T) {
	grammarRecipe(`<-in()`, `<-out()`).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				select {
				case v := <-in():
					_ = v
				}
			}
		`, `
			package main

			func f() {
				select {
				case v := <-out():
					_ = v
				}
			}
		`),
	)
}

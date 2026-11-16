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

var regressionArg = Expr("arg")

func TestTemplateReplacesALoopConditionWithANegatedField(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.InlineIsDone"),
		WithDisplayName("Inline isDone"),
		WithBefore(fmt.Sprintf(`isDone(%s)`, regressionArg)),
		WithAfter(fmt.Sprintf(`!%s.ok`, regressionArg)),
		WithCaptures(regressionArg),
	)).RewriteRun(t,
		test.Golang(`
			package main

			type S struct{ ok bool }

			func isDone(s S) bool { return true }

			func f(s S) {
				for i := 0; isDone(s); i++ {
				}
			}
		`, `
			package main

			type S struct{ ok bool }

			func isDone(s S) bool { return true }

			func f(s S) {
				for i := 0; !s.ok; i++ {
				}
			}
		`),
	)
}

func TestTemplateOfALoopOnAFieldConditionApplies(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.Spin"),
		WithDisplayName("Spin"),
		WithBefore(`spin()`),
		WithAfter("for !done {\n}", asStatement),
	)).RewriteRun(t,
		test.Golang(`
			package main

			var done bool

			func f() {
				spin()
			}
		`, `
			package main

			var done bool

			func f() {
				for !done {
				}
			}
		`),
	)
}

func TestTemplateOfASelectOnABareReceiveApplies(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.Wait"),
		WithDisplayName("Wait"),
		WithBefore(fmt.Sprintf(`wait(%s)`, regressionArg)),
		WithAfter(fmt.Sprintf("select {\ncase <-%s:\n}", regressionArg), asStatement),
		WithCaptures(regressionArg),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f(ch chan int) {
				wait(ch)
			}
		`, `
			package main

			func f(ch chan int) {
				select {
				case <-ch:
				}
			}
		`),
	)
}

func TestTemplateLocalKeepsItsNameWhereTheCaptureNamesOnlyAFieldOfIt(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.Must"),
		WithDisplayName("Must"),
		WithBefore(fmt.Sprintf(`must(%s)`, regressionArg)),
		WithAfter(fmt.Sprintf("if err := %s; err != nil {\n\tpanic(err)\n}", regressionArg), asStatement),
		WithCaptures(regressionArg),
	)).RewriteRun(t,
		test.Golang(`
			package main

			type R struct{ err error }

			func must(e error) {}

			func f(r R) {
				must(r.err)
			}
		`, `
			package main

			type R struct{ err error }

			func must(e error) {}

			func f(r R) {
				if err := r.err; err != nil {
					panic(err)
				}
			}
		`),
	)
}

func TestNewRecipeKeepsTheCgoImport(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.Release"),
		WithDisplayName("Release"),
		WithBefore(fmt.Sprintf(`C.free(%s)`, regressionArg)),
		WithAfter(fmt.Sprintf(`release(%s)`, regressionArg)),
		WithCaptures(regressionArg),
	)).RewriteRun(t,
		test.Golang(`
			package main

			// #include <stdlib.h>
			import "C"

			func release(p any) {}

			func f(p any) {
				C.free(p)
			}
		`, `
			package main

			// #include <stdlib.h>
			import "C"

			func release(p any) {}

			func f(p any) {
				release(p)
			}
		`),
	)
}

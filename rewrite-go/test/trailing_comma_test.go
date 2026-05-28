/*
 * Copyright 2025 the original author or authors.
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

package test

import (
	"testing"

	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

func TestParseTrailingCommaSliceLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				items := []int{
					1,
					2,
					3,
				}
				use(items)
			}
		`))
}

func TestParseTrailingCommaMapLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string]int{
					"a": 1,
					"b": 2,
				}
				use(m)
			}
		`))
}

func TestParseTrailingCommaStructLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				p := Point{
					X: 1,
					Y: 2,
				}
				use(p)
			}
		`))
}

func TestParseTrailingCommaFuncCall(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				result := doWork(
					arg1,
					arg2,
				)
				use(result)
			}
		`))
}

func TestParseNoTrailingCommaInline(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				items := []int{1, 2, 3}
				use(items)
			}
		`))
}

func TestParseTrailingCommaNestedComposite(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				items := []Point{
					{X: 1, Y: 2},
					{X: 3, Y: 4},
				}
				use(items)
			}
		`))
}

func TestParseTrailingCommaFuncArgs(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				fmt.Printf(
					"name=%s age=%d\n",
					name,
					age,
				)
			}
		`))
}

func TestParseTrailingCommaAnonymousStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				tests := []struct {
					name string
					want int
				}{
					{
						name: "test1",
						want: 1,
					},
					{
						name: "test2",
						want: 2,
					},
				}
				use(tests)
			}
		`))
}

func TestParseTrailingCommaFuncTypeParams(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var f = map[string]func(
				a int,
				b int,
			) error{}
		`))
}

func TestParseTrailingCommaMapOfSlices(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string][]int{
					"a": {1, 2, 3},
					"b": {4, 5, 6},
				}
				use(m)
			}
		`))
}

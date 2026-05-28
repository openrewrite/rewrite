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

func TestParseAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func greet() string {
				name = "world"
				return name
			}
		`))
}

func TestParseShortVarDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func hello() {
				x := 1
			}
		`))
}

func TestParseCompoundAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func add() {
				x += 1
			}
		`))
}

func TestParseIncrement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func inc() {
				x++
			}
		`))
}

func TestParseDecrement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func dec() {
				x--
			}
		`))
}

func TestParseMultiAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x, y = 1, 2
			}
		`))
}

func TestParseMultiShortVarDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x, y := 1, 2
			}
		`))
}

func TestParseMultiAssignFromFunc(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				val, err := divide(10, 2)
			}
		`))
}

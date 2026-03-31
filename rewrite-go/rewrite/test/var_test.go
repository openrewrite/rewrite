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

	. "github.com/openrewrite/rewrite/pkg/test"
)

func TestParseVarWithType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func hello() {
				var x int
			}
		`))
}

func TestParseVarWithInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func hello() {
				var x = 5
			}
		`))
}

func TestParseVarWithTypeAndInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func hello() {
				var x int = 5
			}
		`))
}

func TestParseConst(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func hello() {
				const x = 5
			}
		`))
}

func TestParseTopLevelVar(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var x int

			func hello() {
			}
		`))
}

func TestParseTopLevelConst(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const name = "world"

			func hello() {
			}
		`))
}

func TestParseGroupedVar(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var (
				x int
				y string
			)
		`))
}

func TestParseGroupedConst(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			const (
				a = 1
				b = "hello"
			)
		`))
}

func TestParseGroupedVarWithInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var (
				x int = 5
				y     = "hello"
			)
		`))
}

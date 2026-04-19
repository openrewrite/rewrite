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

func TestParseSliceTypeParam(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func iterate(items []string) {
			}
		`))
}

func TestParseArrayTypeParam(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func process(data [5]int) {
			}
		`))
}

func TestParseArrayAccess(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func get(items []int) int {
				return items[0]
			}
		`))
}

func TestParseSliceExpression(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func sub(items []int) []int {
				return items[1:3]
			}
		`))
}

func TestParseSliceExpressionOpenEnd(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func tail(items []int) []int {
				return items[1:]
			}
		`))
}

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

var qualifiedArg = Expr("arg")

func replacingStringsToUpper() *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.ReplaceToUpper"),
		WithDisplayName("Replace ToUpper"),
		WithBefore(fmt.Sprintf(`strings.ToUpper(%s)`, qualifiedArg), Imports("strings")),
		WithAfter(fmt.Sprintf(`up(%s)`, qualifiedArg)),
		WithCaptures(qualifiedArg),
	))
}

func TestPatternPackageQualifierDoesNotMatchAParameterOfTheSameName(t *testing.T) {
	replacingStringsToUpper().RewriteRun(t,
		test.Golang(`
			package main

			type S struct{}

			func (S) ToUpper(s string) string { return s }

			func f(strings S) string {
				return strings.ToUpper("a")
			}
		`),
	)
}

func TestPatternPackageQualifierDoesNotMatchALocalOfTheSameName(t *testing.T) {
	replacingStringsToUpper().RewriteRun(t,
		test.Golang(`
			package main

			type S struct{}

			func (S) ToUpper(s string) string { return s }

			func f() string {
				var strings S
				return strings.ToUpper("a")
			}
		`),
	)
}

func TestPatternPackageQualifierDoesNotMatchAnotherPackageOfTheSameName(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.ReplaceRandIntn"),
		WithDisplayName("Replace rand.Int"),
		WithBefore(`rand.Int()`, Imports("math/rand")),
		WithAfter(`0`),
	)).RewriteRun(t,
		test.Golang(`
			package main

			import "math/rand/v2"

			var a = rand.Int()
		`),
	)
}

func TestPatternPackageQualifierStillMatchesThePackage(t *testing.T) {
	replacingStringsToUpper().RewriteRun(t,
		test.Golang(`
			package main

			import "strings"

			func up(s string) string { return s }

			var a = strings.ToUpper("a")
			var b = strings.TrimSpace("b")
		`, `
			package main

			import "strings"

			func up(s string) string { return s }

			var a = up("a")
			var b = strings.TrimSpace("b")
		`),
	)
}

func TestPatternPackageQualifierStillMatchesAnUnresolvedCandidate(t *testing.T) {
	replacingStringsToUpper().RewriteRun(t,
		test.Golang(`
			package main

			var a = strings.ToUpper("a")
		`, `
			package main

			var a = up("a")
		`),
	)
}

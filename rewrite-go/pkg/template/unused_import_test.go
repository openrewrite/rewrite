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

var upperValue = Expr("value")

func replacingStringsToUpperByLocal() *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.UseLocalUpper"),
		WithDisplayName("Use local upper"),
		WithBefore(fmt.Sprintf(`strings.ToUpper(%s)`, upperValue), Imports("strings")),
		WithAfter(fmt.Sprintf(`upper(%s)`, upperValue)),
		WithCaptures(upperValue),
	))
}

func TestNewRecipeRemovesAnImportOnlyTheReplacedCodeUsed(t *testing.T) {
	replacingStringsToUpperByLocal().RewriteRun(t,
		test.Golang(`
			package main

			import "strings"

			func upper(s string) string { return s }

			var a = strings.ToUpper("x")
		`, `
			package main

			func upper(s string) string { return s }

			var a = upper("x")
		`),
	)
}

func TestNewRecipeKeepsAnImportTheFileStillUses(t *testing.T) {
	replacingStringsToUpperByLocal().RewriteRun(t,
		test.Golang(`
			package main

			import "strings"

			func upper(s string) string { return s }

			var a = strings.ToUpper("x")
			var b = strings.ToLower("y")
		`, `
			package main

			import "strings"

			func upper(s string) string { return s }

			var a = upper("x")
			var b = strings.ToLower("y")
		`),
	)
}

func TestNewRecipeLeavesAnImportTheReplacedCodeDidNotUse(t *testing.T) {
	replacingStringsToUpperByLocal().RewriteRun(t,
		test.Golang(`
			package main

			import (
				_ "embed"
				"os"
				"strings"
			)

			func upper(s string) string { return s }

			var a = strings.ToUpper("x")
		`, `
			package main

			import (
				_ "embed"
				"os"
			)

			func upper(s string) string { return s }

			var a = upper("x")
		`),
	)
}

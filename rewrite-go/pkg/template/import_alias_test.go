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

var aliasedArg = Expr("arg")

func upperThroughStrings(afterOpts ...BeforeOption) *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.UseStringsToUpper"),
		WithDisplayName("Use strings.ToUpper"),
		WithBefore(fmt.Sprintf(`up(%s)`, aliasedArg)),
		WithAfter(fmt.Sprintf(`strings.ToUpper(%s)`, aliasedArg), afterOpts...),
		WithCaptures(aliasedArg),
	))
}

func TestTemplateQualifiesWithTheAliasTheFileImportsThePackageUnder(t *testing.T) {
	upperThroughStrings(Imports("strings"), SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			import str "strings"

			func up(s string) string { return s }

			var a = up(str.TrimSpace(" a"))
		`, `
			package main

			import str "strings"

			func up(s string) string { return s }

			var a = str.ToUpper(str.TrimSpace(" a"))
		`),
	)
}

func TestTemplateLeavesAQualifierItCannotWriteForADotImport(t *testing.T) {
	upperThroughStrings(Imports("strings"), SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			import . "strings"

			func up(s string) string { return s }

			var a = up(TrimSpace(" a"))
		`),
	)
}

func TestTemplateLeavesAnAliasALocalShadows(t *testing.T) {
	upperThroughStrings(Imports("strings"), SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			import str "strings"

			func up(s string) string { return s }

			func f(str int) string {
				return up("a")
			}

			var b = str.TrimSpace(" b")
		`),
	)
}

func TestTemplateKeepsItsQualifierWhereTheFileImportsThePackageUnaliased(t *testing.T) {
	upperThroughStrings(Imports("strings"), SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			import "strings"

			func up(s string) string { return s }

			var a = up(strings.TrimSpace(" a"))
		`, `
			package main

			import "strings"

			func up(s string) string { return s }

			var a = strings.ToUpper(strings.TrimSpace(" a"))
		`),
	)
}

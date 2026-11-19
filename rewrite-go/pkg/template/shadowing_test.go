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

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

var upperArg = Expr("s")

func upperToStrings(afterOpts ...BeforeOption) *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.UseStringsToUpper"),
		WithDisplayName("Use strings.ToUpper"),
		WithBefore(fmt.Sprintf(`upper(%s)`, upperArg)),
		WithAfter(fmt.Sprintf(`strings.ToUpper(%s)`, upperArg), afterOpts...),
		WithCaptures(upperArg),
	))
}

func TestTemplateLeavesAPackageQualifierAParameterShadows(t *testing.T) {
	upperToStrings(Imports("strings"), SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			func upper(s string) string { return s }

			func f(strings []string) string {
				return upper(strings[0])
			}
		`),
	)
}

func TestTemplateLeavesAPackageQualifierALocalShadows(t *testing.T) {
	upperToStrings(Imports("strings"), SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			func upper(s string) string { return s }

			func f() string {
				strings := []string{"a"}
				for _, s := range strings {
					return upper(s)
				}
				return ""
			}
		`),
	)
}

func TestTemplateLeavesAPackageQualifierARangeVariableShadows(t *testing.T) {
	upperToStrings(SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			func upper(s string) string { return s }

			func f(m map[string]string) {
				for strings := range m {
					_ = upper(strings)
				}
			}
		`),
	)
}

func TestTemplateUsesAPackageQualifierDeclaredOnlyAfterTheSite(t *testing.T) {
	upperToStrings(Imports("strings"), SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			func upper(s string) string { return s }

			func f() {
				_ = upper("a")
				strings := 1
				_ = strings
			}
		`, `
			package main

			import "strings"

			func upper(s string) string { return s }

			func f() {
				_ = strings.ToUpper("a")
				strings := 1
				_ = strings
			}
		`),
	)
}

func TestTemplateUsesAPackageQualifierShadowedInAnotherFunction(t *testing.T) {
	upperToStrings(Imports("strings"), SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			func upper(s string) string { return s }

			func g(strings int) {}

			func f() string {
				return upper("a")
			}
		`, `
			package main

			import "strings"

			func upper(s string) string { return s }

			func g(strings int) {}

			func f() string {
				return strings.ToUpper("a")
			}
		`),
	)
}

// A template may mean a name at the site, as `err` in `if err != nil`.
func TestTemplateStillBindsAnUnqualifiedNameToALocal(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.CheckErr"),
		WithDisplayName("Check err"),
		WithBefore(`check()`),
		WithAfter("if err != nil {\n\treturn err\n}"),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f() error {
				var err error
				check()
				return nil
			}
		`, `
			package main

			func f() error {
				var err error
				if err != nil {
					return err
				}
				return nil
			}
		`),
	)
}

func TestPackageNameOfAnImportPath(t *testing.T) {
	for path, want := range map[string]string{
		"strings":                     "strings",
		"encoding/json":               "json",
		"encoding/json/v2":            "json",
		"gopkg.in/yaml.v3":            "yaml",
		"github.com/mattn/go-sqlite3": "sqlite3",
		"github.com/google/uuid":      "uuid",
	} {
		assert.Equal(t, want, packageNameOf(path), path)
	}
}

func TestTemplateLeavesAPackageQualifierAForInitShadows(t *testing.T) {
	upperToStrings(SourceImports("strings")).RewriteRun(t,
		test.Golang(`
			package main

			func upper(s string) string { return s }

			func f() {
				for strings := "a"; ; {
					_ = upper(strings)
				}
			}
		`),
	)
}

// The range expression is evaluated outside the loop variables' scope.
func TestTemplateUsesAPackageQualifierInARangeExpressionTheLoopShadowsOnlyInside(t *testing.T) {
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.UseStringsFields"),
		WithDisplayName("Use strings.Fields"),
		WithBefore(fmt.Sprintf(`fields(%s)`, upperArg)),
		WithAfter(fmt.Sprintf(`strings.Fields(%s)`, upperArg), Imports("strings"), SourceImports("strings")),
		WithCaptures(upperArg),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func fields(s string) []string { return nil }

			func f() {
				for _, strings := range fields("a b") {
					_ = strings
				}
			}
		`, `
			package main

			import "strings"

			func fields(s string) []string { return nil }

			func f() {
				for _, strings := range strings.Fields("a b") {
					_ = strings
				}
			}
		`),
	)
}

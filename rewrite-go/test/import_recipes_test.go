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

package test

import (
	"testing"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func strPtr(s string) *string { return &s }

// stripQualifierAttribution rewrites `y.Hello()` into an equivalent call
// whose qualifier identifier and method type carry no attribution,
// mimicking a hand-built cross-package call node.
type stripQualifierAttribution struct {
	recipe.Base
}

func (r *stripQualifierAttribution) Name() string {
	return "org.openrewrite.golang.test.StripQualifierAttribution"
}
func (r *stripQualifierAttribution) DisplayName() string { return "Strip qualifier attribution" }
func (r *stripQualifierAttribution) Description() string {
	return "Replaces the qualifier of `y.Hello()` with an un-attributed identifier."
}

func (r *stripQualifierAttribution) Editor() recipe.TreeVisitor {
	return visitor.Init(&stripQualifierVisitor{})
}

type stripQualifierVisitor struct {
	visitor.GoVisitor
}

func (v *stripQualifierVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	if mi.Name == nil || mi.Name.Name != "Hello" || mi.Select == nil {
		return v.GoVisitor.VisitMethodInvocation(mi, p)
	}
	qualifier, ok := mi.Select.Element.(*java.Identifier)
	if !ok {
		return v.GoVisitor.VisitMethodInvocation(mi, p)
	}
	c := *mi
	sel := *mi.Select
	sel.Element = &java.Identifier{ID: uuid.New(), Prefix: qualifier.Prefix, Name: qualifier.Name}
	c.Select = &sel
	c.MethodType = nil
	return v.GoVisitor.VisitMethodInvocation(&c, p)
}

type handBuiltCallThenRemoveUnused struct {
	recipe.Base
}

func (r *handBuiltCallThenRemoveUnused) Name() string {
	return "org.openrewrite.golang.test.HandBuiltCallThenRemoveUnused"
}
func (r *handBuiltCallThenRemoveUnused) DisplayName() string {
	return "Hand-built call then remove unused"
}
func (r *handBuiltCallThenRemoveUnused) Description() string {
	return "Strips qualifier attribution, then removes unused imports."
}
func (r *handBuiltCallThenRemoveUnused) RecipeList() []recipe.Recipe {
	return []recipe.Recipe{&stripQualifierAttribution{}, &recipes.RemoveUnusedImports{}}
}

func TestRemoveUnusedImports_KeepsImportReferencedByUnattributedQualifier(t *testing.T) {
	// given a file whose only use of `github.com/x/y` is a call whose
	// qualifier is stripped of attribution (as a hand-built node would be)
	// when RemoveUnusedImports runs after
	// then the import survives on the lexical qualifier match alone
	spec := NewRecipeSpec().WithRecipe(&handBuiltCallThenRemoveUnused{})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"
				"github.com/x/y"
			)

			func main() {
				fmt.Println("hi")
				_ = y.Hello()
			}
		`),
	)
}

func TestAddImport_NoOpWhenAlreadyImported(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "fmt"})
	spec.RewriteRun(t,
		Golang(`
			package main

			import "fmt"

			func main() { fmt.Println("hi") }
		`),
	)
}

func TestAddImport_AddsToExistingBlock(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "strings"})
	before := `
		package main

		import (
			"fmt"
		)

		func main() { fmt.Println("hi") }
	`
	after := `
		package main

		import (
			"fmt"
			"strings"
		)

		func main() { fmt.Println("hi") }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestAddImport_AddsToFileWithNoImports(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "fmt"})
	before := `
		package main

		func main() {}
	`
	after := `
		package main

		import "fmt"

		func main() {}
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestAddImport_AddsAliasedImportToFileWithNoImports(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "net/http/pprof", Alias: strPtr("_")})
	before := `
		package main

		func main() {}
	`
	after := `
		package main

		import _ "net/http/pprof"

		func main() {}
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestAddImport_PromotesAliasedImportToGroupedBlock(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "fmt"})
	before := `
		package main

		import _ "net/http/pprof"

		func main() {}
	`
	after := `
		package main

		import (
			_ "net/http/pprof"
			"fmt"
		)

		func main() {}
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestAddImport_OnlyIfReferenced_NoOpWhenNotReferenced(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{
		PackagePath:      "github.com/x/y",
		OnlyIfReferenced: true,
	})
	spec.RewriteRun(t,
		Golang(`
			package main

			func main() {}
		`),
	)
}

func TestAddImport_AliasedFormDoesNotMatchRegular(t *testing.T) {
	// `import yy "github.com/x/y"` is present; AddImport(github.com/x/y, alias=nil)
	// should treat it as MISSING the regular form because the alias differs.
	// (This mirrors the Java AddImport semantics for explicit alias asks.)
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{
		PackagePath: "github.com/x/y",
		Alias:       strPtr("yy"),
	})
	spec.RewriteRun(t,
		Golang(`
			package main

			import yy "github.com/x/y"

			func main() { _ = yy.Hello() }
		`),
	)
}

func TestRemoveImport_DeletesMatching(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "strings", Force: true})
	before := `
		package main

		import (
			"fmt"
			"strings"
		)

		func main() { fmt.Println(strings.ToUpper("hi")) }
	`
	after := `
		package main

		import (
			"fmt"
		)

		func main() { fmt.Println(strings.ToUpper("hi")) }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveImport_KeepsStillReferenced(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "strings"})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"
				"strings"
			)

			func main() { fmt.Println(strings.ToUpper("hi")) }
		`),
	)
}

func TestRemoveImport_DeletesUnreferenced(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "strings"})
	before := `
		package main

		import (
			"fmt"
			"strings"
		)

		func main() { fmt.Println("hi") }
	`
	after := `
		package main

		import (
			"fmt"
		)

		func main() { fmt.Println("hi") }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveImport_DeletesUnreferencedAliased(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "strings"})
	before := `
		package main

		import (
			"fmt"
			s "strings"
		)

		func main() { fmt.Println("hi") }
	`
	after := `
		package main

		import (
			"fmt"
		)

		func main() { fmt.Println("hi") }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveImport_KeepsReferencedAliased(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "strings"})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"
				s "strings"
			)

			func main() { fmt.Println(s.ToUpper("hi")) }
		`),
	)
}

func TestRemoveImport_KeepsBlankImport(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "github.com/x/y"})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				_ "github.com/x/y"
				"fmt"
			)

			func main() { fmt.Println("hi") }
		`),
	)
}

func TestRemoveImport_ForceDeletesBlankImport(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "github.com/x/y", Force: true})
	before := `
		package main

		import (
			_ "github.com/x/y"
			"fmt"
		)

		func main() { fmt.Println("hi") }
	`
	after := `
		package main

		import (
			"fmt"
		)

		func main() { fmt.Println("hi") }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveImport_KeepsDotImport(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "github.com/x/y"})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				. "github.com/x/y"
				"fmt"
			)

			func main() { fmt.Println("hi") }
		`),
	)
}

func TestRemoveImport_NoOpWhenAbsent(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveImport{PackagePath: "strings"})
	spec.RewriteRun(t,
		Golang(`
			package main

			import "fmt"

			func main() { fmt.Println("hi") }
		`),
	)
}

func TestRemoveUnusedImports_DropsUnreferenced(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	before := `
		package main

		import (
			"fmt"
			"strings"
		)

		func main() { fmt.Println("hi") }
	`
	after := `
		package main

		import (
			"fmt"
		)

		func main() { fmt.Println("hi") }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveUnusedImports_DropsUnreferencedAliased(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	before := `
		package main

		import (
			"fmt"
			s "strings"
		)

		func main() { fmt.Println("hi") }
	`
	after := `
		package main

		import (
			"fmt"
		)

		func main() { fmt.Println("hi") }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveUnusedImports_KeepsReferencedAliased(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"
				s "strings"
			)

			func main() { fmt.Println(s.ToUpper("hi")) }
		`),
	)
}

func TestRemoveUnusedImports_PreservesBlankImports(t *testing.T) {
	// Blank imports stay — they exist for init() side-effects.
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				_ "github.com/x/y"
				"fmt"
			)

			func main() { fmt.Println("hi") }
		`),
	)
}

func TestRemoveUnusedImports_NoOpWhenAllUsed(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"
				"strings"
			)

			func main() { fmt.Println(strings.ToUpper("hi")) }
		`),
	)
}

func TestOrderImports_IdempotentOnAlreadyOrdered(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.OrderImports{})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"

				"github.com/x/y"
			)

			func main() {
				fmt.Println("hi")
				_ = y.Hello()
			}
		`),
	)
}

func TestOrderImports_ReorderJumbledBlock(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.OrderImports{})
	before := `
		package main

		import (
			"github.com/x/y"
			"fmt"
		)

		func main() {
			fmt.Println("hi")
			_ = y.Hello()
		}
	`
	after := `
		package main

		import (
			"fmt"

			"github.com/x/y"
		)

		func main() {
			fmt.Println("hi")
			_ = y.Hello()
		}
	`
	spec.RewriteRun(t, Golang(before, after))
}

// goimports orders within each group alphabetically and inserts a blank
// line between groups. Run on a 3-import block that needs both: cross-group
// reorder of "github.com/x/y" → tail, and within-stdlib reorder of
// "strings" before "fmt".
func TestOrderImports_AlphabeticalWithinGroupAndBlankLineBetween(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.OrderImports{})
	before := `
		package main

		import (
			"github.com/x/y"
			"strings"
			"fmt"
		)

		func main() {
			fmt.Println(strings.ToUpper("hi"))
			_ = y.Hello()
		}
	`
	after := `
		package main

		import (
			"fmt"
			"strings"

			"github.com/x/y"
		)

		func main() {
			fmt.Println(strings.ToUpper("hi"))
			_ = y.Hello()
		}
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveUnusedImports_DropsFirstOfGroupedBlock(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	before := `
		package main

		import (
			"io/ioutil"

			"github.com/x/y"
		)

		func main() { y.Hello() }
	`
	after := `
		package main

		import (
			"github.com/x/y"
		)

		func main() { y.Hello() }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveUnusedImports_KeepsGroupSeparatorWhenDroppingMiddle(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	before := `
		package main

		import (
			"fmt"
			"io/ioutil"

			"github.com/x/y"
		)

		func main() { fmt.Println(y.Hello()) }
	`
	after := `
		package main

		import (
			"fmt"

			"github.com/x/y"
		)

		func main() { fmt.Println(y.Hello()) }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestAddImport_OpensNewLeadingGroup(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "fmt"})
	before := `
		package main

		import (
			"github.com/x/y"
		)

		func main() { y.Hello() }
	`
	after := `
		package main

		import (
			"fmt"

			"github.com/x/y"
		)

		func main() { y.Hello() }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestAddImport_JoinsExistingLeadingGroup(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "os"})
	before := `
		package main

		import (
			"fmt"

			"github.com/x/y"
		)

		func main() { fmt.Println(y.Hello()) }
	`
	after := `
		package main

		import (
			"fmt"
			"os"

			"github.com/x/y"
		)

		func main() { fmt.Println(y.Hello()) }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestAddImport_JoinsExistingTrailingGroup(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "example.com/plainpkg"})
	before := `
		package main

		import (
			"os"

			"github.com/x/y"
		)

		func main() { os.Exit(0); y.Hello() }
	`
	after := `
		package main

		import (
			"os"

			"github.com/x/y"
			"example.com/plainpkg"
		)

		func main() { os.Exit(0); y.Hello() }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestAddImport_OpensNewTrailingGroup(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&recipes.AddImport{PackagePath: "github.com/x/y"})
	before := `
		package main

		import (
			"fmt"
			"os"
		)

		func main() { fmt.Println(os.Args) }
	`
	after := `
		package main

		import (
			"fmt"
			"os"

			"github.com/x/y"
		)

		func main() { fmt.Println(os.Args) }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRemoveUnusedImports_KeepsVersionedPathImportUsedByQualifier(t *testing.T) {
	// Attribution cannot resolve these paths, so the lexical qualifier is
	// the only signal keeping the imports.
	spec := NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				"github.com/x/y/v2"
				"gopkg.in/yaml.v3"
			)

			func f(b []byte, out any) error {
				y.Hello()
				return yaml.Unmarshal(b, out)
			}
		`),
	)
}

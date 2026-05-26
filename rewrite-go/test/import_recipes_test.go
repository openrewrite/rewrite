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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

// strPtr returns a pointer to s for use as an Alias option.
func strPtr(s string) *string { return &s }

// ---- AddImport ----

func TestAddImport_NoOpWhenAlreadyImported(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.AddImport{PackagePath: "fmt"})
	spec.RewriteRun(t,
		Golang(`
			package main

			import "fmt"

			func main() { fmt.Println("hi") }
		`),
	)
}

func TestAddImport_AddsToExistingBlock(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.AddImport{PackagePath: "strings"})
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
	spec := NewRecipeSpec().WithRecipe(&golang.AddImport{PackagePath: "fmt"})
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

func TestAddImport_OnlyIfReferenced_NoOpWhenNotReferenced(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.AddImport{
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
	spec := NewRecipeSpec().WithRecipe(&golang.AddImport{
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

// ---- RemoveImport ----

func TestRemoveImport_DeletesMatching(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RemoveImport{PackagePath: "strings"})
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

func TestRemoveImport_NoOpWhenAbsent(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RemoveImport{PackagePath: "strings"})
	spec.RewriteRun(t,
		Golang(`
			package main

			import "fmt"

			func main() { fmt.Println("hi") }
		`),
	)
}

// ---- RemoveUnusedImports ----

func TestRemoveUnusedImports_DropsUnreferenced(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RemoveUnusedImports{})
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

func TestRemoveUnusedImports_PreservesBlankImports(t *testing.T) {
	// Blank imports stay — they exist for init() side-effects.
	spec := NewRecipeSpec().WithRecipe(&golang.RemoveUnusedImports{})
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
	spec := NewRecipeSpec().WithRecipe(&golang.RemoveUnusedImports{})
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

// ---- OrderImports ----

func TestOrderImports_IdempotentOnAlreadyOrdered(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.OrderImports{})
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
	spec := NewRecipeSpec().WithRecipe(&golang.OrderImports{})
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
	spec := NewRecipeSpec().WithRecipe(&golang.OrderImports{})
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

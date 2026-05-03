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

func TestRenamePackage_RewritesImportPath(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RenamePackage{
		OldPackagePath: "github.com/old/foo",
		NewPackagePath: "github.com/new/foo",
	})
	before := `
		package main

		import "github.com/old/foo"

		func main() { _ = foo.Hello() }
	`
	after := `
		package main

		import "github.com/new/foo"

		func main() { _ = foo.Hello() }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRenamePackage_RewritesSubPackageImports(t *testing.T) {
	// `import "old/foo/sub"` rewrites to `import "new/foo/sub"` when
	// renaming `old/foo` to `new/foo`.
	spec := NewRecipeSpec().WithRecipe(&golang.RenamePackage{
		OldPackagePath: "github.com/old/foo",
		NewPackagePath: "github.com/new/foo",
	})
	before := `
		package main

		import (
			"github.com/old/foo"
			"github.com/old/foo/sub"
		)

		func main() {
			_ = foo.A()
			_ = sub.B()
		}
	`
	after := `
		package main

		import (
			"github.com/new/foo"
			"github.com/new/foo/sub"
		)

		func main() {
			_ = foo.A()
			_ = sub.B()
		}
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestRenamePackage_LeavesUnrelatedImports(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RenamePackage{
		OldPackagePath: "github.com/old/foo",
		NewPackagePath: "github.com/new/foo",
	})
	spec.RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"
				"github.com/other/bar"
			)

			func main() { fmt.Println(bar.X) }
		`),
	)
}

func TestRenamePackage_IdempotentOnRenamedPath(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RenamePackage{
		OldPackagePath: "github.com/old/foo",
		NewPackagePath: "github.com/new/foo",
	})
	spec.RewriteRun(t,
		Golang(`
			package main

			import "github.com/new/foo"

			func main() { _ = foo.Hello() }
		`),
	)
}

// Files that own the renamed package — i.e. live at the directory
// matching OldPackagePath under the module — must have their `package`
// declaration rewritten to the new last-segment name. This is the
// "cross-file scope" half of the recipe, distinct from import
// rewriting in caller files.
func TestRenamePackage_RewritesOwnedPackageDecl(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RenamePackage{
		OldPackagePath: "example.com/myapp/internal/old",
		NewPackagePath: "example.com/myapp/internal/new",
	})

	owned := Golang(`
		package old

		func Util() {}
	`, `
		package new

		func Util() {}
	`).WithPath("internal/old/util.go")

	spec.RewriteRun(t,
		GoProject("myapp",
			GoMod(`
				module example.com/myapp

				go 1.22
			`),
			owned,
		),
	)
}

// A file with a coincidentally-matching package name but living
// elsewhere in the module must NOT have its package declaration
// rewritten. fileBelongsTo gates this on the file's source-relative
// directory matching the candidate's module-relative path.
func TestRenamePackage_LeavesCoincidentPackageDecl(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RenamePackage{
		OldPackagePath: "example.com/myapp/internal/old",
		NewPackagePath: "example.com/myapp/internal/new",
	})

	// Same package name `old`, but at a different directory — must be
	// left alone.
	unrelated := Golang(`
		package old

		func Other() {}
	`).WithPath("vendor/something/old/x.go")

	spec.RewriteRun(t,
		GoProject("myapp",
			GoMod(`
				module example.com/myapp

				go 1.22
			`),
			unrelated,
		),
	)
}

// End-to-end: a single recipe run handles both the owning file's
// package declaration AND the consumer's import path in one project.
func TestRenamePackage_RewritesProjectWide(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&golang.RenamePackage{
		OldPackagePath: "example.com/myapp/internal/old",
		NewPackagePath: "example.com/myapp/internal/new",
	})

	owned := Golang(`
		package old

		func Util() {}
	`, `
		package new

		func Util() {}
	`).WithPath("internal/old/util.go")

	consumer := Golang(`
		package main

		import "example.com/myapp/internal/old"

		func main() { old.Util() }
	`, `
		package main

		import "example.com/myapp/internal/new"

		func main() { old.Util() }
	`).WithPath("cmd/app/main.go")

	spec.RewriteRun(t,
		GoProject("myapp",
			GoMod(`
				module example.com/myapp

				go 1.22
			`),
			owned,
			consumer,
		),
	)
}

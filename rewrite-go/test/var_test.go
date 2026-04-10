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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
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

func TestParseMultiVarWithCompositeLiterals(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				var a, b = []int{}, []int{}
			}
		`))
}

func TestParseVarPointerType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		SourceSpec{
			Before: "package main\n\nfunc f() {\n\tvar x *int\n\t_ = x\n}\n",
			Path:   "test.go",
			AfterRecipe: func(t *testing.T, cu *tree.CompilationUnit) {
				fn := cu.Statements[0].Element.(*tree.MethodDeclaration)
				varDecl := fn.Body.Statements[0].Element.(*tree.VariableDeclarations)

				if varDecl.TypeExpr == nil {
					t.Fatal("expected TypeExpr to be set for 'var x *int'")
				}
				pt, ok := varDecl.TypeExpr.(*tree.PointerType)
				if !ok {
					t.Fatalf("expected TypeExpr to be *tree.PointerType, got %T", varDecl.TypeExpr)
				}
				ident, ok := pt.Elem.(*tree.Identifier)
				if !ok {
					t.Fatalf("expected PointerType.Elem to be *tree.Identifier, got %T", pt.Elem)
				}
				if ident.Name != "int" {
					t.Errorf("expected PointerType.Elem name to be 'int', got %q", ident.Name)
				}
			},
		})
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

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

	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

// Go's tokenizer inserts a semicolon at end of line, so a `;` written
// there is redundant — but it is in the source and must round-trip.

func TestParseSemicolonInlineBetweenStatements(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				g(); g()
			}
		`))
}

func TestParseSemicolonAtEndOfStatementLine(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				g();
				g();
			}
		`))
}

func TestParseSemicolonAfterTopLevelDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var x = 1;

			var y = 2;
		`))
}

func TestParseSemicolonInCaseBody(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(v int) {
				switch v {
				case 1:
					g();
					g();
				}
			}
		`))
}

func TestParseSemicolonBeforeClosingBrace(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() { g(); }
		`))
}

func TestParseSemicolonAfterInterfaceMethod(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type T interface {
				M() string;
			}
		`))
}

func TestParseSemicolonAfterEmbeddedInterface(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type A interface{}

			type T interface {
				A;
				M()
			}
		`))
}

func TestParseSemicolonAfterStructField(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type T struct {
				a int;
				b int;
			}
		`))
}

func TestParseSemicolonInGroupedTypeDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type (
				A int;
				B int;
			)
		`))
}

func TestParseSemicolonInGroupedVarDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			var (
				a int;
			)
		`))
}

// The init clause of an `if`, `switch` or type switch may be empty while
// its `;` is still written.

func TestParseSemicolonEmptySwitchInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				switch ; {
				case true:
					return
				}
			}
		`))
}

func TestParseSemicolonEmptyIfInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				if ; true {
				}
			}
		`))
}

func TestParseSemicolonEmptyTypeSwitchInit(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f(x any) {
				switch ; x.(type) {
				}
			}
		`))
}

func TestParseSemicolonAfterImport(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "go/ast";

			func f(list []ast.Expr) {
			}
		`))
}

func TestParseSemicolonAfterGroupedImport(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import (
				"go/ast";
				"fmt";
			)

			func f(list []ast.Expr) {
				fmt.Println(list)
			}
		`))
}

func TestParseImplicitSeparatorAfterIfInit(t *testing.T) {
	src := "package main\n\nfunc g() {}\n\nfunc f() {\n\tx := true\n\tif g()\n\t(x) {\n\t}\n}\n"
	assertRoundtrip(t, src)
}

func TestParseEmptyStatement(t *testing.T) {
	src := "package main\n\nfunc f() {\n\t;\n}\n"
	assertRoundtrip(t, src)
}

func TestParseLabelledEmptyStatement(t *testing.T) {
	src := "package main\n\nfunc f() {\nL:\n\t;\n\tgoto L\n}\n"
	assertRoundtrip(t, src)
}

func TestParseConsecutiveEmptyStatements(t *testing.T) {
	src := "package main\n\nfunc f() {\n\tx := 0\n\t; ;\n\t_ = x\n}\n"
	assertRoundtrip(t, src)
}

func TestParseSemicolonInSelectClauseBody(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func Send(c chan int) int {
				select {
				default:
					return 1;
				}
				return 2;
			}
		`))
}

// A `;` is claimed only when it is the very next token; one further off
// terminates the enclosing statement, or sits inside the tag expression.

func TestParseSemicolonAfterSwitchStatement(t *testing.T) {
	src := "package main\n\nfunc g() {}\n\nfunc f() { switch { case true: g() }; g() }\n"
	assertRoundtrip(t, src)
}

func TestParseSemicolonAfterSelectStatement(t *testing.T) {
	src := "package main\n\nfunc g() {}\n\nfunc f(c chan int) { select { case <-c: g() }; g() }\n"
	assertRoundtrip(t, src)
}

func TestParseSemicolonInsideSwitchTag(t *testing.T) {
	src := "package main\n\nfunc g(f func()) bool { return true }\n\nfunc f() {\n\tswitch g(func() { for i := 0; i < 3; i++ {} }) {\n\tcase true:\n\t}\n}\n"
	assertRoundtrip(t, src)
}

func TestParseEmptyStatementOnItsOwnLine(t *testing.T) {
	src := "package main\n\nfunc g() {}\n\nfunc f() {\n\tg()\n\t;\n\tg()\n}\n"
	assertRoundtrip(t, src)
}

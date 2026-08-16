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

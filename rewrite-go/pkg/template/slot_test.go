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
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

var slotX = Expr("x")

func asStatement(s *beforeSpec) {
	k := ScaffoldStatement
	s.kind = &k
}

func slotRecipe(before, after string, afterOpts ...BeforeOption) *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.Slot"),
		WithDisplayName("Slot"),
		WithBefore(before),
		WithAfter(after, afterOpts...),
		WithCaptures(slotX),
	))
}

func TestTemplateLeavesAStatementOutOfAnExpressionSlot(t *testing.T) {
	slotRecipe(`bail(`+slotX.String()+`)`, `return `+slotX.String()).RewriteRun(t,
		test.Golang(`
			package main

			var a = bail(1)
		`),
	)
}

func TestTemplateLeavesALoopOutOfAShortVarDeclValue(t *testing.T) {
	slotRecipe(`compute()`, "for {\n}", asStatement).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				x := compute()
				_ = x
			}
		`),
	)
}

func TestTemplateStillReplacesAStatementInAStatementSlot(t *testing.T) {
	slotRecipe(`bail(`+slotX.String()+`)`, `return `+slotX.String()).RewriteRun(t,
		test.Golang(`
			package main

			func f() int {
				bail(1)
			}
		`, `
			package main

			func f() int {
				return 1
			}
		`),
	)
}

func TestTemplateLeavesASelectorOutOfANameSlot(t *testing.T) {
	for _, src := range []string{
		`
			package main

			type T struct{}

			func (T) oldName() {}

			func f(x T) {
				x.oldName()
			}
		`,
		`
			package main

			func f(oldName int) {}
		`,
		`
			package main

			type oldName struct{}
		`,
		`
			package main

			func f() {
			oldName:
				goto oldName
			}
		`,
	} {
		slotRecipe(`oldName`, `pkg.NewName`).RewriteRun(t, test.Golang(src))
	}
}

func TestTemplateLeavesASelectorOutOfAShortVarDeclName(t *testing.T) {
	slotRecipe(`oldName`, `pkg.NewName`).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				oldName := 1
				oldName, other := 2, 3
				_ = other
			}
		`),
	)
}

func TestTemplateLeavesACompoundStatementOutOfAnInitClause(t *testing.T) {
	for _, src := range []string{
		`
			package main

			func f() {
				if setup(); true {
				}
			}
		`,
		`
			package main

			func f() {
				for setup(); ; {
				}
			}
		`,
	} {
		slotRecipe(`setup()`, "if ok {\n}", asStatement).RewriteRun(t, test.Golang(src))
	}
}

func TestTemplateLeavesANonCallOutOfDeferAndGo(t *testing.T) {
	slotRecipe(`old(`+slotX.String()+`)`, slotX.String()+` + 1`).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				defer old(1)
				go old(2)
			}
		`),
	)
}

func TestTemplateStillReplacesACallUnderDefer(t *testing.T) {
	slotRecipe(`old(`+slotX.String()+`)`, `neu(`+slotX.String()+`)`).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				defer old(1)
			}
		`, `
			package main

			func f() {
				defer neu(1)
			}
		`),
	)
}

func TestTemplateLeavesAStatementOutOfAnIfWithInit(t *testing.T) {
	body := Stmt("body")
	test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.UnwrapIfTrue"),
		WithDisplayName("Unwrap if true"),
		WithBefore("if true {\n"+body.String()+"\n}"),
		WithAfter("work()", asStatement),
		WithCaptures(body),
	)).RewriteRun(t,
		test.Golang(`
			package main

			func f() {
				if x := 1; true {
					_ = x
				}
			}
		`),
	)
}

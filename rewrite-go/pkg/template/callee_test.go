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

var calleeFn = Expr("fn")

func invokingCapturedCallee() *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.InlineInvoke"),
		WithDisplayName("Inline invoke"),
		WithBefore(`invoke(`+calleeFn.String()+`)`),
		WithAfter(calleeFn.String()+`()`),
		WithCaptures(calleeFn),
	))
}

func TestTemplateCalleeBoundToIdentifier(t *testing.T) {
	invokingCapturedCallee().RewriteRun(t,
		test.Golang(`
			package main

			func run() {}

			var a = invoke(run)
		`, `
			package main

			func run() {}

			var a = run()
		`),
	)
}

func TestTemplateCalleeBoundToSelector(t *testing.T) {
	invokingCapturedCallee().RewriteRun(t,
		test.Golang(`
			package main

			type T struct{}

			func (T) Method() {}

			var obj T

			var a = invoke(obj.Method)
		`, `
			package main

			type T struct{}

			func (T) Method() {}

			var obj T

			var a = obj.Method()
		`),
	)
}

func TestTemplateCalleeBoundToFuncLiteral(t *testing.T) {
	invokingCapturedCallee().RewriteRun(t,
		test.Golang(`
			package main

			var a = invoke(func() int { return 1 })
		`, `
			package main

			var a = func() int { return 1 }()
		`),
	)
}

func TestTemplateCalleeBoundToOperatorExpression(t *testing.T) {
	invokingCapturedCallee().RewriteRun(t,
		test.Golang(`
			package main

			func f(p *func()) {
				invoke(*p)
			}
		`, `
			package main

			func f(p *func()) {
				(*p)()
			}
		`),
	)
}

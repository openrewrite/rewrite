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

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

var slotArg = Expr("arg")

func replacingApplyTheme(after string) *test.RecipeSpec {
	return test.NewRecipeSpec().WithRecipe(NewRecipe(
		RecipeName("test.ReplaceApplyTheme"),
		WithDisplayName("Replace applyTheme"),
		WithBefore(`applyTheme(`+slotArg.String()+`)`),
		WithAfter(after),
		WithCaptures(slotArg),
	))
}

func bodyStatement(t *testing.T, cu *golang.CompilationUnit, i int) java.Statement {
	t.Helper()
	md, ok := cu.Statements[0].Element.(*java.MethodDeclaration)
	require.True(t, ok, "first statement is %T", cu.Statements[0].Element)
	return md.Body.Statements[i].Element
}

func expressionStatementHolding[T java.Expression](t *testing.T, s java.Statement) {
	t.Helper()
	es, ok := s.(*golang.ExpressionStatement)
	require.True(t, ok, "statement is %T", s)
	_, ok = es.Expression.(T)
	assert.True(t, ok, "expression is %T", es.Expression)
}

func TestTemplateWrapsTypeAssertionReplacingCallStatement(t *testing.T) {
	replacingApplyTheme(slotArg.String()+`.(string)`).RewriteRun(t,
		test.Golang(`
			package main

			func f(svc any) {
				applyTheme(svc)
			}
		`, `
			package main

			func f(svc any) {
				svc.(string)
			}
		`).WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
			expressionStatementHolding[*golang.TypeAssertion](t, bodyStatement(t, cu, 0))
		}),
	)
}

func TestTemplateWrapsBinaryReplacingCallStatement(t *testing.T) {
	replacingApplyTheme(slotArg.String()+` + 1`).RewriteRun(t,
		test.Golang(`
			package main

			func f(svc int) {
				x := 0
				applyTheme(svc)
			}
		`, `
			package main

			func f(svc int) {
				x := 0
				svc + 1
			}
		`).WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
			expressionStatementHolding[*java.Binary](t, bodyStatement(t, cu, 1))
		}),
	)
}

func TestTemplateWrapsFunctionLiteralReplacingCallStatement(t *testing.T) {
	replacingApplyTheme(`func() { _ = `+slotArg.String()+` }`).RewriteRun(t,
		test.Golang(`
			package main

			func f(svc any) {
				applyTheme(svc)
			}
		`, `
			package main

			func f(svc any) {
				func() { _ = svc }
			}
		`).WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
			expressionStatementHolding[*golang.StatementExpression](t, bodyStatement(t, cu, 0))
		}),
	)
}

func TestTemplateKeepsSelectorReplacingCallStatement(t *testing.T) {
	replacingApplyTheme(slotArg.String()+`.Name`).RewriteRun(t,
		test.Golang(`
			package main

			func f(svc any) {
				applyTheme(svc)
			}
		`, `
			package main

			func f(svc any) {
				svc.Name
			}
		`).WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
			s := bodyStatement(t, cu, 0)
			_, ok := s.(*java.FieldAccess)
			assert.True(t, ok, "statement is %T", s)
		}),
	)
}

func TestTemplateKeepsCallStatementReplacedByCall(t *testing.T) {
	replacingApplyTheme(`render(`+slotArg.String()+`)`).RewriteRun(t,
		test.Golang(`
			package main

			func f(svc any) {
				applyTheme(svc)
			}
		`, `
			package main

			func f(svc any) {
				render(svc)
			}
		`).WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
			s := bodyStatement(t, cu, 0)
			_, ok := s.(*java.MethodInvocation)
			assert.True(t, ok, "statement is %T", s)
		}),
	)
}

func TestTemplateLeavesExpressionSlotUnwrapped(t *testing.T) {
	replacingApplyTheme(slotArg.String()+`.(string)`).RewriteRun(t,
		test.Golang(`
			package main

			func f(svc any) {
				_ = applyTheme(svc)
			}
		`, `
			package main

			func f(svc any) {
				_ = svc.(string)
			}
		`).WithAfterRecipe(func(t *testing.T, cu *golang.CompilationUnit) {
			s := bodyStatement(t, cu, 0)
			assignment, ok := s.(*java.Assignment)
			require.True(t, ok, "statement is %T", s)
			_, ok = assignment.Value.Element.(*golang.TypeAssertion)
			assert.True(t, ok, "value is %T", assignment.Value.Element)
		}),
	)
}

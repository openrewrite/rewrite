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

package template_test

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

const wrapDecls = `type Wrapped struct{ V int }

func Wrap(v Wrapped) Wrapped { return v }`

func TestContextAttributesAPatternAgainstLocalDeclarations(t *testing.T) {
	pat := template.Expression(`Wrap(w)`).
		Context(wrapDecls, "var w Wrapped").Build()
	call := patternCall(t, pat)
	require.True(t, matcher.IsResolved(call))
	require.Equal(t, "Wrap", call.MethodType.Name)
}

func TestWithoutContextTheSameCallIsUnresolved(t *testing.T) {
	require.False(t, matcher.IsResolved(patternCall(t, template.Expression(`Wrap(w)`).Build())))
}

func TestContextComposesWithImports(t *testing.T) {
	pat := template.Expression(`Delay(time.Second)`).
		Imports("time").
		Context("func Delay(d time.Duration) {}").Build()
	call := patternCall(t, pat)
	require.True(t, matcher.IsResolved(call))
	require.Equal(t, []string{"time.Duration"}, patternParamFQNs(call))
}

func TestContextLeavesAStatementPatternOnTheRightNode(t *testing.T) {
	body := template.Stmt("body")
	pat := template.StatementPattern(fmt.Sprintf("if Ready() {\n%s\n}", body)).
		Captures(body).
		Context("func Ready() bool { return true }").Build()
	call := patternCall(t, pat)
	require.True(t, matcher.IsResolved(call))
	require.Equal(t, "Ready", call.MethodType.Name)
}

// A comment declares nothing, an unindented body line is not a declaration,
// and two declarations can share a line.
func TestContextTakesAnyShapeOfDeclaration(t *testing.T) {
	for _, decls := range []string{
		"// a helper\ntype Wrapped struct{ V int }\n\nfunc Wrap(v Wrapped) Wrapped { return v }",
		"type Wrapped struct{ V int }\nfunc Wrap(v Wrapped) Wrapped {\nreturn v\n}",
		"type Wrapped struct{ V int }; type Other int\n\nfunc Wrap(v Wrapped) Wrapped { return v }",
	} {
		pat := template.Expression(`Wrap(w)`).Context(decls, "var w Wrapped").Build()
		call := patternCall(t, pat)
		require.True(t, matcher.IsResolved(call), "decls %q", decls)
		require.Equal(t, "Wrap", call.MethodType.Name)
	}
}

func TestContextLeavesATopLevelPatternOnTheRightNode(t *testing.T) {
	pat := template.TopLevel("func Use() { Wrap(Wrapped{}) }").Context(wrapDecls).Build()
	call := patternCall(t, pat)
	require.True(t, matcher.IsResolved(call))
	require.Equal(t, "Wrap", call.MethodType.Name)
}

// An unsubstituted placeholder would print as source.
func TestApplyRefusesAnUnboundCapture(t *testing.T) {
	missing := template.Expr("missing")
	tmpl := template.ExpressionTemplate(fmt.Sprintf("f(%s)", missing)).Captures(missing).Build()

	require.Nil(t, tmpl.Apply(nil, template.NewMatchResult()))
	require.Nil(t, tmpl.Apply(nil, nil))

	bound := template.NewMatchResult().Bind(missing, template.Expression("42").Build().Tree(t))
	require.NotNil(t, tmpl.Apply(nil, bound))
}

// An edit to one application must leave the other and the cache alone.
func TestApplyYieldsIndependentTrees(t *testing.T) {
	tmpl := template.TopLevelTemplate("func F[T any](a *T, m map[string]int, c chan int) error {\n" +
		"\ttype S struct{ X int `json:\"x\"` }\n" +
		"\tconst (\n\t\tA = 1\n\t)\n" +
		"\tgo func() { _ = a }()\n" +
		"\tselect {\n\tcase <-c:\n\t}\n" +
		"\treturn nil\n}").Build()
	first, second := tmpl.Apply(nil, nil), tmpl.Apply(nil, nil)
	require.NotNil(t, first)
	require.NotNil(t, second)

	require.Greater(t, len(template.NodeIDs(first)), 40, "the template should exercise many node kinds")
	ids := map[string]bool{}
	for _, id := range template.NodeIDs(first) {
		ids[id] = true
	}
	for _, id := range template.NodeIDs(second) {
		require.False(t, ids[id], "the two applications share node %s", id)
	}
}

func TestApplyIgnoresACaptureItDoesNotUse(t *testing.T) {
	used, unused := template.Expr("used"), template.Expr("unused")
	tmpl := template.ExpressionTemplate(fmt.Sprintf("f(%s)", used)).Captures(used, unused).Build()

	bound := template.NewMatchResult().Bind(used, template.Expression("42").Build().Tree(t))
	require.NotNil(t, tmpl.Apply(nil, bound))
	require.Nil(t, tmpl.Apply(nil, template.NewMatchResult()))
}

// The scaffold always declares something of its own.
func TestTopLevelPatternNeedsADeclaration(t *testing.T) {
	_, err := template.TopLevel("").Captures(template.Expr("x")).Build().TreeOrError()
	require.Error(t, err)
}

func TestTopLevelPatternTakesTheFirstDeclaration(t *testing.T) {
	tree, err := template.TopLevel("func F() {}\n\nfunc G() {}").Build().TreeOrError()
	require.NoError(t, err)
	md, ok := tree.(*java.MethodDeclaration)
	require.True(t, ok, "got %T", tree)
	require.Equal(t, "F", md.Name.Name)
}

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
)

// A declaration the pattern names but no package exports is what Context is
// for; Imports and ExportData between them cover everything importable.
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

// A statement pattern puts its context at the top level and its captures in
// the function body, so the two offsets are counted apart.
func TestContextLeavesAStatementPatternOnTheRightNode(t *testing.T) {
	body := template.Stmt("body")
	pat := template.StatementPattern(fmt.Sprintf("if Ready() {\n%s\n}", body)).
		Captures(body).
		Context("func Ready() bool { return true }").Build()
	call := patternCall(t, pat)
	require.True(t, matcher.IsResolved(call))
	require.Equal(t, "Ready", call.MethodType.Name)
}

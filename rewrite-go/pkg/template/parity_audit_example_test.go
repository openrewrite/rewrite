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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
)

// The example under "What recipe authors should know" in PARITY-AUDIT.md,
// run so that it stays true. The document previously named a builder that
// does not exist and a placeholder syntax that does not parse.
func TestParityAuditExampleMatches(t *testing.T) {
	x, y := template.Expr("x"), template.Expr("y")
	before := template.Expression(fmt.Sprintf(`errors.Is(%s, %s)`, x, y)).Captures(x, y).Build()
	after := template.ExpressionTemplate(fmt.Sprintf(`xerrors.Is(%s, %s)`, x, y)).Imports("xerrors").Build()
	require.NotNil(t, template.Rewrite(before, after))

	call := firstCall(t, `package a

import "errors"

func f(a, b error) { _ = errors.Is(a, b) }
`)
	match := before.Match(call, nil)
	require.NotNil(t, match)
	require.NotNil(t, match.Get("x"))
	require.NotNil(t, match.Get("y"))
}

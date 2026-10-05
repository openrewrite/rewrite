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
package parser_test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// An import path is held the way J.Import holds one, as a field access whose
// name is the path as written.
func TestImportPathIsAFieldAccess(t *testing.T) {
	src := "package main\n\nimport (\n\t\"fmt\"\n\tr `strings`\n\t_ \"\\x65mbed\"\n)\n\nvar _ = fmt.Sprint(r.ToUpper(\"a\"))\n"
	cu, err := parser.NewGoParser().Parse("imports.go", src)
	require.NoError(t, err)
	require.Equal(t, src, printer.Print(cu))

	names := map[string]string{}
	for _, imp := range cu.Imports.Elements {
		qualid := imp.Element.Qualid
		require.IsType(t, &java.Empty{}, qualid.Target)
		names[qualid.Name.Element.Name] = imp.Element.Path()

		// the imported package, as on the identifiers that qualify with it
		pkg, ok := qualid.Type.(*java.JavaTypeClass)
		require.Truef(t, ok, "%s is typed %T", qualid.Name.Element.Name, qualid.Type)
		assert.Equal(t, imp.Element.Path(), pkg.FullyQualifiedName)
		assert.Same(t, pkg, qualid.Name.Element.Type)
	}
	assert.Equal(t, map[string]string{"fmt": "fmt", "`strings`": "strings", `\x65mbed`: "embed"}, names)
}

// An empty group has no import to hold the space before its `)`, so one
// without a path carries it.
func TestEmptyImportGroupRoundTrips(t *testing.T) {
	src := "package main\n\nimport \"fmt\"\n\nimport ( /* none */ )\n\nvar _ = fmt.Sprint()\n"
	cu, err := parser.NewGoParser().Parse("imports.go", src)
	require.NoError(t, err)

	require.Equal(t, src, printer.Print(cu))
	assert.Equal(t, "", cu.Imports.Elements[1].Element.Path())
}

func TestCaseHoldsTheSpaceBeforeItsColon(t *testing.T) {
	sw, ok := firstStatementInBody(t, "package main\n\nfunc f(x int) {\n\tswitch x {\n\tcase 1 /* a */ :\n\tdefault /* b */ :\n\t}\n}\n").(*java.Switch)
	require.True(t, ok)

	for i, text := range []string{"a", "b"} {
		c := sw.Body.Statements[i].Element.(*java.Case)
		require.Len(t, c.Body.Before.Comments(), 1)
		assert.Equal(t, " "+text+" ", c.Body.Before.Comments()[0].Text)
		assert.Empty(t, c.Expressions.Elements[0].After.Comments())
	}
}

func TestLoopBodyIsPaddedLikeAnIfBody(t *testing.T) {
	loop, ok := firstStatementInBody(t, "package main\n\nfunc f() {\n\tfor {\n\t}\n}\n").(*java.ForLoop)
	require.True(t, ok)
	require.IsType(t, &java.Block{}, loop.Body.Element)

	each, ok := firstStatementInBody(t, "package main\n\nfunc f(xs []int) {\n\tfor range xs {\n\t}\n}\n").(*java.ForEachLoop)
	require.True(t, ok)
	require.IsType(t, &java.Block{}, each.Body.Element)
}

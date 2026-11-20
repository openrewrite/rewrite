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
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// A recipe calling Apply places the result itself, and may name the position
// it is at rather than the node the result replaces, as a visitor's own cursor
// does in the body it is visiting.
func TestApplyLeavesPlacementToTheCallerWhoseCursorNamesAnotherNode(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\n\nfunc f(region string) {\n\t_ = region\n}\n")
	require.NoError(t, err)
	md, ok := cu.Statements[0].Element.(*java.MethodDeclaration)
	require.True(t, ok)
	body := visitor.NewCursor(visitor.NewCursor(visitor.NewCursor(nil, cu), md), md.Body)
	region := Expr("region")
	tmpl := ExpressionTemplate(fmt.Sprintf(`load(%s)`, region)).Captures(region).Build()

	got := tmpl.Apply(body, NewMatchResult().Bind(region, ExpressionTemplate(`region`).Build().Instantiate(nil)))

	require.NotNil(t, got)
	assert.Equal(t, "load(region)", strings.TrimSpace(printer.Print(got)))
}

// Only Rewrite and NewRecipe refuse a qualifier a local shadows; a recipe
// calling Apply may alias the import instead.
func TestApplyLeavesAShadowedQualifierToTheCaller(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\n\nfunc f(strings []string) {\n\t_ = strings\n}\n")
	require.NoError(t, err)
	md, ok := cu.Statements[0].Element.(*java.MethodDeclaration)
	require.True(t, ok)
	stmt := md.Body.Statements[0].Element
	site := visitor.NewCursor(visitor.NewCursor(visitor.NewCursor(visitor.NewCursor(nil, cu), md), md.Body), stmt)
	tmpl := StatementTemplate(`_ = strings.ToUpper("a")`).Imports("strings").Build()

	assert.NotNil(t, tmpl.Apply(site, nil))
	assert.Nil(t, tmpl.applyInFile(site, nil))
}

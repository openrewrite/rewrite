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

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// compositesOf parses src and returns every golang.Composite in source order.
func compositesOf(t *testing.T, src string) (*golang.CompilationUnit, []*golang.Composite) {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("composite.go", src)
	require.NoError(t, err, "parse")

	var found []*golang.Composite
	visitor.Walk(cu, func(n java.Tree) bool {
		if c, ok := n.(*golang.Composite); ok {
			found = append(found, c)
		}
		return true
	})
	return cu, found
}

// The table-driven test idiom, whose inner elements are untyped composites.
const tableDrivenSrc = "package main\n\nfunc f() {\n\ttests := []struct {\n\t\tname string\n\t}{\n\t\t{name: \"a\"},\n\t}\n\t_ = tests\n}\n"

// Leading whitespace belongs on the outermost element, so an untyped composite
// carries it on its own Prefix. Elements.Before covers only what a typed
// composite writes between its type expression and `{`.
func TestUntypedCompositeCarriesLeadingWhitespaceOnPrefix(t *testing.T) {
	// given
	_, composites := compositesOf(t, tableDrivenSrc)
	require.Len(t, composites, 2, "outer slice literal and inner element")

	// when
	inner := composites[1]

	// then
	require.Nil(t, inner.TypeExpr, "inner element is an untyped composite")
	require.Equal(t, "\n\t\t", inner.Prefix.Whitespace, "leading whitespace on Prefix")
	require.True(t, inner.Elements.Before.IsEmpty(), "Elements.Before must not hold leading whitespace")
}

// A typed composite's Prefix sits ahead of its type expression.
func TestTypedCompositeKeepsPrefixAheadOfTypeExpr(t *testing.T) {
	// given
	_, composites := compositesOf(t, tableDrivenSrc)

	// when
	outer := composites[0]

	// then
	require.NotNil(t, outer.TypeExpr, "outer literal is typed")
	require.Equal(t, " ", outer.Prefix.Whitespace, "space after `:=`")
	require.True(t, outer.Elements.Before.IsEmpty(), "nothing between the type and `{`")
}

// The printer emits Prefix ahead of `{`, so the source survives the round trip
// and the tree satisfies the whitespace-attachment objective.
func TestUntypedCompositeRoundTrips(t *testing.T) {
	// given
	cu, _ := compositesOf(t, tableDrivenSrc)

	// then
	require.Equal(t, tableDrivenSrc, printer.Print(cu))
	require.Empty(t, test.WhitespaceAttachmentViolations(cu))
}

// An untyped composite nested in a map literal keeps the same contract.
func TestUntypedCompositeInMapLiteral(t *testing.T) {
	// given
	src := "package main\n\ntype p struct{ x int }\n\nfunc f() {\n\tm := map[string]p{\n\t\t\"a\": {x: 1},\n\t}\n\t_ = m\n}\n"

	// when
	cu, composites := compositesOf(t, src)

	// then
	require.Len(t, composites, 2)
	inner := composites[1]
	require.Nil(t, inner.TypeExpr)
	require.Equal(t, " ", inner.Prefix.Whitespace, "space after the key's `:`")
	require.True(t, inner.Elements.Before.IsEmpty())
	require.Equal(t, src, printer.Print(cu))
}

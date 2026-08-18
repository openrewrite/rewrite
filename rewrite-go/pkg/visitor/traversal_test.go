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

package visitor_test

import (
	"reflect"
	"strings"
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// reachedKinds parses src and returns how many nodes of each type the visitor
// reaches, keyed by "package.TypeName" (e.g. "java.ForEachControl").
func reachedKinds(t *testing.T, src string) map[string]int {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("traversal.go", src)
	require.NoError(t, err, "parse")

	kinds := map[string]int{}
	visitor.Walk(cu, func(n java.Tree) bool {
		kinds[strings.TrimPrefix(reflect.TypeOf(n).String(), "*")]++
		return true
	})
	return kinds
}

type spaceCollector struct {
	visitor.GoVisitor
	spaces []string
}

func (c *spaceCollector) VisitSpace(space java.Space, p any) java.Space {
	c.spaces = append(c.spaces, space.Whitespace)
	return c.GoVisitor.VisitSpace(space, p)
}

// visitedSpaces parses src and returns every whitespace string reachable
// through VisitSpace. Callers widen the gap under test to a distinctive width
// so the assertion names exactly one slot.
func visitedSpaces(t *testing.T, src string) []string {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("spaces.go", src)
	require.NoError(t, err, "parse")

	c := visitor.Init(&spaceCollector{})
	c.Visit(cu, nil)
	return c.spaces
}

// The space ahead of an operator is printed, so a visitor has to be able to
// read and rewrite it — otherwise a formatter cannot set operator spacing.
func TestOperatorSpaceIsVisited(t *testing.T) {
	tests := []struct {
		name string
		src  string
	}{
		{"binary", "package main\n\nfunc f(a, b int) int {\n\treturn a   + b\n}\n"},
		{"unary", "package main\n\nfunc f() {\n\tx := 0\n\tx   ++\n}\n"},
		{"assignment operation", "package main\n\nfunc f() {\n\tx := 0\n\tx   += 1\n}\n"},
		{"variadic parameter", "package main\n\nfunc f(a   ...int) {\n\t_ = a\n}\n"},
	}
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			// when
			spaces := visitedSpaces(t, tc.src)

			// then
			require.Contains(t, spaces, "   ", "the widened gap must reach VisitSpace")
		})
	}
}

// Everything in a `for … range` header — the loop targets and the iterated
// expression — has to be reachable, or recipes searching a range clause find
// nothing and rewrites of it silently no-op.
func TestForEachLoopHeaderIsTraversed(t *testing.T) {
	// given
	src := "package main\n\nfunc f() {\n\tfor _, s := range []string{\"a\"} {\n\t\t_ = s\n\t}\n}\n"

	// when
	kinds := reachedKinds(t, src)

	// then
	require.Equal(t, 1, kinds["java.ForEachLoop"], "the loop itself")
	require.Equal(t, 1, kinds["java.ForEachControl"], "loop header")
	require.Equal(t, 1, kinds["golang.MultiAssignment"], "loop targets")
	require.Equal(t, 1, kinds["golang.Composite"], "iterated composite literal")
	require.Equal(t, 1, kinds["java.ArrayType"], "iterated composite literal's type")
	require.Equal(t, 1, kinds["java.Literal"], `the "a" element`)
}

// A recipe matching method invocations depends on the iterated expression.
func TestForEachLoopIteratedCallIsTraversed(t *testing.T) {
	// given
	src := "package main\n\nfunc items() []int { return nil }\n\nfunc f() {\n\tfor i := range items() {\n\t\t_ = i\n\t}\n}\n"

	// when
	kinds := reachedKinds(t, src)

	// then
	require.Equal(t, 1, kinds["java.MethodInvocation"], "iterated call")
}

func TestForLoopHeaderIsTraversed(t *testing.T) {
	// given
	src := "package main\n\nfunc f() {\n\tfor i := 0; i < 3; i++ {\n\t\t_ = i\n\t}\n}\n"

	// when
	kinds := reachedKinds(t, src)

	// then
	require.Equal(t, 1, kinds["java.ForControl"], "loop header")
	require.Equal(t, 1, kinds["java.Binary"], "condition")
	require.Equal(t, 1, kinds["java.Unary"], "update")
}

// A switch's tag expression is a header the same way a loop control is.
func TestSwitchTagIsTraversed(t *testing.T) {
	// given
	src := "package main\n\nfunc f(x int) {\n\tswitch x + 1 {\n\tcase 2:\n\t}\n}\n"

	// when
	kinds := reachedKinds(t, src)

	// then
	require.Equal(t, 1, kinds["java.Switch"], "the switch itself")
	require.Equal(t, 1, kinds["java.Binary"], "tag expression")
}

// A type switch is a java.Switch whose tag holds the guard, so the guard rides
// the same slot as an ordinary switch tag.
func TestTypeSwitchGuardIsTraversed(t *testing.T) {
	// given
	bare := "package main\n\nfunc f(v any) {\n\tswitch v.(type) {\n\tcase int:\n\t}\n}\n"
	assigning := "package main\n\nfunc f(v any) {\n\tswitch x := v.(type) {\n\tcase int:\n\t\t_ = x\n\t}\n}\n"

	// when
	bareKinds := reachedKinds(t, bare)
	assigningKinds := reachedKinds(t, assigning)

	// then
	require.Equal(t, 1, bareKinds["golang.TypeAssertion"], "bare guard")
	// The assigning form spells the guard as an assignment in the tag slot; the
	// second java.Assignment is the case body's `_ = x`.
	require.Equal(t, 1, assigningKinds["golang.TypeAssertion"], "assigning guard's assertion")
	require.Equal(t, 2, assigningKinds["java.Assignment"], "assigning guard")
}

func TestSelectClausesAreTraversed(t *testing.T) {
	// given
	src := "package main\n\nfunc f(c chan int) {\n\tselect {\n\tcase <-c:\n\t}\n}\n"

	// when
	kinds := reachedKinds(t, src)

	// then
	require.Equal(t, 1, kinds["golang.CommClause"], "comm clause")
	require.Equal(t, 1, kinds["golang.Unary"], "receive expression")
}

type pruningVisitor struct {
	visitor.GoVisitor
	prune func(java.Tree) bool
}

func (v *pruningVisitor) PreVisit(t java.Tree, p any) java.Tree {
	if v.prune(t) {
		return nil
	}
	return t
}

// Returning nil from PreVisit prunes a subtree.
func TestPrunedLoopControlKeepsOriginal(t *testing.T) {
	tests := []struct {
		name string
		src  string
		is   func(java.Tree) bool
	}{
		{"for", "package main\n\nfunc f() {\n\tfor i := 0; i < 3; i++ {\n\t\t_ = i\n\t}\n}\n",
			func(n java.Tree) bool { _, ok := n.(*java.ForControl); return ok }},
		{"for range", "package main\n\nfunc f(items []int) {\n\tfor _, s := range items {\n\t\t_ = s\n\t}\n}\n",
			func(n java.Tree) bool { _, ok := n.(*java.ForEachControl); return ok }},
	}
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			// given
			cu, err := parser.NewGoParser().Parse("prune.go", tc.src)
			require.NoError(t, err, "parse")

			// when
			v := visitor.Init(&pruningVisitor{prune: tc.is})
			var got java.Tree
			require.NotPanics(t, func() { got = v.Visit(cu, nil) })

			// then
			require.Equal(t, tc.src, printer.Print(got))
		})
	}
}

// java.Empty backs a keyless `for range` target, so a marker unreachable here
// is a marker unreachable inside a loop header.
func TestEmptyMarkersAreVisited(t *testing.T) {
	// given
	found := uuid.New()
	empty := &java.Empty{
		ID: uuid.New(),
		Markers: java.Markers{
			ID:      uuid.New(),
			Entries: []java.Marker{java.SearchResult{Ident: found}},
		},
	}

	// when
	ids := visitor.CollectSearchResultIDs(empty)

	// then
	require.Equal(t, []uuid.UUID{found}, ids)
}

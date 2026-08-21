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

package parenthesize

import (
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// unwrapper strips every Parentheses node, which is how these tests get hold of
// the ungrouped tree a recipe would produce by synthesizing one from parts.
type unwrapper struct {
	visitor.GoVisitor
}

func (u *unwrapper) Visit(t java.Tree, p any) java.Tree {
	out := u.GoVisitor.Visit(t, p)
	if parens, ok := out.(*java.Parentheses); ok {
		return format.WithPrefix(parens.Tree.Element, parens.Prefix)
	}
	return out
}

// exprLine renders the `var z = …` line of src after applying each visitor.
func exprLine(t *testing.T, src string, visitors ...visitor.AfterVisitor) string {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("expr.go", "package main\n\nvar a, b, c = 1, 2, 3\n\nvar z = "+src+"\n")
	if err != nil {
		t.Fatal(err)
	}
	var tree java.Tree = cu
	for _, v := range visitors {
		tree = v.Visit(tree, nil)
	}
	for _, line := range strings.Split(printer.Print(tree), "\n") {
		if strings.HasPrefix(line, "var z = ") {
			return strings.TrimPrefix(line, "var z = ")
		}
	}
	t.Fatalf("no `var z` line in output")
	return ""
}

func TestVisitorRestoresGroupingParentheses(t *testing.T) {
	for _, expr := range []string{
		"c * (a + b)",
		"(a + b) * c",
		"c - (a + b)",
		"c / (a - b)",
		"-(a + b)",
		"(a || b) && c",
	} {
		t.Run(expr, func(t *testing.T) {
			// Stripping the parentheses is what makes the expression regroup, so
			// a run that leaves them off would silently pass without this check.
			if ungrouped := exprLine(t, expr, visitor.Init(&unwrapper{})); ungrouped == expr {
				t.Fatalf("unwrapping %q changed nothing, so the case proves nothing", expr)
			}
			got := exprLine(t, expr, visitor.Init(&unwrapper{}), NewVisitor())
			if got != expr {
				t.Errorf("re-parenthesizing %q gave %q", expr, got)
			}
		})
	}
}

// Parentheses that do not change grouping are not put back, so a recipe running
// over a synthesized tree does not accumulate them.
func TestVisitorLeavesRedundantParenthesesOff(t *testing.T) {
	for _, tc := range []struct{ source, want string }{
		{"(a + b) + c", "a + b + c"},
		// Spacing is BinarySpacingVisitor's to decide, not this visitor's, so
		// the tighter operator keeps the blanks it was written with.
		{"(a * b) + c", "a * b + c"},
		{"h((a + b))", "h(a + b)"},
	} {
		t.Run(tc.source, func(t *testing.T) {
			got := exprLine(t, tc.source, visitor.Init(&unwrapper{}), NewVisitor())
			if got != tc.want {
				t.Errorf("got %q, want %q", got, tc.want)
			}
		})
	}
}

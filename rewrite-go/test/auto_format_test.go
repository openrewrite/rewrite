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

package test

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// applyVisitor parses src, runs visitor (and any DoAfterVisit-queued
// follow-ups), and returns the printed result.
func applyVisitor(t *testing.T, src string, v recipe.TreeVisitor) string {
	t.Helper()
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	result := v.Visit(cu, nil)
	if result == nil {
		t.Fatal("visit returned nil")
	}

	final := visitor.DrainAfterVisits(v, result.(tree.Tree), nil)
	return printer.Print(final)
}

// ---- Service registry ----

func TestAutoFormatService_RegisteredOnInit(t *testing.T) {
	svc := recipe.Service[*golang.AutoFormatService](nil)
	if svc == nil {
		t.Fatal("expected AutoFormatService to be registered, got nil")
	}
}

// ---- RemoveTrailingWhitespaceVisitor ----

func TestRemoveTrailingWhitespace_StripsTrailingTabsFromLines(t *testing.T) {
	src := "package main   \n\nfunc main() {}\n"
	out := applyVisitor(t, src, format.NewRemoveTrailingWhitespaceVisitor(nil))
	want := "package main\n\nfunc main() {}\n"
	if out != want {
		t.Errorf("got %q, want %q", out, want)
	}
}

// ---- BlankLinesVisitor ----

// Regression: the leading blank line above the first statement of a
// block lives on the *leftmost descendant* of that statement (e.g.
// Variable.Prefix), not on Asg.Prefix. The visitor walks the leftmost
// spine via transformLeftmostPrefix to find it.
func TestBlankLines_StripsLeadingBlankLineAtBlockStart(t *testing.T) {
	src := `package main

func main() {

	a := 1
	_ = a
}
`
	want := `package main

func main() {
	a := 1
	_ = a
}
`
	out := applyVisitor(t, src, format.NewBlankLinesVisitor(nil))
	if out != want {
		t.Errorf("got:\n%s\nwant:\n%s", out, want)
	}
}

// Regression: the trailing blank line above the closing brace lives on
// Block.End — straightforward direct manipulation.
func TestBlankLines_StripsTrailingBlankLineAtBlockEnd(t *testing.T) {
	src := `package main

func main() {
	a := 1
	_ = a

}
`
	want := `package main

func main() {
	a := 1
	_ = a
}
`
	out := applyVisitor(t, src, format.NewBlankLinesVisitor(nil))
	if out != want {
		t.Errorf("got:\n%s\nwant:\n%s", out, want)
	}
}

func TestBlankLines_CapsRunOfBlankLinesInBlock(t *testing.T) {
	src := `package main

func main() {
	a := 1



	b := 2
	_ = a + b
}
`
	want := `package main

func main() {
	a := 1

	b := 2
	_ = a + b
}
`
	out := applyVisitor(t, src, format.NewBlankLinesVisitor(nil))
	if out != want {
		t.Errorf("got:\n%s\nwant:\n%s", out, want)
	}
}

// ---- TabsAndIndentsVisitor ----

func TestTabsAndIndents_ReindentsFunctionBody(t *testing.T) {
	src := "package main\n\nfunc main() {\n\t\t   a := 1\n\t_ = a\n}\n"
	want := "package main\n\nfunc main() {\n\ta := 1\n\t_ = a\n}\n"
	out := applyVisitor(t, src, format.NewTabsAndIndentsVisitor(nil))
	if out != want {
		t.Errorf("got %q, want %q", out, want)
	}
}

func TestTabsAndIndents_NestedBlockGetsTwoTabs(t *testing.T) {
	src := `package main

func main() {
	if true {
	a := 1
	_ = a
	}
}
`
	want := `package main

func main() {
	if true {
		a := 1
		_ = a
	}
}
`
	out := applyVisitor(t, src, format.NewTabsAndIndentsVisitor(nil))
	if out != want {
		t.Errorf("got:\n%s\nwant:\n%s", out, want)
	}
}

// ---- SpacesVisitor ----

func TestSpaces_NormalizesBinaryOperatorSpacing(t *testing.T) {
	src := `package main

func main() {
	a := 1+2
	_ = a
}
`
	want := `package main

func main() {
	a := 1 + 2
	_ = a
}
`
	out := applyVisitor(t, src, format.NewSpacesVisitor(nil))
	if out != want {
		t.Errorf("got:\n%s\nwant:\n%s", out, want)
	}
}

// Regression: when the right operand of `:=` is itself a Binary, the
// leading single-space-after-`:=` lives on the leftmost leaf of the
// Binary tree (e.g., the Literal `1` in `1+2+3`). Setting Binary.Prefix
// directly would double the space.
func TestSpaces_NoSpaceDoublingWithBinaryOperand(t *testing.T) {
	src := `package main

func main() {
	a := 1+2+3
	_ = a
}
`
	want := `package main

func main() {
	a := 1 + 2 + 3
	_ = a
}
`
	out := applyVisitor(t, src, format.NewSpacesVisitor(nil))
	if out != want {
		t.Errorf("got:\n%s\nwant:\n%s", out, want)
	}
}

// Regression: same delegation rule when the assigned expression is a
// FieldAccess — the space-after-`:=` lives on FieldAccess.Target.Prefix.
func TestSpaces_FieldAccessLeadingSpace(t *testing.T) {
	src := `package main

func main() {
	x := struct{ a int }{a: 1}
	y :=x.a
	_ = y
}
`
	want := `package main

func main() {
	x := struct{ a int }{a: 1}
	y := x.a
	_ = y
}
`
	out := applyVisitor(t, src, format.NewSpacesVisitor(nil))
	if out != want {
		t.Errorf("got:\n%s\nwant:\n%s", out, want)
	}
}

// Regression: TabsAndIndentsVisitor places `case` clauses at the
// switch-keyword's depth (gofmt convention) and case bodies one tab
// deeper.
func TestTabsAndIndents_SwitchCaseAlignsWithSwitch(t *testing.T) {
	src := `package main

func main() {
	switch x := 1; x {
		case 1:
			println("one")
		case 2:
			println("two")
	}
}
`
	want := `package main

func main() {
	switch x := 1; x {
	case 1:
		println("one")
	case 2:
		println("two")
	}
}
`
	out := applyVisitor(t, src, format.NewTabsAndIndentsVisitor(nil))
	if out != want {
		t.Errorf("got:\n%s\nwant:\n%s", out, want)
	}
}

// ---- AutoFormatVisitor (composition) ----

func TestAutoFormat_FullPipelineEndToEnd(t *testing.T) {
	// Combines: trailing whitespace on `func main() {`, blank line at
	// start of body, wrong indent on nested block + its body,
	// missing space around `+`. Expect all four passes to fire.
	src := "package main\n\nfunc main() {   \n\n\n\tif true {\n\ta := 1+2\n\t_ = a\n\t}\n}\n"
	want := "package main\n\nfunc main() {\n\tif true {\n\t\ta := 1 + 2\n\t\t_ = a\n\t}\n}\n"
	out := applyVisitor(t, src, format.NewAutoFormatVisitor(nil))
	if out != want {
		t.Errorf("got %q, want %q", out, want)
	}
}

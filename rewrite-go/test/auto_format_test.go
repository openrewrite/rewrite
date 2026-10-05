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

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// applyVisitor parses src, runs visitor (and any DoAfterVisit-queued
// follow-ups), and returns the printed result.
func applyVisitor(t *testing.T, src string, v recipe.TreeVisitor) string {
	t.Helper()
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	require.NoError(t, err, "parse error")
	result := v.Visit(cu, nil)
	require.NotNil(t, result, "visit returned nil")

	final := visitor.DrainAfterVisits(v, result.(java.Tree), nil)
	return printer.Print(final)
}

func TestAutoFormatService_RegisteredOnInit(t *testing.T) {
	svc := recipe.Service[*recipes.AutoFormatService](nil)
	require.NotNil(t, svc, "expected AutoFormatService to be registered, got nil")
}

func TestRemoveTrailingWhitespace_StripsTrailingTabsFromLines(t *testing.T) {
	src := "package main   \n\nfunc main() {}\n"
	out := applyVisitor(t, src, format.NewRemoveTrailingWhitespaceVisitor(nil))
	want := "package main\n\nfunc main() {}\n"
	assert.Equal(t, want, out)
}

// Regression: the leading blank line above the first statement of a
// block lives on the *leftmost descendant* of that statement (e.g.
// Variable.Prefix), not on Asg.Prefix. The visitor walks the leftmost
// spine via transformLeftmostPrefix to find it.

// Regression: the trailing blank line above the closing brace lives on
// Block.End — straightforward direct manipulation.

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
	assert.Equal(t, want, out)
}

func TestTabsAndIndents_ReindentsFunctionBody(t *testing.T) {
	src := "package main\n\nfunc main() {\n\t\t   a := 1\n\t_ = a\n}\n"
	want := "package main\n\nfunc main() {\n\ta := 1\n\t_ = a\n}\n"
	out := applyVisitor(t, src, format.NewTabsAndIndentsVisitor(nil))
	assert.Equal(t, want, out)
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
	assert.Equal(t, want, out)
}

// Regression: when the right operand of `:=` is itself a Binary, the
// leading single-space-after-`:=` lives on the leftmost leaf of the
// Binary tree (e.g., the Literal `1` in `1+2+3`). Setting Binary.Prefix
// directly would double the space.

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
	assert.Equal(t, want, out)
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
	assert.Equal(t, want, out)
}

func TestAutoFormat_FullPipelineEndToEnd(t *testing.T) {
	// Combines: trailing whitespace on `func main() {`, blank line at
	// start of body, wrong indent on nested block + its body,
	// missing space around `+`. Expect all four passes to fire.
	src := "package main\n\nfunc main() {   \n\n\n\tif true {\n\ta := 1+2\n\t_ = a\n\t}\n}\n"
	want := "package main\n\nfunc main() {\n\n\tif true {\n\t\ta := 1 + 2\n\t\t_ = a\n\t}\n}\n"
	out := applyVisitor(t, src, format.NewAutoFormatVisitor(nil))
	assert.Equal(t, want, out)
}

func TestTabsAndIndents_IndentsImportBlock(t *testing.T) {
	out := applyVisitor(t, "package p\n\nimport (\n\"fmt\"\n\"os\"\n)\n\nvar _ = fmt.Sprint(os.Args)\n",
		format.NewTabsAndIndentsVisitor(nil))
	want := "package p\n\nimport (\n\t\"fmt\"\n\t\"os\"\n)\n\nvar _ = fmt.Sprint(os.Args)\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

func TestTabsAndIndents_IndentsLeadingComment(t *testing.T) {
	out := applyVisitor(t, "package p\n\nfunc f() {\n// leading\nx := 1\n_ = x\n}\n",
		format.NewTabsAndIndentsVisitor(nil))
	want := "package p\n\nfunc f() {\n\t// leading\n\tx := 1\n\t_ = x\n}\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

func TestTabsAndIndents_IndentsConsecutiveComments(t *testing.T) {
	out := applyVisitor(t, "package p\n\nfunc f() {\n// one\n// two\nx := 1\n_ = x\n}\n",
		format.NewTabsAndIndentsVisitor(nil))
	want := "package p\n\nfunc f() {\n\t// one\n\t// two\n\tx := 1\n\t_ = x\n}\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

func TestTabsAndIndents_LeavesTrailingCommentOnItsLine(t *testing.T) {
	out := applyVisitor(t, "package p\n\nfunc f() {\nx := 1 // trailing\n_ = x\n}\n",
		format.NewTabsAndIndentsVisitor(nil))
	want := "package p\n\nfunc f() {\n\tx := 1 // trailing\n\t_ = x\n}\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

func TestTabsAndIndents_IndentsGroupedDeclarations(t *testing.T) {
	cases := map[string][2]string{
		"var group": {
			"package p\n\nvar (\na = 1\nb = 2\n)\n",
			"package p\n\nvar (\n\ta = 1\n\tb = 2\n)\n"},
		"const group": {
			"package p\n\nconst (\nA = 1\nB = 2\n)\n",
			"package p\n\nconst (\n\tA = 1\n\tB = 2\n)\n"},
		"type group": {
			"package p\n\ntype (\nA int\nB string\n)\n",
			"package p\n\ntype (\n\tA int\n\tB string\n)\n"},
		"struct body": {
			"package p\n\ntype T struct {\nX int\nY int\n}\n",
			"package p\n\ntype T struct {\n\tX int\n\tY int\n}\n"},
		"interface body": {
			"package p\n\ntype I interface {\nFoo() int\n}\n",
			"package p\n\ntype I interface {\n\tFoo() int\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewTabsAndIndentsVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestTabsAndIndents_IndentsCompositeLiteral(t *testing.T) {
	out := applyVisitor(t, "package p\n\nvar m = map[string]bool{\n\"a\": true,\n\"b\": false,\n}\n",
		format.NewTabsAndIndentsVisitor(nil))
	want := "package p\n\nvar m = map[string]bool{\n\t\"a\": true,\n\t\"b\": false,\n}\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

func TestTabsAndIndents_IndentsTrailingComments(t *testing.T) {
	cases := map[string][2]string{
		"comment before closing brace": {
			"package p\n\nfunc f() {\nx := 1\n_ = x\n// trailing note\n}\n",
			"package p\n\nfunc f() {\n\tx := 1\n\t_ = x\n\t// trailing note\n}\n"},
		"comment as whole case body": {
			"package p\n\nfunc g(i int) {\nswitch i {\ndefault:\n// note\n}\n}\n",
			"package p\n\nfunc g(i int) {\n\tswitch i {\n\tdefault:\n\t\t// note\n\t}\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewTabsAndIndentsVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestTabsAndIndents_AlignsSelectCaseWithSelect(t *testing.T) {
	out := applyVisitor(t, "package p\n\nfunc h(c chan int) {\nselect {\ncase <-c:\nreturn\n}\n}\n",
		format.NewTabsAndIndentsVisitor(nil))
	want := "package p\n\nfunc h(c chan int) {\n\tselect {\n\tcase <-c:\n\t\treturn\n\t}\n}\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

func TestTabsAndIndents_IndentsWrappedLists(t *testing.T) {
	cases := map[string][2]string{
		"call arguments": {
			"package p\n\nimport \"os/exec\"\n\nfunc f() {\ncmd := exec.Command(\"go\", \"tool\",\n\"-a\", \"b\",\n)\n_ = cmd\n}\n",
			"package p\n\nimport \"os/exec\"\n\nfunc f() {\n\tcmd := exec.Command(\"go\", \"tool\",\n\t\t\"-a\", \"b\",\n\t)\n\t_ = cmd\n}\n"},
		"case expressions": {
			"package p\n\nfunc f(i int) {\nswitch i {\ncase 1, 2,\n3:\nreturn\n}\n}\n",
			"package p\n\nfunc f(i int) {\n\tswitch i {\n\tcase 1, 2,\n\t\t3:\n\t\treturn\n\t}\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewTabsAndIndentsVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestTabsAndIndents_IndentsWrappedBinary(t *testing.T) {
	cases := map[string][2]string{
		"in if condition": {
			"package p\n\nfunc f(a, b, c bool) bool {\nif a &&\nb &&\nc {\nreturn true\n}\nreturn false\n}\n",
			"package p\n\nfunc f(a, b, c bool) bool {\n\tif a &&\n\t\tb &&\n\t\tc {\n\t\treturn true\n\t}\n\treturn false\n}\n"},
		"in assignment": {
			"package p\n\nfunc f(a, b bool) bool {\nx := a ||\nb\nreturn x\n}\n",
			"package p\n\nfunc f(a, b bool) bool {\n\tx := a ||\n\t\tb\n\treturn x\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewTabsAndIndentsVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestBinarySpacing(t *testing.T) {
	cases := map[string][2]string{
		"one precedence keeps blanks": {
			"package p\n\nfunc f() int {\n\ta := 1+2\n\treturn a\n}\n",
			"package p\n\nfunc f() int {\n\ta := 1 + 2\n\treturn a\n}\n"},
		"chain of one precedence keeps blanks": {
			"package p\n\nfunc f() int {\n\ta := 1+2+3\n\treturn a\n}\n",
			"package p\n\nfunc f() int {\n\ta := 1 + 2 + 3\n\treturn a\n}\n"},
		"tighter operator loses them": {
			"package p\n\nfunc f(x, y, z int) int {\n\tb := x * y + z\n\treturn b\n}\n",
			"package p\n\nfunc f(x, y, z int) int {\n\tb := x*y + z\n\treturn b\n}\n"},
		"both sides of a sum tighten": {
			"package p\n\nfunc f(x, y int) int {\n\tg := 2 * x + 3 * y\n\treturn g\n}\n",
			"package p\n\nfunc f(x, y int) int {\n\tg := 2*x + 3*y\n\treturn g\n}\n"},
		"nesting inside an index tightens": {
			"package p\n\nfunc f(s []int, i int) int {\n\td := s[i + 1]\n\treturn d\n}\n",
			"package p\n\nfunc f(s []int, i int) int {\n\td := s[i+1]\n\treturn d\n}\n"},
		"nesting inside a comparison tightens": {
			"package p\n\nfunc f(a, b, c int) bool {\n\treturn a > b - c\n}\n",
			"package p\n\nfunc f(a, b, c int) bool {\n\treturn a > b-c\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewBinarySpacingVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestBinarySpacingInSlices(t *testing.T) {
	cases := map[string][2]string{
		"single index stays tight": {
			"package p\n\nfunc f(a []int, n int) {\n\ta = a[:n - 1]\n\t_ = a\n}\n",
			"package p\n\nfunc f(a []int, n int) {\n\ta = a[:n-1]\n\t_ = a\n}\n"},
		"two indices with a binary set the colon off": {
			"package p\n\nfunc f(a []int, i, j int) {\n\t_ = a[i:j + 1]\n}\n",
			"package p\n\nfunc f(a []int, i, j int) {\n\t_ = a[i : j+1]\n}\n"},
		"two plain indices stay tight": {
			"package p\n\nfunc f(a []int, i, j int) {\n\t_ = a[i : j]\n}\n",
			"package p\n\nfunc f(a []int, i, j int) {\n\t_ = a[i:j]\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewBinarySpacingVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestBlankLinesAtBlockBraces(t *testing.T) {
	cases := map[string][2]string{
		"a function body keeps one": {
			"package p\n\nfunc f() {\n\n\ta := 1\n\t_ = a\n\n}\n",
			"package p\n\nfunc f() {\n\n\ta := 1\n\t_ = a\n\n}\n"},
		"a function body caps a run at one": {
			"package p\n\nfunc f() {\n\n\n\ta := 1\n\t_ = a\n\n\n}\n",
			"package p\n\nfunc f() {\n\n\ta := 1\n\t_ = a\n\n}\n"},
		"a struct body sits flush": {
			"package p\n\ntype T struct {\n\n\tA int\n\n}\n",
			"package p\n\ntype T struct {\n\tA int\n}\n"},
		"an interface body sits flush": {
			"package p\n\ntype I interface {\n\n\tM() int\n\n}\n",
			"package p\n\ntype I interface {\n\tM() int\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewBlankLinesVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestTabsAndIndents_CaseLeadInComments(t *testing.T) {
	cases := map[string][2]string{
		"a comment at the case keyword's level stays": {
			"package p\n\nfunc f(a, b bool) {\n\tswitch {\n\tcase a:\n\t// c\n\tcase b:\n\t\treturn\n\t}\n}\n",
			"package p\n\nfunc f(a, b bool) {\n\tswitch {\n\tcase a:\n\t// c\n\tcase b:\n\t\treturn\n\t}\n}\n"},
		"a comment at the body's level stays there": {
			"package p\n\nfunc f(a, b bool) {\n\tswitch {\n\tcase a:\n\t\t// c\n\tcase b:\n\t\treturn\n\t}\n}\n",
			"package p\n\nfunc f(a, b bool) {\n\tswitch {\n\tcase a:\n\t\t// c\n\tcase b:\n\t\treturn\n\t}\n}\n"},
		"a comment written anywhere else moves to the body": {
			"package p\n\nfunc f(a, b bool) {\n\tswitch {\n\tcase a:\n// c\n\tcase b:\n\t\treturn\n\t}\n}\n",
			"package p\n\nfunc f(a, b bool) {\n\tswitch {\n\tcase a:\n\t\t// c\n\tcase b:\n\t\treturn\n\t}\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewTabsAndIndentsVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestTabsAndIndents_OutdentsLabels(t *testing.T) {
	out := applyVisitor(t,
		"package p\n\nfunc f() {\nloop:\nfor {\nbreak loop\n}\nif true {\ninner:\nfor {\nbreak inner\n}\n}\n}\n",
		format.NewTabsAndIndentsVisitor(nil))
	want := "package p\n\nfunc f() {\nloop:\n\tfor {\n\t\tbreak loop\n\t}\n\tif true {\n\tinner:\n\t\tfor {\n\t\t\tbreak inner\n\t\t}\n\t}\n}\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

func TestBinarySpacing_SiblingArgumentsKeepTheirDepth(t *testing.T) {
	// The parenthesised argument lowers the depth for its own contents only.
	out := applyVisitor(t,
		"package p\n\nfunc f(g func(int, int), max int) {\n\tg(-(max - 1), max - 1)\n}\n",
		format.NewBinarySpacingVisitor(nil))
	want := "package p\n\nfunc f(g func(int, int), max int) {\n\tg(-(max - 1), max-1)\n}\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

func TestBlankLinesAroundCommentsInDeclarationLists(t *testing.T) {
	cases := map[string][2]string{
		"a bare field sits flush": {
			"package p\n\ntype A struct {\n\n\tX int\n\n}\n",
			"package p\n\ntype A struct {\n\tX int\n}\n"},
		"a leading comment keeps its separation": {
			"package p\n\ntype B struct {\n\n\t// doc\n\tX int\n}\n",
			"package p\n\ntype B struct {\n\n\t// doc\n\tX int\n}\n"},
		"a trailing comment keeps its separation": {
			"package p\n\ntype C struct {\n\tX int\n\n\t// trailing\n}\n",
			"package p\n\ntype C struct {\n\tX int\n\n\t// trailing\n}\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewBlankLinesVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

func TestTabsAndIndents_LabelBeforeAClosingBrace(t *testing.T) {
	out := applyVisitor(t,
		"package p\n\nfunc f(xs []int) {\n\tfor _, x := range xs {\n\t\t_ = x\n\tA:\n\t}\n\treturn\n}\n",
		format.NewTabsAndIndentsVisitor(nil))
	want := "package p\n\nfunc f(xs []int) {\n\tfor _, x := range xs {\n\t\t_ = x\n\tA:\n\t}\n\treturn\n}\n"
	if out != want {
		t.Errorf("want %q\ngot  %q", want, out)
	}
}

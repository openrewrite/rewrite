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
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
	"testing"

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func gofmtCU(t *testing.T, src string) *golang.CompilationUnit {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	out, err := format.Gofmt(cu, nil)
	if err != nil {
		t.Fatalf("gofmt: %v", err)
	}
	return out
}

func assertGofmt(t *testing.T, before, after string) {
	t.Helper()
	if got := printer.Print(gofmtCU(t, before)); got != after {
		t.Errorf("unexpected output\n--- want ---\n%s\n--- got ---\n%s", after, got)
	}
}

func TestGofmt_AlignsStructFields(t *testing.T) {
	assertGofmt(t,
		"package p\n\ntype T struct {\nName string `json:\"name\"`\nVeryLongFieldName int `json:\"v\"`\n}\n",
		"package p\n\ntype T struct {\n\tName              string `json:\"name\"`\n\tVeryLongFieldName int    `json:\"v\"`\n}\n")
}

func TestGofmt_AlignsTrailingComments(t *testing.T) {
	assertGofmt(t,
		"package p\n\nfunc f() {\nx := 1 // one\nyy := 22 // two\n_, _ = x, yy\n}\n",
		"package p\n\nfunc f() {\n\tx := 1   // one\n\tyy := 22 // two\n\t_, _ = x, yy\n}\n")
}

func TestGofmt_NormalizesSpacingAndIndent(t *testing.T) {
	assertGofmt(t,
		"package p\n\nfunc f(a int,b int)int{\nif a>b{\nreturn a\n}\nreturn b\n}\n",
		"package p\n\nfunc f(a int, b int) int {\n\tif a > b {\n\t\treturn a\n\t}\n\treturn b\n}\n")
}

func TestGofmt_AlreadyFormattedIsUnchanged(t *testing.T) {
	src := "package p\n\nimport \"fmt\"\n\nfunc f() {\n\tfmt.Println(\"hi\")\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	out, err := format.Gofmt(cu, nil)
	if err != nil {
		t.Fatalf("gofmt: %v", err)
	}
	if out != cu {
		t.Error("expected the same tree instance back for already-formatted source")
	}
}

func TestGofmt_PreservesIdsAndTypes(t *testing.T) {
	src := "package main\n\nfunc main() {\nx := 42\n_ = x\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	beforeIds, beforeTypes := identifierFacts(cu)
	if len(beforeIds) == 0 {
		t.Fatal("no identifiers found")
	}
	typed := 0
	for _, ty := range beforeTypes {
		if ty != nil {
			typed++
		}
	}
	if typed == 0 {
		t.Fatal("expected at least one attributed identifier")
	}

	out, err := format.Gofmt(cu, nil)
	if err != nil {
		t.Fatalf("gofmt: %v", err)
	}
	afterIds, afterTypes := identifierFacts(out)

	if len(afterIds) != len(beforeIds) {
		t.Fatalf("identifier count changed: %d -> %d", len(beforeIds), len(afterIds))
	}
	for i := range beforeIds {
		if beforeIds[i] != afterIds[i] {
			t.Errorf("identifier %d lost its id", i)
		}
		if beforeTypes[i] != afterTypes[i] {
			t.Errorf("identifier %d lost its type attribution", i)
		}
	}
}

func TestGofmt_TargetSubtreeOnly(t *testing.T) {
	src := "package p\n\nfunc a() {\nx:=1\n_ = x\n}\n\nfunc b() {\ny:=2\n_ = y\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	target := cu.Statements[1].Element // func b

	out, err := format.Gofmt(cu, target)
	if err != nil {
		t.Fatalf("gofmt: %v", err)
	}
	got := printer.Print(out)
	want := "package p\n\nfunc a() {\nx:=1\n_ = x\n}\n\nfunc b() {\n\ty := 2\n\t_ = y\n}\n"
	if got != want {
		t.Errorf("unexpected output\n--- want ---\n%s\n--- got ---\n%s", want, got)
	}
}

func TestGofmt_FormatsAroundStructuralDivergence(t *testing.T) {
	// go/printer drops one level of redundant parentheses, so the reparsed tree
	// has fewer nodes than the original. The parentheses survive — a splice
	// carries no token changes — while the rest of the file is laid out.
	assertGofmt(t,
		"package p\n\nfunc f() int {\nreturn ((1 + 2))\n}\n",
		"package p\n\nfunc f() int {\n\treturn ((1 + 2))\n}\n")
}

func TestGofmt_MatchesGofmtBinaryOnCorpus(t *testing.T) {
	gofmtBin, err := exec.LookPath("gofmt")
	if err != nil {
		t.Skip("gofmt binary not on PATH")
	}

	var fixtures []string
	root := filepath.Join("testdata", "printer-corpus")
	if err := filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err == nil && !info.IsDir() && strings.HasSuffix(path, ".go") {
			fixtures = append(fixtures, path)
		}
		return err
	}); err != nil {
		t.Fatalf("walk corpus: %v", err)
	}

	for _, f := range fixtures {
		t.Run(filepath.Base(f), func(t *testing.T) {
			content, err := os.ReadFile(f)
			if err != nil {
				t.Fatalf("read: %v", err)
			}
			mangled := stripIndent(string(content))

			cmd := exec.Command(gofmtBin)
			cmd.Stdin = strings.NewReader(mangled)
			want, err := cmd.Output()
			if err != nil {
				t.Skipf("gofmt rejected the mangled fixture: %v", err)
			}

			cu, err := parser.NewGoParser().Parse(filepath.Base(f), mangled)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			out, err := format.Gofmt(cu, nil)
			if err != nil {
				t.Fatalf("gofmt: %v", err)
			}
			if got := printer.Print(out); got != string(want) {
				t.Errorf("does not match gofmt\n--- want ---\n%s\n--- got ---\n%s", want, got)
			}
		})
	}
}

var indentPattern = regexp.MustCompile(`(?m)^[ \t]+`)

// stripIndent removes leading whitespace from every line. Line structure — and
// so Go's implicit semicolons — is preserved, leaving the token stream intact.
func stripIndent(src string) string {
	return indentPattern.ReplaceAllString(src, "")
}

type identifierCollector struct {
	visitor.GoVisitor
	ids   []uuid.UUID
	types []java.JavaType
}

func (v *identifierCollector) VisitIdentifier(ident *java.Identifier, p any) java.J {
	v.ids = append(v.ids, ident.ID)
	v.types = append(v.types, ident.Type)
	return v.GoVisitor.VisitIdentifier(ident, p)
}

func identifierFacts(cu java.Tree) ([]uuid.UUID, []java.JavaType) {
	c := visitor.Init(&identifierCollector{})
	c.Visit(cu, nil)
	return c.ids, c.types
}

func TestGofmtVisitor_FormatsCompilationUnit(t *testing.T) {
	got := applyVisitor(t, "package p\n\nfunc f(a int,b int)int{\nreturn a+b\n}\n", format.NewGofmtVisitor(nil))
	want := "package p\n\nfunc f(a int, b int) int {\n\treturn a + b\n}\n"
	if got != want {
		t.Errorf("unexpected output\n--- want ---\n%s\n--- got ---\n%s", want, got)
	}
}

func BenchmarkGofmt(b *testing.B) {
	content, err := os.ReadFile(filepath.Join(runtime.GOROOT(), "src", "net", "http", "server.go"))
	if err != nil {
		b.Skip(err)
	}
	mangled := stripIndent(string(content))
	cu, err := parser.NewGoParser().Parse("server.go", mangled)
	if err != nil {
		b.Skip(err)
	}
	b.ReportAllocs()
	for b.Loop() {
		if _, err := format.Gofmt(cu, nil); err != nil {
			b.Fatal(err)
		}
	}
}

func BenchmarkParseOnly(b *testing.B) {
	content, err := os.ReadFile(filepath.Join(runtime.GOROOT(), "src", "net", "http", "server.go"))
	if err != nil {
		b.Skip(err)
	}
	src := string(content)
	b.ReportAllocs()
	for b.Loop() {
		if _, err := parser.NewGoParser().Parse("server.go", src); err != nil {
			b.Fatal(err)
		}
	}
}

func TestGofmt_KeepsIdentityOfUnchangedSubtrees(t *testing.T) {
	src := "package p\n\nfunc a() {\n\tx := 1\n\t_ = x\n}\n\nfunc b() {\ny:=2\n_ = y\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	out, err := format.Gofmt(cu, nil)
	if err != nil {
		t.Fatalf("gofmt: %v", err)
	}
	if out == cu {
		t.Fatal("expected a new compilation unit")
	}
	// func a is already formatted, so nothing below it may be rebuilt.
	if out.Statements[0].Element != cu.Statements[0].Element {
		t.Error("untouched declaration was replaced")
	}
	if out.Statements[1].Element == cu.Statements[1].Element {
		t.Error("reformatted declaration was not replaced")
	}
}

func TestGofmt_ReformatsComments(t *testing.T) {
	assertGofmt(t,
		"package p\n\n// Doc explains things.\n//\n//  - first item\n//  - second item\n//\n// Example:\n//\t\tcode()\nfunc F() {\n// inner\n/* block\nsecond line\n*/\nx := 1 // trailing\n_ = x\n}\n",
		"package p\n\n// Doc explains things.\n//\n//   - first item\n//   - second item\n//\n// Example:\n//\n//\tcode()\nfunc F() {\n\t// inner\n\t/* block\n\t   second line\n\t*/\n\tx := 1 // trailing\n\t_ = x\n}\n")
}

func TestGofmt_FormatsGenericInstantiation(t *testing.T) {
	// Shapes that need type attribution to map: a layout-only reparse reads
	// List[int] as an index expression, which the splice refuses.
	assertGofmt(t,
		"package p\n\ntype List[T any] struct{ items []T }\n\nfunc Get[T any](l List[T]) []T { return l.items }\n\nfunc f() {\nm := List[int]{}\n_ = Get[int](m)\n}\n",
		"package p\n\ntype List[T any] struct{ items []T }\n\nfunc Get[T any](l List[T]) []T { return l.items }\n\nfunc f() {\n\tm := List[int]{}\n\t_ = Get[int](m)\n}\n")
}

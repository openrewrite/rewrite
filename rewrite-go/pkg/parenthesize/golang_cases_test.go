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
	goparser "go/parser"
	gotoken "go/token"
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

const caseHeader = `package main

type T struct{ f int }

`

// Parentheses Go's own grammar requires, rather than ones precedence calls for.
// Each body round-trips: stripping them and grouping again returns the source.
func TestVisitorRestoresGrammarRequiredParentheses(t *testing.T) {
	for _, tc := range []struct{ name, body string }{
		{"composite in if header", "func c(i int) {\n\tif i == (T{}).f {\n\t\t_ = i\n\t}\n}\n"},
		{"composite in switch header", "func c(i int) {\n\tswitch (T{}).f {\n\t}\n}\n"},
		{"unary on unary", "func c(i int) {\n\t_ = -(-i)\n}\n"},
		{"pointer conversion", "func c(p *T) {\n\t_ = (*T)(p)\n}\n"},
		{"channel conversion", "func c(ch chan int) {\n\t_ = (<-chan int)(ch)\n}\n"},
		{"func conversion", "func c(fn func()) {\n\t_ = (func())(fn)\n}\n"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			src := caseHeader + tc.body
			cu, err := parser.NewGoParser().Parse("cases.go", src)
			if err != nil {
				t.Fatal(err)
			}
			stripped := printer.Print(visitor.Init(&unwrapper{}).Visit(cu, nil))
			if stripped == src {
				t.Fatalf("nothing was stripped, so the case proves nothing")
			}
			got := printer.Print(NewVisitor().Visit(visitor.Init(&unwrapper{}).Visit(cu, nil), nil))
			if _, err := goparser.ParseFile(gotoken.NewFileSet(), "cases.go", got, 0); err != nil {
				t.Errorf("result does not parse: %v\n%s", err, got)
			}
			if got != src {
				t.Errorf("round trip differs\n got:\n%s\nwant:\n%s", got, src)
			}
		})
	}
}

// Stripping every parenthesis from real code and grouping it again has to
// produce something Go still accepts, which covers spellings no hand-written
// case list thinks of.
func TestVisitorRegroupsStdlibToParseableCode(t *testing.T) {
	restored := 0
	for _, rel := range []string{
		"net/http/cookie.go",
		"strings/replace.go",
		"encoding/json/encode.go",
		"go/printer/nodes.go",
		"net/http/server.go",
		"runtime/mgc.go",
	} {
		t.Run(rel, func(t *testing.T) {
			content, err := os.ReadFile(filepath.Join(runtime.GOROOT(), "src", rel))
			if err != nil {
				t.Skip(err)
			}
			cu, err := parser.NewGoParser().Parse(filepath.Base(rel), string(content))
			if err != nil {
				t.Skip(err)
			}
			ungrouped := printer.Print(visitor.Init(&unwrapper{}).Visit(cu, nil))
			got := printer.Print(NewVisitor().Visit(visitor.Init(&unwrapper{}).Visit(cu, nil), nil))
			if _, err := goparser.ParseFile(gotoken.NewFileSet(), rel, got, 0); err != nil {
				t.Fatalf("regrouped source does not parse: %v", err)
			}
			if got != ungrouped {
				restored++
			}
			// Reading the result back and stripping it again lands on the same
			// text only if the parentheses put back group it as it grouped
			// before, so this is the check that the meaning survived.
			back, err := parser.NewGoParser().Parse(filepath.Base(rel), got)
			if err != nil {
				t.Fatalf("regrouped source does not re-parse: %v", err)
			}
			if regrouped := printer.Print(visitor.Init(&unwrapper{}).Visit(back, nil)); regrouped != ungrouped {
				t.Errorf("grouping changed; first difference at %d", firstDiff(ungrouped, regrouped))
			}
		})
	}
	// Files whose parentheses are all redundant round-trip without any being
	// put back, and hold for a visitor that does nothing. The corpus as a whole
	// has to exercise one that does not.
	if restored == 0 {
		t.Errorf("no file needed a parenthesis restored, so the corpus proves nothing")
	}
}

func firstDiff(a, b string) int {
	for i := 0; i < len(a) && i < len(b); i++ {
		if a[i] != b[i] {
			return i
		}
	}
	return min(len(a), len(b))
}

// Positions that read unambiguously already, where a parenthesis would be noise.
func TestVisitorLeavesUnambiguousPositionsAlone(t *testing.T) {
	for _, tc := range []struct{ name, body string }{
		{"composite in if body", "func c(i int) {\n\tif i > 0 {\n\t\t_ = T{}\n\t}\n}\n"},
		{"composite in for body", "func c(i int) {\n\tfor i > 0 {\n\t\t_ = T{}\n\t}\n}\n"},
		{"composite in case body", "func c(i int) {\n\tswitch i {\n\tcase 0:\n\t\t_ = T{}\n\t}\n}\n"},
		{"slice bounds", "func c(a []int, i, j int) {\n\t_ = a[i+1 : j+2]\n\t_ = a[i+1 : j+2 : j+3]\n}\n"},
		{"index expression", "func c(a []int, i int) {\n\t_ = a[i+1]\n}\n"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			src := caseHeader + tc.body
			cu, err := parser.NewGoParser().Parse("cases.go", src)
			if err != nil {
				t.Fatal(err)
			}
			if got := printer.Print(NewVisitor().Visit(cu, nil)); got != src {
				t.Errorf("parentheses were added\n got:\n%s\nwant:\n%s", got, src)
			}
		})
	}
}

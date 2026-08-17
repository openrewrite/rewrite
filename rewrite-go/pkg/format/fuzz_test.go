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

package format

import (
	goparser "go/parser"
	"go/scanner"
	gotoken "go/token"
	"math/rand"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

var fuzzSeeds = []string{
	"package p\n\nimport (\n\t\"fmt\"\n\t\"os\"\n)\n\nfunc main() {\n\tfmt.Println(os.Args[0])\n}\n",
	"package p\n\ntype T struct {\n\tName string `json:\"name\"`\n\tN    int\n}\n\nfunc (t T) String() string { return t.Name }\n",
	"package p\n\n// Doc explains things.\n//\n//   - first\n//   - second\nfunc F(i int) int {\n\tswitch i {\n\tcase 1, 2:\n\t\treturn i * 2\n\tdefault:\n\t\treturn i&0xff | 1\n\t}\n}\n",
	"package p\n\nfunc G(c chan int) {\n\tselect {\n\tcase v := <-c:\n\t\t_ = v\n\tdefault:\n\t}\n}\n",
	"package p\n\nvar table = []struct {\n\tname string\n\tn    int\n}{\n\t{name: \"a\", n: 1},\n\t{name: \"b\", n: 2},\n}\n",
	"package p\n\nfunc Map[T, U any](in []T, f func(T) U) []U {\n\tout := make([]U, 0, len(in))\n\tfor _, v := range in {\n\t\tout = append(out, f(v))\n\t}\n\treturn out\n}\n",
}

// FuzzFormat re-whitespaces valid Go source and holds the formatter to the
// properties that survive any input layout: it emits parseable Go, it moves no
// tokens, and a second run changes nothing wherever gofmt is itself stable. Mutating whitespace rather than
// bytes keeps every generated input valid Go, so the budget goes on layouts
// instead of on inputs the parser rejects.
func FuzzFormat(f *testing.F) {
	for _, src := range fuzzSeeds {
		for _, seed := range []int64{1, 2, 3} {
			f.Add(src, seed)
		}
	}

	f.Fuzz(func(t *testing.T, src string, seed int64) {
		// The fuzzer mutates the seed bytes too, so most inputs are not Go at
		// all; only a valid compilation unit says anything about formatting.
		if _, err := goparser.ParseFile(gotoken.NewFileSet(), "f.go", src, goparser.ParseComments); err != nil {
			return
		}
		mutated, ok := reWhitespace(src, rand.New(rand.NewSource(seed)))
		if !ok {
			return
		}
		if _, err := goparser.ParseFile(gotoken.NewFileSet(), "f.go", mutated, goparser.ParseComments); err != nil {
			t.Fatalf("whitespace-only mutation invalidated valid Go: %v\n%s", err, mutated)
		}

		p := parser.NewGoParser()
		p.ParseOnly = true
		cu, err := p.Parse("f.go", mutated)
		if err != nil || printer.Print(cu) != mutated {
			return // a parser or printer gap, not a formatter property
		}

		formatted := runAutoFormat(cu)
		if _, err := goparser.ParseFile(gotoken.NewFileSet(), "f.go", formatted, goparser.ParseComments); err != nil {
			t.Fatalf("formatted output does not parse: %v\n%s", err, formatted)
		}
		if before, after := codeTokens(mutated), codeTokens(formatted); before != after {
			t.Fatalf("formatting moved tokens\n  before %s\n  after  %s", before, after)
		}

		// gofmt is itself not idempotent on every input: go/doc/comment reads an
		// indented line as a code block only when a blank line precedes it, and
		// the first pass removes the blank lines. Hold the formatter to
		// idempotence exactly where gofmt has it.
		once, err := gofmtSource("f.go", mutated)
		if err != nil {
			return
		}
		if twice, err := gofmtSource("f.go", once); err != nil || twice != once {
			return
		}

		again, err := p.Parse("f.go", formatted)
		if err != nil {
			t.Fatalf("cannot re-parse formatted output: %v", err)
		}
		if twice := runAutoFormat(again); twice != formatted {
			t.Fatalf("formatting is not idempotent\n  once %q\n  twice %q", formatted, twice)
		}
	})
}

func runAutoFormat(cu java.Tree) string {
	v := NewAutoFormatVisitor(nil)
	out := v.Visit(cu, nil)
	return printer.Print(visitor.DrainAfterVisits(v, out.(java.Tree), nil))
}

// codeTokens renders the token stream excluding comment text, which doc
// formatting is allowed to rewrite.
func codeTokens(src string) string {
	fset := gotoken.NewFileSet()
	file := fset.AddFile("f.go", fset.Base(), len(src))
	var s scanner.Scanner
	s.Init(file, []byte(src), nil, 0)
	var out strings.Builder
	for {
		_, tok, lit := s.Scan()
		if tok == gotoken.EOF {
			return out.String()
		}
		out.WriteString(tok.String())
		if lit != "" && tok != gotoken.SEMICOLON {
			out.WriteString("(" + lit + ")")
		}
		out.WriteByte(' ')
	}
}

// reWhitespace rebuilds src with every gap between tokens replaced. A gap
// holding a line break keeps one, since Go's semicolon insertion reads line
// breaks; a gap separating two tokens keeps a space; and an empty gap stays
// empty, since splitting a token like `<-` would change the program.
func reWhitespace(src string, rnd *rand.Rand) (string, bool) {
	fset := gotoken.NewFileSet()
	file := fset.AddFile("f.go", fset.Base(), len(src))
	var s scanner.Scanner
	s.Init(file, []byte(src), func(gotoken.Position, string) {}, scanner.ScanComments)

	var out strings.Builder
	prevEnd := 0
	for {
		pos, tok, lit := s.Scan()
		if tok == gotoken.EOF {
			break
		}
		offset := file.Offset(pos)
		if offset < prevEnd || offset > len(src) {
			return "", false
		}
		width := len(lit)
		switch {
		case tok == gotoken.SEMICOLON && lit == "\n":
			// Inserted for a line break rather than written, so it occupies no
			// source text and the gap that follows carries the break.
			width = 0
		case lit == "":
			width = len(tok.String())
		}
		if offset+width > len(src) {
			return "", false
		}

		out.WriteString(mutateGap(src[prevEnd:offset], rnd))
		out.WriteString(src[offset : offset+width])
		prevEnd = offset + width
	}
	out.WriteString(mutateGap(src[prevEnd:], rnd))
	return out.String(), true
}

func mutateGap(gap string, rnd *rand.Rand) string {
	if gap == "" {
		return ""
	}
	if !strings.Contains(gap, "\n") {
		return strings.Repeat(" ", 1+rnd.Intn(3))
	}
	var b strings.Builder
	for i := 0; i <= rnd.Intn(3); i++ {
		b.WriteString("\n")
	}
	switch rnd.Intn(3) {
	case 0:
		b.WriteString(strings.Repeat("\t", rnd.Intn(4)))
	case 1:
		b.WriteString(strings.Repeat(" ", rnd.Intn(8)))
	}
	return b.String()
}

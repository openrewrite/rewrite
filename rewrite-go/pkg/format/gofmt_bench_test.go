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
	gotoken "go/token"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

var indentPattern = regexp.MustCompile(`(?m)^[ \t]+`)

// benchInput is net/http/server.go with every line's indentation stripped, so
// both formatters face the same amount of work.
func benchInput(b *testing.B) (*golang.CompilationUnit, string) {
	b.Helper()
	content, err := os.ReadFile(filepath.Join(runtime.GOROOT(), "src", "net", "http", "server.go"))
	if err != nil {
		b.Skip(err)
	}
	mangled := indentPattern.ReplaceAllString(string(content), "")
	cu, err := parser.NewGoParser().Parse("server.go", mangled)
	if err != nil {
		b.Skip(err)
	}
	return cu, mangled
}

func BenchmarkFormatGofmt(b *testing.B) {
	cu, _ := benchInput(b)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		if _, err := Gofmt(cu, nil); err != nil {
			b.Fatal(err)
		}
	}
}

func BenchmarkFormatHandRolled(b *testing.B) {
	cu, _ := benchInput(b)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		v := NewAutoFormatVisitor(nil)
		out := v.Visit(cu, nil)
		visitor.DrainAfterVisits(v, out.(java.Tree), nil)
	}
}

func BenchmarkStagePrint(b *testing.B) {
	cu, _ := benchInput(b)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		printer.Print(cu)
	}
}

func BenchmarkStageGoPrinter(b *testing.B) {
	_, src := benchInput(b)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		if _, err := gofmtSource("server.go", src); err != nil {
			b.Fatal(err)
		}
	}
}

func BenchmarkStageReparseLayoutOnly(b *testing.B) {
	_, src := benchInput(b)
	formattedSrc, err := gofmtSource("server.go", src)
	if err != nil {
		b.Skip(err)
	}
	p := parser.NewGoParser()
	p.ParseOnly = true
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		if _, err := p.Parse("server.go", formattedSrc); err != nil {
			b.Fatal(err)
		}
	}
}

func BenchmarkStageReparseTyped(b *testing.B) {
	_, src := benchInput(b)
	formattedSrc, err := gofmtSource("server.go", src)
	if err != nil {
		b.Skip(err)
	}
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		if _, err := parser.NewGoParser().Parse("server.go", formattedSrc); err != nil {
			b.Fatal(err)
		}
	}
}

func BenchmarkStageSplice(b *testing.B) {
	cu, src := benchInput(b)
	formattedSrc, err := gofmtSource("server.go", src)
	if err != nil {
		b.Skip(err)
	}
	formatted, err := parser.NewGoParser().Parse("server.go", formattedSrc)
	if err != nil {
		b.Skip(err)
	}
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		SpliceWhitespace(cu, formatted, nil)
	}
}

// A recipe formatting one declaration it just synthesized, in a large file.

func firstDeclaration(b *testing.B, cu *golang.CompilationUnit) java.Tree {
	b.Helper()
	if len(cu.Statements) == 0 {
		b.Skip("no declarations")
	}
	return cu.Statements[0].Element
}

func BenchmarkSubtreeGofmt(b *testing.B) {
	cu, _ := benchInput(b)
	target := firstDeclaration(b, cu)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		if _, err := Gofmt(cu, target); err != nil {
			b.Fatal(err)
		}
	}
}

func BenchmarkSubtreeHandRolled(b *testing.B) {
	cu, _ := benchInput(b)
	target := firstDeclaration(b, cu)
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		v := NewAutoFormatVisitor(nil)
		out := v.Visit(target, nil)
		visitor.DrainAfterVisits(v, out.(java.Tree), nil)
	}
}

// Go parsing on its own, to separate it from LST construction.
func BenchmarkStageGoParseOnly(b *testing.B) {
	_, src := benchInput(b)
	formattedSrc, err := gofmtSource("server.go", src)
	if err != nil {
		b.Skip(err)
	}
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		fset := gotoken.NewFileSet()
		if _, err := goparser.ParseFile(fset, "server.go", formattedSrc, goparser.ParseComments|goparser.SkipObjectResolution); err != nil {
			b.Fatal(err)
		}
	}
}

// What a call costs when the file is already formatted and nothing changes.
func BenchmarkFormatGofmtNoOp(b *testing.B) {
	content, err := os.ReadFile(filepath.Join(runtime.GOROOT(), "src", "net", "http", "server.go"))
	if err != nil {
		b.Skip(err)
	}
	cu, err := parser.NewGoParser().Parse("server.go", string(content))
	if err != nil {
		b.Skip(err)
	}
	b.ReportAllocs()
	b.ResetTimer()
	for b.Loop() {
		if _, err := Gofmt(cu, nil); err != nil {
			b.Fatal(err)
		}
	}
}

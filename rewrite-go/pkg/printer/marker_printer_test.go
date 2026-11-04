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
package printer_test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// markedFunc parses a file whose only declaration is `func f() {}` and puts
// the marker on it.
func markedFunc(t *testing.T, marker java.Marker) *golang.CompilationUnit {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("f.go", "package main\n\nfunc f() {}\n")
	require.NoError(t, err)
	fn := cu.Statements[0].Element.(*java.MethodDeclaration)
	marked := *cu
	marked.Statements = []java.RightPadded[java.Statement]{{
		Element: fn.WithMarkers(java.AddMarker(fn.Markers, marker)),
		After:   cu.Statements[0].After,
	}}
	return &marked
}

// PrintOutputCapture.MarkerPrinter.FENCED on the Java side opens its fence
// after the prefix, so that the fence encloses the syntax alone.
func TestFencedMarkerPrinterOpensAfterThePrefix(t *testing.T) {
	found := java.NewSearchResult("")
	fence := "{{" + found.Ident.String() + "}}"

	printed := printer.PrintWithMarkers(markedFunc(t, found), printer.FencedMarkerPrinter)

	assert.Equal(t, "package main\n\n"+fence+"func f() {}"+fence+"\n", printed)
}

// Java prints a Markup's message, and its detail only when asked to be verbose.
func TestDefaultMarkerPrinterPrintsTheMessageOfAMarkup(t *testing.T) {
	warn := java.NewMarkup(java.MarkupWarnLevel, "careful", "detail")

	printed := printer.PrintWithMarkers(markedFunc(t, warn), printer.DefaultMarkerPrinter)

	assert.Equal(t, "package main\n\n/*~~(careful)~~>*/func f() {}\n", printed)
}

func TestPrintsPartOfAGoModOrGoSum(t *testing.T) {
	gm, err := parser.ParseGoModFile("go.mod", "module example.com/m\n\nrequire (\n\texample.com/a v1.0.0 // indirect\n)\n")
	require.NoError(t, err)
	module := gm.Statements[0].Element.(*golang.GoModDirective)
	block := gm.Statements[1].Element.(*golang.GoModBlock)

	assert.Equal(t, "module example.com/m", printer.Print(module))
	assert.Equal(t, " example.com/m", printer.Print(module.Values[0]))
	assert.Equal(t, "\nrequire (\n\texample.com/a v1.0.0 // indirect\n)", printer.Print(block))
	assert.Equal(t, "\n\texample.com/a v1.0.0", printer.Print(block.Entries[0].Element))

	gs, err := parser.ParseGoSumFile("go.sum", "example.com/a v1.0.0 h1:abc=\nexample.com/a v1.0.0/go.mod h1:def=\n")
	require.NoError(t, err)
	assert.Equal(t, "example.com/a v1.0.0/go.mod h1:def=", printer.Print(gs.Lines[1].Element))
}

const ancestors = `package main

type T struct {
	X int ` + "`json:\"x\" yaml:\"y\"`" + `
}

func (t *T) M() int {
	if v := 1; v > 0 {
		return v
	}
	switch w := 2; w {
	}
	return 0
}
`

// A struct field knows its annotations are a tag without being told it sits
// in a struct, so it prints the same with or without a cursor.
func TestStructFieldPrintsItsTagWithoutACursor(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("ancestors.go", ancestors)
	require.NoError(t, err)
	body := cu.Statements[0].Element.(*golang.TypeDecl).Definition.(*golang.StructType).Body

	assert.Equal(t, "\n\tX int `json:\"x\" yaml:\"y\"`", printer.Print(body.Statements[0].Element))
}

// The receiver of a method and the init of an if or switch are held by the
// parent of the node that prints them.
func TestPrintWithCursorTakesFromTheParent(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("ancestors.go", ancestors)
	require.NoError(t, err)
	method := cu.Statements[1].Element.(*golang.MethodDeclaration)
	withIf := method.Declaration.Body.Statements[0].Element.(*golang.StatementWithInit)
	withSwitch := method.Declaration.Body.Statements[1].Element.(*golang.StatementWithInit)

	printUnder := func(parent, tree java.Tree) string {
		return printer.PrintWithCursor(tree, visitor.NewCursor(nil, parent), nil)
	}

	assert.Contains(t, printUnder(method, method.Declaration), "\n\nfunc (t *T) M() int {")
	assert.Equal(t, "\n\tif v := 1; v > 0 {\n\t\treturn v\n\t}", printUnder(withIf, withIf.Statement))
	assert.Equal(t, "\n\tswitch w := 2; w {\n\t}", printUnder(withSwitch, withSwitch.Statement))

	// with no cursor a node prints only what it holds itself
	assert.Equal(t, "if v > 0 {\n\t\treturn v\n\t}", printer.Print(withIf.Statement))
}

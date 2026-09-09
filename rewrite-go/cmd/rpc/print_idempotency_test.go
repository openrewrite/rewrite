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

package main

import (
	"encoding/json"
	"path/filepath"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// The printer reproduces every source these tests use, so a mismatch has to be
// induced to exercise the reporting path at all.
func manglePrinterFor(t *testing.T, victim string) {
	t.Helper()
	original := printGoCompilationUnit
	printGoCompilationUnit = func(cu *golang.CompilationUnit) string {
		out := original(cu)
		if filepath.Base(cu.SourcePath) == victim {
			return strings.Replace(out, "\tsecond := 2\n", "", 1)
		}
		return out
	}
	t.Cleanup(func() { printGoCompilationUnit = original })
}

const idempotencySource = `package p

func %s() {
	first := 1
	second := 2
	_, _ = first, second
}
`

func parseTwoSiblings(t *testing.T, s *server, options map[string]any) map[string]any {
	t.Helper()
	dir := t.TempDir()
	for _, f := range []struct{ name, fn string }{{"a.go", "a"}, {"b.go", "b"}} {
		writeFile(t, filepath.Join(dir, f.name), strings.Replace(idempotencySource, "%s", f.fn, 1))
	}
	relativeTo := dir
	params, err := json.Marshal(parseRequest{
		Inputs:     []parseInput{{Path: filepath.Join(dir, "a.go")}, {Path: filepath.Join(dir, "b.go")}},
		RelativeTo: &relativeTo,
		Options:    options,
	})
	require.NoError(t, err)

	res, rpcErr := s.handleParse(params)
	require.Nil(t, rpcErr)
	ids, ok := res.([]string)
	require.True(t, ok)
	require.Len(t, ids, 2)

	byName := map[string]any{}
	for i, name := range []string{"a.go", "b.go"} {
		byName[name] = s.localObjects[ids[i]]
	}
	return byName
}

func TestHandleParseReportsPrintMismatchPerFile(t *testing.T) {
	s, _ := newTestServer(t)
	manglePrinterFor(t, "a.go")

	byName := parseTwoSiblings(t, s, nil)

	pe, ok := byName["a.go"].(*java.ParseError)
	require.Truef(t, ok, "a.go should be a ParseError, got %T", byName["a.go"])
	assert.Equal(t, "a.go", pe.SourcePath)

	marker, ok := pe.Markers.Entries[0].(java.ParseExceptionResult)
	require.True(t, ok)
	assert.Contains(t, marker.Message, "a.go is not print idempotent.")
	assert.Contains(t, marker.Message, "--- a/a.go")
	assert.Contains(t, marker.Message, "-\tsecond := 2")

	// The sibling shares a package and a ParsePackage call; a print
	// mismatch in one file must not turn the other into an error.
	_, ok = byName["b.go"].(*golang.CompilationUnit)
	assert.Truef(t, ok, "b.go should still be a CompilationUnit, got %T", byName["b.go"])
}

func TestHandleParseChecksPrintByDefault(t *testing.T) {
	s, _ := newTestServer(t)
	manglePrinterFor(t, "a.go")

	byName := parseTwoSiblings(t, s, map[string]any{})

	assert.IsType(t, &java.ParseError{}, byName["a.go"])
}

func TestHandleParseSkipsPrintCheckWhenDisabled(t *testing.T) {
	s, _ := newTestServer(t)
	manglePrinterFor(t, "a.go")

	byName := parseTwoSiblings(t, s, map[string]any{requirePrintEqualsInputKey: "false"})

	assert.IsType(t, &golang.CompilationUnit{}, byName["a.go"])
}

func TestRequirePrintEqualsInputDefaultsOn(t *testing.T) {
	assert.True(t, requirePrintEqualsInput(nil))
	assert.True(t, requirePrintEqualsInput(map[string]any{}))
	assert.True(t, requirePrintEqualsInput(map[string]any{requirePrintEqualsInputKey: "true"}))
	assert.True(t, requirePrintEqualsInput(map[string]any{requirePrintEqualsInputKey: true}))
	assert.True(t, requirePrintEqualsInput(map[string]any{requirePrintEqualsInputKey: "nonsense"}))
	assert.False(t, requirePrintEqualsInput(map[string]any{requirePrintEqualsInputKey: "false"}))
	assert.False(t, requirePrintEqualsInput(map[string]any{requirePrintEqualsInputKey: false}))
}

func parseProjectItems(t *testing.T, s *server, dir string, options map[string]any) map[string]parseProjectResponseItem {
	t.Helper()
	relativeTo := dir
	params, err := json.Marshal(parseProjectRequest{ProjectPath: dir, RelativeTo: &relativeTo, Options: options})
	require.NoError(t, err)

	res, rpcErr := s.handleParseProject(params)
	require.Nil(t, rpcErr)
	items, ok := res.([]parseProjectResponseItem)
	require.True(t, ok)

	byPath := map[string]parseProjectResponseItem{}
	for _, item := range items {
		byPath[filepath.ToSlash(item.SourcePath)] = item
	}
	return byPath
}

func TestParseProjectReportsPrintMismatchPerFile(t *testing.T) {
	s, _ := newTestServer(t)
	manglePrinterFor(t, "a.go")

	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "go.mod"), "module example.com/m\n\ngo 1.22\n")
	writeFile(t, filepath.Join(dir, "a.go"), strings.Replace(idempotencySource, "%s", "a", 1))
	writeFile(t, filepath.Join(dir, "b.go"), strings.Replace(idempotencySource, "%s", "b", 1))

	byPath := parseProjectItems(t, s, dir, nil)

	require.Contains(t, byPath, "a.go")
	assert.Equal(t, "org.openrewrite.tree.ParseError", byPath["a.go"].SourceFileType)
	pe, ok := s.localObjects[byPath["a.go"].ID].(*java.ParseError)
	require.Truef(t, ok, "expected a ParseError object, got %T", s.localObjects[byPath["a.go"].ID])
	assert.Equal(t, idempotencySourceFor("a"), pe.Text)

	require.Contains(t, byPath, "b.go")
	assert.Equal(t, "org.openrewrite.golang.tree.Go$CompilationUnit", byPath["b.go"].SourceFileType)
}

func TestParseProjectSkipsPrintCheckWhenDisabled(t *testing.T) {
	s, _ := newTestServer(t)
	manglePrinterFor(t, "a.go")

	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "go.mod"), "module example.com/m\n\ngo 1.22\n")
	writeFile(t, filepath.Join(dir, "a.go"), strings.Replace(idempotencySource, "%s", "a", 1))

	byPath := parseProjectItems(t, s, dir, map[string]any{requirePrintEqualsInputKey: "false"})

	assert.Equal(t, "org.openrewrite.golang.tree.Go$CompilationUnit", byPath["a.go"].SourceFileType)
}

func TestParseProjectEmitsParseErrorForUnparseableFile(t *testing.T) {
	s, _ := newTestServer(t)

	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "go.mod"), "module example.com/m\n\ngo 1.22\n")
	writeFile(t, filepath.Join(dir, "broken.go"), "package p\n\nfunc f( {\n")

	byPath := parseProjectItems(t, s, dir, nil)

	require.Contains(t, byPath, "broken.go")
	assert.Equal(t, "org.openrewrite.tree.ParseError", byPath["broken.go"].SourceFileType)
	pe, ok := s.localObjects[byPath["broken.go"].ID].(*java.ParseError)
	require.True(t, ok)
	assert.Equal(t, "package p\n\nfunc f( {\n", pe.Text)
}

func TestParseProjectAttributesSyntaxErrorToTheOffendingFile(t *testing.T) {
	s, _ := newTestServer(t)

	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "go.mod"), "module example.com/m\n\ngo 1.22\n")
	writeFile(t, filepath.Join(dir, "broken.go"), "package p\n\nfunc Broken( {\n")
	writeFile(t, filepath.Join(dir, "innocent.go"), "package p\n\nfunc Innocent() {}\n")

	byPath := parseProjectItems(t, s, dir, nil)

	// One file's syntax error stops the whole package, so neither gets a
	// compilation unit and both have to be accounted for.
	for _, name := range []string{"broken.go", "innocent.go"} {
		require.Containsf(t, byPath, name, "every file in the package must be accounted for")
		assert.Equal(t, "org.openrewrite.tree.ParseError", byPath[name].SourceFileType)
	}

	assert.Contains(t, messageOf(t, s, byPath["broken.go"].ID), "parse broken.go:")

	innocent := messageOf(t, s, byPath["innocent.go"].ID)
	assert.Contains(t, innocent, "package not parsed")
	assert.Contains(t, innocent, "broken.go")
	assert.NotContains(t, innocent, "parse innocent.go:")
}

func messageOf(t *testing.T, s *server, id string) string {
	t.Helper()
	pe, ok := s.localObjects[id].(*java.ParseError)
	require.Truef(t, ok, "expected a ParseError, got %T", s.localObjects[id])
	marker, ok := pe.Markers.Entries[0].(java.ParseExceptionResult)
	require.True(t, ok)
	return marker.Message
}

func idempotencySourceFor(fn string) string {
	return strings.Replace(idempotencySource, "%s", fn, 1)
}

func TestPrinterRoundTripsTrickyFiles(t *testing.T) {
	cases := map[string]string{
		"crlf":            "package m\r\n\r\nfunc f() {\r\n\tx := 1\r\n\t_ = x\r\n}\r\n",
		"bom":             "\ufeffpackage m\n",
		"lineDirective":   "package m\n\n//line foo.go:10\nfunc f() {}\n",
		"noTrailingNL":    "package m",
		"commentInParams": "package m\n\nfunc f(a /* x */ int) {}\n",
		"semicolons":      "package m\n\nfunc f() { a := 1; b := 2; _, _ = a, b }\n",
		"rawString":       "package m\n\nvar s = `a\\nb\n\tc`\n",
		"emptyDecls":      "package m\n\nimport ()\n\nvar ()\n\ntype ()\n",
		"cgoPreamble":     "package m\n\n/*\n#include <stdio.h>\n*/\nimport \"C\"\n",
		"trailingSpaces":  "package m   \n\nfunc f() {}   \n",
		"mixedIndent":     "package m\n\nfunc f() {\n  x := 1\n\t_ = x\n}\n",
		"unicodeIdent":    "package m\n\nvar élève = 1\n",
		"adjacentComment": "package m\n\n// a\n/* b */\n// c\nfunc f() {}\n",
	}

	s, _ := newTestServer(t)
	for name, src := range cases {
		t.Run(name, func(t *testing.T) {
			dir := t.TempDir()
			path := filepath.Join(dir, name+".go")
			writeFile(t, path, src)
			relativeTo := dir
			params, err := json.Marshal(parseRequest{Inputs: []parseInput{{Path: path}}, RelativeTo: &relativeTo})
			require.NoError(t, err)

			res, rpcErr := s.handleParse(params)
			require.Nil(t, rpcErr)
			ids := res.([]string)
			require.Len(t, ids, 1)

			cu, ok := s.localObjects[ids[0]].(*golang.CompilationUnit)
			require.Truef(t, ok, "expected a CompilationUnit, got %T", s.localObjects[ids[0]])
			assert.Equal(t, src, printer.Print(cu))
		})
	}
}

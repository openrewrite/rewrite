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
	"runtime"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TestParsePackageEmitsParseErrorForUnparseableFile verifies that a file
// which fails to parse arrives inline as a *java.ParseError (so the Java
// side represents it as a ParseError rather than a PlainText/Quark) while
// its well-formed siblings still parse to CompilationUnits.
func TestParsePackageEmitsParseErrorForUnparseableFile(t *testing.T) {
	sfs := parser.NewGoParser().ParsePackage([]parser.FileInput{
		{Path: "good.go", Content: "package main\n\nfunc main() {}\n"},
		{Path: "bad.go", Content: "package main\n\nfunc broken( {\n"},
	})

	if len(sfs) != 2 {
		t.Fatalf("expected one SourceFile per input, got %d", len(sfs))
	}

	cu, ok := sfs[0].(*golang.CompilationUnit)
	if !ok {
		t.Fatalf("expected good.go to be a CompilationUnit, got %T", sfs[0])
	}
	if cu.SourcePath != "good.go" {
		t.Errorf("expected CompilationUnit for good.go, got %q", cu.SourcePath)
	}

	pe, ok := sfs[1].(*java.ParseError)
	if !ok {
		t.Fatalf("expected bad.go to be a ParseError, got %T", sfs[1])
	}
	if pe.SourcePath != "bad.go" {
		t.Errorf("expected ParseError for bad.go, got %q", pe.SourcePath)
	}
	if pe.Text != "package main\n\nfunc broken( {\n" {
		t.Errorf("ParseError should preserve the original source verbatim, got %q", pe.Text)
	}
	if pe.Cause() == nil {
		t.Error("expected a recoverable cause on the ParseError")
	}
}

// TestParsePackageEmitsBuildExcludedFiles verifies that a file excluded by the
// build context (a `//go:build windows` file on a non-Windows host) is still
// parsed and emitted as a CompilationUnit — `go mod tidy` unions imports across
// every platform, so a module imported only by a platform-gated file (cobra's
// mousetrap is the canonical case) must remain visible. The excluded file is a
// full, lossless CompilationUnit (it round-trips), just type-checked only with
// the build-included set.
func TestParsePackageEmitsBuildExcludedFiles(t *testing.T) {
	sfs := parser.NewGoParser().ParsePackage([]parser.FileInput{
		{Path: "main.go", Content: "package main\n\nimport \"fmt\"\n\nfunc main() { fmt.Println() }\n"},
		{Path: "win.go", Content: "//go:build windows\n\npackage main\n\nimport _ \"example.com/winonly\"\n"},
	})

	if len(sfs) != 2 {
		t.Fatalf("expected both the included and the build-excluded file as CompilationUnits, got %d", len(sfs))
	}
	var win *golang.CompilationUnit
	for _, sf := range sfs {
		cu, ok := sf.(*golang.CompilationUnit)
		if !ok {
			t.Fatalf("expected a CompilationUnit, got %T", sf)
		}
		if cu.SourcePath == "win.go" {
			win = cu
		}
	}
	if win == nil {
		t.Fatal("the build-excluded win.go was not emitted as a CompilationUnit")
	}
	// Its windows-only import must be present (so the recipe can harvest it) and
	// the file must round-trip losslessly.
	if printed := printer.Print(win); !strings.Contains(printed, "example.com/winonly") {
		t.Errorf("build-excluded file's import was lost; round-trip:\n%s", printed)
	}
}

// TestParsePackageOmitsGoosExcludedFiles verifies an OS-suffix file for a
// different platform is omitted (not failed), mirroring the build context —
// even when that file would not parse on its own.
func TestParsePackageOmitsGoosExcludedFiles(t *testing.T) {
	// Pick an OS suffix that is never the host so the file is always excluded.
	otherOS := "windows_amd64"
	if runtime.GOOS == "windows" {
		otherOS = "linux_amd64"
	}
	sfs := parser.NewGoParser().ParsePackage([]parser.FileInput{
		{Path: "main.go", Content: "package main\n\nfunc main() {}\n"},
		{Path: "plat_" + otherOS + ".go", Content: "package main\n\nfunc broken( {\n"},
	})

	if len(sfs) != 1 {
		t.Fatalf("expected only the build-included file, got %d source files", len(sfs))
	}
	if _, ok := sfs[0].(*golang.CompilationUnit); !ok {
		t.Fatalf("expected a CompilationUnit, got %T", sfs[0])
	}
}

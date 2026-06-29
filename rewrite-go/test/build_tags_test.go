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
	"go/build"
	"sort"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
)

// parsedNames returns the file names included by ParsePackage for the
// given build context — the names of files that survived `//go:build`
// and filename-suffix constraint evaluation.
func parsedNames(t *testing.T, buildCtx build.Context, files []parser.FileInput) []string {
	t.Helper()
	p := parser.NewGoParserWithBuildContext(buildCtx)
	cus, err := p.ParsePackage(files)
	if err != nil {
		t.Fatalf("ParsePackage: %v", err)
	}
	out := make([]string, 0, len(cus))
	for _, cu := range cus {
		out = append(out, cu.SourcePath)
	}
	sort.Strings(out)
	return out
}

func ctx(goos, goarch string) build.Context {
	c := build.Default
	c.GOOS = goos
	c.GOARCH = goarch
	return c
}

// Case 1: //go:build linux — included on Linux, excluded on macOS.
func TestBuildTags_GoBuildLinuxOnly(t *testing.T) {
	files := []parser.FileInput{
		{Path: "main.go", Content: "package p\n\nfunc Main() {}\n"},
		{Path: "lin.go", Content: "//go:build linux\n\npackage p\n\nfunc Lin() {}\n"},
	}
	if got := parsedNames(t, ctx("linux", "amd64"), files); !equal(got, []string{"lin.go", "main.go"}) {
		t.Errorf("on linux: got %v, want [lin.go main.go]", got)
	}
	if got := parsedNames(t, ctx("darwin", "amd64"), files); !equal(got, []string{"main.go"}) {
		t.Errorf("on darwin: got %v, want [main.go]", got)
	}
}

// Case 2: filename suffix matching. `_linux.go`, `_amd64.go`, and
// `_linux_amd64.go` exclude themselves on the wrong platform.
func TestBuildTags_FilenameSuffix(t *testing.T) {
	files := []parser.FileInput{
		{Path: "main.go", Content: "package p\n\nfunc Main() {}\n"},
		{Path: "extra_linux.go", Content: "package p\n\nfunc Lin() {}\n"},
		{Path: "extra_amd64.go", Content: "package p\n\nfunc Amd() {}\n"},
		{Path: "extra_linux_amd64.go", Content: "package p\n\nfunc Both() {}\n"},
	}
	got := parsedNames(t, ctx("linux", "amd64"), files)
	want := []string{"extra_amd64.go", "extra_linux.go", "extra_linux_amd64.go", "main.go"}
	if !equal(got, want) {
		t.Errorf("on linux/amd64: got %v, want %v", got, want)
	}

	got = parsedNames(t, ctx("darwin", "arm64"), files)
	want = []string{"main.go"}
	if !equal(got, want) {
		t.Errorf("on darwin/arm64: got %v, want %v", got, want)
	}
}

// Case 3: combined constraints — `//go:build linux && amd64`.
func TestBuildTags_CombinedConstraint(t *testing.T) {
	files := []parser.FileInput{
		{Path: "main.go", Content: "package p\n\nfunc Main() {}\n"},
		{Path: "both.go", Content: "//go:build linux && amd64\n\npackage p\n\nfunc Both() {}\n"},
	}
	if got := parsedNames(t, ctx("linux", "amd64"), files); !equal(got, []string{"both.go", "main.go"}) {
		t.Errorf("linux/amd64: got %v", got)
	}
	if got := parsedNames(t, ctx("linux", "arm64"), files); !equal(got, []string{"main.go"}) {
		t.Errorf("linux/arm64: got %v", got)
	}
	if got := parsedNames(t, ctx("darwin", "amd64"), files); !equal(got, []string{"main.go"}) {
		t.Errorf("darwin/amd64: got %v", got)
	}
}

// Case 4: negated constraints — `//go:build !windows`.
func TestBuildTags_NegatedConstraint(t *testing.T) {
	files := []parser.FileInput{
		{Path: "main.go", Content: "package p\n\nfunc Main() {}\n"},
		{Path: "nowin.go", Content: "//go:build !windows\n\npackage p\n\nfunc NoWin() {}\n"},
	}
	if got := parsedNames(t, ctx("windows", "amd64"), files); !equal(got, []string{"main.go"}) {
		t.Errorf("on windows: got %v", got)
	}
	if got := parsedNames(t, ctx("linux", "amd64"), files); !equal(got, []string{"main.go", "nowin.go"}) {
		t.Errorf("on linux: got %v", got)
	}
}

// Case 5: legacy `// +build` syntax — still recognized.
func TestBuildTags_LegacyPlusBuild(t *testing.T) {
	files := []parser.FileInput{
		{Path: "main.go", Content: "package p\n\nfunc Main() {}\n"},
		{Path: "lin.go", Content: "// +build linux\n\npackage p\n\nfunc Lin() {}\n"},
	}
	if got := parsedNames(t, ctx("linux", "amd64"), files); !equal(got, []string{"lin.go", "main.go"}) {
		t.Errorf("on linux: got %v", got)
	}
	if got := parsedNames(t, ctx("darwin", "amd64"), files); !equal(got, []string{"main.go"}) {
		t.Errorf("on darwin: got %v", got)
	}
}

// Case 6: mixed filename + //go:build. Filename says linux, content
// constraint says amd64; the file is included only when BOTH match.
func TestBuildTags_MixedFilenameAndGoBuild(t *testing.T) {
	files := []parser.FileInput{
		{Path: "main.go", Content: "package p\n\nfunc Main() {}\n"},
		{Path: "x_linux.go", Content: "//go:build amd64\n\npackage p\n\nfunc Both() {}\n"},
	}
	if got := parsedNames(t, ctx("linux", "amd64"), files); !equal(got, []string{"main.go", "x_linux.go"}) {
		t.Errorf("linux/amd64: got %v", got)
	}
	if got := parsedNames(t, ctx("linux", "arm64"), files); !equal(got, []string{"main.go"}) {
		t.Errorf("linux/arm64 (filename matches, constraint does not): got %v", got)
	}
	if got := parsedNames(t, ctx("darwin", "amd64"), files); !equal(got, []string{"main.go"}) {
		t.Errorf("darwin/amd64 (constraint matches, filename does not): got %v", got)
	}
}

func equal(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

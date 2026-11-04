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

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// parsePackage parses files under a build context and returns the resulting
// CompilationUnits keyed by source path. Every input file is kept, so this
// map has one entry per input.
func parsePackage(t *testing.T, buildCtx build.Context, files []parser.FileInput) map[string]*golang.CompilationUnit {
	t.Helper()
	p := parser.NewGoParserWithBuildContext(buildCtx)
	cus, err := p.ParsePackage(files)
	require.NoError(t, err, "ParsePackage")
	byPath := make(map[string]*golang.CompilationUnit, len(cus))
	for _, cu := range cus {
		byPath[cu.SourcePath] = cu
	}
	return byPath
}

// parsedNames returns the file names ParsePackage produced, sorted. With
// cross-platform parsing no file is dropped, so this is every input.
func parsedNames(t *testing.T, buildCtx build.Context, files []parser.FileInput) []string {
	t.Helper()
	byPath := parsePackage(t, buildCtx, files)
	out := make([]string, 0, len(byPath))
	for path := range byPath {
		out = append(out, path)
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

func buildConstraint(t *testing.T, cu *golang.CompilationUnit) *golang.BuildConstraint {
	t.Helper()
	require.NotNil(t, cu, "compilation unit")
	return java.FindMarker[golang.BuildConstraint](cu.Markers)
}

// Every input is parsed regardless of build context; nothing is dropped.
func TestBuildTags_KeepsEveryFile(t *testing.T) {
	files := []parser.FileInput{
		{Path: "main.go", Content: "package p\n\nfunc Main() {}\n"},
		{Path: "lin.go", Content: "//go:build linux\n\npackage p\n\nfunc Lin() {}\n"},
		{Path: "win.go", Content: "//go:build windows\n\npackage p\n\nfunc Win() {}\n"},
	}
	want := []string{"lin.go", "main.go", "win.go"}
	assert.Equal(t, want, parsedNames(t, ctx("linux", "amd64"), files), "on linux")
	assert.Equal(t, want, parsedNames(t, ctx("darwin", "arm64"), files), "on darwin")
}

// A `//go:build` constraint is recorded verbatim on the file that declares it;
// an unconstrained file carries no marker.
func TestBuildTags_GoBuildRecorded(t *testing.T) {
	files := []parser.FileInput{
		{Path: "main.go", Content: "package p\n\nfunc Main() {}\n"},
		{Path: "lin.go", Content: "//go:build linux\n\npackage p\n\nfunc Lin() {}\n"},
	}
	byPath := parsePackage(t, ctx("linux", "amd64"), files)

	assert.Nil(t, buildConstraint(t, byPath["main.go"]), "unconstrained file")
	lin := buildConstraint(t, byPath["lin.go"])
	require.NotNil(t, lin, "constrained file")
	assert.Contains(t, lin.Constraint, "//go:build linux")
}

// Filename suffixes are captured as GOOS/GOARCH: `_linux.go`, `_amd64.go`,
// and `_linux_amd64.go`.
func TestBuildTags_FilenameSuffixRecorded(t *testing.T) {
	files := []parser.FileInput{
		{Path: "extra_linux.go", Content: "package p\n\nfunc Lin() {}\n"},
		{Path: "extra_amd64.go", Content: "package p\n\nfunc Amd() {}\n"},
		{Path: "extra_linux_amd64.go", Content: "package p\n\nfunc Both() {}\n"},
	}
	byPath := parsePackage(t, ctx("darwin", "arm64"), files)

	os := buildConstraint(t, byPath["extra_linux.go"])
	require.NotNil(t, os)
	assert.Equal(t, "linux", os.GOOS)
	assert.Empty(t, os.GOARCH)

	arch := buildConstraint(t, byPath["extra_amd64.go"])
	require.NotNil(t, arch)
	assert.Empty(t, arch.GOOS)
	assert.Equal(t, "amd64", arch.GOARCH)

	both := buildConstraint(t, byPath["extra_linux_amd64.go"])
	require.NotNil(t, both)
	assert.Equal(t, "linux", both.GOOS)
	assert.Equal(t, "amd64", both.GOARCH)
}

// Legacy `// +build` syntax is recognized and recorded like `//go:build`.
func TestBuildTags_LegacyPlusBuildRecorded(t *testing.T) {
	files := []parser.FileInput{
		{Path: "lin.go", Content: "// +build linux\n\npackage p\n\nfunc Lin() {}\n"},
	}
	byPath := parsePackage(t, ctx("darwin", "amd64"), files)
	lin := buildConstraint(t, byPath["lin.go"])
	require.NotNil(t, lin)
	assert.Contains(t, lin.Constraint, "+build linux")
}

// A file matching the build context is type-checked; a file the context
// excludes keeps its syntax but is left unattributed and marked accordingly.
func TestBuildTags_OnlyMatchingFilesAreTypeChecked(t *testing.T) {
	files := []parser.FileInput{
		{Path: "run_linux.go", Content: "package p\n\nfunc plat() int { return 1 }\n"},
		{Path: "run_windows.go", Content: "package p\n\nfunc plat() string { return \"\" }\n"},
	}
	byPath := parsePackage(t, ctx("linux", "amd64"), files)

	assert.Nil(t, java.FindMarker[golang.PartialTypeAttribution](byPath["run_linux.go"].Markers),
		"matching file is type-checked")
	assert.NotNil(t, java.FindMarker[golang.PartialTypeAttribution](byPath["run_windows.go"].Markers),
		"excluded file is marked as not type-checked")
}

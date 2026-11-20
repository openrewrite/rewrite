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

package parser

import (
	"go/build"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func linuxAmd64() build.Context {
	c := build.Default
	c.GOOS = "linux"
	c.GOARCH = "amd64"
	c.CgoEnabled = false
	return c
}

// A package holding a platform-neutral file, a filename-suffixed file for a
// foreign OS, and a `//go:build`-tagged file for the host OS. Every file is
// mapped to a CompilationUnit; none is dropped for not matching the context.
func crossPlatformPackage() []FileInput {
	return []FileInput{
		{Path: "neutral.go", Content: "package p\n\nfunc Neutral() {}\n"},
		{Path: "only_windows.go", Content: "package p\n\nfunc Win() {}\n"},
		{Path: "tagged.go", Content: "//go:build linux\n\npackage p\n\nfunc Lin() {}\n"},
	}
}

func sourcePaths(cus []*golang.CompilationUnit) map[string]*golang.CompilationUnit {
	byPath := make(map[string]*golang.CompilationUnit, len(cus))
	for _, cu := range cus {
		byPath[cu.SourcePath] = cu
	}
	return byPath
}

func TestParsePackageKeepsNonMatchingFiles(t *testing.T) {
	// given
	p := NewGoParserWithBuildContext(linuxAmd64())

	// when
	cus, err := p.ParsePackage(crossPlatformPackage())

	// then
	require.NoError(t, err)
	byPath := sourcePaths(cus)
	assert.Contains(t, byPath, "neutral.go")
	assert.Contains(t, byPath, "only_windows.go")
	assert.Contains(t, byPath, "tagged.go")
	assert.Len(t, cus, 3)
}

func TestParsePackageTagsConstrainedFiles(t *testing.T) {
	// given
	p := NewGoParserWithBuildContext(linuxAmd64())

	// when
	cus, err := p.ParsePackage(crossPlatformPackage())
	require.NoError(t, err)
	byPath := sourcePaths(cus)

	// then
	assert.Nil(t, java.FindMarker[golang.BuildConstraint](byPath["neutral.go"].Markers),
		"a platform-neutral file carries no BuildConstraint marker")

	win := java.FindMarker[golang.BuildConstraint](byPath["only_windows.go"].Markers)
	require.NotNil(t, win, "a filename-suffixed file carries a BuildConstraint marker")
	assert.Equal(t, "windows", win.GOOS)
	assert.Empty(t, win.GOARCH)

	tagged := java.FindMarker[golang.BuildConstraint](byPath["tagged.go"].Markers)
	require.NotNil(t, tagged, "a //go:build-tagged file carries a BuildConstraint marker")
	assert.Contains(t, tagged.Constraint, "//go:build linux")
}

// The primary/rest split: files matching the build context are type-checked;
// the rest keep their syntax but no attribution, so two files selected by
// mutually exclusive constraints never collide as redeclarations.
func TestParsePackageTypeChecksOnlyPrimaryContext(t *testing.T) {
	// given
	files := []FileInput{
		{Path: "plat_linux.go", Content: "package p\n\nfunc plat() int { return 1 }\n"},
		{Path: "plat_windows.go", Content: "package p\n\nfunc plat() string { return \"\" }\n"},
	}
	p := NewGoParserWithBuildContext(linuxAmd64())

	// when
	cus, err := p.ParsePackage(files)

	// then
	require.NoError(t, err)
	byPath := sourcePaths(cus)
	require.Len(t, cus, 2)

	assert.NotNil(t, firstMethodType(byPath["plat_linux.go"]),
		"the file in the primary context is type-checked")
	assert.Nil(t, firstMethodType(byPath["plat_windows.go"]),
		"the file outside the primary context is not type-checked")

	assert.NotNil(t, java.FindMarker[golang.PartialTypeAttribution](byPath["plat_windows.go"].Markers),
		"a non-type-checked file is marked PartialTypeAttribution")
}

func firstMethodType(cu *golang.CompilationUnit) *java.JavaTypeMethod {
	for _, rp := range cu.Statements {
		if md, ok := rp.Element.(*java.MethodDeclaration); ok {
			return md.MethodType
		}
	}
	return nil
}

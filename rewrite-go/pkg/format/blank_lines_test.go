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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
)

func TestBlankLinesVisitorLeavesTheVisitedTreeUntouched(t *testing.T) {
	// given a struct whose first field is set off from the brace by a blank line
	src := "package p\n\ntype T struct {\n\n\tA int\n}\n"
	cu, err := parser.NewGoParser().Parse("t.go", src)
	require.NoError(t, err)

	// when
	out := NewBlankLinesVisitor(nil).Visit(cu, nil)

	// then
	assert.Equal(t, src, printer.Print(cu))
	assert.Equal(t, "package p\n\ntype T struct {\n\tA int\n}\n", printer.Print(out))
}

func TestBlankLinesVisitorDropsTheBlankLinesAFileOpensWith(t *testing.T) {
	// given a file that opens with blank lines
	cu, err := parser.NewGoParser().Parse("t.go", "\n\n\n// Package p is a package.\npackage p\n")
	require.NoError(t, err)

	// when
	out := NewBlankLinesVisitor(nil).Visit(cu, nil)

	// then
	assert.Equal(t, "// Package p is a package.\npackage p\n", printer.Print(out))
}

func TestBlankLinesVisitorCapsTheBlankLinesAfterAComment(t *testing.T) {
	// given runs of blank lines after comments, at the top level and in a block
	cu, err := parser.NewGoParser().Parse("t.go", "// Copyright.\n\n\n\n// Package p is a package.\npackage p\n\nfunc F() {\n\tx := 1\n\t// a comment\n\n\n\n\t_ = x\n}\n")
	require.NoError(t, err)

	// when
	out := NewBlankLinesVisitor(nil).Visit(cu, nil)

	// then
	assert.Equal(t, "// Copyright.\n\n// Package p is a package.\npackage p\n\nfunc F() {\n\tx := 1\n\t// a comment\n\n\t_ = x\n}\n", printer.Print(out))
}

func TestBlankLinesVisitorCountsALineOfSpacesOrTabsAsBlank(t *testing.T) {
	// given blank lines holding an indent, against a struct's brace, between
	// statements and after a comment
	cu, err := parser.NewGoParser().Parse("t.go", "package p\n\ntype T struct {\n\t\n\tA int\n}\n\nfunc F() {\n\tx := 1\n\t\n\t\n\t\n\t// a comment\n \n\t\n\t_ = x\n}\n")
	require.NoError(t, err)

	// when
	out := NewBlankLinesVisitor(nil).Visit(cu, nil)

	// then
	assert.Equal(t, "package p\n\ntype T struct {\n\tA int\n}\n\nfunc F() {\n\tx := 1\n\n\t// a comment\n\n\t_ = x\n}\n", printer.Print(out))
}

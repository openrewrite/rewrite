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

package golang

import (
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// GoMod is the lossless LST for a Go `go.mod` file. It mirrors
// org.openrewrite.golang.tree.GoMod on the Java side.
//
// Unlike CompilationUnit, GoMod is NOT a J node: go.mod tokens are not
// Java expressions, so the tree carries its own minimal node set. Like
// java.ParseError, it satisfies only Tree and is special-cased ahead of
// the J-dispatch in the RPC sender/receiver.
//
// Every byte of the original file is recoverable: all whitespace and
// comments live inside java.Space prefixes (leading) and the After of
// each statement (same-line trailing, up to and including the newline).
// Re-printing a parsed GoMod yields the original bytes verbatim.
//
// Structured module metadata (module path, requires, replaces, …) is not
// duplicated on the tree; it is attached as a GoResolutionResult marker.
type GoMod struct {
	Ident            uuid.UUID
	Prefix           java.Space
	Markers          java.Markers
	SourcePath       string
	Charset          string
	CharsetBomMarked bool
	Statements       []java.RightPadded[GoModStatement]
	Eof              java.Space
}

func (*GoMod) IsTree()       {}
func (*GoMod) IsSourceFile() {}

func (n *GoMod) GetSourcePath() string { return n.SourcePath }

func (n *GoMod) WithPrefix(prefix java.Space) *GoMod {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *GoMod) WithMarkers(markers java.Markers) *GoMod {
	c := *n
	c.Markers = markers
	return &c
}

func (n *GoMod) WithStatements(statements []java.RightPadded[GoModStatement]) *GoMod {
	c := *n
	c.Statements = statements
	return &c
}

func (n *GoMod) WithEof(eof java.Space) *GoMod {
	c := *n
	c.Eof = eof
	return &c
}

// GoModStatement is a top-level go.mod statement or a single line inside a
// factored block. The two concrete forms are GoModDirective (a single
// line of tokens) and GoModBlock (a `keyword ( … )` factored block).
type GoModStatement interface {
	java.Tree
	isGoModStatement()
}

// GoModDirective is one line of tokens, e.g. `module example.com/foo`,
// `go 1.21`, or a single-line `require example.com/x v1.2.3`.
//
// It is also used for the entry lines inside a GoModBlock, in which case
// Keyword is empty (the block already carries the verb).
type GoModDirective struct {
	Ident   uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	// Keyword is the directive verb (module, go, toolchain, require,
	// replace, exclude, retract, godebug, tool, ignore, or any unknown
	// future verb). Empty for block entry lines.
	Keyword string
	// Values are the tokens following the keyword, each carrying its own
	// leading whitespace. Operators (`=>`) and bracketed retract ranges
	// (`[v1.0.0, v1.1.0]`) are individual values.
	Values []*GoModValue
}

func (*GoModDirective) IsTree()           {}
func (*GoModDirective) isGoModStatement() {}

func (n *GoModDirective) WithPrefix(prefix java.Space) *GoModDirective {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *GoModDirective) WithMarkers(markers java.Markers) *GoModDirective {
	c := *n
	c.Markers = markers
	return &c
}

func (n *GoModDirective) WithValues(values []*GoModValue) *GoModDirective {
	c := *n
	c.Values = values
	return &c
}

// GoModBlock is a factored block, e.g.
//
//	require (
//		example.com/a v1.0.0
//		example.com/b v2.0.0 // indirect
//	)
type GoModBlock struct {
	Ident        uuid.UUID
	Prefix       java.Space
	Markers      java.Markers
	Keyword      string
	BeforeLParen java.Space
	Entries      []java.RightPadded[GoModStatement]
	BeforeRParen java.Space
}

func (*GoModBlock) IsTree()           {}
func (*GoModBlock) isGoModStatement() {}

func (n *GoModBlock) WithPrefix(prefix java.Space) *GoModBlock {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *GoModBlock) WithMarkers(markers java.Markers) *GoModBlock {
	c := *n
	c.Markers = markers
	return &c
}

func (n *GoModBlock) WithEntries(entries []java.RightPadded[GoModStatement]) *GoModBlock {
	c := *n
	c.Entries = entries
	return &c
}

// GoModValue is a single token within a directive line: a module path,
// version, local path, operator, or bracketed range expression. The raw
// text is preserved verbatim (including any quoting).
type GoModValue struct {
	Ident   uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Text    string
}

func (*GoModValue) IsTree() {}

func (n *GoModValue) WithPrefix(prefix java.Space) *GoModValue {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *GoModValue) WithMarkers(markers java.Markers) *GoModValue {
	c := *n
	c.Markers = markers
	return &c
}

func (n *GoModValue) WithText(text string) *GoModValue {
	c := *n
	c.Text = text
	return &c
}

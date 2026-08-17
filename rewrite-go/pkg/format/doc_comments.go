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
	"go/doc/comment"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// DocCommentVisitor rewrites doc comments into the canonical form gofmt
// produces, delegating the text to go/doc/comment — the parser and printer
// gofmt itself uses, so lists, code blocks, headings and links all follow.
// Only an unindented `//` run abutting the token it documents qualifies, which
// is gofmt's rule; every other comment keeps its text.
type DocCommentVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
}

// NewDocCommentVisitor returns a visitor configured with the given stopAfter
// bound. Pass nil to format the entire visited tree.
func NewDocCommentVisitor(stopAfter java.Tree) *DocCommentVisitor {
	return visitor.Init(&DocCommentVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
	})
}

func (v *DocCommentVisitor) Visit(t java.Tree, p any) java.Tree {
	if v.shouldHalt() {
		return t
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

// VisitCompilationUnit holds two positions out of the pass. Comments trailing
// the last declaration document nothing, and gofmt leaves an import
// declaration's doc comment as written — in a file that imports "C" it is the
// cgo preamble, and its spacing is significant.
func (v *DocCommentVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	// The file's own prefix is the one place a doc comment can begin at column
	// one without a line break ahead of it.
	if start, ok := docCommentRun(cu.Prefix, true); ok {
		if formatted, ok := canonicalDocComment(cu.Prefix.Comments[start:]); ok {
			prefix := cu.Prefix
			prefix.Comments = append(append([]java.Comment(nil), prefix.Comments[:start]...), formatted...)
			cu = cu.WithPrefix(prefix)
		}
	}
	out, ok := v.GoVisitor.VisitCompilationUnit(cu, p).(*golang.CompilationUnit)
	if !ok {
		return cu
	}
	return out.WithEOF(cu.EOF).WithImports(cu.Imports)
}

func (v *DocCommentVisitor) VisitSpace(s java.Space, p any) java.Space {
	start, ok := docCommentRun(s, false)
	if !ok {
		return s
	}
	formatted, ok := canonicalDocComment(s.Comments[start:])
	if !ok {
		return s
	}
	s.Comments = append(append([]java.Comment(nil), s.Comments[:start]...), formatted...)
	return s
}

// docCommentRun reports the index at which the run documenting whatever
// follows s begins. The run reaches the next token with nothing but a line
// break between, and every line in it starts at column one.
func docCommentRun(s java.Space, atFileStart bool) (int, bool) {
	last := len(s.Comments) - 1
	if last < 0 || s.Comments[last].Suffix != "\n" {
		return 0, false
	}
	for start := last; ; start-- {
		if !startsLine(s, start, atFileStart) {
			return 0, false
		}
		// A run reaching the start of s, or preceded by anything other than a
		// bare line break, is as far back as this doc comment goes.
		if start == 0 || s.Comments[start-1].Suffix != "\n" {
			return start, true
		}
	}
}

// startsLine reports whether the i-th comment of s begins a line: the text
// ahead of it ends in a line break, or there is no text ahead of it and s sits
// at the start of the file. A comment trailing code on the same line documents
// nothing, and dropping it would join two tokens.
func startsLine(s java.Space, i int, atFileStart bool) bool {
	preceding := s.Whitespace
	if i > 0 {
		preceding = s.Comments[i-1].Suffix
	}
	if preceding == "" {
		return atFileStart && i == 0
	}
	return strings.HasSuffix(preceding, "\n")
}

// canonicalDocComment reformats one doc comment run, mirroring
// go/printer.formatDocComment. Directives keep their own text and follow the
// prose, separated from it by a blank line. Anything that isn't a run of `//`
// lines is reported as unformattable, since the caller should leave it be.
func canonicalDocComment(run []java.Comment) ([]java.Comment, bool) {
	var prose strings.Builder
	var directives []java.Comment
	for _, c := range run {
		if c.Kind != java.LineComment {
			return nil, false
		}
		body, found := strings.CutPrefix(c.Text, "//")
		if !found {
			return nil, false
		}
		if isDirective(body) {
			directives = append(directives, c)
			continue
		}
		prose.WriteString(strings.TrimPrefix(body, " "))
		prose.WriteString("\n")
	}
	if prose.Len() == 0 {
		return nil, false
	}

	var parser comment.Parser
	var printer comment.Printer
	text := string(printer.Comment(parser.Parse(prose.String())))

	out := make([]java.Comment, 0, len(run)+len(directives)+1)
	for text != "" {
		var line string
		line, text, _ = strings.Cut(text, "\n")
		switch {
		case line == "":
			line = "//"
		case strings.HasPrefix(line, "\t"):
			line = "//" + line
		default:
			line = "// " + line
		}
		out = append(out, docLine(run[0], line))
	}
	if len(directives) > 0 {
		out = append(out, docLine(run[0], "//"))
		out = append(out, directives...)
	}
	if len(out) == 0 {
		// A doc comment carrying no text renders as nothing, and gofmt drops
		// it, keeping the whitespace that ran up to it.
		return nil, true
	}

	// Every line but the last is followed by a bare line break; the last one
	// keeps whatever separated the run from the token it documents.
	for i := range out {
		out[i].Suffix = "\n"
	}
	out[len(out)-1].Suffix = run[len(run)-1].Suffix
	return out, true
}

// docLine builds one comment line, taking everything but the text from an
// existing line of the same run.
func docLine(like java.Comment, text string) java.Comment {
	like.Text = text
	like.Multiline = false
	return like
}

// isDirective reports whether a comment body (with `//` already removed) is a
// compiler directive such as `go:build`. Mirrors go/ast.isDirective.
func isDirective(c string) bool {
	if strings.HasPrefix(c, "line ") || strings.HasPrefix(c, "extern ") || strings.HasPrefix(c, "export ") {
		return true
	}
	colon := strings.Index(c, ":")
	if colon <= 0 || colon+1 >= len(c) {
		return false
	}
	for i := 0; i <= colon+1; i++ {
		if i == colon {
			continue
		}
		b := c[i]
		if !('a' <= b && b <= 'z' || '0' <= b && b <= '9') {
			return false
		}
	}
	return true
}

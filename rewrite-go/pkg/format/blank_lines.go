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
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// BlankLinesVisitor enforces gofmt's blank-line rules:
//
//   - Any run of >1 blank line collapses to one (anywhere in the
//     compilation unit — gofmt's `1 blank line max` rule applies
//     uniformly inside blocks AND between top-level decls).
//   - A declaration list — the fields of a struct, the methods of an
//     interface — sits flush against its braces, while a statement
//     block keeps a blank line written against either of its own. A
//     comment against a brace keeps the blank line separating it either
//     way, since the separation reads as the comment's own.
//
// The flush rule operates on the first statement's own Prefix, since the
// parser attaches inter-statement whitespace to the outermost element.
type BlankLinesVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
}

func NewBlankLinesVisitor(stopAfter java.Tree) *BlankLinesVisitor {
	return visitor.Init(&BlankLinesVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
	})
}

func (v *BlankLinesVisitor) Visit(t java.Tree, p any) java.Tree {
	if v.shouldHalt() {
		return t
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

// VisitCompilationUnit drops the blank lines a file opens with, as gofmt does.
func (v *BlankLinesVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	cu = cu.WithPrefix(adjustSpace(cu.Prefix, func(ws string) string {
		return ws[strings.LastIndex(ws, "\n")+1:]
	}))
	return v.GoVisitor.VisitCompilationUnit(cu, p)
}

// VisitSpace caps each run of blank lines in s, ahead of its comments and
// after each of them, at one.
func (v *BlankLinesVisitor) VisitSpace(s java.Space, p any) java.Space {
	whitespace := capInternalBlankLines(s.Whitespace(), 1)
	var comments []java.Comment
	for i, c := range s.Comments() {
		suffix := capInternalBlankLines(c.Suffix, 1)
		if suffix == c.Suffix {
			continue
		}
		if comments == nil {
			comments = append([]java.Comment(nil), s.Comments()...)
		}
		comments[i].Suffix = suffix
	}
	if comments == nil {
		if whitespace == s.Whitespace() {
			return s
		}
		comments = s.Comments()
	}
	return java.MakeSpace(comments, whitespace)
}

func (v *BlankLinesVisitor) VisitBlock(block *java.Block, p any) java.J {
	out := v.GoVisitor.VisitBlock(block, p).(*java.Block)
	if !v.isDeclarationList() {
		return out
	}
	if len(out.End.Comments()) == 0 {
		out = out.WithEnd(adjustSpace(out.End, stripLeadingBlankLines))
	}

	if len(out.Statements) > 0 && out.Statements[0].Element != nil &&
		len(getPrefix(out.Statements[0].Element).Comments()) == 0 {
		first := out.Statements[0].Element
		if updated, ok := transformPrefix(first, stripLeadingBlankLinesSpace).(java.Statement); ok && updated != first {
			statements := append([]java.RightPadded[java.Statement](nil), out.Statements...)
			statements[0].Element = updated
			out = out.WithStatements(statements)
		}
	}
	return out
}

// isDeclarationList reports whether the block being visited holds declarations
// rather than statements, which is what decides whether it sits flush against
// its braces.
func (v *BlankLinesVisitor) isDeclarationList() bool {
	parent := v.Cursor().Parent()
	if parent == nil {
		return false
	}
	switch parent.Value().(type) {
	case *golang.StructType, *golang.InterfaceType:
		return true
	}
	return false
}

func stripLeadingBlankLinesSpace(s java.Space) java.Space {
	return java.MakeSpace(s.Comments(), stripLeadingBlankLines(s.Whitespace()))
}

func adjustSpace(s java.Space, f func(string) string) java.Space {
	updated := f(s.Whitespace())
	if updated == s.Whitespace() {
		return s
	}
	return java.MakeSpace(s.Comments(), updated)
}

// stripLeadingBlankLines collapses the first run of newlines in ws to a single
// one, preserving both the trailing indent and anything ahead of that run,
// which is the previous line's trailing whitespace.
func stripLeadingBlankLines(ws string) string {
	start := strings.IndexByte(ws, '\n')
	if start < 0 {
		return ws
	}
	end, breaks := lineBreakRun(ws, start)
	if breaks < 2 {
		return ws
	}
	return ws[:start] + "\n" + ws[end:]
}

// capInternalBlankLines walks ws and collapses any internal run of
// 3+ newlines to exactly 2 (i.e., one blank line). Leaves runs of
// 0–2 newlines untouched.
func capInternalBlankLines(ws string, max int) string {
	allowed := max + 1
	if strings.Count(ws, "\n") <= allowed {
		return ws
	}
	var b strings.Builder
	b.Grow(len(ws))
	i := 0
	for i < len(ws) {
		if ws[i] != '\n' {
			b.WriteByte(ws[i])
			i++
			continue
		}
		end, breaks := lineBreakRun(ws, i)
		if breaks > allowed {
			b.WriteString(strings.Repeat("\n", allowed))
		} else {
			b.WriteString(ws[i:end])
		}
		i = end
	}
	return b.String()
}

// lineBreakRun measures the run of line breaks starting at ws[start], and
// returns where it ends and how many breaks it holds. A line holding only
// spaces or tabs is blank, so the run goes on through it.
func lineBreakRun(ws string, start int) (end, breaks int) {
	for i := start; i < len(ws); i++ {
		if ws[i] == '\n' {
			end, breaks = i+1, breaks+1
		} else if ws[i] != ' ' && ws[i] != '\t' {
			break
		}
	}
	return end, breaks
}

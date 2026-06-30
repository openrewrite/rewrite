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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// BlankLinesVisitor enforces gofmt's blank-line rules:
//
//   - Any run of >1 blank line collapses to one (anywhere in the
//     compilation unit — gofmt's `1 blank line max` rule applies
//     uniformly inside blocks AND between top-level decls).
//   - Block.End loses any leading blank line so the closing brace
//     sits flush against the last statement.
//   - The first statement of a block has no blank line above it.
//
// The first-statement rule operates on the statement node's own Prefix —
// the parser attaches inter-statement whitespace to the outermost element.
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

func (v *BlankLinesVisitor) VisitSpace(s java.Space, p any) java.Space {
	if !strings.Contains(s.Whitespace, "\n\n\n") {
		return s
	}
	s.Whitespace = capInternalBlankLines(s.Whitespace, 1)
	return s
}

func (v *BlankLinesVisitor) VisitBlock(block *java.Block, p any) java.J {
	out := v.GoVisitor.VisitBlock(block, p).(*java.Block)
	out = out.WithEnd(adjustSpace(out.End, stripLeadingBlankLines))

	// Strip any leading blank line above the first statement, whose Prefix
	// carries the inter-statement whitespace.
	if len(out.Statements) > 0 && out.Statements[0].Element != nil {
		first := out.Statements[0]
		if updated, ok := transformPrefix(first.Element, stripLeadingBlankLinesSpace).(java.Statement); ok {
			first.Element = updated
			out.Statements[0] = first
		}
	}
	return out
}

func stripLeadingBlankLinesSpace(s java.Space) java.Space {
	s.Whitespace = stripLeadingBlankLines(s.Whitespace)
	return s
}

func adjustSpace(s java.Space, f func(string) string) java.Space {
	updated := f(s.Whitespace)
	if updated == s.Whitespace {
		return s
	}
	s.Whitespace = updated
	return s
}

// stripLeadingBlankLines collapses any "\n\n+" run at the start to a
// single "\n", preserving any trailing indent.
func stripLeadingBlankLines(ws string) string {
	if !strings.HasPrefix(ws, "\n\n") {
		return ws
	}
	for strings.HasPrefix(ws, "\n\n") {
		ws = ws[1:]
	}
	return ws
}

// capInternalBlankLines walks ws and collapses any internal run of
// 3+ newlines to exactly 2 (i.e., one blank line). Leaves runs of
// 0–2 newlines untouched.
func capInternalBlankLines(ws string, max int) string {
	allowed := max + 1
	var b strings.Builder
	b.Grow(len(ws))
	i := 0
	for i < len(ws) {
		if ws[i] != '\n' {
			b.WriteByte(ws[i])
			i++
			continue
		}
		// Count run length.
		j := i
		for j < len(ws) && ws[j] == '\n' {
			j++
		}
		run := j - i
		if run > allowed {
			run = allowed
		}
		for k := 0; k < run; k++ {
			b.WriteByte('\n')
		}
		i = j
	}
	return b.String()
}

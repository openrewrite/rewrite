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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
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
// The first-statement rule reaches through the leftmost spine of the
// statement node — the parser places inter-statement whitespace on the
// *leftmost descendant* (Variable.Prefix, not Assignment.Prefix), so
// transformLeftmostPrefix walks down to find it.
type BlankLinesVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
}

// NewBlankLinesVisitor returns a visitor configured with the given
// stopAfter bound. Pass nil to format the entire visited tree.
func NewBlankLinesVisitor(stopAfter tree.Tree) *BlankLinesVisitor {
	return visitor.Init(&BlankLinesVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
	})
}

func (v *BlankLinesVisitor) Visit(t tree.Tree, p any) tree.Tree {
	if v.shouldHalt() {
		return t
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

func (v *BlankLinesVisitor) VisitSpace(s tree.Space, p any) tree.Space {
	if !strings.Contains(s.Whitespace, "\n\n\n") {
		return s
	}
	s.Whitespace = capInternalBlankLines(s.Whitespace, 1)
	return s
}

func (v *BlankLinesVisitor) VisitBlock(block *tree.Block, p any) tree.J {
	out := v.GoVisitor.VisitBlock(block, p).(*tree.Block)
	out = out.WithEnd(adjustSpace(out.End, stripLeadingBlankLines))

	// Strip any leading blank line above the first statement. The
	// inter-statement whitespace lives on the statement's leftmost
	// descendant, so we walk the spine to find it.
	if len(out.Statements) > 0 && out.Statements[0].Element != nil {
		first := out.Statements[0]
		if updated, ok := transformLeftmostPrefix(first.Element, stripLeadingBlankLinesSpace).(tree.Statement); ok {
			first.Element = updated
			out.Statements[0] = first
		}
	}
	return out
}

func stripLeadingBlankLinesSpace(s tree.Space) tree.Space {
	s.Whitespace = stripLeadingBlankLines(s.Whitespace)
	return s
}

func adjustSpace(s tree.Space, f func(string) string) tree.Space {
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

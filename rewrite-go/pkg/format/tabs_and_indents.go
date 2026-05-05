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

// TabsAndIndentsVisitor re-indents block-level whitespace to gofmt's
// `\t × depth` convention.
//
// Strategy: rather than touching every Space in the tree (which would
// rewrite continuation alignments inside multi-line argument lists),
// the visitor drives indentation from VisitBlock explicitly:
//
//   - For each `Block.Statements[i].Element`, the leftmost leaf's
//     Prefix carries the inter-statement whitespace; we rewrite its
//     indent (the post-newline portion) to `\t × (depth+1)`.
//   - `Block.End` (the whitespace before `}`) is re-indented to
//     `\t × depth`.
//   - VisitCase decrements depth around the case-clause Prefix so
//     `case` keywords align with the enclosing `switch` (gofmt
//     convention) while body statements remain at body depth.
//
// Whitespace that doesn't carry a newline, or whitespace inside an
// expression context (continuations, alignments) is left untouched.
type TabsAndIndentsVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
	depth int
}

// NewTabsAndIndentsVisitor returns a visitor configured with the given
// stopAfter bound. Pass nil to format the entire visited tree.
func NewTabsAndIndentsVisitor(stopAfter tree.Tree) *TabsAndIndentsVisitor {
	return visitor.Init(&TabsAndIndentsVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
	})
}

func (v *TabsAndIndentsVisitor) Visit(t tree.Tree, p any) tree.Tree {
	if v.shouldHalt() {
		return t
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

// VisitBlock dispatches the body at depth+1 and re-indents each
// statement's leftmost-leaf Prefix and the closing-brace `End`.
func (v *TabsAndIndentsVisitor) VisitBlock(block *tree.Block, p any) tree.J {
	v.depth++
	stmts := make([]tree.RightPadded[tree.Statement], len(block.Statements))
	for i, rp := range block.Statements {
		if rp.Element != nil {
			fixed, _ := transformLeftmostPrefix(rp.Element, v.reindentSpace).(tree.Statement)
			if fixed != nil {
				rp.Element = fixed
			}
			if next, ok := v.Visit(rp.Element, p).(tree.Statement); ok {
				rp.Element = next
			}
		}
		stmts[i] = rp
	}
	block.Statements = stmts
	v.depth--

	block = block.WithEnd(v.reindentSpace(block.End))
	return block
}

// VisitCase aligns the case keyword with the enclosing switch (one
// tab less than the switch body's depth) while keeping the case body
// statements at body depth. Also explicitly visits Body so nested
// blocks inside a case get their own indent fixes (the default
// GoVisitor.VisitCase doesn't recurse into Body).
func (v *TabsAndIndentsVisitor) VisitCase(c *tree.Case, p any) tree.J {
	v.depth--
	c = c.WithPrefix(v.reindentSpace(c.Prefix))
	v.depth++

	body := make([]tree.RightPadded[tree.Statement], len(c.Body))
	for i, rp := range c.Body {
		if rp.Element != nil {
			fixed, _ := transformLeftmostPrefix(rp.Element, v.reindentSpace).(tree.Statement)
			if fixed != nil {
				rp.Element = fixed
			}
			if next, ok := v.Visit(rp.Element, p).(tree.Statement); ok {
				rp.Element = next
			}
		}
		body[i] = rp
	}
	c.Body = body
	return c
}

// reindentSpace rewrites the indent (post-last-newline portion) of s
// to `\t × v.depth`. Whitespace without a newline is returned
// unchanged. The pre-newline portion (which can hold blank lines) is
// preserved — BlankLinesVisitor handles that.
func (v *TabsAndIndentsVisitor) reindentSpace(s tree.Space) tree.Space {
	if !strings.Contains(s.Whitespace, "\n") {
		return s
	}
	last := strings.LastIndex(s.Whitespace, "\n")
	want := strings.Repeat("\t", v.depth)
	if s.Whitespace[last+1:] == want {
		return s
	}
	s.Whitespace = s.Whitespace[:last+1] + want
	return s
}

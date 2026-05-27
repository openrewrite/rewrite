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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// AutoFormatVisitor composes the per-responsibility format visitors
// into a single end-to-end pipeline. The pipeline applies passes in a
// fixed order — each later pass relies on the structure normalized by
// the earlier ones:
//
//  1. RemoveTrailingWhitespaceVisitor  — strip trailing spaces/tabs
//  2. BlankLinesVisitor                — collapse blank-line runs
//  3. TabsAndIndentsVisitor            — re-indent post-newline whitespace
//  4. SpacesVisitor                    — normalize intra-line spacing
//
// Pass-1 runs first because pass-3 (re-indent) only touches the post-
// newline portion of a Whitespace; if pass-1 ran later, the trailing
// space/tabs of an earlier line could survive. Pass-2 runs before pass-3
// because collapsing blank lines doesn't touch indents — it just
// removes whole `\n` characters — so pass-3 still has the right
// post-newline section to rewrite. Pass-4 runs last because changes
// within a binary/assignment don't affect whether a line carries a
// newline; spacing fixes can't disturb prior passes.
//
// stopAfter is forwarded to every member visitor; pass nil to format
// the entire visited subtree.
type AutoFormatVisitor struct {
	visitor.GoVisitor
	stopAfter tree.Tree
}

// NewAutoFormatVisitor returns a composer visitor that, on its first
// Visit, queues the four per-responsibility passes via DoAfterVisit.
// The recipe runner's after-visit drain runs them in order. Each pass
// sees the partially-normalized tree from its predecessors.
func NewAutoFormatVisitor(stopAfter tree.Tree) *AutoFormatVisitor {
	return visitor.Init(&AutoFormatVisitor{stopAfter: stopAfter})
}

func (v *AutoFormatVisitor) Visit(t tree.Tree, p any) tree.Tree {
	if t == nil {
		return nil
	}
	v.DoAfterVisit(NewRemoveTrailingWhitespaceVisitor(v.stopAfter))
	v.DoAfterVisit(NewBlankLinesVisitor(v.stopAfter))
	v.DoAfterVisit(NewTabsAndIndentsVisitor(v.stopAfter))
	v.DoAfterVisit(NewSpacesVisitor(v.stopAfter))
	return t
}

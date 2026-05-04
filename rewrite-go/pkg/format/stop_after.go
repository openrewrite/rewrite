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

// Package format provides single-responsibility visitors that together
// implement gofmt-style normalization. Each visitor handles one aspect:
//
//   - TabsAndIndentsVisitor    — re-indent post-newline whitespace
//   - BlankLinesVisitor        — collapse runs of >1 blank line and trim
//                                blank lines at block boundaries
//   - SpacesVisitor            — token-level spacing fixes (binary operators,
//                                commas, etc.)
//   - RemoveTrailingWhitespaceVisitor — strip trailing space/tab from
//                                       line endings inside Whitespace
//
// AutoFormatVisitor composes the four into a fixed pipeline via
// DoAfterVisit. Each is independently usable for recipes that only need
// one pass.
//
// All visitors honor a `stopAfter tree.Tree` parameter: when non-nil,
// traversal halts after the given node has been visited, leaving
// downstream subtrees untouched. When nil, the entire visited subtree
// is processed.
//
// The package is deliberately minimal. gofmt's full rule set (column
// alignment in const blocks, struct field tag alignment, etc.) is not
// implemented yet — recipes that need byte-exact gofmt output should
// pipe through the gofmt binary; AutoFormat targets the "splice in a
// synthesized subtree and put it at the right indent" use case.
package format

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// stopAfterTracker is embedded by every format visitor to share the
// "halt after visiting this node" semantics. Once stopAfter has been
// fully visited, halted flips to true and subsequent Visit calls
// short-circuit.
type stopAfterTracker struct {
	stopAfter tree.Tree
	halted    bool
}

// shouldHalt reports whether traversal should be skipped because the
// stopAfter node has already been visited. Call from any Visit override
// before doing work.
func (s *stopAfterTracker) shouldHalt() bool {
	return s.halted
}

// noteVisited records that the given node has been visited; flips
// halted once the configured stopAfter node is encountered. Call this
// at the END of each Visit method (after recursing into children) so
// the stopAfter subtree is processed before halting kicks in.
func (s *stopAfterTracker) noteVisited(t tree.Tree) {
	if s.halted || s.stopAfter == nil || t == nil {
		return
	}
	if t == s.stopAfter {
		s.halted = true
	}
}

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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// AutoFormatVisitor composes the per-responsibility format visitors into a
// single end-to-end pipeline, in the two phases org.openrewrite.java.format.
// AutoFormatVisitor uses. Spacing is settled first, widest scope last:
//
//  1. MinimumViableSpacingVisitor      — separation the tokens require
//  2. DocCommentVisitor                — canonical doc comment text
//  3. SpacesVisitor                    — intra-line spacing
//  4. TabsAndIndentsVisitor            — indentation
//
// then the passes that decide what the lines are, once their text is final:
//
//  5. BlankLinesVisitor                — collapse blank-line runs
//  6. RemoveTrailingWhitespaceVisitor  — strip trailing space/tabs
//
// Phase two runs last because the earlier passes rewrite a line's text and can
// add or drop whole lines, so a line structure normalized ahead of them would
// not stay normalized.
//
// stopAfter is forwarded to every member visitor; pass nil to format
// the entire visited subtree.
type AutoFormatVisitor struct {
	visitor.GoVisitor
	stopAfter java.Tree
}

func NewAutoFormatVisitor(stopAfter java.Tree) *AutoFormatVisitor {
	return visitor.Init(&AutoFormatVisitor{stopAfter: stopAfter})
}

func (v *AutoFormatVisitor) Visit(t java.Tree, p any) java.Tree {
	if t == nil {
		return nil
	}
	v.DoAfterVisit(NewMinimumViableSpacingVisitor(v.stopAfter))
	v.DoAfterVisit(NewDocCommentVisitor(v.stopAfter))
	v.DoAfterVisit(NewSpacesVisitor(v.stopAfter))
	v.DoAfterVisit(NewTabsAndIndentsVisitor(v.stopAfter))
	v.DoAfterVisit(NewBlankLinesVisitor(v.stopAfter))
	v.DoAfterVisit(NewRemoveTrailingWhitespaceVisitor(v.stopAfter))
	return t
}

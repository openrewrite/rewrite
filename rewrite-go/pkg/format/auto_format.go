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
//  3. BinarySpacingVisitor             — blanks around binary operators
//  4. SpacesVisitor                    — intra-line spacing
//  5. TabsAndIndentsVisitor            — indentation
//
// then the passes that decide what the lines are, once their text is final:
//
//  6. BlankLinesVisitor                — collapse blank-line runs
//  7. RemoveTrailingWhitespaceVisitor  — strip trailing space/tabs
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
	for _, member := range []visitor.AfterVisitor{
		NewMinimumViableSpacingVisitor(v.stopAfter),
		NewDocCommentVisitor(v.stopAfter),
		NewBinarySpacingVisitor(v.stopAfter),
		NewSpacesVisitor(v.stopAfter),
		NewTabsAndIndentsVisitor(v.stopAfter),
		NewBlankLinesVisitor(v.stopAfter),
		NewRemoveTrailingWhitespaceVisitor(v.stopAfter),
	} {
		v.DoAfterVisit(withCursor(member, v.Cursor()))
	}
	return t
}

// withCursor hands a member visitor the ancestors above the subtree.
func withCursor(v visitor.AfterVisitor, c *visitor.Cursor) visitor.AfterVisitor {
	if c == nil {
		return v
	}
	if seedable, ok := v.(interface{ SetCursor(*visitor.Cursor) }); ok {
		seedable.SetCursor(c)
	}
	return v
}

// AutoFormat lays out t and returns the result. Pass the cursor of t's parent
// when t is a subtree of a larger file; pass nil when t is the file.
func AutoFormat(t java.Tree, p any, stopAfter java.Tree, cursor *visitor.Cursor) java.Tree {
	if t == nil {
		return nil
	}
	v := NewAutoFormatVisitor(stopAfter)
	v.SetCursor(cursor)
	out := v.Visit(t, p)
	if out == nil {
		return t
	}
	return visitor.DrainAfterVisits(v, out, p)
}

// MaybeAutoFormat lays out after only when it is not before, leaving layout
// alone for a visit that changed nothing.
func MaybeAutoFormat(before, after java.Tree, p any, stopAfter java.Tree, cursor *visitor.Cursor) java.Tree {
	if any(before) == any(after) {
		return after
	}
	return AutoFormat(after, p, stopAfter, cursor)
}

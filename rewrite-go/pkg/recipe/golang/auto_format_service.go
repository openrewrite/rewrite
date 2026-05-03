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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// AutoFormatService exposes gofmt-style normalization as composable
// visitors. Mirrors org.openrewrite.java.service.AutoFormatService.
//
// Recipes get one via recipe.Service:
//
//	svc := recipe.Service[*golang.AutoFormatService](cu)
//	v.DoAfterVisit(svc.AutoFormatVisitor(nil))
//
// AutoFormatVisitor runs the full pipeline (trailing whitespace →
// blank lines → tabs/indents → spaces). The single-pass visitors
// (TabsAndIndentsVisitor, BlankLinesVisitor, etc.) are exposed
// individually for recipes that only need one normalization pass.
//
// Each method accepts a `stopAfter` bound. When non-nil, traversal
// halts once that node has been fully visited — useful for formatting
// only a synthesized subtree without disturbing surrounding code.
// Pass nil to format the entire visited subtree.
type AutoFormatService struct{}

// AutoFormatVisitor returns a visitor that applies the full gofmt-style
// pipeline. Composes via DoAfterVisit so individual passes can be
// inspected or replaced independently if needed.
func (s *AutoFormatService) AutoFormatVisitor(stopAfter tree.Tree) recipe.TreeVisitor {
	return format.NewAutoFormatVisitor(stopAfter)
}

// TabsAndIndentsVisitor returns just the indent-fix pass. Use when a
// recipe has spliced a subtree that needs re-indenting but already has
// correct internal spacing.
func (s *AutoFormatService) TabsAndIndentsVisitor(stopAfter tree.Tree) recipe.TreeVisitor {
	return format.NewTabsAndIndentsVisitor(stopAfter)
}

// BlankLinesVisitor returns just the blank-line collapse pass. Use to
// clean up after a delete-and-replace edit that left stray blank lines
// inside a block.
func (s *AutoFormatService) BlankLinesVisitor(stopAfter tree.Tree) recipe.TreeVisitor {
	return format.NewBlankLinesVisitor(stopAfter)
}

// SpacesVisitor returns just the intra-line spacing pass. Use after a
// recipe has built up a binary/assignment node from raw parts and the
// operator surrounds need normalizing to a single space.
func (s *AutoFormatService) SpacesVisitor(stopAfter tree.Tree) recipe.TreeVisitor {
	return format.NewSpacesVisitor(stopAfter)
}

// RemoveTrailingWhitespaceVisitor returns just the trailing-whitespace
// strip pass. Useful as a standalone cleanup over a tree the rest of
// the pipeline shouldn't touch.
func (s *AutoFormatService) RemoveTrailingWhitespaceVisitor(stopAfter tree.Tree) recipe.TreeVisitor {
	return format.NewRemoveTrailingWhitespaceVisitor(stopAfter)
}

func init() {
	recipe.RegisterService[*AutoFormatService](func() any { return &AutoFormatService{} })
}

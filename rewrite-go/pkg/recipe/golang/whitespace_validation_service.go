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
	"fmt"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// WhitespaceValidationService walks a tree and reports any Space whose
// Whitespace or Comment.Suffix contains non-whitespace characters, or
// any Comment.Text that doesn't begin with `//` or `/*`. Such content
// indicates a parser bug — the printer would otherwise re-emit
// non-whitespace as if it were spacing, silently corrupting the source.
//
// Recipes get one via recipe.Service:
//
//	svc := recipe.Service[*golang.WhitespaceValidationService](cu)
//	if errs := svc.Validate(cu); len(errs) > 0 { /* fail loudly */ }
//
// The test harness uses this via pkg/test, which delegates here so the
// validation logic has a single home and stays callable from recipes
// that want to self-check synthesized subtrees.
type WhitespaceValidationService struct{}

// Validate walks the tree rooted at root and returns one descriptive
// error per offending Space / Comment. Returns nil when the tree is
// well-formed.
func (s *WhitespaceValidationService) Validate(root tree.Tree) []string {
	v := visitor.Init(&whitespaceValidator{})
	v.Visit(root, nil)
	return v.errs
}

// IsValid is the boolean shorthand. Recipes that just want to assert
// "no parser corruption" can write `if !svc.IsValid(cu) { ... }`.
func (s *WhitespaceValidationService) IsValid(root tree.Tree) bool {
	return len(s.Validate(root)) == 0
}

type whitespaceValidator struct {
	visitor.GoVisitor
	errs []string
}

func (v *whitespaceValidator) VisitSpace(space tree.Space, p any) tree.Space {
	if space.Whitespace != "" && !isWhitespaceOnly(space.Whitespace) {
		v.errs = append(v.errs, fmt.Sprintf("Space.Whitespace contains non-whitespace: %q", truncateForError(space.Whitespace, 80)))
	}
	for i, c := range space.Comments {
		if c.Suffix != "" && !isWhitespaceOnly(c.Suffix) {
			v.errs = append(v.errs, fmt.Sprintf("Comment[%d].Suffix contains non-whitespace: %q", i, truncateForError(c.Suffix, 80)))
		}
		if c.Text != "" && !strings.HasPrefix(c.Text, "//") && !strings.HasPrefix(c.Text, "/*") {
			v.errs = append(v.errs, fmt.Sprintf("Comment[%d].Text is not a comment: %q", i, truncateForError(c.Text, 80)))
		}
	}
	return space
}

func isWhitespaceOnly(s string) bool {
	for _, c := range s {
		if c != ' ' && c != '\t' && c != '\n' && c != '\r' {
			return false
		}
	}
	return true
}

func truncateForError(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "..."
}

func init() {
	recipe.RegisterService[*WhitespaceValidationService](func() any { return &WhitespaceValidationService{} })
}

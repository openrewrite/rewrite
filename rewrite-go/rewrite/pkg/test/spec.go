/*
 * Copyright 2025 the original author or authors.
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

package test

import (
	"fmt"
	"math"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/pkg/parser"
	"github.com/openrewrite/rewrite/pkg/printer"
	"github.com/openrewrite/rewrite/pkg/recipe"
	"github.com/openrewrite/rewrite/pkg/tree"
	"github.com/openrewrite/rewrite/pkg/visitor"
)

// SourceSpec describes a Go source file for testing.
type SourceSpec struct {
	Before string
	After  *string // nil means no change expected (parse-print idempotence only)
	Path   string
}

// Golang creates a SourceSpec for Go source code.
// The source string is automatically dedented: the common leading
// whitespace across all non-empty lines is stripped, and a leading/trailing
// blank line (from the backtick) is removed. This lets test strings be
// indented to match their surrounding code:
//
//	Golang(`
//		package main
//
//		func hello() {
//			return "hi"
//		}
//	`)
func Golang(before string, after ...string) SourceSpec {
	spec := SourceSpec{
		Before: TrimIndent(before),
		Path:   "test.go",
	}
	if len(after) > 0 {
		a := TrimIndent(after[0])
		spec.After = &a
	}
	return spec
}

// GolangRaw creates a SourceSpec from raw Go source (no indent trimming).
func GolangRaw(before string, after ...string) SourceSpec {
	spec := SourceSpec{
		Before: before,
		Path:   "test.go",
	}
	if len(after) > 0 {
		spec.After = &after[0]
	}
	return spec
}

// TrimIndent removes the common leading whitespace from all non-empty lines,
// and strips a single leading and trailing blank line (artifacts of backtick
// string literals that start/end on their own line).
func TrimIndent(s string) string {
	lines := strings.Split(s, "\n")

	// Strip leading blank line (the newline right after opening backtick)
	if len(lines) > 0 && strings.TrimSpace(lines[0]) == "" {
		lines = lines[1:]
	}
	// Strip trailing blank line (the newline right before closing backtick)
	if len(lines) > 0 && strings.TrimSpace(lines[len(lines)-1]) == "" {
		lines = lines[:len(lines)-1]
	}

	// Find minimum indentation across non-empty lines
	minIndent := math.MaxInt
	for _, line := range lines {
		if strings.TrimSpace(line) == "" {
			continue
		}
		indent := len(line) - len(strings.TrimLeft(line, " \t"))
		if indent < minIndent {
			minIndent = indent
		}
	}
	if minIndent == math.MaxInt {
		minIndent = 0
	}

	// Strip the common indent
	for i, line := range lines {
		if len(line) >= minIndent {
			lines[i] = line[minIndent:]
		}
	}

	return strings.Join(lines, "\n") + "\n"
}

// spaceValidator is a visitor that checks every Space it encounters
// for non-whitespace content that would indicate a parser bug.
type spaceValidator struct {
	visitor.GoVisitor
	errs []string
}

func (v *spaceValidator) VisitSpace(space tree.Space, p any) tree.Space {
	if space.Whitespace != "" && !isWhitespaceOnly(space.Whitespace) {
		v.errs = append(v.errs, fmt.Sprintf("Space.Whitespace contains non-whitespace: %q", truncate(space.Whitespace, 80)))
	}
	for i, c := range space.Comments {
		if c.Suffix != "" && !isWhitespaceOnly(c.Suffix) {
			v.errs = append(v.errs, fmt.Sprintf("Comment[%d].Suffix contains non-whitespace: %q", i, truncate(c.Suffix, 80)))
		}
		if c.Text != "" && !strings.HasPrefix(c.Text, "//") && !strings.HasPrefix(c.Text, "/*") {
			v.errs = append(v.errs, fmt.Sprintf("Comment[%d].Text is not a comment: %q", i, truncate(c.Text, 80)))
		}
	}
	return space
}

// ValidateSpaces walks the tree and returns errors for any Space that
// contains non-whitespace content (which would indicate a parser bug).
func ValidateSpaces(root tree.Tree) []string {
	v := visitor.Init(&spaceValidator{})
	v.Visit(root, nil)
	return v.errs
}

func isWhitespaceOnly(s string) bool {
	for _, c := range s {
		if c != ' ' && c != '\t' && c != '\n' && c != '\r' {
			return false
		}
	}
	return true
}

func truncate(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "..."
}

// RecipeSpec configures a test run.
type RecipeSpec struct {
	CheckParsePrintIdempotence bool
	Recipe                     recipe.Recipe
}

// NewRecipeSpec creates a new RecipeSpec with default settings.
func NewRecipeSpec() *RecipeSpec {
	return &RecipeSpec{
		CheckParsePrintIdempotence: true,
	}
}

// WithRecipe sets the recipe to apply during the test run.
func (spec *RecipeSpec) WithRecipe(r recipe.Recipe) *RecipeSpec {
	spec.Recipe = r
	return spec
}

// RewriteRun parses the source specs, checks parse-print idempotence,
// and (if configured) applies a recipe and checks the result.
func (spec *RecipeSpec) RewriteRun(t *testing.T, sources ...SourceSpec) {
	t.Helper()

	p := parser.NewGoParser()

	for _, src := range sources {
		cu, err := p.Parse(src.Path, src.Before)
		if err != nil {
			t.Fatalf("parse error: %v", err)
		}

		// Validate that no Space contains non-whitespace syntax
		if errs := ValidateSpaces(cu); len(errs) > 0 {
			for _, e := range errs {
				t.Errorf("space validation: %s", e)
			}
		}

		if spec.CheckParsePrintIdempotence {
			printed := printer.Print(cu)
			if printed != src.Before {
				t.Errorf("parse-print idempotence failed\n\nexpected:\n%s\n\nactual:\n%s\n\ndiff positions:", src.Before, printed)
				showDiff(t, src.Before, printed)
			}
		}

		// Apply recipe if configured
		if spec.Recipe != nil {
			editor := spec.Recipe.Editor()
			if editor != nil {
				ctx := recipe.NewExecutionContext()
				result := editor.Visit(cu, ctx)
				if result == nil {
					if src.After != nil {
						t.Error("recipe returned nil (deleted source file) but expected an after state")
					}
					continue
				}
				actual := printer.Print(result)
				if src.After != nil {
					if actual != *src.After {
						t.Errorf("recipe result mismatch\n\nexpected:\n%s\n\nactual:\n%s", *src.After, actual)
						showDiff(t, *src.After, actual)
					}
				} else {
					// No after state: expect no changes
					if actual != src.Before {
						t.Errorf("recipe made unexpected changes\n\nexpected (no change):\n%s\n\nactual:\n%s", src.Before, actual)
					}
				}
			}
		} else if src.After != nil {
			t.Error("after state specified but no recipe configured")
		}
	}
}

func showDiff(t *testing.T, expected, actual string) {
	t.Helper()
	minLen := len(expected)
	if len(actual) < minLen {
		minLen = len(actual)
	}
	for i := 0; i < minLen; i++ {
		if expected[i] != actual[i] {
			start := i - 20
			if start < 0 {
				start = 0
			}
			end := i + 20
			if end > minLen {
				end = minLen
			}
			t.Errorf("first difference at byte %d:\n  expected: %q\n  actual:   %q",
				i, expected[start:end], actual[start:end])
			break
		}
	}
	if len(expected) != len(actual) {
		t.Errorf("length difference: expected %d, got %d", len(expected), len(actual))
	}
}

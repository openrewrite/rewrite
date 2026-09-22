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

package java

import (
	"strings"
	"testing"
)

// TestParseSpaceCommentTextIsDelimiterFree pins the Java-aligned contract:
// Comment.Text holds only the content between the delimiters (no `//` or
// `/* */`), and Multiline distinguishes block comments from line comments.
func TestParseSpaceCommentTextIsDelimiterFree(t *testing.T) {
	cases := []struct {
		name          string
		raw           string
		wantMultiline bool
		wantText      string
	}{
		{"line", "// Package anwil\n", false, " Package anwil"},
		{"line no trailing newline", "//x", false, "x"},
		{"empty line comment", "//\n", false, ""},
		{"single-line block", "/* x */ ", true, " x "},
		{"multi-line block", "/* a\n   b */\n", true, " a\n   b "},
		{"go directive", "//go:build linux\n", false, "go:build linux"},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			// when
			space := ParseSpace(tc.raw)

			// then
			if len(space.Comments) != 1 {
				t.Fatalf("want 1 comment, got %d for %q", len(space.Comments), tc.raw)
			}
			c := space.Comments[0]
			if c.Multiline != tc.wantMultiline {
				t.Errorf("Multiline: want %v, got %v", tc.wantMultiline, c.Multiline)
			}
			if c.Text != tc.wantText {
				t.Errorf("Text: want %q, got %q", tc.wantText, c.Text)
			}
			if strings.HasPrefix(c.Text, "//") || strings.HasPrefix(c.Text, "/*") {
				t.Errorf("Text should be delimiter-free, got %q", c.Text)
			}
		})
	}
}

// TestParseSpaceRoundTrips verifies that reconstructing the source from the
// parsed Space (Whitespace, then the delimiters re-added around each comment's
// Text, then its Suffix) reproduces the input exactly, so stripping the
// delimiters into Multiline loses nothing.
func TestParseSpaceRoundTrips(t *testing.T) {
	inputs := []string{
		"",
		"   ",
		"\n\t",
		"// leading\n",
		"  // indented comment\n\t",
		"/* block */ ",
		"/* multi\n line\n block */\n",
		"//go:build linux\n",
		"\n// first\n// second\nx",
		"/*a*//*b*/",
	}
	for _, in := range inputs {
		space := ParseSpace(in)
		var sb strings.Builder
		sb.WriteString(space.Whitespace)
		for _, c := range space.Comments {
			if c.Multiline {
				sb.WriteString("/*" + c.Text + "*/")
			} else {
				sb.WriteString("//" + c.Text)
			}
			sb.WriteString(c.Suffix)
		}
		if got := sb.String(); got != in {
			t.Errorf("round-trip mismatch:\n  in:  %q\n  got: %q", in, got)
		}
	}
}

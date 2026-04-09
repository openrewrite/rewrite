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

package tree

import "strings"

// Comment represents a comment in source code.
type Comment struct {
	Kind      CommentKind
	Text      string
	Suffix    string // whitespace after the comment, before the next token
	Multiline bool
	Markers   Markers
}

type CommentKind int

const (
	LineComment CommentKind = iota
	BlockComment
)

// Space represents whitespace and comments that appear before a syntax element.
// This is the fundamental unit of formatting preservation in OpenRewrite.
type Space struct {
	Comments   []Comment
	Whitespace string
}

// EmptySpace is the zero-value Space with no whitespace or comments.
var EmptySpace = Space{}

// SingleSpace is a Space containing exactly one space character.
var SingleSpace = Space{Whitespace: " "}

// IsEmpty returns true if this Space has no whitespace and no comments.
func (s Space) IsEmpty() bool {
	return s.Whitespace == "" && len(s.Comments) == 0
}

// Indent returns the indentation of this space, which is the whitespace
// after the last newline (or all whitespace if no newline is present).
func (s Space) Indent() string {
	if idx := strings.LastIndex(s.Whitespace, "\n"); idx >= 0 {
		return s.Whitespace[idx+1:]
	}
	return s.Whitespace
}

// ParseSpace parses raw text (between two token positions) into a Space,
// extracting any line comments (//) and block comments (/* */).
//
// The model follows Java OpenRewrite's convention:
//   - Space.Whitespace = whitespace BEFORE the first comment
//   - Comment.Suffix = whitespace AFTER each comment
//
// The printer emits: Whitespace + (Comment.Text + Comment.Suffix)* to reconstruct the original text.
func ParseSpace(raw string) Space {
	if raw == "" {
		return EmptySpace
	}

	// Find where the first comment starts
	firstComment := findCommentStart(raw, 0)
	if firstComment == len(raw) {
		// No comments, just whitespace
		return Space{Whitespace: raw}
	}

	whitespace := raw[:firstComment]
	var comments []Comment
	i := firstComment

	for i < len(raw) {
		if i+1 < len(raw) && raw[i] == '/' && raw[i+1] == '/' {
			// Line comment: from // to end of line (not including \n)
			end := strings.IndexByte(raw[i:], '\n')
			var text string
			if end < 0 {
				text = raw[i:]
				i = len(raw)
			} else {
				text = raw[i : i+end]
				i = i + end // i now points at \n
			}
			// Suffix: consume until next comment or end
			suffixEnd := findCommentStart(raw, i)
			suffix := raw[i:suffixEnd]
			i = suffixEnd
			comments = append(comments, Comment{Kind: LineComment, Text: text, Suffix: suffix})
		} else if i+1 < len(raw) && raw[i] == '/' && raw[i+1] == '*' {
			// Block comment: from /* to */
			end := strings.Index(raw[i+2:], "*/")
			var text string
			if end < 0 {
				text = raw[i:]
				i = len(raw)
			} else {
				text = raw[i : i+2+end+2]
				i = i + 2 + end + 2
			}
			multiline := strings.Contains(text, "\n")
			// Suffix: consume until next comment or end
			suffixEnd := findCommentStart(raw, i)
			suffix := raw[i:suffixEnd]
			i = suffixEnd
			comments = append(comments, Comment{Kind: BlockComment, Text: text, Suffix: suffix, Multiline: multiline})
		} else {
			// Should not happen if findCommentStart works correctly
			i++
		}
	}

	return Space{Comments: comments, Whitespace: whitespace}
}

// findCommentStart returns the index of the next // or /* starting from position 'from'.
// Returns len(s) if no comment is found.
func findCommentStart(s string, from int) int {
	for i := from; i+1 < len(s); i++ {
		if s[i] == '/' && (s[i+1] == '/' || s[i+1] == '*') {
			return i
		}
	}
	return len(s)
}

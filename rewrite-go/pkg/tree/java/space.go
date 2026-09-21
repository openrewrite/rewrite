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

package java

import (
	"strings"
	"sync"
)

type Comment struct {
	// Multiline reports whether this is a block comment (/* */) rather than a
	// line comment (//). Named to match Java's TextComment.multiline (and the
	// JS/Python/C# equivalents), where the flag likewise means "is a block
	// comment", not "spans more than one line".
	Multiline bool
	Text      string // content between the delimiters, delimiter-free (like Java's TextComment)
	Suffix    string // whitespace after the comment, before the next token
	Markers   Markers
}

// Space is the fundamental unit of formatting preservation in OpenRewrite. It is
// a value-typed handle over shared, immutable backing: copies are one pointer wide
// and identical spaces (empty, common indentation) share one backing allocation.
// A nil handle is the empty space, so the zero value is valid and allocation-free.
type Space struct {
	d *spaceData
}

type spaceData struct {
	comments   []Comment
	whitespace string
}

var EmptySpace = Space{}

var SingleSpace = MakeSpace(nil, " ")

// wsInterned deduplicates whitespace-only spaces (the overwhelming majority) so
// the same indentation string is backed by a single allocation across the tree.
var wsInterned sync.Map // map[string]*spaceData

// MakeSpace builds a Space, interning empty and whitespace-only spaces so equal
// layout shares one backing allocation. Callers must treat the backing as
// immutable: to change a space, build a new one.
func MakeSpace(comments []Comment, whitespace string) Space {
	if len(comments) == 0 {
		if whitespace == "" {
			return Space{}
		}
		if v, ok := wsInterned.Load(whitespace); ok {
			return Space{v.(*spaceData)}
		}
		d := &spaceData{whitespace: whitespace}
		actual, _ := wsInterned.LoadOrStore(whitespace, d)
		return Space{actual.(*spaceData)}
	}
	return Space{&spaceData{comments: comments, whitespace: whitespace}}
}

// Whitespace returns the whitespace preceding the first comment (or all of it
// when there are no comments).
func (s Space) Whitespace() string {
	if s.d == nil {
		return ""
	}
	return s.d.whitespace
}

// Comments returns the comments carried by this space.
func (s Space) Comments() []Comment {
	if s.d == nil {
		return nil
	}
	return s.d.comments
}

func (s Space) IsEmpty() bool {
	return s.d == nil || (s.d.whitespace == "" && len(s.d.comments) == 0)
}

// Indent returns the indentation of this space, which is the whitespace
// after the last newline (or all whitespace if no newline is present).
func (s Space) Indent() string {
	ws := s.Whitespace()
	if idx := strings.LastIndex(ws, "\n"); idx >= 0 {
		return ws[idx+1:]
	}
	return ws
}

// ParseSpace parses raw text (between two token positions) into a Space,
// extracting any line comments (//) and block comments (/* */).
//
// The model follows Java OpenRewrite's convention:
//   - Space.Whitespace = whitespace BEFORE the first comment
//   - Comment.Suffix = whitespace AFTER each comment
//
// The printer emits Whitespace, then each comment (its `//` or `/* */` delimiters
// wrapped around Comment.Text) followed by Comment.Suffix, to reconstruct the original text.
func ParseSpace(raw string) Space {
	if raw == "" {
		return EmptySpace
	}

	firstComment := findCommentStart(raw, 0)
	if firstComment == len(raw) {
		return MakeSpace(nil, raw)
	}

	whitespace := raw[:firstComment]
	var comments []Comment
	i := firstComment

	for i < len(raw) {
		if i+1 < len(raw) && raw[i] == '/' && raw[i+1] == '/' {
			end := strings.IndexByte(raw[i:], '\n')
			var text string
			if end < 0 {
				text = raw[i+2:]
				i = len(raw)
			} else {
				text = raw[i+2 : i+end]
				i = i + end // i now points at \n
			}
			suffixEnd := findCommentStart(raw, i)
			suffix := raw[i:suffixEnd]
			i = suffixEnd
			comments = append(comments, Comment{Text: text, Suffix: suffix})
		} else if i+1 < len(raw) && raw[i] == '/' && raw[i+1] == '*' {
			end := strings.Index(raw[i+2:], "*/")
			var text string
			if end < 0 {
				text = raw[i+2:]
				i = len(raw)
			} else {
				text = raw[i+2 : i+2+end]
				i = i + 2 + end + 2
			}
			suffixEnd := findCommentStart(raw, i)
			suffix := raw[i:suffixEnd]
			i = suffixEnd
			comments = append(comments, Comment{Multiline: true, Text: text, Suffix: suffix})
		} else {
			// Should not happen if findCommentStart works correctly
			i++
		}
	}

	return MakeSpace(comments, whitespace)
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

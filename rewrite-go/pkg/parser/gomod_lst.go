/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package parser

import (
	"github.com/google/uuid"
	"golang.org/x/mod/modfile"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// ParseGoModFile parses go.mod content into a lossless golang.GoMod LST.
// Mirrors org.openrewrite.golang.GoModParser on the Java side (the LST
// path, not the legacy PlainText+marker path).
//
// The structure is taken from modfile.ParseLax's low-level FileSyntax —
// ParseLax (not Parse) so unknown/future directives (godebug, tool,
// ignore, …) are preserved as generic directive lines instead of
// erroring. All whitespace and comments are reconstructed from the
// original byte ranges so re-printing yields the input verbatim; modfile
// is used only to locate token, paren, and line boundaries.
func ParseGoModFile(path, content string) (*golang.GoMod, error) {
	f, err := modfile.ParseLax(path, []byte(content), nil)
	if err != nil {
		return nil, err
	}

	gm := &golang.GoMod{
		Ident:      uuid.New(),
		Markers:    java.Markers{ID: uuid.New()},
		SourcePath: path,
		Charset:    "UTF-8",
	}

	cursor := 0
	var stmts []java.RightPadded[golang.GoModStatement]
	for _, stmt := range f.Syntax.Stmt {
		switch s := stmt.(type) {
		case *modfile.Line:
			prefix := java.ParseSpace(content[cursor:s.Start.Byte])
			vals := splitGoModTokens(content[s.Start.Byte:s.End.Byte])
			dir := &golang.GoModDirective{
				Ident:   uuid.New(),
				Prefix:  prefix,
				Markers: java.Markers{ID: uuid.New()},
			}
			if len(vals) > 0 {
				dir.Keyword = vals[0].Text
				dir.Values = vals[1:]
			}
			after, next := consumeGoModAfter(content, s.End.Byte)
			cursor = next
			stmts = append(stmts, rightPadStatement(dir, after))

		case *modfile.LineBlock:
			prefix := java.ParseSpace(content[cursor:s.Start.Byte])
			keyword, beforeLParen := splitGoModHead(content[s.Start.Byte:s.LParen.Pos.Byte])
			cursor = s.LParen.Pos.Byte + 1 // past '('

			var entries []java.RightPadded[golang.GoModStatement]
			for _, ln := range s.Line {
				entryPrefix := java.ParseSpace(content[cursor:ln.Start.Byte])
				entry := &golang.GoModDirective{
					Ident:   uuid.New(),
					Prefix:  entryPrefix,
					Markers: java.Markers{ID: uuid.New()},
					Values:  splitGoModTokens(content[ln.Start.Byte:ln.End.Byte]),
				}
				after, next := consumeGoModAfter(content, ln.End.Byte)
				cursor = next
				entries = append(entries, rightPadStatement(entry, after))
			}

			beforeRParen := java.ParseSpace(content[cursor:s.RParen.Pos.Byte])
			cursor = s.RParen.Pos.Byte + 1 // past ')'

			block := &golang.GoModBlock{
				Ident:        uuid.New(),
				Prefix:       prefix,
				Markers:      java.Markers{ID: uuid.New()},
				Keyword:      keyword,
				BeforeLParen: beforeLParen,
				Entries:      entries,
				BeforeRParen: beforeRParen,
			}
			after, next := consumeGoModAfter(content, cursor)
			cursor = next
			stmts = append(stmts, rightPadStatement(block, after))

		case *modfile.CommentBlock:
			// Whole-line comment(s) with no tokens. Not modeled as a node:
			// the bytes are recovered as part of the next statement's
			// prefix (or Eof), keeping the tree minimal and lossless.
			continue
		}
	}

	gm.Statements = stmts
	gm.Eof = java.ParseSpace(content[cursor:])
	return gm, nil
}

func rightPadStatement(s golang.GoModStatement, after java.Space) java.RightPadded[golang.GoModStatement] {
	return java.RightPadded[golang.GoModStatement]{
		Element: s,
		After:   after,
		Markers: java.Markers{ID: uuid.New()},
	}
}

// splitGoModTokens splits a raw token region (the bytes between a line's
// first and last token, exclusive of any suffix comment) into value nodes,
// each carrying its leading whitespace as a prefix. Tokens come straight
// from the raw text, so original quoting and spelling are preserved.
func splitGoModTokens(raw string) []*golang.GoModValue {
	var vals []*golang.GoModValue
	i := 0
	for i < len(raw) {
		ws := i
		for i < len(raw) && isGoModSpace(raw[i]) {
			i++
		}
		prefix := raw[ws:i]
		if i >= len(raw) {
			break
		}
		start := i
		for i < len(raw) && !isGoModSpace(raw[i]) {
			i++
		}
		vals = append(vals, &golang.GoModValue{
			Ident:   uuid.New(),
			Prefix:  java.ParseSpace(prefix),
			Markers: java.Markers{ID: uuid.New()},
			Text:    raw[start:i],
		})
	}
	return vals
}

// splitGoModHead splits the bytes between a block's start and its '(' into
// the keyword and the whitespace immediately before the paren.
func splitGoModHead(head string) (keyword string, before java.Space) {
	i := 0
	for i < len(head) && isGoModSpace(head[i]) {
		i++
	}
	start := i
	for i < len(head) && !isGoModSpace(head[i]) {
		i++
	}
	keyword = head[start:i]
	before = java.ParseSpace(head[i:])
	return keyword, before
}

// consumeGoModAfter captures the same-line trailing content after a
// statement's last token — any suffix comment plus the line's newline —
// as the statement's RightPadded.After, and returns the cursor positioned
// at the start of the next line.
func consumeGoModAfter(content string, from int) (java.Space, int) {
	i := from
	for i < len(content) && content[i] != '\n' {
		i++
	}
	if i < len(content) {
		i++ // include the newline
	}
	return java.ParseSpace(content[from:i]), i
}

func isGoModSpace(b byte) bool {
	return b == ' ' || b == '\t' || b == '\r'
}

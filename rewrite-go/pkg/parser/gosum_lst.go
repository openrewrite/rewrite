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
	"fmt"
	"strings"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// ParseGoSumFile parses go.sum content into a lossless GoSum LST. A malformed
// non-blank line is a hard error, so the caller falls back to a verbatim source.
func ParseGoSumFile(path, content string) (*golang.GoSum, error) {
	gs := &golang.GoSum{
		Ident:      uuid.New(),
		Markers:    java.Markers{ID: uuid.New()},
		SourcePath: path,
		Charset:    "UTF-8",
	}

	cursor := 0
	i := 0
	var lines []java.RightPadded[*golang.GoSumLine]
	for i < len(content) {
		lineStart := i
		j := i
		for j < len(content) && content[j] != '\n' {
			j++
		}
		lineEnd := j
		if lineEnd < len(content) {
			lineEnd++
		}
		lineText := content[lineStart:j]

		if strings.TrimSpace(lineText) == "" {
			i = lineEnd
			continue
		}

		m := goSumLine.FindStringSubmatchIndex(lineText)
		if m == nil {
			return nil, fmt.Errorf("go.sum: malformed line %q", lineText)
		}
		moduleStart := lineStart + m[2]
		hashEnd := lineStart + m[9]

		line := &golang.GoSumLine{
			Ident:      uuid.New(),
			Prefix:     java.ParseSpace(content[cursor:moduleStart]),
			Markers:    java.Markers{ID: uuid.New()},
			ModulePath: lineText[m[2]:m[3]],
			Version:    lineText[m[4]:m[5]],
			GoMod:      m[6] != -1,
			Hash:       "h1:" + lineText[m[8]:m[9]],
		}
		lines = append(lines, java.RightPadded[*golang.GoSumLine]{
			Element: line,
			After:   java.ParseSpace(content[hashEnd:lineEnd]),
			Markers: java.Markers{ID: uuid.New()},
		})
		cursor = lineEnd
		i = lineEnd
	}

	gs.Lines = lines
	gs.Eof = java.ParseSpace(content[cursor:])
	return gs, nil
}

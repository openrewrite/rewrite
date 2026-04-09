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

import "github.com/google/uuid"

// ParseError represents a source file that failed to parse.
// Mirrors org.openrewrite.tree.ParseError on the Java side.
type ParseError struct {
	Ident          uuid.UUID
	Markers        Markers
	SourcePath     string
	CharsetName    string
	CharsetBomMarked bool
	Text           string
}

// NewParseError creates a ParseError from a source path, source text, and error.
func NewParseError(sourcePath string, source string, err error) *ParseError {
	marker := ParseExceptionResult{
		Ident:         uuid.New(),
		ParserType:    "GolangParser",
		ExceptionType: "error",
		Message:       err.Error(),
	}
	markers := Markers{
		ID:      uuid.New(),
		Entries: []Marker{marker},
	}
	return &ParseError{
		Ident:          uuid.New(),
		Markers:        markers,
		SourcePath:     sourcePath,
		CharsetName:    "UTF-8",
		CharsetBomMarked: false,
		Text:           source,
	}
}

// ParseExceptionResult is a marker that captures information about a parse failure.
// Mirrors org.openrewrite.ParseExceptionResult on the Java side.
type ParseExceptionResult struct {
	Ident         uuid.UUID
	ParserType    string
	ExceptionType string
	Message       string
	TreeType      *string // nullable
}

func (p ParseExceptionResult) ID() uuid.UUID { return p.Ident }

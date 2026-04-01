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

package printer

import (
	"fmt"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// CommentWrapper wraps marker output in a language-appropriate comment syntax.
type CommentWrapper func(output string) string

// GoCommentWrapper wraps marker output in Go block comment syntax: /*~~(output)~~>*/
func GoCommentWrapper(output string) string {
	if output == "" {
		return "/*~~>*/"
	}
	return fmt.Sprintf("/*~~%s~~>*/", output)
}

// MarkerPrinter controls how cross-cutting markers (like SearchResult and Markup)
// are rendered in printed output. Go-specific markers (ShortVarDecl, GroupedImport, etc.)
// are handled directly by the printer's visit methods.
type MarkerPrinter interface {
	// BeforePrefix returns text to emit before a node's prefix space.
	BeforePrefix(marker tree.Marker, wrapper CommentWrapper) string

	// BeforeSyntax returns text to emit after a node's prefix but before its syntax.
	BeforeSyntax(marker tree.Marker, wrapper CommentWrapper) string

	// AfterSyntax returns text to emit after a node's syntax.
	AfterSyntax(marker tree.Marker, wrapper CommentWrapper) string
}

// --- Predefined MarkerPrinter implementations ---

// DefaultMarkerPrinter prints SearchResult and Markup markers as comments before syntax.
var DefaultMarkerPrinter MarkerPrinter = defaultMarkerPrinter{}

// SearchOnlyMarkerPrinter prints only SearchResult markers (ignores Markup).
var SearchOnlyMarkerPrinter MarkerPrinter = searchOnlyMarkerPrinter{}

// FencedMarkerPrinter wraps markers with {{uuid}} delimiters before and after syntax.
var FencedMarkerPrinter MarkerPrinter = fencedMarkerPrinter{}

// SanitizedMarkerPrinter strips all markers from output.
var SanitizedMarkerPrinter MarkerPrinter = sanitizedMarkerPrinter{}

// --- defaultMarkerPrinter ---

type defaultMarkerPrinter struct{}

func (defaultMarkerPrinter) BeforePrefix(marker tree.Marker, wrapper CommentWrapper) string {
	return ""
}

func (defaultMarkerPrinter) BeforeSyntax(marker tree.Marker, wrapper CommentWrapper) string {
	switch m := marker.(type) {
	case tree.SearchResult:
		if m.Description != "" {
			return wrapper("(" + m.Description + ")")
		}
		return wrapper("")
	case tree.Markup:
		if m.Detail != "" {
			return wrapper("(" + m.Message + ": " + m.Detail + ")")
		}
		return wrapper("(" + m.Message + ")")
	}
	return ""
}

func (defaultMarkerPrinter) AfterSyntax(marker tree.Marker, wrapper CommentWrapper) string {
	return ""
}

// --- searchOnlyMarkerPrinter ---

type searchOnlyMarkerPrinter struct{}

func (searchOnlyMarkerPrinter) BeforePrefix(marker tree.Marker, wrapper CommentWrapper) string {
	return ""
}

func (searchOnlyMarkerPrinter) BeforeSyntax(marker tree.Marker, wrapper CommentWrapper) string {
	if m, ok := marker.(tree.SearchResult); ok {
		if m.Description != "" {
			return wrapper("(" + m.Description + ")")
		}
		return wrapper("")
	}
	return ""
}

func (searchOnlyMarkerPrinter) AfterSyntax(marker tree.Marker, wrapper CommentWrapper) string {
	return ""
}

// --- fencedMarkerPrinter ---

type fencedMarkerPrinter struct{}

func (fencedMarkerPrinter) BeforePrefix(marker tree.Marker, wrapper CommentWrapper) string {
	return fmt.Sprintf("{{%s}}", marker.ID())
}

func (fencedMarkerPrinter) BeforeSyntax(marker tree.Marker, wrapper CommentWrapper) string {
	return ""
}

func (fencedMarkerPrinter) AfterSyntax(marker tree.Marker, wrapper CommentWrapper) string {
	return fmt.Sprintf("{{%s}}", marker.ID())
}

// --- sanitizedMarkerPrinter ---

type sanitizedMarkerPrinter struct{}

func (sanitizedMarkerPrinter) BeforePrefix(marker tree.Marker, wrapper CommentWrapper) string {
	return ""
}

func (sanitizedMarkerPrinter) BeforeSyntax(marker tree.Marker, wrapper CommentWrapper) string {
	return ""
}

func (sanitizedMarkerPrinter) AfterSyntax(marker tree.Marker, wrapper CommentWrapper) string {
	return ""
}

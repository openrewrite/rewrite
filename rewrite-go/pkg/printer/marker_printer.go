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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

type CommentWrapper func(output string) string

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
	BeforePrefix(marker java.Marker, wrapper CommentWrapper) string

	BeforeSyntax(marker java.Marker, wrapper CommentWrapper) string

	AfterSyntax(marker java.Marker, wrapper CommentWrapper) string
}

var DefaultMarkerPrinter MarkerPrinter = defaultMarkerPrinter{}

var SearchOnlyMarkerPrinter MarkerPrinter = searchOnlyMarkerPrinter{}

var FencedMarkerPrinter MarkerPrinter = fencedMarkerPrinter{}

// SanitizedMarkerPrinter strips all markers from output.
var SanitizedMarkerPrinter MarkerPrinter = sanitizedMarkerPrinter{}

type defaultMarkerPrinter struct{}

func (defaultMarkerPrinter) BeforePrefix(marker java.Marker, wrapper CommentWrapper) string {
	return ""
}

func (defaultMarkerPrinter) BeforeSyntax(marker java.Marker, wrapper CommentWrapper) string {
	switch m := marker.(type) {
	case java.SearchResult:
		if m.Description != "" {
			return wrapper("(" + m.Description + ")")
		}
		return wrapper("")
	case java.Markup:
		if m.Detail != "" {
			return wrapper("(" + m.Message + ": " + m.Detail + ")")
		}
		return wrapper("(" + m.Message + ")")
	}
	return ""
}

func (defaultMarkerPrinter) AfterSyntax(marker java.Marker, wrapper CommentWrapper) string {
	return ""
}

type searchOnlyMarkerPrinter struct{}

func (searchOnlyMarkerPrinter) BeforePrefix(marker java.Marker, wrapper CommentWrapper) string {
	return ""
}

func (searchOnlyMarkerPrinter) BeforeSyntax(marker java.Marker, wrapper CommentWrapper) string {
	if m, ok := marker.(java.SearchResult); ok {
		if m.Description != "" {
			return wrapper("(" + m.Description + ")")
		}
		return wrapper("")
	}
	return ""
}

func (searchOnlyMarkerPrinter) AfterSyntax(marker java.Marker, wrapper CommentWrapper) string {
	return ""
}

type fencedMarkerPrinter struct{}

func (fencedMarkerPrinter) BeforePrefix(marker java.Marker, wrapper CommentWrapper) string {
	if isFenceable(marker) {
		return fmt.Sprintf("{{%s}}", marker.ID())
	}
	return ""
}

func (fencedMarkerPrinter) BeforeSyntax(marker java.Marker, wrapper CommentWrapper) string {
	return ""
}

func (fencedMarkerPrinter) AfterSyntax(marker java.Marker, wrapper CommentWrapper) string {
	if isFenceable(marker) {
		return fmt.Sprintf("{{%s}}", marker.ID())
	}
	return ""
}

func isFenceable(marker java.Marker) bool {
	switch marker.(type) {
	case java.SearchResult, java.Markup:
		return true
	}
	return false
}

type sanitizedMarkerPrinter struct{}

func (sanitizedMarkerPrinter) BeforePrefix(marker java.Marker, wrapper CommentWrapper) string {
	return ""
}

func (sanitizedMarkerPrinter) BeforeSyntax(marker java.Marker, wrapper CommentWrapper) string {
	return ""
}

func (sanitizedMarkerPrinter) AfterSyntax(marker java.Marker, wrapper CommentWrapper) string {
	return ""
}

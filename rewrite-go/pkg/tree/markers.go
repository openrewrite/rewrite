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

// Marker is metadata attached to an LST node without modifying the tree structure.
type Marker interface {
	ID() uuid.UUID
}

// Markers holds a collection of Marker instances attached to a tree node.
type Markers struct {
	ID      uuid.UUID
	Entries []Marker
}

// GenericMarker is a marker type for Java-side markers that the Go side
// doesn't have a native type for (e.g., RecipesThatMadeChanges, SearchResult).
// It preserves the marker data as opaque fields during RPC round-trips.
type GenericMarker struct {
	Ident      uuid.UUID
	JavaType   string         // Original Java class name for round-trip fidelity
	Data       map[string]any
}

func (m GenericMarker) ID() uuid.UUID { return m.Ident }

// SearchResultMarker represents a SearchResult marker from Java.
// It implements RpcCodec on the Java side, sending 2 sub-fields (id, description).
type SearchResultMarker struct {
	Ident       uuid.UUID
	Description string
}

func (m SearchResultMarker) ID() uuid.UUID { return m.Ident }

// FindMarker returns a pointer to the first marker of type T, or nil if not found.
func FindMarker[T any](markers Markers) *T {
	for _, m := range markers.Entries {
		if t, ok := m.(T); ok {
			return &t
		}
	}
	return nil
}

// HasMarker reports whether a marker of type T exists in the markers collection.
func HasMarker[T any](markers Markers) bool {
	for _, m := range markers.Entries {
		if _, ok := m.(T); ok {
			return true
		}
	}
	return false
}

// AddMarker returns a new Markers with the given marker appended.
func AddMarker(markers Markers, marker Marker) Markers {
	entries := make([]Marker, len(markers.Entries)+1)
	copy(entries, markers.Entries)
	entries[len(markers.Entries)] = marker
	return Markers{ID: markers.ID, Entries: entries}
}

// --- Cross-cutting markers used by the recipe framework ---

// SearchResult is a marker indicating that a search recipe found a match.
// It is rendered as a comment in printed output (e.g., /*~~(description)~~>*/).
type SearchResult struct {
	Ident       uuid.UUID
	Description string
}

func (s SearchResult) ID() uuid.UUID { return s.Ident }

// MarkupLevel indicates the severity of a Markup marker.
type MarkupLevel int

const (
	MarkupDebugLevel MarkupLevel = iota
	MarkupInfoLevel
	MarkupWarnLevel
	MarkupErrorLevel
)

// Markup is a marker for attaching diagnostic messages to LST nodes.
// It is rendered as a comment in printed output (e.g., /*~~(message: detail)~~>*/).
type Markup struct {
	Ident   uuid.UUID
	Level   MarkupLevel
	Message string
	Detail  string
}

func (m Markup) ID() uuid.UUID { return m.Ident }

// GoProject identifies the Go project (logical grouping of go.mod + .go
// files) a source belongs to. Mirrors org.openrewrite.golang.marker.GoProject
// on the Java side.
type GoProject struct {
	Ident       uuid.UUID
	ProjectName string
}

func (m GoProject) ID() uuid.UUID { return m.Ident }

// Semicolon marks a RightPadded element that is followed by an explicit
// `;` separator in the source — i.e. multiple statements on one line:
// `_ = 1; _ = 2`. Go inserts implicit semicolons at end-of-line so most
// files don't need this marker; it's only emitted when the source
// literally has a `;` between statements that the printer must
// reproduce.
//
// Mirrors org.openrewrite.java.marker.Semicolon on the Java side.
type Semicolon struct {
	Ident uuid.UUID
}

func (m Semicolon) ID() uuid.UUID { return m.Ident }

// NewSemicolon creates a Semicolon marker with a fresh UUID.
func NewSemicolon() Semicolon {
	return Semicolon{Ident: uuid.New()}
}

// NewGoProject creates a GoProject marker with a new UUID.
func NewGoProject(projectName string) GoProject {
	return GoProject{Ident: uuid.New(), ProjectName: projectName}
}

// NewSearchResult creates a SearchResult marker with a new UUID.
func NewSearchResult(description string) SearchResult {
	return SearchResult{Ident: uuid.New(), Description: description}
}

// NewMarkup creates a Markup marker with the given level and message.
func NewMarkup(level MarkupLevel, message, detail string) Markup {
	return Markup{Ident: uuid.New(), Level: level, Message: message, Detail: detail}
}

// FoundSearchResult attaches a SearchResult marker to the given Markers.
func FoundSearchResult(markers Markers, description string) Markers {
	return AddMarker(markers, NewSearchResult(description))
}

// MarkupWarn attaches a warning-level Markup marker to the given Markers.
func MarkupWarn(markers Markers, message string) Markers {
	return AddMarker(markers, NewMarkup(MarkupWarnLevel, message, ""))
}

// MarkupWarnDetail attaches a warning-level Markup marker with detail to the given Markers.
func MarkupWarnDetail(markers Markers, message, detail string) Markers {
	return AddMarker(markers, NewMarkup(MarkupWarnLevel, message, detail))
}

// MarkupInfo attaches an info-level Markup marker to the given Markers.
func MarkupInfo(markers Markers, message string) Markers {
	return AddMarker(markers, NewMarkup(MarkupInfoLevel, message, ""))
}

// MarkupError attaches an error-level Markup marker to the given Markers.
func MarkupError(markers Markers, message string) Markers {
	return AddMarker(markers, NewMarkup(MarkupErrorLevel, message, ""))
}

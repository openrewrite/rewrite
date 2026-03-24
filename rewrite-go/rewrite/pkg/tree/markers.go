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

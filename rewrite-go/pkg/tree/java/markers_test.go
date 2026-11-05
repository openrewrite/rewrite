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
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestFoundSearchResultKeepsAnEqualSearchResult(t *testing.T) {
	// given
	found := FoundSearchResult(EmptyMarkers, "found")

	// when
	again := FoundSearchResult(found, "found")

	// then
	assert.Same(t, found, again)
}

func TestFoundSearchResultAddsASearchResultWithAnotherDescription(t *testing.T) {
	// given
	found := FoundSearchResult(EmptyMarkers, "found")

	// when
	other := FoundSearchResult(found, "other")

	// then
	assert.Len(t, other.Entries(), 2)
}

func TestMarkupKeepsAnEqualMarkup(t *testing.T) {
	// given
	warned := MarkupWarnDetail(EmptyMarkers, "message", "detail")

	// when
	again := MarkupWarnDetail(warned, "message", "detail")

	// then
	assert.Same(t, warned, again)
}

func TestMarkupAddsAMarkupOfAnotherLevel(t *testing.T) {
	// given
	warned := MarkupWarn(EmptyMarkers, "message")

	// when
	errored := MarkupError(warned, "message")

	// then
	assert.Len(t, errored.Entries(), 2)
}

func TestFoundSearchResultKeepsAnEqualSearchResultHeldByPointer(t *testing.T) {
	// given
	found := AddMarker(EmptyMarkers, &SearchResult{Description: "found"})

	// when
	again := FoundSearchResult(found, "found")

	// then
	assert.Same(t, found, again)
}

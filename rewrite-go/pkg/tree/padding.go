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

// RightPadded wraps an element with trailing whitespace/comments.
// Used for list elements where whitespace appears after the element
// (e.g., before a comma or closing delimiter).
type RightPadded[T any] struct {
	Element T
	After   Space
	Markers Markers
}

// LeftPadded wraps an element with preceding whitespace/comments.
// Used for operators and delimiters where whitespace appears before
// the element (e.g., the space before an operator in a binary expression).
type LeftPadded[T any] struct {
	Before  Space
	Element T
	Markers Markers
}

// Container wraps a list of elements with surrounding whitespace,
// representing constructs like parenthesized argument lists or
// bracketed type parameters.
type Container[T any] struct {
	Before   Space
	Elements []RightPadded[T]
	Markers  Markers
}

// UnwrapRightPadded extracts the elements from a slice of RightPadded wrappers.
func UnwrapRightPadded[T any](padded []RightPadded[T]) []T {
	result := make([]T, len(padded))
	for i, rp := range padded {
		result[i] = rp.Element
	}
	return result
}

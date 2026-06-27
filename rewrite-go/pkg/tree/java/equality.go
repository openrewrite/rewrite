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

// These helpers back the OpenRewrite identity invariant: a withX method must
// return the receiver unchanged when the new value equals the current one, so
// that visiting a subtree that nothing changed yields the SAME pointer — which
// is how the engine detects "no change" (and keeps untouched files out of a
// recipe's results/patch). Go's value types (Space, Markers, slices, the padded
// wrappers) are not ==-comparable, so withX uses these instead of a bare ==.
//
// All of these are O(1) in the unchanged case: the visitor returns the same
// slice / element pointer when nothing changed, so equality bottoms out in
// header/pointer comparisons rather than deep traversal.

// SameSlice reports whether a and b are the same slice — same backing array and
// length. (Go slices aren't ==-comparable.)
func SameSlice[T any](a, b []T) bool {
	if len(a) != len(b) {
		return false
	}
	if len(a) == 0 {
		return true
	}
	return &a[0] == &b[0]
}

// SpaceEqual reports whether two Spaces are unchanged: equal whitespace and the
// same comments slice (the visitor preserves the comments slice when unchanged).
func SpaceEqual(a, b Space) bool {
	return a.Whitespace == b.Whitespace && SameSlice(a.Comments, b.Comments)
}

// MarkersEqual reports whether two Markers are unchanged: same ID and the same
// entries slice.
func MarkersEqual(a, b Markers) bool {
	return a.ID == b.ID && SameSlice(a.Entries, b.Entries)
}

// LeftPaddedEqual reports whether two LeftPadded values are unchanged.
func LeftPaddedEqual[T comparable](a, b LeftPadded[T]) bool {
	return a.Element == b.Element && SpaceEqual(a.Before, b.Before) && MarkersEqual(a.Markers, b.Markers)
}

// RightPaddedEqual reports whether two RightPadded values are unchanged.
func RightPaddedEqual[T comparable](a, b RightPadded[T]) bool {
	return a.Element == b.Element && SpaceEqual(a.After, b.After) && MarkersEqual(a.Markers, b.Markers)
}

// ContainerEqual reports whether two Containers are unchanged: same before
// space, markers, and the same elements slice.
func ContainerEqual[T any](a, b Container[T]) bool {
	return SpaceEqual(a.Before, b.Before) && MarkersEqual(a.Markers, b.Markers) && SameSlice(a.Elements, b.Elements)
}

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

package format

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// eachPadded applies f to each element of a list in order, and copies the list
// only when f changes an element, so that a list nothing changed keeps its
// identity.
func eachPadded[T java.Tree](elements []java.RightPadded[T], f func(java.RightPadded[T]) java.RightPadded[T]) []java.RightPadded[T] {
	var out []java.RightPadded[T]
	for i, rp := range elements {
		next := f(rp)
		if out == nil {
			if any(next.Element) == any(rp.Element) && java.SpaceEqual(next.After, rp.After) &&
				java.MarkersEqual(next.Markers, rp.Markers) {
				continue
			}
			out = append([]java.RightPadded[T](nil), elements...)
		}
		out[i] = next
	}
	if out == nil {
		return elements
	}
	return out
}

// unchangedOr returns s when next lays out the same text, so that a Space a
// pass rebuilt without changing keeps its identity.
func unchangedOr(s, next java.Space) java.Space {
	if spaceContentEqual(s, next) {
		return s
	}
	return next
}

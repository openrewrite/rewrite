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
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// RemoveTrailingWhitespaceVisitor strips trailing space/tab characters
// from the end of each line inside `Space.Whitespace` fields. Indent
// whitespace at the start of the next line is preserved.
//
// Input:  "foo \t\n  bar"
// Output: "foo\n  bar"
type RemoveTrailingWhitespaceVisitor struct {
	visitor.GoVisitor
	stopAfterTracker
}

// NewRemoveTrailingWhitespaceVisitor returns a visitor configured with
// the given stopAfter bound. Pass nil to format the entire visited tree.
func NewRemoveTrailingWhitespaceVisitor(stopAfter tree.Tree) *RemoveTrailingWhitespaceVisitor {
	return visitor.Init(&RemoveTrailingWhitespaceVisitor{
		stopAfterTracker: stopAfterTracker{stopAfter: stopAfter},
	})
}

func (v *RemoveTrailingWhitespaceVisitor) Visit(t tree.Tree, p any) tree.Tree {
	if v.shouldHalt() {
		return t
	}
	out := v.GoVisitor.Visit(t, p)
	v.noteVisited(t)
	return out
}

func (v *RemoveTrailingWhitespaceVisitor) VisitSpace(s tree.Space, p any) tree.Space {
	if s.Whitespace == "" || !strings.ContainsAny(s.Whitespace, " \t") {
		return s
	}
	s.Whitespace = stripTrailingPerLine(s.Whitespace)
	return s
}

// stripTrailingPerLine removes trailing space/tab characters from each
// line. The trailing portion of the LAST line is preserved because it's
// the leading indent of whatever syntax follows.
func stripTrailingPerLine(ws string) string {
	if !strings.Contains(ws, "\n") {
		return ws
	}
	lines := strings.Split(ws, "\n")
	for i := 0; i < len(lines)-1; i++ {
		lines[i] = strings.TrimRight(lines[i], " \t")
	}
	return strings.Join(lines, "\n")
}

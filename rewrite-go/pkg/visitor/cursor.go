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

package visitor

import "github.com/openrewrite/rewrite/rewrite-go/pkg/tree"

// Cursor tracks the path from root to the currently visited node,
// providing context during tree traversal.
type Cursor struct {
	parent *Cursor
	value  tree.Tree
}

func NewCursor(parent *Cursor, value tree.Tree) *Cursor {
	return &Cursor{parent: parent, value: value}
}

func (c *Cursor) Parent() *Cursor { return c.parent }
func (c *Cursor) Value() tree.Tree { return c.value }

// FirstEnclosing walks up the cursor chain to find the first ancestor
// matching the given type.
func FirstEnclosing[T tree.Tree](c *Cursor) (T, bool) {
	for cur := c.parent; cur != nil; cur = cur.parent {
		if v, ok := cur.value.(T); ok {
			return v, true
		}
	}
	var zero T
	return zero, false
}

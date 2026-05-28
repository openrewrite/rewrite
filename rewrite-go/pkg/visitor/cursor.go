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

// Cursor tracks the path from root to the currently visited node and
// carries a per-frame message map so passes can stash state for
// ancestors / descendants. Mirrors org.openrewrite.Cursor in Java.
type Cursor struct {
	parent   *Cursor
	value    tree.Tree
	messages map[string]any
}

func NewCursor(parent *Cursor, value tree.Tree) *Cursor {
	return &Cursor{parent: parent, value: value}
}

func (c *Cursor) Parent() *Cursor  { return c.parent }
func (c *Cursor) Value() tree.Tree { return c.value }

// PutMessage stores a value on this cursor's frame, keyed by name.
// Mirrors Java Cursor.putMessage(String, Object).
func (c *Cursor) PutMessage(key string, value any) {
	if c.messages == nil {
		c.messages = make(map[string]any, 4)
	}
	c.messages[key] = value
}

// GetMessage returns the value previously stored on THIS frame, or nil
// if no value exists. Does not walk up the parent chain. Mirrors Java
// Cursor.getMessage(String).
func (c *Cursor) GetMessage(key string) any {
	if c.messages == nil {
		return nil
	}
	return c.messages[key]
}

// GetNearestMessage walks up the cursor chain (starting from this frame)
// returning the first matching value. Returns nil if no frame has a
// value for the key. Mirrors Java Cursor.getNearestMessage(String).
func (c *Cursor) GetNearestMessage(key string) any {
	for cur := c; cur != nil; cur = cur.parent {
		if cur.messages != nil {
			if v, ok := cur.messages[key]; ok {
				return v
			}
		}
	}
	return nil
}

// GetNearestMessageOrDefault is GetNearestMessage with a fallback when
// no frame holds a value for the key.
func (c *Cursor) GetNearestMessageOrDefault(key string, defaultValue any) any {
	if v := c.GetNearestMessage(key); v != nil {
		return v
	}
	return defaultValue
}

// PollNearestMessage walks up the chain like GetNearestMessage, but
// REMOVES the value from the frame where it was found. Returns nil if
// no frame had it. Mirrors Java Cursor.pollNearestMessage(String).
func (c *Cursor) PollNearestMessage(key string) any {
	for cur := c; cur != nil; cur = cur.parent {
		if cur.messages != nil {
			if v, ok := cur.messages[key]; ok {
				delete(cur.messages, key)
				return v
			}
		}
	}
	return nil
}

// ComputeMessageIfAbsent returns the value for the key on THIS frame,
// computing and storing it via the supplier if absent. Mirrors Java
// Cursor.computeMessageIfAbsent.
func (c *Cursor) ComputeMessageIfAbsent(key string, supplier func() any) any {
	if c.messages == nil {
		c.messages = make(map[string]any, 4)
	}
	if v, ok := c.messages[key]; ok {
		return v
	}
	v := supplier()
	c.messages[key] = v
	return v
}

// PutMessageOnFirstEnclosing walks up looking for the first ancestor
// whose value matches the predicate, and stores the message on that
// frame. No-op if no ancestor matches. Mirrors Java
// Cursor.putMessageOnFirstEnclosing(Class, String, Object), generalized
// to a predicate so callers can match on any type or condition.
func (c *Cursor) PutMessageOnFirstEnclosing(match func(t tree.Tree) bool, key string, value any) {
	for cur := c; cur != nil; cur = cur.parent {
		if cur.value != nil && match(cur.value) {
			cur.PutMessage(key, value)
			return
		}
	}
}

// BuildChain constructs a cursor chain from a list of tree values, root first.
// Returns nil for an empty input. Used by the RPC layer to reconstruct the
// cursor from a Visit request's `cursor` field (a list of tree IDs whose
// values have already been fetched in order).
func BuildChain(values []tree.Tree) *Cursor {
	var c *Cursor
	for _, v := range values {
		c = NewCursor(c, v)
	}
	return c
}

// FirstEnclosing walks up the cursor chain to find the first ancestor
// matching the given type. The cursor itself is not considered — only ancestors.
func FirstEnclosing[T tree.Tree](c *Cursor) (T, bool) {
	for cur := c.parent; cur != nil; cur = cur.parent {
		if v, ok := cur.value.(T); ok {
			return v, true
		}
	}
	var zero T
	return zero, false
}

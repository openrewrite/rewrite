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

package rpc

import (
	"fmt"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

var defaultReceiver = NewGoReceiver()

// ReceiveQueue deserializes RpcObjectData messages from the RPC channel.
type ReceiveQueue struct {
	batch []RpcObjectData
	refs  map[int]any
	pull  func() []RpcObjectData
}

// NewReceiveQueue creates a new ReceiveQueue.
func NewReceiveQueue(refs map[int]any, pull func() []RpcObjectData) *ReceiveQueue {
	return &ReceiveQueue{
		refs: refs,
		pull: pull,
	}
}

// PeekBatch returns the current batch without consuming. Useful for checking
// if END_OF_OBJECT is waiting without triggering a new fetch.
func (q *ReceiveQueue) PeekBatch() []RpcObjectData {
	return q.batch
}

// Take returns the next message from the queue, pulling a new batch if needed.
func (q *ReceiveQueue) Take() RpcObjectData {
	if len(q.batch) == 0 {
		q.batch = q.pull()
	}
	msg := q.batch[0]
	q.batch = q.batch[1:]
	return msg
}

// Receive reads the next value from the queue.
// If onChange is non-nil, it's called for ADD/CHANGE states to deserialize nested fields.
func (q *ReceiveQueue) Receive(before any, onChange func(any) any) any {
	msg := q.Take()
	var ref *int

	switch msg.State {
	case NoChange:
		return before
	case Delete:
		return nil
	case Add:
		ref = msg.Ref
		if ref != nil && msg.ValueType == nil && msg.Value == nil {
			// Pure reference lookup
			if v, ok := q.refs[*ref]; ok {
				return v
			}
			panic(fmt.Sprintf("received reference to unknown object: %d", *ref))
		}
		// New object or forward declaration
		if msg.ValueType == nil {
			before = msg.Value
		} else {
			before = newObj(*msg.ValueType)
		}
		if ref != nil {
			// Store before deserialization to handle cycles
			q.refs[*ref] = before
		}
		// Intentional fall-through to CHANGE
		fallthrough
	case Change:
		var after any
		if onChange != nil {
			after = onChange(before)
		} else if !isNilValue(before) && getValueType(before) != nil {
			if t, ok := before.(tree.Tree); ok {
				after = defaultReceiver.Visit(t, q)
			} else {
				after = before
			}
		} else if msg.Value != nil {
			after = msg.Value
		} else {
			after = before
		}
		if ref != nil {
			q.refs[*ref] = after
		}
		return after
	case EndOfObject:
		// Sentinel from multi-batch GetObject; push back and return before unchanged
		q.batch = append([]RpcObjectData{msg}, q.batch...)
		return before
	default:
		panic(fmt.Sprintf("unsupported state: %v", msg.State))
	}
}

// ReceiveAndGet reads a value and applies a mapping function for ADD/CHANGE.
func (q *ReceiveQueue) ReceiveAndGet(before any, mapping func(any) any) any {
	after := q.Receive(before, nil)
	if after != nil && after != before {
		return mapping(after)
	}
	return after
}

// ReceiveList reads a list from the queue with position-based tracking.
func (q *ReceiveQueue) ReceiveList(before []any, onChange func(any) any) []any {
	msg := q.Take()

	switch msg.State {
	case NoChange:
		return before
	case Delete:
		return nil
	case Add:
		before = []any{}
		fallthrough
	case Change:
		// Next message contains positions
		posMsg := q.Take()
		if posMsg.State != Change {
			panic(fmt.Sprintf("expected CHANGE with positions, got %v (value=%v, valueType=%v)", posMsg.State, posMsg.Value, posMsg.ValueType))
		}
		positionsRaw, ok := posMsg.Value.([]any)
		if !ok {
			panic(fmt.Sprintf("expected []any positions, got %T", posMsg.Value))
		}
		after := make([]any, len(positionsRaw))
		for i, posRaw := range positionsRaw {
			pos := toInt(posRaw)
			var beforeItem any
			if pos >= 0 && before != nil && pos < len(before) {
				beforeItem = before[pos]
			}
			after[i] = q.Receive(beforeItem, onChange)
		}
		return after
	case EndOfObject:
		// Sentinel from multi-batch GetObject; push back and return before unchanged
		q.batch = append([]RpcObjectData{msg}, q.batch...)
		return before
	default:
		panic(fmt.Sprintf("unsupported state for list: %v", msg.State))
	}
}

// toInt converts a value to int, handling both int and float64 (from JSON).
func toInt(v any) int {
	switch n := v.(type) {
	case int:
		return n
	case int64:
		return int(n)
	case float64:
		return int(n)
	default:
		panic(fmt.Sprintf("cannot convert %T to int", v))
	}
}

// Factory registry for creating empty instances by Java class name.
var factories = map[string]func() any{}

// RegisterFactory registers a factory for creating instances of a Java class.
func RegisterFactory(javaClassName string, factory func() any) {
	factories[javaClassName] = factory
}

// newObj creates a new empty instance by Java class name.
// Unknown marker types are treated as GenericMarker to avoid panics
// from markers added in newer versions of rewrite-core.
func newObj(javaClassName string) any {
	if factory, ok := factories[javaClassName]; ok {
		return factory()
	}
	// Unknown marker types — create a GenericMarker with JavaType preserved.
	if strings.Contains(javaClassName, "marker") || strings.Contains(javaClassName, "Marker") {
		return tree.GenericMarker{JavaType: javaClassName}
	}
	panic(fmt.Sprintf("no factory registered for type: %s", javaClassName))
}

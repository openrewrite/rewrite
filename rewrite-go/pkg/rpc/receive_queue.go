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

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

var defaultReceiver = NewGoReceiver()

type ReceiveQueue struct {
	batch []RpcObjectData
	refs  map[int]any
	pull  func() []RpcObjectData
}

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
			// Hydrate GenericMarker.Data from the inline value when the
			// sender shipped a codec-less marker as `{ADD, valueType, value=map}`
			// (matches what every other language's send queue does). Without
			// this, the marker's fields would be silently dropped.
			if gm, ok := before.(java.GenericMarker); ok && !hasGenericMarkerCodec(gm.JavaType) {
				if dataMap, ok := msg.Value.(map[string]any); ok {
					gm.Data = dataMap
					if idStr, ok := dataMap["id"].(string); ok {
						if parsed, err := uuid.Parse(idStr); err == nil {
							gm.Ident = parsed
						}
					}
					before = gm
				}
			}
		}
		if ref != nil {
			// Store before deserialization to handle cycles
			q.refs[*ref] = before
		}
		// Intentional fall-through to CHANGE
		fallthrough
	case Change:
		// If the receiver has no baseline `before` but the sender provided a
		// concrete type, materialize a fresh instance so the codec/onChange
		// can populate its sub-fields. Without this, callers that pass
		// before=nil drop every sub-field message of a CHANGE-typed object,
		// silently desyncing the wire.
		if isNilValue(before) && msg.ValueType != nil {
			before = newObj(*msg.ValueType)
		}
		var after any
		if onChange != nil {
			after = onChange(before)
		} else if !isNilValue(before) && getValueType(before) != nil {
			if t, ok := before.(java.Tree); ok {
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

func (q *ReceiveQueue) ReceiveAndGet(before any, mapping func(any) any) any {
	after := q.Receive(before, nil)
	if after != nil && after != before {
		return mapping(after)
	}
	return after
}

// Typed free-function wrappers around Receive. The Java/TS/Python receivers expose a
// generic q.receive(before, onChange) that returns the value already typed; Go forbids
// type-parameterized methods, so the typed layer lives in these free functions instead.

// receiveValue receives a field that needs a deserialization closure, returning the
// typed value directly (the zero value — nil for pointer/interface T — on delete) so
// call sites are a single branch-free assignment.
//
// onChange receives the prior value already typed as T (receiveValue does the inbound
// cast once) and returns the deserialized value; its result is narrowed back to T here,
// so closure bodies need no casts, e.g.:
//
//	gs.Expr = receiveValue(q, gs.Expr, func(e java.Expression) any { return r.Visit(e, q) })
//	b.End   = receiveValue(q, b.End,   func(s java.Space) any { return receiveSpace(s, q) })
//
// Semantics mirror Java's RpcReceiveQueue.receive: NO_CHANGE returns `before`, DELETE
// returns the zero value (nil for the pointer/interface T of nullable fields — Java's
// `return null`), ADD/CHANGE returns the deserialized value. Receive yields nil only on
// DELETE or a nil `before`, so the zero return never produces a bogus empty value for the
// mandatory value-typed fields (which are never deleted).
func receiveValue[T any](q *ReceiveQueue, before T, onChange func(T) any) T {
	result := q.Receive(before, func(v any) any { return onChange(v.(T)) })
	if result == nil {
		var zero T
		return zero
	}
	return result.(T)
}

// receiveScalar receives a simple leaf value (no nested deserialization, hence a nil
// onChange) and applies convertTo to bridge JSON's numeric/string representations.
func receiveScalar[T any](q *ReceiveQueue, before T) T {
	result := q.Receive(before, nil)
	if result == nil {
		var zero T
		return zero
	}
	return convertTo[T](result)
}

func convertTo[T any](v any) T {
	if t, ok := v.(T); ok {
		return t
	}
	// Handle float64 -> int64 conversion (common with JSON)
	var zero T
	switch any(zero).(type) {
	case int64:
		switch n := v.(type) {
		case float64:
			return any(int64(n)).(T)
		case int:
			return any(int64(n)).(T)
		}
	case string:
		if s, ok := v.(string); ok {
			return any(s).(T)
		}
	}
	return v.(T)
}

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
		return java.GenericMarker{JavaType: javaClassName}
	}
	panic(fmt.Sprintf("no factory registered for type: %s", javaClassName))
}

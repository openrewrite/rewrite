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
	"reflect"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

var defaultSender = NewGoSender()

// SendQueue serializes objects into RpcObjectData messages for RPC transmission.
// It tracks refs for deduplication and maintains a "before" state for delta encoding.
type SendQueue struct {
	batchSize int
	batch     []RpcObjectData
	drain     func([]RpcObjectData)
	refs      map[uintptr]int // pointer identity -> ref number
	before    any
}

// NewSendQueue creates a new SendQueue.
func NewSendQueue(batchSize int, drain func([]RpcObjectData), refs map[uintptr]int) *SendQueue {
	return &SendQueue{
		batchSize: batchSize,
		batch:     make([]RpcObjectData, 0, batchSize),
		drain:     drain,
		refs:      refs,
	}
}

// Put adds a message to the batch, flushing if the batch is full.
func (q *SendQueue) Put(data RpcObjectData) {
	q.batch = append(q.batch, data)
	if len(q.batch) == q.batchSize {
		q.Flush()
	}
}

// Flush sends the accumulated batch and clears it.
func (q *SendQueue) Flush() {
	if len(q.batch) == 0 {
		return
	}
	out := make([]RpcObjectData, len(q.batch))
	copy(out, q.batch)
	q.drain(out)
	q.batch = q.batch[:0]
}

// GetAndSend extracts a value from parent (and before), compares them, and sends the delta.
func (q *SendQueue) GetAndSend(parent any, getter func(any) any, onChange func(any)) {
	after := getter(parent)
	var before any
	if q.before != nil {
		before = getter(q.before)
	}
	q.Send(after, before, onChange)
}

// GetAndSendList extracts a list from parent and sends it with position tracking.
func (q *SendQueue) GetAndSendList(parent any, getter func(any) []any, id func(any) any, onChange func(any)) {
	q.getAndSendList(parent, getter, id, onChange, false)
}

// GetAndSendListAsRef is like GetAndSendList but wraps items in ref tracking.
func (q *SendQueue) GetAndSendListAsRef(parent any, getter func(any) []any, id func(any) any, onChange func(any)) {
	q.getAndSendList(parent, getter, id, onChange, true)
}

func (q *SendQueue) getAndSendList(parent any, getter func(any) []any, id func(any) any, onChange func(any), asRef bool) {
	var after []any
	if parent != nil {
		after = getter(parent)
	}
	var before []any
	if q.before != nil {
		before = getter(q.before)
	}
	q.sendList(after, before, id, onChange, asRef)
}

// Send compares after and before values and emits the appropriate state message.
func (q *SendQueue) Send(after, before any, onChange func(any)) {
	afterVal := GetValue(after)
	beforeVal := GetValue(before)

	if sameIdentity(beforeVal, afterVal) {
		q.Put(RpcObjectData{State: NoChange})
	} else if isNilValue(beforeVal) || (!isNilValue(afterVal) && !sameType(afterVal, beforeVal)) {
		q.add(after, onChange)
	} else if isNilValue(afterVal) {
		q.Put(RpcObjectData{State: Delete})
	} else {
		vt := getValueType(afterVal)
		var val any
		if onChange == nil && vt == nil {
			val = afterVal
		}
		q.Put(RpcObjectData{State: Change, ValueType: vt, Value: val})
		q.doChange(afterVal, beforeVal, onChange)
	}
}

func (q *SendQueue) sendList(after, before []any, id func(any) any, onChange func(any), asRef bool) {
	q.Send(anySlice(after), anySlice(before), func(_ any) {
		if after == nil {
			return
		}

		// Build before index map
		beforeIdx := make(map[any]int)
		if before != nil {
			for i, b := range before {
				beforeIdx[id(b)] = i
			}
		}

		// Send positions
		positions := make([]any, len(after))
		for i, a := range after {
			if pos, ok := beforeIdx[id(a)]; ok {
				positions[i] = pos
			} else {
				positions[i] = AddedListItem
			}
		}
		q.Put(RpcObjectData{State: Change, Value: positions})

		// Send each item
		for _, a := range after {
			aid := id(a)
			pos, existed := beforeIdx[aid]
			var onChangeRun func(any)
			if onChange != nil {
				item := a
				onChangeRun = func(_ any) { onChange(item) }
			}
			if !existed {
				// New item
				if asRef {
					q.add(AsRef(a), onChangeRun)
				} else {
					q.add(a, onChangeRun)
				}
			} else {
				var aBefore any
				if before != nil && pos >= 0 {
					aBefore = before[pos]
				}
				if sameIdentity(aBefore, a) {
					q.Put(RpcObjectData{State: NoChange})
				} else if isNilValue(aBefore) || !sameType(a, aBefore) {
					if asRef {
						q.add(AsRef(a), onChangeRun)
					} else {
						q.add(a, onChangeRun)
					}
				} else {
					vt := getValueType(a)
					q.Put(RpcObjectData{State: Change, ValueType: vt})
					q.doChange(a, aBefore, onChangeRun)
				}
			}
		}
	})
}

func (q *SendQueue) add(after any, onChange func(any)) {
	afterVal := GetValue(after)
	if isNilValue(afterVal) {
		panic("add called with nil value")
	}

	var ref *int
	if IsRef(after) {
		ptr := ptrKey(afterVal)
		if ptr != 0 { // Only track refs for pointer types (value types all return 0)
			if existingRef, ok := q.refs[ptr]; ok {
				// Already sent - emit pure ref
				q.Put(RpcObjectData{State: Add, Ref: &existingRef})
				return
			}
			r := len(q.refs) + 1
			q.refs[ptr] = r
			ref = &r
		}
	}

	vt := getValueType(afterVal)
	var val any
	if onChange == nil && vt == nil {
		val = afterVal
	}
	q.Put(RpcObjectData{State: Add, ValueType: vt, Value: val, Ref: ref})
	q.doChange(afterVal, nil, onChange)
}

func (q *SendQueue) doChange(after, before any, onChange func(any)) {
	lastBefore := q.before
	q.before = before
	defer func() { q.before = lastBefore }()

	if onChange != nil && !isNilValue(after) {
		onChange(after)
	} else if onChange == nil && !isNilValue(after) && getValueType(after) != nil {
		defaultSender.Visit(after, q)
	}
}

// ptrKey returns a uintptr for use as a map key, based on pointer identity.
func ptrKey(v any) uintptr {
	if v == nil {
		return 0
	}
	rv := reflect.ValueOf(v)
	if rv.Kind() == reflect.Ptr || rv.Kind() == reflect.Interface {
		return rv.Pointer()
	}
	// For non-pointer types, we can't track by identity
	return 0
}

// sameIdentity checks if two values are the same object (pointer identity).
func sameIdentity(a, b any) bool {
	aNil := isNilValue(a)
	bNil := isNilValue(b)
	if aNil && bNil {
		return true
	}
	if aNil || bNil {
		return false
	}
	rv1 := reflect.ValueOf(a)
	rv2 := reflect.ValueOf(b)
	if rv1.Kind() == reflect.Ptr && rv2.Kind() == reflect.Ptr {
		return rv1.Pointer() == rv2.Pointer()
	}
	// For non-pointer value types, use reflect to safely compare.
	// Slices are compared by header identity (pointer + length), not content,
	// because tree visitors always create new slices even for unchanged subtrees.
	rv1 = reflect.ValueOf(a)
	rv2 = reflect.ValueOf(b)
	if rv1.Kind() == reflect.Slice && rv2.Kind() == reflect.Slice {
		return rv1.Pointer() == rv2.Pointer() && rv1.Len() == rv2.Len()
	}
	// For structs with uncomparable fields (e.g., Space containing []Comment),
	// use DeepEqual to avoid panics.
	return reflect.DeepEqual(a, b)
}

// sameType checks if two values have the same concrete type.
func sameType(a, b any) bool {
	if isNilValue(a) || isNilValue(b) {
		return false
	}
	return reflect.TypeOf(a) == reflect.TypeOf(b)
}

// anySlice converts a []any to an any (to distinguish nil slice from empty slice).
func anySlice(s []any) any {
	if s == nil {
		return nil
	}
	return s
}

// getValueType returns the Java class name for a value, or nil if it's a primitive.
func getValueType(v any) *string {
	if v == nil {
		return nil
	}
	t := reflect.TypeOf(v)
	// Exclude primitive types, strings, and slices (matching Java's exclusions)
	switch t.Kind() {
	case reflect.Bool, reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64,
		reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64,
		reflect.Float32, reflect.Float64, reflect.String:
		return nil
	case reflect.Slice:
		return nil
	}
	if vt, ok := valueTypeMap[t]; ok {
		return &vt
	}
	// GenericMarker carries the original Java class name
	if gm, ok := v.(tree.GenericMarker); ok && gm.JavaType != "" {
		return &gm.JavaType
	}
	// Check padding types (Go generics have no reflect.Name())
	return getValueTypeForPadding(v)
}

// valueTypeMap maps Go types to their Java class names for RPC wire format.
var valueTypeMap = map[reflect.Type]string{}

// RegisterValueType registers a Go type -> Java class name mapping for RPC serialization.
func RegisterValueType(goType reflect.Type, javaClassName string) {
	valueTypeMap[goType] = javaClassName
}

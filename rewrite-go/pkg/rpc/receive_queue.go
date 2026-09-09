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

	typePool map[string]java.JavaType
}

func (q *ReceiveQueue) WithTypePool(pool map[string]java.JavaType) *ReceiveQueue {
	q.typePool = pool
	return q
}

func (q *ReceiveQueue) internType(t java.JavaType) java.JavaType {
	if q.typePool == nil {
		return t
	}
	sig := java.TypeSignature(t)
	if sig == "" {
		return t
	}
	if c, ok := q.typePool[sig]; ok {
		return c
	}
	q.typePool[sig] = t
	return t
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

func (q *ReceiveQueue) peek() RpcObjectData {
	if len(q.batch) == 0 {
		q.batch = q.pull()
	}
	return q.batch[0]
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
		} else if obj, known := newObjIfKnown(*msg.ValueType); known {
			before = obj
		} else if scalar, ok := inlineScalar(msg.Value); ok {
			before = scalar
		} else {
			panic(missingCodec(*msg.ValueType))
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
			if obj, known := newObjIfKnown(*msg.ValueType); known {
				before = obj
			} else if scalar, ok := inlineScalar(msg.Value); ok {
				before = scalar
			} else {
				panic(missingCodec(*msg.ValueType))
			}
		}
		before = hydrateGenericMarker(before, msg.Value)
		// The remote inlines a value only when it has no codec for the type, so a typed ADD with
		// no value means sub-field messages follow. Anything that reaches a branch below without
		// consuming them leaves the queue desynchronized, and Go is the one peer where that used
		// to happen silently — every other receiver already fails loudly here.
		codecExpected := msg.State == Add && msg.ValueType != nil && msg.Value == nil
		var after any
		if onChange != nil {
			after = onChange(before)
		} else if !isNilValue(before) && getValueType(before) != nil {
			if t, ok := before.(java.Tree); ok {
				after = defaultReceiver.Visit(t, q)
			} else if msg.Value != nil {
				// A codec-less value-typed scalar (e.g. an operator enum)
				after = msg.Value
			} else if codecExpected {
				panic(missingCodec(*msg.ValueType))
			} else {
				after = before
			}
		} else if msg.Value != nil {
			after = msg.Value
		} else if codecExpected {
			panic(missingCodec(*msg.ValueType))
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

// inlineScalar reports whether a type this side does not model arrived as a
// value it can carry verbatim. A scalar — a big integer, a timestamp —
// round-trips unchanged; a structured payload would lose its type on the way
// back, and no value at all means the remote has a codec this side lacks.
func inlineScalar(v any) (any, bool) {
	switch v.(type) {
	case nil, map[string]any, []any:
		return nil, false
	}
	return v, true
}

func missingCodec(valueType string) string {
	return fmt.Sprintf("no RPC codec registered on the Go side for %q. "+
		"The remote side has a codec and sent property messages that will not be consumed, "+
		"causing RPC queue desynchronization.", valueType)
}

// hydrateGenericMarker applies a message's inline data map to a codec-less
// GenericMarker. Such markers travel as `{valueType, value=map}` with no
// sub-field messages (matching every other language's send queue), so this is
// the only place their fields cross the wire.
func hydrateGenericMarker(before any, value any) any {
	if gm, ok := before.(java.GenericMarker); ok && !hasGenericMarkerCodec(gm.JavaType) {
		if dataMap, ok := value.(map[string]any); ok {
			gm.Data = dataMap
			if idStr, ok := dataMap["id"].(string); ok {
				if parsed, err := uuid.Parse(idStr); err == nil {
					gm.Ident = parsed
				}
			}
			return gm
		}
	}
	return before
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
	// A number arrives in the Go type its JSON shape implies (see decodeNumber),
	// which need not be the one the field it fills holds.
	var zero T
	switch any(zero).(type) {
	case int64:
		switch n := v.(type) {
		case int:
			return any(int64(n)).(T)
		case float64:
			return any(int64(n)).(T)
		}
	case int:
		switch n := v.(type) {
		case int64:
			return any(int(n)).(T)
		case float64:
			return any(int(n)).(T)
		}
	case float64:
		switch n := v.(type) {
		case int:
			return any(float64(n)).(T)
		case int64:
			return any(float64(n)).(T)
		}
	case string:
		if s, ok := v.(string); ok {
			return any(s).(T)
		}
	}
	return v.(T)
}

func (q *ReceiveQueue) ReceiveList(before []any, onChange func(any) any) []any {
	return receiveTypedList(q, before, onChange, func(v any) any { return v })
}

// receiveTypedList is the generic, allocation-lean counterpart to ReceiveList.
func receiveTypedList[T any](q *ReceiveQueue, before []T, onChange func(any) any, coerce func(any) T) []T {
	msg := q.Take()

	switch msg.State {
	case NoChange:
		return before
	case Delete:
		return nil
	case Add:
		before = []T{}
		fallthrough
	case Change:
		posMsg := q.Take()
		if posMsg.State != Change {
			panic(fmt.Sprintf("expected CHANGE with positions, got %v (value=%v, valueType=%v)", posMsg.State, posMsg.Value, posMsg.ValueType))
		}
		positionsRaw, ok := posMsg.Value.([]any)
		if !ok {
			panic(fmt.Sprintf("expected []any positions, got %T", posMsg.Value))
		}
		after := make([]T, len(positionsRaw))
		for i, posRaw := range positionsRaw {
			pos := toInt(posRaw)
			hasBefore := pos >= 0 && before != nil && pos < len(before)
			// Unchanged elements — the common case, since a recipe touches few nodes —
			// are copied across in their static type T. Routing them through Receive
			// would box before[pos] into `any` (a heap allocation per element for the
			// non-pointer struct types these lists hold) only to hand the same value back.
			if hasBefore && q.peek().State == NoChange {
				q.Take()
				after[i] = before[pos]
				continue
			}
			if !hasBefore && q.peek().State == NoChange {
				// The sender diffed the edited tree against a baseline it believes
				// this side holds and shipped this element as NO_CHANGE — but our
				// baseline has nothing at this position, so its content never came
				// over the wire and cannot be reconstructed. Fail with the cause
				// named rather than letting the zero element nil-deref downstream in
				// coerceToStatementRP/coerceToExpressionRP (openrewrite/rewrite#8424).
				q.Take()
				panic(fmt.Sprintf("RPC baseline desync: NO_CHANGE list element at position %d has no baseline (before holds %d element(s)); the sender diffed against a tree this receiver never received", pos, len(before)))
			}
			var beforeItem any
			if hasBefore {
				beforeItem = before[pos]
			}
			after[i] = coerce(q.Receive(beforeItem, onChange))
		}
		return after
	case EndOfObject:
		q.batch = append([]RpcObjectData{msg}, q.batch...)
		return before
	default:
		panic(fmt.Sprintf("unsupported state for list: %v", msg.State))
	}
}

// receiveTypedListNonNil is receiveTypedList for fields (e.g. leadingAnnotations) that drop
// elements onChange narrowed to the zero T. It never mutates before: compaction only ever
// rebuilds a freshly allocated ADD/CHANGE slice, and the NO_CHANGE slice — which holds no
// dropped elements — is returned as is.
func receiveTypedListNonNil[T any](q *ReceiveQueue, before []T, onChange func(any) any, coerce func(any) T, isNil func(T) bool) []T {
	after := receiveTypedList(q, before, onChange, coerce)
	if after == nil {
		return nil
	}
	kept := 0
	for _, e := range after {
		if !isNil(e) {
			kept++
		}
	}
	if kept == len(after) {
		return after
	}
	out := make([]T, 0, kept)
	for _, e := range after {
		if !isNil(e) {
			out = append(out, e)
		}
	}
	return out
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

// newObjIfKnown creates a new empty instance by Java class name, reporting
// whether the name is one this side models. Unknown marker types are treated as
// GenericMarker so markers added in newer versions of rewrite-core still arrive.
func newObjIfKnown(javaClassName string) (any, bool) {
	if factory, ok := factories[javaClassName]; ok {
		return factory(), true
	}
	// Unknown marker types — create a GenericMarker with JavaType preserved.
	if strings.Contains(javaClassName, "marker") || strings.Contains(javaClassName, "Marker") {
		return java.GenericMarker{JavaType: javaClassName}, true
	}
	return nil, false
}

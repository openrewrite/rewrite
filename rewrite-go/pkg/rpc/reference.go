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
	"sync"
)

type Reference struct {
	Value any
}

// ReferenceStore assigns stable wire IDs to objects sent as references.
type ReferenceStore interface {
	GetOrCreate(obj any) (ref int, existed bool)
}

// ReferenceMap retains referenced objects for as long as their wire IDs are
// valid. Keeping the objects themselves as keys prevents a collected object's
// address from being reused for a different object while the remote side still
// has the old ID in its receive table.
type ReferenceMap struct {
	mu     sync.Mutex
	refs   map[any]int
	nextID int
}

func NewReferenceMap() *ReferenceMap {
	return &ReferenceMap{
		refs:   make(map[any]int),
		nextID: 1,
	}
}

func (m *ReferenceMap) GetOrCreate(obj any) (int, bool) {
	assertReferenceIdentity(obj)
	m.mu.Lock()
	defer m.mu.Unlock()
	if ref, ok := m.refs[obj]; ok {
		return ref, true
	}
	ref := m.nextID
	m.nextID++
	m.refs[obj] = ref
	return ref, false
}

func (m *ReferenceMap) Len() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return len(m.refs)
}

// Begin creates an isolated allocation transaction. References allocated by
// one transfer are visible within that transfer immediately, but become
// reusable by other transfers only after Commit. IDs are never reused after a
// rollback because the remote may already have received an earlier page that
// defined one of them.
func (m *ReferenceMap) Begin() *ReferenceTransaction {
	return &ReferenceTransaction{
		parent: m,
		refs:   make(map[any]int),
	}
}

type ReferenceTransaction struct {
	mu          sync.Mutex
	parent      *ReferenceMap
	refs        map[any]int
	allocations []any
	closed      bool
}

func (t *ReferenceTransaction) GetOrCreate(obj any) (int, bool) {
	assertReferenceIdentity(obj)
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.closed {
		panic("reference transaction is closed")
	}
	if ref, ok := t.refs[obj]; ok {
		return ref, true
	}

	t.parent.mu.Lock()
	if ref, ok := t.parent.refs[obj]; ok {
		t.parent.mu.Unlock()
		return ref, true
	}
	ref := t.parent.nextID
	t.parent.nextID++
	t.parent.mu.Unlock()

	t.refs[obj] = ref
	t.allocations = append(t.allocations, obj)
	return ref, false
}

func (t *ReferenceTransaction) Commit() {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.closed {
		return
	}

	t.parent.mu.Lock()
	for _, obj := range t.allocations {
		if _, exists := t.parent.refs[obj]; !exists {
			t.parent.refs[obj] = t.refs[obj]
		}
	}
	t.parent.mu.Unlock()
	t.close()
}

func (t *ReferenceTransaction) Rollback() {
	t.mu.Lock()
	defer t.mu.Unlock()
	if !t.closed {
		t.close()
	}
}

func (t *ReferenceTransaction) close() {
	t.closed = true
	t.refs = nil
	t.allocations = nil
}

func isReferenceIdentity(v any) bool {
	if v == nil {
		return false
	}
	rv := reflect.ValueOf(v)
	return rv.Kind() == reflect.Ptr && !rv.IsNil()
}

func assertReferenceIdentity(v any) {
	if !isReferenceIdentity(v) {
		panic("references require a non-nil pointer identity")
	}
}

// Returns nil if the value is nil (including typed nil pointers/interfaces).
func AsRef(v any) any {
	if isNilValue(v) {
		return nil
	}
	return &Reference{Value: v}
}

func isNilValue(v any) bool {
	if v == nil {
		return true
	}
	rv := reflect.ValueOf(v)
	switch rv.Kind() {
	case reflect.Ptr, reflect.Interface, reflect.Slice, reflect.Map, reflect.Chan, reflect.Func:
		return rv.IsNil()
	}
	return false
}

// GetValue unwraps a Reference, returning the inner value.
// If the argument is not a Reference, it is returned as-is.
func GetValue(maybeRef any) any {
	if ref, ok := maybeRef.(*Reference); ok {
		return ref.Value
	}
	return maybeRef
}

// GetValueNonNull unwraps a Reference and panics if the result is nil.
func GetValueNonNull(maybeRef any) any {
	v := GetValue(maybeRef)
	if v == nil {
		panic("expected non-nil value from reference")
	}
	return v
}

func IsRef(v any) bool {
	_, ok := v.(*Reference)
	return ok
}

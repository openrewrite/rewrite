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

func (m *ReferenceMap) deleteIfMatches(obj any, ref int) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if current, ok := m.refs[obj]; ok && current == ref {
		delete(m.refs, obj)
	}
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

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

import "reflect"

// Reference wraps a value to indicate it should be ref-tracked during RPC serialization.
type Reference struct {
	Value any
}

// AsRef wraps a value in a Reference for ref tracking.
// Returns nil if the value is nil (including typed nil pointers/interfaces).
func AsRef(v any) any {
	if isNilValue(v) {
		return nil
	}
	return &Reference{Value: v}
}

// isNilValue checks if a value is nil, handling both untyped nil and typed nil pointers/interfaces.
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

// IsRef reports whether the given value is a Reference.
func IsRef(v any) bool {
	_, ok := v.(*Reference)
	return ok
}

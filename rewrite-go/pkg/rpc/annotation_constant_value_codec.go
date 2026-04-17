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

package rpc

import (
	"fmt"
	"strconv"
)

// Wire codec for JavaType.Annotation.ElementValue constant values.
// JSON cannot distinguish e.g. Integer 42 from Long 42 or Character 'c' from
// String "c", so each constant is encoded as a tagged string on the wire:
// "<kind>:<lexical>" where <kind> is one of:
//   s (String), b (Boolean), i (Integer), l (Long), S (Short), B (Byte),
//   f (Float), d (Double), c (Character).
// A null constant is encoded as the literal string "n" (or nil on the wire —
// both are accepted on receive).
//
// Only the listed primitive-like values appear as constant values; class
// literals and enum constants flow through the ReferenceValue branch as
// JavaType references. Go receivers collapse all numeric kinds to int64 or
// float64 as appropriate (Go has no native byte/short/int/long distinction
// suitable for round-tripping back through JSON).

// EncodeAnnotationConstant encodes a single annotation element constant value
// to its tagged string representation. Returns nil for nil input.
func EncodeAnnotationConstant(value any) *string {
	if value == nil {
		return nil
	}
	var s string
	switch v := value.(type) {
	case string:
		s = "s:" + v
	case bool:
		if v {
			s = "b:true"
		} else {
			s = "b:false"
		}
	case int8:
		s = "B:" + strconv.FormatInt(int64(v), 10)
	case int16:
		s = "S:" + strconv.FormatInt(int64(v), 10)
	case int32:
		// Note: Go's rune is an alias for int32, so we can't distinguish between
		// a Java int and a Java char by Go type. Encoders that mean char must
		// pass a single-character string instead.
		s = "i:" + strconv.FormatInt(int64(v), 10)
	case int64:
		s = "l:" + strconv.FormatInt(v, 10)
	case int:
		s = "i:" + strconv.FormatInt(int64(v), 10)
	case float32:
		s = "f:" + strconv.FormatFloat(float64(v), 'g', -1, 32)
	case float64:
		s = "d:" + strconv.FormatFloat(v, 'g', -1, 64)
	default:
		panic(fmt.Sprintf("unsupported annotation constant value type: %T", value))
	}
	return &s
}

// DecodeAnnotationConstant decodes a tagged string back to its native Go type.
// Returns nil for nil or "n" input.
func DecodeAnnotationConstant(encoded *string) any {
	if encoded == nil || *encoded == "n" {
		return nil
	}
	e := *encoded
	if len(e) < 2 || e[1] != ':' {
		panic(fmt.Sprintf("malformed annotation constant value envelope: %q", e))
	}
	kind := e[0]
	body := e[2:]
	switch kind {
	case 's':
		return body
	case 'b':
		return body == "true"
	case 'i', 'S', 'B':
		n, err := strconv.ParseInt(body, 10, 32)
		if err != nil {
			panic(fmt.Sprintf("malformed annotation constant value envelope %q: %v", e, err))
		}
		return n
	case 'l':
		n, err := strconv.ParseInt(body, 10, 64)
		if err != nil {
			panic(fmt.Sprintf("malformed annotation constant value envelope %q: %v", e, err))
		}
		return n
	case 'f', 'd':
		f, err := strconv.ParseFloat(body, 64)
		if err != nil {
			panic(fmt.Sprintf("malformed annotation constant value envelope %q: %v", e, err))
		}
		return f
	case 'c':
		if len(body) == 0 {
			panic("malformed char envelope: empty body")
		}
		return rune(body[0])
	default:
		panic(fmt.Sprintf("unknown annotation constant value kind: %c", kind))
	}
}

// EncodeAnnotationConstantList encodes a list of annotation element constant
// values to their tagged string representations. Returns nil for nil input.
func EncodeAnnotationConstantList(values []any) []*string {
	if values == nil {
		return nil
	}
	out := make([]*string, len(values))
	for i, v := range values {
		out[i] = EncodeAnnotationConstant(v)
	}
	return out
}

// DecodeAnnotationConstantList decodes a list of tagged strings back to their
// native Go types. Returns nil for nil input.
func DecodeAnnotationConstantList(encoded []*string) []any {
	if encoded == nil {
		return nil
	}
	out := make([]any, len(encoded))
	for i, s := range encoded {
		out[i] = DecodeAnnotationConstant(s)
	}
	return out
}

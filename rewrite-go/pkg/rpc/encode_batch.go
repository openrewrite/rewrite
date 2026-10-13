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
	"encoding/json"
	"slices"
	"strconv"
	"unicode/utf8"
)

// AppendBatch appends a page's JSON encoding to dst, byte for byte as json.Marshal writes the
// same batch, without the per-value allocation reflection costs.
//
// A value of a type this does not recognise goes to json.Marshal, so an unexpected one is slower
// rather than wrong.
func AppendBatch(dst []byte, batch []RpcObjectData) ([]byte, error) {
	// A nil slice is null and an empty one is [], as json.Marshal distinguishes them. The
	// receiver reads null as "no page" rather than as a page of nothing.
	if batch == nil {
		return append(dst, "null"...), nil
	}
	dst = append(dst, '[')
	for i := range batch {
		if i > 0 {
			dst = append(dst, ',')
		}
		var err error
		if dst, err = appendMessage(dst, &batch[i]); err != nil {
			return dst, err
		}
	}
	return append(dst, ']'), nil
}

// Field order and omission follow the struct tags: byte-identical, not merely equivalent.
func appendMessage(dst []byte, m *RpcObjectData) ([]byte, error) {
	dst = append(dst, `{"state":`...)
	dst = appendJSONString(dst, m.State.String())
	if m.ValueType != nil {
		dst = append(dst, `,"valueType":`...)
		dst = appendJSONString(dst, *m.ValueType)
	}
	if m.Value != nil {
		dst = append(dst, `,"value":`...)
		var err error
		if dst, err = appendValue(dst, m.Value); err != nil {
			return dst, err
		}
	}
	if m.Ref != nil {
		dst = append(dst, `,"ref":`...)
		dst = strconv.AppendInt(dst, int64(*m.Ref), 10)
	}
	return append(dst, '}'), nil
}

func appendValue(dst []byte, value any) ([]byte, error) {
	switch v := value.(type) {
	case nil:
		return append(dst, "null"...), nil
	case string:
		return appendJSONString(dst, v), nil
	case bool:
		if v {
			return append(dst, "true"...), nil
		}
		return append(dst, "false"...), nil
	case int:
		return strconv.AppendInt(dst, int64(v), 10), nil
	case int64:
		return strconv.AppendInt(dst, v, 10), nil
	case json.Number:
		// What wireNumber produces for every float. json.Marshal rejects a malformed literal
		// instead of writing it through, so an invalid one goes there for the same error.
		if !validJSONNumber(string(v)) {
			return appendMarshaled(dst, value)
		}
		return append(dst, v...), nil
	case []any:
		// A nil slice or map is null, not empty delimiters — the type assertion above matches a
		// nil value of the type, so this cannot be left to the loop.
		if v == nil {
			return append(dst, "null"...), nil
		}
		dst = append(dst, '[')
		for i, element := range v {
			if i > 0 {
				dst = append(dst, ',')
			}
			var err error
			if dst, err = appendValue(dst, element); err != nil {
				return dst, err
			}
		}
		return append(dst, ']'), nil
	case map[string]any:
		return appendJSONObject(dst, v)
	default:
		return appendMarshaled(dst, value)
	}
}

// Keys are sorted because json.Marshal sorts a map's keys, and a peer's wire test pins the order.
func appendJSONObject(dst []byte, fields map[string]any) ([]byte, error) {
	if fields == nil {
		return append(dst, "null"...), nil
	}
	keys := make([]string, 0, len(fields))
	for key := range fields {
		keys = append(keys, key)
	}
	slices.Sort(keys)

	dst = append(dst, '{')
	for i, key := range keys {
		if i > 0 {
			dst = append(dst, ',')
		}
		dst = appendJSONString(dst, key)
		dst = append(dst, ':')
		var err error
		if dst, err = appendValue(dst, fields[key]); err != nil {
			return dst, err
		}
	}
	return append(dst, '}'), nil
}

func appendMarshaled(dst []byte, value any) ([]byte, error) {
	encoded, err := json.Marshal(value)
	if err != nil {
		return dst, err
	}
	return append(dst, encoded...), nil
}

const hexDigits = "0123456789abcdef"

// appendJSONString writes a JSON string literal exactly as encoding/json does, which is not the
// same as writing a valid one: it escapes <, > and & so a document can be embedded in HTML, keeps
// U+2028 and U+2029 escaped so it stays valid JavaScript, leaves DEL alone, and replaces a byte
// that is not UTF-8 with the replacement character rather than failing.
func appendJSONString(dst []byte, s string) []byte {
	dst = append(dst, '"')
	written := 0
	for i := 0; i < len(s); {
		if b := s[i]; b < utf8.RuneSelf {
			if safeJSONByte(b) {
				i++
				continue
			}
			dst = append(dst, s[written:i]...)
			switch b {
			case '\\', '"':
				dst = append(dst, '\\', b)
			case '\b':
				dst = append(dst, '\\', 'b')
			case '\f':
				dst = append(dst, '\\', 'f')
			case '\n':
				dst = append(dst, '\\', 'n')
			case '\r':
				dst = append(dst, '\\', 'r')
			case '\t':
				dst = append(dst, '\\', 't')
			default:
				dst = append(dst, '\\', 'u', '0', '0', hexDigits[b>>4], hexDigits[b&0xF])
			}
			i++
			written = i
			continue
		}
		r, size := utf8.DecodeRuneInString(s[i:])
		switch {
		case r == utf8.RuneError && size == 1:
			dst = append(dst, s[written:i]...)
			dst = append(dst, '\\', 'u', 'f', 'f', 'f', 'd')
		case r == '\u2028' || r == '\u2029':
			dst = append(dst, s[written:i]...)
			dst = append(dst, '\\', 'u', '2', '0', '2', hexDigits[r&0xF])
		default:
			i += size
			continue
		}
		i += size
		written = i
	}
	dst = append(dst, s[written:]...)
	return append(dst, '"')
}

func safeJSONByte(b byte) bool {
	return b >= 0x20 && b != '"' && b != '\\' && b != '<' && b != '>' && b != '&'
}

func validJSONNumber(s string) bool {
	end, err := endOfNumber([]byte(s), 0)
	return err == nil && end == len(s)
}

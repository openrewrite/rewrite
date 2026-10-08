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
	"bytes"
	"encoding/json"
	"fmt"
	"math/big"
	"strconv"
	"strings"
	"unicode"
	"unicode/utf16"
)

type State int

const (
	NoChange State = iota
	Add
	Delete
	Change
	EndOfObject
)

// Text, not JSON: the encoder quotes and escapes this directly, where a Marshaler's
// JSON bytes would have to be reparsed and re-emitted to splice into the stream.
func (s State) MarshalText() ([]byte, error) {
	return []byte(s.String()), nil
}

func (s State) String() string {
	switch s {
	case NoChange:
		return "NO_CHANGE"
	case Add:
		return "ADD"
	case Delete:
		return "DELETE"
	case Change:
		return "CHANGE"
	case EndOfObject:
		return "END_OF_OBJECT"
	default:
		return "UNKNOWN"
	}
}

// AddedListItem is the sentinel value used in list positions to indicate a new item.
const AddedListItem = -1

// RpcObjectData is the wire format for RPC messages.
type RpcObjectData struct {
	State     State   `json:"state"`
	ValueType *string `json:"valueType,omitempty"`
	Value     any     `json:"value,omitempty"`
	Ref       *int    `json:"ref,omitempty"`
}

// A value that carries no valueType is bound on the JVM side by its JSON shape,
// and Go writes a float64 without a fraction or an exponent for every magnitude
// between 1e-6 and 1e21 -- a shape the receiver reads as an integer. Only a value
// sent on its own is bound that way; one nested in an inlined map arrives under a
// valueType, which binds it against the type of the field it fills.
func wireNumber(v any) any {
	f, ok := v.(float64)
	if !ok {
		return v
	}
	s := strconv.FormatFloat(f, 'g', -1, 64)
	if !strings.ContainsAny(s, ".eE") {
		s += ".0"
	}
	return json.Number(s)
}

// DecodeBatch reads a page of messages straight out of its bytes, building the values the rest of
// this package expects: strings interned, numbers given the Go type their JSON shape implies,
// objects as maps. A truncated page is an error rather than a short batch.
func DecodeBatch(data []byte, intern map[string]string) ([]RpcObjectData, error) {
	i := skipSpace(data, 0)
	if i >= len(data) {
		return nil, nil
	}
	if hasLiteral(data, i, "null") {
		return nil, nil
	}
	if data[i] != '[' {
		return nil, fmt.Errorf("expected JSON array, got %s", describe(data, i))
	}
	i++
	batch := make([]RpcObjectData, 0, len(data)/40+1)
	for {
		i = skipSpace(data, i)
		if i >= len(data) {
			return nil, fmt.Errorf("unterminated JSON array")
		}
		switch data[i] {
		case ']':
			return batch, nil
		case ',':
			i++
			continue
		}
		d, next, err := scanMessage(data, i, intern)
		if err != nil {
			return nil, err
		}
		batch = append(batch, d)
		i = next
	}
}

// Reads one message. Unknown members are skipped rather than rejected, so a remote that learns a
// new field does not break a reader that has not.
func scanMessage(data []byte, i int, tbl map[string]string) (RpcObjectData, int, error) {
	var d RpcObjectData
	if data[i] != '{' {
		return d, 0, fmt.Errorf("expected JSON object, got %s", describe(data, i))
	}
	i++
	for {
		i = skipSpace(data, i)
		if i >= len(data) {
			return d, 0, fmt.Errorf("unterminated JSON object")
		}
		switch data[i] {
		case '}':
			return d, i + 1, nil
		case ',':
			i++
			continue
		}
		key, next, err := scanString(data, i, tbl)
		if err != nil {
			return d, 0, err
		}
		i = skipSpace(data, next)
		if i >= len(data) || data[i] != ':' {
			return d, 0, fmt.Errorf("expected a colon after member %q", key)
		}
		i = skipSpace(data, i+1)
		if i >= len(data) {
			return d, 0, fmt.Errorf("member %q has no value", key)
		}

		switch key {
		case "state":
			name, next, err := scanString(data, i, tbl)
			if err != nil {
				return d, 0, fmt.Errorf("state is not a string: %w", err)
			}
			d.State = parseState(name)
			i = next
		// A valueType or ref that is not of the member's type — null, most often — leaves the
		// field unset rather than failing the page, as binding into the struct did.
		case "valueType":
			if data[i] != '"' {
				next, err := skipValue(data, i)
				if err != nil {
					return d, 0, err
				}
				i = next
				break
			}
			name, next, err := scanString(data, i, tbl)
			if err != nil {
				return d, 0, err
			}
			d.ValueType = &name
			i = next
		case "ref":
			if !isDigit(data[i]) && data[i] != '-' {
				next, err := skipValue(data, i)
				if err != nil {
					return d, 0, err
				}
				i = next
				break
			}
			end, err := endOfNumber(data, i)
			if err != nil {
				return d, 0, err
			}
			ref, err := strconv.Atoi(string(data[i:end]))
			if err != nil {
				return d, 0, err
			}
			d.Ref = &ref
			i = end
		case "value":
			value, next, err := scanValue(data, i, tbl)
			if err != nil {
				return d, 0, err
			}
			d.Value = value
			i = next
		default:
			next, err := skipValue(data, i)
			if err != nil {
				return d, 0, err
			}
			i = next
		}
	}
}

// scanValue builds the value the rest of this package expects: an object is a map with interned
// keys, an array is always non-nil, a string is interned, and a number keeps the type its JSON
// shape implies.
func scanValue(data []byte, i int, tbl map[string]string) (any, int, error) {
	switch data[i] {
	case '{':
		m := map[string]any{}
		i++
		for {
			i = skipSpace(data, i)
			if i >= len(data) {
				return nil, 0, fmt.Errorf("unterminated JSON object")
			}
			switch data[i] {
			case '}':
				return m, i + 1, nil
			case ',':
				i++
				continue
			}
			key, next, err := scanString(data, i, tbl)
			if err != nil {
				return nil, 0, err
			}
			i = skipSpace(data, next)
			if i >= len(data) || data[i] != ':' {
				return nil, 0, fmt.Errorf("expected a colon after member %q", key)
			}
			i = skipSpace(data, i+1)
			if i >= len(data) {
				return nil, 0, fmt.Errorf("member %q has no value", key)
			}
			value, next, err := scanValue(data, i, tbl)
			if err != nil {
				return nil, 0, err
			}
			m[key] = value
			i = next
		}
	case '[':
		arr := []any{}
		i++
		for {
			i = skipSpace(data, i)
			if i >= len(data) {
				return nil, 0, fmt.Errorf("unterminated JSON array")
			}
			switch data[i] {
			case ']':
				return arr, i + 1, nil
			case ',':
				i++
				continue
			}
			value, next, err := scanValue(data, i, tbl)
			if err != nil {
				return nil, 0, err
			}
			arr = append(arr, value)
			i = next
		}
	case '"':
		text, next, err := scanString(data, i, tbl)
		return text, next, err
	case 't':
		if !hasLiteral(data, i, "true") {
			return nil, 0, fmt.Errorf("invalid literal at %s", describe(data, i))
		}
		return true, i + len("true"), nil
	case 'f':
		if !hasLiteral(data, i, "false") {
			return nil, 0, fmt.Errorf("invalid literal at %s", describe(data, i))
		}
		return false, i + len("false"), nil
	case 'n':
		if !hasLiteral(data, i, "null") {
			return nil, 0, fmt.Errorf("invalid literal at %s", describe(data, i))
		}
		return nil, i + len("null"), nil
	default:
		end, err := endOfNumber(data, i)
		if err != nil {
			return nil, 0, err
		}
		return decodeNumber(json.Number(data[i:end])), end, nil
	}
}

// The remote's numbers arrive as text (see UseNumber above) and take the Go type
// their JSON shape implies, so a value keeps both its kind and its full precision
// across a round trip.
// Converting an int64 to an interface allocates unless the runtime holds the value
// statically, which it does only for 0..255. List positions are small and dominated
// by ADDED_LIST_ITEM, so the range that covers them is pre-boxed once.
var boxedInts = func() [1025]any {
	var b [1025]any
	for i := range b {
		b[i] = int64(i - 1)
	}
	return b
}()

func boxInt(v int64) any {
	// Bounded before the shift: v+1 overflows for MaxInt64 and would index negatively.
	if v >= -1 && v < int64(len(boxedInts))-1 {
		return boxedInts[v+1]
	}
	return v
}

func decodeNumber(n json.Number) any {
	s := n.String()
	if !strings.ContainsAny(s, ".eE") {
		if i, err := strconv.ParseInt(s, 10, 64); err == nil {
			return boxInt(i)
		}
		if i, ok := new(big.Int).SetString(s, 10); ok {
			return i
		}
	}
	f, _ := n.Float64()
	return f
}

func internString(s string, tbl map[string]string) string {
	if s == "" || tbl == nil {
		return s
	}
	if c, ok := tbl[s]; ok {
		return c
	}
	tbl[s] = s
	return s
}

func parseState(s string) State {
	switch s {
	case "NO_CHANGE":
		return NoChange
	case "ADD":
		return Add
	case "DELETE":
		return Delete
	case "CHANGE":
		return Change
	case "END_OF_OBJECT":
		return EndOfObject
	default:
		return NoChange
	}
}

func skipSpace(data []byte, i int) int {
	for i < len(data) {
		switch data[i] {
		case ' ', '\t', '\n', '\r':
			i++
		default:
			return i
		}
	}
	return i
}

func hasLiteral(data []byte, i int, literal string) bool {
	return i+len(literal) <= len(data) && string(data[i:i+len(literal)]) == literal
}

// describe names what was found, for an error that has to be readable without the input to hand.
func describe(data []byte, i int) string {
	if i >= len(data) {
		return "end of input"
	}
	end := i + 12
	if end > len(data) {
		end = len(data)
	}
	return strconv.Quote(string(data[i:end]))
}

// endOfNumber bounds a number against JSON's grammar rather than strconv's, which differs at both
// ends: strconv takes "01", "+1" and ".5", and rejects the out-of-range "1e999" that JSON allows
// and decodeNumber reads as an infinity. Parsing stays with decodeNumber, which is what gives a
// number the Go type its JSON shape implies.
func endOfNumber(data []byte, i int) (int, error) {
	end := i
	if end < len(data) && data[end] == '-' {
		end++
	}
	whole := end
	for end < len(data) && isDigit(data[end]) {
		end++
	}
	if end == whole {
		return 0, fmt.Errorf("expected a number, got %s", describe(data, i))
	}
	if data[whole] == '0' && end-whole > 1 {
		return 0, fmt.Errorf("number has a leading zero at %s", describe(data, i))
	}
	if end < len(data) && data[end] == '.' {
		end++
		fraction := end
		for end < len(data) && isDigit(data[end]) {
			end++
		}
		if end == fraction {
			return 0, fmt.Errorf("number has no digits after the point at %s", describe(data, i))
		}
	}
	if end < len(data) && (data[end] == 'e' || data[end] == 'E') {
		end++
		if end < len(data) && (data[end] == '+' || data[end] == '-') {
			end++
		}
		exponent := end
		for end < len(data) && isDigit(data[end]) {
			end++
		}
		if end == exponent {
			return 0, fmt.Errorf("number has no exponent digits at %s", describe(data, i))
		}
	}
	return end, nil
}

func isDigit(c byte) bool {
	return c >= '0' && c <= '9'
}

// skipValue passes over a value without building it, for a member this reader does not know.
func skipValue(data []byte, i int) (int, error) {
	switch data[i] {
	case '{', '[':
		depth := 0
		for ; i < len(data); i++ {
			switch data[i] {
			case '{', '[':
				depth++
			case '}', ']':
				if depth--; depth == 0 {
					return i + 1, nil
				}
			case '"':
				end, err := endOfString(data, i)
				if err != nil {
					return 0, err
				}
				i = end - 1
			}
		}
		return 0, fmt.Errorf("unterminated structure")
	case '"':
		return endOfString(data, i)
	case 't':
		if !hasLiteral(data, i, "true") {
			return 0, fmt.Errorf("invalid literal at %s", describe(data, i))
		}
		return i + len("true"), nil
	case 'f':
		if !hasLiteral(data, i, "false") {
			return 0, fmt.Errorf("invalid literal at %s", describe(data, i))
		}
		return i + len("false"), nil
	case 'n':
		if !hasLiteral(data, i, "null") {
			return 0, fmt.Errorf("invalid literal at %s", describe(data, i))
		}
		return i + len("null"), nil
	default:
		return endOfNumber(data, i)
	}
}

// endOfString returns the index just past the closing quote.
func endOfString(data []byte, i int) (int, error) {
	if data[i] != '"' {
		return 0, fmt.Errorf("expected a string, got %s", describe(data, i))
	}
	for j := i + 1; j < len(data); j++ {
		switch data[j] {
		case '\\':
			j++
		case '"':
			return j + 1, nil
		}
	}
	return 0, fmt.Errorf("unterminated string at %s", describe(data, i))
}

// scanString reads a string and interns it. An unescaped string — most of them — becomes a Go
// string straight from the bytes; anything carrying an escape is unescaped first.
func scanString(data []byte, i int, tbl map[string]string) (string, int, error) {
	end, err := endOfString(data, i)
	if err != nil {
		return "", 0, err
	}
	body := data[i+1 : end-1]
	if bytes.IndexByte(body, '\\') < 0 {
		return internString(string(body), tbl), end, nil
	}
	unescaped, err := unescape(body)
	if err != nil {
		return "", 0, err
	}
	return internString(unescaped, tbl), end, nil
}

// unescape applies JSON's escape rules — not Go's, which differ over \/ and over how a surrogate
// pair is written. A lone surrogate becomes the replacement character, as encoding/json does.
func unescape(body []byte) (string, error) {
	var out strings.Builder
	out.Grow(len(body))
	for i := 0; i < len(body); {
		c := body[i]
		if c != '\\' {
			out.WriteByte(c)
			i++
			continue
		}
		i++
		if i >= len(body) {
			return "", fmt.Errorf("string ends in a backslash")
		}
		switch body[i] {
		case '"', '\\', '/':
			out.WriteByte(body[i])
			i++
		case 'b':
			out.WriteByte('\b')
			i++
		case 'f':
			out.WriteByte('\f')
			i++
		case 'n':
			out.WriteByte('\n')
			i++
		case 'r':
			out.WriteByte('\r')
			i++
		case 't':
			out.WriteByte('\t')
			i++
		case 'u':
			r, next, err := unescapeRune(body, i)
			if err != nil {
				return "", err
			}
			out.WriteRune(r)
			i = next
		default:
			return "", fmt.Errorf("invalid escape \\%c", body[i])
		}
	}
	return out.String(), nil
}

// unescapeRune reads \uXXXX at body[i] == 'u', joining a surrogate pair when one follows.
func unescapeRune(body []byte, i int) (rune, int, error) {
	first, next, err := hex4(body, i+1)
	if err != nil {
		return 0, 0, err
	}
	if !utf16.IsSurrogate(first) {
		return first, next, nil
	}
	if next+1 < len(body) && body[next] == '\\' && body[next+1] == 'u' {
		second, after, err := hex4(body, next+2)
		if err != nil {
			return 0, 0, err
		}
		if joined := utf16.DecodeRune(first, second); joined != unicode.ReplacementChar {
			return joined, after, nil
		}
	}
	return unicode.ReplacementChar, next, nil
}

func hex4(body []byte, i int) (rune, int, error) {
	if i+4 > len(body) {
		return 0, 0, fmt.Errorf("truncated \\u escape")
	}
	value, err := strconv.ParseUint(string(body[i:i+4]), 16, 32)
	if err != nil {
		return 0, 0, fmt.Errorf("invalid \\u escape %q", string(body[i:i+4]))
	}
	return rune(value), i + 4, nil
}

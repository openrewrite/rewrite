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
	"io"
	"math/big"
	"strconv"
	"strings"
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

type wireObjectData struct {
	State     string  `json:"state"`
	ValueType *string `json:"valueType"`
	Value     any     `json:"value"`
	Ref       *int    `json:"ref"`
}

func DecodeBatch(data []byte, intern map[string]string) ([]RpcObjectData, error) {
	dec := json.NewDecoder(bytes.NewReader(data))
	dec.UseNumber()
	open, err := dec.Token()
	if err != nil {
		if err == io.EOF {
			return nil, nil
		}
		return nil, err
	}
	if open == nil {
		return nil, nil
	}
	if d, ok := open.(json.Delim); !ok || d != '[' {
		return nil, fmt.Errorf("expected JSON array, got %v", open)
	}
	batch := make([]RpcObjectData, 0, len(data)/40+1)
	for dec.More() {
		d, err := decodeObjectData(dec, intern)
		if err != nil {
			return nil, err
		}
		batch = append(batch, d)
	}
	return batch, nil
}

// Reads one message straight off the token stream. Binding into a struct whose value
// is an `any` materializes a map/slice tree that a second walk then has to revisit to
// intern strings and give numbers the type their JSON shape implies; the tokens carry
// enough to build the final value in one pass.
func decodeObjectData(dec *json.Decoder, tbl map[string]string) (RpcObjectData, error) {
	var d RpcObjectData
	t, err := dec.Token()
	if err != nil {
		return d, err
	}
	if delim, ok := t.(json.Delim); !ok || delim != '{' {
		return d, fmt.Errorf("expected JSON object, got %v", t)
	}
	for dec.More() {
		kt, err := dec.Token()
		if err != nil {
			return d, err
		}
		key, ok := kt.(string)
		if !ok {
			return d, fmt.Errorf("expected member name, got %v", kt)
		}
		switch key {
		case "state":
			v, err := dec.Token()
			if err != nil {
				return d, err
			}
			name, ok := v.(string)
			if !ok {
				return d, fmt.Errorf("state is not a string: %v", v)
			}
			d.State = parseState(name)
		case "valueType":
			v, err := dec.Token()
			if err != nil {
				return d, err
			}
			if name, ok := v.(string); ok {
				name = internString(name, tbl)
				d.ValueType = &name
			}
		case "ref":
			v, err := dec.Token()
			if err != nil {
				return d, err
			}
			if n, ok := v.(json.Number); ok {
				ref, err := strconv.Atoi(n.String())
				if err != nil {
					return d, err
				}
				d.Ref = &ref
			}
		case "value":
			if d.Value, err = decodeTokenValue(dec, tbl); err != nil {
				return d, err
			}
		default:
			var skipped any
			if err := dec.Decode(&skipped); err != nil {
				return d, err
			}
		}
	}
	if _, err := dec.Token(); err != nil { // closing brace
		return d, err
	}
	return d, nil
}

func decodeTokenValue(dec *json.Decoder, tbl map[string]string) (any, error) {
	t, err := dec.Token()
	if err != nil {
		return nil, err
	}
	switch v := t.(type) {
	case json.Delim:
		switch v {
		case '[':
			arr := []any{}
			for dec.More() {
				e, err := decodeTokenValue(dec, tbl)
				if err != nil {
					return nil, err
				}
				arr = append(arr, e)
			}
			_, err = dec.Token() // closing bracket
			return arr, err
		case '{':
			m := map[string]any{}
			for dec.More() {
				kt, err := dec.Token()
				if err != nil {
					return nil, err
				}
				k, ok := kt.(string)
				if !ok {
					return nil, fmt.Errorf("expected member name, got %v", kt)
				}
				if m[internString(k, tbl)], err = decodeTokenValue(dec, tbl); err != nil {
					return nil, err
				}
			}
			_, err = dec.Token() // closing brace
			return m, err
		}
		return nil, fmt.Errorf("unexpected delimiter %v", v)
	case string:
		return internString(v, tbl), nil
	case json.Number:
		return decodeNumber(v), nil
	default:
		return v, nil
	}
}

func decodeValue(v any, tbl map[string]string) any {
	switch x := v.(type) {
	case string:
		return internString(x, tbl)
	case json.Number:
		return decodeNumber(x)
	case []any:
		for i := range x {
			x[i] = decodeValue(x[i], tbl)
		}
		return x
	case map[string]any:
		for k, val := range x {
			x[k] = decodeValue(val, tbl)
		}
		return x
	default:
		return v
	}
}

// The remote's numbers arrive as text (see UseNumber above) and take the Go type
// their JSON shape implies, so a value keeps both its kind and its full precision
// across a round trip.
func decodeNumber(n json.Number) any {
	s := n.String()
	if !strings.ContainsAny(s, ".eE") {
		if i, err := strconv.ParseInt(s, 10, 64); err == nil {
			return i
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

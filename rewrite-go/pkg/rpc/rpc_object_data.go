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
)

type State int

const (
	NoChange State = iota
	Add
	Delete
	Change
	EndOfObject
)

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

func (d RpcObjectData) MarshalJSON() ([]byte, error) {
	type Alias struct {
		State     string  `json:"state"`
		ValueType *string `json:"valueType,omitempty"`
		Value     any     `json:"value,omitempty"`
		Ref       *int    `json:"ref,omitempty"`
	}
	return json.Marshal(Alias{
		State:     d.State.String(),
		ValueType: d.ValueType,
		Value:     d.Value,
		Ref:       d.Ref,
	})
}

type wireObjectData struct {
	State     string  `json:"state"`
	ValueType *string `json:"valueType"`
	Value     any     `json:"value"`
	Ref       *int    `json:"ref"`
}

func DecodeBatch(data []byte, intern map[string]string) ([]RpcObjectData, error) {
	dec := json.NewDecoder(bytes.NewReader(data))
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
	var w wireObjectData
	for dec.More() {
		w = wireObjectData{}
		if err := dec.Decode(&w); err != nil {
			return nil, err
		}
		d := RpcObjectData{State: parseState(w.State), ValueType: w.ValueType, Ref: w.Ref}
		if w.Value != nil {
			d.Value = internValue(w.Value, intern)
		}
		batch = append(batch, d)
	}
	return batch, nil
}

func internValue(v any, tbl map[string]string) any {
	switch x := v.(type) {
	case string:
		return internString(x, tbl)
	case []any:
		for i := range x {
			x[i] = internValue(x[i], tbl)
		}
		return x
	case map[string]any:
		for k, val := range x {
			x[k] = internValue(val, tbl)
		}
		return x
	default:
		return v
	}
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

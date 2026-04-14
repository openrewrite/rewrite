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

import "encoding/json"

// State represents the state of an RPC object data message.
type State int

const (
	NoChange    State = iota
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

// MarshalJSON implements custom JSON marshaling for RpcObjectData.
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

// ParseObjectData converts a JSON-decoded map to an RpcObjectData.
func ParseObjectData(m map[string]any) RpcObjectData {
	d := RpcObjectData{}
	if s, ok := m["state"].(string); ok {
		d.State = parseState(s)
	}
	if vt, ok := m["valueType"].(string); ok {
		d.ValueType = &vt
	}
	d.Value = m["value"]
	if ref, ok := m["ref"]; ok && ref != nil {
		r := int(ref.(float64))
		d.Ref = &r
	}
	return d
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

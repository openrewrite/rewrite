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
	"testing"

	"github.com/stretchr/testify/require"
)

// Pins the exact bytes each message shape serialises to, so the encoding can be
// made cheaper without moving the wire. A peer binds a value with no valueType by
// its JSON shape, so the integer/float distinction below is load-bearing.
func TestRpcObjectDataWireFormat(t *testing.T) {
	vt := "org.openrewrite.java.tree.J$Identifier"
	ref := 7

	cases := []struct {
		name string
		in   RpcObjectData
		want string
	}{
		{"state only", RpcObjectData{State: NoChange}, `{"state":"NO_CHANGE"}`},
		{"delete", RpcObjectData{State: Delete}, `{"state":"DELETE"}`},
		{"value type", RpcObjectData{State: Add, ValueType: &vt}, `{"state":"ADD","valueType":"` + vt + `"}`},
		{"ref", RpcObjectData{State: Add, Ref: &ref}, `{"state":"ADD","ref":7}`},
		{"string value", RpcObjectData{State: Add, Value: "A"}, `{"state":"ADD","value":"A"}`},
		{"list positions", RpcObjectData{State: Change, Value: []any{0, -1, 2}}, `{"state":"CHANGE","value":[0,-1,2]}`},
		{"empty positions", RpcObjectData{State: Change, Value: []any{}}, `{"state":"CHANGE","value":[]}`},
		{"bool value", RpcObjectData{State: Add, Value: true}, `{"state":"ADD","value":true}`},
		{"int value", RpcObjectData{State: Add, Value: 42}, `{"state":"ADD","value":42}`},
		{"shaped float", RpcObjectData{State: Add, Value: json.Number("3.0")}, `{"state":"ADD","value":3.0}`},
		{"map value", RpcObjectData{State: Add, Value: map[string]any{"b": 1, "a": "x"}}, `{"state":"ADD","value":{"a":"x","b":1}}`},
		{"everything", RpcObjectData{State: Change, ValueType: &vt, Value: "v", Ref: &ref},
			`{"state":"CHANGE","valueType":"` + vt + `","value":"v","ref":7}`},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			got, err := json.Marshal(tc.in)
			require.NoError(t, err)
			require.JSONEq(t, tc.want, string(got))
			require.Equal(t, tc.want, string(got), "byte-for-byte, not just semantically")
		})
	}
}

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
	"math"
	"math/big"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// A value that carries no valueType is bound by its JSON shape, so a float has
// to reach the wire as a JSON float (see wireNumber).
func TestMarshalKeepsFloatsFloating(t *testing.T) {
	cases := []struct {
		name  string
		value any
		want  string
	}{
		// given
		{"fraction", 1.5, `1.5`},
		{"whole float", 3.0, `3.0`},
		{"large float", 3e20, `3e+20`},
		{"larger than any int64", 1e21, `1e+21`},
		{"small float", 1e-7, `1e-07`},
		{"negative float", -0.25, `-0.25`},
		{"int64", int64(9223372036854775807), `9223372036854775807`},
		{"int wider than int64", bigWire("300000000000000000000"), `300000000000000000000`},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			// when
			data, err := json.Marshal(RpcObjectData{State: Change, Value: tc.value})

			// then
			require.NoError(t, err)
			assert.Equal(t, `{"state":"CHANGE","value":`+tc.want+`}`, string(data))
		})
	}
}

// The scalars Java sends carry no type of their own; their Go type follows the
// JSON shape (see decodeNumber).
func TestDecodeBatchCanonicalizesNumbers(t *testing.T) {
	cases := []struct {
		name string
		json string
		want any
	}{
		// given
		{"int", `1`, int64(1)},
		{"max int64", `9223372036854775807`, int64(math.MaxInt64)},
		{"wider than int64", `300000000000000000000`, bigWire("300000000000000000000")},
		{"float", `1.5`, 1.5},
		{"whole float", `3.0`, 3.0},
		{"float with exponent", `3e+20`, 3e20},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			// when
			batch, err := DecodeBatch([]byte(`[{"state":"CHANGE","value":`+tc.json+`}]`), nil)

			// then
			require.NoError(t, err)
			require.Len(t, batch, 1)
			assert.Equalf(t, tc.want, batch[0].Value, "value = %#v (%T)", batch[0].Value, batch[0].Value)
		})
	}
}

// Numbers nested in a list (list positions) or a map (a marker's fields) take
// the same treatment as a scalar.
func TestDecodeBatchCanonicalizesNestedNumbers(t *testing.T) {
	// given
	data := []byte(`[{"state":"CHANGE","value":[0,-1]},{"state":"ADD","valueType":"m","value":{"n":2,"f":1.5}}]`)

	// when
	batch, err := DecodeBatch(data, nil)

	// then
	require.NoError(t, err)
	assert.Equal(t, []any{int64(0), int64(-1)}, batch[0].Value)
	assert.Equal(t, map[string]any{"n": int64(2), "f": 1.5}, batch[1].Value)
}

// A literal's value has to come back from Java unchanged, whatever its width.
func TestNumbersSurviveTheReturnLeg(t *testing.T) {
	for _, value := range []any{int64(1), int64(math.MaxInt64), bigWire("300000000000000000000"), 3e20, 1.5, 3.0} {
		// when
		data, err := json.Marshal([]RpcObjectData{{State: Change, Value: value}})
		require.NoError(t, err)
		batch, err := DecodeBatch(data, nil)

		// then
		require.NoError(t, err)
		assert.Equalf(t, value, batch[0].Value, "%#v did not survive the round trip", value)
	}
}

func bigWire(s string) *big.Int {
	i, _ := new(big.Int).SetString(s, 10)
	return i
}

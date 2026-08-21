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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestReceive_UnknownValueTypeWithInlineValue(t *testing.T) {
	valueType := "java.math.BigInteger"
	q := queueOf(RpcObjectData{
		State:     Add,
		ValueType: &valueType,
		Value:     "300000000000000000000",
	})

	got := q.Receive(nil, nil)

	assert.Equal(t, "300000000000000000000", got,
		"a type with no codec on either side travels as a scalar, as it does to the Python peer")
}

func TestReceive_UnknownValueTypeWithoutInlineValue(t *testing.T) {
	valueType := "com.example.Widget"
	q := queueOf(RpcObjectData{State: Add, ValueType: &valueType})

	// No inline value means sub-field messages follow that nothing would consume.
	require.PanicsWithValue(t, missingCodec(valueType), func() {
		q.Receive(nil, nil)
	})
}

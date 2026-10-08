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
	"fmt"
	"testing"
)

// The send side of a page, shaped as SendQueue.Put leaves it: states and value types interned by
// the caller, floats already turned into json.Number by wireNumber, list positions as []any.
func benchBatch(messages int) []RpcObjectData {
	valueType := "org.openrewrite.java.tree.J$Identifier"
	batch := make([]RpcObjectData, 0, messages)
	for i := 0; i < messages; i++ {
		switch i % 4 {
		case 0:
			batch = append(batch, RpcObjectData{State: Change, Value: "\n    "})
		case 1:
			batch = append(batch, RpcObjectData{State: Change, Value: " "})
		case 2:
			batch = append(batch, RpcObjectData{State: Change, Value: []any{0, 1, 2, 3, 4}})
		case 3:
			ref := i
			batch = append(batch, RpcObjectData{
				State:     Add,
				ValueType: &valueType,
				Value:     map[string]any{"id": fmt.Sprint(i), "desc": "\n    "},
				Ref:       &ref,
			})
		}
	}
	return batch
}

func BenchmarkEncodeBatch(b *testing.B) {
	batch := benchBatch(2000)
	b.ReportAllocs()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		if _, err := json.Marshal(batch); err != nil {
			b.Fatal(err)
		}
	}
}

// The same page through the appender, into a buffer the caller keeps, as a framed write does.
func BenchmarkAppendBatch(b *testing.B) {
	batch := benchBatch(2000)
	buf := make([]byte, 0, 1<<18)
	b.ReportAllocs()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		var err error
		if buf, err = AppendBatch(buf[:0], batch); err != nil {
			b.Fatal(err)
		}
	}
}

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
	"strings"
	"testing"
)

func benchBatchJSON(messages int) []byte {
	var b strings.Builder
	b.WriteByte('[')
	for i := 0; i < messages; i++ {
		if i > 0 {
			b.WriteByte(',')
		}
		switch i % 4 {
		case 0:
			b.WriteString(`{"state":"CHANGE","value":"\n    "}`)
		case 1:
			b.WriteString(`{"state":"CHANGE","value":" "}`)
		case 2:
			b.WriteString(`{"state":"CHANGE","value":[0,1,2,3,4]}`)
		case 3:
			fmt.Fprintf(&b, `{"state":"ADD","valueType":"org.openrewrite.marker.SearchResult","value":{"id":"%d","desc":"\n    "},"ref":%d}`, i, i)
		}
	}
	b.WriteByte(']')
	return []byte(b.String())
}

func BenchmarkDecodeBatch(b *testing.B) {
	data := benchBatchJSON(2000)
	b.ReportAllocs()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		if _, err := DecodeBatch(data, make(map[string]string)); err != nil {
			b.Fatal(err)
		}
	}
}

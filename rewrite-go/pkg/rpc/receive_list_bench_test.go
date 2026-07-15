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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func buildUnchangedListBatch(n int) []RpcObjectData {
	positions := make([]any, n)
	for i := range positions {
		positions[i] = i
	}
	batch := make([]RpcObjectData, 0, n+2)
	batch = append(batch, RpcObjectData{State: Change})
	batch = append(batch, RpcObjectData{State: Change, Value: positions})
	for i := 0; i < n; i++ {
		batch = append(batch, RpcObjectData{State: NoChange})
	}
	return batch
}

func newBatchQueue(batch []RpcObjectData) *ReceiveQueue {
	return NewReceiveQueue(map[int]any{}, func() []RpcObjectData { return batch })
}

func makeStmtSlice(n int) []java.RightPadded[java.Statement] {
	before := make([]java.RightPadded[java.Statement], n)
	for i := range before {
		before[i] = java.RightPadded[java.Statement]{}
	}
	return before
}

const benchListSize = 64

func receiveStatementsLegacy(q *ReceiveQueue, before []java.RightPadded[java.Statement]) []java.RightPadded[java.Statement] {
	beforeStmts := make([]any, len(before))
	for i, s := range before {
		beforeStmts[i] = s
	}
	afterStmts := q.ReceiveList(beforeStmts, func(v any) any { return v })
	if afterStmts != nil {
		out := make([]java.RightPadded[java.Statement], len(afterStmts))
		for i, s := range afterStmts {
			out[i] = coerceToStatementRP(s)
		}
		return out
	}
	return before
}

func BenchmarkReceiveList_Legacy(b *testing.B) {
	before := makeStmtSlice(benchListSize)
	batch := buildUnchangedListBatch(benchListSize)
	b.ReportAllocs()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		q := newBatchQueue(batch)
		_ = receiveStatementsLegacy(q, before)
	}
}

func BenchmarkReceiveList_Typed(b *testing.B) {
	before := makeStmtSlice(benchListSize)
	batch := buildUnchangedListBatch(benchListSize)
	b.ReportAllocs()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		q := newBatchQueue(batch)
		_ = receiveTypedList(q, before, func(v any) any { return v }, coerceToStatementRP)
	}
}

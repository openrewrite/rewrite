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

package main

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/require"

	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func getObjectParams(t *testing.T, id string) json.RawMessage {
	t.Helper()
	params, err := json.Marshal(getObjectRequest{ID: id})
	require.NoError(t, err, "marshal GetObject params")
	return params
}

func getObjectBatchForTest(t *testing.T, s *server, params json.RawMessage) []rpc.RpcObjectData {
	t.Helper()
	result, rpcErr := s.handleGetObject(params)
	require.Nil(t, rpcErr, "GetObject failed")
	return result.([]rpc.RpcObjectData)
}

func getCompleteObjectForTest(t *testing.T, s *server, id string) []rpc.RpcObjectData {
	t.Helper()
	params := getObjectParams(t, id)
	var result []rpc.RpcObjectData
	for {
		batch := getObjectBatchForTest(t, s, params)
		result = append(result, batch...)
		if len(batch) > 0 && batch[len(batch)-1].State == rpc.EndOfObject {
			return result
		}
	}
}

func getObjectTreeForTest(t *testing.T) java.Tree {
	t.Helper()
	cu, err := goparser.NewGoParser().Parse("main.go", "package main\n")
	require.NoError(t, err, "parse test source")
	return cu
}

func TestHandleGetObjectReturnsOneBatchPerRequest(t *testing.T) {
	s, _ := newTestServer(t)
	s.batchSize = 3

	const id = "tree"
	tree := getObjectTreeForTest(t)
	s.localObjects[id] = tree
	params := getObjectParams(t, id)

	first := getObjectBatchForTest(t, s, params)
	require.Falsef(t, len(first) != s.batchSize || first[0].State != rpc.Add, "first batch = %+v, want a full batch beginning with ADD", first)
	if _, complete := s.remoteObjects[id]; complete {
		t.Fatal("remote baseline was updated before END_OF_OBJECT was delivered")
	}
	if _, active := s.inProgressGetObjects[id]; !active {
		t.Fatal("transfer was not retained for the next GetObject request")
	}

	for {
		batch := getObjectBatchForTest(t, s, params)
		if len(batch) > s.batchSize {
			t.Fatalf("batch length = %d, want at most %d", len(batch), s.batchSize)
		}
		if batch[len(batch)-1].State == rpc.EndOfObject {
			break
		}
		if _, complete := s.remoteObjects[id]; complete {
			t.Fatal("remote baseline was updated before END_OF_OBJECT was delivered")
		}
	}
	if got := s.remoteObjects[id]; got != tree {
		t.Fatalf("remote baseline = %#v, want transferred object", got)
	}
	if _, active := s.inProgressGetObjects[id]; active {
		t.Fatal("completed transfer was not removed")
	}
}

func TestHandleGetObjectBatchesAreConsumedByReceiveQueue(t *testing.T) {
	s, _ := newTestServer(t)
	s.batchSize = 3

	const id = "tree"
	const want = "package main\n"
	s.localObjects[id] = getObjectTreeForTest(t)
	params := getObjectParams(t, id)
	pulls := 0

	q := rpc.NewReceiveQueue(make(map[int]any), func() []rpc.RpcObjectData {
		pulls++
		return getObjectBatchForTest(t, s, params)
	})
	receiver := rpc.NewGoReceiver()
	got := q.Receive(nil, func(v any) any {
		if tree, ok := v.(java.Tree); ok {
			return receiver.Visit(tree, q)
		}
		return v
	})
	gotTree, ok := got.(java.Tree)
	require.Truef(t, ok, "received object = %T, want java.Tree", got)
	if printed := printer.Print(gotTree); printed != want {
		t.Fatalf("received source = %q, want %q", printed, want)
	}
	if end := q.Take(); end.State != rpc.EndOfObject {
		t.Fatalf("end marker = %s, want END_OF_OBJECT", end.State)
	}
	if pulls < 2 {
		t.Fatalf("GetObject pulls = %d, want a multi-batch transfer", pulls)
	}
}

func TestHandleGetObjectReusesReferencesAcrossTransfers(t *testing.T) {
	s, _ := newTestServer(t)
	sharedType := &java.JavaTypeClass{
		Kind:               "Class",
		FullyQualifiedName: "example.Shared",
	}
	s.localObjects["first"] = &java.Identifier{Name: "first", Type: sharedType}
	s.localObjects["second"] = &java.Identifier{Name: "second", Type: sharedType}

	getCompleteObjectForTest(t, s, "first")
	refsAfterFirst := s.localRefs.Len()
	require.NotEqual(t, 0, refsAfterFirst, "first transfer did not retain any references")

	second := getCompleteObjectForTest(t, s, "second")
	if got := s.localRefs.Len(); got != refsAfterFirst {
		t.Fatalf("references after second transfer = %d, want %d", got, refsAfterFirst)
	}

	for _, data := range second {
		if data.State == rpc.Add && data.Ref != nil && data.ValueType == nil && data.Value == nil {
			return
		}
	}
	t.Fatal("second transfer did not reuse the shared type by reference")
}

func TestHandleGetObjectRejectsInterleavedTransfers(t *testing.T) {
	s, _ := newTestServer(t)
	s.batchSize = 1
	sharedType := &java.JavaTypeClass{
		Kind:               "Class",
		FullyQualifiedName: "example.Shared",
	}
	s.localObjects["first"] = &java.Identifier{Name: "first", Type: sharedType}
	s.localObjects["second"] = &java.Identifier{Name: "second", Type: sharedType}

	firstParams := getObjectParams(t, "first")
	firstBatch := getObjectBatchForTest(t, s, firstParams)
	if firstBatch[len(firstBatch)-1].State == rpc.EndOfObject {
		t.Fatal("first transfer unexpectedly completed in one batch")
	}

	if _, rpcErr := s.handleGetObject(getObjectParams(t, "second")); rpcErr == nil {
		t.Fatal("interleaved transfer was not rejected")
	}

	for {
		batch := getObjectBatchForTest(t, s, firstParams)
		if batch[len(batch)-1].State == rpc.EndOfObject {
			break
		}
	}
	require.Len(t, s.inProgressGetObjects, 0, "in-progress GetObjects after completion")
	getCompleteObjectForTest(t, s, "second")
}

func TestResetCancelsInProgressGetObject(t *testing.T) {
	s, _ := newTestServer(t)
	s.batchSize = 1

	const id = "tree"
	s.localObjects[id] = getObjectTreeForTest(t)
	params := getObjectParams(t, id)
	_ = getObjectBatchForTest(t, s, params)

	s.handleReset()
	require.Len(t, s.inProgressGetObjects, 0, "in-progress transfers after Reset")
	require.False(t, len(s.localObjects) != 0 || len(s.remoteObjects) != 0, "Reset did not clear object state")
}

// receiveWithNoRefs takes a transfer the way a peer does that kept nothing of
// an earlier one: a bare ref to anything it was not sent here fails.
func receiveWithNoRefs(t *testing.T, s *server, id string) string {
	t.Helper()
	params := getObjectParams(t, id)
	q := rpc.NewReceiveQueue(make(map[int]any), func() []rpc.RpcObjectData {
		return getObjectBatchForTest(t, s, params)
	})
	receiver := rpc.NewGoReceiver()
	got := q.Receive(nil, func(v any) any {
		if tree, ok := v.(java.Tree); ok {
			return receiver.Visit(tree, q)
		}
		return v
	})
	require.Equal(t, rpc.EndOfObject, q.Take().State)
	tree, ok := got.(java.Tree)
	require.Truef(t, ok, "received object = %T, want java.Tree", got)
	return printer.Print(tree)
}

func abortGetObjectParams(t *testing.T, id string) json.RawMessage {
	t.Helper()
	params, err := json.Marshal(map[string]string{"id": id})
	require.NoError(t, err)
	return params
}

// The peer failed on a batch while later ones were still to come.
func TestAbortGetObjectUndoesATransferStillStreaming(t *testing.T) {
	s, _ := newTestServer(t)
	s.batchSize = 1

	const id = "tree"
	const source = "package main\n\nvar x, y int = 1, 2\n"
	cu, err := goparser.NewGoParser().Parse("main.go", source)
	require.NoError(t, err)
	s.localObjects[id] = cu
	params := getObjectParams(t, id)
	for i := 0; i < 12; i++ {
		_ = getObjectBatchForTest(t, s, params)
	}
	require.NotZero(t, s.localRefs.Len(), "the batches handed over defined no refs")

	require.True(t, s.handleAbortGetObject(abortGetObjectParams(t, id)))

	require.Empty(t, s.inProgressGetObjects)
	require.Zero(t, s.localRefs.Len(), "refs the peer never kept")
	require.Equal(t, source, receiveWithNoRefs(t, s, id))
}

// The peer failed on the last batch, after this side had counted the object as taken.
func TestAbortGetObjectUndoesATransferAlreadyHandedOver(t *testing.T) {
	s, _ := newTestServer(t)

	const id = "tree"
	const source = "package main\n\nvar x, y int = 1, 2\n"
	cu, err := goparser.NewGoParser().Parse("main.go", source)
	require.NoError(t, err)
	s.localObjects[id] = cu
	getCompleteObjectForTest(t, s, id)
	require.Same(t, cu, s.remoteObjects[id])

	require.True(t, s.handleAbortGetObject(abortGetObjectParams(t, id)))

	require.NotContains(t, s.remoteObjects, id)
	require.Zero(t, s.localRefs.Len(), "refs the peer never kept")
	// whole again, where without the abort it would be reported as unchanged
	require.Equal(t, source, receiveWithNoRefs(t, s, id))
}

// Only the refs of the transfer that failed are forgotten while it is the
// latest one. After another transfer, which refs it assigned is not known.
func TestAbortGetObjectKeepsTheRefsOfEarlierTransfers(t *testing.T) {
	s, _ := newTestServer(t)
	shared := &java.JavaTypeClass{Kind: "Class", FullyQualifiedName: "example.Shared"}
	other := &java.JavaTypeClass{Kind: "Class", FullyQualifiedName: "example.Other"}
	s.localObjects["first"] = &java.Identifier{Name: "first", Type: shared}
	s.localObjects["second"] = &java.Identifier{Name: "second", Type: other}

	getCompleteObjectForTest(t, s, "first")
	kept := s.localRefs.Len()
	getCompleteObjectForTest(t, s, "second")
	require.Greater(t, s.localRefs.Len(), kept)

	require.True(t, s.handleAbortGetObject(abortGetObjectParams(t, "second")))
	require.Equal(t, kept, s.localRefs.Len())

	require.True(t, s.handleAbortGetObject(abortGetObjectParams(t, "first")))
	require.Zero(t, s.localRefs.Len())
}

func TestAbortGetObjectForAnObjectNeverSentChangesNothing(t *testing.T) {
	s, _ := newTestServer(t)

	require.True(t, s.handleAbortGetObject(abortGetObjectParams(t, "unknown")))
	require.True(t, s.handleAbortGetObject(abortGetObjectParams(t, "unknown")))
}

// Java reads the context without a callback for its fields, so it has to
// arrive under the type Java knows it by.
func TestExecutionContextIsSentUnderItsJavaType(t *testing.T) {
	s, _ := newTestServer(t)
	s.localObjects["ctx"] = recipe.NewExecutionContext()

	messages := getCompleteObjectForTest(t, s, "ctx")

	require.Len(t, messages, 2)
	require.Equal(t, rpc.Add, messages[0].State)
	require.NotNil(t, messages[0].ValueType)
	require.Equal(t, "org.openrewrite.InMemoryExecutionContext", *messages[0].ValueType)
	require.Equal(t, rpc.EndOfObject, messages[1].State)
}

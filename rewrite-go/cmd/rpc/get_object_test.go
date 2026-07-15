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

	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func getObjectParams(t *testing.T, id string) json.RawMessage {
	t.Helper()
	params, err := json.Marshal(getObjectRequest{ID: id})
	if err != nil {
		t.Fatalf("marshal GetObject params: %v", err)
	}
	return params
}

func getObjectBatchForTest(t *testing.T, s *server, params json.RawMessage) []rpc.RpcObjectData {
	t.Helper()
	result, rpcErr := s.handleGetObject(params)
	if rpcErr != nil {
		t.Fatalf("GetObject failed: %+v", rpcErr)
	}
	return result.([]rpc.RpcObjectData)
}

func getObjectTreeForTest(t *testing.T) java.Tree {
	t.Helper()
	cu, err := goparser.NewGoParser().Parse("main.go", "package main\n")
	if err != nil {
		t.Fatalf("parse test source: %v", err)
	}
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
	if len(first) != s.batchSize || first[0].State != rpc.Add {
		t.Fatalf("first batch = %+v, want a full batch beginning with ADD", first)
	}
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
	if !ok {
		t.Fatalf("received object = %T, want java.Tree", got)
	}
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

func TestResetCancelsInProgressGetObject(t *testing.T) {
	s, _ := newTestServer(t)
	s.batchSize = 1

	const id = "tree"
	s.localObjects[id] = getObjectTreeForTest(t)
	params := getObjectParams(t, id)
	_ = getObjectBatchForTest(t, s, params)

	s.handleReset()
	if len(s.inProgressGetObjects) != 0 {
		t.Fatalf("in-progress transfers after Reset = %d, want 0", len(s.inProgressGetObjects))
	}
	if len(s.localObjects) != 0 || len(s.remoteObjects) != 0 {
		t.Fatal("Reset did not clear object state")
	}
}

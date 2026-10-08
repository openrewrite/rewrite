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
	"bytes"
	"encoding/json"
	"strconv"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc"
)

// The encoder writes the whole frame body for a batch reply, envelope included, so the envelope
// has to match what json.Marshal wrote for the same response down to the byte — including which
// members are present and in what order.
func TestAppendBatchResponseMatchesMarshal(t *testing.T) {
	valueType := "org.openrewrite.java.tree.J$Identifier"
	ref := 3

	page := []rpc.RpcObjectData{
		{State: rpc.Add, ValueType: &valueType, Value: "someIdentifier", Ref: &ref},
		{State: rpc.Change, Value: []any{0, 1, 2}},
		{State: rpc.Change, Value: map[string]any{"b": 1, "a": "x"}},
		{State: rpc.EndOfObject},
	}

	ids := []json.RawMessage{
		json.RawMessage(`1`),
		json.RawMessage(`0`),
		json.RawMessage(`-1`),
		json.RawMessage(`"a-string-id"`),
		json.RawMessage(`"an id with \"quotes\" and \\"`),
		json.RawMessage(`null`),
		nil,
	}
	batches := map[string][]rpc.RpcObjectData{
		"a page":        page,
		"one message":   {{State: rpc.Delete}},
		"empty batch":   {},
		"nil batch":     nil,
		"end of object": {{State: rpc.Delete}, {State: rpc.EndOfObject}},
	}

	for name, batch := range batches {
		for _, id := range ids {
			resp := &jsonRPCResponse{JSONRPC: "2.0", ID: id, Result: batch}
			want, err := json.Marshal(resp)
			if err != nil {
				t.Fatalf("%s id=%s: json.Marshal: %v", name, id, err)
			}
			got, err := appendBatchResponse(nil, resp.ID, batch)
			if err != nil {
				t.Fatalf("%s id=%s: appendBatchResponse: %v", name, id, err)
			}
			if string(got) != string(want) {
				t.Errorf("%s id=%s:\n json.Marshal: %s\n      appended: %s", name, id, want, got)
			}
		}
	}
}

// writeMessage must write the same frame it always did, whichever path it takes. Comparing the
// bytes rather than the branch means the guard is covered too: a reply the encoder should decline
// still has to come out identical.
func TestWriteMessageWritesTheSameFrameAsMarshal(t *testing.T) {
	valueType := "org.openrewrite.java.tree.J$Identifier"
	ref := 3

	for name, resp := range map[string]*jsonRPCResponse{
		"a page": {JSONRPC: "2.0", ID: json.RawMessage(`7`), Result: []rpc.RpcObjectData{
			{State: rpc.Add, ValueType: &valueType, Value: "someIdentifier", Ref: &ref},
			{State: rpc.Change, Value: map[string]any{"b": 1, "a": "x"}},
			{State: rpc.EndOfObject},
		}},
		"a string id":         {JSONRPC: "2.0", ID: json.RawMessage(`"id"`), Result: []rpc.RpcObjectData{{State: rpc.Delete}}},
		"empty batch":         {JSONRPC: "2.0", ID: json.RawMessage(`1`), Result: []rpc.RpcObjectData{}},
		"nil batch":           {JSONRPC: "2.0", ID: json.RawMessage(`1`), Result: []rpc.RpcObjectData(nil)},
		"an error":            {JSONRPC: "2.0", ID: json.RawMessage(`1`), Error: &rpcError{Code: -32603, Message: "boom"}},
		"a different version": {JSONRPC: "1.0", ID: json.RawMessage(`1`), Result: []rpc.RpcObjectData{{State: rpc.Delete}}},
		"no result":           {JSONRPC: "2.0", ID: json.RawMessage(`1`)},
		"some other result":   {JSONRPC: "2.0", ID: json.RawMessage(`1`), Result: map[string]any{"ok": true}},
		"a bare message":      {JSONRPC: "2.0", ID: json.RawMessage(`1`), Result: rpc.RpcObjectData{State: rpc.Add, Ref: &ref}},
	} {
		body, err := json.Marshal(resp)
		if err != nil {
			t.Fatalf("%s: json.Marshal: %v", name, err)
		}
		want := "Content-Length: " + strconv.Itoa(len(body)) + "\r\n\r\n" + string(body)

		var written bytes.Buffer
		s := &server{writer: &written}
		if err := s.writeMessage(resp); err != nil {
			t.Fatalf("%s: writeMessage: %v", name, err)
		}
		if written.String() != want {
			t.Errorf("%s:\n  want %q\n   got %q", name, want, written.String())
		}
	}
}

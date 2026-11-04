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
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/require"
)

func frame(t *testing.T, msg map[string]any) []byte {
	t.Helper()
	body, err := json.Marshal(msg)
	require.NoError(t, err, "marshal message")
	return append([]byte(fmt.Sprintf("Content-Length: %d\r\n\r\n", len(body))), body...)
}

// frameReverseGetObjectReply renders one Content-Length framed JSON-RPC
// response carrying `result` — the shape Java sends back when Go issues a
// reverse GetObject during Print/Visit.
func frameReverseGetObjectReply(t *testing.T, result any) []byte {
	t.Helper()
	return frame(t, map[string]any{
		"jsonrpc": "2.0",
		"id":      "go-GetObject",
		"result":  result,
	})
}

// frameReverseRequest renders a request Java initiates, as opposed to a reply to Go's.
func frameReverseRequest(t *testing.T, method string) []byte {
	t.Helper()
	return frame(t, map[string]any{
		"jsonrpc": "2.0",
		"id":      "java-1",
		"method":  method,
		"params":  map[string]any{},
	})
}

// frameAbortGetObjectReply is the peer's answer once it has undone a transfer.
func frameAbortGetObjectReply(t *testing.T) []byte {
	t.Helper()
	return frame(t, map[string]any{"jsonrpc": "2.0", "id": "go-AbortGetObject", "result": true})
}

// newResilienceTestServer returns a server with its log captured, since the
// receive paths report desync by logging.
func newResilienceTestServer(t *testing.T) (*server, *bytes.Buffer) {
	t.Helper()
	s := newServer(serverConfig{logFile: filepath.Join(t.TempDir(), "server.log")})
	t.Cleanup(s.closeMetrics)
	var logs bytes.Buffer
	s.logger = log.New(&logs, "", 0)
	return s, &logs
}

// frameReverseGetObjectError is frameReverseGetObjectReply's error twin — the
// shape Java sends when its own traversal fails partway through the reply.
func frameReverseGetObjectError(t *testing.T, message, data string) []byte {
	t.Helper()
	return frame(t, map[string]any{
		"jsonrpc": "2.0",
		"id":      "go-GetObject",
		"error":   map[string]any{"code": -32603, "message": message, "data": data},
	})
}

// TestGetObjectFromJavaSurfacesRemoteError pins that an error response to a
// reverse GetObject fails the receive with the peer's message, and puts the
// peer's frames in the log.
func TestGetObjectFromJavaSurfacesRemoteError(t *testing.T) {
	s, logs := newResilienceTestServer(t)

	const remoteMessage = "Internal error: Failed to send object tree-X " +
		"(type: org.openrewrite.text.PlainText): java.lang.NullPointerException"
	const remoteTrace = "\tat org.openrewrite.text.PlainTextRpcCodec.rpcSend(PlainTextRpcCodec.java:44)"
	s.reader = bufio.NewReader(bytes.NewReader(frameReverseGetObjectError(t, remoteMessage, remoteTrace)))
	s.writer = &bytes.Buffer{}

	recovered := func() (r any) {
		defer func() { r = recover() }()
		s.getObjectFromJava("tree-X", "")
		return nil
	}()

	require.NotNil(t, recovered, "expected the error response to fail the receive")
	require.Contains(t, fmt.Sprint(recovered), remoteMessage)

	require.Contains(t, logs.String(), remoteTrace, "the peer's frames belong in the log")
}

// TestErrorOnAPrefetchedPageFailsTheTransfer pins the page requested ahead. The object
// is already complete when the error arrives, so nothing else forces that page to be
// read, and draining it would leave the peer's failure unreported.
func TestErrorOnAPrefetchedPageFailsTheTransfer(t *testing.T) {
	s, _ := newResilienceTestServer(t)

	const remoteMessage = "Internal error: Failed to send object tree-X: java.lang.NullPointerException"
	// Page 1 completes the value but does not close the transfer, so Go asks for a
	// page 2 that carries END_OF_OBJECT — and Java fails while producing it.
	stream := append(
		frameReverseGetObjectReply(t, []map[string]any{{"state": "ADD", "value": "package main\n"}}),
		frameReverseGetObjectError(t, remoteMessage, "")...,
	)
	s.reader = bufio.NewReader(bytes.NewReader(stream))
	s.writer = &bytes.Buffer{}

	recovered := func() (r any) {
		defer func() { r = recover() }()
		s.getObjectFromJava("tree-X", "")
		return nil
	}()

	require.NotNil(t, recovered, "expected the error page to fail the transfer")
	require.Contains(t, fmt.Sprint(recovered), remoteMessage)
}

// sentRequests reads back the requests the engine wrote to its peer.
func sentRequests(t *testing.T, s *server) []jsonRPCRequest {
	t.Helper()
	sent, _ := newResilienceTestServer(t)
	sent.reader = bufio.NewReader(bytes.NewReader(s.writer.(*bytes.Buffer).Bytes()))
	var requests []jsonRPCRequest
	for {
		request, err := sent.readMessage()
		if err != nil {
			return requests
		}
		requests = append(requests, *request)
	}
}

// failedThenCleanTransfer scripts a transfer that defines ref 6 and then goes
// on where the object should have ended, followed by the peer's answer to
// being told so and a clean transfer of the same object.
func failedThenCleanTransfer(t *testing.T, abortReply []byte) []byte {
	t.Helper()
	// The first page does not close the transfer, so the page after it has
	// been asked for by the time the receive fails and has to be read past.
	stream := frameReverseGetObjectReply(t, []map[string]any{
		{"state": "ADD", "valueType": "org.openrewrite.java.tree.Space", "ref": 6},
		{"state": "ADD", "ref": 7},
	})
	stream = append(stream, frameReverseGetObjectReply(t, []map[string]any{{"state": "END_OF_OBJECT"}})...)
	stream = append(stream, abortReply...)
	return append(stream, frameReverseGetObjectReply(t, []map[string]any{
		{"state": "ADD", "value": "package main\n"},
		{"state": "END_OF_OBJECT"},
	})...)
}

// A receive that fails leaves the peer counting the object, and every ref it
// sent with it, as received. Told that the transfer failed, the peer sends
// both whole next time, so this side forgets what it had of them too.
func TestFailedReceiveIsRolledBackWithThePeer(t *testing.T) {
	s, logs := newResilienceTestServer(t)
	s.reader = bufio.NewReader(bytes.NewReader(failedThenCleanTransfer(t, frameAbortGetObjectReply(t))))
	s.writer = &bytes.Buffer{}

	const id = "tree-X"
	s.reverseRemoteObjects[id] = "STALE-BASELINE"
	s.reverseRemoteRefs[5] = "shared-value-from-earlier-transfer"

	require.Panics(t, func() { s.getObjectFromJava(id, "") })

	require.NotContains(t, s.reverseRemoteObjects, id, "the baseline the peer no longer diffs against")
	require.NotContains(t, s.reverseRemoteRefs, 6, "a ref the failed transfer defined")
	require.Equal(t, "shared-value-from-earlier-transfer", s.reverseRemoteRefs[5], "a ref from before it")

	requests := sentRequests(t, s)
	last := requests[len(requests)-1]
	require.Equal(t, "AbortGetObject", last.Method)
	require.JSONEq(t, `{"id":"tree-X"}`, string(last.Params))

	// the next transfer reads its own reply, not one left over from the failure
	require.Equal(t, "package main\n", s.getObjectFromJava(id, ""))
	require.Equal(t, "package main\n", s.reverseRemoteObjects[id])
	require.NotContains(t, logs.String(), "Expected the prefetched GetObject page",
		"draining the peer's own reply is the ordinary case and must not be reported as a desync")
}

// A peer without the method still counts the refs it sent as received and
// goes on sending them bare, so they are kept for it to name.
func TestFailedReceiveKeepsItsRefsWhenThePeerCannotRollBack(t *testing.T) {
	s, _ := newResilienceTestServer(t)
	methodNotFound := frame(t, map[string]any{
		"jsonrpc": "2.0",
		"id":      "go-AbortGetObject",
		"error":   map[string]any{"code": -32601, "message": "Method not found: AbortGetObject"},
	})
	s.reader = bufio.NewReader(bytes.NewReader(failedThenCleanTransfer(t, methodNotFound)))
	s.writer = &bytes.Buffer{}

	const id = "tree-X"
	s.reverseRemoteObjects[id] = "STALE-BASELINE"

	recovered := func() (r any) {
		defer func() { r = recover() }()
		s.getObjectFromJava(id, "")
		return nil
	}()

	require.Contains(t, fmt.Sprint(recovered), "expected END_OF_OBJECT", "the receive's own failure")
	require.NotContains(t, s.reverseRemoteObjects, id)
	require.Contains(t, s.reverseRemoteRefs, 6)
	require.Equal(t, "package main\n", s.getObjectFromJava(id, ""))
}

func TestDrainPageReadsPastARequestToReachTheGetObjectReply(t *testing.T) {
	s, logs := newResilienceTestServer(t)

	// A bare ref to an object Go never received panics mid-receive, so the
	// deferred drain runs against whatever comes next — here a Visit request
	// Java initiated ahead of the page Go prefetched.
	stream := append(
		frameReverseGetObjectReply(t, []map[string]any{{"state": "ADD", "ref": 7}}),
		append(
			frameReverseRequest(t, "Visit"),
			append(
				frameReverseGetObjectReply(t, []map[string]any{{"state": "END_OF_OBJECT"}}),
				append(
					frameAbortGetObjectReply(t),
					frameReverseGetObjectReply(t, []map[string]any{
						{"state": "ADD", "value": "package main\n"},
						{"state": "END_OF_OBJECT"},
					})...,
				)...,
			)...,
		)...,
	)
	s.reader = bufio.NewReader(bytes.NewReader(stream))
	s.writer = &bytes.Buffer{}

	const id = "tree-X"
	require.Panics(t, func() { s.getObjectFromJava(id, "") })
	require.Contains(t, logs.String(), "Expected the prefetched GetObject page, got a Visit request")

	// The drain having consumed the page, the next transfer reads its own reply.
	require.Equal(t, "package main\n", s.getObjectFromJava(id, ""))
}

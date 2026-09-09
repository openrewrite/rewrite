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

// TestGetObjectFromJavaPanicResetsBaselineButKeepsRefs reproduces the
// receive-stream cascade and pins down the containment contract.
//
// When a reverse GetObject panics mid-receive, the per-id baseline must be
// dropped (so the next transfer of that id re-syncs as a full ADD rather than
// a CHANGE delta against a baseline Go never finished applying — the source of
// the "expected CHANGE with positions" cascade). The shared ref table must NOT
// be wiped: Java's reverse-direction send refs are connection-scoped (cleared
// only on Reset), so discarding Go's would make Java's bare {ref:N} look-ups
// fail with "received reference to unknown object: N".
func TestGetObjectFromJavaPanicResetsBaselineButKeepsRefs(t *testing.T) {
	s, logs := newResilienceTestServer(t)

	// Java's two scripted replies on the reverse GetObject stream:
	//  1) a bare reference to an object Go never received -> panics mid-receive
	//     ("received reference to unknown object: 7"), a documented cascade
	//     signature.
	//  2) the page Go asks for ahead of the panic, since reply 1 does not close
	//     the transfer; the failed transfer drains it rather than leaving it for
	//     the next request to misread.
	//  3) a clean full ADD of a simple value -> the follow-up request.
	stream := append(
		frameReverseGetObjectReply(t, []map[string]any{{"state": "ADD", "ref": 7}}),
		append(
			frameReverseGetObjectReply(t, []map[string]any{{"state": "END_OF_OBJECT"}}),
			frameReverseGetObjectReply(t, []map[string]any{
				{"state": "ADD", "value": "package main\n"},
				{"state": "END_OF_OBJECT"},
			})...,
		)...,
	)
	s.reader = bufio.NewReader(bytes.NewReader(stream))
	s.writer = &bytes.Buffer{} // the GetObject requests Go writes are irrelevant here

	const id = "tree-X"
	// Pre-seed state a live session would hold from prior successful cycles:
	//  - a baseline that diverges from Java's once the receive panics, and
	//  - a shared ref the panic must NOT wipe.
	s.reverseRemoteObjects[id] = "STALE-BASELINE"
	s.reverseRemoteRefs[5] = "shared-value-from-earlier-transfer"

	// Transfer 1: must panic mid-receive.
	panicked := func() (p bool) {
		defer func() {
			if r := recover(); r != nil {
				p = true
			}
		}()
		s.getObjectFromJava(id, "")
		return false
	}()
	require.True(t, panicked, "transfer 1: expected a panic mid-receive, got none")

	// Containment: the diverged per-id baseline is dropped.
	if v, ok := s.reverseRemoteObjects[id]; ok {
		t.Errorf("reverseRemoteObjects[%q] should be deleted after a receive panic, still present: %v", id, v)
	}
	// ...but the shared ref table survives.
	if got := s.reverseRemoteRefs[5]; got != "shared-value-from-earlier-transfer" {
		t.Errorf("reverseRemoteRefs[5] should survive a receive panic, got %v", got)
	}

	// Transfer 2: a fresh request on the same server must succeed cleanly,
	// with no leftover state poisoning the receive.
	got := s.getObjectFromJava(id, "")
	if got != "package main\n" {
		t.Errorf("transfer 2: want clean ADD value %q, got %#v", "package main\n", got)
	}
	if s.reverseRemoteObjects[id] != "package main\n" {
		t.Errorf("transfer 2: baseline should be repopulated, got %#v", s.reverseRemoteObjects[id])
	}
	require.NotContains(t, logs.String(), "Expected the prefetched GetObject page",
		"draining Java's own reply is the ordinary case and must not be reported as a desync")
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
				frameReverseGetObjectReply(t, []map[string]any{
					{"state": "ADD", "value": "package main\n"},
					{"state": "END_OF_OBJECT"},
				})...,
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

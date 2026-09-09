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
	"strings"
	"testing"
)

const fileAttributesType = "org.openrewrite.FileAttributes"

// A source file's fileAttributes is a codec field, not a marker, so it arrives as a typed ADD
// followed by one message per sub-field. Consuming only the ADD leaves the rest to be read as
// whatever field comes next.
func TestReceiveFileAttributesConsumesEverySubField(t *testing.T) {
	valueType := fileAttributesType
	q := queueOf(
		RpcObjectData{State: Add, ValueType: &valueType},
		RpcObjectData{State: Add, Value: "2026-08-16T10:15:30.123456789+02:00[Europe/Berlin]"},
		RpcObjectData{State: NoChange},
		RpcObjectData{State: NoChange},
		RpcObjectData{State: Add, Value: true},
		RpcObjectData{State: Add, Value: true},
		RpcObjectData{State: Add, Value: false},
		RpcObjectData{State: Add, Value: float64(42)},
		RpcObjectData{State: Add, Value: "the next field"},
	)

	receiveFileAttributes(q)

	if got := q.Receive(nil, nil); got != "the next field" {
		t.Fatalf("next field after fileAttributes = %v, want %q", got, "the next field")
	}
}

// A null fileAttributes is a single NO_CHANGE with no sub-fields, so the drain must not run.
func TestReceiveFileAttributesNoChangeConsumesOneMessage(t *testing.T) {
	q := queueOf(
		RpcObjectData{State: NoChange},
		RpcObjectData{State: Add, Value: "the next field"},
	)

	receiveFileAttributes(q)

	if got := q.Receive(nil, nil); got != "the next field" {
		t.Fatalf("next field after fileAttributes = %v, want %q", got, "the next field")
	}
}

func TestReceiveChecksumConsumesEverySubField(t *testing.T) {
	valueType := "org.openrewrite.Checksum"
	q := queueOf(
		RpcObjectData{State: Add, ValueType: &valueType},
		RpcObjectData{State: Add, Value: "SHA-256"},
		RpcObjectData{State: Add, Value: "cafebabe"},
		RpcObjectData{State: Add, Value: "the next field"},
	)

	receiveChecksum(q)

	if got := q.Receive(nil, nil); got != "the next field" {
		t.Fatalf("next field after checksum = %v, want %q", got, "the next field")
	}
}

// Go used to answer a typed ADD it had no codec for by returning the shell and consuming
// nothing, so the sub-fields were read as later fields and the tree came out quietly wrong.
func TestReceiveWithoutCodecPanicsRatherThanDesyncing(t *testing.T) {
	valueType := fileAttributesType
	q := queueOf(RpcObjectData{State: Add, ValueType: &valueType})

	defer func() {
		r := recover()
		if r == nil {
			t.Fatal("want a panic naming the missing codec, got none")
		}
		if msg, ok := r.(string); !ok || !strings.Contains(msg, "no RPC codec registered on the Go side") {
			t.Fatalf("panic = %v, want the missing-codec diagnostic", r)
		}
	}()

	q.Receive(nil, nil)
}

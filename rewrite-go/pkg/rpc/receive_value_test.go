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

func queueOf(msgs ...RpcObjectData) *ReceiveQueue {
	delivered := false
	return NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return msgs
	})
}

// receiveValue mirrors Java's RpcReceiveQueue.receive: NO_CHANGE returns `before`,
// DELETE returns the zero value (nil for the nullable pointer/interface fields it is
// used on). These lock in the semantics the call-site assignments rely on.

func TestReceiveValue_NoChangeReturnsBefore(t *testing.T) {
	before := makeIdent("x")
	got := receiveValue(queueOf(RpcObjectData{State: NoChange}), java.Expression(before),
		func(e java.Expression) any { return e })
	if got != java.Expression(before) {
		t.Errorf("NO_CHANGE: want before %p, got %v", before, got)
	}
}

func TestReceiveValue_DeleteReturnsNil(t *testing.T) {
	before := makeIdent("x")
	got := receiveValue(queueOf(RpcObjectData{State: Delete}), java.Expression(before),
		func(e java.Expression) any { return e })
	if got != nil {
		t.Errorf("DELETE: want nil, got %v", got)
	}
}

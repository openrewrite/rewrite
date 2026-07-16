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

import "testing"

func TestReferenceMapReusesStrongIdentity(t *testing.T) {
	type object struct{ value int }
	shared := &object{value: 1}
	refs := NewReferenceMap()

	firstID, existed := refs.GetOrCreate(shared)
	if existed || firstID != 1 {
		t.Fatalf("first allocation = (%d, %v), want (1, false)", firstID, existed)
	}
	if got := refs.Len(); got != 1 {
		t.Fatalf("reference count = %d, want 1", got)
	}
	if id, ok := refs.GetOrCreate(shared); !ok || id != firstID {
		t.Fatalf("reused allocation = (%d, %v), want (%d, true)", id, ok, firstID)
	}
}

func TestSendQueueDiscardDoesNotReuseReferenceIDs(t *testing.T) {
	type object struct{ value int }
	refs := NewReferenceMap()

	firstQueue := NewSendQueue(10, func([]RpcObjectData) {}, refs)
	firstQueue.add(AsRef(&object{value: 1}), nil)
	rolledBackID := *firstQueue.batch[0].Ref
	firstQueue.DiscardNewReferences()
	if got := refs.Len(); got != 0 {
		t.Fatalf("reference count after rollback = %d, want 0", got)
	}

	secondQueue := NewSendQueue(10, func([]RpcObjectData) {}, refs)
	secondQueue.add(AsRef(&object{value: 2}), nil)
	committedID := *secondQueue.batch[0].Ref
	if committedID <= rolledBackID {
		t.Fatalf("reference ID after rollback = %d, want greater than %d", committedID, rolledBackID)
	}
	if got := refs.Len(); got != 1 {
		t.Fatalf("reference count after commit = %d, want 1", got)
	}
}

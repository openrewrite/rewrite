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

func TestReferenceTransactionsPublishOnlyOnCommit(t *testing.T) {
	type object struct{}
	shared := &object{}
	refs := NewReferenceMap()

	first := refs.Begin()
	firstID, existed := first.GetOrCreate(shared)
	if existed || firstID != 1 {
		t.Fatalf("first allocation = (%d, %v), want (1, false)", firstID, existed)
	}
	if got := refs.Len(); got != 0 {
		t.Fatalf("published refs before commit = %d, want 0", got)
	}

	second := refs.Begin()
	secondID, existed := second.GetOrCreate(shared)
	if existed || secondID != 2 {
		t.Fatalf("isolated allocation = (%d, %v), want (2, false)", secondID, existed)
	}
	second.Commit()

	if got := refs.Len(); got != 1 {
		t.Fatalf("published refs after commit = %d, want 1", got)
	}
	if id, ok := refs.GetOrCreate(shared); !ok || id != secondID {
		t.Fatalf("committed allocation = (%d, %v), want (%d, true)", id, ok, secondID)
	}

	// A later commit for the same sender object must not replace the ID that
	// was published first.
	first.Commit()
	if id, ok := refs.GetOrCreate(shared); !ok || id != secondID {
		t.Fatalf("allocation after competing commit = (%d, %v), want (%d, true)", id, ok, secondID)
	}
}

func TestReferenceTransactionRollbackDoesNotReuseIDs(t *testing.T) {
	type object struct{ value int }
	refs := NewReferenceMap()

	rolledBack := refs.Begin()
	rolledBackID, existed := rolledBack.GetOrCreate(&object{value: 1})
	if existed {
		t.Fatal("new transaction unexpectedly reused a reference")
	}
	rolledBack.Rollback()

	committed := refs.Begin()
	committedID, existed := committed.GetOrCreate(&object{value: 2})
	if existed {
		t.Fatal("new object unexpectedly reused a reference")
	}
	if committedID <= rolledBackID {
		t.Fatalf("reference ID after rollback = %d, want greater than %d", committedID, rolledBackID)
	}
	committed.Commit()
}

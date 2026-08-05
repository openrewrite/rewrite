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

// A changed ref-deduplicated slot is re-added under a fresh ref, never CHANGEd
// (see Send for why).
func TestChangedRefSlotIsReAddedInsteadOfChanged(t *testing.T) {
	type object struct{ value int }
	refs := NewReferenceMap()
	q := NewSendQueue(10, func([]RpcObjectData) {}, refs)

	t1 := &object{value: 1}
	t2 := &object{value: 2}

	q.Send(AsRef(t1), nil, nil)
	q.Send(AsRef(t2), AsRef(t1), nil)
	// A repeat of the same transition dedups against the ref registered by the re-add
	q.Send(AsRef(t2), AsRef(t1), nil)

	if len(q.batch) != 3 {
		t.Fatalf("batch length = %d, want 3", len(q.batch))
	}
	for i, d := range q.batch {
		if d.State != Add {
			t.Fatalf("batch[%d].State = %v, want Add", i, d.State)
		}
	}
	if *q.batch[0].Ref != 1 || *q.batch[1].Ref != 2 {
		t.Fatalf("refs = %d, %d, want 1, 2", *q.batch[0].Ref, *q.batch[1].Ref)
	}
	if q.batch[2].Ref == nil || *q.batch[2].Ref != 2 || q.batch[2].Value != nil {
		t.Fatalf("batch[2] = %+v, want ref-only ADD with ref 2", q.batch[2])
	}
}

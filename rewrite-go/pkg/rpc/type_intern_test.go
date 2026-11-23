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

func poolQueue(pool map[string]java.JavaType) *ReceiveQueue {
	return NewReceiveQueue(make(map[int]any), func() []RpcObjectData { return nil }).WithTypePool(pool)
}

func TestInternType_CanonicalizesEqualSignaturesAcrossQueues(t *testing.T) {
	// given a pool shared across two transfers (separate queues, same connection)
	pool := map[string]java.JavaType{}

	// when the same logical primitive arrives as a distinct object in each
	first := poolQueue(pool).internType(&java.JavaTypePrimitive{Keyword: "int"})
	second := poolQueue(pool).internType(&java.JavaTypePrimitive{Keyword: "int"})

	// then both collapse to one canonical instance
	if first != second {
		t.Errorf("expected canonical identity across transfers, got %p and %p", first, second)
	}
}

func TestInternType_KeepsDistinctSignaturesSeparate(t *testing.T) {
	// given a shared pool
	pool := map[string]java.JavaType{}
	q := poolQueue(pool)

	// when two different primitives are interned
	i := q.internType(&java.JavaTypePrimitive{Keyword: "int"})
	l := q.internType(&java.JavaTypePrimitive{Keyword: "long"})

	// then they are not merged
	if i == l {
		t.Errorf("distinct signatures must not share identity: %p", i)
	}
}

func TestInternType_FirstWins(t *testing.T) {
	// given a primitive already registered
	pool := map[string]java.JavaType{}
	q := poolQueue(pool)
	canonical := q.internType(&java.JavaTypePrimitive{Keyword: "boolean"})

	// when a structurally-equal duplicate is interned
	dup := &java.JavaTypePrimitive{Keyword: "boolean"}
	got := q.internType(dup)

	// then the canonical is returned and the duplicate is discarded
	if got != canonical {
		t.Errorf("want canonical %p, got %p", canonical, got)
	}
	if got == java.JavaType(dup) {
		t.Errorf("duplicate should not become canonical")
	}
}

func TestInternType_NilPoolIsNoOp(t *testing.T) {
	// given a queue with no pool (e.g. a test or a non-shared transfer)
	q := NewReceiveQueue(make(map[int]any), func() []RpcObjectData { return nil })

	// when two equal primitives are interned
	a := q.internType(&java.JavaTypePrimitive{Keyword: "int"})
	b := q.internType(&java.JavaTypePrimitive{Keyword: "int"})

	// then nothing is canonicalized — identity is preserved as-is
	if a == b {
		t.Errorf("nil pool must not canonicalize: %p", a)
	}
}

func TestVisitPrimitive_InternsAcrossTransfers(t *testing.T) {
	// given the type receiver and a pool shared across two transfers
	r := NewJavaTypeReceiver()
	pool := map[string]java.JavaType{}

	// when the same primitive is received on two separate queues (Keyword
	// arrives as NO_CHANGE against the pre-seeded baseline)
	q1 := poolQueue(pool)
	q1.batch = []RpcObjectData{{State: NoChange}}
	first := r.VisitPrimitive(&java.JavaTypePrimitive{Keyword: "int"}, q1)

	q2 := poolQueue(pool)
	q2.batch = []RpcObjectData{{State: NoChange}}
	second := r.VisitPrimitive(&java.JavaTypePrimitive{Keyword: "int"}, q2)

	// then the receive path yields one canonical instance
	if first != second {
		t.Errorf("VisitPrimitive should canonicalize across transfers, got %p and %p", first, second)
	}
}

func TestInternType_ArrayCanonicalizes(t *testing.T) {
	// given a shared pool
	pool := map[string]java.JavaType{}

	// when annotation-free arrays of the same element type are interned
	mk := func() *java.JavaTypeArray {
		return &java.JavaTypeArray{ElemType: &java.JavaTypePrimitive{Keyword: "int"}}
	}
	a := poolQueue(pool).internType(mk())
	b := poolQueue(pool).internType(mk())

	// then they collapse to one instance
	if a != b {
		t.Errorf("equal arrays should canonicalize, got %p and %p", a, b)
	}
}

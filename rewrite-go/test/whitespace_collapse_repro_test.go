/*
 * Copyright 2025 the original author or authors.
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

package test

import (
	"testing"

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

const collapseReproSrc = `package main

import "fmt"

func Bind(x int) int {
	value := x
	if value == 1 {
		value = 2
	}
	fmt.Println(value, x)
	return value
}
`

// ifThenBlockLocator captures the *java.Block that is the then-branch of the
// untouched `if value == 1 { ... }`, keyed by its stable ID.
type ifThenBlockLocator struct {
	visitor.GoVisitor
	block *java.Block
	id    uuid.UUID
}

func (v *ifThenBlockLocator) VisitIf(ifStmt *java.If, p any) java.J {
	if b, ok := ifStmt.ThenPart.Element.(*java.Block); ok {
		v.block = b
		v.id = b.ID
	}
	return ifStmt
}

func locateIfThenBlock(t *testing.T, root java.Tree) *java.Block {
	t.Helper()
	loc := visitor.Init(&ifThenBlockLocator{})
	loc.Visit(root, nil)
	if loc.block == nil {
		t.Fatal("could not locate the if-then block in the parsed tree")
	}
	return loc.block
}

// TestNoOpVisitPreservesUntouchedBlockIdentity: the untouched `if` block keeps a
// stable identity across a no-op visit. This is exactly the subtree that
// collapses to `if value == 1 {value = 2}` on the wire.
func TestNoOpVisitPreservesUntouchedBlockIdentity(t *testing.T) {
	// given
	p := parser.NewGoParser()
	cu, err := p.Parse("bind.go", collapseReproSrc)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	before := locateIfThenBlock(t, cu)

	// when
	result := visitor.Init(&visitor.GoVisitor{}).Visit(cu, nil)
	after := locateIfThenBlock(t, result)

	// then
	if before.ID != after.ID {
		t.Fatalf("block ID changed across visit: %s -> %s", before.ID, after.ID)
	}
	if before != after {
		t.Errorf("no-op visit reallocated the untouched if-then block (id %s); a "+
			"fresh pointer makes the sender diverge from the receiver baseline, so a "+
			"NO_CHANGE delta resolves against a diverged node and the block's "+
			"whitespace is dropped\n  before: %p\n  after:  %p", before.ID, before, after)
	}
}

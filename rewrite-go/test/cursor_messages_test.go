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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func threeFrameCursor(t *testing.T) (*visitor.Cursor, *visitor.Cursor, *visitor.Cursor) {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("a.go", "package main\n")
	if err != nil {
		t.Fatal(err)
	}
	root := visitor.NewCursor(nil, cu)
	mid := visitor.NewCursor(root, cu)
	leaf := visitor.NewCursor(mid, cu)
	return root, mid, leaf
}

func TestCursorPutGetMessageThisFrameOnly(t *testing.T) {
	_, _, leaf := threeFrameCursor(t)
	leaf.PutMessage("k", 42)
	if got := leaf.GetMessage("k"); got != 42 {
		t.Fatalf("expected 42 on leaf, got %v", got)
	}
	// GetMessage does NOT walk up — value stays on the frame it was set.
	if got := leaf.Parent().GetMessage("k"); got != nil {
		t.Fatalf("expected nil on parent, got %v", got)
	}
}

func TestCursorGetNearestMessageWalksUp(t *testing.T) {
	root, _, leaf := threeFrameCursor(t)
	root.PutMessage("k", "from-root")
	if got := leaf.GetNearestMessage("k"); got != "from-root" {
		t.Fatalf("expected leaf to find root's message, got %v", got)
	}
	if got := leaf.GetNearestMessage("missing"); got != nil {
		t.Fatalf("expected nil for missing key, got %v", got)
	}
	if got := leaf.GetNearestMessageOrDefault("missing", "fallback"); got != "fallback" {
		t.Fatalf("expected fallback, got %v", got)
	}
}

func TestCursorPollNearestMessageRemoves(t *testing.T) {
	root, _, leaf := threeFrameCursor(t)
	root.PutMessage("k", "v")
	if got := leaf.PollNearestMessage("k"); got != "v" {
		t.Fatalf("expected to poll 'v' from root, got %v", got)
	}
	if got := root.GetMessage("k"); got != nil {
		t.Fatalf("expected key to be removed from root, got %v", got)
	}
	if got := leaf.PollNearestMessage("k"); got != nil {
		t.Fatalf("expected nil after poll, got %v", got)
	}
}

func TestCursorComputeMessageIfAbsent(t *testing.T) {
	_, _, leaf := threeFrameCursor(t)
	calls := 0
	v1 := leaf.ComputeMessageIfAbsent("k", func() any {
		calls++
		return "computed"
	})
	v2 := leaf.ComputeMessageIfAbsent("k", func() any {
		calls++
		return "second"
	})
	if v1 != "computed" || v2 != "computed" {
		t.Fatalf("expected stable computed value, got v1=%v v2=%v", v1, v2)
	}
	if calls != 1 {
		t.Fatalf("expected supplier to fire once, fired %d", calls)
	}
}

func TestCursorPutMessageOnFirstEnclosing(t *testing.T) {
	root, _, leaf := threeFrameCursor(t)
	// Stash a message on the first ancestor whose value is a CompilationUnit.
	leaf.PutMessageOnFirstEnclosing(func(t tree.Tree) bool {
		_, ok := t.(*tree.CompilationUnit)
		return ok
	}, "tag", "matched")
	// Found ancestor (every frame in this fixture is the same CU); the
	// leaf itself matches first.
	if got := leaf.GetMessage("tag"); got != "matched" {
		t.Fatalf("expected leaf to receive the message, got %v", got)
	}
	// Sanity: a predicate matching nothing leaves the chain untouched.
	leaf.PutMessageOnFirstEnclosing(func(tree.Tree) bool { return false }, "x", 1)
	if got := root.GetMessage("x"); got != nil {
		t.Fatalf("expected no-op when predicate matches nothing, got %v", got)
	}
}

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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func TestCursorBuildChain(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\n")
	if err != nil {
		t.Fatal(err)
	}
	chain := visitor.BuildChain([]tree.Tree{cu})
	if chain == nil || chain.Value() != cu || chain.Parent() != nil {
		t.Fatalf("expected single-element chain rooted at cu; got parent=%v value=%v", chain.Parent(), chain.Value())
	}

	chain2 := visitor.BuildChain(nil)
	if chain2 != nil {
		t.Fatalf("expected nil chain for empty input, got %v", chain2)
	}
}

// TestVisitorCursorState confirms that GoVisitor exposes its cursor as
// state via Cursor() / SetCursor(), matching the JavaVisitor pattern.
// The RPC layer seeds an initial cursor before traversal; recipes read
// it from inside any Visit* override.
func TestVisitorCursorState(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\nfunc f(){}\n")
	if err != nil {
		t.Fatal(err)
	}

	v := &cursorObservingVisitor{}
	visitor.Init(v)

	outer := visitor.BuildChain([]tree.Tree{cu})
	v.SetCursor(outer)
	if v.Cursor() != outer {
		t.Fatalf("Cursor() should return what SetCursor seeded")
	}

	v.Visit(cu, recipe.NewExecutionContext())
	if !v.observedCU {
		t.Fatal("VisitCompilationUnit was never invoked")
	}
	if v.cuCursor == nil || v.cuCursor.Value() != cu {
		t.Fatalf("expected v.Cursor().Value() == cu inside VisitCompilationUnit; got %v", v.cuCursor)
	}
}

type cursorObservingVisitor struct {
	visitor.GoVisitor
	observedCU bool
	cuCursor   *visitor.Cursor
}

func (v *cursorObservingVisitor) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	v.observedCU = true
	v.cuCursor = v.Cursor()
	return cu
}

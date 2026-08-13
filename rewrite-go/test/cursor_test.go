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

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func TestCursorBuildChain(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\n")
	require.NoError(t, err)
	chain := visitor.BuildChain([]java.Tree{cu})
	require.Falsef(t, chain == nil || chain.Value() != cu || chain.Parent() != nil, "expected single-element chain rooted at cu; got parent=%v value", chain.Parent())

	chain2 := visitor.BuildChain(nil)
	require.Nil(t, chain2, "expected nil chain for empty input")
}

// TestVisitorCursorState confirms that GoVisitor exposes its cursor as
// state via Cursor() / SetCursor(), matching the JavaVisitor pattern.
// The RPC layer seeds an initial cursor before traversal; recipes read
// it from inside any Visit* override.
func TestVisitorCursorState(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\nfunc f(){}\n")
	require.NoError(t, err)

	v := &cursorObservingVisitor{}
	visitor.Init(v)

	outer := visitor.BuildChain([]java.Tree{cu})
	v.SetCursor(outer)
	if v.Cursor() != outer {
		t.Fatalf("Cursor() should return what SetCursor seeded")
	}

	v.Visit(cu, recipe.NewExecutionContext())
	require.True(t, v.observedCU, "VisitCompilationUnit was never invoked")
	require.False(t, v.cuCursor == nil || v.cuCursor.Value() != cu, "expected v.Cursor().Value() == cu inside VisitCompilationUnit")
}

type cursorObservingVisitor struct {
	visitor.GoVisitor
	observedCU bool
	cuCursor   *visitor.Cursor
}

func (v *cursorObservingVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	v.observedCU = true
	v.cuCursor = v.Cursor()
	return cu
}

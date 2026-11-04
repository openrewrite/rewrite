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

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// Go's condition-only `for cond {}` and infinite `for {}` fill their init/update
// slots with synthetic J.Empty placeholders so J.ForLoop.Control keeps the
// single-element-list shape a JavaVisitor may index at 0. The golang.
// ImplicitForClauses marker records that they are synthetic. Both the
// placeholders and the marker must survive the RPC wire, or the loop would
// re-print as a 3-clause `for ; cond ; {}`.
func TestImplicitForClausesSurvivesRpcRoundTrip(t *testing.T) {
	cases := []string{
		"package main\n\nfunc f(data []byte) {\n\tfor len(data) > 0 {\n\t}\n}\n",
		"package main\n\nfunc f() {\n\tfor {\n\t}\n}\n",
	}
	for _, src := range cases {
		src := src
		t.Run(src, func(t *testing.T) {
			// given: a parsed for-loop that already prints back to its source
			cu, err := parser.NewGoParser().Parse("f.go", src)
			require.NoError(t, err, "parse")
			if got := printer.Print(cu); got != src {
				t.Fatalf("parse-print idempotence failed:\n got=%q\nwant=%q", got, src)
			}

			// when: the whole compilation unit round-trips through the Go
			// sender/receiver (the path a recipe edit forces)
			seed := &golang.CompilationUnit{ID: cu.ID}
			rt := roundTripNode(t, cu, seed).(java.Tree)

			// then: it still prints identically...
			if got := printer.Print(rt); got != src {
				t.Errorf("for-loop shape corrupted on round-trip:\n got=%q\nwant=%q", got, src)
			}
			// ...and the control still carries the placeholders and the marker
			control := firstForControl(t, rt)
			assert.NotNil(t, control.Init, "Init placeholder lost on round-trip")
			assert.NotNil(t, control.Update, "Update placeholder lost on round-trip")
			assert.NotNil(t, java.FindMarker[golang.ImplicitForClauses](control.Markers), "ImplicitForClauses marker lost on round-trip")
		})
	}
}

// firstForControl returns the ForControl of the first for-loop found under t.
func firstForControl(t *testing.T, tree java.Tree) *java.ForControl {
	t.Helper()
	cu, ok := tree.(*golang.CompilationUnit)
	require.Truef(t, ok, "expected *golang.CompilationUnit, got %T", tree)
	for _, st := range cu.Statements {
		fn, ok := st.Element.(*java.MethodDeclaration)
		if !ok || fn.Body == nil {
			continue
		}
		for _, bs := range fn.Body.Statements {
			if loop, ok := bs.Element.(*java.ForLoop); ok {
				return &loop.Control
			}
		}
	}
	t.Fatalf("no *java.ForLoop found")
	return nil
}

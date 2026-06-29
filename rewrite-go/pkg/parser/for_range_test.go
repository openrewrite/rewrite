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

package parser_test

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// nthStatementInBody parses src and returns the n-th (0-based) statement of the
// first top-level function's body.
func nthStatementInBody(t *testing.T, src string, n int) java.Statement {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("body.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	fn, ok := cu.Statements[0].Element.(*java.MethodDeclaration)
	if !ok {
		t.Fatalf("expected first statement to be *java.MethodDeclaration, got %T", cu.Statements[0].Element)
	}
	if fn.Body == nil || len(fn.Body.Statements) <= n {
		t.Fatalf("function body has fewer than %d statements", n+1)
	}
	return fn.Body.Statements[n].Element
}

// forEachControlOf parses src and returns the ForEachControl of the first
// for-range loop in the first function body.
func forEachControlOf(t *testing.T, src string) *java.ForEachControl {
	t.Helper()
	stmt := firstStatementInBody(t, src)
	loop, ok := stmt.(*java.ForEachLoop)
	if !ok {
		t.Fatalf("expected first statement to be *java.ForEachLoop, got %T", stmt)
	}
	return &loop.Control
}

// A keyless `for range expr {}` has no loop target; the Variable slot holds a
// J.Empty so the round trip stays keyless (mirroring J.ForEachLoop.Control).
func TestForRangeKeylessUsesEmpty(t *testing.T) {
	// given
	src := "package main\n\nfunc f(items []int) {\n\tfor range items {\n\t}\n}\n"

	// when
	control := forEachControlOf(t, src)

	// then
	if _, ok := control.Variable.Element.(*java.Empty); !ok {
		t.Fatalf("keyless range Variable must be *java.Empty, got %T", control.Variable.Element)
	}
}

// `for k, v := range expr {}` carries both targets and the `:=` operator in a
// golang.MultiAssignment, with a ShortVarDecl marker for the `:=` spelling.
func TestForRangeTwoVarsDefineUsesMultiAssignment(t *testing.T) {
	// given
	src := "package main\n\nfunc f(items []int) {\n\tfor k, v := range items {\n\t\t_ = k\n\t\t_ = v\n\t}\n}\n"

	// when
	control := forEachControlOf(t, src)

	// then
	ma, ok := control.Variable.Element.(*golang.MultiAssignment)
	if !ok {
		t.Fatalf("range head Variable must be *golang.MultiAssignment, got %T", control.Variable.Element)
	}
	if len(ma.Variables) != 2 {
		t.Fatalf("expected 2 loop targets, got %d", len(ma.Variables))
	}
	if java.FindMarker[golang.ShortVarDecl](ma.Markers) == nil {
		t.Fatalf("`:=` range must carry a ShortVarDecl marker")
	}
}

// `for k, v = range expr {}` uses `=`, so the MultiAssignment must NOT carry a
// ShortVarDecl marker — this is the distinction the old single-variable mapping
// dropped (it always re-emitted `:=`).
func TestForRangeAssignHasNoShortVarDeclMarker(t *testing.T) {
	// given
	src := "package main\n\nfunc f(items []int) {\n\tvar k, v int\n\tfor k, v = range items {\n\t}\n\t_ = k\n\t_ = v\n}\n"

	// when
	stmt := nthStatementInBody(t, src, 1) // the for-range, after `var k, v int`
	loop, ok := stmt.(*java.ForEachLoop)
	if !ok {
		t.Fatalf("expected *java.ForEachLoop, got %T", stmt)
	}
	ma, ok := loop.Control.Variable.Element.(*golang.MultiAssignment)
	if !ok {
		t.Fatalf("range head Variable must be *golang.MultiAssignment, got %T", loop.Control.Variable.Element)
	}

	// then
	if java.FindMarker[golang.ShortVarDecl](ma.Markers) != nil {
		t.Fatalf("`=` range must NOT carry a ShortVarDecl marker")
	}
}

// Every for-range variant must round-trip parse → print byte-for-byte.
func TestForRangeVariantsRoundTrip(t *testing.T) {
	cases := []string{
		"package main\n\nfunc f(items []int) {\n\tfor range items {\n\t}\n}\n",
		"package main\n\nfunc f(items []int) {\n\tfor k := range items {\n\t\t_ = k\n\t}\n}\n",
		"package main\n\nfunc f(items []int) {\n\tfor k, v := range items {\n\t\t_ = k\n\t\t_ = v\n\t}\n}\n",
		"package main\n\nfunc f(items []int) {\n\tfor _, v := range items {\n\t\t_ = v\n\t}\n}\n",
		"package main\n\nfunc f(items []int) {\n\tvar k, v int\n\tfor k, v = range items {\n\t}\n\t_ = k\n\t_ = v\n}\n",
		"package main\n\nfunc f(items []int, m map[int]int) {\n\tvar v int\n\tfor m[0], v = range items {\n\t}\n\t_ = v\n}\n",
	}
	for _, src := range cases {
		src := src
		t.Run(src, func(t *testing.T) {
			assertRoundTrip(t, src)
		})
	}
}

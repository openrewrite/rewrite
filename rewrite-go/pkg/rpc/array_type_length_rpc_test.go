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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// A fixed-size array type `[N]T` carries an inline length expression that
// Java's J.ArrayType has no slot for. Modelling it as golang.ArrayType lets the
// length travel the RPC wire, so a Print round-trip of `[5]int` stays `[5]int`.
func TestArrayTypeLengthSurvivesPrintRoundTrip(t *testing.T) {
	// given: a Go file using a fixed-size array type `[5]int`
	src := "package main\n\nfunc process(data [5]int) {\n}\n"
	cu, err := parser.NewGoParser().Parse("a.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	if got := printer.Print(cu); got != src {
		t.Fatalf("parse-print idempotence failed:\n got=%q\nwant=%q", got, src)
	}

	// when: the whole compilation unit makes a full Print round-trip through the
	// Go sender/receiver (the path a recipe edit forces, no NO_CHANGE shortcut)
	seed := &golang.CompilationUnit{ID: cu.ID}
	rt := roundTripNode(t, cu, seed).(java.Tree)

	// then: the length `5` survives — it still prints `[5]int`, not `[]int`
	if got := printer.Print(rt); got != src {
		t.Errorf("array length dropped on round-trip:\n got=%q\nwant=%q", got, src)
	}
}

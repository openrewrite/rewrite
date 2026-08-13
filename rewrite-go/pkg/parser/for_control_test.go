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

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// forControl parses src and returns the ForControl of the first for-loop in the
// first top-level function's body.
func forControl(t *testing.T, src string) *java.ForControl {
	t.Helper()
	loop, ok := firstStatementInBody(t, src).(*java.ForLoop)
	if !ok {
		t.Fatalf("expected first statement to be *java.ForLoop, got %T", firstStatementInBody(t, src))
	}
	return &loop.Control
}

// The J.ForLoop.Control contract every other parser upholds is that init and
// update are single-element lists a visitor can index at 0. Go's condition-only
// `for cond {}` must therefore carry a J.Empty in both slots, marked implicit.
func TestConditionOnlyForHasEmptyInitAndUpdate(t *testing.T) {
	// given
	src := "package main\n\nfunc f(data []byte) {\n\tfor len(data) > 0 {\n\t}\n}\n"

	// when
	control := forControl(t, src)

	// then
	require.NotNil(t, control.Init)
	if _, ok := control.Init.Element.(*java.Empty); !ok {
		t.Fatalf("Init element must be *java.Empty, got %T", control.Init.Element)
	}
	require.NotNil(t, control.Update)
	if _, ok := control.Update.Element.(*java.Empty); !ok {
		t.Fatalf("Update element must be *java.Empty, got %T", control.Update.Element)
	}
	require.NotNil(t, control.Condition)
	require.NotNil(t, java.FindMarker[golang.ImplicitForClauses](control.Markers))
}

// An infinite `for {}` has no condition, but its init/update placeholders must
// still be present and marked implicit.
func TestInfiniteForHasEmptyInitAndUpdate(t *testing.T) {
	// given
	src := "package main\n\nfunc f() {\n\tfor {\n\t}\n}\n"

	// when
	control := forControl(t, src)

	// then
	require.NotNil(t, control.Init)
	if _, ok := control.Init.Element.(*java.Empty); !ok {
		t.Fatalf("Init element must be *java.Empty, got %T", control.Init.Element)
	}
	require.NotNil(t, control.Update)
	require.Nil(t, control.Condition)
	require.NotNil(t, java.FindMarker[golang.ImplicitForClauses](control.Markers))
}

// A genuine 3-clause for keeps its real init and is NOT marked implicit; an
// omitted update clause still gets a J.Empty placeholder so the list contract
// holds, without acquiring the marker.
func TestThreeClauseForIsNotMarkedImplicit(t *testing.T) {
	// given
	src := "package main\n\nfunc f() {\n\tfor i := 0; i < 10; {\n\t}\n}\n"

	// when
	control := forControl(t, src)

	// then
	require.Nil(t, java.FindMarker[golang.ImplicitForClauses](control.Markers))
	require.NotNil(t, control.Init)
	if _, ok := control.Init.Element.(*java.Empty); ok {
		t.Fatalf("Init element must be the real init statement, not J.Empty")
	}
	require.NotNil(t, control.Update)
	if _, ok := control.Update.Element.(*java.Empty); !ok {
		t.Fatalf("omitted update must be a *java.Empty placeholder, got %T", control.Update.Element)
	}
}

// Every for-loop form must round-trip parse → print byte-for-byte, proving the
// synthetic placeholders and the marker do not corrupt printing.
func TestForLoopFormsRoundTrip(t *testing.T) {
	cases := []string{
		"package main\n\nfunc f() {\n\tfor {\n\t}\n}\n",
		"package main\n\nfunc f(data []byte) {\n\tfor len(data) > 0 {\n\t}\n}\n",
		"package main\n\nfunc f() {\n\tfor i := 0; i < 10; i++ {\n\t}\n}\n",
		"package main\n\nfunc f() {\n\tfor i := 0; i < 10; {\n\t\ti++\n\t}\n}\n",
		"package main\n\nfunc f(cond bool) {\n\tfor ; cond; {\n\t}\n}\n",
		"package main\n\nfunc f() {\n\tfor ; ; {\n\t}\n}\n",
	}
	for _, src := range cases {
		src := src
		t.Run(src, func(t *testing.T) {
			assertRoundTrip(t, src)
		})
	}
}

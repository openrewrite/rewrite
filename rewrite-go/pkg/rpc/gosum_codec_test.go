/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
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
)

// TestGoSumRPCRoundTrip parses go.sum into a GoSum, ships it Go→wire→Go through
// the sender/receiver, and asserts the received tree prints identically — i.e.
// the RPC codec preserves the lossless tree.
func TestGoSumRPCRoundTrip(t *testing.T) {
	cases := map[string]string{
		"single": "github.com/a/b v1.0.0 h1:aaaa=\n" +
			"github.com/a/b v1.0.0/go.mod h1:bbbb=\n",
		"multiple": "github.com/a/b v1.0.0 h1:aaaa=\n" +
			"github.com/a/b v1.0.0/go.mod h1:bbbb=\n" +
			"github.com/c/d v2.3.4 h1:cccc=\n" +
			"github.com/c/d v2.3.4/go.mod h1:dddd=\n",
		"blank_lines": "github.com/a/b v1.0.0 h1:aaaa=\n\ngithub.com/c/d v2.0.0 h1:cccc=\n",
		"no_newline":  "github.com/a/b v1.0.0 h1:aaaa=",
	}

	for name, content := range cases {
		t.Run(name, func(t *testing.T) {
			// given: a parsed GoSum LST
			before, err := parser.ParseGoSumFile("go.sum", content)
			if err != nil {
				t.Fatalf("parse error: %v", err)
			}

			// when: round-tripped through the RPC sender/receiver
			seed := &golang.GoSum{Ident: before.Ident}
			got := roundTripNode(t, before, seed)

			// then: the received tree prints identically to the original
			gs, ok := got.(*golang.GoSum)
			if !ok {
				t.Fatalf("expected *golang.GoSum, got %T", got)
			}
			if printed := printer.PrintGoSum(gs); printed != content {
				t.Fatalf("RPC round-trip not lossless\n--- want ---\n%q\n--- got ---\n%q", content, printed)
			}
		})
	}
}

// TestGoSumRPCPreservesResolutionMarker verifies a GoResolutionResult attached
// to GoSum.Markers survives the RPC round-trip.
func TestGoSumRPCPreservesResolutionMarker(t *testing.T) {
	content := "github.com/x/y v1.2.3 h1:aaaa=\n" +
		"github.com/x/y v1.2.3/go.mod h1:bbbb=\n"
	before, err := parser.ParseGoSumFile("go.sum", content)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	mrr, err := parser.ParseGoMod("go.mod", "module example.com/foo\n\nrequire github.com/x/y v1.2.3\n")
	if err != nil {
		t.Fatalf("marker parse error: %v", err)
	}
	before.Markers.Entries = append(before.Markers.Entries, *mrr)

	seed := &golang.GoSum{Ident: before.Ident}
	got := roundTripNode(t, before, seed).(*golang.GoSum)

	var found *golang.GoResolutionResult
	for i := range got.Markers.Entries {
		if r, ok := got.Markers.Entries[i].(golang.GoResolutionResult); ok {
			found = &r
		}
	}
	if found == nil {
		t.Fatalf("GoResolutionResult marker lost in round-trip; markers=%#v", got.Markers.Entries)
	}
	if found.ModulePath != "example.com/foo" || len(found.Requires) != 1 {
		t.Fatalf("marker fields not preserved: %#v", found)
	}
}

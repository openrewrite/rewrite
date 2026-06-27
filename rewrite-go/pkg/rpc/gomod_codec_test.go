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

// TestGoModRPCRoundTrip parses go.mod into a GoMod, ships it Go→wire→Go through
// the sender/receiver, and asserts the received tree prints identically — i.e.
// the RPC codec preserves the lossless tree.
func TestGoModRPCRoundTrip(t *testing.T) {
	cases := map[string]string{
		"minimal":  "module example.com/foo\n",
		"full":     "module example.com/foo\n\ngo 1.21\n\ntoolchain go1.22.0\n",
		"requires": "module example.com/foo\n\nrequire (\n\tgithub.com/a/b v1.0.0\n\tgithub.com/c/d v1.5.0 // indirect\n)\n",
		"replace":  "module example.com/foo\n\nreplace github.com/old v1 => ../local/path\n",
		"retract":  "module example.com/foo\n\nretract (\n\tv1.0.0 // buggy\n\t[v1.2.0, v1.3.0]\n)\n",
		"comments": "// header\nmodule example.com/foo\n\n// deps\nrequire github.com/x/y v1.2.3 // indirect\n",
		"unknown":  "module example.com/foo\n\nsomenewdirective foo bar\n",
	}

	for name, content := range cases {
		t.Run(name, func(t *testing.T) {
			// given: a parsed GoMod LST
			before, err := parser.ParseGoModFile("go.mod", content)
			if err != nil {
				t.Fatalf("parse error: %v", err)
			}

			// when: round-tripped through the RPC sender/receiver
			seed := &golang.GoMod{Ident: before.Ident}
			got := roundTripNode(t, before, seed)

			// then: the received tree prints identically to the original
			gm, ok := got.(*golang.GoMod)
			if !ok {
				t.Fatalf("expected *golang.GoMod, got %T", got)
			}
			if printed := printer.PrintGoMod(gm); printed != content {
				t.Fatalf("RPC round-trip not lossless\n--- want ---\n%q\n--- got ---\n%q", content, printed)
			}
		})
	}
}

// TestGoModRPCPreservesResolutionMarker verifies a GoResolutionResult attached
// to GoMod.Markers survives the RPC round-trip.
func TestGoModRPCPreservesResolutionMarker(t *testing.T) {
	content := "module example.com/foo\n\ngo 1.21\n\nrequire github.com/x/y v1.2.3\n"
	before, err := parser.ParseGoModFile("go.mod", content)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	mrr, err := parser.ParseGoMod("go.mod", content)
	if err != nil {
		t.Fatalf("marker parse error: %v", err)
	}
	before.Markers.Entries = append(before.Markers.Entries, *mrr)

	seed := &golang.GoMod{Ident: before.Ident}
	got := roundTripNode(t, before, seed).(*golang.GoMod)

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

// TestGoModRPCPreservesModuleGraph verifies the resolved module graph fields
// added to GoResolutionResult survive the Go↔wire round-trip in lockstep with
// the Java codec.
func TestGoModRPCPreservesModuleGraph(t *testing.T) {
	content := "module example.com/foo\n\ngo 1.21\n\nrequire github.com/x/y v1.2.3\n"
	before, err := parser.ParseGoModFile("go.mod", content)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	marker := golang.NewGoResolutionResult("example.com/foo", "1.21", "", "go.mod")
	marker.BuildList = []golang.GoModule{
		{ModulePath: "example.com/foo", Version: "", GoVersion: "1.21", Main: true},
		{ModulePath: "github.com/x/y", Version: "v1.2.3", GoVersion: "1.18",
			ModuleHash: "h1:zip=", GoModHash: "h1:mod="},
		{ModulePath: "github.com/x/dep", Version: "v0.4.0", GoVersion: "1.16", GoModHash: "h1:onlymod="},
	}
	marker.Graph = []golang.GoModuleEdge{
		{FromPath: "example.com/foo", FromVersion: "", ToPath: "github.com/x/y", ToVersion: "v1.2.3"},
		{FromPath: "github.com/x/y", FromVersion: "v1.2.3", ToPath: "github.com/x/dep", ToVersion: "v0.4.0", Indirect: true},
	}
	marker.GraphComplete = true
	before.Markers.Entries = append(before.Markers.Entries, marker)

	seed := &golang.GoMod{Ident: before.Ident}
	got := roundTripNode(t, before, seed).(*golang.GoMod)

	var found *golang.GoResolutionResult
	for i := range got.Markers.Entries {
		if r, ok := got.Markers.Entries[i].(golang.GoResolutionResult); ok {
			found = &r
		}
	}
	if found == nil {
		t.Fatalf("GoResolutionResult marker lost in round-trip")
	}
	if !found.GraphComplete {
		t.Errorf("GraphComplete not preserved")
	}
	if len(found.BuildList) != 3 {
		t.Fatalf("BuildList length: want 3, got %d", len(found.BuildList))
	}
	xy := found.BuildList[1]
	if xy.ModulePath != "github.com/x/y" || xy.Version != "v1.2.3" || xy.GoVersion != "1.18" ||
		xy.ModuleHash != "h1:zip=" || xy.GoModHash != "h1:mod=" {
		t.Errorf("BuildList[1] not preserved: %#v", xy)
	}
	if !found.BuildList[0].Main {
		t.Errorf("main module flag not preserved")
	}
	if len(found.Graph) != 2 {
		t.Fatalf("Graph length: want 2, got %d", len(found.Graph))
	}
	e := found.Graph[1]
	if e.FromPath != "github.com/x/y" || e.ToPath != "github.com/x/dep" || e.ToVersion != "v0.4.0" || !e.Indirect {
		t.Errorf("Graph[1] edge not preserved: %#v", e)
	}
}

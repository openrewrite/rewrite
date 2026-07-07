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
	"reflect"
	"testing"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// roundTripMarkers serializes `before` via SendMarkersCodec, then feeds the
// emitted RpcObjectData stream back into a ReceiveQueue and reads it via
// receiveMarkersCodec. Returns whatever the receiver produced.
func roundTripMarkers(t *testing.T, before java.Markers) java.Markers {
	t.Helper()
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, make(map[uintptr]int))
	SendMarkersCodec(before, sendQ)
	sendQ.Flush()

	// Pretend the wire delivers the captured stream as a single batch.
	delivered := false
	recvQ := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return messages
	})
	// receiveMarkersCodec expects the receive queue positioned at the
	// Markers ID slot, matching what SendMarkersCodec emits.
	return receiveMarkersCodec(recvQ, java.Markers{})
}

func TestGoProjectMarkerRoundTrip(t *testing.T) {
	id := uuid.MustParse("11111111-2222-3333-4444-555555555555")
	gp := golang.GoProject{Ident: id, ProjectName: "example/foo"}
	before := java.Markers{ID: uuid.New(), Entries: []java.Marker{gp}}

	after := roundTripMarkers(t, before)
	if len(after.Entries) != 1 {
		t.Fatalf("entries: want 1, got %d", len(after.Entries))
	}
	got, ok := after.Entries[0].(golang.GoProject)
	if !ok {
		t.Fatalf("entry is %T, want golang.GoProject", after.Entries[0])
	}
	if got.Ident != id {
		t.Errorf("Ident: want %s, got %s", id, got.Ident)
	}
	if got.ProjectName != "example/foo" {
		t.Errorf("ProjectName: want %q, got %q", "example/foo", got.ProjectName)
	}
}

func TestGoResolutionResultMarkerRoundTrip(t *testing.T) {
	id := uuid.MustParse("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
	mrr := golang.GoResolutionResult{
		Ident:      id,
		ModulePath: "example.com/foo",
		GoVersion:  "1.22",
		Toolchain:  "go1.22.5",
		Path:       "/tmp/go.mod",
		Requires: []golang.GoRequire{
			{ModulePath: "github.com/google/uuid", Version: "v1.6.0"},
			{ModulePath: "golang.org/x/mod", Version: "v0.35.0", Indirect: true},
		},
		Replaces: []golang.GoReplace{
			{OldPath: "github.com/x/y", NewPath: "../local/y"},
			{OldPath: "github.com/a/b", OldVersion: "v1.0.0", NewPath: "github.com/forked/b", NewVersion: "v1.0.1"},
		},
		Excludes: []golang.GoExclude{
			{ModulePath: "github.com/bad", Version: "v0.0.1"},
		},
		Retracts: []golang.GoRetract{
			{VersionRange: "v0.0.5", Rationale: "deleted main.go"},
			{VersionRange: "[v1.0.0, v1.0.5]"},
		},
		ResolvedDependencies: []golang.GoResolvedDependency{
			{ModulePath: "github.com/google/uuid", Version: "v1.6.0", ModuleHash: "h1:abc=", GoModHash: "h1:def="},
		},
	}
	before := java.Markers{ID: uuid.New(), Entries: []java.Marker{mrr}}

	after := roundTripMarkers(t, before)
	if len(after.Entries) != 1 {
		t.Fatalf("entries: want 1, got %d", len(after.Entries))
	}
	got, ok := after.Entries[0].(golang.GoResolutionResult)
	if !ok {
		t.Fatalf("entry is %T, want golang.GoResolutionResult", after.Entries[0])
	}
	if !reflect.DeepEqual(mrr, got) {
		t.Errorf("round-trip mismatch\nbefore: %+v\nafter:  %+v", mrr, got)
	}
}

func TestGoResolutionResultEmptyListsRoundTrip(t *testing.T) {
	// Mirrors the recent rewrite-core fix where empty descriptor collections
	// must serialize as empty arrays, not be omitted. After round-trip an
	// initially-empty list should still be present and empty.
	id := uuid.MustParse("99999999-0000-0000-0000-000000000000")
	mrr := golang.GoResolutionResult{
		Ident:      id,
		ModulePath: "example.com/empty",
		Path:       "go.mod",
		Requires:   []golang.GoRequire{},
		Replaces:   []golang.GoReplace{},
		Excludes:   []golang.GoExclude{},
		Retracts:   []golang.GoRetract{},
	}
	before := java.Markers{ID: uuid.New(), Entries: []java.Marker{mrr}}

	after := roundTripMarkers(t, before)
	got := after.Entries[0].(golang.GoResolutionResult)
	if got.ModulePath != "example.com/empty" {
		t.Errorf("ModulePath: want %q, got %q", "example.com/empty", got.ModulePath)
	}
}

func TestRecipesThatMadeChangesMarkerRoundTripSharesRecipePayloads(t *testing.T) {
	recipe := &opaqueRpcPayload{
		JavaType: "org.openrewrite.rpc.RpcRecipe",
		Value: map[string]any{
			"descriptor": map[string]any{
				"name":        "example.Recipe",
				"displayName": "Example recipe",
			},
		},
	}
	firstID := uuid.MustParse("aaaaaaaa-1111-2222-3333-444444444444")
	secondID := uuid.MustParse("bbbbbbbb-1111-2222-3333-444444444444")
	first := java.RecipesThatMadeChanges{
		Ident:   firstID,
		Recipes: [][]any{{recipe, recipe}},
	}
	second := java.RecipesThatMadeChanges{
		Ident:   secondID,
		Recipes: [][]any{{recipe}},
	}
	before := java.Markers{ID: uuid.New(), Entries: []java.Marker{first, second}}

	after := roundTripMarkers(t, before)
	if len(after.Entries) != 2 {
		t.Fatalf("entries: want 2, got %d", len(after.Entries))
	}

	gotFirst, ok := after.Entries[0].(java.RecipesThatMadeChanges)
	if !ok {
		t.Fatalf("first entry is %T, want java.RecipesThatMadeChanges", after.Entries[0])
	}
	gotSecond, ok := after.Entries[1].(java.RecipesThatMadeChanges)
	if !ok {
		t.Fatalf("second entry is %T, want java.RecipesThatMadeChanges", after.Entries[1])
	}
	if gotFirst.Ident != firstID {
		t.Errorf("first Ident: want %s, got %s", firstID, gotFirst.Ident)
	}
	if gotSecond.Ident != secondID {
		t.Errorf("second Ident: want %s, got %s", secondID, gotSecond.Ident)
	}
	if len(gotFirst.Recipes) != 1 || len(gotFirst.Recipes[0]) != 2 || len(gotSecond.Recipes) != 1 || len(gotSecond.Recipes[0]) != 1 {
		t.Fatalf("unexpected recipe stack shape: first=%#v second=%#v", gotFirst.Recipes, gotSecond.Recipes)
	}
	if gotFirst.Recipes[0][0] != gotFirst.Recipes[0][1] {
		t.Fatalf("first marker did not preserve shared recipe payload identity")
	}
	if gotFirst.Recipes[0][0] != gotSecond.Recipes[0][0] {
		t.Fatalf("recipe payload identity was not shared across markers")
	}
}

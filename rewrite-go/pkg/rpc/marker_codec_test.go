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

func roundTripMarkersInterned(t *testing.T, before java.Markers, pool *GoResolutionInternPool) java.Markers {
	t.Helper()
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, make(map[uintptr]int))
	SendMarkersCodec(before, sendQ)
	sendQ.Flush()

	delivered := false
	recvQ := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return messages
	})
	recvQ.SetGoResolutionIntern(pool)
	return receiveMarkersCodec(recvQ, java.Markers{})
}

func sampleResolutionResult(id uuid.UUID, modulePath string) golang.GoResolutionResult {
	return golang.GoResolutionResult{
		Ident:      id,
		ModulePath: modulePath,
		GoVersion:  "1.22",
		Path:       "/tmp/go.mod",
		Requires: []golang.GoRequire{
			{ModulePath: "github.com/google/uuid", Version: "v1.6.0"},
		},
		ResolvedDependencies: []golang.GoResolvedDependency{
			{ModulePath: "github.com/google/uuid", Version: "v1.6.0", ModuleHash: "h1:abc="},
		},
		PackageModules: []golang.GoPackageModule{
			{ImportPath: "github.com/google/uuid", ModulePath: "github.com/google/uuid", Version: "v1.6.0"},
		},
	}
}

func TestGoResolutionResultInterningSharesBackingSlices(t *testing.T) {
	// given
	id := uuid.MustParse("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
	mrr := sampleResolutionResult(id, "example.com/foo")
	pool := NewGoResolutionInternPool()

	// when
	after1 := roundTripMarkersInterned(t, java.Markers{ID: uuid.New(), Entries: []java.Marker{mrr}}, pool)
	after2 := roundTripMarkersInterned(t, java.Markers{ID: uuid.New(), Entries: []java.Marker{mrr}}, pool)
	got1 := after1.Entries[0].(golang.GoResolutionResult)
	got2 := after2.Entries[0].(golang.GoResolutionResult)

	// then
	if !reflect.DeepEqual(mrr, got1) {
		t.Fatalf("round-trip mismatch\nbefore: %+v\nafter:  %+v", mrr, got1)
	}
	if &got1.ResolvedDependencies[0] != &got2.ResolvedDependencies[0] {
		t.Errorf("ResolvedDependencies not interned: distinct backing arrays")
	}
	if &got1.Requires[0] != &got2.Requires[0] {
		t.Errorf("Requires not interned: distinct backing arrays")
	}
	if &got1.PackageModules[0] != &got2.PackageModules[0] {
		t.Errorf("PackageModules not interned: distinct backing arrays")
	}
}

func TestGoResolutionResultInterningKeepsDistinctModulesSeparate(t *testing.T) {
	// given
	pool := NewGoResolutionInternPool()
	a := sampleResolutionResult(uuid.MustParse("11111111-1111-1111-1111-111111111111"), "example.com/a")
	b := sampleResolutionResult(uuid.MustParse("22222222-2222-2222-2222-222222222222"), "example.com/b")

	// when
	gotA := roundTripMarkersInterned(t, java.Markers{ID: uuid.New(), Entries: []java.Marker{a}}, pool).Entries[0].(golang.GoResolutionResult)
	gotB := roundTripMarkersInterned(t, java.Markers{ID: uuid.New(), Entries: []java.Marker{b}}, pool).Entries[0].(golang.GoResolutionResult)

	// then
	if gotA.ModulePath != "example.com/a" || gotB.ModulePath != "example.com/b" {
		t.Errorf("distinct modules collapsed: %q / %q", gotA.ModulePath, gotB.ModulePath)
	}
	if &gotA.ResolvedDependencies[0] == &gotB.ResolvedDependencies[0] {
		t.Errorf("distinct modules unexpectedly share a backing array")
	}
}

func TestGoResolutionResultInterningPassesThroughNilIdent(t *testing.T) {
	// given
	pool := NewGoResolutionInternPool()
	mrr := sampleResolutionResult(uuid.Nil, "example.com/foo")

	// when
	got1 := roundTripMarkersInterned(t, java.Markers{ID: uuid.New(), Entries: []java.Marker{mrr}}, pool).Entries[0].(golang.GoResolutionResult)
	got2 := roundTripMarkersInterned(t, java.Markers{ID: uuid.New(), Entries: []java.Marker{mrr}}, pool).Entries[0].(golang.GoResolutionResult)

	// then
	if &got1.ResolvedDependencies[0] == &got2.ResolvedDependencies[0] {
		t.Errorf("nil-Ident results must not be interned")
	}
}

func TestGoResolutionResultInterningDisabledByDefault(t *testing.T) {
	// given
	id := uuid.MustParse("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
	mrr := sampleResolutionResult(id, "example.com/foo")

	// when
	got1 := roundTripMarkers(t, java.Markers{ID: uuid.New(), Entries: []java.Marker{mrr}}).Entries[0].(golang.GoResolutionResult)
	got2 := roundTripMarkers(t, java.Markers{ID: uuid.New(), Entries: []java.Marker{mrr}}).Entries[0].(golang.GoResolutionResult)

	// then
	if &got1.ResolvedDependencies[0] == &got2.ResolvedDependencies[0] {
		t.Errorf("expected independent backing arrays without an intern pool")
	}
}

func TestGoProjectMarkerRoundTrip(t *testing.T) {
	id := uuid.MustParse("11111111-2222-3333-4444-555555555555")
	gp := golang.GoProject{Ident: id, ProjectName: "example/foo", ModulePath: "example.com/foo"}
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
	if got.ModulePath != "example.com/foo" {
		t.Errorf("ModulePath: want %q, got %q", "example.com/foo", got.ModulePath)
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
			{
				ModulePath: "github.com/google/uuid", Version: "v1.6.0",
				ModuleHash: "h1:abc=", GoModHash: "h1:def=",
				Main: true, ModuleGoVersion: "1.22",
				Deps: []golang.GoModuleRef{
					{ModulePath: "golang.org/x/mod", Version: "v0.35.0"},
				},
			},
			{
				ModulePath: "golang.org/x/mod", Version: "v0.35.0",
				ModuleHash: "h1:ghi=", GoModHash: "h1:jkl=",
				Indirect: true, ReplacePath: "github.com/forked/mod", ReplaceVersion: "v0.35.1",
			},
		},
		PackageModules: []golang.GoPackageModule{
			{ImportPath: "fmt", Standard: true},
			{ImportPath: "github.com/google/uuid", ModulePath: "github.com/google/uuid", Version: "v1.6.0"},
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

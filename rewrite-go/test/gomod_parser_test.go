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

package test

import (
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

func TestParseGoModBasicFields(t *testing.T) {
	mrr, err := parser.ParseGoMod("go.mod", `module example.com/foo

go 1.22

toolchain go1.22.3

require (
	github.com/x/y v1.2.3
	github.com/z/w v0.5.0 // indirect
)

replace github.com/x/y => github.com/x/y v1.2.4

exclude github.com/bad v0.0.1

retract v0.0.5 // accidentally deleted main.go
retract [v1.0.0, v1.0.5]
`)
	require.NoError(t, err, "parse failed")
	assert.Equalf(t, "example.com/foo", mrr.ModulePath, "ModulePath: want %q", "example.com/foo")
	assert.Equalf(t, "1.22", mrr.GoVersion, "GoVersion: want %q", "1.22")
	assert.Equalf(t, "go1.22.3", mrr.Toolchain, "Toolchain: want %q", "go1.22.3")
	require.Len(t, mrr.Requires, 2, "Requires len")
	assert.Falsef(t, mrr.Requires[0].ModulePath != "github.com/x/y" || mrr.Requires[0].Version != "v1.2.3" || mrr.Requires[0].Indirect, "Requires[0]: %+v", mrr.Requires[0])
	assert.Falsef(t, mrr.Requires[1].ModulePath != "github.com/z/w" || !mrr.Requires[1].Indirect, "Requires[1]: %+v", mrr.Requires[1])
	assert.False(t, len(mrr.Replaces) != 1 || mrr.Replaces[0].OldPath != "github.com/x/y" || mrr.Replaces[0].NewVersion != "v1.2.4", "Replaces")
	assert.False(t, len(mrr.Excludes) != 1 || mrr.Excludes[0].ModulePath != "github.com/bad", "Excludes")
	require.Len(t, mrr.Retracts, 2, "Retracts len")
	assert.Falsef(t, mrr.Retracts[0].VersionRange != "v0.0.5" || mrr.Retracts[0].Rationale == "", "Retracts[0]: %+v", mrr.Retracts[0])
	assert.Equalf(t, "[v1.0.0, v1.0.5]", mrr.Retracts[1].VersionRange, "Retracts[1] range: %+v", mrr.Retracts[1])
}

func TestGoModSourceSpecCarriesParsedMarker(t *testing.T) {
	spec := test.GoMod(`
		module example.com/foo

		go 1.22

		require github.com/x/y v1.2.3
	`)
	mrr := test.FindGoResolutionResult(spec)
	require.NotNil(t, mrr, "expected GoResolutionResult marker on the GoMod SourceSpec")
	assert.Equalf(t, "example.com/foo", mrr.ModulePath, "ModulePath: want %q", "example.com/foo")
	if r := mrr.FindRequire("github.com/x/y"); r == nil || r.Version != "v1.2.3" {
		t.Errorf("FindRequire: %+v", r)
	}
}

func TestGoModBadInputDoesNotAttachMarker(t *testing.T) {
	spec := test.GoMod(`this is not a valid go.mod`)
	if mrr := test.FindGoResolutionResult(spec); mrr != nil {
		t.Errorf("expected no GoResolutionResult marker on malformed input, got %+v", mrr)
	}
}

func TestParseGoSumBasic(t *testing.T) {
	resolved := parser.ParseGoSum(`github.com/google/uuid v1.6.0 h1:NIvaJDMOsjHA8n1jAhLSgzrAzy1Hgr+hNrb57e+94F0=
github.com/google/uuid v1.6.0/go.mod h1:TIyPZe4MgqvfeYDBFedMoGGpEw/LqOeaOT+nhxU+yHo=
golang.org/x/mod v0.35.0 h1:Ww1D637e6Pg+Zb2KrWfHQUnH2dQRLBQyAtpr/haaJeM=
golang.org/x/mod v0.35.0/go.mod h1:+GwiRhIInF8wPm+4AoT6L0FA1QWAad3OMdTRx4tFYlU=
`)
	require.Len(t, resolved, 2, "ParseGoSum: want 2 entries")
	uuid := resolved[0]
	assert.False(t, uuid.ModulePath != "github.com/google/uuid" || uuid.Version != "v1.6.0", "entry[0]: want github.com/google/uuid@v")
	assert.Equal(t, "h1:NIvaJDMOsjHA8n1jAhLSgzrAzy1Hgr+hNrb57e+94F0=", uuid.ModuleHash, "entry[0].ModuleHash")
	assert.Equal(t, "h1:TIyPZe4MgqvfeYDBFedMoGGpEw/LqOeaOT+nhxU+yHo=", uuid.GoModHash, "entry[0].GoModHash")
	if mod := resolved[1]; mod.ModulePath != "golang.org/x/mod" || mod.Version != "v0.35.0" {
		t.Errorf("entry[1]: want golang.org/x/mod@v0.35.0, got %+v", mod)
	}
}

func TestParseGoSumOnlyGoModHashRecorded(t *testing.T) {
	// When go.sum has only the /go.mod line for a dependency (i.e. the
	// module zip wasn't downloaded), ModuleHash is empty but GoModHash is
	// set. This is what go.sum looks like for indirect deps that only the
	// build graph knows about.
	resolved := parser.ParseGoSum(`example.com/indirect v1.0.0/go.mod h1:abc123=
`)
	require.Len(t, resolved, 1, "want 1 entry")
	assert.Equalf(t, "", resolved[0].ModuleHash, "ModuleHash: want empty, got %q", resolved[0].ModuleHash)
	assert.Equalf(t, "h1:abc123=", resolved[0].GoModHash, "GoModHash: %q", resolved[0].GoModHash)
}

func TestParseGoSumMalformedLineSkipped(t *testing.T) {
	// A malformed line in the middle of a valid go.sum should be skipped
	// (logged, not fatal) and adjacent entries should still parse.
	resolved := parser.ParseGoSum(`github.com/a/b v1.0.0 h1:hashA=
this is not a valid go.sum line
github.com/c/d v2.0.0 h1:hashC=
`)
	require.Len(t, resolved, 2, "want 2 entries (malformed skipped")
	assert.Falsef(t, resolved[0].ModulePath != "github.com/a/b" || resolved[1].ModulePath != "github.com/c/d", "unexpected modules: %+v", resolved)
}

func TestParseGoSumEmptyInput(t *testing.T) {
	// Empty input must yield a non-nil empty slice: callers assign the result
	// directly to GoResolutionResult.ResolvedDependencies, and a nil slice
	// would be serialized as a null list and break the LST write.
	got := parser.ParseGoSum("")
	assert.NotNil(t, got, "want non-nil empty slice for empty input, got nil")
	assert.Len(t, got, 0, "want empty slice for empty input")
}

func TestGoProjectMergesGoSumIntoGoModMarker(t *testing.T) {
	// Sibling go.mod + go.sum inside a GoProject: harness should merge
	// the parsed ResolvedDependencies into the GoResolutionResult marker
	// at expansion time.
	expanded := test.GoProject("foo",
		test.GoMod(`
			module example.com/foo

			go 1.22

			require github.com/google/uuid v1.6.0
		`),
		test.GoSum(`
			github.com/google/uuid v1.6.0 h1:NIvaJDMOsjHA8n1jAhLSgzrAzy1Hgr+hNrb57e+94F0=
			github.com/google/uuid v1.6.0/go.mod h1:TIyPZe4MgqvfeYDBFedMoGGpEw/LqOeaOT+nhxU+yHo=
		`),
	).Expand()

	var modSpec *test.SourceSpec
	for i, s := range expanded {
		if s.Path == "go.mod" {
			modSpec = &expanded[i]
		}
	}
	require.NotNil(t, modSpec, "no go.mod spec in expanded project")
	mrr := test.FindGoResolutionResult(*modSpec)
	require.NotNil(t, mrr, "no GoResolutionResult marker on go.mod")
	require.Len(t, mrr.ResolvedDependencies, 1, "want 1 resolved dep")
	rd := mrr.ResolvedDependencies[0]
	assert.False(t, rd.ModulePath != "github.com/google/uuid" || rd.Version != "v1.6.0", "unexpected resolved dep")
	assert.False(t, rd.ModuleHash == "" || rd.GoModHash == "", "expected both ModuleHash and GoModHash populated")
}

// TestParseGoModDirectiveListsNeverNil guards the root cause of the Moderne CLI
// Go build LST serialization failure: a go.mod that omits a directive must still
// yield non-nil (empty) slices. The RPC send codec serializes a nil slice as a
// null list, which the Java receive side stores as a null field, and the Moderne
// reflective binary LST serializer then NPEs calling items.size() on it.
func TestParseGoModDirectiveListsNeverNil(t *testing.T) {
	mrr, err := parser.ParseGoMod("go.mod", "module example.com/foo\n\ngo 1.22\n")
	require.NoError(t, err, "parse failed")
	assert.NotNil(t, mrr.Requires, "Requires is nil; want non-nil empty slice")
	assert.NotNil(t, mrr.Replaces, "Replaces is nil; want non-nil empty slice")
	assert.NotNil(t, mrr.Excludes, "Excludes is nil; want non-nil empty slice")
	assert.NotNil(t, mrr.Retracts, "Retracts is nil; want non-nil empty slice")
	assert.NotNil(t, mrr.ResolvedDependencies, "ResolvedDependencies is nil; want non-nil empty slice")
}

// TestParseGoModRequireButNoReplace covers the gin/cobra/zap failure shape: a
// require block is present but replace/exclude/retract/resolved are absent and
// must still be non-nil empty slices.
func TestParseGoModRequireButNoReplace(t *testing.T) {
	mrr, err := parser.ParseGoMod("go.mod", "module example.com/foo\n\ngo 1.22\n\nrequire github.com/x/y v1.2.3\n")
	require.NoError(t, err, "parse failed")
	require.Len(t, mrr.Requires, 1, "Requires len")
	assert.NotNil(t, mrr.Replaces, "Replaces is nil; want non-nil empty slice")
	assert.NotNil(t, mrr.Excludes, "Excludes is nil; want non-nil empty slice")
	assert.NotNil(t, mrr.Retracts, "Retracts is nil; want non-nil empty slice")
	assert.NotNil(t, mrr.ResolvedDependencies, "ResolvedDependencies is nil; want non-nil empty slice")
}

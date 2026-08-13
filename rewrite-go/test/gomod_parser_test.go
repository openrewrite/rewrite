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
	require.NoError(t, err)
	assert.Equal(t, "example.com/foo", mrr.ModulePath)
	assert.Equal(t, "1.22", mrr.GoVersion)
	assert.Equal(t, "go1.22.3", mrr.Toolchain)
	require.Len(t, mrr.Requires, 2)
	if mrr.Requires[0].ModulePath != "github.com/x/y" || mrr.Requires[0].Version != "v1.2.3" || mrr.Requires[0].Indirect {
		t.Errorf("Requires[0]: %+v", mrr.Requires[0])
	}
	if mrr.Requires[1].ModulePath != "github.com/z/w" || !mrr.Requires[1].Indirect {
		t.Errorf("Requires[1]: %+v", mrr.Requires[1])
	}
	assert.False(t, len(mrr.Replaces) != 1 || mrr.Replaces[0].OldPath != "github.com/x/y" || mrr.Replaces[0].NewVersion != "v1.2.4")
	assert.False(t, len(mrr.Excludes) != 1 || mrr.Excludes[0].ModulePath != "github.com/bad")
	require.Len(t, mrr.Retracts, 2)
	if mrr.Retracts[0].VersionRange != "v0.0.5" || mrr.Retracts[0].Rationale == "" {
		t.Errorf("Retracts[0]: %+v", mrr.Retracts[0])
	}
	if mrr.Retracts[1].VersionRange != "[v1.0.0, v1.0.5]" {
		t.Errorf("Retracts[1] range: %+v", mrr.Retracts[1])
	}
}

func TestGoModSourceSpecCarriesParsedMarker(t *testing.T) {
	spec := test.GoMod(`
		module example.com/foo

		go 1.22

		require github.com/x/y v1.2.3
	`)
	mrr := test.FindGoResolutionResult(spec)
	require.NotNil(t, mrr)
	assert.Equal(t, "example.com/foo", mrr.ModulePath)
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
	require.Len(t, resolved, 2)
	uuid := resolved[0]
	assert.False(t, uuid.ModulePath != "github.com/google/uuid" || uuid.Version != "v1.6.0")
	assert.Equal(t, "h1:NIvaJDMOsjHA8n1jAhLSgzrAzy1Hgr+hNrb57e+94F0=", uuid.ModuleHash)
	assert.Equal(t, "h1:TIyPZe4MgqvfeYDBFedMoGGpEw/LqOeaOT+nhxU+yHo=", uuid.GoModHash)
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
	require.Len(t, resolved, 1)
	if resolved[0].ModuleHash != "" {
		t.Errorf("ModuleHash: want empty, got %q", resolved[0].ModuleHash)
	}
	if resolved[0].GoModHash != "h1:abc123=" {
		t.Errorf("GoModHash: %q", resolved[0].GoModHash)
	}
}

func TestParseGoSumMalformedLineSkipped(t *testing.T) {
	// A malformed line in the middle of a valid go.sum should be skipped
	// (logged, not fatal) and adjacent entries should still parse.
	resolved := parser.ParseGoSum(`github.com/a/b v1.0.0 h1:hashA=
this is not a valid go.sum line
github.com/c/d v2.0.0 h1:hashC=
`)
	require.Len(t, resolved, 2)
	if resolved[0].ModulePath != "github.com/a/b" || resolved[1].ModulePath != "github.com/c/d" {
		t.Errorf("unexpected modules: %+v", resolved)
	}
}

func TestParseGoSumEmptyInput(t *testing.T) {
	// Empty input must yield a non-nil empty slice: callers assign the result
	// directly to GoResolutionResult.ResolvedDependencies, and a nil slice
	// would be serialized as a null list and break the LST write.
	got := parser.ParseGoSum("")
	assert.NotNil(t, got)
	assert.Len(t, got, 0)
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
	require.NotNil(t, modSpec)
	mrr := test.FindGoResolutionResult(*modSpec)
	require.NotNil(t, mrr)
	require.Len(t, mrr.ResolvedDependencies, 1)
	rd := mrr.ResolvedDependencies[0]
	assert.False(t, rd.ModulePath != "github.com/google/uuid" || rd.Version != "v1.6.0")
	assert.False(t, rd.ModuleHash == "" || rd.GoModHash == "")
}

// TestParseGoModDirectiveListsNeverNil guards the root cause of the Moderne CLI
// Go build LST serialization failure: a go.mod that omits a directive must still
// yield non-nil (empty) slices. The RPC send codec serializes a nil slice as a
// null list, which the Java receive side stores as a null field, and the Moderne
// reflective binary LST serializer then NPEs calling items.size() on it.
func TestParseGoModDirectiveListsNeverNil(t *testing.T) {
	mrr, err := parser.ParseGoMod("go.mod", "module example.com/foo\n\ngo 1.22\n")
	require.NoError(t, err)
	assert.NotNil(t, mrr.Requires)
	assert.NotNil(t, mrr.Replaces)
	assert.NotNil(t, mrr.Excludes)
	assert.NotNil(t, mrr.Retracts)
	assert.NotNil(t, mrr.ResolvedDependencies)
}

// TestParseGoModRequireButNoReplace covers the gin/cobra/zap failure shape: a
// require block is present but replace/exclude/retract/resolved are absent and
// must still be non-nil empty slices.
func TestParseGoModRequireButNoReplace(t *testing.T) {
	mrr, err := parser.ParseGoMod("go.mod", "module example.com/foo\n\ngo 1.22\n\nrequire github.com/x/y v1.2.3\n")
	require.NoError(t, err)
	require.Len(t, mrr.Requires, 1)
	assert.NotNil(t, mrr.Replaces)
	assert.NotNil(t, mrr.Excludes)
	assert.NotNil(t, mrr.Retracts)
	assert.NotNil(t, mrr.ResolvedDependencies)
}

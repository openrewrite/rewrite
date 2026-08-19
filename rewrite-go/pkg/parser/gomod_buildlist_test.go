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

package parser

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

func sumRow(path, version, moduleHash string) golang.GoResolvedDependency {
	return golang.GoResolvedDependency{ModulePath: path, Version: version, ModuleHash: moduleHash}
}

func TestDeriveBuildListFromPrunedRequires(t *testing.T) {
	requires := []golang.GoRequire{
		{ModulePath: "github.com/google/uuid", Version: "v1.6.0"},
		{ModulePath: "golang.org/x/mod", Version: "v0.35.0", Indirect: true},
	}

	list, source := DeriveBuildList("1.25.0", requires, nil)

	assert.Equal(t, golang.ResolutionGoMod, source)
	require.Len(t, list, 2)
	assert.Equal(t, "github.com/google/uuid", list[0].ModulePath)
	assert.Equal(t, "v1.6.0", list[0].Version)
	assert.True(t, list[0].Selected)
	assert.False(t, list[0].Indirect)
	assert.True(t, list[1].Selected)
	assert.True(t, list[1].Indirect, "// indirect requires stay flagged indirect in the build list")
}

func TestDeriveBuildListPrePruningFallsBackToGoSumOnly(t *testing.T) {
	requires := []golang.GoRequire{{ModulePath: "github.com/google/uuid", Version: "v1.6.0"}}
	sum := []golang.GoResolvedDependency{sumRow("github.com/google/uuid", "v1.6.0", "h1:aaa")}

	list, source := DeriveBuildList("1.16", requires, sum)

	assert.Equal(t, golang.ResolutionGoSumOnly, source)
	require.Len(t, list, 1)
	assert.False(t, list[0].Selected, "nothing is selected when there is no build list")
}

func TestDeriveBuildListMissingGoDirectiveFallsBackToGoSumOnly(t *testing.T) {
	requires := []golang.GoRequire{{ModulePath: "github.com/google/uuid", Version: "v1.6.0"}}

	_, source := DeriveBuildList("", requires, nil)

	assert.Equal(t, golang.ResolutionGoSumOnly, source)
}

func TestDeriveBuildListJoinsGoSumHashes(t *testing.T) {
	requires := []golang.GoRequire{{ModulePath: "github.com/google/uuid", Version: "v1.6.0"}}
	sum := []golang.GoResolvedDependency{
		{ModulePath: "github.com/google/uuid", Version: "v1.6.0", ModuleHash: "h1:zip", GoModHash: "h1:mod"},
	}

	list, _ := DeriveBuildList("1.21", requires, sum)

	require.Len(t, list, 1)
	assert.Equal(t, "h1:zip", list[0].ModuleHash)
	assert.Equal(t, "h1:mod", list[0].GoModHash)
	assert.True(t, list[0].Selected)
}

func TestDeriveBuildListMarksRejectedGoSumVersionsUnselected(t *testing.T) {
	requires := []golang.GoRequire{{ModulePath: "golang.org/x/mod", Version: "v0.35.0"}}
	sum := []golang.GoResolvedDependency{
		sumRow("golang.org/x/mod", "v0.35.0", "h1:new"),
		sumRow("golang.org/x/mod", "v0.27.0", "h1:old"),
	}

	list, source := DeriveBuildList("1.21", requires, sum)

	assert.Equal(t, golang.ResolutionGoMod, source)
	require.Len(t, list, 2)

	selected := map[string]bool{}
	for _, d := range list {
		selected[d.Version] = d.Selected
	}
	assert.True(t, selected["v0.35.0"])
	assert.False(t, selected["v0.27.0"], "a version MVS rejected is not in the build")
}

func TestDeriveBuildListEmptyRequires(t *testing.T) {
	list, source := DeriveBuildList("1.21", nil, nil)

	assert.Equal(t, golang.ResolutionGoMod, source)
	assert.NotNil(t, list, "callers assign this straight onto the marker; nil serializes as a null list")
	assert.Empty(t, list)
}

func TestSupportsPruning(t *testing.T) {
	for _, tc := range []struct {
		goVersion string
		want      bool
	}{
		{"1.17", true},
		{"1.16", false},
		{"1.9", false},
		{"1.21.5", true},
		{"1.25.0", true},
		{"", false},
		{"garbage", false},
	} {
		assert.Equalf(t, tc.want, supportsPruning(tc.goVersion), "go %q", tc.goVersion)
	}
}

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
	"golang.org/x/mod/semver"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// pruningMinGoVersion is the `go` directive at which module graph pruning was
// introduced: from here on `go mod tidy` records an indirect require for every
// module providing a transitively imported package, which is what makes the
// expanded require block a build list.
const pruningMinGoVersion = "v1.17"

// DeriveBuildList produces the resolved build list for a main module from its
// require block plus go.sum's hash rows, without the Go toolchain. Pruning is
// what makes it sound (see pruningMinGoVersion); before that the require block
// holds only the direct roots and the go.sum rows are returned as-is.
func DeriveBuildList(goVersion string, requires []golang.GoRequire, replaces []golang.GoReplace, fromSum []golang.GoResolvedDependency) ([]golang.GoResolvedDependency, golang.GoResolutionSource) {
	if !supportsPruning(goVersion) {
		out := make([]golang.GoResolvedDependency, 0, len(fromSum))
		for _, d := range fromSum {
			d.Selected = false
			out = append(out, d)
		}
		return out, golang.ResolutionGoSumOnly
	}

	buildList := make([]golang.GoResolvedDependency, 0, len(requires))
	for _, r := range requires {
		mod := golang.GoResolvedDependency{
			ModulePath: r.ModulePath,
			Version:    r.Version,
			Indirect:   r.Indirect,
		}
		applyReplace(&mod, replaces)
		buildList = append(buildList, mod)
	}
	return MergeResolvedDependencies(fromSum, buildList), golang.ResolutionGoMod
}

// applyReplace records the `replace` target for mod, matching how `go list -m`
// reports one. A replace with no old version binds every version of the path;
// with one it binds only that version. go.sum records the replacement's hashes
// under the replacement's own coordinate, so a replaced module has none here.
func applyReplace(mod *golang.GoResolvedDependency, replaces []golang.GoReplace) {
	for _, r := range replaces {
		if r.OldPath != mod.ModulePath {
			continue
		}
		if r.OldVersion != "" && r.OldVersion != mod.Version {
			continue
		}
		mod.ReplacePath = r.NewPath
		mod.ReplaceVersion = r.NewVersion
		return
	}
}

// supportsPruning reports whether a `go` directive is at or past
// pruningMinGoVersion. An unparseable or absent directive predates it.
func supportsPruning(goVersion string) bool {
	if goVersion == "" {
		return false
	}
	v := "v" + goVersion
	if !semver.IsValid(v) {
		return false
	}
	return semver.Compare(semver.MajorMinor(v), pruningMinGoVersion) >= 0
}

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

package modgraph

import (
	"strings"

	"golang.org/x/mod/modfile"
	"golang.org/x/mod/semver"
)

// TidyRequireSet computes the exact go.mod require set `go mod tidy` would write,
// including the go 1.17+ pruning-completeness roots that NeededModules alone does
// not capture.
//
// NeededModules gives the modules that PROVIDE a package in `all` (direct +
// import-reachable indirect). For a go>=1.17 main module, `go mod tidy` also adds
// an indirect root for every module that is reachable from `all` through imports
// OR TESTS but whose version the pruned module graph would UNDER-SELECT — i.e. a
// test-transitive dependency whose required version is higher than any root's
// pruned go.mod implies (e.g. kr/text via gopkg.in/check.v1, go.uber.org/mock via
// a dependency's test). This mirrors cmd/go/internal/modload.tidyPrunedRoots:
// walk imports+tests outward from `all`, and promote a module to an explicit root
// whenever Selected(path) < loadedVersion(path).
//
// The version gate is essential: it adds mock/kr/text (genuinely under-selected)
// while leaving testify-style clusters out when the pruned graph already selects
// them correctly. For go<1.17 (no pruning) or an incomplete base resolution it
// returns NeededModules unchanged.
func TidyRequireSet(mainImports []string, mainModulePath, mainGoMod string, res Result, src ModSource, separateIndirect bool) RequireSet {
	base := NeededModules(mainImports, mainModulePath, res, src, separateIndirect)
	if !separateIndirect || !base.Complete {
		return base
	}

	// loaded[path] = the version each module is selected at in the full build
	// list (go's `m.Version` — the version a package's module is loaded at).
	loaded := map[string]string{}
	for _, m := range res.BuildList {
		if !m.Main {
			loaded[m.Path] = m.Version
		}
	}

	// Reachable modules via imports AND tests, starting from `all`. Their
	// providing modules are the candidates that may need explicit roots.
	reachable := testReachableModules(mainImports, mainModulePath, res, src)

	// Current root set: everything NeededModules already requires.
	roots := map[string]string{}
	for p, v := range base.Direct {
		roots[p] = v
	}
	for p, v := range base.Indirect {
		roots[p] = v
	}

	// Iterate: under the current roots' PRUNED graph, promote any reachable
	// module that is under-selected to a root. Adding roots raises selections,
	// so repeat to a fixpoint (bounded by the number of candidates).
	for {
		sel, ok := prunedSelection(mainGoMod, roots, loaded, src)
		if !ok {
			return base // re-resolution failed; fall back rather than guess
		}
		added := false
		for path := range reachable {
			if _, isRoot := roots[path]; isRoot {
				continue
			}
			want, have := loaded[path], sel[path]
			if want == "" {
				continue
			}
			if have == "" || semver.Compare(have, want) < 0 {
				roots[path] = want
				added = true
			}
		}
		if !added {
			break
		}
	}

	// Reclassify: direct stays direct; every other root is indirect.
	out := RequireSet{
		Direct:      map[string]string{},
		Indirect:    map[string]string{},
		Complete:    true,
		Unresolved:  base.Unresolved,
		MissingDirs: base.MissingDirs,
	}
	for p, v := range roots {
		if _, isDirect := base.Direct[p]; isDirect {
			out.Direct[p] = v
		} else {
			out.Indirect[p] = v
		}
	}
	return out
}

// prunedSelection returns the MVS-selected version of every module under the
// go>=1.17 PRUNED module graph rooted at the given root set. It builds a
// synthetic go.mod (preserving the main module's go directive, replaces, and
// excludes) requiring each root at its loaded version, then re-resolves.
func prunedSelection(mainGoMod string, roots, loaded map[string]string, src ModSource) (map[string]string, bool) {
	mf, err := modfile.Parse("go.mod", []byte(mainGoMod), nil)
	if err != nil {
		return nil, false
	}
	// Replace the require block with exactly our root set.
	for _, r := range mf.Require {
		_ = mf.DropRequire(r.Mod.Path)
	}
	for path := range roots {
		v := loaded[path]
		if v == "" {
			v = roots[path]
		}
		if v == "" {
			continue
		}
		_ = mf.AddRequire(path, v)
	}
	mf.Cleanup()
	synthetic := modfile.Format(mf.Syntax)

	res, err := Resolve(synthetic, src)
	if err != nil || !res.Complete {
		return nil, false
	}
	sel := map[string]string{}
	for _, m := range res.BuildList {
		if !m.Main {
			sel[m.Path] = m.Version
		}
	}
	return sel, true
}

// testReachableModules returns the set of module paths reachable from the main
// module's imports by following BOTH ordinary and test imports, recursively. It
// maps each reached package to its providing build-list module. This is the set
// of modules `go mod tidy` considers when deciding which need explicit roots for
// the pruned graph to be reproducible.
func testReachableModules(mainImports []string, mainModulePath string, res Result, src ModSource) map[string]bool {
	type modver struct{ path, version string }
	var mods []modver
	for _, m := range res.BuildList {
		if !m.Main {
			mods = append(mods, modver{m.Path, m.Version})
		}
	}
	moduleOf := func(importPath string) (string, string) {
		best, bestVer := "", ""
		for _, m := range mods {
			if importPath == m.path || strings.HasPrefix(importPath, m.path+"/") {
				if len(m.path) > len(best) {
					best, bestVer = m.path, m.version
				}
			}
		}
		return best, bestVer
	}
	isLocal := func(importPath string) bool {
		return importPath == mainModulePath || strings.HasPrefix(importPath, mainModulePath+"/")
	}

	out := map[string]bool{}
	visited := map[string]bool{}
	var queue []string
	queue = append(queue, mainImports...)

	for len(queue) > 0 {
		imp := queue[0]
		queue = queue[1:]
		if visited[imp] || isStdlibImport(imp) || isLocal(imp) {
			continue
		}
		visited[imp] = true
		m, ver := moduleOf(imp)
		if m == "" {
			continue
		}
		out[m] = true
		imports, testImports, err := packageImportsWithTests(src, m, ver, imp)
		if err != nil {
			continue
		}
		for _, dep := range imports {
			if !visited[dep] {
				queue = append(queue, dep)
			}
		}
		for _, dep := range testImports {
			if !visited[dep] {
				queue = append(queue, dep)
			}
		}
	}
	return out
}

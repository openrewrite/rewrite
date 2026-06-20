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
	"golang.org/x/mod/module"
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
	var mods []modVer
	for _, m := range res.BuildList {
		if !m.Main {
			loaded[m.Path] = m.Version
			mods = append(mods, modVer{m.Path, m.Version})
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

	// Current root set: everything NeededModules already requires.
	roots := map[string]string{}
	for p, v := range base.Direct {
		roots[p] = v
	}
	for p, v := range base.Indirect {
		roots[p] = v
	}

	// Build a requirement index once: every module version's direct requires,
	// seeded for free from the already-resolved graph and lazily fetching only
	// the few promoted roots that pruning left unloaded. Pruned MVS then runs
	// entirely in memory — no per-iteration re-resolution.
	idx := newReqIndex(res, mainGoMod, src)

	// Walk the package import graph FRONTIER BY FRONTIER, by increasing import-
	// stack depth, mirroring cmd/go/internal/modload.tidyPrunedRoots. At each
	// frontier we recompute the pruned selection under the roots accumulated so
	// far, then promote any frontier module the pruned graph under-selects. This
	// ordering is essential: promoting a shallow module (e.g. kr/pretty) pins its
	// deeper requirements (kr/text) BEFORE they are examined, so they are not
	// wrongly promoted. A package's TEST imports are deferred one frontier deeper
	// (go models this as a separate `<pkg>.test` node) so test-transitive deps
	// sort below ordinary ones.
	type qitem struct {
		path   string
		isTest bool
	}
	queued := map[string]bool{}
	var queue []qitem
	enq := func(path string, isTest bool) {
		if isStdlibImport(path) || isLocal(path) {
			return
		}
		k := path
		if isTest {
			k += "\x00t"
		}
		if !queued[k] {
			queued[k] = true
			queue = append(queue, qitem{path, isTest})
		}
	}
	for _, imp := range mainImports {
		enq(imp, false)
	}

	for len(queue) > 0 {
		sel := prunedSelectInMemory(roots, idx)
		frontier := queue
		queue = nil
		for _, it := range frontier {
			mod, ver := moduleOf(it.path)
			if mod == "" {
				continue
			}
			imports, testImports, err := packageImportsWithTests(src, mod, ver, it.path)
			if err == nil {
				if it.isTest {
					for _, d := range testImports {
						enq(d, false)
					}
				} else {
					for _, d := range imports {
						enq(d, false)
					}
					enq(it.path, true) // the package's test node, one frontier deeper
				}
			}
			if _, isRoot := roots[mod]; !isRoot {
				want := loaded[mod]
				have := sel[mod]
				if want != "" && (have == "" || semver.Compare(have, want) < 0) {
					roots[mod] = want
				}
			}
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

// reqIndex memoizes each module version's direct requirements (post-replace) and
// go directive. It is seeded for free from an already-resolved graph (whose edges
// cover every LOADED module) and lazily fetches the go.mod of any module that
// pruning left unloaded — only ever the handful of pruned modules promoted to
// roots. This lets prunedSelectInMemory run a pruned MVS without re-resolving.
type reqIndex struct {
	edges   map[string][]module.Version // "path@version" -> requires (post-replace)
	gover   map[string]string           // "path@version" -> go directive
	known   map[string]bool             // keys whose edges are fully populated
	replace map[string]module.Version   // main module's version replacements
	src     ModSource
}

func newReqIndex(res Result, mainGoMod string, src ModSource) *reqIndex {
	idx := &reqIndex{
		edges:   map[string][]module.Version{},
		gover:   map[string]string{},
		known:   map[string]bool{},
		replace: map[string]module.Version{},
		src:     src,
	}
	// Main module's version replacements (local-path replaces are skipped; they
	// already mark the resolution incomplete upstream).
	if mf, err := modfile.Parse("go.mod", []byte(mainGoMod), nil); err == nil {
		for _, r := range mf.Replace {
			if r.New.Version == "" {
				continue
			}
			idx.replace[r.Old.Path] = r.New
			idx.replace[r.Old.Path+"@"+r.Old.Version] = r.New
		}
	}
	// Seed edges from the resolved graph. Every module that was LOADED contributes
	// all of its require edges here, so its key is fully known.
	for _, e := range res.Graph {
		key := e.FromPath + "@" + e.FromVersion
		idx.edges[key] = append(idx.edges[key], module.Version{Path: e.ToPath, Version: e.ToVersion})
		idx.known[key] = true
	}
	for _, m := range res.BuildList {
		idx.gover[m.Path+"@"+m.Version] = m.GoVersion
	}
	return idx
}

func (idx *reqIndex) applyReplace(m module.Version) module.Version {
	if nv, ok := idx.replace[m.Path+"@"+m.Version]; ok {
		return nv
	}
	if nv, ok := idx.replace[m.Path]; ok {
		return nv
	}
	return m
}

// requires returns module path@version's direct requirements and go directive,
// fetching and memoizing the go.mod when not already seeded.
func (idx *reqIndex) requires(path, version string) ([]module.Version, string) {
	key := path + "@" + version
	if idx.known[key] {
		return idx.edges[key], idx.gover[key]
	}
	idx.known[key] = true // memoize even on miss, so a failed fetch isn't retried
	b, ok := idx.src.GoMod(path, version)
	if !ok {
		return nil, idx.gover[key]
	}
	df, err := modfile.Parse(key, b, nil)
	if err != nil {
		return nil, idx.gover[key]
	}
	if df.Go != nil {
		idx.gover[key] = df.Go.Version
	}
	reqs := make([]module.Version, 0, len(df.Require))
	for _, r := range df.Require {
		reqs = append(reqs, idx.applyReplace(r.Mod))
	}
	idx.edges[key] = reqs
	return reqs, idx.gover[key]
}

// prunedSelectInMemory computes the MVS-selected version of every module under the
// go>=1.17 PRUNED module graph rooted at the given root set, reading requirements
// from idx. It mirrors Resolve's traversal exactly: every loaded module's requires
// become build-list nodes, but a module's requires are recursed into only when the
// module is unpruned (its go directive is < 1.17).
func prunedSelectInMemory(roots map[string]string, idx *reqIndex) map[string]string {
	present := map[string]string{} // path -> selected version
	loadPath := map[string]bool{}  // paths we recurse into
	enqueued := map[string]bool{}
	type pv struct{ path, version string }
	var queue []pv

	enqueue := func(m module.Version) {
		k := m.Path + "@" + m.Version
		if !enqueued[k] {
			enqueued[k] = true
			queue = append(queue, pv{m.Path, m.Version})
		}
	}
	setNode := func(m module.Version) {
		if v, ok := present[m.Path]; !ok || semver.Compare(m.Version, v) > 0 {
			present[m.Path] = m.Version
			if loadPath[m.Path] {
				enqueue(m)
			}
		}
	}
	markLoad := func(m module.Version) {
		loadPath[m.Path] = true
		enqueue(m)
	}

	// Roots = the synthetic main module's requirements.
	for path, ver := range roots {
		m := module.Version{Path: path, Version: ver}
		setNode(m)
		markLoad(m)
	}

	for len(queue) > 0 {
		cur := queue[0]
		queue = queue[1:]
		if present[cur.path] != cur.version {
			continue // superseded by a higher selected version
		}
		reqs, goV := idx.requires(cur.path, cur.version)
		unpruned := goUnpruned(goV)
		for _, req := range reqs {
			setNode(req)
			if unpruned {
				markLoad(req)
			}
		}
	}
	return present
}

// modVer is a build-list module path at its selected version.
type modVer struct{ path, version string }

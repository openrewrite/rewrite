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

// Package modgraph resolves a Go module's transitive module graph from the
// local module cache, WITHOUT invoking the `go` tool or touching the network.
//
// It exists so the parser/connector can compute the graph once, at ingest
// time, and freeze it into the GoResolutionResult marker — recipes then read
// the graph as pure data and never perform I/O.
//
// What it reads (all plain files under $GOMODCACHE/cache/download):
//
//	<esc(path)>/@v/<esc(version)>.mod      each dependency's own go.mod (the edges)
//	<esc(path)>/@v/<esc(version)>.ziphash  the h1: module hash (for go.sum)
//
// Limitations (documented, not hidden):
//   - The graph is the FULL transitive graph, not the go>=1.17 pruned graph,
//     so BuildList may be a superset of `go list -m all` for pruned modules.
//   - MVS edges are recorded at the version each module was first reached at;
//     this matches `go mod graph` whenever a module appears at a single
//     version (the common case), which it does for tidy'd modules.
//   - Only version-form `replace` directives in the MAIN module are applied;
//     local-path replaces are skipped (marked incomplete).
package modgraph

import (
	"bytes"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"golang.org/x/mod/modfile"
	"golang.org/x/mod/module"
	"golang.org/x/mod/semver"
	"golang.org/x/mod/sumdb/dirhash"
)

// Module is one node of the resolved graph at its selected version.
type Module struct {
	Path       string
	Version    string // "" for the main module
	GoVersion  string // the module's own `go` directive
	Main       bool
	ModuleHash string // h1: zip hash
	GoModHash  string // h1: go.mod hash
}

// Edge is a require edge From -> To.
type Edge struct {
	FromPath    string
	FromVersion string
	ToPath      string
	ToVersion   string
	Indirect    bool
}

// Result is the resolved graph.
type Result struct {
	BuildList []Module
	Graph     []Edge
	Complete  bool // false if any dependency metadata was missing/unreadable
}

// Resolve builds the module graph and (pruned) build list for the main module
// described by the raw go.mod content, fetching each dependency's go.mod from
// the given ModSource (local cache, GOPROXY, or a tiered combination). It
// performs no process execution.
//
// The traversal mirrors the go>=1.17 pruned module graph
// (cmd/go/internal/modload/buildlist.go:readModGraph): every loaded module's
// requirements become build-list NODES, but a module's requirements are only
// RECURSED into when that module is unpruned (its go directive is < 1.17). The
// resulting build list matches `go list -m all`.
func Resolve(mainGoMod []byte, src ModSource) (Result, error) {
	mf, err := modfile.Parse("go.mod", mainGoMod, nil)
	if err != nil {
		return Result{}, fmt.Errorf("parse main go.mod: %w", err)
	}

	res := Result{Complete: true}

	// Main-module version replacements, keyed by "path" and "path@version".
	replace := map[string]module.Version{}
	for _, r := range mf.Replace {
		if r.New.Version == "" { // local filesystem replace — can't resolve from a module source
			res.Complete = false
			continue
		}
		replace[r.Old.Path] = r.New
		replace[r.Old.Path+"@"+r.Old.Version] = r.New
	}
	applyReplace := func(m module.Version) module.Version {
		if nv, ok := replace[m.Path+"@"+m.Version]; ok {
			return nv
		}
		if nv, ok := replace[m.Path]; ok {
			return nv
		}
		return m
	}

	mainPath := ""
	mainGo := ""
	if mf.Module != nil {
		mainPath = mf.Module.Mod.Path
	}
	if mf.Go != nil {
		mainGo = mf.Go.Version
	}

	present := map[string]string{}      // path -> MVS-selected version (build-list nodes)
	goVersionAt := map[string]string{}  // path@version -> go directive
	goModBytesAt := map[string][]byte{} // path@version -> go.mod bytes (loaded modules)
	loadPath := map[string]bool{}       // paths we recurse into

	type pv struct{ path, version string }
	var loadQueue []pv
	enqueued := map[string]bool{}
	enqueueLoad := func(m module.Version) {
		k := m.Path + "@" + m.Version
		if !enqueued[k] {
			enqueued[k] = true
			loadQueue = append(loadQueue, pv{m.Path, m.Version})
		}
	}
	// setNode records a build-list node at its highest seen version. If the
	// version of a load-path is raised, re-enqueue it (simple iterative MVS).
	setNode := func(m module.Version) {
		if v, ok := present[m.Path]; !ok || semver.Compare(m.Version, v) > 0 {
			present[m.Path] = m.Version
			if loadPath[m.Path] {
				enqueueLoad(m)
			}
		}
	}
	markLoad := func(m module.Version) {
		loadPath[m.Path] = true
		enqueueLoad(m)
	}

	// Roots: the main module's requirements.
	for _, r := range mf.Require {
		to := applyReplace(r.Mod)
		res.Graph = append(res.Graph, Edge{FromPath: mainPath, FromVersion: "", ToPath: to.Path, ToVersion: to.Version, Indirect: r.Indirect})
		setNode(to)
		markLoad(to)
	}

	for len(loadQueue) > 0 {
		cur := loadQueue[0]
		loadQueue = loadQueue[1:]
		if present[cur.path] != cur.version {
			continue // superseded by a higher selected version
		}
		key := cur.path + "@" + cur.version
		b, ok := src.GoMod(cur.path, cur.version)
		if !ok {
			res.Complete = false
			continue
		}
		df, err := modfile.Parse(key, b, nil)
		if err != nil {
			res.Complete = false
			continue
		}
		goModBytesAt[key] = b
		goV := ""
		if df.Go != nil {
			goV = df.Go.Version
		}
		goVersionAt[key] = goV
		unpruned := goUnpruned(goV)
		for _, r := range df.Require {
			to := applyReplace(r.Mod)
			res.Graph = append(res.Graph, Edge{FromPath: cur.path, FromVersion: cur.version, ToPath: to.Path, ToVersion: to.Version, Indirect: r.Indirect})
			setNode(to) // every requirement of a loaded module is a build-list node
			if unpruned {
				markLoad(to) // recurse only through unpruned (go<1.17) modules
			}
		}
	}

	// Assemble the build list. Each module carries a GoModHash (go.sum records
	// a go.mod hash for the whole build list); ModuleHash (the zip hash) is set
	// only when the source can provide it without downloading the zip.
	res.BuildList = append(res.BuildList, Module{Path: mainPath, Version: "", GoVersion: mainGo, Main: true})
	for path, version := range present {
		key := path + "@" + version
		m := Module{Path: path, Version: version, GoVersion: goVersionAt[key]}
		b, ok := goModBytesAt[key]
		if !ok {
			// Leaf node (not recursed into): fetch its go.mod for the go
			// directive and hash. Best-effort — its absence does not change
			// build-list membership.
			b, ok = src.GoMod(path, version)
			if ok {
				if df, e := modfile.Parse(key, b, nil); e == nil && df.Go != nil {
					m.GoVersion = df.Go.Version
				}
			}
		}
		if ok {
			if h, err := goModHashBytes(b, path, version); err == nil {
				m.GoModHash = h
			}
		}
		if h, has := src.ZipHash(path, version); has {
			m.ModuleHash = h
		}
		res.BuildList = append(res.BuildList, m)
	}
	return res, nil
}

// goUnpruned reports whether a module with the given go directive is UNPRUNED
// (go < 1.17), meaning its transitive requirements are part of the module graph.
// An empty/invalid version is treated as unpruned (pre-1.16 behavior).
func goUnpruned(v string) bool {
	if v == "" {
		return true
	}
	parts := strings.SplitN(v, ".", 3)
	if len(parts) < 2 {
		return true
	}
	maj, err1 := strconv.Atoi(parts[0])
	min, err2 := strconv.Atoi(parts[1])
	if err1 != nil || err2 != nil {
		return true
	}
	return maj < 1 || (maj == 1 && min < 17)
}

// readCacheFile reads $download/<esc(path)>/@v/<esc(version)><suffix>.
func readCacheFile(download, path, version, suffix string) ([]byte, error) {
	ep, err := module.EscapePath(path)
	if err != nil {
		return nil, err
	}
	ev, err := module.EscapeVersion(version)
	if err != nil {
		return nil, err
	}
	return os.ReadFile(filepath.Join(download, ep, "@v", ev+suffix))
}

// readZipHash returns the h1: module hash recorded in the cache `.ziphash`.
func readZipHash(download, path, version string) (string, error) {
	b, err := readCacheFile(download, path, version, ".ziphash")
	if err != nil {
		return "", err
	}
	h := string(b)
	for len(h) > 0 && (h[len(h)-1] == '\n' || h[len(h)-1] == '\r') {
		h = h[:len(h)-1]
	}
	return h, nil
}

// goModHash computes the h1: hash of a module's go.mod, matching the value go
// records in go.sum (dirhash.Hash1 over the single "<path>@<version>/go.mod").
// goModHashBytes computes the h1: hash of go.mod content, matching the value go
// records in go.sum (dirhash.Hash1 over the single "<path>@<version>/go.mod").
func goModHashBytes(b []byte, path, version string) (string, error) {
	name := path + "@" + version + "/go.mod"
	return dirhash.Hash1([]string{name}, func(string) (io.ReadCloser, error) {
		return io.NopCloser(bytes.NewReader(b)), nil
	})
}

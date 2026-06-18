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
	"errors"
	goparser "go/parser"
	"go/token"
	"strings"
)

var errPackageNotFound = errors.New("package source not found")

// RequireSet is the set of modules `go mod tidy` would write to go.mod,
// classified the way tidy classifies them. It is computed from the package
// import graph, not just the module graph, so it matches go.mod exactly.
type RequireSet struct {
	// Direct maps module path -> version for modules that provide a package
	// imported by the main module itself.
	Direct map[string]string
	// Indirect maps module path -> version for modules needed transitively to
	// build the main module's packages, but not imported by it directly.
	Indirect map[string]string
	// Complete is false if any package directory could not be read (e.g. a
	// module was not extracted to the cache), making the set best-effort.
	Complete bool
	// Unresolved lists import paths that mapped to no build-list module.
	Unresolved []string
	// MissingDirs lists package directories that could not be read.
	MissingDirs []string
}

// NeededModules computes the exact go.mod require set by walking the package
// import graph. It starts from mainImports — the union of import paths across
// ALL of the main module's .go files (including its tests, which tidy counts)
// — and follows imports into dependency packages, reading their imports from
// the extracted module cache. No `go` execution, no network.
//
// Dependency test files are skipped (tidy does not load dep tests for a
// go>=1.17 main module). Build-constraint-excluded files ARE read: tidy keeps
// modules needed by any GOOS/GOARCH, so all platform files count.
//
// separateIndirect selects the go.mod policy: when true (go >= 1.17) the full
// transitive indirect set is recorded; when false (go < 1.17) indirect modules
// that are implied by another required module's go.mod are omitted, matching
// the smaller pre-1.17 require list.
func NeededModules(mainImports []string, mainModulePath string, res Result, src ModSource, separateIndirect bool) RequireSet {
	rs := RequireSet{Direct: map[string]string{}, Indirect: map[string]string{}, Complete: true}

	type modver struct{ path, version string }
	var mods []modver
	for _, m := range res.BuildList {
		if m.Main {
			continue
		}
		mods = append(mods, modver{m.Path, m.Version})
	}
	// moduleOf returns the longest build-list module path that is a prefix of
	// importPath (the module that provides that package), with its version.
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

	needed := map[string]string{}
	direct := map[string]bool{}
	visited := map[string]bool{}
	var queue []string

	for _, imp := range mainImports {
		if isStdlibImport(imp) || isLocal(imp) {
			continue
		}
		m, ver := moduleOf(imp)
		if m == "" {
			rs.Complete = false
			continue
		}
		direct[m] = true
		needed[m] = ver
		queue = append(queue, imp)
	}

	for len(queue) > 0 {
		imp := queue[0]
		queue = queue[1:]
		if visited[imp] {
			continue
		}
		visited[imp] = true
		if isStdlibImport(imp) || isLocal(imp) {
			continue
		}
		m, ver := moduleOf(imp)
		if m == "" {
			// Import maps to no build-list module. Since the build list is
			// authoritative (it mirrors `go list -m all`), this means the
			// import is not part of the real build — e.g. an `//go:build
			// ignore` generator or a platform the build doesn't select. It is
			// not a missing dependency, so record it for diagnostics but do
			// not treat resolution as incomplete.
			rs.Unresolved = append(rs.Unresolved, imp)
			continue
		}
		needed[m] = ver

		deps, err := packageImports(src, m, ver, imp)
		if err != nil {
			rs.Complete = false
			rs.MissingDirs = append(rs.MissingDirs, imp)
			continue
		}
		for _, dep := range deps {
			if isStdlibImport(dep) || isLocal(dep) || visited[dep] {
				continue
			}
			queue = append(queue, dep)
		}
	}

	// For go < 1.17, omit indirect modules that are implied by another needed
	// module's go.mod (their version is already pinned by the graph). go >= 1.17
	// records the full set.
	implied := map[string]bool{}
	if !separateIndirect {
		for _, e := range res.Graph {
			if e.FromPath != "" && e.FromPath != mainModulePath && needed[e.FromPath] != "" {
				implied[e.ToPath] = true
			}
		}
	}

	for mod, ver := range needed {
		switch {
		case direct[mod]:
			rs.Direct[mod] = ver
		case implied[mod]:
			// omitted (pre-1.17 implied indirect)
		default:
			rs.Indirect[mod] = ver
		}
	}
	return rs
}

// packageImports returns the non-test imports of the package at importPath,
// which lives in module mod@version. Source files come from the ModSource (the
// local cache or a downloaded+extracted module zip).
func packageImports(src ModSource, mod, version, importPath string) ([]string, error) {
	files, ok := src.PackageGoFiles(mod, version, importPath)
	if !ok {
		return nil, errPackageNotFound
	}
	set := map[string]bool{}
	fset := token.NewFileSet()
	for name, content := range files {
		if strings.HasSuffix(name, "_test.go") {
			continue
		}
		f, err := goparser.ParseFile(fset, name, content, goparser.ImportsOnly)
		if err != nil {
			continue
		}
		for _, spec := range f.Imports {
			p := strings.Trim(spec.Path.Value, "\"`")
			if p != "" {
				set[p] = true
			}
		}
	}
	out := make([]string, 0, len(set))
	for p := range set {
		out = append(out, p)
	}
	return out, nil
}

// isStdlibImport reports whether importPath is a standard-library package
// (no dot in its first path segment). Mirrors gofmt/goimports' heuristic.
func isStdlibImport(importPath string) bool {
	first := importPath
	if i := strings.IndexByte(importPath, '/'); i >= 0 {
		first = importPath[:i]
	}
	return !strings.Contains(first, ".")
}

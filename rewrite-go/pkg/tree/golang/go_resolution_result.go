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

package golang

import "github.com/google/uuid"

// GoResolutionResult mirrors org.openrewrite.golang.marker.GoResolutionResult
// on the Java side: the metadata parsed from a Go module's go.mod file.
// Attached as a Marker to a source representing a go.mod (in tests, to the
// test SourceSpec; at runtime, to whatever tree the Go parser produces for
// go.mod content).
type GoResolutionResult struct {
	Ident                uuid.UUID
	ModulePath           string
	GoVersion            string // empty if no `go` directive
	Toolchain            string // empty if no `toolchain` directive
	Path                 string // path to the go.mod file
	Requires             []GoRequire
	Replaces             []GoReplace
	Excludes             []GoExclude
	Retracts             []GoRetract
	ResolvedDependencies []GoResolvedDependency

	// --- resolved module graph (populated at parse time by pkg/parser/modgraph) ---
	//
	// These fields carry the transitive module graph so that recipes (e.g.
	// GoModTidy Phase 2) can prune unused indirect requires, bump the `go`
	// directive, and generate go.sum WITHOUT any I/O or toolchain access at
	// recipe time. They are best-effort: if the module cache / proxy could
	// not be fully traversed at parse time, GraphComplete is false and the
	// data is partial.

	// BuildList is the MVS-selected version of every module in the graph,
	// including the main module. Mirrors `go list -m all`.
	BuildList []GoModule
	// Graph holds the require edges between modules (the data that the main
	// module's go.mod alone does not contain). Mirrors `go mod graph`.
	Graph []GoModuleEdge
	// GraphComplete reports whether BuildList/Graph were fully resolved.
	GraphComplete bool
}

// GoModule is one node of the resolved module graph: a module at its
// MVS-selected version, with the metadata needed for tidy operations.
type GoModule struct {
	ModulePath string
	Version    string // "" for the main module
	GoVersion  string // the module's OWN `go` directive — drives go-directive bump
	Main       bool
	ModuleHash string // h1: zip hash from the cache `.ziphash` — for go.sum
	GoModHash  string // h1: hash of the module's go.mod — for go.sum
}

// GoModuleEdge is a require edge `From` -> `To` in the resolved graph.
// Indirect reflects whether the require in From's go.mod was `// indirect`.
type GoModuleEdge struct {
	FromPath    string
	FromVersion string
	ToPath      string
	ToVersion   string
	Indirect    bool
}

func (m GoResolutionResult) ID() uuid.UUID { return m.Ident }

// FindRequire returns the require entry for a module, or nil.
func (m GoResolutionResult) FindRequire(modulePath string) *GoRequire {
	for i := range m.Requires {
		if m.Requires[i].ModulePath == modulePath {
			return &m.Requires[i]
		}
	}
	return nil
}

// FindResolved returns the resolved dependency for a module, or nil.
func (m GoResolutionResult) FindResolved(modulePath string) *GoResolvedDependency {
	for i := range m.ResolvedDependencies {
		if m.ResolvedDependencies[i].ModulePath == modulePath {
			return &m.ResolvedDependencies[i]
		}
	}
	return nil
}

// GoRequire is one entry in the go.mod `require` list.
type GoRequire struct {
	ModulePath string
	Version    string
	Indirect   bool // true if marked `// indirect`
}

// GoReplace is one entry in the go.mod `replace` list.
// OldVersion is empty if the replace targets all versions of OldPath.
// NewVersion is empty if NewPath is a local filesystem path.
type GoReplace struct {
	OldPath    string
	OldVersion string
	NewPath    string
	NewVersion string
}

// GoExclude is one entry in the go.mod `exclude` list.
type GoExclude struct {
	ModulePath string
	Version    string
}

// GoRetract is one entry in the go.mod `retract` list.
// VersionRange is the raw expression as written, e.g. "v1.0.0" or "[v1.0.0, v1.1.0]".
type GoRetract struct {
	VersionRange string
	Rationale    string // empty if no `// ...` comment
}

// GoResolvedDependency is one entry from go.sum.
type GoResolvedDependency struct {
	ModulePath string
	Version    string
	ModuleHash string // h1:... — empty if only the go.mod hash is recorded
	GoModHash  string
}

// NewGoResolutionResult creates a GoResolutionResult marker with a fresh UUID.
//
// The directive slices are initialized to empty (non-nil) values. Go has no
// way to declare a slice non-nullable at the type level, so this constructor is
// the single chokepoint that guarantees it: a nil slice would be serialized by
// the RPC send codec as a null list, which the Java receive side stores as a
// null field and the Moderne reflective binary LST serializer then NPEs on.
func NewGoResolutionResult(modulePath, goVersion, toolchain, path string) GoResolutionResult {
	return GoResolutionResult{
		Ident:                uuid.New(),
		ModulePath:           modulePath,
		GoVersion:            goVersion,
		Toolchain:            toolchain,
		Path:                 path,
		Requires:             []GoRequire{},
		Replaces:             []GoReplace{},
		Excludes:             []GoExclude{},
		Retracts:             []GoRetract{},
		ResolvedDependencies: []GoResolvedDependency{},
		BuildList:            []GoModule{},
		Graph:                []GoModuleEdge{},
		GraphComplete:        false,
	}
}

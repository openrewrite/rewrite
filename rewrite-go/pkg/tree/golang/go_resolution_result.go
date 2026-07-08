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
	// PackageModules maps an imported package path to its providing module.
	// Go-specific: unlike other ecosystems the import path is not the module
	// coordinate, so this mapping requires toolchain resolution. Empty unless
	// the parse-time resolution gate is on.
	PackageModules []GoPackageModule
}

func (m GoResolutionResult) ID() uuid.UUID { return m.Ident }

func (m GoResolutionResult) FindRequire(modulePath string) *GoRequire {
	for i := range m.Requires {
		if m.Requires[i].ModulePath == modulePath {
			return &m.Requires[i]
		}
	}
	return nil
}

func (m GoResolutionResult) FindResolved(modulePath string) *GoResolvedDependency {
	for i := range m.ResolvedDependencies {
		if m.ResolvedDependencies[i].ModulePath == modulePath {
			return &m.ResolvedDependencies[i]
		}
	}
	return nil
}

func (m GoResolutionResult) FindPackageModule(importPath string) *GoPackageModule {
	for i := range m.PackageModules {
		if m.PackageModules[i].ImportPath == importPath {
			return &m.PackageModules[i]
		}
	}
	return nil
}

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

// GoResolvedDependency is one node in the resolved build list. It merges what
// go.sum records (content hashes) with what the toolchain resolves (`go list -m`
// build-list metadata and `go mod graph` edges). The toolchain-sourced fields
// are zero-valued when the parse-time resolution gate is off (go.sum-only).
type GoResolvedDependency struct {
	ModulePath      string
	Version         string
	ModuleHash      string // h1:... — empty if only the go.mod hash is recorded
	GoModHash       string
	Indirect        bool   // from `go list -m`: present only transitively
	Main            bool   // from `go list -m`: this is the main module
	ReplacePath     string // toolchain-applied replace target, empty if none
	ReplaceVersion  string
	ModuleGoVersion string // this module's own `go` directive, from `go list -m`
	// Deps are the direct module dependencies of this node (from `go mod graph`),
	// referenced by module@version. Resolve against ResolvedDependencies. Nil when
	// the graph is unavailable. Edges (not nested nodes) keep this cycle-safe and
	// value-typed; Go's MVS gives one selected version per module path.
	Deps []GoModuleRef
}

// GoModuleRef identifies a module version, used as a graph edge target.
type GoModuleRef struct {
	ModulePath string
	Version    string
}

// GoPackageModule maps an imported package path to the module that provides it,
// from `go list -deps -json ./...`. ModulePath is empty for the standard library
// (Standard is true).
type GoPackageModule struct {
	ImportPath string
	ModulePath string
	Version    string
	Standard   bool
}

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
		PackageModules:       []GoPackageModule{},
	}
}

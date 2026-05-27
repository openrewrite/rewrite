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

package tree

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
func NewGoResolutionResult(modulePath, goVersion, toolchain, path string) GoResolutionResult {
	return GoResolutionResult{
		Ident:      uuid.New(),
		ModulePath: modulePath,
		GoVersion:  goVersion,
		Toolchain:  toolchain,
		Path:       path,
	}
}

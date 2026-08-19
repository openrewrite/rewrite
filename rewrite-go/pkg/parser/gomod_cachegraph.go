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
	"os"
	"path/filepath"
	"strings"

	"golang.org/x/mod/modfile"
	"golang.org/x/mod/module"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// AttachCachedEdges fills in each selected build-list member's Deps from the copy
// of its go.mod in the module cache, whose requires are that module's edges — the
// same set `go mod graph` prints. The build list supplies every version, so this
// reads the cache and nothing else.
//
// Deps stays nil for a module the cache does not hold, which is distinct from an
// empty slice: a partially warm cache must not read as a module with no
// dependencies.
func AttachCachedEdges(cacheDir string, buildList []golang.GoResolvedDependency) []golang.GoResolvedDependency {
	out := make([]golang.GoResolvedDependency, len(buildList))
	copy(out, buildList)
	if cacheDir == "" {
		return out
	}
	for i, mod := range out {
		if !mod.Selected {
			continue
		}
		if deps, ok := cachedModuleEdges(cacheDir, mod.ModulePath, mod.Version); ok {
			out[i].Deps = deps
		}
	}
	return out
}

// cachedModuleEdges reads $GOMODCACHE/cache/download/<escaped>/@v/<version>.mod.
// A module whose own go.mod is unreadable or unparseable contributes no edges
// rather than failing the parse.
func cachedModuleEdges(cacheDir, modulePath, version string) ([]golang.GoModuleRef, bool) {
	// The cache lowercases each uppercase letter behind a `!` so its layout
	// survives case-insensitive filesystems. Versions carry the same encoding.
	escapedPath, err := module.EscapePath(modulePath)
	if err != nil {
		return nil, false
	}
	escapedVersion, err := module.EscapeVersion(version)
	if err != nil {
		return nil, false
	}
	path := filepath.Join(cacheDir, "cache", "download", escapedPath, "@v", escapedVersion+".mod")
	content, err := os.ReadFile(path)
	if err != nil {
		return nil, false
	}
	f, err := modfile.Parse(path, content, nil)
	if err != nil {
		return nil, false
	}
	edges := make([]golang.GoModuleRef, 0, len(f.Require))
	for _, r := range f.Require {
		edges = append(edges, golang.GoModuleRef{ModulePath: r.Mod.Path, Version: r.Mod.Version})
	}
	return edges, true
}

func GoModCacheDir() string {
	if dir := os.Getenv("GOMODCACHE"); dir != "" {
		return dir
	}
	if gopath := os.Getenv("GOPATH"); gopath != "" {
		// GOPATH is a list; the module cache lives under its first entry.
		first, _, _ := strings.Cut(gopath, string(os.PathListSeparator))
		return filepath.Join(first, "pkg", "mod")
	}
	home, err := os.UserHomeDir()
	if err != nil {
		return ""
	}
	return filepath.Join(home, "go", "pkg", "mod")
}

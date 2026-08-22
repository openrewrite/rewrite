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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"golang.org/x/mod/module"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// writeCachedGoMod lays out $GOMODCACHE/cache/download/<escaped>/@v/<version>.mod,
// the path the Go toolchain populates when it downloads a module.
func writeCachedGoMod(t *testing.T, cache, modulePath, version, content string) {
	t.Helper()
	escaped, err := module.EscapePath(modulePath)
	require.NoError(t, err)
	dir := filepath.Join(cache, "cache", "download", escaped, "@v")
	require.NoError(t, os.MkdirAll(dir, 0o755))
	require.NoError(t, os.WriteFile(filepath.Join(dir, version+".mod"), []byte(content), 0o644))
}

func buildListOf(pairs ...string) []golang.GoResolvedDependency {
	var out []golang.GoResolvedDependency
	for i := 0; i < len(pairs); i += 2 {
		out = append(out, golang.GoResolvedDependency{ModulePath: pairs[i], Version: pairs[i+1], Selected: true})
	}
	return out
}

func TestAttachCachedEdges(t *testing.T) {
	cache := t.TempDir()
	writeCachedGoMod(t, cache, "example.com/a", "v1.0.0", `module example.com/a
go 1.21
require example.com/b v1.2.0
`)
	writeCachedGoMod(t, cache, "example.com/b", "v1.2.0", "module example.com/b\ngo 1.21\n")

	list := buildListOf("example.com/a", "v1.0.0", "example.com/b", "v1.2.0")
	attached := AttachCachedEdges(cache, list)

	require.Len(t, attached, 2)
	require.Len(t, attached[0].Deps, 1)
	assert.Equal(t, "example.com/b", attached[0].Deps[0].ModulePath)
	assert.Equal(t, "v1.2.0", attached[0].Deps[0].Version)
	assert.Empty(t, attached[1].Deps)
}

func TestAttachCachedEdgesEscapesUppercasePaths(t *testing.T) {
	cache := t.TempDir()
	writeCachedGoMod(t, cache, "github.com/Azure/go-autorest", "v1.0.0", `module github.com/Azure/go-autorest
require example.com/b v1.2.0
`)
	escaped, err := module.EscapePath("github.com/Azure/go-autorest")
	require.NoError(t, err)
	assert.Contains(t, escaped, "!azure")

	attached := AttachCachedEdges(cache, buildListOf("github.com/Azure/go-autorest", "v1.0.0"))

	require.Len(t, attached[0].Deps, 1)
	assert.Equal(t, "example.com/b", attached[0].Deps[0].ModulePath)
}

func TestAttachCachedEdgesLeavesUncachedModulesNil(t *testing.T) {
	cache := t.TempDir()
	writeCachedGoMod(t, cache, "example.com/a", "v1.0.0", "module example.com/a\nrequire example.com/b v1.2.0\n")

	attached := AttachCachedEdges(cache, buildListOf("example.com/a", "v1.0.0", "example.com/absent", "v9.9.9"))

	assert.NotNil(t, attached[0].Deps)
	assert.Nil(t, attached[1].Deps, "absent from the cache is not the same as having no dependencies")
}

func TestAttachCachedEdgesSkipsUnselectedRows(t *testing.T) {
	cache := t.TempDir()
	writeCachedGoMod(t, cache, "example.com/a", "v1.0.0", "module example.com/a\nrequire example.com/b v1.2.0\n")

	list := []golang.GoResolvedDependency{{ModulePath: "example.com/a", Version: "v1.0.0", Selected: false}}
	attached := AttachCachedEdges(cache, list)

	assert.Nil(t, attached[0].Deps, "a version MVS rejected has no edges in this build")
}

func TestAttachCachedEdgesTolerearesMalformedGoMod(t *testing.T) {
	cache := t.TempDir()
	writeCachedGoMod(t, cache, "example.com/a", "v1.0.0", "this is not a go.mod {{{")

	attached := AttachCachedEdges(cache, buildListOf("example.com/a", "v1.0.0"))

	assert.Nil(t, attached[0].Deps)
}

func TestAttachCachedEdgesNoCacheDir(t *testing.T) {
	attached := AttachCachedEdges("", buildListOf("example.com/a", "v1.0.0"))

	assert.Nil(t, attached[0].Deps)
}

func TestCachedEdgesEnrichVendoredBuildList(t *testing.T) {
	cache := t.TempDir()
	writeCachedGoMod(t, cache, "golang.org/x/mod", "v0.35.0", `module golang.org/x/mod
require golang.org/x/tools v0.43.0
`)
	vendored, _ := ParseVendorModules("# golang.org/x/mod v0.35.0\n## explicit\ngolang.org/x/mod/modfile\n")

	attached := AttachCachedEdges(cache, vendored)

	require.Len(t, attached[0].Deps, 1)
	assert.Equal(t, "golang.org/x/tools", attached[0].Deps[0].ModulePath)
}

func TestAttachCachedEdgesEscapesUppercaseVersions(t *testing.T) {
	cache := t.TempDir()
	escaped, err := module.EscapeVersion("v1.0.0-RC1")
	require.NoError(t, err)
	assert.Contains(t, escaped, "!r!c1", "the cache escapes the version, not just the path")
	dir := filepath.Join(cache, "cache", "download", "example.com/a", "@v")
	require.NoError(t, os.MkdirAll(dir, 0o755))
	require.NoError(t, os.WriteFile(filepath.Join(dir, escaped+".mod"),
		[]byte("module example.com/a\nrequire example.com/b v1.2.0\n"), 0o644))

	attached := AttachCachedEdges(cache, buildListOf("example.com/a", "v1.0.0-RC1"))

	require.Len(t, attached[0].Deps, 1)
	assert.Equal(t, "example.com/b", attached[0].Deps[0].ModulePath)
}

func TestGoModCacheDirUsesFirstGopathEntry(t *testing.T) {
	t.Setenv("GOMODCACHE", "")
	t.Setenv("GOPATH", strings.Join([]string{"/first", "/second"}, string(os.PathListSeparator)))

	assert.Equal(t, filepath.Join("/first", "pkg", "mod"), GoModCacheDir())
}

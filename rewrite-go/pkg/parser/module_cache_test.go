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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// writeCacheModule lays out one extracted module in a fake module cache and
// returns the cache root, which the caller points $GOMODCACHE at.
func writeCacheModule(t *testing.T, cache, coordinate string, files map[string]string) {
	t.Helper()
	for rel, content := range files {
		full := filepath.Join(cache, filepath.FromSlash(coordinate), filepath.FromSlash(rel))
		require.NoError(t, os.MkdirAll(filepath.Dir(full), 0o755))
		require.NoError(t, os.WriteFile(full, []byte(content), 0o644))
	}
}

const greeterSource = `package foo

type Greeter struct{ Name string }

func (g *Greeter) Greet() string { return "hi " + g.Name }
`

// A required module that is neither vendored nor a sibling resolves against its
// extracted sources in the module cache, so its symbols carry real types
// instead of the empty scope a stub package has.
func TestImportResolvesFromTheModuleCache(t *testing.T) {
	cache := t.TempDir()
	writeCacheModule(t, cache, "example.com/foo@v1.2.3", map[string]string{"greeter.go": greeterSource})
	t.Setenv("GOMODCACHE", cache)

	pi := NewProjectImporter("example.com/m", nil)
	pi.SetProjectRoot(t.TempDir())
	pi.AddRequire("example.com/foo")
	pi.AddModule("example.com/foo", "", "v1.2.3")

	pkg, err := pi.Import("example.com/foo")
	require.NoError(t, err)
	require.NotNil(t, pkg)
	require.NotNil(t, pkg.Scope().Lookup("Greeter"), "Greeter not in scope; got %v", pkg.Scope().Names())
}

// A sub-package of a required module lives under the same extracted directory.
func TestSubPackageResolvesFromTheModuleCache(t *testing.T) {
	cache := t.TempDir()
	writeCacheModule(t, cache, "example.com/foo@v1.2.3", map[string]string{
		"greeter.go": greeterSource,
		"sub/sub.go": "package sub\n\nfunc Helper() int { return 1 }\n",
	})
	t.Setenv("GOMODCACHE", cache)

	pi := NewProjectImporter("example.com/m", nil)
	pi.SetProjectRoot(t.TempDir())
	pi.AddModule("example.com/foo", "", "v1.2.3")

	pkg, err := pi.Import("example.com/foo/sub")
	require.NoError(t, err)
	require.NotNil(t, pkg.Scope().Lookup("Helper"), "Helper not in scope; got %v", pkg.Scope().Names())
}

// The module cache keeps every platform's sources side by side, so a package
// type-checks only when the files that do not apply are left out.
func TestModuleCacheHonorsBuildConstraints(t *testing.T) {
	cache := t.TempDir()
	writeCacheModule(t, cache, "example.com/plat@v1.0.0", map[string]string{
		"plat_linux.go":   "package plat\n\nconst OS = \"linux\"\n",
		"plat_windows.go": "package plat\n\nconst OS = \"windows\"\n",
		"plat_darwin.go":  "package plat\n\nconst OS = \"darwin\"\n",
	})
	t.Setenv("GOMODCACHE", cache)

	pi := NewProjectImporter("example.com/m", nil)
	pi.SetProjectRoot(t.TempDir())
	pi.AddModule("example.com/plat", "", "v1.0.0")

	pkg, err := pi.Import("example.com/plat")
	require.NoError(t, err)
	// Every file declares OS; only the one for this platform may be read, or
	// the package would not type-check at all.
	require.NotNil(t, pkg.Scope().Lookup("OS"), "OS not in scope; got %v", pkg.Scope().Names())
}

// Vendored sources are what the build would compile, so they win.
func TestVendorTreeWinsOverTheModuleCache(t *testing.T) {
	cache := t.TempDir()
	writeCacheModule(t, cache, "example.com/foo@v1.2.3", map[string]string{
		"greeter.go": "package foo\n\ntype FromCache struct{}\n",
	})
	t.Setenv("GOMODCACHE", cache)

	root := t.TempDir()
	vendored := filepath.Join(root, "vendor", "example.com", "foo")
	require.NoError(t, os.MkdirAll(vendored, 0o755))
	require.NoError(t, os.WriteFile(filepath.Join(vendored, "greeter.go"),
		[]byte("package foo\n\ntype FromVendor struct{}\n"), 0o644))

	pi := NewProjectImporter("example.com/m", nil)
	pi.SetProjectRoot(root)
	pi.AddModule("example.com/foo", "", "v1.2.3")

	pkg, err := pi.Import("example.com/foo")
	require.NoError(t, err)
	assert.NotNil(t, pkg.Scope().Lookup("FromVendor"))
	assert.Nil(t, pkg.Scope().Lookup("FromCache"))
}

func TestModuleCacheFollowsAModulePathReplace(t *testing.T) {
	cache := t.TempDir()
	writeCacheModule(t, cache, "example.com/fork@v2.0.0", map[string]string{
		"greeter.go": "package foo\n\ntype FromFork struct{}\n",
	})
	// The replaced-away coordinate is present too, so following the directive
	// is what the assertion turns on rather than the absence of the original.
	writeCacheModule(t, cache, "example.com/foo@v1.2.3", map[string]string{
		"greeter.go": "package foo\n\ntype FromOriginal struct{}\n",
	})
	t.Setenv("GOMODCACHE", cache)

	pi := NewProjectImporter("example.com/m", nil)
	pi.SetProjectRoot(t.TempDir())
	pi.AddModule("example.com/foo", "", "v1.2.3")
	pi.AddReplace("example.com/foo", "example.com/fork", "v2.0.0")

	pkg, err := pi.Import("example.com/foo")
	require.NoError(t, err)
	assert.NotNil(t, pkg.Scope().Lookup("FromFork"))
	assert.Nil(t, pkg.Scope().Lookup("FromOriginal"))
}

func TestModuleCacheFollowsALocalReplace(t *testing.T) {
	t.Setenv("GOMODCACHE", t.TempDir())
	root := t.TempDir()
	local := filepath.Join(root, "local", "foo")
	require.NoError(t, os.MkdirAll(local, 0o755))
	require.NoError(t, os.WriteFile(filepath.Join(local, "greeter.go"),
		[]byte("package foo\n\ntype FromLocal struct{}\n"), 0o644))

	pi := NewProjectImporter("example.com/m", nil)
	pi.SetProjectRoot(root)
	pi.AddModule("example.com/foo", "", "v1.2.3")
	pi.AddReplace("example.com/foo", "./local/foo", "")

	pkg, err := pi.Import("example.com/foo")
	require.NoError(t, err)
	assert.NotNil(t, pkg.Scope().Lookup("FromLocal"))
}

func TestAResolvedCoordinateIgnoresTheReplaceDirective(t *testing.T) {
	cache := t.TempDir()
	writeCacheModule(t, cache, "example.com/fork@v2.0.0", map[string]string{
		"greeter.go": "package foo\n\ntype FromFork struct{}\n",
	})
	t.Setenv("GOMODCACHE", cache)

	pi := NewProjectImporter("example.com/m", nil)
	pi.SetProjectRoot(t.TempDir())
	pi.AddModule("example.com/foo", "example.com/fork", "v2.0.0")
	pi.AddReplace("example.com/foo", "example.com/fork", "v2.0.0")

	pkg, err := pi.Import("example.com/foo")
	require.NoError(t, err)
	assert.NotNil(t, pkg.Scope().Lookup("FromFork"))
}

// A module the build list does not name, or one absent from the cache, still
// yields the typed-but-empty stub rather than an error.
func TestUnresolvableModuleStillStubs(t *testing.T) {
	t.Setenv("GOMODCACHE", t.TempDir())

	pi := NewProjectImporter("example.com/m", nil)
	pi.SetProjectRoot(t.TempDir())
	pi.AddRequire("example.com/gone")
	pi.AddModule("example.com/gone", "", "v9.9.9")

	pkg, err := pi.Import("example.com/gone")
	require.NoError(t, err)
	require.NotNil(t, pkg)
	assert.Empty(t, pkg.Scope().Names(), "a stub has an empty scope")
}

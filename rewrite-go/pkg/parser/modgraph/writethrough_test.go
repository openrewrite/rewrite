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
	"archive/zip"
	"bytes"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// fakeZip builds a minimal but valid module zip: every entry is prefixed
// "<module>@<version>/" as the module proxy requires.
func fakeZip(t *testing.T, modPath, version string, files map[string]string) []byte {
	t.Helper()
	var buf bytes.Buffer
	zw := zip.NewWriter(&buf)
	for name, content := range files {
		w, err := zw.Create(modPath + "@" + version + "/" + name)
		if err != nil {
			t.Fatal(err)
		}
		if _, err := w.Write([]byte(content)); err != nil {
			t.Fatal(err)
		}
	}
	if err := zw.Close(); err != nil {
		t.Fatal(err)
	}
	return buf.Bytes()
}

// TestWriteThroughPersistsStandardCacheLayout verifies that fetching a module's
// .mod and .zip through ProxyWriteThroughSource persists them (plus a computed
// h1: .ziphash) into the standard $GOMODCACHE/cache/download layout, and that a
// fresh CacheSource pointed at that directory then serves the same data offline
// — including package .go files via the zip fallback (no extraction needed).
func TestWriteThroughPersistsStandardCacheLayout(t *testing.T) {
	const modPath = "example.com/dep"
	const version = "v1.2.3"
	goMod := []byte("module example.com/dep\n\ngo 1.21\n")
	zipBytes := fakeZip(t, modPath, version, map[string]string{
		"pkg/util.go": "package util\n\nimport _ \"example.com/other\"\n",
		"go.mod":      string(goMod),
	})

	gomodcache := t.TempDir()
	var fetched []string
	get := func(url string) ([]byte, int, error) {
		fetched = append(fetched, url)
		switch {
		case strings.HasSuffix(url, "/@v/"+version+".mod"):
			return goMod, 200, nil
		case strings.HasSuffix(url, "/@v/"+version+".zip"):
			return zipBytes, 200, nil
		}
		return nil, 404, nil
	}

	src := ProxyWriteThroughSource("https://proxy.example", gomodcache, get)

	// 1. Fetch .mod and package files through the proxy (warming the cache).
	if b, ok := src.GoMod(modPath, version); !ok || !bytes.Equal(b, goMod) {
		t.Fatalf("proxy GoMod: ok=%v bytes=%q", ok, b)
	}
	files, ok := src.PackageGoFiles(modPath, version, modPath+"/pkg")
	if !ok || len(files) != 1 || !strings.Contains(string(files["util.go"]), "package util") {
		t.Fatalf("proxy PackageGoFiles: ok=%v files=%v", ok, files)
	}

	// 2. The standard cache layout must now hold .mod, .zip, and .ziphash.
	dl := filepath.Join(gomodcache, "cache", "download", modPath, "@v")
	for _, suffix := range []string{".mod", ".zip", ".ziphash"} {
		if _, err := os.Stat(filepath.Join(dl, version+suffix)); err != nil {
			t.Errorf("expected cached %s in standard layout: %v", version+suffix, err)
		}
	}
	zh, err := os.ReadFile(filepath.Join(dl, version+".ziphash"))
	if err != nil || !strings.HasPrefix(string(zh), "h1:") {
		t.Errorf("ziphash not a valid h1: hash: %q (err=%v)", zh, err)
	}

	// 3. A fresh CacheSource (no proxy) must serve everything offline.
	cache := CacheSource(gomodcache)
	if b, ok := cache.GoMod(modPath, version); !ok || !bytes.Equal(b, goMod) {
		t.Errorf("cache GoMod after write-through: ok=%v", ok)
	}
	if h, ok := cache.ZipHash(modPath, version); !ok || h != strings.TrimSpace(string(zh)) {
		t.Errorf("cache ZipHash after write-through: ok=%v h=%q want %q", ok, h, zh)
	}
	cfiles, ok := cache.PackageGoFiles(modPath, version, modPath+"/pkg")
	if !ok || !strings.Contains(string(cfiles["util.go"]), `import _ "example.com/other"`) {
		t.Errorf("cache PackageGoFiles via zip fallback: ok=%v files=%v", ok, cfiles)
	}
}

// TestProxySourceWithoutCacheDoesNotPersist guards the opt-in: plain ProxySource
// (no gomodcache) must not write anything to disk.
func TestProxySourceWithoutCacheDoesNotPersist(t *testing.T) {
	const modPath = "example.com/dep"
	const version = "v0.1.0"
	goMod := []byte("module example.com/dep\n\ngo 1.21\n")
	gomodcache := t.TempDir()
	get := func(url string) ([]byte, int, error) {
		if strings.HasSuffix(url, ".mod") {
			return goMod, 200, nil
		}
		return nil, 404, nil
	}
	// Plain ProxySource ignores the cache dir entirely.
	src := ProxySource("https://proxy.example", get)
	if _, ok := src.GoMod(modPath, version); !ok {
		t.Fatal("GoMod should succeed")
	}
	dl := filepath.Join(gomodcache, "cache", "download")
	if entries, _ := os.ReadDir(dl); len(entries) != 0 {
		t.Errorf("plain ProxySource must not persist; found %d entries", len(entries))
	}
}

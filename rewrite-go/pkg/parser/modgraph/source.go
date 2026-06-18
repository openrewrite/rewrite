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
	"sync"

	"golang.org/x/mod/module"
)

// ModSource supplies module metadata (go.mod files, and where available the
// zip hash) for the resolver. It abstracts WHERE the data comes from so the
// resolution algorithm is identical whether the bytes are read from the local
// module cache or fetched from a GOPROXY.
//
// The proxy implementation deliberately does not perform HTTP itself: it calls
// an injected getter, so in production the bytes are fetched through the CLI's
// OpenRewrite HttpSender (via bidirectional RPC), and in tests through a direct
// HTTP client or a fake.
type ModSource interface {
	// GoMod returns the go.mod bytes for module path@version, and whether it
	// was found.
	GoMod(path, version string) ([]byte, bool)
	// ZipHash returns the h1: module (zip) hash, and whether it is available.
	// Only the local cache can provide this without downloading the zip.
	ZipHash(path, version string) (string, bool)
	// PackageGoFiles returns the .go source files (keyed by base filename,
	// including tests) of the package at importPath within module
	// modPath@version. ok is false if the package could not be located. This is
	// what gives a clean clone the dependency SOURCES needed to compute the
	// package-import graph (and, in future, full type attribution) without any
	// Go tooling — the proxy implementation downloads and extracts the module
	// zip on demand.
	PackageGoFiles(modPath, version, importPath string) (files map[string][]byte, ok bool)
}

// HTTPGet performs an HTTP GET and returns the body, status code, and error.
// In production this is backed by the CLI's HttpSender over RPC.
type HTTPGet func(url string) (body []byte, status int, err error)

// CacheSource reads module metadata from a local module cache (the value of
// `go env GOMODCACHE`).
func CacheSource(gomodcache string) ModSource {
	return &cacheSource{root: gomodcache, download: filepath.Join(gomodcache, "cache", "download")}
}

type cacheSource struct {
	root     string // GOMODCACHE; extracted module dirs live directly under it
	download string // GOMODCACHE/cache/download
}

func (c *cacheSource) GoMod(path, version string) ([]byte, bool) {
	b, err := readCacheFile(c.download, path, version, ".mod")
	if err != nil {
		return nil, false
	}
	return b, true
}

func (c *cacheSource) ZipHash(path, version string) (string, bool) {
	h, err := readZipHash(c.download, path, version)
	if err != nil {
		return "", false
	}
	return h, true
}

func (c *cacheSource) PackageGoFiles(modPath, version, importPath string) (map[string][]byte, bool) {
	ep, err := module.EscapePath(modPath)
	if err != nil {
		return nil, false
	}
	ev, err := module.EscapeVersion(version)
	if err != nil {
		return nil, false
	}
	rel := strings.TrimPrefix(strings.TrimPrefix(importPath, modPath), "/")
	dir := filepath.Join(c.root, ep+"@"+ev, filepath.FromSlash(rel))
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil, false
	}
	files := map[string][]byte{}
	for _, e := range entries {
		if e.IsDir() || !strings.HasSuffix(e.Name(), ".go") {
			continue
		}
		if b, err := os.ReadFile(filepath.Join(dir, e.Name())); err == nil {
			files[e.Name()] = b
		}
	}
	return files, len(files) > 0
}

// ProxySource fetches module metadata from one or more GOPROXY base URLs using
// the injected get function. goproxy is a GOPROXY-style list (comma/pipe
// separated); "off"/"direct"/"none" entries and the VCS fallback are skipped
// (this source only speaks the proxy protocol). If goproxy is empty,
// https://proxy.golang.org is used.
func ProxySource(goproxy string, get HTTPGet) ModSource {
	var bases []string
	for _, p := range strings.FieldsFunc(goproxy, func(r rune) bool { return r == ',' || r == '|' }) {
		p = strings.TrimSpace(p)
		if p == "" || p == "off" || p == "direct" || p == "none" {
			continue
		}
		bases = append(bases, strings.TrimRight(p, "/"))
	}
	if len(bases) == 0 {
		bases = []string{"https://proxy.golang.org"}
	}
	return &proxySource{bases: bases, get: get, zips: map[string]map[string][]byte{}}
}

type proxySource struct {
	bases []string
	get   HTTPGet
	mu    sync.Mutex
	// zips caches the extracted contents of a module zip, keyed by
	// "modPath@version" -> (full zip entry path -> bytes). A nil value records
	// a failed download so it is not retried.
	zips map[string]map[string][]byte
}

func (p *proxySource) GoMod(path, version string) ([]byte, bool) {
	ep, err := module.EscapePath(path)
	if err != nil {
		return nil, false
	}
	ev, err := module.EscapeVersion(version)
	if err != nil {
		return nil, false
	}
	suffix := "/" + ep + "/@v/" + ev + ".mod"
	for _, base := range p.bases {
		body, status, err := p.get(base + suffix)
		if err == nil && status == 200 && len(body) > 0 {
			return body, true
		}
	}
	return nil, false
}

// ZipHash is unavailable from the proxy without downloading and hashing the
// module zip; go.sum generation is handled separately.
func (p *proxySource) ZipHash(path, version string) (string, bool) {
	return "", false
}

func (p *proxySource) PackageGoFiles(modPath, version, importPath string) (map[string][]byte, bool) {
	entries, ok := p.moduleZip(modPath, version)
	if !ok {
		return nil, false
	}
	rel := strings.TrimPrefix(strings.TrimPrefix(importPath, modPath), "/")
	// Package files live directly under "<modPath>@<version>/<rel>/" — the
	// package is exactly one directory level (no deeper recursion).
	prefix := modPath + "@" + version + "/"
	if rel != "" {
		prefix += rel + "/"
	}
	files := map[string][]byte{}
	for name, content := range entries {
		if !strings.HasPrefix(name, prefix) {
			continue
		}
		tail := name[len(prefix):]
		if strings.Contains(tail, "/") || !strings.HasSuffix(tail, ".go") {
			continue // a deeper subpackage or non-go file
		}
		files[tail] = content
	}
	return files, len(files) > 0
}

// moduleZip downloads (once) and extracts the module zip for modPath@version,
// returning a map of zip-entry path -> contents.
func (p *proxySource) moduleZip(modPath, version string) (map[string][]byte, bool) {
	key := modPath + "@" + version
	p.mu.Lock()
	if cached, seen := p.zips[key]; seen {
		p.mu.Unlock()
		return cached, cached != nil
	}
	p.mu.Unlock()

	entries := p.downloadZip(modPath, version)
	p.mu.Lock()
	p.zips[key] = entries // nil on failure — caches the negative result
	p.mu.Unlock()
	return entries, entries != nil
}

func (p *proxySource) downloadZip(modPath, version string) map[string][]byte {
	ep, err := module.EscapePath(modPath)
	if err != nil {
		return nil
	}
	ev, err := module.EscapeVersion(version)
	if err != nil {
		return nil
	}
	suffix := "/" + ep + "/@v/" + ev + ".zip"
	var raw []byte
	for _, base := range p.bases {
		body, status, err := p.get(base + suffix)
		if err == nil && status == 200 && len(body) > 0 {
			raw = body
			break
		}
	}
	if raw == nil {
		return nil
	}
	zr, err := zip.NewReader(bytes.NewReader(raw), int64(len(raw)))
	if err != nil {
		return nil
	}
	entries := map[string][]byte{}
	for _, f := range zr.File {
		if f.FileInfo().IsDir() || !strings.HasSuffix(f.Name, ".go") {
			continue
		}
		rc, err := f.Open()
		if err != nil {
			continue
		}
		var buf bytes.Buffer
		_, _ = buf.ReadFrom(rc)
		rc.Close()
		entries[f.Name] = buf.Bytes()
	}
	return entries
}

// TieredSource tries each source in order, returning the first hit. Use it to
// prefer the local cache and fall back to the proxy.
func TieredSource(sources ...ModSource) ModSource {
	return &tieredSource{sources: sources}
}

type tieredSource struct{ sources []ModSource }

func (t *tieredSource) GoMod(path, version string) ([]byte, bool) {
	for _, s := range t.sources {
		if b, ok := s.GoMod(path, version); ok {
			return b, true
		}
	}
	return nil, false
}

func (t *tieredSource) ZipHash(path, version string) (string, bool) {
	for _, s := range t.sources {
		if h, ok := s.ZipHash(path, version); ok {
			return h, true
		}
	}
	return "", false
}

func (t *tieredSource) PackageGoFiles(modPath, version, importPath string) (map[string][]byte, bool) {
	for _, s := range t.sources {
		if files, ok := s.PackageGoFiles(modPath, version, importPath); ok {
			return files, true
		}
	}
	return nil, false
}

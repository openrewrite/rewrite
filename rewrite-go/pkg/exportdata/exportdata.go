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

// Package exportdata resolves Go packages from compiler export data a recipe
// module carries with it, for machines that cannot reach a module proxy. See
// doc/recipe-authoring.md: Shipped export data.
package exportdata

import (
	"bytes"
	"fmt"
	"go/importer"
	"go/token"
	"go/types"
	"io"
	"io/fs"
	"strconv"
	"strings"
	"sync"
)

// BlobName is where Importer looks for the blob covering an import path. The
// layout mirrors the path so a directory of blobs reads as the set of packages
// it covers.
func BlobName(importPath string) string {
	return importPath + ".a"
}

// Importer resolves an import path from the first of sets that carries it, and
// otherwise from the toolchain's own search. Shipped blobs win, which is what
// lets a module carry a package the toolchain resolves differently or not at
// all; a blob that is absent or unreadable leaves the path to the next set and
// finally to the toolchain, so export data a newer toolchain rejects costs
// attribution and nothing more.
func Importer(sets ...fs.FS) types.Importer {
	imp := make([]types.Importer, 0, len(sets)+1)
	for _, fsys := range sets {
		if fsys != nil {
			imp = append(imp, shippedOnly(fsys))
		}
	}
	return &chain{imp: append(imp, importer.Default())}
}

// Verify reports the first path fsys does not cover with a readable blob. It
// is where a module that ships export data makes Importer's silent fallback
// loud, by asserting on its own blobs from its own tests.
func Verify(fsys fs.FS, importPaths ...string) error {
	imp := shippedOnly(fsys)
	for _, path := range importPaths {
		if _, err := imp.Import(path); err != nil {
			return fmt.Errorf("export data for %q: %w", path, err)
		}
	}
	return nil
}

// shippedOnly resolves nothing but what fsys carries. A non-nil lookup takes
// go/importer off its default search entirely, which is why Importer chains
// this rather than handing it the whole job.
func shippedOnly(fsys fs.FS) types.Importer {
	return importer.ForCompiler(token.NewFileSet(), "gc", func(path string) (io.ReadCloser, error) {
		blob, err := fs.ReadFile(fsys, BlobName(path))
		if err != nil {
			return nil, err
		}
		return io.NopCloser(bytes.NewReader(blob)), nil
	})
}

// chain is the first importer to resolve a path, in order. The lock lets one
// chain serve concurrent parses, which is how callers get to hold on to what
// its importers have already resolved.
type chain struct {
	mu  sync.Mutex
	imp []types.Importer
}

func (c *chain) Import(path string) (*types.Package, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	var err error
	for _, imp := range c.imp {
		pkg, e := imp.Import(path)
		if e == nil && pkg != nil {
			return pkg, nil
		}
		if e != nil {
			err = e
		}
	}
	if err == nil {
		err = fmt.Errorf("no importer resolved %q", path)
	}
	return nil, err
}

// Pack reduces a gc archive to the export data an importer reads, leaving the
// object code behind. The result is still an archive because that is the only
// framing go/importer accepts.
func Pack(archive []byte) ([]byte, error) {
	def, err := pkgdef(archive)
	if err != nil {
		return nil, err
	}
	var out bytes.Buffer
	out.WriteString(archiveMagic)
	fmt.Fprintf(&out, "%-16s%-12s%-6s%-6s%-8s%-10d`\n", pkgdefMember, "0", "0", "0", "644", len(def))
	out.Write(def)
	// ar member bodies are padded to an even length.
	if len(def)%2 == 1 {
		out.WriteByte('\n')
	}
	return out.Bytes(), nil
}

const (
	archiveMagic = "!<arch>\n"
	pkgdefMember = "__.PKGDEF"
	headerLen    = 60
	sizeOffset   = 48
	sizeEnd      = 58
)

// pkgdef returns the export-data member of an ar archive.
func pkgdef(archive []byte) ([]byte, error) {
	if !bytes.HasPrefix(archive, []byte(archiveMagic)) {
		return nil, fmt.Errorf("not a gc archive")
	}
	for at := len(archiveMagic); at+headerLen <= len(archive); {
		header := archive[at : at+headerLen]
		size, err := strconv.Atoi(strings.TrimSpace(string(header[sizeOffset:sizeEnd])))
		if err != nil || size < 0 {
			return nil, fmt.Errorf("malformed archive member header")
		}
		body := at + headerLen
		if body+size > len(archive) {
			return nil, fmt.Errorf("archive member runs past end of file")
		}
		if strings.TrimSpace(string(header[:16])) == pkgdefMember {
			return archive[body : body+size], nil
		}
		at = body + size
		if size%2 == 1 {
			at++
		}
	}
	return nil, fmt.Errorf("archive has no %s member", pkgdefMember)
}

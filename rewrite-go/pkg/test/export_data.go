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

package test

import (
	"bytes"
	"io/fs"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
	"testing/fstest"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/exportdata"
)

// ShippedPath and ShippedPathB are the fixture's two packages. Nothing
// resolves them but a blob: they are not in GOROOT and never published.
const (
	ShippedPath  = "example.com/shipped/mathx"
	ShippedPathB = "example.com/shipped/strx"
	// ShippedPathV2 supersedes ShippedPath under the same qualifier, the shape
	// a major-version package move takes.
	ShippedPathV2 = "example.com/shipped/mathx/v2"
)

const shippedSource = `package mathx

import "time"

func Clamp(v, lo, hi int) int { return v }

type Range struct{ Lo, Hi int }

func NewRange(lo, hi int) *Range { return &Range{lo, hi} }

func (r *Range) Contains(v int) bool { return true }

func Deadline(d time.Duration) time.Time { return time.Now() }

func Map[T any](v T) T { return v }
`

const shippedSourceB = `package strx

func Repeat(s string, n int) string { return s }
`

const shippedSourceV2 = `package mathx

func Clamp(v, lo, hi int) int { return v }
`

// ShippedExportData compiles a throwaway module and returns its export data
// as a recipe module would carry it. Building the fixture keeps it matched to
// whatever toolchain runs the tests.
func ShippedExportData(t *testing.T) fs.FS {
	t.Helper()
	return ShippedExportDataFor(t, ShippedPath)
}

// ShippedExportDataFor covers one of the fixture's packages, so a test can
// spread them over several fs.FS the way separately generated packages are.
func ShippedExportDataFor(t *testing.T, importPath string) fs.FS {
	t.Helper()
	return fstest.MapFS{exportdata.BlobName(importPath): {Data: ShippedArchive(t, true, importPath)}}
}

// ShippedModule writes the fixture's sources and returns its module root.
func ShippedModule(t *testing.T) string {
	t.Helper()
	dir := t.TempDir()
	require.NoError(t, os.WriteFile(filepath.Join(dir, "go.mod"),
		[]byte("module example.com/shipped\n\ngo 1.25\n"), 0o644))
	for dirName, src := range map[string]string{
		"mathx":    shippedSource,
		"strx":     shippedSourceB,
		"mathx/v2": shippedSourceV2,
	} {
		require.NoError(t, os.MkdirAll(filepath.Join(dir, dirName), 0o755))
		name := filepath.Base(dirName) + ".go"
		require.NoError(t, os.WriteFile(filepath.Join(dir, dirName, name), []byte(src), 0o644))
	}
	return dir
}

// ShippedArchive builds one of the fixture's packages and returns it packed as
// Importer reads it, or raw as `go build` left it.
func ShippedArchive(t *testing.T, packed bool, importPath string) []byte {
	t.Helper()
	cmd := exec.Command("go", "list", "-export", "-f", "{{.Export}}", importPath)
	cmd.Dir = ShippedModule(t)
	var stderr bytes.Buffer
	cmd.Stderr = &stderr
	out, err := cmd.Output()
	require.NoError(t, err, "go list -export: %s", stderr.String())

	archive, err := os.ReadFile(strings.TrimSpace(string(out)))
	require.NoError(t, err)
	if !packed {
		return archive
	}
	blob, err := exportdata.Pack(archive)
	require.NoError(t, err)
	return blob
}

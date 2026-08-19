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

package main

import (
	"os"
	"os/exec"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/exportdata"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

func generateFixture(t *testing.T) string {
	t.Helper()
	out := filepath.Join(t.TempDir(), "exportdata")
	require.NoError(t, generate(test.ShippedModule(t), out, []string{test.ShippedPath}, ""))
	return out
}

func TestGenerateWritesABlobImporterCanRead(t *testing.T) {
	out := generateFixture(t)

	blob := filepath.Join(out, blobDir, exportdata.BlobName(test.ShippedPath))
	require.FileExists(t, blob)

	require.NoError(t, exportdata.Verify(os.DirFS(filepath.Join(out, blobDir)), test.ShippedPath))
}

func TestGenerateWritesACompilingEmbedShim(t *testing.T) {
	out := generateFixture(t)

	shim, err := os.ReadFile(filepath.Join(out, shimFile))
	require.NoError(t, err)
	assert.Contains(t, string(shim), "package exportdata")
	assert.Contains(t, string(shim), "//go:embed "+blobDir)
	assert.Contains(t, string(shim), test.ShippedPath)

	cmd := exec.Command("gofmt", "-l", filepath.Join(out, shimFile))
	formatted, err := cmd.CombinedOutput()
	require.NoError(t, err)
	assert.Empty(t, string(formatted), "generated shim should already be gofmt-clean")
}

func TestGeneratedShimCompilesAndServesTheBlobs(t *testing.T) {
	mod := t.TempDir()
	require.NoError(t, os.WriteFile(filepath.Join(mod, "go.mod"),
		[]byte("module example.com/consumer\n\ngo 1.25\n"), 0o644))
	require.NoError(t, generate(test.ShippedModule(t), filepath.Join(mod, "exportdata"), []string{test.ShippedPath}, ""))

	consumer := `package main

import (
	"fmt"
	"io/fs"

	"example.com/consumer/exportdata"
)

func main() {
	for _, p := range exportdata.Paths {
		b, err := fs.ReadFile(exportdata.FS, p+".a")
		fmt.Println(p, len(b), err)
	}
}
`
	require.NoError(t, os.WriteFile(filepath.Join(mod, "main.go"), []byte(consumer), 0o644))

	cmd := exec.Command("go", "run", ".")
	cmd.Dir = mod
	out, err := cmd.CombinedOutput()
	require.NoError(t, err, "go run: %s", out)
	assert.Regexp(t, test.ShippedPath+` \d{4,} <nil>`, string(out),
		"generated FS should serve a non-trivial blob at the import path")
}

func TestGeneratePackageNameFollowsOutputDir(t *testing.T) {
	out := filepath.Join(t.TempDir(), "jsonv2types")
	require.NoError(t, generate(test.ShippedModule(t), out, []string{test.ShippedPath}, ""))

	shim, err := os.ReadFile(filepath.Join(out, shimFile))
	require.NoError(t, err)
	assert.Contains(t, string(shim), "package jsonv2types")
}

func TestGenerateRejectsADirectoryNameThatIsNotAPackageName(t *testing.T) {
	out := filepath.Join(t.TempDir(), "export-data")
	err := generate(test.ShippedModule(t), out, []string{test.ShippedPath}, "")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "-pkg")

	require.NoError(t, generate(test.ShippedModule(t), out, []string{test.ShippedPath}, "exportdata"))
	shim, err := os.ReadFile(filepath.Join(out, shimFile))
	require.NoError(t, err)
	assert.Contains(t, string(shim), "package exportdata")
}

// `unsafe` builds but has no export data, so `go list` succeeds with nothing
// to read.
func TestGenerateReportsAPathWithNoExportData(t *testing.T) {
	err := generate(test.ShippedModule(t), filepath.Join(t.TempDir(), "exportdata"),
		[]string{"unsafe"}, "")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "no export data")
}

func TestGenerateReportsAPathTheModuleCannotBuild(t *testing.T) {
	err := generate(test.ShippedModule(t), filepath.Join(t.TempDir(), "exportdata"),
		[]string{"example.com/shipped/absent"}, "")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "example.com/shipped/absent")
}

// goWrapper puts a `go` on PATH that writes to stderr before delegating, the
// way the real one does while downloading a module.
func goWrapper(t *testing.T) {
	t.Helper()
	real, err := exec.LookPath("go")
	require.NoError(t, err)
	dir := t.TempDir()
	script := "#!/bin/sh\necho 'go: downloading example.com/fake v1.0.0' >&2\nexec " + real + " \"$@\"\n"
	require.NoError(t, os.WriteFile(filepath.Join(dir, "go"), []byte(script), 0o755))
	t.Setenv("PATH", dir+string(os.PathListSeparator)+os.Getenv("PATH"))
}

func TestGenerateIgnoresProgressOnStderr(t *testing.T) {
	mod := test.ShippedModule(t)
	goWrapper(t)

	out := filepath.Join(t.TempDir(), "exportdata")
	require.NoError(t, generate(mod, out, []string{test.ShippedPath}, ""))
	require.NoError(t, exportdata.Verify(os.DirFS(filepath.Join(out, blobDir)), test.ShippedPath))
}

func TestGenerateDropsBlobsNoLongerAskedFor(t *testing.T) {
	mod := test.ShippedModule(t)
	out := filepath.Join(t.TempDir(), "exportdata")
	require.NoError(t, generate(mod, out, []string{test.ShippedPath, test.ShippedPathB}, ""))
	require.FileExists(t, filepath.Join(out, blobDir, exportdata.BlobName(test.ShippedPathB)))

	require.NoError(t, generate(mod, out, []string{test.ShippedPath}, ""))
	assert.NoFileExists(t, filepath.Join(out, blobDir, exportdata.BlobName(test.ShippedPathB)))
	require.FileExists(t, filepath.Join(out, blobDir, exportdata.BlobName(test.ShippedPath)))
}

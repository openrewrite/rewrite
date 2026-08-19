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

package exportdata_test

import (
	"io/fs"
	"testing"
	"testing/fstest"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/exportdata"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

const shippedPath = test.ShippedPath

func packed(t *testing.T) []byte {
	t.Helper()
	return test.ShippedArchive(t, true, test.ShippedPath)
}

func shippedFS(t *testing.T) fs.FS {
	t.Helper()
	return test.ShippedExportData(t)
}

func TestPackDropsObjectCodeAndKeepsMeaning(t *testing.T) {
	archive := test.ShippedArchive(t, false, test.ShippedPath)
	blob, err := exportdata.Pack(archive)
	require.NoError(t, err)
	// Object code dominates a real dependency's archive but not a four-symbol
	// fixture's, so the contract here is smaller, not a ratio.
	assert.Less(t, len(blob), len(archive))

	fromArchive, err := exportdata.Importer(
		fstest.MapFS{exportdata.BlobName(shippedPath): {Data: archive}}).Import(shippedPath)
	require.NoError(t, err)
	fromBlob, err := exportdata.Importer(
		fstest.MapFS{exportdata.BlobName(shippedPath): {Data: blob}}).Import(shippedPath)
	require.NoError(t, err)
	assert.ElementsMatch(t, fromArchive.Scope().Names(), fromBlob.Scope().Names())
}

func TestPackRejectsNonArchive(t *testing.T) {
	_, err := exportdata.Pack([]byte("hello world"))
	assert.Error(t, err)
}

func TestImporterResolvesShippedPackage(t *testing.T) {
	pkg, err := exportdata.Importer(shippedFS(t)).Import(shippedPath)
	require.NoError(t, err)
	require.NotNil(t, pkg)
	assert.Equal(t, "mathx", pkg.Name())
	assert.ElementsMatch(t, []string{"Clamp", "Range", "NewRange", "Deadline", "Map"}, pkg.Scope().Names())
}

func TestImporterStillResolvesStdlib(t *testing.T) {
	pkg, err := exportdata.Importer(shippedFS(t)).Import("strings")
	require.NoError(t, err)
	require.NotNil(t, pkg)
	assert.Contains(t, pkg.Scope().Names(), "Contains")
}

func TestImporterFailsOverUnreadableBlob(t *testing.T) {
	for _, tc := range []struct {
		name string
		data []byte
	}{
		{"garbage", []byte("not an archive")},
		{"empty", nil},
		{"truncated", packed(t)[:200]},
	} {
		t.Run(tc.name, func(t *testing.T) {
			imp := exportdata.Importer(fstest.MapFS{exportdata.BlobName(shippedPath): {Data: tc.data}})
			_, err := imp.Import(shippedPath)
			assert.Error(t, err, "unreadable blob must report an error, not panic")

			pkg, err := imp.Import("strings")
			require.NoError(t, err, "one bad blob must not take the stdlib down with it")
			assert.NotNil(t, pkg)
		})
	}
}

func TestImporterWithoutBlobDefersToDefault(t *testing.T) {
	_, err := exportdata.Importer(fstest.MapFS{}).Import(shippedPath)
	assert.Error(t, err)
}

func TestBlobNameMirrorsImportPath(t *testing.T) {
	assert.Equal(t, "encoding/json/jsontext.a", exportdata.BlobName("encoding/json/jsontext"))
	assert.Equal(t, "github.com/pkg/errors.a", exportdata.BlobName("github.com/pkg/errors"))
}

func TestVerify(t *testing.T) {
	assert.NoError(t, exportdata.Verify(shippedFS(t), shippedPath))
	assert.Error(t, exportdata.Verify(shippedFS(t), "example.com/shipped/absent"),
		"a path with no blob is a packaging mistake, not a fallback")
	assert.Error(t, exportdata.Verify(
		fstest.MapFS{exportdata.BlobName(shippedPath): {Data: []byte("stale")}}, shippedPath))
}

func TestImporterSkipsNilSets(t *testing.T) {
	imp := exportdata.Importer(nil, shippedFS(t), nil)
	pkg, err := imp.Import(shippedPath)
	require.NoError(t, err)
	assert.NotNil(t, pkg)

	_, err = exportdata.Importer(nil).Import("example.com/shipped/absent")
	assert.Error(t, err)
}

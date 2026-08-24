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
	"fmt"
	"go/types"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// recordingFromImporter is an ImporterFrom, as importer.Default() is, so a
// wrapper that drops to plain Import shows up here as a lost srcDir.
type recordingFromImporter struct{ srcDir string }

func (r *recordingFromImporter) Import(path string) (*types.Package, error) {
	return r.ImportFrom(path, "", 0)
}

func (r *recordingFromImporter) ImportFrom(path, srcDir string, _ types.ImportMode) (*types.Package, error) {
	r.srcDir = srcDir
	return types.NewPackage(path, path), nil
}

func TestResilientImporterKeepsTheSourceDirectory(t *testing.T) {
	rec := &recordingFromImporter{}
	res := &resilientImporter{delegate: rec}

	from, ok := types.Importer(res).(types.ImporterFrom)
	require.True(t, ok, "wrapping an ImporterFrom must stay an ImporterFrom")
	_, err := from.ImportFrom("example.com/lib", "/src/app", 0)
	require.NoError(t, err)
	assert.Equal(t, "/src/app", rec.srcDir, "srcDir is what resolves vendored and relative imports")
}

type panickingImporter struct{}

func (panickingImporter) Import(path string) (*types.Package, error) {
	panic(fmt.Errorf("cannot decode %q, export data version 4 is greater than maximum supported version 2", path))
}

// One dependency's type table spans many packages, so a stdlib read the
// toolchain cannot decode must cost the package that named it and no more.
func TestReferenceImporterReportsAnUndecodableStdlibRead(t *testing.T) {
	ri := newReferenceImporter(nil)
	res, ok := ri.def.(*resilientImporter)
	require.True(t, ok, "the stdlib fallback must be wrapped where it is built")
	res.delegate = panickingImporter{}

	pkg, err := ri.Import("strings")
	require.Error(t, err, "an undecodable read must report an error, not panic")
	assert.Nil(t, pkg)
	assert.Contains(t, err.Error(), "export data version")
}

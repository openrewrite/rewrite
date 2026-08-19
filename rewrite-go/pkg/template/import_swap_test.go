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

package template_test

import (
	"fmt"
	"io/fs"
	"strings"
	"testing"
	"testing/fstest"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/exportdata"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// bothShipped covers the superseded path and the one replacing it.
func bothShipped(t *testing.T) fs.FS {
	t.Helper()
	merged := fstest.MapFS{}
	for _, path := range []string{test.ShippedPath, test.ShippedPathV2} {
		blob := test.ShippedArchive(t, true, path)
		merged[exportdata.BlobName(path)] = &fstest.MapFile{Data: blob}
	}
	return merged
}

// applyRecipe parses src against fsys and runs r over it, the way a recipe run
// reaches a source file already attributed to the module it belongs to.
func applyRecipe(t *testing.T, r recipe.Recipe, fsys fs.FS, src string) string {
	t.Helper()
	p := parser.NewGoParser()
	p.Importer = exportdata.Importer(fsys)
	cu, err := p.Parse("main.go", strings.TrimSpace(src)+"\n")
	require.NoError(t, err)

	var tree java.Tree = cu
	ed := r.Editor()
	ctx := recipe.NewExecutionContext()
	if out := ed.Visit(tree, ctx); out != nil {
		tree = out
	}
	tree = visitor.DrainAfterVisits(ed, tree, ctx)
	return printer.Print(tree)
}

func TestSupersededImportIsRemovedWhenOnlyThePathMoves(t *testing.T) {
	fsys := bothShipped(t)
	v := template.Expr("v")

	r := template.NewRecipe(
		template.RecipeName("test.SwapMathxToV2"),
		template.WithDisplayName("Swap mathx for mathx/v2"),
		template.WithCaptures(v),
		template.WithBefore(fmt.Sprintf(`mathx.Clamp(%s, 0, 10)`, v), template.Imports(test.ShippedPath)),
		template.WithAfter(fmt.Sprintf(`mathx.Clamp(%s, 0, 100)`, v),
			template.Imports(test.ShippedPathV2),
			template.ExportData(fsys),
			template.SourceImports(test.ShippedPathV2)),
	)

	out := applyRecipe(t, r, fsys, `
package main

import "example.com/shipped/mathx"

func f(x int) int {
	return mathx.Clamp(x, 0, 10)
}
`)

	assert.Contains(t, out, test.ShippedPathV2)
	assert.NotContains(t, out, `"example.com/shipped/mathx"`,
		"two imports binding mathx would not compile")
}

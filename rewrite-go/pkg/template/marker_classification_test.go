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

package template

import (
	"go/ast"
	goparser "go/parser"
	gotoken "go/token"
	"sort"
	"testing"

	"github.com/stretchr/testify/require"
)

// formattingMarkers move whitespace or punctuation. Two nodes differing only
// in one of these are the same source written out differently.
var formattingMarkers = map[string]bool{
	"Semicolon": true, "TrailingComma": true, "GroupedSpec": true,
	"GroupedImport": true, "ImportBlock": true, "StructTagQuote": true,
	"ChanDirMarker": true, "ImplicitForClauses": true,
}

// printerMarkers reads back the golang markers the printer consults, so the
// split between what a match reads and what it ignores is derived from what
// reaches the output rather than restated by hand.
func printerMarkers(t *testing.T) []string {
	t.Helper()
	file, err := goparser.ParseFile(gotoken.NewFileSet(), "../printer/go_printer.go", nil, 0)
	require.NoError(t, err)

	seen := map[string]bool{}
	ast.Inspect(file, func(n ast.Node) bool {
		index, ok := n.(*ast.IndexExpr)
		if !ok {
			return true
		}
		fn, ok := index.X.(*ast.SelectorExpr)
		if !ok || fn.Sel.Name != "FindMarker" && fn.Sel.Name != "HasMarker" {
			return true
		}
		if sel, ok := index.Index.(*ast.SelectorExpr); ok {
			if pkg, ok := sel.X.(*ast.Ident); ok && pkg.Name == "golang" {
				seen[sel.Sel.Name] = true
			}
		}
		return true
	})

	var names []string
	for name := range seen {
		names = append(names, name)
	}
	sort.Strings(names)
	return names
}

func TestMarkerClassification(t *testing.T) {
	found := printerMarkers(t)
	require.NotEmpty(t, found, "no golang markers read out of the printer")

	for _, name := range found {
		semantic := semanticMarkerNames()[name]
		formatting := formattingMarkers[name]
		require.False(t, semantic && formatting, "marker %q is in both lists", name)
		require.True(t, semantic || formatting,
			"the printer reads marker %q but nothing says whether a match should. "+
				"A marker that changes the keywords or operators printed belongs in "+
				"semanticMarkers; one that only moves whitespace belongs in "+
				"formattingMarkers.", name)
	}

	// StructTag reaches the output through the struct-field printer rather
	// than a FindMarker call, so its classification is asserted directly.
	require.True(t, semanticMarkerNames()["StructTag"])
}

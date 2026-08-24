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

// ignoredMarkers carry layout — where the whitespace and punctuation go — or
// describe the file's project rather than its meaning. Two nodes differing
// only in one of these are the same source.
var ignoredMarkers = map[string]bool{
	"Semicolon": true, "TrailingComma": true, "GroupedSpec": true,
	"GroupedImport": true, "ImportBlock": true, "StructTagQuote": true,
	"ChanDirMarker": true, "ImplicitForClauses": true, "TypeSwitchGuard": true,
	"GoProject": true, "GoResolutionResult": true,
	// Recipe bookkeeping the java tree carries: what a run found or changed,
	// never what the source says.
	"GenericMarker": true, "RecipesThatMadeChanges": true, "SearchResult": true,
	"SearchResultMarker": true, "Markup": true, "RecipeThatMadeChanges": true,
	"ParseExceptionResult": true,
}

// declaredMarkers reads back every marker the golang tree package declares, so
// a marker cannot be added without this test asking what a match should do
// with it. A marker is a struct with an ID method returning a uuid.UUID.
func declaredMarkers(t *testing.T) []string {
	t.Helper()
	structs := map[string]bool{}
	withID := map[string]bool{}
	for _, dir := range []string{"../tree/golang", "../tree/java"} {
		collectMarkers(t, dir, structs, withID)
	}

	var names []string
	for name := range structs {
		if withID[name] {
			names = append(names, name)
		}
	}
	sort.Strings(names)
	return names
}

func collectMarkers(t *testing.T, dir string, structs, withID map[string]bool) {
	t.Helper()
	pkgs, err := goparser.ParseDir(gotoken.NewFileSet(), dir, nil, 0)
	require.NoError(t, err)
	for _, pkg := range pkgs {
		for _, file := range pkg.Files {
			for _, decl := range file.Decls {
				switch d := decl.(type) {
				case *ast.GenDecl:
					for _, spec := range d.Specs {
						ts, ok := spec.(*ast.TypeSpec)
						if !ok {
							continue
						}
						if _, ok := ts.Type.(*ast.StructType); ok && ts.Name.IsExported() {
							structs[ts.Name.Name] = true
						}
					}
				case *ast.FuncDecl:
					if d.Name.Name != "ID" || d.Recv == nil || len(d.Recv.List) != 1 {
						continue
					}
					if ident, ok := d.Recv.List[0].Type.(*ast.Ident); ok {
						withID[ident.Name] = true
					}
				}
			}
		}
	}
}

func TestEveryMarkerIsClassified(t *testing.T) {
	found := declaredMarkers(t)
	require.NotEmpty(t, found, "no markers read out of the golang tree package")

	semantic := semanticMarkerNames()
	for _, name := range found {
		require.False(t, semantic[name] && ignoredMarkers[name], "marker %q is in both lists", name)
		require.True(t, semantic[name] || ignoredMarkers[name],
			"nothing says whether a match should read marker %q. A marker that "+
				"changes what the source says belongs in semanticMarkers; one that "+
				"carries layout or project metadata belongs in ignoredMarkers.", name)
	}

	for name := range semantic {
		require.Contains(t, found, name, "semanticMarkers names %q, which is not a marker", name)
	}
}

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
	"os"
	"path/filepath"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// vendorScaffold builds a vendor directory layout under root from a map of
// relative path → file content. Used by the T2 corpus.
func vendorScaffold(t *testing.T, root string, files map[string]string) {
	t.Helper()
	for rel, content := range files {
		full := filepath.Join(root, rel)
		if err := os.MkdirAll(filepath.Dir(full), 0o755); err != nil {
			t.Fatalf("mkdir %s: %v", full, err)
		}
		if err := os.WriteFile(full, []byte(content), 0o644); err != nil {
			t.Fatalf("write %s: %v", full, err)
		}
	}
}

// parseInProject sets up a ProjectImporter with the given project root +
// require/replace metadata, then parses src as the file at sourcePath.
// Returns the parsed compilation unit so tests can assert on resolved
// types via the ExpectType helpers.
func parseInProject(t *testing.T, root string, modulePath string, requires []string, replaces map[string]string, sourcePath, src string) *tree.CompilationUnit {
	t.Helper()
	pi := parser.NewProjectImporter(modulePath, nil)
	pi.SetProjectRoot(root)
	for _, r := range requires {
		pi.AddRequire(r)
	}
	for old, newPath := range replaces {
		pi.AddReplace(old, newPath, "")
	}
	pi.AddSource(sourcePath, src)
	p := parser.NewGoParser()
	p.Importer = pi
	cu, err := p.Parse(sourcePath, src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	return cu
}

// Case 1: vendor happy path — vendored package's func type-resolves.
func TestVendorWalker_HappyPath(t *testing.T) {
	root := t.TempDir()
	vendorScaffold(t, root, map[string]string{
		"vendor/github.com/x/y/y.go": "package y\n\nfunc Hello() string { return \"hi\" }\n",
	})
	cu := parseInProject(t, root,
		"example.com/foo",
		[]string{"github.com/x/y"},
		nil,
		"main.go",
		"package main\n\nimport \"github.com/x/y\"\n\nfunc main() { _ = y.Hello() }\n",
	)
	ExpectMethodType(t, cu, "Hello", "github.com/x/y")
}

// Case 2: replace → local path. `replace foo => ./local/foo` walks the
// local dir relative to the project root.
func TestVendorWalker_ReplaceLocal(t *testing.T) {
	root := t.TempDir()
	vendorScaffold(t, root, map[string]string{
		"local/y/y.go": "package y\n\nfunc Hello() string { return \"hi\" }\n",
	})
	cu := parseInProject(t, root,
		"example.com/foo",
		[]string{"github.com/x/y"},
		map[string]string{"github.com/x/y": "./local/y"},
		"main.go",
		"package main\n\nimport \"github.com/x/y\"\n\nfunc main() { _ = y.Hello() }\n",
	)
	ExpectMethodType(t, cu, "Hello", "github.com/x/y")
}

// Case 3: replace → other module path. `replace foo => bar` walks
// `vendor/bar/` instead of `vendor/foo/`.
func TestVendorWalker_ReplaceModule(t *testing.T) {
	root := t.TempDir()
	vendorScaffold(t, root, map[string]string{
		"vendor/github.com/forked/y/y.go": "package y\n\nfunc Hello() string { return \"forked\" }\n",
	})
	cu := parseInProject(t, root,
		"example.com/foo",
		[]string{"github.com/x/y"},
		map[string]string{"github.com/x/y": "github.com/forked/y"},
		"main.go",
		"package main\n\nimport \"github.com/x/y\"\n\nfunc main() { _ = y.Hello() }\n",
	)
	// The package's import path is still github.com/x/y — Go's importer
	// is told that's the path the user wrote. Method resolves on it.
	ExpectMethodType(t, cu, "Hello", "github.com/x/y")
}

// Case 4: aliased third-party import. `import yy "github.com/x/y"` —
// the alias should still resolve because Go's parser handles aliasing
// independently of the package's actual name.
func TestVendorWalker_AliasedImport(t *testing.T) {
	root := t.TempDir()
	vendorScaffold(t, root, map[string]string{
		"vendor/github.com/x/y/y.go": "package y\n\nfunc Hello() string { return \"hi\" }\n",
	})
	cu := parseInProject(t, root,
		"example.com/foo",
		[]string{"github.com/x/y"},
		nil,
		"main.go",
		"package main\n\nimport yy \"github.com/x/y\"\n\nfunc main() { _ = yy.Hello() }\n",
	)
	ExpectMethodType(t, cu, "Hello", "github.com/x/y")
}

// Case 5: multi-level transitive third-party. Vendored A imports
// vendored B; the importer must recursively walk vendor for B too.
func TestVendorWalker_MultiLevelTransitive(t *testing.T) {
	root := t.TempDir()
	vendorScaffold(t, root, map[string]string{
		"vendor/github.com/a/a/a.go": "package a\n\nimport \"github.com/b/b\"\n\nfunc Use() string { return b.World() }\n",
		"vendor/github.com/b/b/b.go": "package b\n\nfunc World() string { return \"world\" }\n",
	})
	cu := parseInProject(t, root,
		"example.com/foo",
		[]string{"github.com/a/a", "github.com/b/b"},
		nil,
		"main.go",
		"package main\n\nimport \"github.com/a/a\"\n\nfunc main() { _ = a.Use() }\n",
	)
	ExpectMethodType(t, cu, "Use", "github.com/a/a")
}

// Case 6: vendor parse error → fallback to stub. Per C4 directive: a
// broken vendored file logs + falls back to the stub tier so the parent
// parse still succeeds.
func TestVendorWalker_ParseErrorFallsBack(t *testing.T) {
	root := t.TempDir()
	vendorScaffold(t, root, map[string]string{
		// Intentionally malformed: missing function body close brace.
		"vendor/github.com/x/y/y.go": "package y\n\nfunc Broken() string { return\n",
	})
	cu := parseInProject(t, root,
		"example.com/foo",
		[]string{"github.com/x/y"},
		nil,
		"main.go",
		"package main\n\nimport \"github.com/x/y\"\n\nvar _ = y\n",
	)
	// The parent parse must succeed (stub fallback). The `y` package
	// alias should be a FullyQualified type with the import path as FQN.
	ExpectType(t, cu, "y", "github.com/x/y")
}

// Case 7: missing vendor → fallback to stub. No vendor dir exists for
// the imported package; importer falls through to the stub tier so the
// parent parse still succeeds and the package alias resolves.
func TestVendorWalker_MissingVendorFallsBack(t *testing.T) {
	root := t.TempDir()
	cu := parseInProject(t, root,
		"example.com/foo",
		[]string{"github.com/x/y"},
		nil,
		"main.go",
		"package main\n\nimport \"github.com/x/y\"\n\nvar _ = y\n",
	)
	ExpectType(t, cu, "y", "github.com/x/y")
}

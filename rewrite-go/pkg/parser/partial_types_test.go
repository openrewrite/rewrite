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

package parser_test

import (
	"fmt"
	"go/importer"
	"go/types"
	"os"
	"runtime"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// hostileImporter fails one import path — by panicking, as a toolchain too old
// to decode the export data does, or by returning an error — and resolves the rest.
type hostileImporter struct {
	path     string
	panics   bool
	delegate types.Importer
}

func (h hostileImporter) Import(path string) (*types.Package, error) {
	if path == h.path {
		if h.panics {
			panic(fmt.Errorf("cannot decode %q, export data version 4 is greater than maximum supported version 2", path))
		}
		return nil, fmt.Errorf("cannot find package %q", path)
	}
	return h.delegate.Import(path)
}

const twoImports = `package p

import (
	"fmt"
	"strings"
)

type Greeter struct{ Name string }

func (g Greeter) Greet() string {
	return fmt.Sprintf("hi %s", strings.ToUpper(g.Name))
}
`

// parseWith parses src as a whole package resolved through imp, which may be
// nil for a parser that resolves nothing.
func parseWith(t *testing.T, imp types.Importer, src string) *golang.CompilationUnit {
	t.Helper()
	gp := parser.NewGoParser()
	if imp != nil {
		gp.Importer = imp
	}
	cus, err := gp.ParsePackage([]parser.FileInput{{Path: "p.go", Content: src}})
	require.NoError(t, err)
	require.Len(t, cus, 1)
	return cus[0]
}

func TestUndecodableImportCostsOnlyThatImport(t *testing.T) {
	cu := parseWith(t, hostileImporter{path: "strings", panics: true, delegate: importer.Default()}, twoImports)

	m := java.FindMarker[golang.PartialTypeAttribution](cu.Markers)
	require.NotNil(t, m, "a package that lost an import must say so on the tree")
	assert.Contains(t, m.Reason, "strings")
	assert.Contains(t, m.Reason, "export data version 4 is greater than maximum supported version 2",
		"the version mismatch is the whole diagnostic; compacting the cause must not drop it")

	// The rest of the package still attributes: without recovering the import
	// panic, conf.Check abandons the whole file and Greeter has no type.
	assert.NotNil(t, findType(t, cu, "Greeter"), "unrelated declarations must keep their types")
}

func TestUnresolvableImportIsRecorded(t *testing.T) {
	cu := parseWith(t, hostileImporter{path: "strings", delegate: importer.Default()}, twoImports)

	m := java.FindMarker[golang.PartialTypeAttribution](cu.Markers)
	require.NotNil(t, m)
	assert.Contains(t, m.Reason, "strings")
}

func TestStubbedRequireIsMarked(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/app", nil)
	pi.AddRequire("github.com/nowhere/lib")
	cu := parseWith(t, pi, `package app

import "github.com/nowhere/lib"

func F() { lib.Do() }
`)

	m := java.FindMarker[golang.PartialTypeAttribution](cu.Markers)
	require.NotNil(t, m, "a stubbed dependency leaves every one of its symbols missing")
	assert.Contains(t, m.Reason, "github.com/nowhere/lib")
	assert.Contains(t, m.Reason, "stub", "the reason separates a stub from an import that failed outright")
}

func TestPackageWithoutStubbedRequiresIsUnmarked(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/app", nil)
	pi.AddRequire("github.com/nowhere/lib")
	cu := parseWith(t, pi, `package app

func F() int { return 1 }
`)
	assert.Nil(t, java.FindMarker[golang.PartialTypeAttribution](cu.Markers),
		"a registered require that the package never imports costs it nothing")
}

func TestReasonIsIndependentOfTheHostItParsedOn(t *testing.T) {
	// go/importer reports a miss by listing every directory it searched.
	cu := parseWith(t, nil, `package app

import "example.com/definitely/not/here"

func F() { here.Do() }
`)
	m := java.FindMarker[golang.PartialTypeAttribution](cu.Markers)
	require.NotNil(t, m)

	// The reason is serialized into the LST and crosses RPC, so two machines
	// parsing the same sources have to produce the same tree.
	assert.NotContains(t, m.Reason, "\n", "a reason spanning lines carries the importer's search path")
	assert.NotContains(t, m.Reason, runtime.GOROOT())
	if home, err := os.UserHomeDir(); err == nil && home != "" {
		assert.NotContains(t, m.Reason, home)
	}
}

func TestBlankImportCostsNoAttribution(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/app", nil)
	pi.AddRequire("github.com/lib/pq")
	cu := parseWith(t, pi, `package app

import _ "github.com/lib/pq"

func F() int { return 1 }
`)
	assert.Nil(t, java.FindMarker[golang.PartialTypeAttribution](cu.Markers),
		"a blank import names no symbol, so a stub in its place costs the file nothing")
}

func TestStubReachedThroughASiblingPackageIsMarked(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/app", nil)
	pi.AddRequire("github.com/nowhere/lib")
	pi.AddSource("b/b.go", `package b

import "github.com/nowhere/lib"

func Make() lib.T { return lib.New() }
`)
	cu := parseWith(t, pi, `package app

import "example.com/app/b"

func F() { _ = b.Make() }
`)

	m := java.FindMarker[golang.PartialTypeAttribution](cu.Markers)
	require.NotNil(t, m, "a type this package uses is missing, whichever import lost it")
	assert.Contains(t, m.Reason, "github.com/nowhere/lib")
	assert.Contains(t, m.Reason, "example.com/app/b", "the reason names the import the stub was reached through")
}

func TestFullyAttributedPackageIsUnmarked(t *testing.T) {
	cu := parseWith(t, importer.Default(), twoImports)
	assert.Nil(t, java.FindMarker[golang.PartialTypeAttribution](cu.Markers),
		"a package that type-checked completely must be distinguishable from one that did not")
}

// findType returns the type attributed to the named `type` declaration.
func findType(t *testing.T, cu *golang.CompilationUnit, name string) java.JavaType {
	t.Helper()
	for _, rp := range cu.Statements {
		if td, ok := rp.Element.(*golang.TypeDecl); ok && td.Name != nil && td.Name.Name == name {
			return td.Name.Type
		}
	}
	return nil
}

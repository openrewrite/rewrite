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
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// reportInternalImports reports the imports of the module's own packages, and
// the module path on go.mod, trusting its scanner to find go.mod unless checked.
type reportInternalImports struct {
	recipe.ScanningBase
	checked bool
}

type modulePath struct {
	module *string
}

func (r *reportInternalImports) Name() string {
	return "org.openrewrite.golang.test.ReportInternalImports"
}
func (r *reportInternalImports) DisplayName() string { return "Report internal imports" }
func (r *reportInternalImports) Description() string {
	return "Reports the imports of the module's own packages, and the module path on go.mod."
}

func (r *reportInternalImports) InitialValue(*recipe.ExecutionContext) any { return &modulePath{} }

func (r *reportInternalImports) Scanner(acc any) recipe.TreeVisitor {
	return visitor.Init(&modulePathScanner{acc: acc.(*modulePath)})
}

func (r *reportInternalImports) EditorWithData(acc any) recipe.TreeVisitor {
	return visitor.Init(&internalImportReporter{acc: acc.(*modulePath), checked: r.checked})
}

type modulePathScanner struct {
	visitor.GoVisitor
	acc *modulePath
}

func (v *modulePathScanner) VisitGoMod(gm *golang.GoMod, _ any) java.Tree {
	if mrr := java.FindMarker[golang.GoResolutionResult](gm.Markers); mrr != nil {
		v.acc.module = &mrr.ModulePath
	}
	return gm
}

type internalImportReporter struct {
	visitor.GoVisitor
	acc     *modulePath
	checked bool
}

func (v *internalImportReporter) VisitCompilationUnit(cu *golang.CompilationUnit, _ any) java.J {
	if java.HasMarker[java.SearchResult](cu.Markers) || cu.Imports == nil || v.checked && v.acc.module == nil {
		return cu
	}
	var internal []string
	for _, rp := range cu.Imports.Elements {
		if path := rp.Element.Path(); strings.HasPrefix(path, *v.acc.module+"/") {
			internal = append(internal, path)
		}
	}
	if len(internal) == 0 {
		return cu
	}
	return cu.WithMarkers(java.FoundSearchResult(cu.Markers, "internal: "+strings.Join(internal, ", ")))
}

func (v *internalImportReporter) VisitGoMod(gm *golang.GoMod, _ any) java.Tree {
	if java.HasMarker[java.SearchResult](gm.Markers) || v.acc.module == nil {
		return gm
	}
	return gm.WithMarkers(java.FoundSearchResult(gm.Markers, *v.acc.module))
}

func internalImportsProject() Sources {
	return GoProject("app",
		GoMod(`
			module example.com/app

			go 1.22
		`, `
			/*~~(example.com/app)~~>*/module example.com/app

			go 1.22
		`),
		Golang(`
			package main

			import "example.com/app/util"

			func main() { util.Do() }
		`, `
			/*~~(internal: example.com/app/util)~~>*/package main

			import "example.com/app/util"

			func main() { util.Do() }
		`).WithPath("main.go"),
		Golang(`
			package util

			func Do() {}
		`).WithPath("util/util.go"),
	)
}

func TestRecipeThatPanicsWhenAFileItChangedIsMissingFails(t *testing.T) {
	forgetProbes(t)
	// given a scanning recipe that trusts its scanner to find go.mod
	spec := NewRecipeSpec().WithRecipe(&reportInternalImports{})

	// when it runs on a project with a go.mod
	got := failures(t, spec, internalImportsProject())

	// then the run fails, naming the file whose absence the recipe panicked on
	require.Len(t, got, 1)
	assert.True(t, strings.HasPrefix(got[0], "Check that the scanner found the files the recipe needs before "+
		"using what it keeps of them, so that the recipe changes nothing instead of panicking when one is missing.\n"+
		"org.openrewrite.golang.test.ReportInternalImports panicked when go.mod was missing, which is how a Go "+
		"recipe sees a file its parser failed on, left unparsed or read as plain text:\n"+
		"    runtime error: invalid memory address or nil pointer dereference\n"+
		"        at test.(*internalImportReporter).VisitCompilationUnit(unparsed_sources_test.go:"), got[0])
	assert.True(t, strings.HasSuffix(got[0], "  - don't recover from the panic to make this pass"), got[0])
}

func TestRecipeThatChecksWhatItsScannerFoundPasses(t *testing.T) {
	forgetProbes(t)
	NewRecipeSpec().
		WithRecipe(&reportInternalImports{checked: true}).
		RewriteRun(t, internalImportsProject())
}

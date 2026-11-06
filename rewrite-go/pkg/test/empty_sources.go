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
	"fmt"
	"reflect"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// expectEmptySourcesHandled runs the recipe on an empty source of each kind it
// changed, once per process, failing t if the recipe panics there.
func (spec *RecipeSpec) expectEmptySourcesHandled(t testing.TB, run *recipeRun) {
	t.Helper()
	if spec.Recipe == nil {
		return
	}
	for _, ps := range run.parsed {
		if ps.tree == nil || ps.result == ps.tree {
			continue
		}
		failure := probes.once(spec.Recipe, reflect.TypeOf(ps.tree), "empty", func() string {
			empty, what, carried := emptySource(ps.tree)
			if empty == nil {
				return ""
			}
			if p := probe(spec.Recipe, []java.Tree{empty}, run.freshParseContext, spec.cycles()); p != nil {
				return emptySourceFailure(spec, what, carried, p)
			}
			return ""
		})
		if failure != "" {
			t.Error(failure)
		}
	}
}

// emptySource is the emptiest source of the kind of before that its parser
// reads, at the same path and with the markers before has from the test, along
// with what it is and those markers, or nil when the parser reads none. A Go
// file holds at least its package clause.
func emptySource(before java.Tree) (java.Tree, string, []java.Marker) {
	sourcePath := sourcePathOf(before)
	var empty java.Tree
	var what string
	switch sf := before.(type) {
	case *golang.CompilationUnit:
		if sf.PackageDecl == nil || sf.PackageDecl.Element == nil {
			return nil, "", nil
		}
		clause := "package " + sf.PackageDecl.Element.Name
		empty = reparse(parser.NewGoParser(), sourcePath, clause+"\n")
		what = fmt.Sprintf("a golang.CompilationUnit at %s holding only `%s`", sourcePath, clause)
	case *golang.GoMod, *golang.GoSum:
		empty = reparse(parser.NewGoParser(), sourcePath, "")
		what = "an empty " + typeName(reflect.TypeOf(before)) + " at " + sourcePath
	}
	if empty == nil {
		return nil, "", nil
	}
	markers := markersOf(empty)
	if _, ok := empty.(*golang.GoMod); ok {
		// as the test's GoMod("") would carry
		if mrr, err := parser.ParseGoMod(sourcePath, ""); err == nil && mrr != nil {
			markers = java.AddMarker(markers, *mrr)
		}
	}
	var carried []java.Marker
	for _, m := range markersOf(before).Entries() {
		// a resolution result describes what the file itself declares, which an empty file doesn't
		if _, ok := m.(golang.GoResolutionResult); ok || hasMarkerOfType(markers, m) {
			continue
		}
		markers = java.AddMarker(markers, m)
		carried = append(carried, m)
	}
	return withMarkers(empty, markers), what, carried
}

func hasMarkerOfType(markers java.Markers, m java.Marker) bool {
	for _, e := range markers.Entries() {
		if reflect.TypeOf(e) == reflect.TypeOf(m) {
			return true
		}
	}
	return false
}

func emptySourceFailure(spec *RecipeSpec, what string, carried []java.Marker, p *probePanic) string {
	with := ""
	if names := carriedMarkerTypes(carried); names != "" {
		with = " with the markers of the test's source (" + names + ")"
	}
	return "Check for the structure the recipe edits before using it, so that it handles an empty file or " +
		"leaves it unchanged instead of panicking.\n" +
		spec.Recipe.Name() + " panicked when run on " + what + with + ":\n" +
		p.describe(spec.Recipe, "    ") + "\n" +
		"The test gives the recipe only files that contain what it edits, but repositories also hold Go files " +
		"with nothing but a package clause, such as a doc.go, and empty go.mod and go.sum files.\n" +
		"  - return the tree unchanged when what the visitor assumes is absent: check the kind before a type " +
		"assertion (`fn, ok := s.Element.(*java.MethodDeclaration)`), the length before indexing " +
		"(`len(cu.Statements) > 0`) and nil before a dereference (`cu.Imports != nil`)\n" +
		"  - if the recipe adds what is missing, build that content correctly for an empty file too\n" +
		"  - don't recover from the panic to make this pass\n" +
		"Then add a test with such a source, such as Golang(\"package main\"), GoMod(\"\") or GoSum(\"\"), that " +
		"states the intended result."
}

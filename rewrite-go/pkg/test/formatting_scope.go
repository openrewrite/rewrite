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
	"runtime"
	"strings"
	"sync"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

var (
	formatPackage  = reflect.TypeOf(format.AutoFormatVisitor{}).PkgPath()
	visitorPackage = reflect.TypeOf(visitor.GoVisitor{}).PkgPath()
	harnessPackage = reflect.TypeOf(RecipeSpec{}).PkgPath()
	// formattingPlumbing holds the packages between a formatter and the code
	// that asked for the format.
	formattingPlumbing = map[string]bool{
		formatPackage:  true,
		visitorPackage: true,
		strings.TrimSuffix(formatPackage, "/format") + "/preconditions": true,
	}
)

// formattingScope finds the requests a recipe makes to auto-format a whole Go
// source file that existed before the run. Test inputs are usually formatted
// already, so the reformatting this causes on real code rarely shows in an
// expected output.
type formattingScope struct {
	sources  map[uuid.UUID]bool
	mu       sync.Mutex
	asked    map[*golang.CompilationUnit]bool
	requests []formatRequest
}

type formatRequest struct {
	cu       *golang.CompilationUnit
	callSite string
}

// newFormattingScope watches the Go source files of parsed, leaving out go.mod
// and go.sum, which the formatter does not lay out.
func newFormattingScope(parsed []parsedSource) *formattingScope {
	s := &formattingScope{sources: map[uuid.UUID]bool{}, asked: map[*golang.CompilationUnit]bool{}}
	for _, ps := range parsed {
		if cu, ok := ps.tree.(*golang.CompilationUnit); ok {
			s.sources[cu.ID] = true
		}
	}
	return s
}

func (s *formattingScope) onFormat(t, stopAfter java.Tree, _ *visitor.Cursor) {
	cu, ok := t.(*golang.CompilationUnit)
	if stopAfter != nil || !ok || cu == nil || !s.sources[cu.ID] {
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.asked[cu] {
		return
	}
	s.asked[cu] = true
	if callSite, formatter := formatCallSite(); !formatter {
		s.requests = append(s.requests, formatRequest{cu: cu, callSite: callSite})
	}
}

// formatCallSite names the code that asked for the format in progress, or
// reports that a formatter asked, which formats whole files, as does a recipe
// whose own visitor is a formatter.
func formatCallSite() (callSite string, formatter bool) {
	pcs := make([]uintptr, 64)
	frames := runtime.CallersFrames(pcs[:runtime.Callers(2, pcs)])
	inFormatter, afterVisit := false, false
	entry := ""
	for {
		f, more := frames.Next()
		pkg := funcPackage(f.Function)
		switch {
		case !inFormatter:
			inFormatter = pkg == formatPackage
		case formattingPlumbing[pkg]:
			afterVisit = afterVisit || f.Function == visitorPackage+".DrainAfterVisits"
			entry = f.Function[strings.LastIndex(f.Function, "/")+1:]
		case inFormatPackage(pkg):
			return "", true
		case afterVisit:
			// the visitor that asked is gone from the stack by the time its after-visits run
			return "A visitor's DoAfterVisit(format.NewAutoFormatVisitor(nil)) ran on", false
		case f.Function == harnessPackage+".edit" || f.Function == harnessPackage+".scan":
			return "", true
		case entry == "format.AutoFormat" || entry == "format.MaybeAutoFormat":
			return frameString(f) + " called " + entry + " on", false
		default:
			return frameString(f) + " ran a format.AutoFormatVisitor on", false
		}
		if !more {
			return "", true
		}
	}
}

func inFormatPackage(pkg string) bool {
	return strings.Contains("/"+pkg+"/", "/format/")
}

// isFormatter reports whether r or a recipe it runs is a formatter, by its name
// or its package, which may format whole files.
func isFormatter(r recipe.Recipe) bool {
	for _, rc := range recipeStack(r) {
		name := rc.Name()
		if dot := strings.LastIndex(name, "."); dot > 0 && strings.Contains("."+name[:dot]+".", ".format.") {
			return true
		}
		t := reflect.TypeOf(rc)
		for t.Kind() == reflect.Pointer {
			t = t.Elem()
		}
		if inFormatPackage(t.PkgPath()) {
			return true
		}
	}
	return false
}

// failure describes the recipe's requests to format a whole source file whose
// code it changed, or "" when there are none.
func (s *formattingScope) failure(r recipe.Recipe, parsed []parsedSource) string {
	if s == nil || len(s.requests) == 0 || isFormatter(r) {
		return ""
	}
	bySource := map[uuid.UUID]parsedSource{}
	for _, ps := range parsed {
		if cu, ok := ps.tree.(*golang.CompilationUnit); ok {
			bySource[cu.ID] = ps
		}
	}
	var callSites []string
	targets := map[string][]string{}
	for _, req := range s.requests {
		ps := bySource[req.cu.ID]
		if ps.result == nil || ps.result == ps.tree || onlyWhitespaceChanged(ps.tree, ps.result) ||
			holdsNoneOfItsChildren(ps.tree.(*golang.CompilationUnit), req.cu) {
			continue
		}
		target := "all of " + sourcePathOf(ps.tree)
		if _, ok := targets[req.callSite]; !ok {
			callSites = append(callSites, req.callSite)
		}
		if !containsString(targets[req.callSite], target) {
			targets[req.callSite] = append(targets[req.callSite], target)
		}
	}
	if len(callSites) == 0 {
		return ""
	}
	var found strings.Builder
	for _, c := range callSites {
		fmt.Fprintf(&found, "  %s\n", c)
		for _, target := range targets[c] {
			fmt.Fprintf(&found, "    %s\n", target)
		}
	}
	return "Format only what the recipe changed, not the whole file around it. These calls auto-formatted a " +
		"whole source file that existed before the run, with no stopAfter:\n" + found.String() +
		"That rewrites the whitespace of code the recipe did not change. Test inputs are usually formatted " +
		"already, so expected outputs rarely show it, but on real code the diff will carry unrelated " +
		"reformatting. Instead:\n" +
		"  - format the element you created or replaced: format.MaybeAutoFormat(original, changed, p, nil, " +
		"v.Cursor().Parent()) on that element, or format.AutoFormat(newElement, p, nil, cursor) with a cursor " +
		"on its parent\n" +
		"  - when a new element only formats right inside its container, format a copy of the container that " +
		"holds just the new element, and add the formatted element to the original\n" +
		"  - when you remove an element, give its prefix to the element that takes its place\n" +
		"  - a template from pkg/template already formats what it inserts, so drop the extra call\n" +
		"Recipes in a format package are exempt. Any other recipe whose purpose is formatting opts out with " +
		"spec.CheckMinimalFormatting = false."
}

func containsString(list []string, s string) bool {
	for _, e := range list {
		if e == s {
			return true
		}
	}
	return false
}

func onlyWhitespaceChanged(before, after java.Tree) bool {
	return strings.Join(strings.Fields(printTree(before, printer.DefaultMarkerPrinter)), "") ==
		strings.Join(strings.Fields(printTree(after, printer.DefaultMarkerPrinter)), "")
}

// holdsNoneOfItsChildren reports whether formatted holds none of the elements
// the source file original held, as a copy of a file holding only new code to
// be put back afterwards does. A file that had none would format them on real
// code.
func holdsNoneOfItsChildren(original, formatted *golang.CompilationUnit) bool {
	children := sourceChildren(original)
	if len(children) == 0 {
		return false
	}
	for id := range sourceChildren(formatted) {
		if children[id] {
			return false
		}
	}
	return true
}

func sourceChildren(cu *golang.CompilationUnit) map[uuid.UUID]bool {
	ids := map[uuid.UUID]bool{}
	if cu.PackageDecl != nil && cu.PackageDecl.Element != nil {
		ids[cu.PackageDecl.Element.ID] = true
	}
	if cu.Imports != nil {
		for _, rp := range cu.Imports.Elements {
			ids[rp.Element.ID] = true
		}
	}
	for _, rp := range cu.Statements {
		ids[rp.Element.GetID()] = true
	}
	return ids
}

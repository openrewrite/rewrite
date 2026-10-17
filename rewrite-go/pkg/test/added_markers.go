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
	"path"
	"reflect"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

const addedMarkersAdvice = "Markers travel with the LST to every recipe that runs after this one and to every tool " +
	"that reads the result, so they are not a place for a recipe to keep information for itself. Instead:\n" +
	"  - share information between the visit methods of one visitor with cursor messages, " +
	"e.g. v.Cursor().PutMessage(...) and v.Cursor().GetNearestMessage(...)\n" +
	"  - share information between source files, or from scanning to editing, " +
	"through the accumulator of a ScanningRecipe\n" +
	"  - keep the recipe idempotent by recognizing code it has already changed, rather than tagging that code\n" +
	"  - report a finding with SearchResult, and a problem with Markup"

// placement is a marker type in a kind of place: on the source file itself, or
// on an element of it, padding included.
type placement struct {
	onSourceFile bool
	markerType   string
}

// placed is where a placement first occurs in a tree.
type placed struct {
	placement
	holder reflect.Value // the node or padding whose Markers hold the marker
}

// addedMarkersFailure describes the placements in the recipe's results that
// neither the parsed sources nor a fresh parse of the result's printed source
// have, or returns "" when there are none.
func addedMarkersFailure(parsed []parsedSource, generated []java.Tree) string {
	var explained map[placement]bool
	var found strings.Builder
	check := func(sourcePath string, before, after java.Tree) {
		if explained == nil {
			explained = map[placement]bool{}
			for _, ps := range parsed {
				addPlacements(explained, ps.tree)
			}
		}
		var unexplained []placed
		for _, p := range placements(after) {
			if !explained[p.placement] {
				unexplained = append(unexplained, p)
			}
		}
		if len(unexplained) == 0 {
			return
		}
		// Code a recipe writes brings along the syntax markers the parser gives it.
		printed := printTree(after, printer.SanitizedMarkerPrinter)
		if fresh := reparse(sourcePath, printed); fresh != nil {
			parsedToo := map[placement]bool{}
			addPlacements(parsedToo, fresh)
			kept := unexplained[:0]
			for _, p := range unexplained {
				if !parsedToo[p.placement] {
					kept = append(kept, p)
				}
			}
			unexplained = kept
		}
		if len(unexplained) == 0 {
			return
		}
		note := ""
		if before == nil {
			note = " (generated)"
		} else if printTree(before, printer.SanitizedMarkerPrinter) == printed {
			note = " (the printed source is unchanged)"
		}
		fmt.Fprintf(&found, "  %s%s\n", sourcePath, note)
		for _, p := range unexplained {
			fmt.Fprintf(&found, "    %s\n", p)
		}
	}
	for _, ps := range parsed {
		if ps.tree != nil && ps.result != nil && ps.result != ps.tree {
			check(ps.spec.Path, ps.tree, ps.result)
		}
	}
	for _, g := range generated {
		if g != nil {
			check(generatedSourcePath(g), nil, g)
		}
	}
	if found.Len() == 0 {
		return ""
	}
	return "The recipe added markers to the LST that neither the sources it was given nor a fresh parse of its output contain:\n" +
		found.String() + addedMarkersAdvice
}

// reparse parses source as the harness parses a file at sourcePath, returning nil
// when it does not parse.
func reparse(sourcePath, source string) (t java.Tree) {
	// A parser panic would abort the whole test binary, not just this test.
	defer func() {
		if recover() != nil {
			t = nil
		}
	}()
	switch {
	case strings.HasSuffix(sourcePath, ".go"):
		if cu, err := parser.NewGoParser().Parse(sourcePath, source); err == nil {
			return cu
		}
	case path.Base(sourcePath) == "go.mod":
		if gm, err := parser.ParseGoModFile(sourcePath, source); err == nil {
			return gm
		}
	case path.Base(sourcePath) == "go.sum":
		if gs, err := parser.ParseGoSumFile(sourcePath, source); err == nil {
			return gs
		}
	}
	return nil
}

func (p placed) String() string {
	if p.onSourceFile {
		return p.markerType + " on the source file"
	}
	place := typeName(p.holder.Type())
	if s := snippet(p.holder); s != "" {
		place += " `" + s + "`"
	}
	return p.markerType + " on " + place
}

// snippet is the start of the printed source of a node, or of the element a
// padding wraps, or "" when there is no tree to print.
func snippet(holder reflect.Value) (s string) {
	var t java.Tree
	if holder.CanAddr() {
		t, _ = holder.Addr().Interface().(java.Tree)
	}
	if e := holder.FieldByName("Element"); t == nil && e.IsValid() {
		t, _ = e.Interface().(java.Tree)
	}
	if t == nil {
		return ""
	}
	// A node the printer cannot print on its own just goes without a snippet.
	defer func() {
		if recover() != nil {
			s = ""
		}
	}()
	s, _, _ = strings.Cut(strings.TrimSpace(printer.Print(t)), "\n")
	s = strings.TrimSpace(s)
	if r := []rune(s); len(r) > 60 {
		s = string(r[:60]) + "..."
	}
	return s
}

var (
	markersType   = reflect.TypeOf(java.EmptyMarkers)
	spaceType     = reflect.TypeOf(java.EmptySpace)
	javaTypeIface = reflect.TypeOf((*java.JavaType)(nil)).Elem()
)

func addPlacements(set map[placement]bool, t java.Tree) {
	for _, p := range placements(t) {
		set[p.placement] = true
	}
}

// placements lists the placements in t in tree order, each where it first
// occurs. The walk is by reflection because GoVisitor visits the markers of
// nodes only, not those of RightPadded, LeftPadded and most Containers.
func placements(t java.Tree) []placed {
	if t == nil {
		return nil
	}
	w := &placementWalker{seen: map[uintptr]bool{}, found: map[placement]bool{}}
	w.walk(reflect.ValueOf(t), true)
	return w.placed
}

type placementWalker struct {
	seen   map[uintptr]bool
	found  map[placement]bool
	placed []placed
}

func (w *placementWalker) walk(v reflect.Value, root bool) {
	switch v.Kind() {
	case reflect.Interface:
		if !v.IsNil() {
			w.walk(v.Elem(), root)
		}
	case reflect.Pointer:
		// Type attribution carries no markers, and its graphs are large and cyclic.
		if v.IsNil() || v.Type() == spaceType || v.Type().Implements(javaTypeIface) || w.seen[v.Pointer()] {
			return
		}
		w.seen[v.Pointer()] = true
		w.walk(v.Elem(), root)
	case reflect.Slice:
		for i := 0; i < v.Len(); i++ {
			w.walk(v.Index(i), false)
		}
	case reflect.Struct:
		for i := 0; i < v.NumField(); i++ {
			if !v.Type().Field(i).IsExported() {
				continue
			}
			f := v.Field(i)
			if f.Type() != markersType {
				w.walk(f, false)
				continue
			}
			for _, m := range f.Interface().(java.Markers).Entries() {
				if m == nil || isFinding(m) || rewriteRecipeMarkers[markerTypeName(m)] {
					continue
				}
				p := placement{onSourceFile: root, markerType: markerTypeName(m)}
				if !w.found[p] {
					w.found[p] = true
					w.placed = append(w.placed, placed{placement: p, holder: v})
				}
			}
		}
	}
}

// rewriteRecipeMarkers are markers that rewrite's own Java recipes still leave
// for themselves, as the Java harness allows.
var rewriteRecipeMarkers = map[string]bool{
	"org.openrewrite.marker.AlreadyReplaced":             true,
	"org.openrewrite.maven.marker.AlreadyIncremented":    true,
	"org.openrewrite.yaml.MultilineScalarChanged":        true,
	"org.openrewrite.java.search.UsesMethod$MethodMatch": true,
}

// isFinding reports whether m is one of the markers a recipe adds to tell of
// what it found or changed, which is what markers are for.
func isFinding(m java.Marker) bool {
	switch m.(type) {
	case java.SearchResult, *java.SearchResult, java.Markup, *java.Markup,
		java.RecipesThatMadeChanges, *java.RecipesThatMadeChanges:
		return true
	}
	return false
}

// markerTypeName names a GenericMarker by the Java marker it stands in for.
func markerTypeName(m java.Marker) string {
	if g, ok := m.(java.GenericMarker); ok && g.JavaType != "" {
		return g.JavaType
	}
	return typeName(reflect.TypeOf(m))
}

// typeName is t's package-qualified name without pointer or type arguments,
// e.g. java.RightPadded.
func typeName(t reflect.Type) string {
	for t.Kind() == reflect.Pointer {
		t = t.Elem()
	}
	name, _, _ := strings.Cut(t.String(), "[")
	return name
}

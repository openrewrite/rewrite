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
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// visited is the kind of marker a recipe adds to remember what it has seen.
type visited struct {
	Ident uuid.UUID
}

func (m visited) ID() uuid.UUID { return m.Ident }

// markCalls adds a marker to every call of foo, renaming the call too when
// rename is set.
type markCalls struct {
	recipe.Base
	marker func(cu *golang.CompilationUnit) java.Marker
	rename string
}

func (r *markCalls) Name() string        { return "org.openrewrite.golang.test.MarkCalls" }
func (r *markCalls) DisplayName() string { return "Mark calls of foo" }
func (r *markCalls) Description() string { return "Adds a marker to every call of `foo`." }

func (r *markCalls) Editor() recipe.TreeVisitor {
	return visitor.Init(&callMarker{recipe: r})
}

type callMarker struct {
	visitor.GoVisitor
	recipe *markCalls
}

func (v *callMarker) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	mi = v.GoVisitor.VisitMethodInvocation(mi, p).(*java.MethodInvocation)
	if mi.Name.Name != "foo" {
		return mi
	}
	cu, _ := visitor.FirstEnclosing[*golang.CompilationUnit](v.Cursor())
	if v.recipe.rename != "" {
		mi = mi.WithName(mi.Name.WithName(v.recipe.rename))
	}
	return mi.WithMarkers(java.AddMarker(mi.Markers, v.recipe.marker(cu)))
}

func visitedMarker(*golang.CompilationUnit) java.Marker { return visited{Ident: uuid.New()} }

const callsFoo = `
	package main

	func foo(n int) {}

	func bar(n int) {}

	func main() {
		foo(1)
	}
`

// recorder takes the failures reported to it, so a test can assert on them
// instead of failing.
type recorder struct {
	testing.TB
	failures []string
}

func (r *recorder) Error(args ...any) { r.failures = append(r.failures, fmt.Sprint(args...)) }

func (r *recorder) Errorf(format string, args ...any) {
	r.failures = append(r.failures, fmt.Sprintf(format, args...))
}

// failures runs sources through spec as RewriteRun does, returning what it
// reports about the recipe's results rather than failing t.
func failures(t *testing.T, spec *RecipeSpec, sources ...Sources) []string {
	t.Helper()
	parsed, genSpecs, generated := spec.parseAndApply(t, sources)
	r := &recorder{TB: t}
	spec.verify(r, parsed, genSpecs, generated)
	return r.failures
}

func addedMarkersMessage(files string) string {
	return "The recipe added markers to the LST that neither the sources it was given nor a fresh parse of its output contain:\n" +
		files + addedMarkersAdvice
}

func TestMarkerAddedAlongsideAChangeFails(t *testing.T) {
	// given a recipe that renames a call and tags it to remember having visited it
	spec := NewRecipeSpec().WithRecipe(&markCalls{marker: visitedMarker, rename: "bar"})

	// when it runs
	got := failures(t, spec, Golang(callsFoo, `
		package main

		func foo(n int) {}

		func bar(n int) {}

		func main() {
			bar(1)
		}
	`))

	// then the run fails, naming the marker and where the recipe put it
	assert.Equal(t, []string{
		"The recipe added markers to the LST that neither the sources it was given nor a fresh parse of its output contain:\n" +
			"  test.go\n" +
			"    test.visited on java.MethodInvocation `bar(1)`\n" +
			"Markers travel with the LST to every recipe that runs after this one and to every tool that reads the result, " +
			"so they are not a place for a recipe to keep information for itself. Instead:\n" +
			"  - share information between the visit methods of one visitor with cursor messages, " +
			"e.g. v.Cursor().PutMessage(...) and v.Cursor().GetNearestMessage(...)\n" +
			"  - share information between source files, or from scanning to editing, through the accumulator of a ScanningRecipe\n" +
			"  - keep the recipe idempotent by recognizing code it has already changed, rather than tagging that code\n" +
			"  - report a finding with SearchResult, and a problem with Markup",
	}, got)
}

func TestMarkerAddedWithoutAChangeFailsOnTheMarker(t *testing.T) {
	// given a recipe that only tags calls
	spec := NewRecipeSpec().WithRecipe(&markCalls{marker: visitedMarker})

	// when it runs
	got := failures(t, spec, Golang(callsFoo))

	// then the marker is the failure, not the new tree the marker made
	assert.Equal(t, []string{addedMarkersMessage(
		"  test.go (the printed source is unchanged)\n" +
			"    test.visited on java.MethodInvocation `foo(1)`\n",
	)}, got)
}

func TestSourceFileMarkerAttachedToAnElementFails(t *testing.T) {
	// given a recipe that copies the project marker of the source file onto a call
	spec := NewRecipeSpec().WithRecipe(&markCalls{marker: func(cu *golang.CompilationUnit) java.Marker {
		return *java.FindMarker[golang.GoProject](cu.Markers)
	}})

	// when it runs over a project, whose sources carry a GoProject marker
	got := failures(t, spec, GoProject("app", Golang(callsFoo)))

	// then the marker type being known on source files does not explain it on a call
	assert.Equal(t, []string{addedMarkersMessage(
		"  test.go (the printed source is unchanged)\n" +
			"    golang.GoProject on java.MethodInvocation `foo(1)`\n",
	)}, got)
}

func TestSearchResultIsNotAnAddedMarker(t *testing.T) {
	// given a recipe that reports the calls it finds
	spec := NewRecipeSpec().WithRecipe(&markCalls{marker: func(*golang.CompilationUnit) java.Marker {
		return java.NewSearchResult("")
	}})

	// when it runs, then it passes
	spec.RewriteRun(t, Golang(callsFoo, `
		package main

		func foo(n int) {}

		func bar(n int) {}

		func main() {
			/*~~>*/foo(1)
		}
	`))
}

func TestSyntaxMarkerOfWrittenCodeIsNotAnAddedMarker(t *testing.T) {
	// given a recipe that writes a trailing comma, which the parser keeps in a
	// TrailingComma marker that none of the sources has
	spec := NewRecipeSpec().WithRecipe(&markCalls{marker: func(*golang.CompilationUnit) java.Marker {
		return golang.TrailingComma{Ident: uuid.New(), Before: java.EmptySpace, After: java.EmptySpace}
	}})

	// when it runs, then a fresh parse of its output explains the marker
	spec.RewriteRun(t, Golang(callsFoo, `
		package main

		func foo(n int) {}

		func bar(n int) {}

		func main() {
			foo(1,)
		}
	`))
}

func TestAddedMarkersCheckCanBeTurnedOff(t *testing.T) {
	// given a recipe that tags the calls it renames, with the check turned off
	spec := NewRecipeSpec().WithRecipe(&markCalls{marker: visitedMarker, rename: "bar"})
	spec.CheckReportingMarkersOnly = false

	// when it runs, then it passes
	spec.RewriteRun(t, Golang(callsFoo, `
		package main

		func foo(n int) {}

		func bar(n int) {}

		func main() {
			bar(1)
		}
	`))
}

// generateMarked generates tools.go, tagged.
type generateMarked struct {
	recipe.ScanningBase
}

func (r *generateMarked) Name() string                              { return "org.openrewrite.golang.test.GenerateMarked" }
func (r *generateMarked) DisplayName() string                       { return "Generate a tagged file" }
func (r *generateMarked) Description() string                       { return "Generates tools.go with a marker on it." }
func (r *generateMarked) InitialValue(*recipe.ExecutionContext) any { return nil }

func (r *generateMarked) Generate(any, *recipe.ExecutionContext) []java.Tree {
	cu, err := parser.NewGoParser().Parse("tools.go", "package main\n")
	if err != nil {
		return nil
	}
	return []java.Tree{cu.WithMarkers(java.AddMarker(cu.Markers, visited{Ident: uuid.New()}))}
}

func TestMarkerOnAGeneratedFileFails(t *testing.T) {
	// given a scanning recipe that tags the file it generates
	spec := NewRecipeSpec().WithRecipe(&generateMarked{})

	// when it runs
	got := failures(t, spec, Golang(callsFoo), Generated("tools.go", "package main\n"))

	// then the generated file is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "  tools.go (generated)\n    test.visited on the source file\n")
}

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
	"errors"
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/preconditions"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// keepWhatItSees keeps in its accumulator what keep picks out of each source
// file its scanner sees, or its editor when whileEditing.
type keepWhatItSees struct {
	recipe.ScanningBase
	keep         func(acc *seen, v *seeingVisitor, sf java.Tree)
	whileEditing bool
}

type seen struct {
	byPath map[string]any
	all    []any
}

func (r *keepWhatItSees) Name() string        { return "org.openrewrite.golang.test.KeepWhatItSees" }
func (r *keepWhatItSees) DisplayName() string { return "Keep what it sees" }
func (r *keepWhatItSees) Description() string {
	return "Keeps in its accumulator what it picks out of each source file it sees."
}

func (r *keepWhatItSees) InitialValue(*recipe.ExecutionContext) any {
	return &seen{byPath: map[string]any{}}
}

func (r *keepWhatItSees) Scanner(acc any) recipe.TreeVisitor {
	if r.whileEditing {
		return nil
	}
	return visitor.Init(&seeingVisitor{acc: acc.(*seen), keep: r.keep})
}

func (r *keepWhatItSees) EditorWithData(acc any) recipe.TreeVisitor {
	if !r.whileEditing {
		return nil
	}
	return visitor.Init(&seeingVisitor{acc: acc.(*seen), keep: r.keep})
}

type seeingVisitor struct {
	visitor.GoVisitor
	acc  *seen
	keep func(acc *seen, v *seeingVisitor, sf java.Tree)
}

func (v *seeingVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, _ any) java.J {
	v.keep(v.acc, v, cu)
	return cu
}

func (v *seeingVisitor) VisitGoMod(gm *golang.GoMod, _ any) java.Tree {
	v.keep(v.acc, v, gm)
	return gm
}

func TestSourceFileKeptInAnAccumulatorFails(t *testing.T) {
	// given a scanning recipe that keeps the source files it scans
	spec := NewRecipeSpec().WithRecipe(&keepWhatItSees{keep: func(acc *seen, _ *seeingVisitor, sf java.Tree) {
		acc.byPath[sourcePathOf(sf)] = sf
	}})

	// when it runs
	got := failures(t, spec, Golang(`
		package main
	`).WithPath("a.go"))

	// then the run fails, naming the path to the source file
	assert.Equal(t, []string{
		"A ScanningRecipe's accumulator should keep the values the recipe needs, such as names, paths, " +
			"positions and ids, and no recipe.Heavyweight objects, such as trees, cursors, visitors, execution " +
			"contexts and module graphs:\n" +
			"  org.openrewrite.golang.test.KeepWhatItSees keeps\n" +
			"    test.seen.byPath[a.go] -> golang.CompilationUnit a.go, after scanning\n" +
			"An accumulator stays in memory from the first source file scanned until the cycle ends, so " +
			"what it keeps for each source file adds up over a large repository. A GoResolutionResult can hold the " +
			"module graph of a whole build, while small markers such as GoProject are fine to keep. A recipe that " +
			"must keep a Heavyweight object opts out with spec.CheckLightweightAccumulators = false.",
	}, got)
}

func TestModuleGraphKeptInAnAccumulatorFails(t *testing.T) {
	// given a scanning recipe that keeps the GoResolutionResult of go.mod
	spec := NewRecipeSpec().WithRecipe(&keepWhatItSees{keep: func(acc *seen, _ *seeingVisitor, sf java.Tree) {
		if gm, ok := sf.(*golang.GoMod); ok {
			acc.all = append(acc.all, *java.FindMarker[golang.GoResolutionResult](gm.Markers))
		}
	}})

	// when it runs
	got := failures(t, spec, GoProject("app", GoMod(`
		module example.com/app

		go 1.22
	`)))

	// then the marker is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "    test.seen.all[0] -> golang.GoResolutionResult, after scanning\n")
}

func TestCursorKeptInAnAccumulatorFails(t *testing.T) {
	// given a scanning recipe that keeps the cursor it scans with
	spec := NewRecipeSpec().WithRecipe(&keepWhatItSees{keep: func(acc *seen, v *seeingVisitor, _ java.Tree) {
		acc.all = append(acc.all, v.Cursor())
	}})

	// when it runs
	got := failures(t, spec, Golang(`
		package main
	`))

	// then the cursor is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "    test.seen.all[0] -> visitor.Cursor, after scanning\n")
}

// moduleIndex is a recipe library's own type, which it tags as too costly to keep.
type moduleIndex struct {
	modules map[string][]string
}

func (*moduleIndex) IsHeavyweight() {}

func TestLibraryTypeTaggedHeavyweightKeptInAnAccumulatorFails(t *testing.T) {
	// given a scanning recipe that keeps a type its library tags as Heavyweight
	spec := NewRecipeSpec().WithRecipe(&keepWhatItSees{keep: func(acc *seen, _ *seeingVisitor, sf java.Tree) {
		acc.byPath[sourcePathOf(sf)] = &moduleIndex{modules: map[string][]string{}}
	}})

	// when it runs
	got := failures(t, spec, Golang(`
		package main
	`).WithPath("a.go"))

	// then the run fails, naming the path to it
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "    test.seen.byPath[a.go] -> test.moduleIndex, after scanning\n")
}

func TestVisitorAndExecutionContextKeptInAnAccumulatorFail(t *testing.T) {
	// given a scanning recipe that keeps its scanner and an execution context
	spec := NewRecipeSpec().WithRecipe(&keepWhatItSees{keep: func(acc *seen, v *seeingVisitor, _ java.Tree) {
		acc.all = append(acc.all, v, recipe.NewExecutionContext())
	}})

	// when it runs
	got := failures(t, spec, Golang(`
		package main
	`))

	// then both are reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "    test.seen.all[0] -> test.seeingVisitor, after scanning\n"+
		"    test.seen.all[1] -> recipe.ExecutionContext, after scanning\n")
}

func TestPreconditionVisitorsKeptInAnAccumulatorFail(t *testing.T) {
	// given a scanning recipe that keeps the SDK's visitors that embed no GoVisitor
	spec := NewRecipeSpec().WithRecipe(&keepWhatItSees{keep: func(acc *seen, _ *seeingVisitor, _ java.Tree) {
		acc.all = append(acc.all, preconditions.NewIsSourceFile("**/*.go"), preconditions.NewUsesType("fmt.Stringer"),
			preconditions.NewUsesMethod("fmt.Println(..)"), preconditions.Check(preconditions.UsesType("fmt.Stringer"), nil))
	}})

	// when it runs
	got := failures(t, spec, Golang(`
		package main
	`))

	// then each is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "    test.seen.all[0] -> preconditions.IsSourceFileVisitor, after scanning\n"+
		"    test.seen.all[1] -> preconditions.UsesTypeVisitor, after scanning\n"+
		"    test.seen.all[2] -> preconditions.UsesMethodVisitor, after scanning\n"+
		"    test.seen.all[3] -> preconditions.CheckVisitor, after scanning\n")
}

func TestSourceFileKeptWhileEditingFails(t *testing.T) {
	// given a scanning recipe whose editor keeps the source files it edits
	spec := NewRecipeSpec().WithRecipe(&keepWhatItSees{whileEditing: true, keep: func(acc *seen, _ *seeingVisitor, sf java.Tree) {
		acc.all = append(acc.all, sf)
	}})

	// when it runs
	got := failures(t, spec, Golang(`
		package main
	`).WithPath("a.go"))

	// then the source file is reported as kept after editing
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "    test.seen.all[0] -> golang.CompilationUnit a.go, after editing\n")
}

func TestValuesAndSmallMarkersKeptInAnAccumulatorPass(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&keepWhatItSees{keep: func(acc *seen, _ *seeingVisitor, sf java.Tree) {
			if cu, ok := sf.(*golang.CompilationUnit); ok {
				acc.byPath[cu.SourcePath] = *java.FindMarker[golang.GoProject](cu.Markers)
				acc.all = append(acc.all, cu.SourcePath, cu.ID, cu.Markers)
			}
		}}).
		RewriteRun(t, GoProject("app", Golang(`
			package main
		`)))
}

func TestPatternKeptInAnAccumulatorPasses(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&keepWhatItSees{keep: func(acc *seen, v *seeingVisitor, sf java.Tree) {
			if cu, ok := sf.(*golang.CompilationUnit); ok {
				// matching parses the pattern, which then keeps the tree of its own code
				pattern := template.Expression("a()").Build()
				pattern.Match(cu, v.Cursor())
				acc.all = append(acc.all, pattern)
			}
		}}).
		RewriteRun(t, Golang(`
			package main
		`))
}

func TestAccumulatorCheckCanBeTurnedOff(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&keepWhatItSees{keep: func(acc *seen, _ *seeingVisitor, sf java.Tree) {
		acc.all = append(acc.all, sf)
	}})
	spec.CheckLightweightAccumulators = false
	spec.RewriteRun(t, Golang(`
		package main
	`))
}

func TestEveryTreeIsHeavyweight(t *testing.T) {
	// given trees of each kind the parser builds
	trees := []java.Tree{java.NewParseError("broken.go", "package", errors.New("expected 'IDENT'"))}
	for sourcePath, source := range map[string]string{
		"main.go": everyKindOfTree,
		"go.mod":  "module example.com/app\n\ngo 1.22\n\nrequire (\n\texample.com/lib v1.0.0\n)\n",
		"go.sum":  "example.com/lib v1.0.0 h1:abc=\n",
	} {
		sf, err := parseSource(parser.NewGoParser(), sourcePath, source)
		require.NoError(t, err)
		trees = append(trees, sf)
	}

	// when they are walked
	untagged := map[string]bool{}
	for _, tr := range trees {
		visitor.Walk(tr, func(node java.Tree) bool {
			if _, ok := node.(recipe.Heavyweight); !ok {
				untagged[reflect.TypeOf(node).String()] = true
			}
			return true
		})
	}

	// then each of them carries the tag
	assert.Empty(t, untagged)
}

const everyKindOfTree = `package main

import (
	"fmt"
	str "strings"
)

type (
	Number interface{ ~int | ~float64 }
	Pair[K comparable, V any] struct {
		Key   K
		Value V
	}
	Celsius float64
)

const limit, other = 10, 20

var (
	table    = map[string][]int{"a": {1, 2}}
	callback func(int) error
)

func Sum[T Number](values ...T) (total T) {
	for _, v := range values {
		total += v
	}
	return
}

func Swap[A, B any](a A, b B) (B, A) { return b, a }

func (p *Pair[K, V]) String() string { return fmt.Sprint(p.Key, p.Value) }

//go:noinline
func main() {
	ch := make(chan int, 1)
	done := make(<-chan struct{})
	var arr [2]int
	x, y := 1, 2
	x, y = y, x
	ptr := &arr
	f := func() {}
	go f()
	defer f()
	ch <- x
	select {
	case v := <-ch:
		_ = v
	case <-done:
	default:
	}
	switch v := any(x).(type) {
	case int:
		_ = v
		fallthrough
	default:
	}
	if n := len(arr); n > 0 && !false {
		x++
	} else {
		x--
	}
loop:
	for i := 0; i < limit; i++ {
		if i == 1 {
			continue loop
		}
		break loop
	}
	switch {
	case x > y:
		goto end
	}
end:
	_ = Celsius(1.5)
	_ = Pair[string, int]{Key: "a", Value: (1 + 2) * -3}
	_ = ptr[0]
	_ = arr[:1]
	_ = Sum[int](1, 2)
	_ = Sum(arr[:]...)
	swap := Swap[int, string]
	_, _ = swap(1, "a")
	x &^= y
	_ = x &^ y
	<-ch
	(f())
	_ = str.ToUpper("a")
	_ = (*int)(nil)
	_ = struct{ A int }{A: 1}
	_ = table
	_ = other
	_ = callback
}
`

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

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// renameIdentifiers renames each identifier the function returns a new name for.
type renameIdentifiers struct {
	recipe.Base
	rename func(v *identifierRenamer, ident *java.Identifier) *java.Identifier
}

func (r *renameIdentifiers) Name() string        { return "org.openrewrite.golang.test.RenameIdentifiers" }
func (r *renameIdentifiers) DisplayName() string { return "Rename identifiers" }
func (r *renameIdentifiers) Description() string { return "Renames identifiers." }

func (r *renameIdentifiers) Editor() recipe.TreeVisitor {
	return visitor.Init(&identifierRenamer{recipe: r})
}

type identifierRenamer struct {
	visitor.GoVisitor
	recipe *renameIdentifiers
	loud   bool
}

func (v *identifierRenamer) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	v.loud = java.HasMarker[loud](cu.Markers)
	return v.GoVisitor.VisitCompilationUnit(cu, p)
}

func (v *identifierRenamer) VisitIdentifier(ident *java.Identifier, p any) java.J {
	return v.recipe.rename(v, v.GoVisitor.VisitIdentifier(ident, p).(*java.Identifier))
}

// oneStepAlong renames a to b and b to c, so a recipe running it once turns a into
// b and needs another run to reach c, as a recipe that only looks one step ahead
// would.
func oneStepAlong(_ *identifierRenamer, ident *java.Identifier) *java.Identifier {
	switch ident.Name {
	case "a":
		return ident.WithName("b")
	case "b":
		return ident.WithName("c")
	}
	return ident
}

// loud is a marker a source file carries from outside its text.
type loud struct {
	Ident uuid.UUID
}

func (m loud) ID() uuid.UUID { return m.Ident }

// done marks an identifier as renamed through its type, which a fresh parse
// does not bring back.
var done = &java.JavaTypeClass{FullyQualifiedName: "done"}

func TestRecipeChangingItsResultAgainFails(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&renameIdentifiers{rename: oneStepAlong})

	// when
	got := failures(t, spec, Golang(`
		package main

		func a() {}
	`, `
		package main

		func b() {}
	`))

	// then
	require.Len(t, got, 1)
	assert.True(t, strings.HasPrefix(got[0], "Expected recipe to complete in 1 cycle, but cycle 2 changed the sources again:\n"+
		"  test.go\n"+
		"Expected:\npackage main\n\nfunc b() {}\n\n\n"+
		"Actual:\npackage main\n\nfunc c() {}\n\n"), got[0])
}

func TestExpectedCyclesThatMakeChangesComparesTheLastExpectedCycle(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&renameIdentifiers{rename: oneStepAlong}).
		WithExpectedCyclesThatMakeChanges(2).
		RewriteRun(t, Golang(`
			package main

			func a() {}
		`, `
			package main

			func c() {}
		`))
}

func TestExpectedCyclesThatMakeChangesFailsARecipeThatSettlesSooner(t *testing.T) {
	// given
	spec := NewRecipeSpec().
		WithRecipe(&renameIdentifiers{rename: oneStepAlong}).
		WithExpectedCyclesThatMakeChanges(2)

	// when
	got := failures(t, spec, Golang(`
		package main

		func b() {}
	`, `
		package main

		func c() {}
	`))

	// then
	assert.Equal(t, []string{"Expected recipe to complete in 2 cycles, but it settled after 1 cycle. " +
		"Lower ExpectedCyclesThatMakeChanges."}, got)
}

func TestOneCycleTurnsTheCheckOff(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&renameIdentifiers{rename: oneStepAlong}).
		WithCycles(1).
		RewriteRun(t, Golang(`
			package main

			func a() {}
		`, `
			package main

			func b() {}
		`))
}

func TestRecipeChangingTheLSTAgainWithoutChangingTheTextFails(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&renameIdentifiers{rename: func(_ *identifierRenamer, ident *java.Identifier) *java.Identifier {
		switch ident.Name {
		case "a":
			return ident.WithName("A")
		case "A":
			rebuilt := *ident
			return &rebuilt
		}
		return ident
	}})

	// when
	got := failures(t, spec, Golang(`
		package main

		func a() {}
	`, `
		package main

		func A() {}
	`))

	// then
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "  test.go, whose LST it changed without changing the printed source\n")
}

// generateAlways generates tools.go whether or not it exists.
type generateAlways struct {
	recipe.ScanningBase
}

func (r *generateAlways) Name() string        { return "org.openrewrite.golang.test.GenerateAlways" }
func (r *generateAlways) DisplayName() string { return "Generate always" }
func (r *generateAlways) Description() string {
	return "Generates `tools.go` whether or not it exists."
}

func (r *generateAlways) InitialValue(*recipe.ExecutionContext) any { return nil }

func (r *generateAlways) Generate(any, *recipe.ExecutionContext) []java.Tree {
	cu, err := parser.NewGoParser().Parse("tools.go", "package app\n")
	if err != nil {
		return nil
	}
	return []java.Tree{cu}
}

func TestRecipeGeneratingAFileAgainFails(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&generateAlways{})

	// when
	got := failures(t, spec, Generated("tools.go", "package app\n"))

	// then
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "  tools.go, which it generated\n")
}

func TestRecipeRecognizingItsOutputOnlyByTheShapeOfItsLSTFailsOnAFreshParse(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&renameIdentifiers{rename: func(_ *identifierRenamer, ident *java.Identifier) *java.Identifier {
		if !strings.HasPrefix(ident.Name, "hi") || ident.Type == done {
			return ident
		}
		return ident.WithName(ident.Name + "_x").WithType(done)
	}})

	// when
	got := failures(t, spec, Golang(`
		package main

		func hi() {}
	`, `
		package main

		func hi_x() {}
	`))

	// then
	require.Len(t, got, 1)
	assert.True(t, strings.HasPrefix(got[0], "Running the recipe on a fresh parse of its output changed the sources again:\n"+
		"  test.go\n"+
		"Expected:\npackage main\n\nfunc hi_x() {}\n\n\n"+
		"Actual:\npackage main\n\nfunc hi_x_x() {}\n\n"), got[0])
}

func TestFreshParseKeepsWhatTheSourceFileCarriesFromOutsideItsText(t *testing.T) {
	source := Golang(`
		package main

		func Hi() {}
	`, `
		package main

		func HI() {}
	`)
	source.Markers = []java.Marker{loud{Ident: uuid.New()}}

	NewRecipeSpec().
		WithRecipe(&renameIdentifiers{rename: func(v *identifierRenamer, ident *java.Identifier) *java.Identifier {
			if ident.Name == "main" {
				return ident
			}
			cased := strings.ToLower(ident.Name)
			if v.loud {
				cased = strings.ToUpper(ident.Name)
			}
			if cased == ident.Name {
				return ident
			}
			return ident.WithName(cased)
		}}).
		RewriteRun(t, source)
}

func TestFewerCyclesThanExpectedCyclesThatMakeChangesFails(t *testing.T) {
	// given
	spec := NewRecipeSpec().
		WithRecipe(&renameIdentifiers{rename: oneStepAlong}).
		WithCycles(1).
		WithExpectedCyclesThatMakeChanges(2)

	// when
	got := failures(t, spec, Golang(`
		package main

		func a() {}
	`, `
		package main

		func c() {}
	`))

	// then
	assert.Equal(t, []string{"Cycles is 1, fewer than the 2 ExpectedCyclesThatMakeChanges."}, got)
}

func TestFreshParseStillRunsWhenALaterCycleOnlyRebuildsTreesUnchecked(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&renameIdentifiers{rename: func(_ *identifierRenamer, ident *java.Identifier) *java.Identifier {
		if ident.Name == "main" {
			rebuilt := *ident
			return &rebuilt
		}
		if !strings.HasPrefix(ident.Name, "hi") || ident.Type == done {
			return ident
		}
		return ident.WithName(ident.Name + "_x").WithType(done)
	}})
	spec.CheckUnchangedTreeIdentity = false

	// when
	got := failures(t, spec, Golang(`
		package main

		func hi() {}
	`, `
		package main

		func hi_x() {}
	`))

	// then
	require.Len(t, got, 1)
	assert.True(t, strings.HasPrefix(got[0], "Running the recipe on a fresh parse of its output changed the sources again:\n"), got[0])
}

// stamp is a marker a recipe leaves on a source file it has changed.
type stamp struct {
	Ident uuid.UUID
}

func (m stamp) ID() uuid.UUID { return m.Ident }

// stampedRename renames identifiers starting with hi in a source file without a
// stamp, stamping the file.
type stampedRename struct {
	recipe.Base
}

func (r *stampedRename) Name() string        { return "org.openrewrite.golang.test.StampedRename" }
func (r *stampedRename) DisplayName() string { return "Stamped rename" }
func (r *stampedRename) Description() string {
	return "Renames identifiers in a source file it has not stamped."
}

func (r *stampedRename) Editor() recipe.TreeVisitor {
	return visitor.Init(&stampedRenamer{})
}

type stampedRenamer struct {
	visitor.GoVisitor
}

func (v *stampedRenamer) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	if java.HasMarker[stamp](cu.Markers) {
		return cu
	}
	renamed := v.GoVisitor.VisitCompilationUnit(cu, p).(*golang.CompilationUnit)
	if renamed == cu {
		return cu
	}
	return renamed.WithMarkers(java.AddMarker(renamed.Markers, stamp{Ident: uuid.New()}))
}

func (v *stampedRenamer) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if !strings.HasPrefix(ident.Name, "hi") {
		return ident
	}
	return ident.WithName(ident.Name + "_x")
}

func TestFreshParseDropsMarkersTheRecipeLeftOnTheSourceFile(t *testing.T) {
	// given
	spec := NewRecipeSpec().WithRecipe(&stampedRename{})
	spec.CheckReportingMarkersOnly = false

	// when
	got := failures(t, spec, Golang(`
		package main

		func hi() {}
	`, `
		package main

		func hi_x() {}
	`))

	// then
	require.Len(t, got, 1)
	assert.True(t, strings.HasPrefix(got[0], "Running the recipe on a fresh parse of its output changed the sources again:\n"), got[0])
}

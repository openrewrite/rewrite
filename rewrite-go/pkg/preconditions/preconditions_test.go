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

package preconditions

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// newSourceFile returns a fresh CompilationUnit pointer to use as the
// source-file sentinel. Each call returns a distinct identity so a
// "marking" visitor can return a different value.
func newSourceFile() java.Tree {
	return &golang.CompilationUnit{}
}

// recordingVisitor is a TreeVisitor that returns the same tree, recording
// each call. Simulates a precondition that does NOT match.
type recordingVisitor struct{ calls int }

func (r *recordingVisitor) Visit(t java.Tree, _ any) java.Tree {
	r.calls++
	return t
}

// markingVisitor is a TreeVisitor that returns a *different* tree
// (any non-identity value), simulating a precondition that DOES match
// (e.g. by adding a SearchResult marker).
type markingVisitor struct{ calls int }

func (m *markingVisitor) Visit(t java.Tree, _ any) java.Tree {
	m.calls++
	return newSourceFile() // different identity from the input
}

func TestCheckRunsEditorWhenConditionMarks(t *testing.T) {
	cond := &markingVisitor{}
	editor := &recordingVisitor{}

	wrapped := Check(cond, editor)
	wrapped.Visit(newSourceFile(), nil)

	assert.Equal(t, 1, cond.calls, "condition calls")
	assert.Equal(t, 1, editor.calls, "editor calls")
}

func TestCheckSkipsEditorWhenConditionReturnsIdentity(t *testing.T) {
	cond := &recordingVisitor{}
	editor := &recordingVisitor{}

	wrapped := Check(cond, editor)
	wrapped.Visit(newSourceFile(), nil)

	assert.Equal(t, 1, cond.calls, "condition calls")
	assert.Equalf(t, 0, editor.calls, "editor calls = %d, want 0 (gate did not match)", editor.calls)
}

func TestOrShortCircuitsOnFirstMatch(t *testing.T) {
	matching := &markingVisitor{}
	nonMatching := &recordingVisitor{}
	editor := &recordingVisitor{}

	Check(Or(matching, nonMatching), editor).Visit(newSourceFile(), nil)

	assert.Equal(t, 1, matching.calls, "matching calls")
	assert.Equalf(t, 0, nonMatching.calls, "nonMatching calls = %d, want 0 (Or should short-circuit)", nonMatching.calls)
	assert.Equal(t, 1, editor.calls, "editor calls")
}

func TestOrSkipsEditorWhenNoOperandMatches(t *testing.T) {
	a := &recordingVisitor{}
	b := &recordingVisitor{}
	editor := &recordingVisitor{}

	Check(Or(a, b), editor).Visit(newSourceFile(), nil)

	assert.False(t, a.calls != 1 || b.calls != 1, "operand calls")
	assert.Equal(t, 0, editor.calls, "editor calls")
}

func TestAndRunsEditorOnlyWhenAllMatch(t *testing.T) {
	a := &markingVisitor{}
	b := &markingVisitor{}
	editor := &recordingVisitor{}
	Check(And(a, b), editor).Visit(newSourceFile(), nil)
	assert.Equal(t, 1, editor.calls, "editor calls (all match")

	editor2 := &recordingVisitor{}
	Check(And(a, &recordingVisitor{}), editor2).Visit(newSourceFile(), nil)
	assert.Equal(t, 0, editor2.calls, "editor2 calls (one non-matching")
}

func TestNotInvertsMatch(t *testing.T) {
	editor1 := &recordingVisitor{}
	Check(Not(&markingVisitor{}), editor1).Visit(newSourceFile(), nil)
	assert.Equal(t, 0, editor1.calls, "not(matching): editor calls")

	editor2 := &recordingVisitor{}
	Check(Not(&recordingVisitor{}), editor2).Visit(newSourceFile(), nil)
	assert.Equal(t, 1, editor2.calls, "not(non-matching): editor calls")
}

func TestBareRecipeRefShortCircuitsToMatch(t *testing.T) {
	// A bare RecipeRef without a LocalVisitor short-circuits to "matches"
	// in-process so the wrapped editor still runs. The host evaluates the
	// gate for real once the response goes over the wire.
	editor := &recordingVisitor{}
	bare := &RecipeRef{
		RecipeName: "org.openrewrite.java.search.HasMethod",
		Options:    map[string]any{"methodPattern": "*..* nope(..)"},
	}
	Check(bare, editor).Visit(newSourceFile(), nil)
	assert.Equal(t, 1, editor.calls, "editor calls (bare RecipeRef")
}

func TestRecipeRefWithLocalVisitorEvaluatesForReal(t *testing.T) {
	// Helpers like UsesMethod populate a native LocalVisitor so unit
	// tests without an active RPC connection still see real filtering.
	// An empty CompilationUnit has no method invocations, so the gate
	// fails and the editor is skipped.
	editor := &recordingVisitor{}
	Check(UsesMethod("*..* tostring(..)"), editor).Visit(newSourceFile(), nil)
	assert.Equal(t, 0, editor.calls, "editor calls (RecipeRef with LocalVisitor")
}

func TestHelpersPopulateLocalVisitor(t *testing.T) {
	// Spot-check that helpers bundle a TreeVisitor for offline eval.
	assert.NotNil(t, HasSourcePath("**/*.go").LocalVisitor, "HasSourcePath did not populate LocalVisitor")
	assert.NotNil(t, UsesMethod("*..* a(..)").LocalVisitor, "UsesMethod did not populate LocalVisitor")
	assert.NotNil(t, UsesType("foo.Bar").LocalVisitor, "UsesType did not populate LocalVisitor")
	assert.NotNil(t, FindMethods("*..* a(..)").LocalVisitor, "FindMethods did not populate LocalVisitor")
	assert.NotNil(t, FindTypes("foo.Bar").LocalVisitor, "FindTypes did not populate LocalVisitor")
}

func TestHasSourcePathMatchesCompilationUnit(t *testing.T) {
	// given a real source file with a path
	editor := &recordingVisitor{}
	cu := &golang.CompilationUnit{SourcePath: "pkg/foo/bar.go"}

	// when the glob matches the path
	Check(HasSourcePath("**/*.go"), editor).Visit(cu, nil)

	// then the editor runs
	assert.Equal(t, 1, editor.calls, "editor calls (matching path")

	// and when the glob does not match
	editor2 := &recordingVisitor{}
	Check(HasSourcePath("**/*.java"), editor2).Visit(cu, nil)
	assert.Equal(t, 0, editor2.calls, "editor calls (non-matching path")
}

func TestHasSourcePathMatchesGoMod(t *testing.T) {
	// given a go.mod root (a non-J SourceFile)
	editor := &recordingVisitor{}
	mod := &golang.GoMod{SourcePath: "go.mod"}

	// when the glob matches
	Check(HasSourcePath("**/go.mod"), editor).Visit(mod, nil)

	// then the editor runs even though GoMod is not a java.SourceFile
	assert.Equal(t, 1, editor.calls, "editor calls (go.mod")

	// and a non-matching glob must actually filter the GoMod out, rather
	// than the gate being bypassed because GoMod is not a java.SourceFile.
	editor2 := &recordingVisitor{}
	Check(HasSourcePath("**/*.go"), editor2).Visit(mod, nil)
	assert.Equal(t, 0, editor2.calls, "editor calls (go.mod, non-matching")
}

func TestUsesMethodMatchesInvocationInTree(t *testing.T) {
	// given a compilation unit containing fmt.Println(...)
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{Element: &java.Identifier{Name: "fmt", Type: &java.JavaTypeClass{FullyQualifiedName: "fmt"}}},
		Name:   &java.Identifier{Name: "Println"},
	}
	cu := &golang.CompilationUnit{
		Statements: []java.RightPadded[java.Statement]{{Element: mi}},
	}

	// when gating on the matching method pattern
	editor := &recordingVisitor{}
	Check(UsesMethod("fmt Println(..)"), editor).Visit(cu, nil)

	// then the editor runs
	assert.Equal(t, 1, editor.calls, "editor calls (matching method")

	// and a non-matching pattern skips the editor
	editor2 := &recordingVisitor{}
	Check(UsesMethod("fmt Printf(..)"), editor2).Visit(cu, nil)
	assert.Equal(t, 0, editor2.calls, "editor calls (non-matching method")
}

func TestUsesTypeMatchesAttributionInTree(t *testing.T) {
	// given a node carrying type attribution for "tarfile"
	typed := &java.Identifier{
		Name: "tr",
		Type: &java.JavaTypeClass{FullyQualifiedName: "tarfile"},
	}
	mi := &java.MethodInvocation{Name: typed}
	cu := &golang.CompilationUnit{
		Statements: []java.RightPadded[java.Statement]{{Element: mi}},
	}

	// when gating on the matching type
	editor := &recordingVisitor{}
	Check(UsesType("tarfile"), editor).Visit(cu, nil)

	// then the editor runs
	assert.Equal(t, 1, editor.calls, "editor calls (matching type")

	// and an unrelated type skips the editor
	editor2 := &recordingVisitor{}
	Check(UsesType("zipfile"), editor2).Visit(cu, nil)
	assert.Equal(t, 0, editor2.calls, "editor calls (non-matching type")
}

func TestOrRequiresAtLeastTwoOperands(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			t.Errorf("expected panic on single-operand Or")
		}
	}()
	Or(&recordingVisitor{})
}

func TestAndRequiresAtLeastTwoOperands(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			t.Errorf("expected panic on single-operand And")
		}
	}()
	And(&recordingVisitor{})
}

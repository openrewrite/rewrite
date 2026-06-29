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

	if cond.calls != 1 {
		t.Errorf("condition calls = %d, want 1", cond.calls)
	}
	if editor.calls != 1 {
		t.Errorf("editor calls = %d, want 1", editor.calls)
	}
}

func TestCheckSkipsEditorWhenConditionReturnsIdentity(t *testing.T) {
	cond := &recordingVisitor{}
	editor := &recordingVisitor{}

	wrapped := Check(cond, editor)
	wrapped.Visit(newSourceFile(), nil)

	if cond.calls != 1 {
		t.Errorf("condition calls = %d, want 1", cond.calls)
	}
	if editor.calls != 0 {
		t.Errorf("editor calls = %d, want 0 (gate did not match)", editor.calls)
	}
}

func TestOrShortCircuitsOnFirstMatch(t *testing.T) {
	matching := &markingVisitor{}
	nonMatching := &recordingVisitor{}
	editor := &recordingVisitor{}

	Check(Or(matching, nonMatching), editor).Visit(newSourceFile(), nil)

	if matching.calls != 1 {
		t.Errorf("matching calls = %d, want 1", matching.calls)
	}
	if nonMatching.calls != 0 {
		t.Errorf("nonMatching calls = %d, want 0 (Or should short-circuit)", nonMatching.calls)
	}
	if editor.calls != 1 {
		t.Errorf("editor calls = %d, want 1", editor.calls)
	}
}

func TestOrSkipsEditorWhenNoOperandMatches(t *testing.T) {
	a := &recordingVisitor{}
	b := &recordingVisitor{}
	editor := &recordingVisitor{}

	Check(Or(a, b), editor).Visit(newSourceFile(), nil)

	if a.calls != 1 || b.calls != 1 {
		t.Errorf("operand calls = %d / %d, want 1 / 1", a.calls, b.calls)
	}
	if editor.calls != 0 {
		t.Errorf("editor calls = %d, want 0", editor.calls)
	}
}

func TestAndRunsEditorOnlyWhenAllMatch(t *testing.T) {
	a := &markingVisitor{}
	b := &markingVisitor{}
	editor := &recordingVisitor{}
	Check(And(a, b), editor).Visit(newSourceFile(), nil)
	if editor.calls != 1 {
		t.Errorf("editor calls (all match) = %d, want 1", editor.calls)
	}

	editor2 := &recordingVisitor{}
	Check(And(a, &recordingVisitor{}), editor2).Visit(newSourceFile(), nil)
	if editor2.calls != 0 {
		t.Errorf("editor2 calls (one non-matching) = %d, want 0", editor2.calls)
	}
}

func TestNotInvertsMatch(t *testing.T) {
	editor1 := &recordingVisitor{}
	Check(Not(&markingVisitor{}), editor1).Visit(newSourceFile(), nil)
	if editor1.calls != 0 {
		t.Errorf("not(matching): editor calls = %d, want 0", editor1.calls)
	}

	editor2 := &recordingVisitor{}
	Check(Not(&recordingVisitor{}), editor2).Visit(newSourceFile(), nil)
	if editor2.calls != 1 {
		t.Errorf("not(non-matching): editor calls = %d, want 1", editor2.calls)
	}
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
	if editor.calls != 1 {
		t.Errorf("editor calls (bare RecipeRef) = %d, want 1", editor.calls)
	}
}

func TestRecipeRefWithLocalVisitorEvaluatesForReal(t *testing.T) {
	// Helpers like UsesMethod populate a native LocalVisitor so unit
	// tests without an active RPC connection still see real filtering.
	// An empty CompilationUnit has no method invocations, so the gate
	// fails and the editor is skipped.
	editor := &recordingVisitor{}
	Check(UsesMethod("*..* tostring(..)"), editor).Visit(newSourceFile(), nil)
	if editor.calls != 0 {
		t.Errorf("editor calls (RecipeRef with LocalVisitor) = %d, want 0", editor.calls)
	}
}

func TestHelpersPopulateLocalVisitor(t *testing.T) {
	// Spot-check that helpers bundle a TreeVisitor for offline eval.
	if HasSourcePath("**/*.go").LocalVisitor == nil {
		t.Errorf("HasSourcePath did not populate LocalVisitor")
	}
	if UsesMethod("*..* a(..)").LocalVisitor == nil {
		t.Errorf("UsesMethod did not populate LocalVisitor")
	}
	if UsesType("foo.Bar").LocalVisitor == nil {
		t.Errorf("UsesType did not populate LocalVisitor")
	}
	if FindMethods("*..* a(..)").LocalVisitor == nil {
		t.Errorf("FindMethods did not populate LocalVisitor")
	}
	if FindTypes("foo.Bar").LocalVisitor == nil {
		t.Errorf("FindTypes did not populate LocalVisitor")
	}
}

func TestHasSourcePathMatchesCompilationUnit(t *testing.T) {
	// given a real source file with a path
	editor := &recordingVisitor{}
	cu := &golang.CompilationUnit{SourcePath: "pkg/foo/bar.go"}

	// when the glob matches the path
	Check(HasSourcePath("**/*.go"), editor).Visit(cu, nil)

	// then the editor runs
	if editor.calls != 1 {
		t.Errorf("editor calls (matching path) = %d, want 1", editor.calls)
	}

	// and when the glob does not match
	editor2 := &recordingVisitor{}
	Check(HasSourcePath("**/*.java"), editor2).Visit(cu, nil)
	if editor2.calls != 0 {
		t.Errorf("editor calls (non-matching path) = %d, want 0", editor2.calls)
	}
}

func TestHasSourcePathMatchesGoMod(t *testing.T) {
	// given a go.mod root (a non-J SourceFile)
	editor := &recordingVisitor{}
	mod := &golang.GoMod{SourcePath: "go.mod"}

	// when the glob matches
	Check(HasSourcePath("**/go.mod"), editor).Visit(mod, nil)

	// then the editor runs even though GoMod is not a java.SourceFile
	if editor.calls != 1 {
		t.Errorf("editor calls (go.mod) = %d, want 1", editor.calls)
	}

	// and a non-matching glob must actually filter the GoMod out, rather
	// than the gate being bypassed because GoMod is not a java.SourceFile.
	editor2 := &recordingVisitor{}
	Check(HasSourcePath("**/*.go"), editor2).Visit(mod, nil)
	if editor2.calls != 0 {
		t.Errorf("editor calls (go.mod, non-matching) = %d, want 0", editor2.calls)
	}
}

func TestUsesMethodMatchesInvocationInTree(t *testing.T) {
	// given a compilation unit containing fmt.Println(...)
	mi := &java.MethodInvocation{
		Select: &java.RightPadded[java.Expression]{Element: &java.Identifier{Name: "fmt"}},
		Name:   &java.Identifier{Name: "Println"},
	}
	cu := &golang.CompilationUnit{
		Statements: []java.RightPadded[java.Statement]{{Element: mi}},
	}

	// when gating on the matching method pattern
	editor := &recordingVisitor{}
	Check(UsesMethod("fmt Println(..)"), editor).Visit(cu, nil)

	// then the editor runs
	if editor.calls != 1 {
		t.Errorf("editor calls (matching method) = %d, want 1", editor.calls)
	}

	// and a non-matching pattern skips the editor
	editor2 := &recordingVisitor{}
	Check(UsesMethod("fmt Printf(..)"), editor2).Visit(cu, nil)
	if editor2.calls != 0 {
		t.Errorf("editor calls (non-matching method) = %d, want 0", editor2.calls)
	}
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
	if editor.calls != 1 {
		t.Errorf("editor calls (matching type) = %d, want 1", editor.calls)
	}

	// and an unrelated type skips the editor
	editor2 := &recordingVisitor{}
	Check(UsesType("zipfile"), editor2).Visit(cu, nil)
	if editor2.calls != 0 {
		t.Errorf("editor calls (non-matching type) = %d, want 0", editor2.calls)
	}
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

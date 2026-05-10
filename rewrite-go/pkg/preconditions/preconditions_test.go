/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// newSourceFile returns a fresh CompilationUnit pointer to use as the
// source-file sentinel. Each call returns a distinct identity so a
// "marking" visitor can return a different value.
func newSourceFile() tree.Tree {
	return &tree.CompilationUnit{}
}

// recordingVisitor is a TreeVisitor that returns the same tree, recording
// each call. Simulates a precondition that does NOT match.
type recordingVisitor struct{ calls int }

func (r *recordingVisitor) Visit(t tree.Tree, _ any) tree.Tree {
	r.calls++
	return t
}

// markingVisitor is a TreeVisitor that returns a *different* tree
// (any non-identity value), simulating a precondition that DOES match
// (e.g. by adding a SearchResult marker).
type markingVisitor struct{ calls int }

func (m *markingVisitor) Visit(t tree.Tree, _ any) tree.Tree {
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

func TestRecipeRefOperandShortCircuitsToMatch(t *testing.T) {
	editor := &recordingVisitor{}
	Check(UsesMethod("*..* tostring(..)"), editor).Visit(newSourceFile(), nil)
	if editor.calls != 1 {
		t.Errorf("editor calls (RecipeRef in-process) = %d, want 1", editor.calls)
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

/*
 * Copyright 2025 the original author or authors.
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
	"testing"

	"github.com/google/uuid"

	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func TestParseAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func greet() string {
				name = "world"
				return name
			}
		`))
}

func TestParseShortVarDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func hello() {
				x := 1
			}
		`))
}

func TestParseCompoundAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func add() {
				x += 1
			}
		`))
}

func TestParseIncrement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func inc() {
				x++
			}
		`))
}

func TestParseDecrement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func dec() {
				x--
			}
		`))
}

func TestParseMultiAssignment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x, y = 1, 2
			}
		`))
}

func TestParseMultiShortVarDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				x, y := 1, 2
			}
		`))
}

func TestParseMultiAssignFromFunc(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				val, err := divide(10, 2)
			}
		`))
}

// ident builds a bare identifier expression for the visitor fixtures.
func ident(name string) java.Expression {
	return &java.Identifier{ID: uuid.New(), Name: name}
}

// callRecorder records every method invocation it visits, by name.
type callRecorder struct {
	visitor.GoVisitor
	visited []string
}

func (v *callRecorder) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	v.visited = append(v.visited, mi.Name.Name)
	return v.GoVisitor.VisitMethodInvocation(mi, p)
}

// TestMultiAssignmentVisitsRHSMethodInvocation confirms that a method
// invocation on the RHS of a multi-target assignment (`data, err := f()`)
// is actually visited. Regression test: VisitMultiAssignment previously
// only walked Prefix/Markers and never recursed into Values, so any
// recipe relying on VisitMethodInvocation silently skipped the call.
func TestMultiAssignmentVisitsRHSMethodInvocation(t *testing.T) {
	// given
	call := &java.MethodInvocation{
		ID:        uuid.New(),
		Name:      &java.Identifier{ID: uuid.New(), Name: "ReadAll"},
		Arguments: java.Container[java.Expression]{},
	}
	ma := &golang.MultiAssignment{
		ID:        uuid.New(),
		Variables: []java.RightPadded[java.Expression]{{Element: ident("data")}, {Element: ident("err")}},
		Values:    []java.RightPadded[java.Expression]{{Element: call}},
	}

	// when
	rec := &callRecorder{}
	v := visitor.Init(rec)
	v.Visit(ma, nil)

	// then
	if len(rec.visited) != 1 || rec.visited[0] != "ReadAll" {
		t.Errorf("expected RHS method invocation %q to be visited, got %v", "ReadAll", rec.visited)
	}
}

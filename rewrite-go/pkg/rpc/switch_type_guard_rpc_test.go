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

package rpc

import (
	"testing"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// Covers the `switch v := x.(type)` shape, where the selector is an Assignment
// whose value is the TypeCast.
func TestSwitchTypeGuardRoundTrip_AssignmentSelector(t *testing.T) {
	swID := uuid.New()
	asgID := uuid.New()
	tcID := uuid.New()

	// The `(type)` wrapper (TypeCast.Clazz) ships as NO_CHANGE because the recipe never touches it.
	clazz := &java.ControlParentheses{
		ID:   uuid.New(),
		Tree: java.RightPadded[java.Expression]{Element: makeIdent("type")},
	}
	variable := makeIdent("v")

	// before: `switch v := x.(type) { }`.
	beforeTag := &java.RightPadded[java.Expression]{
		Element: &java.Assignment{
			ID:       asgID,
			Variable: variable,
			Value: java.LeftPadded[java.Expression]{
				Before:  java.Space{Whitespace: " "},
				Element: &java.TypeCast{ID: tcID, Expr: makeIdent("x"), Clazz: clazz},
			},
		},
		After: java.Space{Whitespace: " "},
	}
	body := &java.Block{ID: uuid.New(), End: java.Space{Whitespace: "\n"}}
	before := &java.Switch{ID: swID, Tag: beforeTag, Body: body}

	// after: the recipe edited the cast operand (x -> y), so the selector is a CHANGE
	// while the `(type)` wrapper (Clazz, same pointer) stays NO_CHANGE.
	after := &java.Switch{
		ID: swID,
		Tag: &java.RightPadded[java.Expression]{
			Element: &java.Assignment{
				ID:       asgID,
				Variable: variable, // same pointer -> NO_CHANGE
				Value: java.LeftPadded[java.Expression]{
					Before:  java.Space{Whitespace: " "},
					Element: &java.TypeCast{ID: tcID, Expr: makeIdent("y"), Clazz: clazz},
				},
			},
			After: java.Space{Whitespace: " "},
		},
		Body: body, // same pointer -> NO_CHANGE
	}

	// seed mirrors the baseline a real session holds from a prior GET_OBJECT.
	seed := &java.Switch{ID: swID, Tag: beforeTag, Body: body}

	got := roundTripNodeWithBefore(t, after, before, seed).(*java.Switch)

	if got.Tag == nil {
		t.Fatal("Tag: got nil, want the type-switch selector")
	}
	assign, ok := got.Tag.Element.(*java.Assignment)
	if !ok {
		t.Fatalf("Tag.Element: got %T, want *Assignment", got.Tag.Element)
	}
	tc, ok := assign.Value.Element.(*java.TypeCast)
	if !ok {
		t.Fatalf("Assignment.Value.Element: got %T, want *TypeCast", assign.Value.Element)
	}
	if tc.Clazz == nil {
		t.Fatal("TypeCast.Clazz: got nil, want the `(type)` ControlParentheses")
	}
	if id, ok := tc.Clazz.Tree.Element.(*java.Identifier); !ok || id.Name != "type" {
		t.Errorf("TypeCast.Clazz.Tree.Element: got %+v, want Identifier{type}", tc.Clazz.Tree.Element)
	}

	// Printing exercises the VisitSwitch -> VisitAssignment -> VisitTypeCast ->
	// VisitControlParentheses path from the reported panic.
	if out := printer.Print(got); out == "" {
		t.Error("printed output: got empty string, want rendered switch")
	}
}

// Covers `switch f(); v := x.(type)`, where the type switch is wrapped in a
// golang.StatementWithInit and the init clause is unchanged.
func TestSwitchTypeGuardRoundTrip_WithInitClause(t *testing.T) {
	swiID, swID, asgID, tcID := uuid.New(), uuid.New(), uuid.New(), uuid.New()
	clazz := &java.ControlParentheses{
		ID:   uuid.New(),
		Tree: java.RightPadded[java.Expression]{Element: makeIdent("type")},
	}
	variable := makeIdent("v")
	body := &java.Block{ID: uuid.New(), End: java.Space{Whitespace: "\n"}}
	// The init clause `f()` stays unchanged across the diff.
	init := java.RightPadded[java.Statement]{Element: makeMethodInvocation(), After: java.Space{Whitespace: " "}}

	mkSwitch := func(cast java.Expression) *java.Switch {
		return &java.Switch{
			ID: swID,
			Tag: &java.RightPadded[java.Expression]{
				Element: &java.Assignment{
					ID:       asgID,
					Variable: variable,
					Value: java.LeftPadded[java.Expression]{
						Before:  java.Space{Whitespace: " "},
						Element: &java.TypeCast{ID: tcID, Expr: cast, Clazz: clazz},
					},
				},
				After: java.Space{Whitespace: " "},
			},
			Body: body,
		}
	}

	// The recipe edited the cast operand (x -> y), so the inner selector is a CHANGE
	// while the init clause and the `(type)` wrapper stay NO_CHANGE.
	before := &golang.StatementWithInit{ID: swiID, Init: init, Statement: mkSwitch(makeIdent("x"))}
	after := &golang.StatementWithInit{ID: swiID, Init: init, Statement: mkSwitch(makeIdent("y"))}
	seed := &golang.StatementWithInit{ID: swiID, Init: init, Statement: mkSwitch(makeIdent("x"))}

	got := roundTripNodeWithBefore(t, after, before, seed).(*golang.StatementWithInit)

	sw, ok := got.Statement.(*java.Switch)
	if !ok {
		t.Fatalf("Statement: got %T, want *Switch", got.Statement)
	}
	asg, ok := sw.Tag.Element.(*java.Assignment)
	if !ok {
		t.Fatalf("Tag.Element: got %T, want *Assignment", sw.Tag.Element)
	}
	tc, ok := asg.Value.Element.(*java.TypeCast)
	if !ok {
		t.Fatalf("Assignment.Value.Element: got %T, want *TypeCast", asg.Value.Element)
	}
	if tc.Clazz == nil {
		t.Fatal("TypeCast.Clazz: got nil, want the `(type)` ControlParentheses")
	}
	if id, ok := tc.Clazz.Tree.Element.(*java.Identifier); !ok || id.Name != "type" {
		t.Errorf("TypeCast.Clazz.Tree.Element: got %+v, want Identifier{type}", tc.Clazz.Tree.Element)
	}

	// Printing exercises the StatementWithInit -> Switch -> Assignment -> TypeCast ->
	// ControlParentheses path.
	if out := printer.Print(got); out == "" {
		t.Error("printed output: got empty string, want rendered switch")
	}
}

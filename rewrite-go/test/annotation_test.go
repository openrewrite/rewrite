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
	"testing"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Step 1 of AnnotationService rollout: tree.Annotation type + visitor +
// printer entry. No parser wiring yet — these tests construct
// annotations programmatically and verify the visitor walks them and
// the printer emits the expected struct-tag form.

// newJSONTagAnnotation builds an Annotation that represents a single
// struct-tag pair `json:"name"` for the test fixtures below.
func newJSONTagAnnotation(key, value string) *tree.Annotation {
	return &tree.Annotation{
		ID:             uuid.New(),
		AnnotationType: &tree.Identifier{ID: uuid.New(), Name: key},
		Arguments: &tree.Container[tree.Expression]{
			Elements: []tree.RightPadded[tree.Expression]{
				{Element: &tree.Literal{
					ID:     uuid.New(),
					Source: `"` + value + `"`,
					Value:  value,
					Kind:   tree.StringLiteral,
				}},
			},
		},
	}
}

func TestAnnotation_PrintsBasicTagShape(t *testing.T) {
	ann := newJSONTagAnnotation("json", "name")
	out := printer.Print(ann)
	want := `json:"name"`
	if out != want {
		t.Errorf("got %q, want %q", out, want)
	}
}

func TestAnnotation_PrintsWithoutArguments(t *testing.T) {
	// An Annotation with nil Arguments should print just the type
	// expression (mirrors Java's bare `@Override`-style annotation).
	ann := &tree.Annotation{
		ID:             uuid.New(),
		AnnotationType: &tree.Identifier{ID: uuid.New(), Name: "go:noinline"},
	}
	out := printer.Print(ann)
	want := `go:noinline`
	if out != want {
		t.Errorf("got %q, want %q", out, want)
	}
}

func TestAnnotation_PrintsPrefixWhitespace(t *testing.T) {
	// The leading space (between the previous syntax and the annotation)
	// lives on the Annotation's Prefix.
	ann := newJSONTagAnnotation("validate", "required")
	ann.Prefix = tree.Space{Whitespace: " "}
	out := printer.Print(ann)
	want := ` validate:"required"`
	if out != want {
		t.Errorf("got %q, want %q", out, want)
	}
}

func TestAnnotation_VisitorRoundtripIdentity(t *testing.T) {
	// A no-op visitor over an Annotation should produce a tree whose
	// printed form is identical to the input's.
	ann := newJSONTagAnnotation("json", "user_id")
	ann.Prefix = tree.Space{Whitespace: " "}

	v := visitor.Init(&visitor.GoVisitor{})
	out := v.Visit(ann, nil)
	if out == nil {
		t.Fatal("visitor returned nil")
	}
	got := printer.Print(out.(tree.Tree))
	want := ` json:"user_id"`
	if got != want {
		t.Errorf("got %q, want %q", got, want)
	}
}

func TestAnnotation_VisitorReachesAnnotationType(t *testing.T) {
	// Custom visitor that renames the AnnotationType identifier
	// confirms the visitor recurses into the type child.
	ann := newJSONTagAnnotation("json", "x")
	v := visitor.Init(&renamingVisitor{from: "json", to: "yaml"})
	out := v.Visit(ann, nil).(*tree.Annotation)

	got := printer.Print(out)
	want := `yaml:"x"`
	if got != want {
		t.Errorf("got %q, want %q", got, want)
	}
}

func TestAnnotation_VisitorReachesArguments(t *testing.T) {
	// Custom visitor that mutates the Literal value in Arguments
	// confirms the visitor recurses into the arguments container.
	ann := newJSONTagAnnotation("json", "x")
	v := visitor.Init(&literalRewriter{want: "x", repl: "y"})
	out := v.Visit(ann, nil).(*tree.Annotation)

	got := printer.Print(out)
	want := `json:"y"`
	if got != want {
		t.Errorf("got %q, want %q", got, want)
	}
}

type renamingVisitor struct {
	visitor.GoVisitor
	from, to string
}

func (v *renamingVisitor) VisitIdentifier(id *tree.Identifier, p any) tree.J {
	id = v.GoVisitor.VisitIdentifier(id, p).(*tree.Identifier)
	if id.Name == v.from {
		c := *id
		c.Name = v.to
		return &c
	}
	return id
}

type literalRewriter struct {
	visitor.GoVisitor
	want, repl string
}

func (v *literalRewriter) VisitLiteral(lit *tree.Literal, p any) tree.J {
	lit = v.GoVisitor.VisitLiteral(lit, p).(*tree.Literal)
	if s, ok := lit.Value.(string); ok && s == v.want {
		c := *lit
		c.Value = v.repl
		c.Source = `"` + v.repl + `"`
		return &c
	}
	return lit
}

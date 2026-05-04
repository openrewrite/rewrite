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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Step 4 of AnnotationService rollout: the public service surface.
// Recipes use AllAnnotations / IsAnnotatedWith / FindAnnotations to
// inspect, and AddAnnotationVisitor / RemoveAnnotationVisitor (via
// DoAfterVisit) to mutate.

func TestAnnotationService_Registered(t *testing.T) {
	svc := recipe.Service[*golang.AnnotationService](nil)
	if svc == nil {
		t.Fatal("expected AnnotationService to be registered")
	}
}

func TestAnnotationService_IsAnnotatedWith_StructTag(t *testing.T) {
	src := "package main\n\ntype User struct {\n\tName string `json:\"name\"`\n}\n"
	field := parseStructAndFindField(t, src, "Name")
	svc := &golang.AnnotationService{}
	if !svc.IsAnnotatedWith(field, "json") {
		t.Errorf("expected struct field with json tag to match \"json\"")
	}
	if svc.IsAnnotatedWith(field, "validate") {
		t.Errorf("did not expect match for absent tag \"validate\"")
	}
}

func TestAnnotationService_IsAnnotatedWith_Directive(t *testing.T) {
	src := "package main\n\n//go:noinline\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	svc := &golang.AnnotationService{}
	if !svc.IsAnnotatedWith(md, "go:noinline") {
		t.Errorf("expected method with go:noinline to match")
	}
}

func TestAnnotationService_IsAnnotatedWith_WildcardPrefix(t *testing.T) {
	src := "package main\n\n//go:noinline\n//go:nosplit\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	svc := &golang.AnnotationService{}
	if !svc.IsAnnotatedWith(md, "go:*") {
		t.Errorf("expected method with go: directives to match \"go:*\"")
	}
	if !svc.IsAnnotatedWith(md, "*") {
		t.Errorf("expected universal match \"*\" to succeed")
	}
	if svc.IsAnnotatedWith(md, "lint:*") {
		t.Errorf("did not expect match for \"lint:*\" on go-only directives")
	}
}

func TestAnnotationService_FindAnnotations(t *testing.T) {
	src := "package main\n\ntype User struct {\n\tEmail string `json:\"email\" db:\"email_address\" validate:\"required\"`\n}\n"
	field := parseStructAndFindField(t, src, "Email")
	svc := &golang.AnnotationService{}

	jsonAnns := svc.FindAnnotations(field, "json")
	if len(jsonAnns) != 1 {
		t.Fatalf("expected 1 json annotation, got %d", len(jsonAnns))
	}
	if v, _ := jsonAnns[0].Arguments.Elements[0].Element.(*tree.Literal).Value.(string); v != "email" {
		t.Errorf("json value: got %q, want \"email\"", v)
	}
}

func TestAnnotationService_AllAnnotations_ViaCursor(t *testing.T) {
	src := "package main\n\n//go:noinline\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	svc := &golang.AnnotationService{}

	// Build a cursor positioned AT the MethodDeclaration.
	c := buildCursor(md)
	anns := svc.AllAnnotations(c)
	if len(anns) != 1 {
		t.Fatalf("AllAnnotations: got %d, want 1", len(anns))
	}
	if anns[0].AnnotationType.(*tree.Identifier).Name != "go:noinline" {
		t.Errorf("annotation: got %+v", anns[0].AnnotationType)
	}
}

func TestAnnotationService_AddAnnotationVisitor_OnFunc(t *testing.T) {
	src := "package main\n\nfunc slow() { _ = 1 }\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}

	svc := &golang.AnnotationService{}
	ann := &tree.Annotation{
		ID:             uuid.New(),
		Prefix:         tree.Space{Whitespace: "\n"},
		AnnotationType: &tree.Identifier{ID: uuid.New(), Name: "go:noinline"},
	}
	v := svc.AddAnnotationVisitor(func(t tree.Tree) bool {
		md, ok := t.(*tree.MethodDeclaration)
		return ok && md.Name != nil && md.Name.Name == "slow"
	}, ann)

	out := v.Visit(cu, nil).(tree.Tree)

	want := "package main\n\n//go:noinline\nfunc slow() { _ = 1 }\n"
	if got := printer.Print(out); got != want {
		t.Errorf("got %q, want %q", got, want)
	}
}

func TestAnnotationService_RemoveAnnotationVisitor(t *testing.T) {
	// Start with two go: directives, remove one specifically.
	src := "package main\n\n//go:noinline\n//go:nosplit\nfunc slow() {}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}

	svc := &golang.AnnotationService{}
	v := svc.RemoveAnnotationVisitor("go:nosplit")
	out := v.Visit(cu, nil).(tree.Tree)

	want := "package main\n\n//go:noinline\nfunc slow() {}\n"
	if got := printer.Print(out); got != want {
		t.Errorf("got %q, want %q", got, want)
	}
}

func TestAnnotationService_Matches_ViaCursor(t *testing.T) {
	src := "package main\n\n//go:noinline\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	c := buildCursor(md)
	svc := &golang.AnnotationService{}
	if !svc.Matches(c, golang.NewAnnotationMatcher("go:noinline")) {
		t.Error("expected matcher \"go:noinline\" to match")
	}
	if svc.Matches(c, golang.NewAnnotationMatcher("go:nosplit")) {
		t.Error("did not expect matcher \"go:nosplit\" to match")
	}
}

// buildCursor wraps a node in a single-element cursor for testing
// AnnotationService.AllAnnotations / Matches without going through the
// full visitor dispatch.
func buildCursor(t tree.Tree) *visitor.Cursor {
	return visitor.NewCursor(nil, t)
}

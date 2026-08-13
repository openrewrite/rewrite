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

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Step 4 of AnnotationService rollout: the public service surface.
// Recipes use AllAnnotations / IsAnnotatedWith / FindAnnotations to
// inspect, and AddAnnotationVisitor / RemoveAnnotationVisitor (via
// DoAfterVisit) to mutate.

func TestAnnotationService_Registered(t *testing.T) {
	svc := recipe.Service[*recipes.AnnotationService](nil)
	require.NotNil(t, svc)
}

func TestAnnotationService_IsAnnotatedWith_StructTag(t *testing.T) {
	src := "package main\n\ntype User struct {\n\tName string `json:\"name\"`\n}\n"
	field := parseStructAndFindField(t, src, "Name")
	svc := &recipes.AnnotationService{}
	assert.True(t, svc.IsAnnotatedWith(field, "json"))
	assert.False(t, svc.IsAnnotatedWith(field, "validate"))
}

func TestAnnotationService_IsAnnotatedWith_Directive(t *testing.T) {
	src := "package main\n\n//go:noinline\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	svc := &recipes.AnnotationService{}
	assert.True(t, svc.IsAnnotatedWith(md, "go:noinline"))
}

func TestAnnotationService_IsAnnotatedWith_WildcardPrefix(t *testing.T) {
	src := "package main\n\n//go:noinline\n//go:nosplit\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	svc := &recipes.AnnotationService{}
	assert.True(t, svc.IsAnnotatedWith(md, "go:*"))
	assert.True(t, svc.IsAnnotatedWith(md, "*"))
	assert.False(t, svc.IsAnnotatedWith(md, "lint:*"))
}

func TestAnnotationService_FindAnnotations(t *testing.T) {
	src := "package main\n\ntype User struct {\n\tEmail string `json:\"email\" db:\"email_address\" validate:\"required\"`\n}\n"
	field := parseStructAndFindField(t, src, "Email")
	svc := &recipes.AnnotationService{}

	jsonAnns := svc.FindAnnotations(field, "json")
	require.Len(t, jsonAnns, 1)
	if v, _ := jsonAnns[0].Arguments.Elements[0].Element.(*java.Literal).Value.(string); v != "email" {
		t.Errorf("json value: got %q, want \"email\"", v)
	}
}

func TestAnnotationService_AllAnnotations_ViaCursor(t *testing.T) {
	src := "package main\n\n//go:noinline\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	svc := &recipes.AnnotationService{}

	c := buildCursor(md)
	anns := svc.AllAnnotations(c)
	require.Len(t, anns, 1)
	if anns[0].AnnotationType.(*java.Identifier).Name != "go:noinline" {
		t.Errorf("annotation: got %+v", anns[0].AnnotationType)
	}
}

func TestAnnotationService_AddAnnotationVisitor_OnFunc(t *testing.T) {
	src := "package main\n\nfunc slow() { _ = 1 }\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	require.NoError(t, err)

	svc := &recipes.AnnotationService{}
	ann := &java.Annotation{
		ID:             uuid.New(),
		Prefix:         java.Space{Whitespace: "\n"},
		AnnotationType: &java.Identifier{ID: uuid.New(), Name: "go:noinline"},
	}
	v := svc.AddAnnotationVisitor(func(t java.Tree) bool {
		md, ok := t.(*java.MethodDeclaration)
		return ok && md.Name != nil && md.Name.Name == "slow"
	}, ann)

	out := v.Visit(cu, nil).(java.Tree)

	want := "package main\n\n//go:noinline\nfunc slow() { _ = 1 }\n"
	if got := printer.Print(out); got != want {
		t.Errorf("got %q, want %q", got, want)
	}
}

func TestAnnotationService_RemoveAnnotationVisitor(t *testing.T) {
	// Start with two go: directives, remove one specifically.
	src := "package main\n\n//go:noinline\n//go:nosplit\nfunc slow() {}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	require.NoError(t, err)

	svc := &recipes.AnnotationService{}
	v := svc.RemoveAnnotationVisitor("go:nosplit")
	out := v.Visit(cu, nil).(java.Tree)

	want := "package main\n\n//go:noinline\nfunc slow() {}\n"
	if got := printer.Print(out); got != want {
		t.Errorf("got %q, want %q", got, want)
	}
}

func TestAnnotationService_Matches_ViaCursor(t *testing.T) {
	src := "package main\n\n//go:noinline\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	c := buildCursor(md)
	svc := &recipes.AnnotationService{}
	assert.True(t, svc.Matches(c, recipes.NewAnnotationMatcher("go:noinline")))
	assert.False(t, svc.Matches(c, recipes.NewAnnotationMatcher("go:nosplit")))
}

// buildCursor wraps a node in a single-element cursor for testing
// AnnotationService.AllAnnotations / Matches without going through the
// full visitor dispatch.
func buildCursor(t java.Tree) *visitor.Cursor {
	return visitor.NewCursor(nil, t)
}

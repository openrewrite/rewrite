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

package parser_test

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// firstAssignRHS parses src and returns the RHS expression of the first
// short variable declaration (`:=`) found in the first function body.
func firstAssignRHS(t *testing.T, src string) java.Expression {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("g.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	for _, rp := range cu.Statements {
		md, ok := rp.Element.(*java.MethodDeclaration)
		if !ok || md.Body == nil {
			continue
		}
		for _, st := range md.Body.Statements {
			if a, ok := st.Element.(*java.Assignment); ok {
				return a.Value.Element
			}
		}
	}
	t.Fatalf("no `:=` assignment found")
	return nil
}

// A call to a generic function with an explicit type argument — `Map[int](42)`
// — must be modeled as a J.MethodInvocation whose callee Name is "Map", whose
// type argument `int` is captured in TypeParameters, and whose MethodType is
// attributed. It must NOT be modeled as a J.ArrayAccess in the Select slot with
// an empty Name (the pre-fix behavior).
func TestGenericFuncCallSingleTypeArg(t *testing.T) {
	// given
	src := "package main\n\nfunc Map[T any](x T) T {\n\treturn x\n}\n\nfunc main() {\n\tv := Map[int](42)\n\t_ = v\n}\n"

	// when
	rhs := firstAssignRHS(t, src)

	// then
	mi, ok := rhs.(*java.MethodInvocation)
	if !ok {
		t.Fatalf("expected RHS to be *java.MethodInvocation, got %T", rhs)
	}
	if mi.Select != nil {
		t.Errorf("expected Select to be nil for a free generic function call, got %T", mi.Select.Element)
	}
	if mi.Name == nil || mi.Name.Name != "Map" {
		t.Errorf("expected Name == %q, got %q", "Map", nameOrEmpty(mi.Name))
	}
	if mi.TypeParameters == nil {
		t.Fatalf("expected TypeParameters to be non-nil")
	}
	if len(mi.TypeParameters.Elements) != 1 {
		t.Fatalf("expected 1 type argument, got %d", len(mi.TypeParameters.Elements))
	}
	if id, ok := mi.TypeParameters.Elements[0].Element.(*java.Identifier); !ok || id.Name != "int" {
		t.Errorf("expected type argument Identifier(int), got %T", mi.TypeParameters.Elements[0].Element)
	}
	if len(mi.Arguments.Elements) != 1 {
		t.Errorf("expected 1 call argument, got %d", len(mi.Arguments.Elements))
	}
	if mi.MethodType == nil {
		t.Errorf("expected MethodType to be attributed")
	}

	assertRoundTrip(t, src)
}

// `Pair[int, string](1, "x")` — a generic call with multiple explicit type
// arguments — must capture both type arguments in TypeParameters.
func TestGenericFuncCallMultipleTypeArgs(t *testing.T) {
	// given
	callSrc := "package main\n\nfunc Pair[A any, B any](a A, b B) {\n}\n\nfunc main() {\n\tPair[int, string](1, \"x\")\n}\n"

	// when
	ccu, err := parser.NewGoParser().Parse("g.go", callSrc)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}

	// then
	var mi *java.MethodInvocation
	for _, rp := range ccu.Statements {
		md, ok := rp.Element.(*java.MethodDeclaration)
		if !ok || md.Body == nil || md.Name == nil || md.Name.Name != "main" {
			continue
		}
		for _, st := range md.Body.Statements {
			if m, ok := st.Element.(*java.MethodInvocation); ok {
				mi = m
			}
		}
	}
	if mi == nil {
		t.Fatalf("no MethodInvocation found")
	}
	if mi.Name == nil || mi.Name.Name != "Pair" {
		t.Errorf("expected Name == %q, got %q", "Pair", nameOrEmpty(mi.Name))
	}
	if mi.TypeParameters == nil || len(mi.TypeParameters.Elements) != 2 {
		t.Fatalf("expected 2 type arguments, got %v", mi.TypeParameters)
	}
	assertRoundTrip(t, callSrc)
}

// Calling a function value stored in a slice — `funcs[0]()` — is ordinary
// indexing, NOT a generic instantiation. The Select must remain a J.ArrayAccess
// and TypeParameters must stay nil.
func TestFuncValueIndexCallIsNotGeneric(t *testing.T) {
	// given
	src := "package main\n\nfunc main() {\n\tfuncs := []func() int{}\n\tfuncs[0]()\n}\n"

	// when
	cu, err := parser.NewGoParser().Parse("g.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}

	// then
	var mi *java.MethodInvocation
	for _, rp := range cu.Statements {
		md, ok := rp.Element.(*java.MethodDeclaration)
		if !ok || md.Body == nil {
			continue
		}
		for _, st := range md.Body.Statements {
			if m, ok := st.Element.(*java.MethodInvocation); ok {
				mi = m
			}
		}
	}
	if mi == nil {
		t.Fatalf("no MethodInvocation found")
	}
	if mi.TypeParameters != nil {
		t.Errorf("expected TypeParameters to be nil for func-value index call")
	}
	if mi.Select == nil {
		t.Fatalf("expected Select to be present")
	}
	if _, ok := mi.Select.Element.(*java.ArrayAccess); !ok {
		t.Errorf("expected Select to be *java.ArrayAccess, got %T", mi.Select.Element)
	}
	assertRoundTrip(t, src)
}

func nameOrEmpty(id *java.Identifier) string {
	if id == nil {
		return ""
	}
	return id.Name
}

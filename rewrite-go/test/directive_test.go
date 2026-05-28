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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// Step 3 of AnnotationService rollout: the parser extracts `//go:`
// and `//lint:` directives from leading comments above top-level
// MethodDeclaration / TypeDecl / VariableDeclarations into
// LeadingAnnotations. The printer reassembles them as `//<name> <args>`
// lines on roundtrip.

func parseAndFindMethod(t *testing.T, src, name string) *tree.MethodDeclaration {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	for _, rp := range cu.Statements {
		if md, ok := rp.Element.(*tree.MethodDeclaration); ok && md.Name != nil && md.Name.Name == name {
			return md
		}
	}
	t.Fatalf("method %q not found", name)
	return nil
}

func parseAndFindType(t *testing.T, src, name string) *tree.TypeDecl {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	for _, rp := range cu.Statements {
		if td, ok := rp.Element.(*tree.TypeDecl); ok && td.Name != nil && td.Name.Name == name {
			return td
		}
	}
	t.Fatalf("type %q not found", name)
	return nil
}

func parseAndFindVar(t *testing.T, src string) *tree.VariableDeclarations {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	for _, rp := range cu.Statements {
		if vd, ok := rp.Element.(*tree.VariableDeclarations); ok {
			return vd
		}
	}
	t.Fatalf("no var declaration found")
	return nil
}

func TestDirective_BareGoNoinline(t *testing.T) {
	src := "package main\n\n//go:noinline\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	if got := len(md.LeadingAnnotations); got != 1 {
		t.Fatalf("LeadingAnnotations: got %d, want 1", got)
	}
	ann := md.LeadingAnnotations[0]
	if id, _ := ann.AnnotationType.(*tree.Identifier); id == nil || id.Name != "go:noinline" {
		t.Errorf("AnnotationType: got %+v, want Identifier{Name:\"go:noinline\"}", ann.AnnotationType)
	}
	if ann.Arguments != nil {
		t.Errorf("Arguments: got %+v, want nil for bare directive", ann.Arguments)
	}
}

func TestDirective_GoLinknameWithArgs(t *testing.T) {
	src := "package main\n\n//go:linkname x runtime.x\nvar x int = 1\n"
	vd := parseAndFindVar(t, src)
	if got := len(vd.LeadingAnnotations); got != 1 {
		t.Fatalf("LeadingAnnotations: got %d, want 1", got)
	}
	ann := vd.LeadingAnnotations[0]
	if id, _ := ann.AnnotationType.(*tree.Identifier); id == nil || id.Name != "go:linkname" {
		t.Errorf("AnnotationType: got %+v, want Identifier{Name:\"go:linkname\"}", ann.AnnotationType)
	}
	if ann.Arguments == nil || len(ann.Arguments.Elements) != 1 {
		t.Fatalf("Arguments: got %+v, want one Literal", ann.Arguments)
	}
	lit, _ := ann.Arguments.Elements[0].Element.(*tree.Literal)
	if lit == nil || lit.Source != "x runtime.x" {
		t.Errorf("Args: got %+v, want \"x runtime.x\"", lit)
	}
}

func TestDirective_MultipleDirectivesOnFunc(t *testing.T) {
	src := "package main\n\n//go:noinline\n//go:nosplit\nfunc slow() {}\n"
	md := parseAndFindMethod(t, src, "slow")
	if got := len(md.LeadingAnnotations); got != 2 {
		t.Fatalf("LeadingAnnotations: got %d, want 2", got)
	}
	if md.LeadingAnnotations[0].AnnotationType.(*tree.Identifier).Name != "go:noinline" {
		t.Errorf("[0]: got %+v", md.LeadingAnnotations[0].AnnotationType)
	}
	if md.LeadingAnnotations[1].AnnotationType.(*tree.Identifier).Name != "go:nosplit" {
		t.Errorf("[1]: got %+v", md.LeadingAnnotations[1].AnnotationType)
	}
}

func TestDirective_LintIgnoreOnType(t *testing.T) {
	src := "package main\n\n//lint:ignore U1000 unused but kept\ntype Foo struct{}\n"
	td := parseAndFindType(t, src, "Foo")
	if got := len(td.LeadingAnnotations); got != 1 {
		t.Fatalf("LeadingAnnotations: got %d, want 1", got)
	}
	ann := td.LeadingAnnotations[0]
	if ann.AnnotationType.(*tree.Identifier).Name != "lint:ignore" {
		t.Errorf("AnnotationType: got %+v", ann.AnnotationType)
	}
	if ann.Arguments == nil {
		t.Fatal("Arguments: got nil")
	}
	if got := ann.Arguments.Elements[0].Element.(*tree.Literal).Source; got != "U1000 unused but kept" {
		t.Errorf("Args: got %q, want %q", got, "U1000 unused but kept")
	}
}

func TestDirective_RegularCommentNotExtracted(t *testing.T) {
	src := "package main\n\n// regular doc comment\nfunc f() {}\n"
	md := parseAndFindMethod(t, src, "f")
	if got := len(md.LeadingAnnotations); got != 0 {
		t.Errorf("LeadingAnnotations: got %d, want 0 — regular doc comments stay as comments", got)
	}
}

func TestDirective_DirectivePrecedingRegularComment(t *testing.T) {
	// Directive first, regular comment after, then func: the directive
	// is extracted, the regular comment stays in the func's Prefix.
	src := "package main\n\n//go:noinline\n// regular doc\nfunc f() {}\n"
	md := parseAndFindMethod(t, src, "f")
	if got := len(md.LeadingAnnotations); got != 1 {
		t.Fatalf("LeadingAnnotations: got %d, want 1", got)
	}
	if got := len(md.Prefix.Comments); got != 1 {
		t.Errorf("Prefix.Comments: got %d, want 1 (the regular doc)", got)
	}
}

func TestDirective_RegularCommentBeforeDirectiveStops(t *testing.T) {
	// When a regular comment appears BEFORE the directive, extraction
	// stops at the regular comment — the directive stays in Prefix.
	src := "package main\n\n// regular doc\n//go:noinline\nfunc f() {}\n"
	md := parseAndFindMethod(t, src, "f")
	if got := len(md.LeadingAnnotations); got != 0 {
		t.Errorf("LeadingAnnotations: got %d, want 0 (extraction halts at first non-directive)", got)
	}
}

func TestDirective_RoundtripFunc(t *testing.T) {
	src := "package main\n\n//go:noinline\n//go:nosplit\nfunc slow() {}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	if got := printer.Print(cu); got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
}

func TestDirective_RoundtripType(t *testing.T) {
	src := "package main\n\n//go:generate go run gen.go\ntype Foo struct{}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	if got := printer.Print(cu); got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
}

func TestDirective_RoundtripVar(t *testing.T) {
	src := "package main\n\n//go:linkname x runtime.x\nvar x int = 1\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	if got := printer.Print(cu); got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
}

func TestDirective_RoundtripMixed(t *testing.T) {
	src := "package main\n\n//go:noinline\n//go:nosplit\nfunc slow() { _ = 1 }\n\n//go:generate go run gen.go\ntype Foo struct{}\n\n//go:linkname x runtime.x\nvar x int = 1\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	if got := printer.Print(cu); got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
}

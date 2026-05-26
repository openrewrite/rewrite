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

// Step 2 of AnnotationService rollout: the parser splits struct field
// tags into one Annotation per `key:"value"` pair on
// VariableDeclarations.LeadingAnnotations. The printer reassembles the
// run, wrapping it in backticks. Roundtrip on gofmt'd input is exact
// (Option 1 in the design discussion: lossy on inner-padding only).

func parseStructAndFindField(t *testing.T, src, fieldName string) *tree.VariableDeclarations {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	for _, rp := range cu.Statements {
		td, ok := rp.Element.(*tree.TypeDecl)
		if !ok {
			continue
		}
		st, ok := td.Definition.(*tree.StructType)
		if !ok || st.Body == nil {
			continue
		}
		for _, fr := range st.Body.Statements {
			vd, ok := fr.Element.(*tree.VariableDeclarations)
			if !ok {
				continue
			}
			for _, vr := range vd.Variables {
				if vr.Element != nil && vr.Element.Name != nil && vr.Element.Name.Name == fieldName {
					return vd
				}
			}
		}
	}
	t.Fatalf("field %q not found in struct", fieldName)
	return nil
}

func TestStructTag_SingleKeyParsesIntoOneAnnotation(t *testing.T) {
	src := "package main\n\ntype User struct {\n\tName string `json:\"name\"`\n}\n"
	vd := parseStructAndFindField(t, src, "Name")

	if got := len(vd.LeadingAnnotations); got != 1 {
		t.Fatalf("LeadingAnnotations: got %d, want 1", got)
	}
	ann := vd.LeadingAnnotations[0]
	if id, ok := ann.AnnotationType.(*tree.Identifier); !ok || id.Name != "json" {
		t.Errorf("AnnotationType: got %+v, want Identifier{Name:\"json\"}", ann.AnnotationType)
	}
	if ann.Arguments == nil || len(ann.Arguments.Elements) != 1 {
		t.Fatalf("Arguments: got %+v, want 1 element", ann.Arguments)
	}
	lit, ok := ann.Arguments.Elements[0].Element.(*tree.Literal)
	if !ok {
		t.Fatalf("Arguments[0]: got %T, want *Literal", ann.Arguments.Elements[0].Element)
	}
	if lit.Source != `"name"` {
		t.Errorf("Arguments[0].Source: got %q, want %q", lit.Source, `"name"`)
	}
	if v, _ := lit.Value.(string); v != "name" {
		t.Errorf("Arguments[0].Value: got %v, want %q", lit.Value, "name")
	}
}

func TestStructTag_MultipleKeysParseIntoMultipleAnnotations(t *testing.T) {
	src := "package main\n\ntype User struct {\n\tEmail string `json:\"email,omitempty\" db:\"email_address\"`\n}\n"
	vd := parseStructAndFindField(t, src, "Email")

	if got := len(vd.LeadingAnnotations); got != 2 {
		t.Fatalf("LeadingAnnotations: got %d, want 2", got)
	}

	first := vd.LeadingAnnotations[0]
	if id, ok := first.AnnotationType.(*tree.Identifier); !ok || id.Name != "json" {
		t.Errorf("[0] AnnotationType: got %+v, want json", first.AnnotationType)
	}
	if lit := first.Arguments.Elements[0].Element.(*tree.Literal); lit.Source != `"email,omitempty"` {
		t.Errorf("[0] Source: got %q, want %q", lit.Source, `"email,omitempty"`)
	}

	second := vd.LeadingAnnotations[1]
	if id, ok := second.AnnotationType.(*tree.Identifier); !ok || id.Name != "db" {
		t.Errorf("[1] AnnotationType: got %+v, want db", second.AnnotationType)
	}
	if lit := second.Arguments.Elements[0].Element.(*tree.Literal); lit.Source != `"email_address"` {
		t.Errorf("[1] Source: got %q, want %q", lit.Source, `"email_address"`)
	}
	// Inter-pair whitespace lives on the second annotation's Prefix.
	if second.Prefix.Whitespace != " " {
		t.Errorf("[1] Prefix.Whitespace: got %q, want %q", second.Prefix.Whitespace, " ")
	}
}

func TestStructTag_NoMarkerLeftBehind(t *testing.T) {
	src := "package main\n\ntype User struct {\n\tName string `json:\"name\"`\n}\n"
	vd := parseStructAndFindField(t, src, "Name")

	for _, m := range vd.Markers.Entries {
		if _, ok := m.(tree.StructTag); ok {
			t.Errorf("StructTag marker should no longer be emitted; LeadingAnnotations is the canonical pathway")
		}
	}
}

func TestStructTag_RoundtripGofmtdInput(t *testing.T) {
	src := "package main\n\ntype User struct {\n\tName  string `json:\"name\"`\n\tEmail string `json:\"email,omitempty\" db:\"email_address\"`\n\tID    int    `json:\"-\"`\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	got := printer.Print(cu)
	if got != src {
		t.Errorf("roundtrip mismatch\nexpected:\n%s\nactual:\n%s", src, got)
	}
}

func TestStructTag_RoundtripWithEscapes(t *testing.T) {
	// A backslash-escaped quote inside the value must roundtrip.
	src := "package main\n\ntype X struct {\n\tField string `json:\"a\\\"b\"`\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	got := printer.Print(cu)
	if got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
}

func TestStructTag_DashValueRoundtrip(t *testing.T) {
	// `json:"-"` is the convention for "skip this field".
	src := "package main\n\ntype X struct {\n\tField string `json:\"-\"`\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	got := printer.Print(cu)
	if got != src {
		t.Errorf("roundtrip mismatch\nexpected: %q\nactual:   %q", src, got)
	}
	vd := parseStructAndFindField(t, src, "Field")
	lit := vd.LeadingAnnotations[0].Arguments.Elements[0].Element.(*tree.Literal)
	if v, _ := lit.Value.(string); v != "-" {
		t.Errorf("Value: got %v, want %q", lit.Value, "-")
	}
}

func TestStructTag_NonStructDoesNotEmitAnnotations(t *testing.T) {
	// Top-level / local var declarations don't have struct-tag syntax;
	// the parser should not emit any LeadingAnnotations on them.
	src := "package main\n\nvar x int = 1\n\nfunc f() {\n\ty := 2\n\t_ = y\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	for _, rp := range cu.Statements {
		if vd, ok := rp.Element.(*tree.VariableDeclarations); ok {
			if len(vd.LeadingAnnotations) > 0 {
				t.Errorf("top-level VariableDeclarations got %d LeadingAnnotations, want 0", len(vd.LeadingAnnotations))
			}
		}
	}
}

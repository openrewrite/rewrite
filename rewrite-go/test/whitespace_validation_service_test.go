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
	"strings"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TestWhitespaceValidationService_RegisteredOnInit verifies that
// importing pkg/recipe/golang registers the service.
func TestWhitespaceValidationService_RegisteredOnInit(t *testing.T) {
	svc := recipe.Service[*recipes.WhitespaceValidationService](nil)
	require.NotNil(t, svc)
}

// TestWhitespaceValidationService_CleanTree confirms a freshly parsed
// CU validates clean.
func TestWhitespaceValidationService_CleanTree(t *testing.T) {
	src := "package main\n\nfunc main() {\n\tprintln(\"hi\")\n}\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	require.NoError(t, err)
	svc := &recipes.WhitespaceValidationService{}
	if errs := svc.Validate(cu); len(errs) != 0 {
		t.Fatalf("expected clean tree to validate, got %d errs:\n%s", len(errs), strings.Join(errs, "\n"))
	}
	assert.True(t, svc.IsValid(cu))
}

// TestWhitespaceValidationService_DetectsCorruption hand-crafts a tree
// containing a Space whose Whitespace contains source text that should
// have been parsed into a node. Verifies the validator flags it.
func TestWhitespaceValidationService_DetectsCorruption(t *testing.T) {
	cu := &golang.CompilationUnit{
		Prefix: java.Space{Whitespace: "package main"}, // non-whitespace stowed away
	}
	svc := &recipes.WhitespaceValidationService{}
	errs := svc.Validate(cu)
	if len(errs) == 0 {
		t.Fatal("expected validator to flag non-whitespace in Space.Whitespace")
	}
	if !strings.Contains(errs[0], "non-whitespace") {
		t.Errorf("error should mention non-whitespace, got: %s", errs[0])
	}
	assert.False(t, svc.IsValid(cu))
}

// TestWhitespaceValidationService_DetectsBadComment crafts a Comment
// whose Text doesn't begin with `//` or `/*` — the printer would emit
// it raw, so the validator must catch it.
func TestWhitespaceValidationService_DetectsBadComment(t *testing.T) {
	cu := &golang.CompilationUnit{
		Prefix: java.Space{
			Comments: []java.Comment{{Text: "this is not a comment", Suffix: "\n"}},
		},
	}
	svc := &recipes.WhitespaceValidationService{}
	errs := svc.Validate(cu)
	if len(errs) == 0 {
		t.Fatal("expected validator to flag a non-comment Text")
	}
}

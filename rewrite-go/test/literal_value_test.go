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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type literalWalker struct {
	visitor.GoVisitor
	lits map[string]*java.Literal
}

func (v *literalWalker) VisitLiteral(lit *java.Literal, p any) java.J {
	v.lits[lit.Source] = lit
	return v.GoVisitor.VisitLiteral(lit, p)
}

// collectLiterals walks a compilation unit collecting every java.Literal, keyed
// by its Source (the original Go literal text).
func collectLiterals(cu *golang.CompilationUnit) map[string]*java.Literal {
	w := &literalWalker{lits: map[string]*java.Literal{}}
	visitor.Init(w)
	w.Visit(cu, nil)
	return w.lits
}

// TestLiteralValueIsJavaCoercible verifies that Go literal syntax (hex, octal,
// binary, underscores, quoted runes, oversized int64) is normalized in
// Literal.Value into a form org.openrewrite's J.Literal can coerce — while
// Source keeps the original text (so printing is byte-identical). Pre-fix, these
// crashed V3's coerceLiteralValue with NumberFormatException, desyncing the RPC
// stream (observed as gin's spurious TypeCast NPE).
func TestLiteralValueIsJavaCoercible(t *testing.T) {
	src := `package p

func f() {
	var _ = 0xf5
	var _ = 100_000
	var _ = 0o17
	var _ = 'a'
	var _ byte = 200
	var _ int64 = 62135596800
	var _ = 3.14
}
`
	cu, err := parser.NewGoParser().Parse("p.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	lits := collectLiterals(cu)

	cases := []struct {
		source string
		value  string // expected Literal.Value (decimal / char)
	}{
		{"0xf5", "245"},
		{"100_000", "100000"},
		{"0o17", "15"},
		{"'a'", "a"},
		{"200", "200"},
		{"62135596800", "62135596800"},
	}
	for _, c := range cases {
		lit, ok := lits[c.source]
		if !ok {
			t.Errorf("literal %q not found", c.source)
			continue
		}
		got, _ := lit.Value.(string)
		if got != c.value {
			t.Errorf("%q: Value = %q, want %q", c.source, got, c.value)
		}
		if lit.Source != c.source {
			t.Errorf("%q: Source = %q, want it preserved", c.source, lit.Source)
		}
		// No Go-specific syntax may leak into Value (what Java can't parse).
		if strings.ContainsAny(got, "_'\"") || strings.HasPrefix(got, "0x") || strings.HasPrefix(got, "0o") {
			t.Errorf("%q: Value %q still carries Go literal syntax", c.source, got)
		}
	}

	// int64 that overflows Java int must widen to long.
	if lit := lits["62135596800"]; lit != nil {
		if p, ok := lit.Type.(*java.JavaTypePrimitive); !ok || p.Keyword != "long" {
			t.Errorf("62135596800: type = %v, want primitive long", lit.Type)
		}
	}
	// A byte value > 127 can't fit Java's signed byte, so it widens to int.
	if lit := lits["200"]; lit != nil {
		if p, ok := lit.Type.(*java.JavaTypePrimitive); !ok || p.Keyword != "int" {
			t.Errorf("byte 200: type = %v, want primitive int", lit.Type)
		}
	}
}

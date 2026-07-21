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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// litRHS parses src and returns the J.Literal on the RHS of the first `:=`.
func litRHS(t *testing.T, src string) *java.Literal {
	t.Helper()
	rhs := firstAssignRHS(t, src)
	lit, ok := rhs.(*java.Literal)
	if !ok {
		t.Fatalf("expected *java.Literal, got %T", rhs)
	}
	return lit
}

// A literal's Value must carry the decoded semantic value (no quotes, escapes
// resolved, numbers typed), matching the other rewrite parsers. The raw source
// text — with quotes — is preserved only in Source for lossless printing.
func TestBasicLitDecodedValue(t *testing.T) {
	cases := []struct {
		name       string
		expr       string
		wantValue  any
		wantSource string
	}{
		// given
		{"interpreted string", `"foo"`, "foo", `"foo"`},
		{"string with escape", `"a\tb"`, "a\tb", `"a\tb"`},
		{"raw string", "`foo`", "foo", "`foo`"},
		{"char", `'a'`, int64('a'), `'a'`},
		{"char escape", `'\n'`, int64('\n'), `'\n'`},
		{"char multibyte", `'π'`, int64('π'), `'π'`},
		{"int", `42`, int64(42), `42`},
		{"hex int", `0x7f`, int64(127), `0x7f`},
		{"underscored int", `1_000`, int64(1000), `1_000`},
		{"float", `3.14`, 3.14, `3.14`},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			// when
			src := "package main\n\nfunc main() {\n\tv := " + tc.expr + "\n\t_ = v\n}\n"
			lit := litRHS(t, src)

			// then
			if lit.Value != tc.wantValue {
				t.Errorf("Value = %#v (%T), want %#v (%T)", lit.Value, lit.Value, tc.wantValue, tc.wantValue)
			}
			if lit.Source != tc.wantSource {
				t.Errorf("Source = %q, want %q", lit.Source, tc.wantSource)
			}
		})
	}
}

// firstLiteralOfType parses src and returns the first *java.Literal whose
// attributed Type is the primitive `keyword` (e.g. "byte", "char").
func firstLiteralOfType(t *testing.T, src, keyword string) *java.Literal {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("g.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	var found *java.Literal
	visitor.Walk(cu, func(n java.Tree) bool {
		if lit, ok := n.(*java.Literal); ok {
			if prim, ok := lit.Type.(*java.JavaTypePrimitive); ok && prim.Keyword == keyword {
				found = lit
				return false
			}
		}
		return true
	})
	if found == nil {
		t.Fatalf("no *java.Literal of primitive type %q found", keyword)
	}
	return found
}

// A Go byte-typed character literal such as `'^'` must round-trip through the
// JVM-side LST deserializer. That deserializer is type-directed: for a J.Literal
// whose Type is the `byte` primitive it coerces the value with
func TestByteCharLiteralRoundTripsAsByte(t *testing.T) {
	// given: `'^'` in a byte context, so go/types attributes it the `byte` type
	src := "package main\n\nfunc main() {\n\tvar b byte = '^'\n\t_ = b\n}\n"

	// when
	lit := firstLiteralOfType(t, src, "byte")

	// then: the Value must be the numeric code point, so the JVM stores it
	// without running Byte.valueOf on a character string.
	if s, ok := lit.Value.(string); ok {
		if _, err := strconv.ParseInt(s, 10, 8); err != nil {
			t.Fatalf("byte literal Value = %q (string): the JVM runs Byte.valueOf(%q), "+
				"which throws NumberFormatException (%v). A byte literal's Value must be a "+
				"numeric code point, not the raw character string.", s, s, err)
		}
	}
	if lit.Value != int64('^') {
		t.Errorf("Value = %#v (%T), want %#v (int64)", lit.Value, lit.Value, int64('^'))
	}
}

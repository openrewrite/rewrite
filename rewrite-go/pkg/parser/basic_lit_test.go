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
		{"char", `'a'`, "a", `'a'`},
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

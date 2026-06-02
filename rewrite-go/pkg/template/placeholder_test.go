/*
 * Copyright 2025 the original author or authors.
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

package template

import "testing"

func TestToPlaceholder(t *testing.T) {
	if got := ToPlaceholder("expr"); got != "__plh_expr__" {
		t.Errorf("ToPlaceholder(\"expr\") = %q, want %q", got, "__plh_expr__")
	}
}

func TestFromPlaceholder(t *testing.T) {
	name, ok := FromPlaceholder("__plh_expr__")
	if !ok || name != "expr" {
		t.Errorf("FromPlaceholder(\"__plh_expr__\") = (%q, %v), want (\"expr\", true)", name, ok)
	}
}

func TestFromPlaceholderNotPlaceholder(t *testing.T) {
	name, ok := FromPlaceholder("notPlaceholder")
	if ok {
		t.Errorf("FromPlaceholder(\"notPlaceholder\") = (%q, %v), want (\"\", false)", name, ok)
	}
}

func TestIsPlaceholder(t *testing.T) {
	tests := []struct {
		input string
		want  bool
	}{
		{"__plh_expr__", true},
		{"__plh_x__", true},
		{"__plh___", false}, // empty name
		{"notPlaceholder", false},
		{"__plh_", false},
		{"", false},
	}
	for _, tt := range tests {
		if got := IsPlaceholder(tt.input); got != tt.want {
			t.Errorf("IsPlaceholder(%q) = %v, want %v", tt.input, got, tt.want)
		}
	}
}

func TestCaptureString(t *testing.T) {
	c := Expr("myExpr")
	if got := c.String(); got != "__plh_myExpr__" {
		t.Errorf("Expr(\"myExpr\").String() = %q, want %q", got, "__plh_myExpr__")
	}
}

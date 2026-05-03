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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
)

func TestNamingService_RegisteredOnInit(t *testing.T) {
	svc := recipe.Service[*golang.NamingService](nil)
	if svc == nil {
		t.Fatal("recipe.Service returned nil for *golang.NamingService")
	}
}

func TestNamingService_ToPascalCase(t *testing.T) {
	svc := &golang.NamingService{}
	cases := []struct{ in, want string }{
		{"fooBar", "FooBar"},
		{"foo", "Foo"},
		{"FooBar", "FooBar"}, // already exported
		{"", ""},
		{"_priv", "_priv"}, // first rune isn't a letter; passthrough
		{"αlpha", "Αlpha"}, // unicode (Greek alpha)
	}
	for _, c := range cases {
		if got := svc.ToPascalCase(c.in); got != c.want {
			t.Errorf("ToPascalCase(%q) = %q, want %q", c.in, got, c.want)
		}
	}
}

func TestNamingService_ToCamelCase(t *testing.T) {
	svc := &golang.NamingService{}
	cases := []struct{ in, want string }{
		{"FooBar", "fooBar"},
		{"Foo", "foo"},
		{"foo", "foo"}, // already camel
		{"", ""},
		{"Αlpha", "αlpha"}, // unicode
	}
	for _, c := range cases {
		if got := svc.ToCamelCase(c.in); got != c.want {
			t.Errorf("ToCamelCase(%q) = %q, want %q", c.in, got, c.want)
		}
	}
}

func TestNamingService_IsExported(t *testing.T) {
	svc := &golang.NamingService{}
	cases := []struct {
		in   string
		want bool
	}{
		{"Foo", true},
		{"foo", false},
		{"_Foo", false}, // underscore is not an uppercase letter
		{"", false},
		{"Αlpha", true},  // unicode upper
		{"αlpha", false}, // unicode lower
	}
	for _, c := range cases {
		if got := svc.IsExported(c.in); got != c.want {
			t.Errorf("IsExported(%q) = %v, want %v", c.in, got, c.want)
		}
	}
}

func TestNamingService_IsValidIdentifier(t *testing.T) {
	svc := &golang.NamingService{}
	cases := []struct {
		in   string
		want bool
	}{
		{"foo", true},
		{"Foo123", true},
		{"_x", true},
		{"123foo", false}, // can't start with digit
		{"foo-bar", false},
		{"", false},
		{"func", false}, // reserved keyword
	}
	for _, c := range cases {
		if got := svc.IsValidIdentifier(c.in); got != c.want {
			t.Errorf("IsValidIdentifier(%q) = %v, want %v", c.in, got, c.want)
		}
	}
}

func TestNamingService_IsPredeclared(t *testing.T) {
	svc := &golang.NamingService{}
	for _, name := range []string{"int", "string", "true", "false", "nil", "iota", "len", "make", "new", "any", "comparable", "min", "max", "clear", "error"} {
		if !svc.IsPredeclared(name) {
			t.Errorf("IsPredeclared(%q) = false, want true", name)
		}
	}
	for _, name := range []string{"Foo", "foo", "MyType", "Println", "func", "if"} {
		if svc.IsPredeclared(name) {
			t.Errorf("IsPredeclared(%q) = true, want false", name)
		}
	}
}

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

package golang

import (
	"go/token"
	"unicode"
	"unicode/utf8"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

// NamingService bundles Go-style identifier helpers behind the
// recipe.Service registry. Recipes that synthesize identifiers — e.g.
// when generating accessors or renaming via a target style — can call
// these without re-implementing the rune-level logic each time.
//
// Recipes get one via recipe.Service:
//
//	svc := recipe.Service[*golang.NamingService](cu)
//	if svc.IsPredeclared(newName) { /* refuse: shadowing builtin */ }
//	exportedName := svc.ToPascalCase(field)
//
// All helpers operate on the *first rune* per the Go spec — visibility
// is determined by the first rune's case, not by the rest of the
// identifier — and use unicode-aware case mapping so non-ASCII
// identifiers (rare but legal) round-trip correctly.
type NamingService struct{}

// ToPascalCase returns the identifier with its first rune upper-cased
// (the "exported" form). Non-letter first runes are returned unchanged
// — caller's job to ensure the result is a valid identifier.
//
//	ToPascalCase("fooBar") == "FooBar"
//	ToPascalCase("Bar")    == "Bar"
//	ToPascalCase("")       == ""
func (s *NamingService) ToPascalCase(name string) string {
	r, size := utf8.DecodeRuneInString(name)
	if r == utf8.RuneError || size == 0 {
		return name
	}
	upper := unicode.ToUpper(r)
	if upper == r {
		return name
	}
	return string(upper) + name[size:]
}

// ToCamelCase returns the identifier with its first rune lower-cased
// (the "unexported" form). Mirror of ToPascalCase.
//
//	ToCamelCase("FooBar") == "fooBar"
//	ToCamelCase("foo")    == "foo"
func (s *NamingService) ToCamelCase(name string) string {
	r, size := utf8.DecodeRuneInString(name)
	if r == utf8.RuneError || size == 0 {
		return name
	}
	lower := unicode.ToLower(r)
	if lower == r {
		return name
	}
	return string(lower) + name[size:]
}

// IsExported reports whether name is exported per Go's rule: the first
// rune is an uppercase Unicode letter. Empty strings are not exported.
// Mirrors go/token.IsExported but kept inside the service surface so
// recipes don't need a separate import.
func (s *NamingService) IsExported(name string) bool {
	return token.IsExported(name)
}

// IsValidIdentifier reports whether name parses as a Go identifier
// (token.IsIdentifier — letter or `_` followed by letters/digits/_,
// not a reserved keyword).
func (s *NamingService) IsValidIdentifier(name string) bool {
	return token.IsIdentifier(name)
}

// IsPredeclared reports whether name is one of Go's predeclared
// identifiers (built-in types, constants, or functions). Recipes
// generating new names should refuse to shadow these — even though
// shadowing is technically legal, it produces confusing code.
//
// The set is taken from the Go spec
// (https://go.dev/ref/spec#Predeclared_identifiers); it does NOT
// include reserved keywords (those are caught by IsValidIdentifier).
func (s *NamingService) IsPredeclared(name string) bool {
	_, ok := predeclaredIdentifiers[name]
	return ok
}

// predeclaredIdentifiers is the universe-block name set per the Go
// spec. Update when the language adds new builtins (e.g. `min` /
// `max` / `clear` were added in 1.21).
var predeclaredIdentifiers = map[string]struct{}{
	// Types.
	"any": {}, "bool": {}, "byte": {}, "comparable": {},
	"complex64": {}, "complex128": {}, "error": {},
	"float32": {}, "float64": {},
	"int": {}, "int8": {}, "int16": {}, "int32": {}, "int64": {},
	"rune": {}, "string": {},
	"uint": {}, "uint8": {}, "uint16": {}, "uint32": {}, "uint64": {}, "uintptr": {},

	// Constants.
	"true": {}, "false": {}, "iota": {},

	// Zero value.
	"nil": {},

	// Functions.
	"append": {}, "cap": {}, "clear": {}, "close": {}, "complex": {},
	"copy": {}, "delete": {}, "imag": {}, "len": {}, "make": {},
	"max": {}, "min": {}, "new": {}, "panic": {}, "print": {},
	"println": {}, "real": {}, "recover": {},
}

func init() {
	recipe.RegisterService[*NamingService](func() any { return &NamingService{} })
}

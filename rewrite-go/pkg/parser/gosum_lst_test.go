/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package parser

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
)

// roundTripSum parses go.sum content into a GoSum LST and prints it back,
// asserting byte-for-byte equality — the lossless contract.
func roundTripSum(t *testing.T, content string) {
	t.Helper()
	gs, err := ParseGoSumFile("go.sum", content)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	got := printer.PrintGoSum(gs)
	if got != content {
		t.Fatalf("not lossless\n--- want ---\n%q\n--- got ---\n%q", content, got)
	}
}

func TestGoSumLossless(t *testing.T) {
	cases := map[string]string{
		"empty": "",

		"single_pair": "github.com/a/b v1.0.0 h1:aaaa=\n" +
			"github.com/a/b v1.0.0/go.mod h1:bbbb=\n",

		"multiple_modules": "github.com/a/b v1.0.0 h1:aaaa=\n" +
			"github.com/a/b v1.0.0/go.mod h1:bbbb=\n" +
			"github.com/c/d v2.3.4 h1:cccc=\n" +
			"github.com/c/d v2.3.4/go.mod h1:dddd=\n",

		"gomod_only": "github.com/a/b v1.0.0/go.mod h1:bbbb=\n",

		"pseudo_version": "github.com/a/b v0.0.0-20200101000000-abcdef123456 h1:aaaa=\n" +
			"github.com/a/b v0.0.0-20200101000000-abcdef123456/go.mod h1:bbbb=\n",

		"incompatible": "github.com/a/b v2.0.0+incompatible h1:aaaa=\n" +
			"github.com/a/b v2.0.0+incompatible/go.mod h1:bbbb=\n",

		"no_trailing_newline": "github.com/a/b v1.0.0 h1:aaaa=",

		"blank_lines": "github.com/a/b v1.0.0 h1:aaaa=\n" +
			"\n" +
			"github.com/c/d v2.0.0 h1:cccc=\n",

		"leading_blank_line": "\ngithub.com/a/b v1.0.0 h1:aaaa=\n",

		"trailing_blank_lines": "github.com/a/b v1.0.0 h1:aaaa=\n\n\n",

		"crlf_endings": "github.com/a/b v1.0.0 h1:aaaa=\r\n" +
			"github.com/a/b v1.0.0/go.mod h1:bbbb=\r\n",
	}

	for name, content := range cases {
		t.Run(name, func(t *testing.T) {
			roundTripSum(t, content)
		})
	}
}

func TestGoSumFields(t *testing.T) {
	// given
	content := "github.com/a/b v1.0.0 h1:zip=\n" +
		"github.com/a/b v1.0.0/go.mod h1:mod=\n"

	// when
	gs, err := ParseGoSumFile("go.sum", content)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}

	// then
	if len(gs.Lines) != 2 {
		t.Fatalf("expected 2 lines, got %d", len(gs.Lines))
	}
	zip := gs.Lines[0].Element
	if zip.ModulePath != "github.com/a/b" || zip.Version != "v1.0.0" || zip.GoMod || zip.Hash != "h1:zip=" {
		t.Fatalf("zip line fields wrong: %#v", zip)
	}
	mod := gs.Lines[1].Element
	if !mod.GoMod || mod.Hash != "h1:mod=" {
		t.Fatalf("go.mod line fields wrong: %#v", mod)
	}
}

func TestGoSumMalformedLineErrors(t *testing.T) {
	// given a line that is not a valid go.sum entry
	// when / then
	if _, err := ParseGoSumFile("go.sum", "not a valid go.sum line\n"); err == nil {
		t.Fatal("expected error for malformed go.sum line, got nil")
	}
}

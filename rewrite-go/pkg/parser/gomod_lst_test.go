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

// roundTrip parses go.mod content into a GoMod LST and prints it back,
// asserting byte-for-byte equality — the lossless contract.
func roundTrip(t *testing.T, content string) {
	t.Helper()
	gm, err := ParseGoModFile("go.mod", content)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	got := printer.PrintGoMod(gm)
	if got != content {
		t.Fatalf("not lossless\n--- want ---\n%q\n--- got ---\n%q", content, got)
	}
}

func TestGoModLossless(t *testing.T) {
	cases := map[string]string{
		"minimal": "module example.com/foo\n",

		"module_go_toolchain": "module example.com/foo\n\ngo 1.21\n\ntoolchain go1.22.0\n",

		"single_require": "module example.com/foo\n\ngo 1.21\n\nrequire github.com/x/y v1.2.3\n",

		"single_require_indirect": "module example.com/foo\n\nrequire github.com/x/y v1.2.3 // indirect\n",

		"block_require": "module example.com/foo\n\n" +
			"require (\n" +
			"\tgithub.com/a/b v1.0.0\n" +
			"\tgithub.com/c/d v1.5.0 // indirect\n" +
			")\n",

		"replace_single": "module example.com/foo\n\nreplace github.com/old => github.com/new v1.0.0\n",

		"replace_with_old_version": "module example.com/foo\n\nreplace github.com/old v1.2.3 => github.com/new v1.0.0\n",

		"replace_local_path": "module example.com/foo\n\nreplace github.com/old => ../local/path\n",

		"replace_block": "module example.com/foo\n\n" +
			"replace (\n" +
			"\tgithub.com/old => github.com/new v1.0.0\n" +
			"\tgithub.com/a v1 => ../local\n" +
			")\n",

		"exclude_single": "module example.com/foo\n\nexclude github.com/bad v1.2.3\n",

		"exclude_block": "module example.com/foo\n\n" +
			"exclude (\n" +
			"\tgithub.com/bad v1.2.3\n" +
			"\tgithub.com/worse v2.0.0\n" +
			")\n",

		"retract_single": "module example.com/foo\n\nretract v1.0.0 // buggy\n",

		"retract_range": "module example.com/foo\n\nretract [v1.0.0, v1.1.0]\n",

		"retract_block": "module example.com/foo\n\n" +
			"retract (\n" +
			"\tv1.0.0 // buggy\n" +
			"\t[v1.2.0, v1.3.0] // broken range\n" +
			")\n",

		"godebug": "module example.com/foo\n\ngo 1.24\n\ngodebug default=go1.21\n",

		"godebug_block": "module example.com/foo\n\n" +
			"godebug (\n" +
			"\tdefault=go1.21\n" +
			"\tpanicnil=1\n" +
			")\n",

		"tool_directive": "module example.com/foo\n\ngo 1.24\n\ntool example.com/foo/cmd/mytool\n",

		"tool_block": "module example.com/foo\n\n" +
			"tool (\n" +
			"\texample.com/foo/cmd/a\n" +
			"\texample.com/foo/cmd/b\n" +
			")\n",

		"leading_comment": "// This is my module\nmodule example.com/foo\n",

		"full_line_comment_between": "module example.com/foo\n\n" +
			"// dependencies below\n" +
			"require github.com/x/y v1.2.3\n",

		"comment_inside_block": "module example.com/foo\n\n" +
			"require (\n" +
			"\t// core deps\n" +
			"\tgithub.com/a/b v1.0.0\n" +
			"\n" +
			"\t// test deps\n" +
			"\tgithub.com/c/d v1.5.0\n" +
			")\n",

		"unusual_spacing": "module    example.com/foo\n\ngo   1.21\n\nrequire\tgithub.com/x/y\tv1.2.3\n",

		"blank_lines_galore": "module example.com/foo\n\n\n\ngo 1.21\n\n\n",

		"no_trailing_newline": "module example.com/foo",

		"empty_require_block": "module example.com/foo\n\nrequire ()\n",

		"empty_require_block_newline": "module example.com/foo\n\nrequire (\n)\n",

		"trailing_comment_eof": "module example.com/foo\n// trailing\n",

		"crlf_endings": "module example.com/foo\r\n\r\ngo 1.21\r\n",

		"unknown_future_directive": "module example.com/foo\n\nsomenewdirective foo bar baz\n",

		"indented_directive": "module example.com/foo\n\n  go 1.21\n",

		"block_trailing_inline_after_paren": "module example.com/foo\n\n" +
			"require ( // inline\n" +
			"\tgithub.com/a/b v1.0.0\n" +
			") // closing\n",
	}

	for name, content := range cases {
		t.Run(name, func(t *testing.T) {
			roundTrip(t, content)
		})
	}
}

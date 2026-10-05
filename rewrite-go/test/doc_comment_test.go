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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
)

func TestDocComments(t *testing.T) {
	cases := map[string][2]string{
		"list indentation": {
			"package p\n\n// Doc explains things.\n//\n//  - first\n//  - second\nfunc F() {}\n",
			"package p\n\n// Doc explains things.\n//\n//   - first\n//   - second\nfunc F() {}\n"},
		"code block": {
			"package p\n\n// Doc with code:\n//\t\tcode()\nfunc G() {}\n",
			"package p\n\n// Doc with code:\n//\n//\tcode()\nfunc G() {}\n"},
		"directive is held off from the doc by a blank line": {
			"package p\n\n// Doc text.\n//go:noinline\nfunc H() {}\n",
			"package p\n\n// Doc text.\n//\n//go:noinline\nfunc H() {}\n"},
		"indented field doc is left alone": {
			"package p\n\ntype T struct {\n\t//  - field doc list\n\t//  - second\n\tX int\n}\n",
			"package p\n\ntype T struct {\n\t//  - field doc list\n\t//  - second\n\tX int\n}\n"},
		"body comment is left alone": {
			"package p\n\nfunc F() {\n\t//  - body list\n\t//  - second\n\tx := 1\n\t_ = x\n}\n",
			"package p\n\nfunc F() {\n\t//  - body list\n\t//  - second\n\tx := 1\n\t_ = x\n}\n"},
		"comment detached by a blank line is left alone": {
			"package p\n\n//  - detached\n\nfunc F() {}\n",
			"package p\n\n//  - detached\n\nfunc F() {}\n"},
		"empty doc comment is dropped": {
			"package p\n\n//\nfunc F() {}\n",
			"package p\n\nfunc F() {}\n"},
		"package doc": {
			"//  - package list\n//  - second\n//\n// Text.\npackage p\n",
			"//   - package list\n//   - second\n//\n// Text.\npackage p\n"},
	}
	for name, io := range cases {
		if out := applyVisitor(t, io[0], format.NewDocCommentVisitor(nil)); out != io[1] {
			t.Errorf("%s:\n  want %q\n  got  %q", name, io[1], out)
		}
	}
}

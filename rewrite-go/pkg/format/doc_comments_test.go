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

package format

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
)

func TestDocCommentRun(t *testing.T) {
	tests := map[string]struct{ src, want string }{
		"indented line stays in the run": {
			src:  "package p\n\n//a\n\t\t//b\nfunc A() {}\n",
			want: "package p\n\n// a\n// b\nfunc A() {}\n",
		},
		"indented token documents nothing": {
			src:  "package p\n\n//a\n\t\tfunc A() {}\n",
			want: "package p\n\n//a\n\t\tfunc A() {}\n",
		},
		"blank line ends the run": {
			src:  "package p\n\n//a\n\n//b\nfunc A() {}\n",
			want: "package p\n\n//a\n\n// b\nfunc A() {}\n",
		},
		"trailing comment documents nothing": {
			src:  "package p\n\nvar x = 1 //a\n",
			want: "package p\n\nvar x = 1 //a\n",
		},
		"comment after a comment on the same line documents nothing": {
			src:  "package p /**/ //\nfunc A() {}\n",
			want: "package p /**/ //\nfunc A() {}\n",
		},
	}
	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			p := parser.NewGoParser()
			p.ParseOnly = true
			cu, err := p.Parse("t.go", tc.src)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			if got := printer.Print(NewDocCommentVisitor(nil).Visit(cu, nil)); got != tc.want {
				t.Errorf("got  %q\nwant %q", got, tc.want)
			}
		})
	}
}

func TestDocCommentFormattingIsIdempotent(t *testing.T) {
	// go/doc/comment rewrites this text again on a second pass when it is read
	// alone; grouped with the line above it, as go/ast groups it, the first
	// pass settles it.
	src := "package A\n\n//\n\n//0\n\t\t\t//`0``0``````0`0`0\nfunc  A()"
	p := parser.NewGoParser()
	p.ParseOnly = true
	cu, err := p.Parse("t.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	once := runAutoFormat(cu)
	again, err := p.Parse("t.go", once)
	if err != nil {
		t.Fatalf("re-parse: %v", err)
	}
	if twice := runAutoFormat(again); twice != once {
		t.Errorf("once  %q\ntwice %q", once, twice)
	}
}

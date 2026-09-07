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

package visitor_test

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func cursorDepth(c *visitor.Cursor) int {
	d := 0
	for p := c.Parent(); p != nil; p = p.Parent() {
		d++
	}
	return d
}

type innermostBlockFinder struct {
	visitor.GoVisitor
	depth  int
	found  bool
	result bool
}

func (v *innermostBlockFinder) VisitBlock(block *java.Block, p any) java.J {
	if d := cursorDepth(v.Cursor()); !v.found || d > v.depth {
		v.depth = d
		v.result = visitor.IsFunctionBodyBlock(v.Cursor())
		v.found = true
	}
	return v.GoVisitor.VisitBlock(block, p)
}

type firstIfFinder struct {
	visitor.GoVisitor
	found  bool
	result bool
}

func (v *firstIfFinder) VisitIf(ifStmt *java.If, p any) java.J {
	if !v.found {
		v.found = true
		v.result = visitor.IsInitWrappedIf(v.Cursor())
	}
	return v.GoVisitor.VisitIf(ifStmt, p)
}

func TestIsFunctionBodyBlock(t *testing.T) {
	tests := []struct {
		name string
		src  string
		want bool
	}{
		{
			name: "method body is a function body block",
			src:  "package main\n\ntype T int\n\nfunc (r T) m() {\n}\n",
			want: true,
		},
		{
			name: "if body is not a function body block",
			src:  "package main\n\nfunc f(b bool) {\n\tif b {\n\t}\n}\n",
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cu, err := parser.NewGoParser().Parse("fb.go", tt.src)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			v := visitor.Init(&innermostBlockFinder{})
			v.Visit(cu, nil)
			if !v.found {
				t.Fatal("no block visited")
			}
			if v.result != tt.want {
				t.Errorf("IsFunctionBodyBlock = %v, want %v", v.result, tt.want)
			}
		})
	}
}

func TestIsInitWrappedIf(t *testing.T) {
	tests := []struct {
		name string
		src  string
		want bool
	}{
		{
			name: "if with init clause is init-wrapped",
			src:  "package main\n\nfunc f() {\n\tif x := 1; x > 0 {\n\t}\n}\n",
			want: true,
		},
		{
			name: "plain if is not init-wrapped",
			src:  "package main\n\nfunc f(b bool) {\n\tif b {\n\t}\n}\n",
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cu, err := parser.NewGoParser().Parse("iw.go", tt.src)
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			v := visitor.Init(&firstIfFinder{})
			v.Visit(cu, nil)
			if !v.found {
				t.Fatal("no if visited")
			}
			if v.result != tt.want {
				t.Errorf("IsInitWrappedIf = %v, want %v", v.result, tt.want)
			}
		})
	}
}

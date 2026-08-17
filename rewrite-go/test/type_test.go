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

package test

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func TestParseMapType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func get(m map[string]int) {
			}
		`))
}

func TestParseChannelType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func send(ch chan int) {
			}
		`))
}

func TestParseSendOnlyChannel(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func send(ch chan<- int) {
			}
		`))
}

func TestParseRecvOnlyChannel(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func recv(ch <-chan int) {
			}
		`))
}

func TestParsePointerType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func deref(p *int) {
			}
		`))
}

func TestParseTypeAssertion(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func check(x interface{}) string {
				return x.(string)
			}
		`))
}

func TestParseParenthesizedExpression(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func calc() int {
				return (1 + 2) * 3
			}
		`))
}

func TestParseTypeAssertionShape(t *testing.T) {
	src := "package main\n\nfunc f(e any) {\n\tx := e /*c*/ .(error)\n\t_ = x\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	var seen int
	forEachTypeAssertion(cu, func(ta *golang.TypeAssertion) {
		seen++
		if ws := ta.Left.After.Whitespace; ws != " " {
			t.Errorf("space before the dot: got %q, want %q", ws, " ")
		}
		if len(ta.Left.After.Comments) != 1 {
			t.Errorf("comment before the dot: got %d, want 1", len(ta.Left.After.Comments))
		}
		if ta.Type == nil {
			t.Error("type assertion has no type")
		}
	})
	if seen != 1 {
		t.Fatalf("expected 1 type assertion, found %d", seen)
	}
}

func TestParseSpaceBeforeTypeAssertionDot(t *testing.T) {
	src := "package main\n\nfunc f(e any) {\n\tx := e  .(error)\n\t_ = x\n}\n"
	assertRoundtrip(t, src)
}

func TestParseCommentBeforeTypeAssertionDot(t *testing.T) {
	src := "package main\n\nfunc f(e any) {\n\tx := e /*c*/ .(error)\n\t_ = x\n}\n"
	assertRoundtrip(t, src)
}

type typeAssertionWalker struct {
	visitor.GoVisitor
	f func(*golang.TypeAssertion)
}

func (v *typeAssertionWalker) VisitTypeAssertion(ta *golang.TypeAssertion, p any) java.J {
	v.f(ta)
	return v.GoVisitor.VisitTypeAssertion(ta, p)
}

func forEachTypeAssertion(cu java.Tree, f func(*golang.TypeAssertion)) {
	visitor.Init(&typeAssertionWalker{f: f}).Visit(cu, nil)
}

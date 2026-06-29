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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// firstStatementInBody parses src and returns the first statement of the first
// top-level function's body.
func firstStatementInBody(t *testing.T, src string) java.Statement {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("body.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	if len(cu.Statements) == 0 {
		t.Fatalf("no top-level statements parsed")
	}
	fn, ok := cu.Statements[0].Element.(*java.MethodDeclaration)
	if !ok {
		t.Fatalf("expected first statement to be *java.MethodDeclaration, got %T", cu.Statements[0].Element)
	}
	if fn.Body == nil || len(fn.Body.Statements) == 0 {
		t.Fatalf("function body is empty")
	}
	return fn.Body.Statements[0].Element
}

// A plain `if cond {}` (no init clause) must stay a bare java.If — mirroring
// J.If — and must NOT be wrapped in golang.StatementWithInit.
func TestPlainIfIsBareIf(t *testing.T) {
	// given
	src := "package main\n\nfunc f(b bool) {\n\tif b {\n\t}\n}\n"

	// when
	stmt := firstStatementInBody(t, src)

	// then
	if _, ok := stmt.(*golang.StatementWithInit); ok {
		t.Fatalf("plain if must not be wrapped in *golang.StatementWithInit")
	}
	if _, ok := stmt.(*java.If); !ok {
		t.Fatalf("expected *java.If, got %T", stmt)
	}
}

// The if condition is modeled as a ControlParentheses (matching J.If), whose
// inner element is the bare Go condition — there is no separate cached wrapper.
func TestIfConditionIsControlParentheses(t *testing.T) {
	// given
	src := "package main\n\nfunc f(b bool) {\n\tif b {\n\t}\n}\n"

	// when
	ifStmt, ok := firstStatementInBody(t, src).(*java.If)
	if !ok {
		t.Fatalf("expected *java.If")
	}

	// then
	if ifStmt.Condition == nil {
		t.Fatalf("condition must be a *java.ControlParentheses, got nil")
	}
	if _, ok := ifStmt.Condition.Tree.Element.(*java.Identifier); !ok {
		t.Fatalf("expected the condition to wrap the bare *java.Identifier, got %T", ifStmt.Condition.Tree.Element)
	}
}

// `if x := f(); cond {}` carries an init clause java.If has no slot for, so it
// must wrap the inner java.If in golang.StatementWithInit, which holds the init.
func TestIfWithInitIsWrapped(t *testing.T) {
	// given
	src := "package main\n\nfunc f() {\n\tif x := g(); x {\n\t}\n}\n"

	// when
	stmt := firstStatementInBody(t, src)

	// then
	wrapper, ok := stmt.(*golang.StatementWithInit)
	if !ok {
		t.Fatalf("expected *golang.StatementWithInit, got %T", stmt)
	}
	if wrapper.Init.Element == nil {
		t.Fatalf("wrapper must carry the init statement")
	}
	if _, ok := wrapper.Statement.(*java.If); !ok {
		t.Fatalf("wrapper must hold the inner *java.If, got %T", wrapper.Statement)
	}
	// The prefix (whitespace before `if`) belongs on the outermost node.
	if wrapper.Prefix.Whitespace != "\n\t" {
		t.Fatalf("expected wrapper to carry the prefix %q, got %q", "\n\t", wrapper.Prefix.Whitespace)
	}
	if inner := wrapper.Statement.(*java.If); inner.Prefix.Whitespace != "" || len(inner.Prefix.Comments) != 0 {
		t.Fatalf("inner if must be prefix-less, got %+v", inner.Prefix)
	}
}

// `switch x := f(); x {}` carries an init clause java.Switch has no slot for, so
// it wraps the inner java.Switch in golang.StatementWithInit.
func TestSwitchWithInitIsWrapped(t *testing.T) {
	// given
	src := "package main\n\nfunc f() {\n\tswitch x := g(); x {\n\t}\n}\n"

	// when
	stmt := firstStatementInBody(t, src)

	// then
	wrapper, ok := stmt.(*golang.StatementWithInit)
	if !ok {
		t.Fatalf("expected *golang.StatementWithInit, got %T", stmt)
	}
	if _, ok := wrapper.Statement.(*java.Switch); !ok {
		t.Fatalf("wrapper must hold the inner *java.Switch, got %T", wrapper.Statement)
	}
}

// Every if/switch init variant must round-trip parse → print byte-for-byte.
func TestInitClauseRoundTrips(t *testing.T) {
	cases := []string{
		"package main\n\nfunc f(b bool) {\n\tif b {\n\t}\n}\n",
		"package main\n\nfunc f(x int) {\n\tif x > 0 {\n\t} else if x < 0 {\n\t} else {\n\t}\n}\n",
		"package main\n\nfunc f() {\n\tif x := g(); x {\n\t}\n}\n",
		"package main\n\nfunc f() {\n\tif v, ok := m[k]; ok {\n\t} else {\n\t}\n}\n",
		"package main\n\nfunc f() {\n\tif a := g(); a {\n\t} else if b := h(); b {\n\t}\n}\n",
		"package main\n\nfunc f() {\n\tswitch x := g(); x {\n\tcase 1:\n\t}\n}\n",
		"package main\n\nfunc f(i any) {\n\tswitch v := i.(type) {\n\tcase int:\n\t\t_ = v\n\t}\n}\n",
		"package main\n\nfunc f(i any) {\n\tswitch x := g(); v := i.(type) {\n\tcase int:\n\t\t_ = v\n\t}\n}\n",
	}
	for _, src := range cases {
		src := src
		t.Run(src, func(t *testing.T) {
			assertRoundTrip(t, src)
		})
	}
}

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

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// firstDeclaration parses src and returns the first top-level declaration
// statement of the compilation unit.
func firstDeclaration(t *testing.T, src string) java.Statement {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("decl.go", src)
	require.NoError(t, err, "parse")
	if len(cu.Statements) == 0 {
		t.Fatalf("no top-level statements parsed")
	}
	return cu.Statements[0].Element
}

// A free function (no receiver) must stay a bare java.MethodDeclaration —
// mirroring J.MethodDeclaration — and must NOT be wrapped in the Go-specific
// golang.MethodDeclaration carrier, which exists only to hold a receiver.
func TestFreeFunctionIsBareMethodDeclaration(t *testing.T) {
	// given
	src := "package main\n\nfunc Run() {\n}\n"

	// when
	decl := firstDeclaration(t, src)

	// then
	if _, ok := decl.(*golang.MethodDeclaration); ok {
		t.Fatalf("free function must not be wrapped in *golang.MethodDeclaration")
	}
	if _, ok := decl.(*java.MethodDeclaration); !ok {
		t.Fatalf("expected *java.MethodDeclaration, got %T", decl)
	}
}

// A method declaration with a receiver wraps the inner java.MethodDeclaration
// in golang.MethodDeclaration to carry the receiver J.MethodDeclaration cannot.
func TestMethodWithReceiverIsWrapped(t *testing.T) {
	// given
	src := "package main\n\nfunc (s *Service) Run() {\n}\n"

	// when
	decl := firstDeclaration(t, src)

	// then
	wrapper, ok := decl.(*golang.MethodDeclaration)
	require.Truef(t, ok, "expected *golang.MethodDeclaration, got %T", decl)
	require.NotNil(t, wrapper.Declaration, "wrapper must carry the inner *java.MethodDeclaration")
	require.Len(t, wrapper.Receiver.Elements, 1, "expected a single receiver element")
	// The prefix (the blank line before `func`) belongs on the outermost node.
	require.Equalf(t, "\n\n", wrapper.Prefix.Whitespace, "expected wrapper to carry the prefix %q", "\n\n")
	require.False(t, wrapper.Declaration.Prefix.Whitespace != "" || len(wrapper.Declaration.Prefix.Comments) != 0, "inner declaration must be prefix-less")
}

// With the prefix on the wrapper but `//go:` directives still on the inner
// declaration, the printer must emit directives first, then the prefix, then
// `func` + receiver — exercising the prefix-after-directive ordering.
func TestReceiverMethodWithDirectiveRoundTrips(t *testing.T) {
	assertRoundTrip(t, "package main\n\n//go:noinline\nfunc (s *Service) Run() {\n}\n")
}

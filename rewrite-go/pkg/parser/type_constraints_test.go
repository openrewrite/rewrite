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
	"fmt"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// interfaceBodyTypeExpr parses src, finds the first interface type
// declaration, and returns the TypeExpr of its single embedded
// type-set element (a VariableDeclarations with no declared names).
func interfaceBodyTypeExpr(t *testing.T, src string) tree.Expression {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("constraints.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	for _, rp := range cu.Statements {
		td, ok := rp.Element.(*tree.TypeDecl)
		if !ok {
			continue
		}
		it, ok := td.Definition.(*tree.InterfaceType)
		if !ok {
			continue
		}
		if len(it.Body.Statements) != 1 {
			t.Fatalf("expected 1 interface body element, got %d", len(it.Body.Statements))
		}
		vd, ok := it.Body.Statements[0].Element.(*tree.VariableDeclarations)
		if !ok {
			t.Fatalf("expected body element to be *tree.VariableDeclarations, got %T", it.Body.Statements[0].Element)
		}
		return vd.TypeExpr
	}
	t.Fatalf("no interface type declaration found")
	return nil
}

func assertRoundTrip(t *testing.T, src string) {
	t.Helper()
	cu, err := parser.NewGoParser().Parse("constraints.go", src)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	if printed := printer.Print(cu); printed != src {
		t.Errorf("round-trip mismatch\n--- expected ---\n%q\n--- actual ---\n%q", src, printed)
	}
}

// A type-set union element such as `~int | ~int8` must NOT be modeled
// as a J.Binary in the TypeTree-typed `typeExpression` slot: that fails
// a cast to TypeTree when the LST is received over RPC on the Java side.
func TestUnionConstraintIsUnionType(t *testing.T) {
	src := "package main\n\ntype Signed interface {\n\t~int | ~int8 | ~int16\n}\n"
	typeExpr := interfaceBodyTypeExpr(t, src)

	union, ok := typeExpr.(*tree.Union)
	if !ok {
		t.Fatalf("expected TypeExpr to be *tree.Union, got %T", typeExpr)
	}
	if len(union.Types) != 3 {
		t.Fatalf("expected 3 union terms, got %d", len(union.Types))
	}
	for i, term := range union.Types {
		ut, ok := term.Element.(*tree.UnderlyingType)
		if !ok {
			t.Fatalf("term %d: expected *tree.UnderlyingType, got %T", i, term.Element)
		}
		if _, ok := ut.Element.(*tree.Identifier); !ok {
			t.Fatalf("term %d: expected underlying *tree.Identifier, got %T", i, ut.Element)
		}
	}
	assertRoundTrip(t, src)
}

// A bare approximation element `~int` (no union) must be a TypeTree too.
func TestStandaloneApproximationIsUnderlyingType(t *testing.T) {
	src := "package main\n\ntype OnlyTilde interface {\n\t~int\n}\n"
	typeExpr := interfaceBodyTypeExpr(t, src)

	ut, ok := typeExpr.(*tree.UnderlyingType)
	if !ok {
		t.Fatalf("expected TypeExpr to be *tree.UnderlyingType, got %T", typeExpr)
	}
	if id, ok := ut.Element.(*tree.Identifier); !ok || id.Name != "int" {
		t.Fatalf("expected underlying Identifier(int), got %T (%v)", ut.Element, fmt.Sprintf("%v", ut.Element))
	}
	assertRoundTrip(t, src)
}

// A union of named constraints `Signed | Unsigned` is a union of plain
// type names (no approximation).
func TestUnionOfNamedConstraints(t *testing.T) {
	src := "package main\n\ntype Number interface {\n\tSigned | Unsigned\n}\n"
	typeExpr := interfaceBodyTypeExpr(t, src)

	union, ok := typeExpr.(*tree.Union)
	if !ok {
		t.Fatalf("expected TypeExpr to be *tree.Union, got %T", typeExpr)
	}
	if len(union.Types) != 2 {
		t.Fatalf("expected 2 union terms, got %d", len(union.Types))
	}
	for i, term := range union.Types {
		if _, ok := term.Element.(*tree.Identifier); !ok {
			t.Fatalf("term %d: expected *tree.Identifier, got %T", i, term.Element)
		}
	}
	assertRoundTrip(t, src)
}

// Approximation over a composite (slice) type: `~[]byte`. The underlying
// type must itself be a rich TypeTree (J.ArrayType), not J.Unknown.
func TestApproximationOverSliceType(t *testing.T) {
	src := "package main\n\ntype ByteSeq interface {\n\t~[]byte | ~string\n}\n"
	typeExpr := interfaceBodyTypeExpr(t, src)

	union, ok := typeExpr.(*tree.Union)
	if !ok {
		t.Fatalf("expected TypeExpr to be *tree.Union, got %T", typeExpr)
	}
	if len(union.Types) != 2 {
		t.Fatalf("expected 2 union terms, got %d", len(union.Types))
	}
	first, ok := union.Types[0].Element.(*tree.UnderlyingType)
	if !ok {
		t.Fatalf("term 0: expected *tree.UnderlyingType, got %T", union.Types[0].Element)
	}
	if _, ok := first.Element.(*tree.ArrayType); !ok {
		t.Fatalf("term 0: expected underlying *tree.ArrayType (slice), got %T", first.Element)
	}
	assertRoundTrip(t, src)
}

// Union of pointer types: `*int | *int64`. Each term is a rich
// Go.PointerType (a TypeTree), not J.Unary/J.Unknown.
func TestUnionOfPointerTypes(t *testing.T) {
	src := "package main\n\ntype IntPtr interface {\n\t*int | *int64\n}\n"
	typeExpr := interfaceBodyTypeExpr(t, src)

	union, ok := typeExpr.(*tree.Union)
	if !ok {
		t.Fatalf("expected TypeExpr to be *tree.Union, got %T", typeExpr)
	}
	if len(union.Types) != 2 {
		t.Fatalf("expected 2 union terms, got %d", len(union.Types))
	}
	for i, term := range union.Types {
		if _, ok := term.Element.(*tree.PointerType); !ok {
			t.Fatalf("term %d: expected *tree.PointerType, got %T", i, term.Element)
		}
	}
	assertRoundTrip(t, src)
}

// Union of qualified names: `constraints.Signed | constraints.Float`.
// Each term is a rich J.FieldAccess (a TypeTree).
func TestUnionOfQualifiedNames(t *testing.T) {
	src := "package main\n\nimport \"golang.org/x/exp/constraints\"\n\ntype Num interface {\n\tconstraints.Signed | constraints.Float\n}\n"
	typeExpr := interfaceBodyTypeExpr(t, src)

	union, ok := typeExpr.(*tree.Union)
	if !ok {
		t.Fatalf("expected TypeExpr to be *tree.Union, got %T", typeExpr)
	}
	if len(union.Types) != 2 {
		t.Fatalf("expected 2 union terms, got %d", len(union.Types))
	}
	for i, term := range union.Types {
		if _, ok := term.Element.(*tree.FieldAccess); !ok {
			t.Fatalf("term %d: expected *tree.FieldAccess, got %T", i, term.Element)
		}
	}
	assertRoundTrip(t, src)
}

// Plain (non-tilde) union of primitive type names, mirroring
// klauspost/compress .../le.go. Every term is a plain J.Identifier.
func TestPlainUnionOfPrimitives(t *testing.T) {
	src := "package main\n\ntype Indexer interface {\n\tint | int8 | int16 | int32 | int64 | uint | uint8 | uint16 | uint32 | uint64\n}\n"
	typeExpr := interfaceBodyTypeExpr(t, src)

	union, ok := typeExpr.(*tree.Union)
	if !ok {
		t.Fatalf("expected TypeExpr to be *tree.Union, got %T", typeExpr)
	}
	if len(union.Types) != 10 {
		t.Fatalf("expected 10 union terms, got %d", len(union.Types))
	}
	for i, term := range union.Types {
		if _, ok := term.Element.(*tree.Identifier); !ok {
			t.Fatalf("term %d: expected *tree.Identifier, got %T", i, term.Element)
		}
	}
	assertRoundTrip(t, src)
}

// Mixed (non-tilde) union of a qualified name and a slice type,
// mirroring golang-jwt/jwt .../token.go: `crypto.PublicKey | []uint8`.
func TestUnionOfQualifiedNameAndSlice(t *testing.T) {
	src := "package main\n\nimport \"crypto\"\n\ntype VerificationKey interface {\n\tcrypto.PublicKey | []uint8\n}\n"
	typeExpr := interfaceBodyTypeExpr(t, src)

	union, ok := typeExpr.(*tree.Union)
	if !ok {
		t.Fatalf("expected TypeExpr to be *tree.Union, got %T", typeExpr)
	}
	if len(union.Types) != 2 {
		t.Fatalf("expected 2 union terms, got %d", len(union.Types))
	}
	if _, ok := union.Types[0].Element.(*tree.FieldAccess); !ok {
		t.Fatalf("term 0: expected *tree.FieldAccess (qualified name), got %T", union.Types[0].Element)
	}
	if _, ok := union.Types[1].Element.(*tree.ArrayType); !ok {
		t.Fatalf("term 1: expected *tree.ArrayType (slice), got %T", union.Types[1].Element)
	}
	assertRoundTrip(t, src)
}

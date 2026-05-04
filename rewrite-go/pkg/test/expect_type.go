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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// ExpectType walks the tree rooted at root and asserts that the first
// identifier whose Name == name carries a fully-qualified type whose FQN
// matches expectedFQN. Use this for class/struct/parameterized types; for
// primitives use ExpectPrimitiveType.
//
// Fails the test if no matching identifier is found, if its Type is nil,
// or if the type does not implement tree.FullyQualified.
func ExpectType(t *testing.T, root tree.Tree, name string, expectedFQN string) {
	t.Helper()
	c := visitor.Init(&identifierTypeCollector{name: name})
	c.Visit(root, nil)
	if !c.found {
		t.Fatalf("ExpectType(%q): no identifier with that name in tree", name)
	}
	if c.typ == nil {
		t.Fatalf("ExpectType(%q): identifier has nil Type", name)
	}
	fq, ok := c.typ.(tree.FullyQualified)
	if !ok {
		t.Fatalf("ExpectType(%q): identifier Type is %T, want FullyQualified", name, c.typ)
	}
	if got := fq.GetFullyQualifiedName(); got != expectedFQN {
		t.Errorf("ExpectType(%q): FQN = %q, want %q", name, got, expectedFQN)
	}
}

// ExpectPrimitiveType asserts that the first identifier named `name` has a
// JavaTypePrimitive whose Keyword matches expectedKeyword (e.g. "int",
// "String", "bool"). Mirrors ExpectType for primitive type attribution.
func ExpectPrimitiveType(t *testing.T, root tree.Tree, name string, expectedKeyword string) {
	t.Helper()
	c := visitor.Init(&identifierTypeCollector{name: name})
	c.Visit(root, nil)
	if !c.found {
		t.Fatalf("ExpectPrimitiveType(%q): no identifier with that name in tree", name)
	}
	if c.typ == nil {
		t.Fatalf("ExpectPrimitiveType(%q): identifier has nil Type", name)
	}
	prim, ok := c.typ.(*tree.JavaTypePrimitive)
	if !ok {
		t.Fatalf("ExpectPrimitiveType(%q): identifier Type is %T, want *JavaTypePrimitive", name, c.typ)
	}
	if prim.Keyword != expectedKeyword {
		t.Errorf("ExpectPrimitiveType(%q): keyword = %q, want %q", name, prim.Keyword, expectedKeyword)
	}
}

// ExpectMethodType walks the tree rooted at root and asserts that the
// first MethodInvocation or MethodDeclaration whose name matches `name`
// carries a non-nil JavaTypeMethod whose DeclaringType.FullyQualifiedName
// equals expectedDeclaringFQN.
//
// For invocations across packages, expectedDeclaringFQN is the import path
// of the owning package (e.g. "fmt" for fmt.Println). For methods declared
// in the file under test, it is the package's full path
// (e.g. "main.Point" for a method on Point in package main).
func ExpectMethodType(t *testing.T, root tree.Tree, name string, expectedDeclaringFQN string) {
	t.Helper()
	c := visitor.Init(&methodTypeCollector{name: name})
	c.Visit(root, nil)
	if !c.found {
		t.Fatalf("ExpectMethodType(%q): no method with that name in tree", name)
	}
	if c.methodType == nil {
		t.Fatalf("ExpectMethodType(%q): method has nil MethodType", name)
	}
	if c.methodType.DeclaringType == nil {
		t.Fatalf("ExpectMethodType(%q): method has nil DeclaringType", name)
	}
	if got := c.methodType.DeclaringType.FullyQualifiedName; got != expectedDeclaringFQN {
		t.Errorf("ExpectMethodType(%q): declaring FQN = %q, want %q", name, got, expectedDeclaringFQN)
	}
}

type identifierTypeCollector struct {
	visitor.GoVisitor
	name  string
	found bool
	typ   tree.JavaType
}

func (v *identifierTypeCollector) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	if !v.found && ident.Name == v.name {
		v.found = true
		v.typ = ident.Type
	}
	return ident
}

type methodTypeCollector struct {
	visitor.GoVisitor
	name       string
	found      bool
	methodType *tree.JavaTypeMethod
}

func (v *methodTypeCollector) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
	if !v.found && mi.Name != nil && mi.Name.Name == v.name {
		v.found = true
		v.methodType = mi.MethodType
	}
	return v.GoVisitor.VisitMethodInvocation(mi, p)
}

func (v *methodTypeCollector) VisitMethodDeclaration(md *tree.MethodDeclaration, p any) tree.J {
	if !v.found && md.Name != nil && md.Name.Name == v.name {
		v.found = true
		v.methodType = md.MethodType
	}
	return v.GoVisitor.VisitMethodDeclaration(md, p)
}

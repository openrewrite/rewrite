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

	"github.com/stretchr/testify/assert"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// typeCollector visits a tree and collects identifiers with their types.
type typeCollector struct {
	visitor.GoVisitor
	identTypes map[string]java.JavaType
}

func (v *typeCollector) VisitIdentifier(ident *java.Identifier, p any) java.J {
	if ident.Type != nil {
		v.identTypes[ident.Name] = ident.Type
	}
	return ident
}

// methodTypeCollector visits a tree and collects method declaration types.
type methodTypeCollector struct {
	visitor.GoVisitor
	methodTypes map[string]*java.JavaTypeMethod
}

func (v *methodTypeCollector) VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J {
	if md.MethodType != nil && md.Name != nil {
		v.methodTypes[md.Name.Name] = md.MethodType
	}
	return v.GoVisitor.VisitMethodDeclaration(md, p)
}

func TestTypeAttributionLocalVars(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

func main() {
	x := 42
	y := "hello"
	_ = x
	_ = y
}
`)
	require.NoError(t, err)

	v := visitor.Init(&typeCollector{identTypes: make(map[string]java.JavaType)})
	v.Visit(cu, nil)

	// "x" should be an int type
	if xType, ok := v.identTypes["x"]; ok {
		if prim, ok := xType.(*java.JavaTypePrimitive); ok {
			assert.Equal(t, "int", prim.Keyword, "expected x to be int")
		} else {
			t.Errorf("expected x to be primitive, got %T", xType)
		}
	} else {
		t.Error("no type attribution for x")
	}

	// "y" should be a string type (mapped as Primitive "String")
	if yType, ok := v.identTypes["y"]; ok {
		if prim, ok := yType.(*java.JavaTypePrimitive); ok {
			assert.Equal(t, "String", prim.Keyword, "expected y to be String")
		} else {
			t.Errorf("expected y to be primitive, got %T", yType)
		}
	} else {
		t.Error("no type attribution for y")
	}
}

func TestTypeAttributionFuncDecl(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

func add(a int, b int) int {
	return a + b
}
`)
	require.NoError(t, err)

	v := visitor.Init(&methodTypeCollector{methodTypes: make(map[string]*java.JavaTypeMethod)})
	v.Visit(cu, nil)

	addType, ok := v.methodTypes["add"]
	require.True(t, ok, "no method type for add()")
	assert.Equal(t, "add", addType.Name, "expected method name 'add")
	assert.Len(t, addType.ParameterTypes, 2, "expected 2 parameters")
	if ret, ok := addType.ReturnType.(*java.JavaTypePrimitive); ok {
		assert.Equal(t, "int", ret.Keyword, "expected return type int")
	} else {
		t.Errorf("expected primitive return type, got %T", addType.ReturnType)
	}
}

func TestTypeAttributionMultiReturn(t *testing.T) {
	// given
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

func divmod(a int, b int) (int, int) {
	return a / b, a % b
}
`)
	require.NoError(t, err)

	// when
	v := visitor.Init(&methodTypeCollector{methodTypes: make(map[string]*java.JavaTypeMethod)})
	v.Visit(cu, nil)

	// then
	mt, ok := v.methodTypes["divmod"]
	require.True(t, ok, "no method type for divmod()")
	param, ok := mt.ReturnType.(*java.JavaTypeParameterized)
	require.Truef(t, ok, "expected parameterized return type, got %T", mt.ReturnType)
	assert.False(t, param.Type == nil || param.Type.GetFullyQualifiedName() != "go.tuple", "expected tuple FQN 'go.tuple")
	require.Len(t, param.TypeParameters, 2, "expected 2 tuple type parameters")
	for i, tp := range param.TypeParameters {
		prim, ok := tp.(*java.JavaTypePrimitive)
		if !ok {
			t.Errorf("tuple element %d: expected primitive, got %T", i, tp)
			continue
		}
		assert.Equalf(t, "int", prim.Keyword, "tuple element %d: expected int", i)
	}
}

func TestTypeAttributionMultiReturnHeterogeneous(t *testing.T) {
	// given
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

func split() (string, int, bool) {
	return "x", 1, true
}
`)
	require.NoError(t, err)

	// when
	v := visitor.Init(&methodTypeCollector{methodTypes: make(map[string]*java.JavaTypeMethod)})
	v.Visit(cu, nil)

	// then
	mt, ok := v.methodTypes["split"]
	require.True(t, ok, "no method type for split()")
	param, ok := mt.ReturnType.(*java.JavaTypeParameterized)
	require.Truef(t, ok, "expected parameterized return type, got %T", mt.ReturnType)
	assert.False(t, param.Type == nil || param.Type.GetFullyQualifiedName() != "go.tuple", "expected tuple FQN 'go.tuple")
	require.Len(t, param.TypeParameters, 3, "expected 3 tuple type parameters")
	expectedKeywords := []string{"String", "int", "boolean"}
	for i, want := range expectedKeywords {
		prim, ok := param.TypeParameters[i].(*java.JavaTypePrimitive)
		if !ok {
			t.Errorf("tuple element %d: expected primitive, got %T", i, param.TypeParameters[i])
			continue
		}
		assert.Equalf(t, want, prim.Keyword, "tuple element %d", i)
	}
}

func TestTypeAttributionStdlib(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

import "fmt"

func main() {
	fmt.Println("hello")
}
`)
	require.NoError(t, err)

	ExpectMethodType(t, cu, "Println", "fmt")
}

func TestTypeAttributionStructType(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

type Point struct {
	X int
	Y int
}

func main() {
	p := Point{X: 1, Y: 2}
	_ = p
}
`)
	require.NoError(t, err)

	ExpectType(t, cu, "p", "main.Point")
}

func TestTypeAttributionGracefulDegradation(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

import "github.com/nonexistent/pkg"

func main() {
	pkg.DoSomething()
}
`)
	// Parser should still succeed
	require.NoError(t, err)
	require.NotNil(t, cu, "expected non-nil compilation unit")
}

func TestTypeAttributionAssignment(t *testing.T) {
	src := "package main\n\nfunc f() {\n\tx := 1\n\tx = 2\n\t_ = x\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	var found int
	forEachAssignment(cu, func(a *java.Assignment) {
		found++
		if a.Type == nil {
			t.Errorf("assignment %d has no type", found)
		} else if prim, ok := a.Type.(*java.JavaTypePrimitive); !ok || prim.Keyword != "int" {
			t.Errorf("assignment %d type: got %v, want int", found, a.Type)
		}
	})
	if found != 3 {
		t.Fatalf("expected 3 assignments, found %d", found)
	}
}

func TestTypeAttributionSliceType(t *testing.T) {
	src := "package main\n\nvar xs []int\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	var found int
	forEachArrayType(cu, func(a *java.ArrayType) {
		found++
		if a.Type == nil {
			t.Error("slice type has no type")
		}
	})
	if found != 1 {
		t.Fatalf("expected 1 array type, found %d", found)
	}
}

type assignmentWalker struct {
	visitor.GoVisitor
	onAssignment        func(*java.Assignment)
	onArrayType         func(*java.ArrayType)
	onMethodDeclaration func(*java.MethodDeclaration)
	onMethodInvocation  func(*java.MethodInvocation)
	onIdentifier        func(*java.Identifier)
}

func (v *assignmentWalker) VisitAssignment(a *java.Assignment, p any) java.J {
	if v.onAssignment != nil {
		v.onAssignment(a)
	}
	return v.GoVisitor.VisitAssignment(a, p)
}

func (v *assignmentWalker) VisitArrayType(a *java.ArrayType, p any) java.J {
	if v.onArrayType != nil {
		v.onArrayType(a)
	}
	return v.GoVisitor.VisitArrayType(a, p)
}

func forEachAssignment(cu java.Tree, f func(*java.Assignment)) {
	visitor.Init(&assignmentWalker{onAssignment: f}).Visit(cu, nil)
}

func forEachArrayType(cu java.Tree, f func(*java.ArrayType)) {
	visitor.Init(&assignmentWalker{onArrayType: f}).Visit(cu, nil)
}

func TestTypeAttributionFuncLit(t *testing.T) {
	src := "package main\n\nvar fn = func(a int) int { return a }\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	var found int
	forEachMethodDeclaration(cu, func(m *java.MethodDeclaration) {
		found++
		if m.MethodType == nil {
			t.Fatal("function literal has no method type")
		}
		if got := len(m.MethodType.ParameterTypes); got != 1 {
			t.Errorf("parameter types: got %d, want 1", got)
		}
		if prim, ok := m.MethodType.ReturnType.(*java.JavaTypePrimitive); !ok || prim.Keyword != "int" {
			t.Errorf("return type: got %v, want int", m.MethodType.ReturnType)
		}
	})
	if found != 1 {
		t.Fatalf("expected 1 method declaration, found %d", found)
	}
}

func (v *assignmentWalker) VisitMethodDeclaration(m *java.MethodDeclaration, p any) java.J {
	if v.onMethodDeclaration != nil {
		v.onMethodDeclaration(m)
	}
	return v.GoVisitor.VisitMethodDeclaration(m, p)
}

func forEachMethodDeclaration(cu java.Tree, f func(*java.MethodDeclaration)) {
	visitor.Init(&assignmentWalker{onMethodDeclaration: f}).Visit(cu, nil)
}

func TestTypeAttributionCallThroughFuncValue(t *testing.T) {
	src := "package main\n\ntype H func(a int) int\n\ntype S struct{ cb func(a int) int }\n\nfunc f(s S, h H) {\n\tfn := func(a int) int { return a }\n\t_ = fn(1)\n\t_ = s.cb(2)\n\t_ = h(3)\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	byName := map[string]*java.JavaTypeMethod{}
	forEachMethodInvocation(cu, func(m *java.MethodInvocation) {
		if m.Name != nil {
			byName[m.Name.Name] = m.MethodType
		}
	})
	for _, name := range []string{"fn", "cb", "h"} {
		mt, ok := byName[name]
		if !ok {
			t.Fatalf("no call to %q found", name)
		}
		if mt == nil {
			t.Errorf("call through %q has no method type", name)
			continue
		}
		if got := len(mt.ParameterTypes); got != 1 {
			t.Errorf("%q parameter types: got %d, want 1", name, got)
		}
	}
}

func (v *assignmentWalker) VisitMethodInvocation(m *java.MethodInvocation, p any) java.J {
	if v.onMethodInvocation != nil {
		v.onMethodInvocation(m)
	}
	return v.GoVisitor.VisitMethodInvocation(m, p)
}

func forEachMethodInvocation(cu java.Tree, f func(*java.MethodInvocation)) {
	visitor.Init(&assignmentWalker{onMethodInvocation: f}).Visit(cu, nil)
}

func TestTypeAttributionFieldOwner(t *testing.T) {
	src := "package main\n\ntype T struct{ N int }\n\nfunc f(t T) {\n\t_ = t.N\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	var checked int
	forEachIdentifier(cu, func(i *java.Identifier) {
		if i.Name != "N" || i.FieldType == nil {
			return
		}
		checked++
		owner, ok := i.FieldType.Owner.(*java.JavaTypeClass)
		if !ok {
			t.Fatalf("field N owner: got %T, want *JavaTypeClass", i.FieldType.Owner)
		}
		if owner.FullyQualifiedName != "main.T" {
			t.Errorf("field N owner: got %q, want main.T", owner.FullyQualifiedName)
		}
	})
	if checked == 0 {
		t.Fatal("no field type for N found")
	}
}

func TestTypeAttributionInterfaceMethodDeclaringType(t *testing.T) {
	src := "package main\n\ntype I interface{ M() int }\n\nvar i I\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	var checked int
	forEachIdentifier(cu, func(id *java.Identifier) {
		cls, ok := id.Type.(*java.JavaTypeClass)
		if !ok || cls.FullyQualifiedName != "main.I" || len(cls.Methods) == 0 {
			return
		}
		checked++
		m := cls.Methods[0]
		if m.DeclaringType == nil {
			t.Fatal("interface method M has no declaring type")
		}
		if got := m.DeclaringType.GetFullyQualifiedName(); got != "main.I" {
			t.Errorf("declaring type: got %q, want main.I", got)
		}
	})
	if checked == 0 {
		t.Fatal("interface I not found")
	}
}

func (v *assignmentWalker) VisitIdentifier(i *java.Identifier, p any) java.J {
	if v.onIdentifier != nil {
		v.onIdentifier(i)
	}
	return v.GoVisitor.VisitIdentifier(i, p)
}

func forEachIdentifier(cu java.Tree, f func(*java.Identifier)) {
	visitor.Init(&assignmentWalker{onIdentifier: f}).Visit(cu, nil)
}

func TestTypeAttributionLocalOwner(t *testing.T) {
	src := "package main\n\nfunc f(a int) {\n\tb := 1\n\t_ = a\n\t_ = b\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	seen := map[string]bool{}
	forEachIdentifier(cu, func(i *java.Identifier) {
		if i.FieldType == nil || (i.Name != "a" && i.Name != "b") {
			return
		}
		seen[i.Name] = true
		owner, ok := i.FieldType.Owner.(*java.JavaTypeMethod)
		if !ok {
			t.Errorf("%q owner: got %T, want *JavaTypeMethod", i.Name, i.FieldType.Owner)
			return
		}
		if owner.Name != "f" {
			t.Errorf("%q owner method: got %q, want f", i.Name, owner.Name)
		}
	})
	for _, n := range []string{"a", "b"} {
		if !seen[n] {
			t.Errorf("no field type seen for %q", n)
		}
	}
}

func TestTypeAttributionNamedFuncTypeDeclaringType(t *testing.T) {
	src := "package main\n\ntype optionFunc func(a int)\n\nfunc f(o optionFunc, g func(a int)) {\n\to(1)\n\tg(2)\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	byName := map[string]*java.JavaTypeMethod{}
	forEachMethodInvocation(cu, func(m *java.MethodInvocation) {
		if m.Name != nil {
			byName[m.Name.Name] = m.MethodType
		}
	})
	named := byName["o"]
	if named == nil || named.DeclaringType == nil {
		t.Fatalf("call through named func type has no declaring type: %v", named)
	}
	if got := named.DeclaringType.GetFullyQualifiedName(); got != "main.optionFunc" {
		t.Errorf("declaring type: got %q, want main.optionFunc", got)
	}
	// An unnamed func type declares nothing.
	if unnamed := byName["g"]; unnamed == nil || unnamed.DeclaringType != nil {
		t.Errorf("call through unnamed func type: got declaring type %v, want none", unnamed.DeclaringType)
	}
}

func TestTypeAttributionPackageLevelVarOwner(t *testing.T) {
	src := "package main\n\nvar Global int\n\ntype S struct{ F int }\n\nfunc (s *S) M(a int) {\n\t_ = Global\n\t_ = s.F\n\t_ = a\n}\n"
	cu, err := parser.NewGoParser().Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	owners := map[string][]string{}
	forEachIdentifier(cu, func(i *java.Identifier) {
		if i.FieldType == nil {
			return
		}
		owners[i.Name] = append(owners[i.Name], describeOwner(i.FieldType.Owner))
	})
	for name, want := range map[string]string{
		"Global": "class:main",   // package scope, not whoever reads it
		"F":      "class:main.S", // the struct declaring it
		"a":      "method:M",     // the function it lives in
		"s":      "method:M",     // the receiver, at declaration and use alike
	} {
		got := owners[name]
		if len(got) == 0 {
			t.Errorf("%s: no field type seen", name)
			continue
		}
		for _, g := range got {
			if g != want {
				t.Errorf("%s owner: got %q, want %q (all sightings: %v)", name, g, want, got)
				break
			}
		}
	}
}

func describeOwner(o java.JavaType) string {
	switch v := o.(type) {
	case nil:
		return "none"
	case *java.JavaTypeMethod:
		if v == nil {
			return "TYPED-NIL method"
		}
		return "method:" + v.Name
	case *java.JavaTypeClass:
		if v == nil {
			return "TYPED-NIL class"
		}
		return "class:" + v.FullyQualifiedName
	}
	return "other"
}

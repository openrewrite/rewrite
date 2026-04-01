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

	"github.com/openrewrite/rewrite/pkg/parser"
	"github.com/openrewrite/rewrite/pkg/tree"
	"github.com/openrewrite/rewrite/pkg/visitor"
)

// typeCollector visits a tree and collects identifiers with their types.
type typeCollector struct {
	visitor.GoVisitor
	identTypes map[string]tree.JavaType
}

func (v *typeCollector) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	if ident.Type != nil {
		v.identTypes[ident.Name] = ident.Type
	}
	return ident
}

// methodTypeCollector visits a tree and collects method declaration types.
type methodTypeCollector struct {
	visitor.GoVisitor
	methodTypes map[string]*tree.JavaTypeMethod
}

func (v *methodTypeCollector) VisitMethodDeclaration(md *tree.MethodDeclaration, p any) tree.J {
	if md.MethodType != nil && md.Name != nil {
		v.methodTypes[md.Name.Name] = md.MethodType
	}
	return v.GoVisitor.VisitMethodDeclaration(md, p)
}

// methodInvocationCollector visits a tree and collects method invocation types.
type methodInvocationCollector struct {
	visitor.GoVisitor
	methodTypes map[string]*tree.JavaTypeMethod
}

func (v *methodInvocationCollector) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
	if mi.MethodType != nil && mi.Name != nil {
		v.methodTypes[mi.Name.Name] = mi.MethodType
	}
	return v.GoVisitor.VisitMethodInvocation(mi, p)
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
	if err != nil {
		t.Fatal(err)
	}

	v := visitor.Init(&typeCollector{identTypes: make(map[string]tree.JavaType)})
	v.Visit(cu, nil)

	// "x" should be an int type
	if xType, ok := v.identTypes["x"]; ok {
		if prim, ok := xType.(*tree.JavaTypePrimitive); ok {
			if prim.Keyword != "int" {
				t.Errorf("expected x to be int, got %s", prim.Keyword)
			}
		} else {
			t.Errorf("expected x to be primitive, got %T", xType)
		}
	} else {
		t.Error("no type attribution for x")
	}

	// "y" should be a string type (mapped as Primitive "String")
	if yType, ok := v.identTypes["y"]; ok {
		if prim, ok := yType.(*tree.JavaTypePrimitive); ok {
			if prim.Keyword != "String" {
				t.Errorf("expected y to be String, got %s", prim.Keyword)
			}
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
	if err != nil {
		t.Fatal(err)
	}

	v := visitor.Init(&methodTypeCollector{methodTypes: make(map[string]*tree.JavaTypeMethod)})
	v.Visit(cu, nil)

	addType, ok := v.methodTypes["add"]
	if !ok {
		t.Fatal("no method type for add()")
	}
	if addType.Name != "add" {
		t.Errorf("expected method name 'add', got '%s'", addType.Name)
	}
	if len(addType.ParameterTypes) != 2 {
		t.Errorf("expected 2 parameters, got %d", len(addType.ParameterTypes))
	}
	if ret, ok := addType.ReturnType.(*tree.JavaTypePrimitive); ok {
		if ret.Keyword != "int" {
			t.Errorf("expected return type int, got %s", ret.Keyword)
		}
	} else {
		t.Errorf("expected primitive return type, got %T", addType.ReturnType)
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
	if err != nil {
		t.Fatal(err)
	}

	v := visitor.Init(&methodInvocationCollector{methodTypes: make(map[string]*tree.JavaTypeMethod)})
	v.Visit(cu, nil)

	printlnType, ok := v.methodTypes["Println"]
	if !ok {
		t.Fatal("no method type for fmt.Println()")
	}
	if printlnType.Name != "Println" {
		t.Errorf("expected method name 'Println', got '%s'", printlnType.Name)
	}
	if printlnType.DeclaringType == nil {
		t.Fatal("expected declaring type for Println")
	}
	if printlnType.DeclaringType.FullyQualifiedName != "fmt" {
		t.Errorf("expected declaring type 'fmt', got '%s'", printlnType.DeclaringType.FullyQualifiedName)
	}
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
	if err != nil {
		t.Fatal(err)
	}

	v := visitor.Init(&typeCollector{identTypes: make(map[string]tree.JavaType)})
	v.Visit(cu, nil)

	// "p" should have a type attributed to it
	if pType, ok := v.identTypes["p"]; ok {
		if cls, ok := pType.(*tree.JavaTypeClass); ok {
			if cls.FullyQualifiedName != "main.Point" {
				t.Errorf("expected p to be main.Point, got %s", cls.FullyQualifiedName)
			}
			if cls.Kind != "Class" {
				t.Errorf("expected kind Class, got %s", cls.Kind)
			}
		} else {
			t.Errorf("expected p to be class type, got %T", pType)
		}
	} else {
		t.Error("no type attribution for p")
	}
}

func TestTypeAttributionGracefulDegradation(t *testing.T) {
	// Parse with unresolvable import — should not error, just have nil types
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

import "github.com/nonexistent/pkg"

func main() {
	pkg.DoSomething()
}
`)
	// Parser should still succeed
	if err != nil {
		t.Fatal(err)
	}
	if cu == nil {
		t.Fatal("expected non-nil compilation unit")
	}
}

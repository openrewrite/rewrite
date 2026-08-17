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

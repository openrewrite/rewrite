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
)

func TestExpectType_ClassType(t *testing.T) {
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
	ExpectType(t, cu, "p", "main.Point")
}

func TestExpectPrimitiveType_LocalVar(t *testing.T) {
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
	ExpectPrimitiveType(t, cu, "x", "int")
	ExpectPrimitiveType(t, cu, "y", "String")
}

func TestExpectMethodType_StdlibInvocation(t *testing.T) {
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
	ExpectMethodType(t, cu, "Println", "fmt")
}

func TestExpectMethodType_LocalDeclaration(t *testing.T) {
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", `package main

func add(a int, b int) int {
	return a + b
}
`)
	if err != nil {
		t.Fatal(err)
	}
	ExpectMethodType(t, cu, "add", "main")
}

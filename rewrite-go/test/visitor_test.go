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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// deletingVisitor returns nil for Return nodes, exercising the nil guard in visitAndCast/visitExpression.
type deletingVisitor struct {
	visitor.GoVisitor
}

func (v *deletingVisitor) VisitReturn(ret *tree.Return, p any) tree.J {
	return nil // delete the return statement
}

func TestVisitorReturningNilDoesNotPanic(t *testing.T) {
	src := "package main\n\nfunc foo() int {\n\treturn 1\n}\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}

	v := visitor.Init(&deletingVisitor{})
	// This should not panic even though VisitReturn returns nil.
	result := v.Visit(cu, nil)
	if result == nil {
		t.Fatal("visitor returned nil for compilation unit")
	}
}

// importCountingVisitor counts how many Import nodes are visited.
type importCountingVisitor struct {
	visitor.GoVisitor
	count int
}

func (v *importCountingVisitor) VisitImport(imp *tree.Import, p any) tree.J {
	v.count++
	return imp
}

func TestVisitorVisitsImports(t *testing.T) {
	src := "package main\n\nimport (\n\t\"fmt\"\n\t\"os\"\n)\n\nfunc main() {\n}\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}

	v := visitor.Init(&importCountingVisitor{})
	v.Visit(cu, nil)
	if v.count != 2 {
		t.Errorf("expected 2 imports visited, got %d", v.count)
	}
}

// identCountingVisitor counts how many Identifier nodes are visited.
type identCountingVisitor struct {
	visitor.GoVisitor
	names []string
}

func (v *identCountingVisitor) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	v.names = append(v.names, ident.Name)
	return ident
}

func TestVisitorVisitsPackageDecl(t *testing.T) {
	// "main" appears as the package name and as the function name.
	// Without visiting PackageDecl, only the function name would be found.
	src := "package pkg\n\nfunc foo() {\n}\n"
	p := parser.NewGoParser()
	cu, err := p.Parse("test.go", src)
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}

	v := visitor.Init(&identCountingVisitor{})
	v.Visit(cu, nil)

	found := false
	for _, name := range v.names {
		if name == "pkg" {
			found = true
			break
		}
	}
	if !found {
		t.Errorf("visitor did not visit package decl identifier 'pkg'; visited: %v", v.names)
	}
}

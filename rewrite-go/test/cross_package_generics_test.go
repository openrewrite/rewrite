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

package test

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// genericsScaffold builds a multi-package project rooted at a temp dir and
// returns the parsed compilation units keyed by their relative path. Each
// case stages files via the on-disk vendor walker pattern so the parser
// resolves cross-package generic references the same way it does in
// production parses.
func genericsScaffold(t *testing.T, files map[string]string, modulePath, mainRel string) *tree.CompilationUnit {
	t.Helper()
	root := t.TempDir()
	for rel, content := range files {
		full := filepath.Join(root, rel)
		if err := os.MkdirAll(filepath.Dir(full), 0o755); err != nil {
			t.Fatalf("mkdir %s: %v", full, err)
		}
		if err := os.WriteFile(full, []byte(content), 0o644); err != nil {
			t.Fatalf("write %s: %v", full, err)
		}
	}
	pi := parser.NewProjectImporter(modulePath, nil)
	pi.SetProjectRoot(root)
	for rel, content := range files {
		pi.AddSource(rel, content)
	}
	p := parser.NewGoParser()
	p.Importer = pi

	mainContent := files[mainRel]
	cu, err := p.Parse(mainRel, mainContent)
	if err != nil {
		t.Fatalf("parse %s: %v", mainRel, err)
	}
	return cu
}

// Case 1: Generic function used across packages.
//
//	package a
//	func Map[T any](xs []T, f func(T) T) []T { ... }
//
//	package main
//	import "example.com/foo/a"
//	func main() { a.Map([]int{1, 2, 3}, double) }
//
// The MethodInvocation for `a.Map` should carry a non-nil MethodType
// whose DeclaringType.FullyQualifiedName == the imported package's path.
// Without cross-package generic resolution the MethodType would be nil.
func TestCrossPackageGenerics_GenericFunc(t *testing.T) {
	cu := genericsScaffold(t, map[string]string{
		"a/a.go": `package a

func Map[T any](xs []T, f func(T) T) []T {
	out := make([]T, len(xs))
	for i, x := range xs {
		out[i] = f(x)
	}
	return out
}
`,
		"main.go": `package main

import "example.com/foo/a"

func double(x int) int { return x * 2 }

func main() {
	_ = a.Map([]int{1, 2, 3}, double)
}
`,
	}, "example.com/foo", "main.go")
	ExpectMethodType(t, cu, "Map", "example.com/foo/a")
}

// Case 2: Generic struct used across packages.
//
//	package a
//	type Box[T any] struct{ V T }
//
//	package main
//	import "example.com/foo/a"
//	var b a.Box[int]
//
// The `Box` identifier in main should resolve to a FullyQualified type
// whose FQN is the package's full path.
func TestCrossPackageGenerics_GenericStruct(t *testing.T) {
	cu := genericsScaffold(t, map[string]string{
		"a/a.go": `package a

type Box[T any] struct{ V T }
`,
		"main.go": `package main

import "example.com/foo/a"

func main() {
	_ = a.Box[int]{V: 42}
}
`,
	}, "example.com/foo", "main.go")
	ExpectType(t, cu, "Box", "example.com/foo/a.Box")
}

// Case 3: Multi-parameter generics across packages.
//
//	package a
//	type Pair[K, V comparable] struct{ K K; V V }
//
//	package main
//	import "example.com/foo/a"
//	var p a.Pair[string, int]
//
// Pair should resolve to a FullyQualified type whose FQN ends in `Pair`
// and the multi-param shape doesn't break attribution.
func TestCrossPackageGenerics_MultiParam(t *testing.T) {
	cu := genericsScaffold(t, map[string]string{
		"a/a.go": `package a

type Pair[K, V comparable] struct {
	Key K
	Val V
}
`,
		"main.go": `package main

import "example.com/foo/a"

func main() {
	_ = a.Pair[string, int]{Key: "x", Val: 1}
}
`,
	}, "example.com/foo", "main.go")
	ExpectType(t, cu, "Pair", "example.com/foo/a.Pair")
}

// Case 4: Bounded type parameters across packages.
//
//	package a
//	func Sum[T int | float64](xs []T) T { ... }
//
//	package main
//	import "example.com/foo/a"
//	func main() { _ = a.Sum([]int{1, 2}) }
//
// Even with a union constraint, the cross-package call should resolve.
func TestCrossPackageGenerics_BoundedTypeParam(t *testing.T) {
	cu := genericsScaffold(t, map[string]string{
		"a/a.go": `package a

func Sum[T int | float64](xs []T) T {
	var total T
	for _, x := range xs {
		total += x
	}
	return total
}
`,
		"main.go": `package main

import "example.com/foo/a"

func main() {
	_ = a.Sum([]int{1, 2, 3})
}
`,
	}, "example.com/foo", "main.go")
	ExpectMethodType(t, cu, "Sum", "example.com/foo/a")
}

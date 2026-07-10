/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package main

import (
	"encoding/json"
	"path/filepath"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// TestParseProjectParsesTestFiles pins down that `_test.go` files are parsed
// as project sources (previously excluded), and that a black-box external
// test package (`package foo_test`) co-located with its production package
// still gets type attribution — i.e. it is type-checked separately from the
// production files and resolves the production package through the importer.
func TestParseProjectParsesTestFiles(t *testing.T) {
	// given
	s, _ := newTestServer(t)
	projectDir := t.TempDir()
	writeFile(t, filepath.Join(projectDir, "go.mod"), "module example.com/m\n\ngo 1.22\n")
	writeFile(t, filepath.Join(projectDir, "foo", "foo.go"),
		"package foo\n\nfunc Hello() string { return \"hi\" }\n")
	// In-package (white-box) test: same `package foo`.
	writeFile(t, filepath.Join(projectDir, "foo", "foo_internal_test.go"),
		"package foo\n\nfunc internalHello() string { return Hello() }\n")
	// Black-box (external) test: `package foo_test`, co-located with the
	// production package. It must be type-checked as its own package — if it
	// were lumped in with `package foo`, go/types would drop the whole file
	// and `helper()` below would lose its type attribution.
	writeFile(t, filepath.Join(projectDir, "foo", "foo_test.go"),
		"package foo_test\n\nfunc helper() string { return \"x\" }\nfunc useHelper() string { return helper() }\n")

	relativeTo := projectDir
	params, err := json.Marshal(parseProjectRequest{ProjectPath: projectDir, RelativeTo: &relativeTo})
	if err != nil {
		t.Fatalf("marshal params: %v", err)
	}

	// when
	if _, rpcErr := s.handleParseProject(params); rpcErr != nil {
		t.Fatalf("handleParseProject: %v", rpcErr.Message)
	}

	// then
	cusByPath := map[string]*golang.CompilationUnit{}
	for _, obj := range s.localObjects {
		if cu, ok := obj.(*golang.CompilationUnit); ok {
			cusByPath[filepath.ToSlash(cu.SourcePath)] = cu
		}
	}
	for _, want := range []string{"foo/foo.go", "foo/foo_internal_test.go", "foo/foo_test.go"} {
		if _, ok := cusByPath[want]; !ok {
			t.Errorf("expected %q to be parsed into a CompilationUnit", want)
		}
	}

	// The black-box test's `helper()` call must be type-attributed, which only
	// holds if the file was type-checked as its own `foo_test` package rather
	// than lumped in with the co-located `package foo` sources.
	blackBox := cusByPath["foo/foo_test.go"]
	if blackBox == nil {
		t.Fatal("black-box test CU missing; cannot check attribution")
	}
	var attributed bool
	visitor.Walk(blackBox, func(tr java.Tree) bool {
		if mi, ok := tr.(*java.MethodInvocation); ok && mi.Name != nil &&
			mi.Name.Name == "helper" && mi.MethodType != nil {
			attributed = true
			return false
		}
		return true
	})
	if !attributed {
		t.Error("expected helper() in the black-box test package to be type-attributed")
	}
}

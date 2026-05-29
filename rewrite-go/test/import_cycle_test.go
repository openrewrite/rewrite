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
	"go/types"
	"testing"
	"time"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
)

// importWithTimeout runs pi.Import(importPath) in a separate goroutine and
// fails the test if it does not return within the deadline. Without an
// in-progress cycle guard the import recurses unboundedly; this surfaces as
// either a goroutine that never returns (caught by the timeout here) or a
// `fatal error: stack overflow` that crashes the test binary outright.
func importWithTimeout(t *testing.T, pi *parser.ProjectImporter, importPath string) *types.Package {
	t.Helper()
	type result struct {
		pkg *types.Package
		err error
	}
	ch := make(chan result, 1)
	go func() {
		pkg, err := pi.Import(importPath)
		ch <- result{pkg, err}
	}()
	select {
	case r := <-ch:
		if r.err != nil {
			t.Fatalf("Import(%q) returned error: %v", importPath, r.err)
		}
		if r.pkg == nil {
			t.Fatalf("Import(%q) returned nil package", importPath)
		}
		return r.pkg
	case <-time.After(15 * time.Second):
		t.Fatalf("Import(%q) did not return within 15s — unbounded recursion (missing import-cycle guard)", importPath)
		return nil
	}
}

// A direct import cycle between two sibling source packages: a imports b and
// b imports a. Each Import re-enters parsePackage for a path that hasn't been
// cached yet (the cache is only populated after parsePackage returns), so
// without an in-progress guard the type-checker recurses until the goroutine
// stack overflows. The fix must break the cycle and return a usable package.
func TestProjectImporter_DirectImportCycle(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/foo", nil)
	pi.AddSource("a/a.go", "package a\n\nimport \"example.com/foo/b\"\n\nfunc A() string { return b.B() }\n")
	pi.AddSource("b/b.go", "package b\n\nimport \"example.com/foo/a\"\n\nfunc B() string { return a.A() }\n")

	pkg := importWithTimeout(t, pi, "example.com/foo/a")
	if pkg.Path() != "example.com/foo/a" {
		t.Errorf("expected package path %q, got %q", "example.com/foo/a", pkg.Path())
	}
}

// A longer cycle (a -> b -> c -> a) exercises the same guard across more than
// two hops, mirroring the deep, indirectly-cyclic import graphs in real-world
// repositories such as entgo.io/ent that originally triggered the crash.
func TestProjectImporter_TransitiveImportCycle(t *testing.T) {
	pi := parser.NewProjectImporter("example.com/foo", nil)
	pi.AddSource("a/a.go", "package a\n\nimport \"example.com/foo/b\"\n\nfunc A() string { return b.B() }\n")
	pi.AddSource("b/b.go", "package b\n\nimport \"example.com/foo/c\"\n\nfunc B() string { return c.C() }\n")
	pi.AddSource("c/c.go", "package c\n\nimport \"example.com/foo/a\"\n\nfunc C() string { return a.A() }\n")

	pkg := importWithTimeout(t, pi, "example.com/foo/a")
	if pkg.Path() != "example.com/foo/a" {
		t.Errorf("expected package path %q, got %q", "example.com/foo/a", pkg.Path())
	}
}

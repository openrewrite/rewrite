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
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// TestParsePackageResolvesCrossFileSymbols directly exercises ParsePackage
// without the test harness: file A calls a function defined in file B,
// both in the same package. The shared types.Info should populate B's
// definition AND A's reference with the same types.Object.
func TestParsePackageResolvesCrossFileSymbols(t *testing.T) {
	cus, err := parser.NewGoParser().ParsePackage([]parser.FileInput{
		{Path: "main.go", Content: "package main\n\nfunc main() { helper() }\n"},
		{Path: "helper.go", Content: "package main\n\nfunc helper() {}\n"},
	})
	if err != nil {
		t.Fatalf("parse error: %v", err)
	}
	if len(cus) != 2 {
		t.Fatalf("expected 2 CUs, got %d", len(cus))
	}

	mainTypes := collectIdentTypes(cus[0])
	if mainTypes["helper"] == nil {
		t.Errorf("expected `helper` reference in main.go to have a non-nil Type after multi-file parse; got nil (cross-file resolution still broken)")
	}
}

// TestGoProjectMultiFilePackageResolves is the harness-level integration:
// inside a GoProject with a go.mod, two files at the project root both
// declare `package main` and reference each other.
func TestGoProjectMultiFilePackageResolves(t *testing.T) {
	mainSrc := test.Golang(`
		package main

		func main() { helper() }
	`).WithPath("main.go")
	mainSrc.AfterRecipe = func(t *testing.T, cu *tree.CompilationUnit) {
		t.Helper()
		ids := collectIdentTypes(cu)
		if ids["helper"] == nil {
			t.Errorf("`helper` reference should have a resolved Type when parsed alongside helper.go; got nil")
		}
	}

	helperSrc := test.Golang(`
		package main

		func helper() {}
	`).WithPath("helper.go")

	spec := test.NewRecipeSpec()
	spec.RewriteRun(t,
		test.GoProject("foo",
			test.GoMod(`
				module example.com/foo

				go 1.22
			`),
			helperSrc,
			mainSrc,
		),
	)
}


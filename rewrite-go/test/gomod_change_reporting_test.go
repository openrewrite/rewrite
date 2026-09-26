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
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

const identityGoMod = "module example.com/foo\n\ngo 1.21\n\nrequire (\n\tgithub.com/a/b v1.0.0\n\tgithub.com/c/d v1.5.0 // indirect\n)\n"

const identityGoSum = "github.com/a/b v1.0.0 h1:aaa=\n" +
	"github.com/a/b v1.0.0/go.mod h1:bbb=\n" +
	"github.com/c/d v1.5.0 h1:ccc=\n"

// A recipe that only rewrites .go syntax is still handed the go.mod and go.sum
// LSTs to traverse.
func TestGoOnlyRecipeLeavesGoModAndGoSumPointersUntouched(t *testing.T) {
	v := (&recipes.RemoveUnusedImports{}).Editor()

	gm, err := parser.ParseGoModFile("go.mod", identityGoMod)
	if err != nil {
		t.Fatalf("go.mod parse error: %v", err)
	}
	if after := v.Visit(gm, nil); after != java.Tree(gm) {
		t.Error("go.mod came back as a new tree from a recipe that only edits .go sources")
	}

	gs, err := parser.ParseGoSumFile("go.sum", identityGoSum)
	if err != nil {
		t.Fatalf("go.sum parse error: %v", err)
	}
	if after := v.Visit(gs, nil); after != java.Tree(gs) {
		t.Error("go.sum came back as a new tree from a recipe that only edits .go sources")
	}
}

func TestGoModEditingRecipeStillReportsAChange(t *testing.T) {
	gm, err := parser.ParseGoModFile("go.mod", identityGoMod)
	if err != nil {
		t.Fatalf("go.mod parse error: %v", err)
	}
	v := (&changeGoVersion{NewVersion: "1.23"}).Editor()
	if after := v.Visit(gm, nil); after == java.Tree(gm) {
		t.Error("a recipe that rewrites the go directive reported no change")
	}
}

func TestGoOnlyRecipeAcrossAProject(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&recipes.RemoveUnusedImports{})
	spec.RewriteRun(t,
		test.GoProject("foo",
			test.GoMod(identityGoMod),
			test.GoSum(identityGoSum),
			test.Golang(`
				package main

				import "fmt"

				func main() {}
			`, `
				package main

				func main() {}
			`),
		),
	)
}

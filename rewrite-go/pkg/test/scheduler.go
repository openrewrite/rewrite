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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// runCycle runs one cycle of r and its recipe list over trees the way Java's
// RecipeRunCycle does: every scanning recipe scans every source before any
// recipe generates, every recipe generates before any edits, and each source
// then goes through every recipe in turn. A nil result for an existing source
// is its deletion; generated files the cycle deleted are left out.
func runCycle(r recipe.Recipe, trees []java.Tree, ctx *recipe.ExecutionContext) ([]java.Tree, []java.Tree) {
	recipes := recipeStack(r)
	accs := make([]any, len(recipes))
	for i, rc := range recipes {
		if sr, ok := rc.(recipe.ScanningRecipe); ok {
			accs[i] = sr.InitialValue(ctx)
		}
	}

	for _, tr := range trees {
		for i, rc := range recipes {
			if sr, ok := rc.(recipe.ScanningRecipe); ok {
				scan(sr, accs[i], tr, ctx)
			}
		}
	}

	var generated []java.Tree
	for i, rc := range recipes {
		sr, ok := rc.(recipe.ScanningRecipe)
		if !ok {
			continue
		}
		for _, g := range generated {
			scan(sr, accs[i], g, ctx)
		}
		for _, g := range sr.Generate(accs[i], ctx) {
			if g != nil {
				generated = append(generated, g)
			}
		}
	}

	results := make([]java.Tree, len(trees))
	for k, tr := range trees {
		results[k] = edit(recipes, accs, tr, ctx)
	}
	var kept []java.Tree
	for _, g := range generated {
		if g = edit(recipes, accs, g, ctx); g != nil {
			kept = append(kept, g)
		}
	}
	return results, kept
}

// recipeStack lists r and its recipe list depth first, in the order Java's
// RecipeStack runs them.
func recipeStack(r recipe.Recipe) []recipe.Recipe {
	recipes := []recipe.Recipe{r}
	for _, sub := range r.RecipeList() {
		recipes = append(recipes, recipeStack(sub)...)
	}
	return recipes
}

func scan(sr recipe.ScanningRecipe, acc any, tr java.Tree, ctx *recipe.ExecutionContext) {
	scanner := sr.Scanner(acc)
	if scanner == nil {
		return
	}
	if after := scanner.Visit(tr, ctx); after != nil {
		visitor.DrainAfterVisits(scanner, after, ctx)
	}
}

func edit(recipes []recipe.Recipe, accs []any, tr java.Tree, ctx *recipe.ExecutionContext) java.Tree {
	for i, rc := range recipes {
		var editor recipe.TreeVisitor
		if sr, ok := rc.(recipe.ScanningRecipe); ok {
			editor = sr.EditorWithData(accs[i])
		} else {
			editor = rc.Editor()
		}
		if editor == nil {
			continue
		}
		after := editor.Visit(tr, ctx)
		if after == nil {
			if q, ok := editor.(visitor.AfterVisitsProvider); ok {
				q.AfterVisits()
			}
			return nil
		}
		tr = visitor.DrainAfterVisits(editor, after, ctx)
	}
	return tr
}

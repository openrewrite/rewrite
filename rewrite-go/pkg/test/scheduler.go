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
// is its deletion; generated files the cycle deleted are left out. checks, when
// not nil, records how the recipes behave as they run.
func runCycle(r recipe.Recipe, trees []java.Tree, ctx *recipe.ExecutionContext, checks *runChecks) ([]java.Tree, []java.Tree) {
	recipes := recipeStack(r)
	accs := make([]any, len(recipes))
	for i, rc := range recipes {
		if sr, ok := rc.(recipe.ScanningRecipe); ok {
			done := checks.watch(rc, ctx)
			accs[i] = sr.InitialValue(ctx)
			done("while creating its accumulator")
		}
	}

	for _, tr := range trees {
		for i, rc := range recipes {
			if sr, ok := rc.(recipe.ScanningRecipe); ok {
				checks.scan(sr, accs[i], tr, ctx)
			}
		}
	}
	checks.checkAccumulators(recipes, accs, "after scanning")

	var generated []java.Tree
	for i, rc := range recipes {
		sr, ok := rc.(recipe.ScanningRecipe)
		if !ok {
			continue
		}
		for _, g := range generated {
			checks.scan(sr, accs[i], g, ctx)
		}
		done := checks.watch(rc, ctx)
		gen := sr.Generate(accs[i], ctx)
		done("while generating")
		for _, g := range gen {
			if g != nil {
				generated = append(generated, g)
			}
		}
	}

	results := make([]java.Tree, len(trees))
	for k, tr := range trees {
		results[k] = edit(recipes, accs, tr, ctx, checks)
	}
	var kept []java.Tree
	for _, g := range generated {
		if g = edit(recipes, accs, g, ctx, checks); g != nil {
			kept = append(kept, g)
		}
	}
	checks.checkAccumulators(recipes, accs, "after editing")
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

// scan runs the scanner of sr on tr, returning what the scanner made of it.
func scan(sr recipe.ScanningRecipe, acc any, tr java.Tree, ctx *recipe.ExecutionContext) java.Tree {
	scanner := sr.Scanner(acc)
	if scanner == nil {
		return tr
	}
	after := scanner.Visit(tr, ctx)
	if after != nil {
		after = visitor.DrainAfterVisits(scanner, after, ctx)
	}
	return after
}

func edit(recipes []recipe.Recipe, accs []any, tr java.Tree, ctx *recipe.ExecutionContext, checks *runChecks) java.Tree {
	for i, rc := range recipes {
		done := checks.watch(rc, ctx)
		where := "while editing " + sourcePathOf(tr)
		var editor recipe.TreeVisitor
		if sr, ok := rc.(recipe.ScanningRecipe); ok {
			editor = sr.EditorWithData(accs[i])
		} else {
			editor = rc.Editor()
		}
		if editor == nil {
			done(where)
			continue
		}
		after := editor.Visit(tr, ctx)
		if after == nil {
			if q, ok := editor.(visitor.AfterVisitsProvider); ok {
				q.AfterVisits()
			}
			done(where)
			return nil
		}
		tr = visitor.DrainAfterVisits(editor, after, ctx)
		done(where)
	}
	return tr
}

// runChecks are the checks RewriteRun makes of how a recipe behaves while it
// runs, with what they found.
type runChecks struct {
	executionContextUnchanged bool
	noEditsInScanningPhase    bool
	lightweightAccumulators   bool
	contextChanges            []contextChange
	scannerEdits              []scannerEdit
	accumulators              accumulatorContents
}

// scan runs the scanner of sr on tr as the checks watch it.
func (c *runChecks) scan(sr recipe.ScanningRecipe, acc any, tr java.Tree, ctx *recipe.ExecutionContext) {
	done := c.watch(sr, ctx)
	after := scan(sr, acc, tr, ctx)
	done("while scanning " + sourcePathOf(tr))
	if c != nil && c.noEditsInScanningPhase && after != tr {
		c.scannerEdited(scannerEdit{recipe: sr.Name(), sourcePath: sourcePathOf(tr), deleted: after == nil})
	}
}

// failures describes what the checks found, one failure per check.
func (c *runChecks) failures() []string {
	if c == nil {
		return nil
	}
	var failures []string
	if len(c.contextChanges) > 0 {
		failures = append(failures, contextChangesFailure(c.contextChanges))
	}
	if len(c.scannerEdits) > 0 {
		failures = append(failures, scannerEditsFailure(c.scannerEdits))
	}
	if failure := c.accumulators.failure(); failure != "" {
		failures = append(failures, failure)
	}
	return failures
}

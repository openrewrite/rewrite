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
	"math"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// scanOntoContext keeps the path of each source file it scans on the
// ExecutionContext.
type scanOntoContext struct {
	recipe.ScanningBase
}

const scannedKey = "org.openrewrite.golang.test.scanned"

func (r *scanOntoContext) Name() string        { return "org.openrewrite.golang.test.ScanOntoContext" }
func (r *scanOntoContext) DisplayName() string { return "Scan onto the context" }
func (r *scanOntoContext) Description() string {
	return "Keeps the path of each source file it scans on the execution context."
}

func (r *scanOntoContext) InitialValue(*recipe.ExecutionContext) any { return nil }

func (r *scanOntoContext) Scanner(any) recipe.TreeVisitor {
	return visitor.Init(&contextScanner{})
}

type contextScanner struct {
	visitor.GoVisitor
}

func (v *contextScanner) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	p.(*recipe.ExecutionContext).PutMessage(scannedKey, cu.SourcePath)
	return cu
}

// keepNoB tells the recipes after it to keep `b` no longer.
type keepNoB struct {
	recipe.Base
}

func (r *keepNoB) Name() string        { return "org.openrewrite.golang.test.KeepNoB" }
func (r *keepNoB) DisplayName() string { return "Keep no `b`" }
func (r *keepNoB) Description() string { return "Tells the recipes after it to keep `b` no longer." }

func (r *keepNoB) Editor() recipe.TreeVisitor {
	return visitor.Init(&keepNoBVisitor{})
}

type keepNoBVisitor struct {
	visitor.GoVisitor
}

func (v *keepNoBVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	p.(*recipe.ExecutionContext).PutMessage(keepBKey, false)
	return cu
}

func TestRecipePuttingAMessageOnTheExecutionContextFails(t *testing.T) {
	// given a recipe that counts source files on the execution context
	spec := NewRecipeSpec().WithRecipe(&tally{count: true, report: true})

	// when it runs over two source files
	got := failures(t, spec,
		Golang(`
			package main
		`, `
			/*~~(tally 1)~~>*/package main
		`).WithPath("a.go"),
		Golang(`
			package main
		`, `
			/*~~(tally 2)~~>*/package main
		`).WithPath("b.go"),
	)

	// then the run fails once, naming the message and where the recipe put it
	assert.Equal(t, []string{
		"Keep state that spans source files in a ScanningRecipe's accumulator, and state within one source file " +
			"in cursor messages, with v.Cursor().PutMessage(...) or v.Cursor().PutMessageOnFirstEnclosing(...), " +
			"instead of on the ExecutionContext:\n" +
			"  org.openrewrite.golang.test.Tally put \"org.openrewrite.golang.test.tally\" on the ExecutionContext " +
			"while editing a.go\n" +
			"Every recipe and source file in a run shares the ExecutionContext, so what a recipe keeps there leaks " +
			"into the others. A recipe that must change the ExecutionContext opts out with " +
			"spec.CheckExecutionContextUnchanged = false.",
	}, got)
}

func TestRecipeReplacingAMessageFails(t *testing.T) {
	// given a recipe that changes a message the test put on the execution context
	ctx := recipe.NewExecutionContext()
	ctx.PutMessage(keepBKey, true)
	spec := NewRecipeSpec().WithRecipe(&keepNoB{}).WithExecutionContext(ctx)

	// when it runs
	got := failures(t, spec, Golang(`
		package main
	`))

	// then the changed message is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "  org.openrewrite.golang.test.KeepNoB put \"org.openrewrite.golang.test.keepB\" "+
		"on the ExecutionContext while editing test.go\n")
}

func TestScannerPuttingAMessageOnTheExecutionContextFails(t *testing.T) {
	// given a scanning recipe whose scanner keeps what it scans on the execution context
	spec := NewRecipeSpec().WithRecipe(&scanOntoContext{})

	// when it runs
	got := failures(t, spec, Golang(`
		package main
	`))

	// then the scan is reported
	require.Len(t, got, 1)
	assert.Contains(t, got[0], "  org.openrewrite.golang.test.ScanOntoContext put "+
		"\"org.openrewrite.golang.test.scanned\" on the ExecutionContext while scanning test.go\n")
}

func TestRecipeReadingTheExecutionContextPasses(t *testing.T) {
	ctx := recipe.NewExecutionContext()
	ctx.PutMessage(keepBKey, true)
	NewRecipeSpec().
		WithRecipe(&renameBToCUnlessKept{}).
		WithExecutionContext(ctx).
		RewriteRun(t, Golang(`
			package main

			func b() {}
		`))
}

// handlerOptions holds a func and a NaN, which reflect.DeepEqual finds unequal to themselves.
type handlerOptions struct {
	onError func(error)
	ratio   float64
}

func TestRecipeLeavingAMessageThatHoldsAFuncAlonePasses(t *testing.T) {
	ctx := recipe.NewExecutionContext()
	ctx.PutMessage("org.openrewrite.golang.test.options", handlerOptions{onError: func(error) {}, ratio: math.NaN()})
	NewRecipeSpec().
		WithRecipe(&renameIdentifiers{rename: renameAToB}).
		WithExecutionContext(ctx).
		RewriteRun(t, Golang(`
			package main

			func a() {}
		`, `
			package main

			func b() {}
		`))
}

func TestExecutionContextCheckCanBeTurnedOff(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&tally{count: true, report: true})
	spec.CheckExecutionContextUnchanged = false
	spec.RewriteRun(t, Golang(`
		package main
	`, `
		/*~~(tally 1)~~>*/package main
	`))
}

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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// TestImportService_RegisteredOnInit verifies that simply importing
// pkg/recipe/golang triggers registration of *golang.ImportService.
func TestImportService_RegisteredOnInit(t *testing.T) {
	svc := recipe.Service[*recipes.ImportService](nil)
	if svc == nil {
		t.Fatal("recipe.Service returned nil for *golang.ImportService")
	}
}

// addStringsImportRecipe is a recipe that uses ImportService via
// DoAfterVisit to add a "strings" import as a side-effect of finding a
// MethodInvocation matching a target name. Demonstrates the canonical
// composition pattern.
type addStringsImportRecipe struct {
	recipe.Base
}

func (r *addStringsImportRecipe) Name() string        { return "test.AddStringsImport" }
func (r *addStringsImportRecipe) DisplayName() string { return "Add strings import via service" }
func (r *addStringsImportRecipe) Description() string { return "Test recipe." }

func (r *addStringsImportRecipe) Editor() recipe.TreeVisitor {
	return visitor.Init(&addStringsVisitor{})
}

type addStringsVisitor struct{ visitor.GoVisitor }

func (v *addStringsVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	mi = v.GoVisitor.VisitMethodInvocation(mi, p).(*java.MethodInvocation)
	if mi.Name != nil && mi.Name.Name == "Println" {
		// Recipe edits don't change the call here — just queue a
		// follow-up visitor that adds the import. The harness drains
		// the after-visits queue after the main visit completes.
		svc := recipe.Service[*recipes.ImportService](nil)
		v.DoAfterVisit(svc.AddImportVisitor("strings", nil, false))
	}
	return mi
}

// TestImportService_AddImportViaDoAfterVisit demonstrates the full
// pattern: a recipe edits a method body, then queues an AddImport
// visitor via DoAfterVisit. The after-visit drain in the recipe
// runner applies it without explicit caller orchestration.
func TestImportService_AddImportViaDoAfterVisit(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&addStringsImportRecipe{})
	before := `
		package main

		import "fmt"

		func main() { fmt.Println("hi") }
	`
	after := `
		package main

		import (
			"fmt"
			"strings"
		)

		func main() { fmt.Println("hi") }
	`
	spec.RewriteRun(t, Golang(before, after))
}

func TestMaybeAddImport_DeduplicatesPendingVisitors(t *testing.T) {
	v := visitor.Init(&maybeAddImportPendingVisitor{})
	recipes.MaybeAddImport(v, "fmt", nil, false)
	recipes.MaybeAddImport(v, "fmt", nil, false)

	if got := len(v.PendingAfterVisits()); got != 1 {
		t.Fatalf("expected one pending AddImport visitor, got %d", got)
	}
}

type maybeAddImportPendingVisitor struct{ visitor.GoVisitor }

func TestMaybeAddImport_AddsImportViaDoAfterVisit(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&maybeAddFmtImportRecipe{})
	before := `
		package main

		func main() {}
	`
	after := `
		package main

		import "fmt"

		func main() {}
	`
	spec.RewriteRun(t, Golang(before, after))
}

type maybeAddFmtImportRecipe struct{ recipe.Base }

func (r *maybeAddFmtImportRecipe) Name() string        { return "test.MaybeAddFmtImport" }
func (r *maybeAddFmtImportRecipe) DisplayName() string { return "Add fmt import via MaybeAddImport" }
func (r *maybeAddFmtImportRecipe) Description() string { return "Test recipe." }
func (r *maybeAddFmtImportRecipe) Editor() recipe.TreeVisitor {
	return visitor.Init(&maybeAddFmtVisitor{})
}

type maybeAddFmtVisitor struct{ visitor.GoVisitor }

func (v *maybeAddFmtVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*golang.CompilationUnit)
	recipes.MaybeAddImport(v, "fmt", nil, false)
	return cu
}

// TestImportService_RemoveImport via DoAfterVisit.
func TestImportService_RemoveImportViaDoAfterVisit(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&removeFmtImportRecipe{})
	before := `
		package main

		import (
			"fmt"
			"strings"
		)

		func main() { strings.ToUpper("hi") }
	`
	after := `
		package main

		import (
			"strings"
		)

		func main() { strings.ToUpper("hi") }
	`
	spec.RewriteRun(t, Golang(before, after))
}

type removeFmtImportRecipe struct{ recipe.Base }

func (r *removeFmtImportRecipe) Name() string        { return "test.RemoveFmtImport" }
func (r *removeFmtImportRecipe) DisplayName() string { return "Remove fmt import via service" }
func (r *removeFmtImportRecipe) Description() string { return "Test recipe." }
func (r *removeFmtImportRecipe) Editor() recipe.TreeVisitor {
	return visitor.Init(&removeFmtVisitor{})
}

type removeFmtVisitor struct{ visitor.GoVisitor }

func (v *removeFmtVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*golang.CompilationUnit)
	svc := recipe.Service[*recipes.ImportService](nil)
	v.DoAfterVisit(svc.RemoveImportVisitor("fmt"))
	return cu
}

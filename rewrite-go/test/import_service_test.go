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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// TestImportService_RegisteredOnInit verifies that simply importing
// pkg/recipe/golang triggers registration of *golang.ImportService.
func TestImportService_RegisteredOnInit(t *testing.T) {
	svc := recipe.Service[*golang.ImportService](nil)
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

func (v *addStringsVisitor) VisitMethodInvocation(mi *tree.MethodInvocation, p any) tree.J {
	mi = v.GoVisitor.VisitMethodInvocation(mi, p).(*tree.MethodInvocation)
	if mi.Name != nil && mi.Name.Name == "Println" {
		// Recipe edits don't change the call here — just queue a
		// follow-up visitor that adds the import. The harness drains
		// the after-visits queue after the main visit completes.
		svc := recipe.Service[*golang.ImportService](nil)
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

func (v *removeFmtVisitor) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*tree.CompilationUnit)
	svc := recipe.Service[*golang.ImportService](nil)
	v.DoAfterVisit(svc.RemoveImportVisitor("fmt"))
	return cu
}

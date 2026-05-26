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

package golang

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang/internal"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// RemoveImport deletes a single import from a Go compilation unit.
//
// Matches by import path: any form (regular, aliased, dot, or blank)
// that imports PackagePath is removed. If the imports container becomes
// empty as a result, it is nil-ed out so the printer doesn't emit an
// empty `import ()` block.
//
// Removing an actively-referenced import will of course break the file;
// this recipe trusts the caller to know that. For unused-import cleanup,
// use RemoveUnusedImports.
type RemoveImport struct {
	recipe.Base
	PackagePath string
}

func (r *RemoveImport) Name() string        { return "org.openrewrite.golang.RemoveImport" }
func (r *RemoveImport) DisplayName() string { return "Remove import" }
func (r *RemoveImport) Description() string {
	return "Remove an `import` statement from a Go compilation unit. Matches by import path; any form (regular, aliased, dot, blank) is removed."
}

func (r *RemoveImport) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("packagePath", "Package path", "The import path to remove.").
			WithExample("fmt").WithValue(r.PackagePath),
	}
}

func (r *RemoveImport) Editor() recipe.TreeVisitor {
	return visitor.Init(&removeImportVisitor{cfg: r})
}

type removeImportVisitor struct {
	visitor.GoVisitor
	cfg *RemoveImport
}

func (v *removeImportVisitor) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*tree.CompilationUnit)
	if v.cfg.PackagePath == "" || cu.Imports == nil {
		return cu
	}
	for _, rp := range cu.Imports.Elements {
		if internal.ImportPath(rp.Element) == v.cfg.PackagePath {
			cu = internal.RemoveFromBlock(cu, rp.Element)
		}
	}
	return cu
}

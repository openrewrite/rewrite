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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Matches by import path, whichever form (regular, aliased, dot, or
// blank) the import takes. If the imports container becomes empty as a
// result, it is nil-ed out so the printer doesn't emit an empty
// `import ()` block.
//
// Force mirrors the Java recipe's flag of the same name. Left false, the
// import survives while the file still references it, and blank (`_`) and
// dot (`.`) imports survive outright under the rule RemoveUnusedImports
// documents. Set it to remove every matching form regardless.
type RemoveImport struct {
	recipe.Base
	PackagePath string
	Force       bool
}

func (r *RemoveImport) Name() string        { return "org.openrewrite.golang.RemoveImport" }
func (r *RemoveImport) DisplayName() string { return "Remove import" }
func (r *RemoveImport) Description() string {
	return "Remove an `import` statement from a Go compilation unit. Matches by import path, in any form (regular, aliased, dot, blank). Unless `force` is set, an import that the file still references is kept, as are blank (`_`) and dot (`.`) imports."
}

func (r *RemoveImport) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("packagePath", "Package path", "The import path to remove.").
			WithExample("fmt").WithValue(r.PackagePath),
		recipe.Option("force", "Force",
			"When true, remove the import even if the file still references the package, and remove blank (`_`) and dot (`.`) imports.").
			AsOptional().WithValue(r.Force),
	}
}

func (r *RemoveImport) Editor() recipe.TreeVisitor {
	return visitor.Init(&removeImportVisitor{cfg: r})
}

type removeImportVisitor struct {
	visitor.GoVisitor
	cfg *RemoveImport
}

func (v *removeImportVisitor) VisitCompilationUnit(cu *golang.CompilationUnit, p any) java.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*golang.CompilationUnit)
	if v.cfg.PackagePath == "" || cu.Imports == nil {
		return cu
	}
	var refs, quals map[string]bool
	if !v.cfg.Force {
		refs, quals = internal.ReferencedImports(cu)
	}
	for _, rp := range cu.Imports.Elements {
		imp := rp.Element
		if internal.ImportPath(imp) != v.cfg.PackagePath {
			continue
		}
		if !v.cfg.Force {
			if alias := internal.AliasName(imp); alias == "_" || alias == "." {
				continue
			}
			if internal.IsReferenced(imp, refs, quals) {
				continue
			}
		}
		cu = internal.RemoveFromBlock(cu, imp)
	}
	return cu
}

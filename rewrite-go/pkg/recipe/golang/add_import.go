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

// AddImport adds an `import` statement to a Go compilation unit.
//
// Mirrors the Java AddImport recipe with full surface parity (per the
// C1 directive in the eng review):
//
//   - PackagePath is the import path to add (e.g. "fmt", "github.com/x/y").
//   - Alias is the import alias: nil = regular import; "_" = blank import;
//     "." = dot import; any other identifier = aliased import.
//   - OnlyIfReferenced gates the add: true means add only when something
//     in the file references the package (typed identifier with the
//     matching FQN); false means add unconditionally.
//
// Cross-form idempotence: if the file already has an import of PackagePath
// in any form (regular, aliased, dot, or blank) AND the existing form is
// compatible with the requested form, AddImport is a no-op. Compatible
// means: requested alias == existing alias; or requested alias == nil and
// existing import is non-blank/non-dot.
type AddImport struct {
	recipe.Base
	PackagePath      string
	Alias            *string
	OnlyIfReferenced bool
}

func (r *AddImport) Name() string        { return "org.openrewrite.golang.AddImport" }
func (r *AddImport) DisplayName() string { return "Add import" }
func (r *AddImport) Description() string {
	return "Add an `import` statement to a Go compilation unit. No-op if the import is already present in a compatible form."
}

func (r *AddImport) Options() []recipe.OptionDescriptor {
	opts := []recipe.OptionDescriptor{
		recipe.Option("packagePath", "Package path", "The import path to add.").
			WithExample("fmt").WithValue(r.PackagePath),
	}
	if r.Alias != nil {
		opts = append(opts, recipe.Option("alias", "Alias", `The import alias. Use "_" for blank imports, "." for dot imports.`).
			AsOptional().WithExample("fmtutil").WithValue(*r.Alias))
	}
	opts = append(opts, recipe.Option("onlyIfReferenced", "Only if referenced",
		"When true, add the import only if some identifier in the file already references the package via type attribution.").
		AsOptional().WithValue(r.OnlyIfReferenced))
	return opts
}

func (r *AddImport) Editor() recipe.TreeVisitor {
	return visitor.Init(&addImportVisitor{cfg: r})
}

type addImportVisitor struct {
	visitor.GoVisitor
	cfg *AddImport
}

func (v *addImportVisitor) VisitCompilationUnit(cu *tree.CompilationUnit, p any) tree.J {
	cu = v.GoVisitor.VisitCompilationUnit(cu, p).(*tree.CompilationUnit)
	if v.cfg.PackagePath == "" {
		return cu
	}
	if internal.HasImport(cu, v.cfg.PackagePath, v.cfg.Alias) {
		return cu
	}
	if v.cfg.OnlyIfReferenced && !internal.ReferencedPackages(cu)[v.cfg.PackagePath] {
		return cu
	}
	imp := internal.NewImport(v.cfg.PackagePath, v.cfg.Alias)
	return internal.AddToBlock(cu, imp, internal.FindModulePath(cu))
}

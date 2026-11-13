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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type pruneModule struct {
	recipe.Base
	ModulePath string
}

func (r *pruneModule) Name() string        { return "org.openrewrite.golang.test.PruneModule" }
func (r *pruneModule) DisplayName() string { return "Prune go.sum lines for a module" }
func (r *pruneModule) Description() string {
	return "Removes all `go.sum` entries for the given module path."
}

func (r *pruneModule) Editor() recipe.TreeVisitor {
	return visitor.Init(&pruneModuleVisitor{modulePath: r.ModulePath})
}

type pruneModuleVisitor struct {
	visitor.GoVisitor
	modulePath string
}

func (v *pruneModuleVisitor) VisitGoSumLine(l *golang.GoSumLine, p any) java.Tree {
	if l.ModulePath == v.modulePath {
		return nil // delete the line
	}
	return v.GoVisitor.VisitGoSumLine(l, p)
}

type findModuleHash struct {
	recipe.Base
	ModulePath string
}

func (r *findModuleHash) Name() string        { return "org.openrewrite.golang.test.FindModuleHash" }
func (r *findModuleHash) DisplayName() string { return "Find go.sum hashes for a module" }
func (r *findModuleHash) Description() string {
	return "Marks each `go.sum` line for the given module with a search result."
}

func (r *findModuleHash) Editor() recipe.TreeVisitor {
	return visitor.Init(&findModuleHashVisitor{modulePath: r.ModulePath})
}

type findModuleHashVisitor struct {
	visitor.GoVisitor
	modulePath string
}

func (v *findModuleHashVisitor) VisitGoSumLine(l *golang.GoSumLine, p any) java.Tree {
	l = v.GoVisitor.VisitGoSumLine(l, p).(*golang.GoSumLine)
	if l.ModulePath == v.modulePath {
		l = l.WithMarkers(java.FoundSearchResult(l.Markers, "found"))
	}
	return l
}

func TestGoSumNoChange(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&pruneModule{ModulePath: "github.com/absent/mod"})
	spec.RewriteRun(t,
		test.GoSum(
			"github.com/a/b v1.0.0 h1:aaaa=\n"+
				"github.com/a/b v1.0.0/go.mod h1:bbbb=\n",
		),
	)
}

func TestGoSumPruneModule(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&pruneModule{ModulePath: "github.com/c/d"})
	spec.RewriteRun(t,
		test.GoSum(
			"github.com/a/b v1.0.0 h1:aaaa=\n"+
				"github.com/a/b v1.0.0/go.mod h1:bbbb=\n"+
				"github.com/c/d v2.0.0 h1:cccc=\n"+
				"github.com/c/d v2.0.0/go.mod h1:dddd=\n",
			"github.com/a/b v1.0.0 h1:aaaa=\n"+
				"github.com/a/b v1.0.0/go.mod h1:bbbb=\n",
		),
	)
}

func TestGoSumSearchRecipe(t *testing.T) {
	spec := test.NewRecipeSpec().WithRecipe(&findModuleHash{ModulePath: "github.com/a/b"})
	spec.RewriteRun(t,
		test.GoSum(
			"github.com/a/b v1.0.0 h1:aaaa=\n",
			"/*~~(found)~~>*/github.com/a/b v1.0.0 h1:aaaa=\n",
		),
	)
}

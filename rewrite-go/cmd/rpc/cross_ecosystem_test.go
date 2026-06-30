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

package main

import (
	"encoding/json"
	"reflect"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

// goDelegatingRecipe delegates entirely to a Java host recipe. The Go server can't run it, so
// PrepareRecipe answers with delegatesTo{recipeName, options} and the Java host resolves the
// recipe from its own marketplace instead of wrapping the Go recipe in an RpcRecipe.
type goDelegatingRecipe struct{ recipe.Base }

func (*goDelegatingRecipe) Name() string           { return "org.openrewrite.go.test.Delegating" }
func (*goDelegatingRecipe) DisplayName() string    { return "Delegating" }
func (*goDelegatingRecipe) Description() string     { return "Delegates to a Java recipe." }
func (*goDelegatingRecipe) JavaRecipeName() string { return "org.openrewrite.java.ChangeType" }
func (*goDelegatingRecipe) JavaOptions() map[string]any {
	return map[string]any{
		"oldFullyQualifiedTypeName": "a.A",
		"newFullyQualifiedTypeName": "b.B",
	}
}

// goCompositeWithJavaChild is a Go composite whose only child delegates to a Java recipe — the
// cross-ecosystem case the host's whole-tree path resolves from the prepared recipeList.
type goCompositeWithJavaChild struct{ recipe.Base }

func (*goCompositeWithJavaChild) Name() string        { return "org.openrewrite.go.test.CompositeWithJavaChild" }
func (*goCompositeWithJavaChild) DisplayName() string { return "Composite with Java child" }
func (*goCompositeWithJavaChild) Description() string { return "A composite whose child delegates to a Java recipe." }
func (*goCompositeWithJavaChild) RecipeList() []recipe.Recipe {
	return []recipe.Recipe{&goDelegatingRecipe{}}
}

func prepareOK(t *testing.T, s *server, id string) prepareRecipeResponse {
	t.Helper()
	params, err := json.Marshal(prepareRecipeRequest{ID: id})
	if err != nil {
		t.Fatalf("marshal prepare request: %v", err)
	}
	resp, rpcErr := s.handlePrepareRecipe(params)
	if rpcErr != nil {
		t.Fatalf("handlePrepareRecipe error: %+v", rpcErr)
	}
	return resp.(prepareRecipeResponse)
}

// A root recipe that delegates to a Java recipe answers with delegatesTo and no child tree, so the
// host loads the Java recipe locally rather than wrapping the Go recipe in an RpcRecipe.
func TestPrepareRecipeRootDelegatesToJavaRecipe(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goDelegatingRecipe{})

	pr := prepareOK(t, s, "org.openrewrite.go.test.Delegating")

	if pr.DelegatesTo == nil {
		t.Fatal("expected delegatesTo to be set, got nil")
	}
	if got := pr.DelegatesTo.RecipeName; got != "org.openrewrite.java.ChangeType" {
		t.Errorf("delegatesTo.recipeName = %q, want org.openrewrite.java.ChangeType", got)
	}
	want := map[string]any{
		"oldFullyQualifiedTypeName": "a.A",
		"newFullyQualifiedTypeName": "b.B",
	}
	if !reflect.DeepEqual(pr.DelegatesTo.Options, want) {
		t.Errorf("delegatesTo.options = %+v, want %+v", pr.DelegatesTo.Options, want)
	}
	if len(pr.RecipeList) != 0 {
		t.Errorf("a delegating recipe carries no child tree, got %d children", len(pr.RecipeList))
	}
}

// A composite's child that delegates to a Java recipe is emitted inside the prepared recipeList as
// a delegatesTo node (its descriptor named for the Java recipe), so the host resolves it
// cross-ecosystem while building the tree locally — no PrepareRecipe round trip and no Go-side
// recursion into the delegated child.
func TestPrepareRecipeCompositeChildDelegatesToJavaRecipe(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goCompositeWithJavaChild{})

	pr := prepareOK(t, s, "org.openrewrite.go.test.CompositeWithJavaChild")

	if pr.DelegatesTo != nil {
		t.Errorf("the composite itself should not delegate, got %+v", pr.DelegatesTo)
	}
	if len(pr.RecipeList) != 1 {
		t.Fatalf("expected 1 prepared child, got %d: %+v", len(pr.RecipeList), pr.RecipeList)
	}
	child := pr.RecipeList[0]
	if child.DelegatesTo == nil {
		t.Fatal("expected the child to carry delegatesTo, got nil")
	}
	if got := child.DelegatesTo.RecipeName; got != "org.openrewrite.java.ChangeType" {
		t.Errorf("child delegatesTo.recipeName = %q, want org.openrewrite.java.ChangeType", got)
	}
	if got := child.Descriptor.Name; got != "org.openrewrite.java.ChangeType" {
		t.Errorf("child descriptor name = %q, want the Java recipe name", got)
	}
}

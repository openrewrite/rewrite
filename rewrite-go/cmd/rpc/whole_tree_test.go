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
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

// goReqOptRecipe declares a required option with no value, so preparing it must fail validation.
type goReqOptRecipe struct{ recipe.Base }

func (*goReqOptRecipe) Name() string        { return "org.openrewrite.go.test.RequiresOpt" }
func (*goReqOptRecipe) DisplayName() string { return "Requires opt" }
func (*goReqOptRecipe) Description() string { return "A recipe with a required option." }
func (*goReqOptRecipe) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{recipe.Option("text", "Text", "Required text.")}
}

type goLeafRecipe struct{ recipe.Base }

func (*goLeafRecipe) Name() string        { return "org.openrewrite.go.test.Leaf" }
func (*goLeafRecipe) DisplayName() string { return "Leaf" }
func (*goLeafRecipe) Description() string { return "A leaf recipe." }

type goCompositeRecipe struct{ recipe.Base }

func (*goCompositeRecipe) Name() string                { return "org.openrewrite.go.test.Composite" }
func (*goCompositeRecipe) DisplayName() string         { return "Composite" }
func (*goCompositeRecipe) Description() string         { return "A composite recipe." }
func (*goCompositeRecipe) RecipeList() []recipe.Recipe { return []recipe.Recipe{&goLeafRecipe{}} }

type goCompositeInvalidChild struct{ recipe.Base }

func (*goCompositeInvalidChild) Name() string        { return "org.openrewrite.go.test.CompositeInvalid" }
func (*goCompositeInvalidChild) DisplayName() string { return "Composite invalid" }
func (*goCompositeInvalidChild) Description() string { return "A composite whose child lacks a required option." }
func (*goCompositeInvalidChild) RecipeList() []recipe.Recipe {
	return []recipe.Recipe{&goReqOptRecipe{}}
}

func prepareErr(t *testing.T, s *server, id string) *rpcError {
	t.Helper()
	params, err := json.Marshal(prepareRecipeRequest{ID: id})
	if err != nil {
		t.Fatalf("marshal prepare request: %v", err)
	}
	_, rpcErr := s.handlePrepareRecipe(params)
	return rpcErr
}

func TestPrepareRecipeRejectsMissingRequiredOption(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goReqOptRecipe{})

	rpcErr := prepareErr(t, s, "org.openrewrite.go.test.RequiresOpt")

	if rpcErr == nil {
		t.Fatal("expected a missing-required-option error, got success")
	}
	if !strings.Contains(rpcErr.Message, "Missing required option") || !strings.Contains(rpcErr.Message, "text") {
		t.Errorf("error %q does not report the missing `text` option", rpcErr.Message)
	}
}

// Validation recurses through the whole prepared tree (like the C#, JS, and Python servers).
func TestPrepareRecipeValidatesChildRequiredOptions(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goCompositeInvalidChild{})

	rpcErr := prepareErr(t, s, "org.openrewrite.go.test.CompositeInvalid")

	if rpcErr == nil {
		t.Fatal("expected a missing-required-option error for the child, got success")
	}
	if !strings.Contains(rpcErr.Message, "Missing required option") || !strings.Contains(rpcErr.Message, "text") {
		t.Errorf("error %q does not report the child's missing `text` option", rpcErr.Message)
	}
}

// PrepareRecipe returns the whole prepared tree (recipeList) so the host builds it locally.
func TestPrepareRecipeReturnsWholeChildTree(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goCompositeRecipe{})

	params, err := json.Marshal(prepareRecipeRequest{ID: "org.openrewrite.go.test.Composite"})
	if err != nil {
		t.Fatalf("marshal prepare request: %v", err)
	}
	resp, rpcErr := s.handlePrepareRecipe(params)
	if rpcErr != nil {
		t.Fatalf("handlePrepareRecipe error: %+v", rpcErr)
	}

	pr := resp.(prepareRecipeResponse)
	if len(pr.RecipeList) != 1 {
		t.Fatalf("expected 1 prepared child, got %d: %+v", len(pr.RecipeList), pr.RecipeList)
	}
	if got := pr.RecipeList[0].Descriptor.Name; got != "org.openrewrite.go.test.Leaf" {
		t.Errorf("expected child Leaf, got %q", got)
	}
}

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
	"strings"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

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
func (*goCompositeInvalidChild) Description() string {
	return "A composite whose child lacks a required option."
}
func (*goCompositeInvalidChild) RecipeList() []recipe.Recipe {
	return []recipe.Recipe{&goReqOptRecipe{}}
}

func prepareErr(t *testing.T, s *server, id string) *rpcError {
	t.Helper()
	params, err := json.Marshal(prepareRecipeRequest{ID: id})
	require.NoError(t, err)
	_, rpcErr := s.handlePrepareRecipe(params)
	return rpcErr
}

func TestPrepareRecipeRejectsMissingRequiredOption(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goReqOptRecipe{})

	rpcErr := prepareErr(t, s, "org.openrewrite.go.test.RequiresOpt")

	require.NotNil(t, rpcErr)
	assert.False(t, !strings.Contains(rpcErr.Message, "Missing required option") || !strings.Contains(rpcErr.Message, "text"))
}

// Validation recurses through the whole prepared tree (like the C#, JS, and Python servers).
func TestPrepareRecipeValidatesChildRequiredOptions(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goCompositeInvalidChild{})

	rpcErr := prepareErr(t, s, "org.openrewrite.go.test.CompositeInvalid")

	require.NotNil(t, rpcErr)
	assert.False(t, !strings.Contains(rpcErr.Message, "Missing required option") || !strings.Contains(rpcErr.Message, "text"))
}

func TestPrepareRecipeReturnsWholeChildTree(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goCompositeRecipe{})

	params, err := json.Marshal(prepareRecipeRequest{ID: "org.openrewrite.go.test.Composite"})
	require.NoError(t, err)
	resp, rpcErr := s.handlePrepareRecipe(params)
	require.Nil(t, rpcErr)

	pr := resp.(prepareRecipeResponse)
	require.Len(t, pr.RecipeList, 1)
	if got := pr.RecipeList[0].Descriptor.Name; got != "org.openrewrite.go.test.Leaf" {
		t.Errorf("expected child Leaf, got %q", got)
	}
}

// goSetsText carries a text option value; distinct instances hold distinct values.
type goSetsText struct {
	recipe.Base
	Text string
}

func (*goSetsText) Name() string        { return "org.openrewrite.go.test.SetsText" }
func (*goSetsText) DisplayName() string { return "Sets text" }
func (*goSetsText) Description() string { return "A recipe with a text option." }
func (r *goSetsText) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{recipe.Option("text", "Text", "Text value.").WithValue(r.Text)}
}

type goCompositeSameType struct{ recipe.Base }

func (*goCompositeSameType) Name() string        { return "org.openrewrite.go.test.CompositeSameType" }
func (*goCompositeSameType) DisplayName() string { return "Composite with same-type children" }
func (*goCompositeSameType) Description() string {
	return "A composite with distinct-option children of one type."
}
func (*goCompositeSameType) RecipeList() []recipe.Recipe {
	return []recipe.Recipe{
		&goSetsText{Text: "a"},
		&goSetsText{Text: "b"},
		&goSetsText{Text: "c"},
	}
}

// A composite whose RecipeList() yields multiple instances of the same recipe class with different
// option values must keep each prepared child's own id and options, rather than collapsing them.
func TestPrepareRecipeSameTypeChildrenPreserveDistinctOptions(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&goCompositeSameType{})

	params, err := json.Marshal(prepareRecipeRequest{ID: "org.openrewrite.go.test.CompositeSameType"})
	require.NoError(t, err)
	resp, rpcErr := s.handlePrepareRecipe(params)
	require.Nil(t, rpcErr)

	pr := resp.(prepareRecipeResponse)
	require.Len(t, pr.RecipeList, 3)

	ids := map[string]bool{}
	var texts []any
	for _, child := range pr.RecipeList {
		if got := child.Descriptor.Name; got != "org.openrewrite.go.test.SetsText" {
			t.Errorf("expected child SetsText, got %q", got)
		}
		ids[child.ID] = true
		for _, opt := range child.Descriptor.Options {
			if opt.Name == "text" {
				texts = append(texts, opt.Value)
			}
		}
	}

	// Each child is prepared as its own instance (distinct id)...
	assert.Len(t, ids, 3)
	// ...retaining its own distinct option value, in the order the composite declared them.
	want := []any{"a", "b", "c"}
	assert.True(t, reflect.DeepEqual(texts, want))
}

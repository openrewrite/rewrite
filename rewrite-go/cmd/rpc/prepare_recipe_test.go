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

func prepareRecipe(t *testing.T, s *server, id string) string {
	t.Helper()
	params, err := json.Marshal(prepareRecipeRequest{ID: id})
	if err != nil {
		t.Fatalf("marshal prepare request: %v", err)
	}
	resp, rpcErr := s.handlePrepareRecipe(params)
	if rpcErr != nil {
		t.Fatalf("handlePrepareRecipe returned error: %+v", rpcErr)
	}
	return resp.(prepareRecipeResponse).ID
}

func visit(t *testing.T, s *server, visitor string) (any, *rpcError) {
	t.Helper()
	params, err := json.Marshal(visitRequest{Visitor: visitor, TreeID: "tree-1", SourceFileType: "Go"})
	if err != nil {
		t.Fatalf("marshal visit request: %v", err)
	}
	return s.handleVisit(params)
}

// A recipe registered descriptor-only (nil instance) — as the installer does
// for marketplace listing — must NOT silently report "no changes" when an
// edit/scan run is dispatched against it. Doing so masks a stale or missing
// workspace binary. The Visit handler must instead return an rpcError naming
// the recipe.
func TestVisitMetadataOnlyRecipeFailsLoudly(t *testing.T) {
	// given
	s, _ := newTestServer(t)
	const recipeName = "org.openrewrite.go.example.MetadataOnly"
	s.registry.RegisterDescriptor(recipe.RecipeDescriptor{
		Name:        recipeName,
		DisplayName: "Metadata only",
		Description: "Registered without an executable implementation.",
	})
	recipeID := prepareRecipe(t, s, recipeName)

	// when
	for _, phase := range []string{"edit", "scan"} {
		resp, rpcErr := visit(t, s, phase+":"+recipeID)

		// then
		if rpcErr == nil {
			t.Fatalf("%s: expected an rpcError, got success response %+v", phase, resp)
		}
		if !strings.Contains(rpcErr.Message, recipeName) {
			t.Errorf("%s: error message %q does not name the recipe %q", phase, rpcErr.Message, recipeName)
		}
		if !strings.Contains(rpcErr.Message, "stale or missing") {
			t.Errorf("%s: error message %q does not explain the stale/missing binary cause", phase, rpcErr.Message)
		}
	}
}

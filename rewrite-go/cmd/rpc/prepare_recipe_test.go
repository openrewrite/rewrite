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

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

func prepareRecipe(t *testing.T, s *server, id string) string {
	t.Helper()
	params, err := json.Marshal(prepareRecipeRequest{ID: id})
	require.NoError(t, err, "marshal prepare request")
	resp, rpcErr := s.handlePrepareRecipe(params)
	require.Nil(t, rpcErr, "handlePrepareRecipe returned error")
	return resp.(prepareRecipeResponse).ID
}

func visit(t *testing.T, s *server, visitor string) (any, *rpcError) {
	t.Helper()
	params, err := json.Marshal(visitRequest{Visitor: visitor, TreeID: "tree-1", SourceFileType: "Go"})
	require.NoError(t, err, "marshal visit request")
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
		require.NotNilf(t, rpcErr, "%s: expected an rpcError, got success response %+v", phase, resp)
		assert.Truef(t, strings.Contains(rpcErr.Message, recipeName), "%s: error message %q does not name the recipe", phase, rpcErr.Message)
		assert.Truef(t, strings.Contains(rpcErr.Message, "stale or missing"), "%s: error message %q does not explain the stale/missing binary cause", phase, rpcErr.Message)
	}
}

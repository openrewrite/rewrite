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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

type optionBindingRecipe struct {
	recipe.Base
	Threshold int64
	Enabled   bool
}

func (r *optionBindingRecipe) Name() string        { return "org.openrewrite.go.example.OptionBinding" }
func (r *optionBindingRecipe) DisplayName() string { return "Option binding" }
func (r *optionBindingRecipe) Description() string { return "Binds options sent over the wire." }

func prepareWithRawOptions(t *testing.T, s *server, rawOptions string) (*optionBindingRecipe, *rpcError) {
	t.Helper()
	params := json.RawMessage(`{"id":"org.openrewrite.go.example.OptionBinding","options":` + rawOptions + `}`)
	resp, rpcErr := s.handlePrepareRecipe(params)
	if rpcErr != nil {
		return nil, rpcErr
	}
	prepared := s.preparedRecipes[resp.(prepareRecipeResponse).ID]
	bound, ok := prepared.(*optionBindingRecipe)
	require.True(t, ok, "expected an *optionBindingRecipe")
	return bound, nil
}

func TestPrepareRecipeBindsOptionsFromWireJSON(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&optionBindingRecipe{}, recipe.CategoryDescriptor{DisplayName: "Go"})

	// The CLI's -P delivers every option as a JSON string, whatever its type.
	bound, rpcErr := prepareWithRawOptions(t, s, `{"threshold":"12","enabled":"false"}`)
	require.Nil(t, rpcErr)
	assert.Equal(t, int64(12), bound.Threshold)
	assert.False(t, bound.Enabled)

	// A bare JSON integer past 2^53 has no exact float64 form.
	bound, rpcErr = prepareWithRawOptions(t, s, `{"threshold":9007199254740993}`)
	require.Nil(t, rpcErr)
	assert.Equal(t, int64(9007199254740993), bound.Threshold)
}

func TestPrepareRecipeRejectsUnbindableOption(t *testing.T) {
	s, _ := newTestServer(t)
	s.registry.Register(&optionBindingRecipe{}, recipe.CategoryDescriptor{DisplayName: "Go"})

	_, rpcErr := prepareWithRawOptions(t, s, `{"enabled":"yes"}`)
	require.NotNil(t, rpcErr)
	assert.Equal(t, -32602, rpcErr.Code)
	assert.Contains(t, rpcErr.Message, "org.openrewrite.go.example.OptionBinding")
	assert.Contains(t, rpcErr.Message, "enabled")
}

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

type goCausesAnotherCycleRecipe struct{ recipe.Base }

func (*goCausesAnotherCycleRecipe) Name() string             { return "org.openrewrite.go.test.CausesAnotherCycle" }
func (*goCausesAnotherCycleRecipe) DisplayName() string      { return "Causes another cycle" }
func (*goCausesAnotherCycleRecipe) Description() string      { return "Causes another cycle." }
func (*goCausesAnotherCycleRecipe) CausesAnotherCycle() bool { return true }

// prepareWire returns the PrepareRecipe response as it appears on the wire.
func prepareWire(t *testing.T, req prepareRecipeRequest) map[string]any {
	t.Helper()
	s, _ := newTestServer(t)
	s.registry.Register(&goCausesAnotherCycleRecipe{})
	s.registry.Register(&goLeafRecipe{})
	params, err := json.Marshal(req)
	require.NoError(t, err)
	resp, rpcErr := s.handlePrepareRecipe(params)
	require.Nil(t, rpcErr)
	out, err := json.Marshal(resp)
	require.NoError(t, err)
	var wire map[string]any
	require.NoError(t, json.Unmarshal(out, &wire))
	return wire
}

// Older Java hosts reject unknown response fields, so the field must only appear when asked for.
func TestCausesAnotherCycleOmittedForHostsThatDidNotAsk(t *testing.T) {
	wire := prepareWire(t, prepareRecipeRequest{ID: "org.openrewrite.go.test.CausesAnotherCycle"})
	assert.NotContains(t, wire, "causesAnotherCycle")
}

func TestCausesAnotherCycleOmittedWhenFalse(t *testing.T) {
	wire := prepareWire(t, prepareRecipeRequest{ID: "org.openrewrite.go.test.Leaf", AcceptsCausesAnotherCycle: true})
	assert.NotContains(t, wire, "causesAnotherCycle")
}

func TestCausesAnotherCycleSentWhenRequested(t *testing.T) {
	wire := prepareWire(t, prepareRecipeRequest{ID: "org.openrewrite.go.test.CausesAnotherCycle", AcceptsCausesAnotherCycle: true})
	assert.Equal(t, true, wire["causesAnotherCycle"])
}

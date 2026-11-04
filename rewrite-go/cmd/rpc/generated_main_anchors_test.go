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
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// Moderne CLI generates a custom RPC main by splicing recipe module imports
// and activations after these exact lines of main.go.
func TestMainKeepsAnchorsForGeneratedMain(t *testing.T) {
	// given
	src, err := os.ReadFile("main.go")
	require.NoError(t, err)

	// when
	text := string(src)

	// then
	assert.Contains(t, text, "\"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc\"\n")
	assert.Contains(t, text, "reg.Activate(recipes.Activate)\n")
}

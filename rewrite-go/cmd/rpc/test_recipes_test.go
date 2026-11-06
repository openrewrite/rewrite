//go:build !rpctest

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
	"path/filepath"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRegistryHasNoTestRecipes(t *testing.T) {
	// given
	s := newServer(serverConfig{logFile: filepath.Join(t.TempDir(), "server.log")})

	// when
	descriptors := s.registry.AllRecipes()

	// then
	assert.NotEmpty(t, descriptors)
	for _, d := range descriptors {
		assert.Falsef(t, strings.HasPrefix(d.Name, "org.openrewrite.golang.test."), "test recipe %s is registered", d.Name)
	}
}

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

	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

type goGenerateRecipe struct {
	recipe.ScanningBase
	generated []java.Tree
}

func (*goGenerateRecipe) Name() string                                         { return "org.openrewrite.go.test.Generate" }
func (*goGenerateRecipe) DisplayName() string                                  { return "Generate" }
func (*goGenerateRecipe) Description() string                                  { return "Generates source files." }
func (*goGenerateRecipe) InitialValue(*recipe.ExecutionContext) any            { return nil }
func (r *goGenerateRecipe) Generate(any, *recipe.ExecutionContext) []java.Tree { return r.generated }

func TestGenerateReportsTheSourceFileTypeOfEveryGeneratedTree(t *testing.T) {
	// given
	cu, err := goparser.NewGoParser().Parse("main.go", "package main\n")
	require.NoError(t, err)
	mod, err := goparser.ParseGoModFile("go.mod", "module example.com/m\n\ngo 1.22\n")
	require.NoError(t, err)
	sum, err := goparser.ParseGoSumFile("go.sum", "golang.org/x/text v0.14.0 h1:ScX5w1eTa3QqT8oi6+ziP7dTV1S2+ALU0bI+0zXKWiQ=\n")
	require.NoError(t, err)
	s, _ := newTestServer(t)
	s.preparedRecipes["generate"] = &goGenerateRecipe{generated: []java.Tree{cu, mod, sum}}
	params, err := json.Marshal(generateRequest{ID: "generate"})
	require.NoError(t, err)

	// when
	result, rpcErr := s.handleGenerate(params)

	// then
	require.Nil(t, rpcErr)
	resp := result.(*generateResponse)
	assert.Equal(t, []string{
		"org.openrewrite.golang.tree.Go$CompilationUnit",
		"org.openrewrite.golang.tree.GoMod",
		"org.openrewrite.golang.tree.GoSum",
	}, resp.SourceFileTypes)
}

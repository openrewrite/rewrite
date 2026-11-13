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

	"github.com/openrewrite/rewrite/rewrite-go/pkg/preconditions"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

func prepareRecipe(t *testing.T, s *server, id string) string {
	t.Helper()
	return prepareRecipeResponseFor(t, s, id).ID
}

func prepareRecipeResponseFor(t *testing.T, s *server, id string) prepareRecipeResponse {
	t.Helper()
	params, err := json.Marshal(prepareRecipeRequest{ID: id})
	require.NoError(t, err, "marshal prepare request")
	resp, rpcErr := s.handlePrepareRecipe(params)
	require.Nil(t, rpcErr, "handlePrepareRecipe returned error")
	return resp.(prepareRecipeResponse)
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

type goEditor struct{ visitor.GoVisitor }

type javaFind struct{ recipe.Base }

func (*javaFind) Name() string                { return "org.openrewrite.go.test.JavaFind" }
func (*javaFind) DisplayName() string         { return "Java find" }
func (*javaFind) Description() string         { return "Delegates to a Java recipe." }
func (*javaFind) JavaRecipeName() string      { return "org.openrewrite.text.Find" }
func (*javaFind) JavaOptions() map[string]any { return map[string]any{"find": "TODO"} }

const editGatedOnJavaName = "org.openrewrite.go.test.EditGatedOnJava"

type editGatedOnJava struct{ recipe.Base }

func (*editGatedOnJava) Name() string        { return editGatedOnJavaName }
func (*editGatedOnJava) DisplayName() string { return "Edit gated on Java" }
func (*editGatedOnJava) Description() string { return "Edits files a Java search matches." }
func (*editGatedOnJava) Editor() recipe.TreeVisitor {
	return preconditions.Check(preconditions.Or(preconditions.UsesType("fmt.Stringer"), &javaFind{}),
		visitor.Init(&goEditor{}))
}

const scanGatedOnJavaName = "org.openrewrite.go.test.ScanGatedOnJava"

type scanGatedOnJava struct{ recipe.ScanningBase }

func (*scanGatedOnJava) Name() string                              { return scanGatedOnJavaName }
func (*scanGatedOnJava) DisplayName() string                       { return "Scan gated on Java" }
func (*scanGatedOnJava) Description() string                       { return "Scans files a Java search matches." }
func (*scanGatedOnJava) InitialValue(*recipe.ExecutionContext) any { return struct{}{} }
func (*scanGatedOnJava) Scanner(any) recipe.TreeVisitor {
	return preconditions.Check(preconditions.UsesType("fmt.Stringer"), visitor.Init(&goEditor{}))
}

const hasStringer = `{"visitorName": "org.openrewrite.java.search.HasType",
	"visitorOptions": {"fullyQualifiedTypeName": "fmt.Stringer", "checkAssignability": false}}`

const goLanguageGateWire = `{"op": "or", "operands": [
	{"visitorName": "org.openrewrite.rpc.internal.FindTreesOfType", "visitorOptions": {"type": "org.openrewrite.golang.tree.Go"}},
	{"visitorName": "org.openrewrite.rpc.internal.FindTreesOfType", "visitorOptions": {"type": "org.openrewrite.golang.tree.GoMod"}},
	{"visitorName": "org.openrewrite.rpc.internal.FindTreesOfType", "visitorOptions": {"type": "org.openrewrite.golang.tree.GoSum"}}]}`

func requireWire(t *testing.T, expected string, preconditions []any) {
	t.Helper()
	actual, err := json.Marshal(preconditions)
	require.NoError(t, err)
	require.JSONEq(t, expected, string(actual))
}

func TestPrepareRecipeHoistsPreconditionsWithAJavaImplementation(t *testing.T) {
	// given
	s, _ := newTestServer(t)
	s.registry.Register(&editGatedOnJava{})
	s.registry.Register(&scanGatedOnJava{})

	// when
	edit := prepareRecipeResponseFor(t, s, editGatedOnJavaName)
	scan := prepareRecipeResponseFor(t, s, scanGatedOnJavaName)

	// then
	requireWire(t, `[{"op": "or", "operands": [`+hasStringer+`,
		{"visitorName": "org.openrewrite.text.Find", "visitorOptions": {"find": "TODO"}}]}, `+goLanguageGateWire+`]`,
		edit.EditPreconditions)
	requireWire(t, `[`+hasStringer+`, `+goLanguageGateWire+`]`, scan.ScanPreconditions)
}

const scannerNeedingARunName = "org.openrewrite.go.test.ScannerNeedingARun"

type scannerNeedingARun struct{ recipe.ScanningBase }

func (*scannerNeedingARun) Name() string                              { return scannerNeedingARunName }
func (*scannerNeedingARun) DisplayName() string                       { return "Scanner needing a run" }
func (*scannerNeedingARun) Description() string                       { return "Builds its scanner only during a run." }
func (*scannerNeedingARun) InitialValue(*recipe.ExecutionContext) any { return nil }
func (*scannerNeedingARun) Scanner(acc any) recipe.TreeVisitor {
	return preconditions.Check(preconditions.UsesType("fmt.Stringer"), acc.(recipe.TreeVisitor))
}

func TestPrepareRecipeKeepsThePreconditionOfAScannerThatNeedsARun(t *testing.T) {
	// given
	s, _ := newTestServer(t)
	s.registry.Register(&scannerNeedingARun{})

	// when
	resp := prepareRecipeResponseFor(t, s, scannerNeedingARunName)

	// then
	require.Equal(t, "scan:"+resp.ID, *resp.ScanVisitor)
	requireWire(t, `[]`, resp.ScanPreconditions)
}

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
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"testing"

	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/template"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

const sourceImportsChildName = "org.openrewrite.go.test.PreferSlicesSort$Strings"

// sourceImportsChild mirrors the shape every `template.SourceImports` recipe in
// recipes-go has: the after template's import edit is composed onto the source
// file through GoVisitor.DoAfterVisit rather than returned from Visit.
var sourceImportsChild = func() recipe.Recipe {
	s := template.Expr("s")
	return template.NewRecipe(
		template.RecipeName(sourceImportsChildName),
		template.WithDisplayName("sort.Strings -> slices.Sort"),
		template.WithDescription("Replaces `sort.Strings` with `slices.Sort`."),
		template.WithBefore(fmt.Sprintf(`sort.Strings(%s)`, s), template.Imports("sort")),
		template.WithAfter(fmt.Sprintf(`slices.Sort(%s)`, s), template.Imports("slices"), template.SourceImports("slices")),
		template.WithCaptures(s),
	)
}()

// sourceImportsComposite is the declaring recipe, the only form under which a
// configured template recipe reaches the registry: Register derives a
// constructor by reflection, which yields a zero-valued instance, so only a
// composite's children are registered as the instances they were built as.
type sourceImportsComposite struct{ recipe.Base }

func (*sourceImportsComposite) Name() string { return "org.openrewrite.go.test.PreferSlicesSort" }
func (*sourceImportsComposite) DisplayName() string {
	return "Prefer slices.Sort over sort type helpers"
}
func (*sourceImportsComposite) Description() string {
	return "Replaces `sort.Strings` with `slices.Sort`."
}
func (*sourceImportsComposite) RecipeList() []recipe.Recipe {
	return []recipe.Recipe{sourceImportsChild}
}

const afterVisitsBefore = `package slicesort

import (
	"sort"
)

// SortKeys sorts the given keys.
func SortKeys(keys []string) []string {
	sort.Strings(keys)
	return keys
}
`

const afterVisitsAfter = `package slicesort

import (
	"slices"
)

// SortKeys sorts the given keys.
func SortKeys(keys []string) []string {
	slices.Sort(keys)
	return keys
}
`

// seedTreeForVisit stages `src` as the tree the host is about to dispatch a
// visit against: the parsed LST becomes Go's reverse-direction baseline and the
// host's scripted reply is a NO_CHANGE, so getObjectFromJava hands it back.
func seedTreeForVisit(t *testing.T, s *server, treeID, src string) {
	t.Helper()
	cu, err := goparser.NewGoParser().Parse("sort.go", src)
	if err != nil {
		t.Fatalf("parse source: %v", err)
	}
	s.reverseRemoteObjects[treeID] = cu
	s.reader = bufio.NewReader(bytes.NewReader(frameReverseGetObjectReply(t, []map[string]any{
		{"state": "NO_CHANGE"},
		{"state": "END_OF_OBJECT"},
	})))
	s.writer = &bytes.Buffer{}
}

func printVisited(t *testing.T, s *server, treeID string) string {
	t.Helper()
	tree, ok := s.localObjects[treeID].(java.Tree)
	if !ok {
		t.Fatalf("localObjects[%q] = %#v, want a tree", treeID, s.localObjects[treeID])
	}
	return printer.Print(tree)
}

// The host dispatches through Visit for a lone recipe and BatchVisit for a
// composite, so a drain in only one of them would make a recipe's import edits
// depend on the shape of the run.
func TestVisitPathsDrainAfterVisits(t *testing.T) {
	for _, tc := range []struct {
		name     string
		dispatch func(t *testing.T, s *server, visitorName, treeID string)
	}{
		{
			name: "Visit",
			dispatch: func(t *testing.T, s *server, visitorName, treeID string) {
				params, err := json.Marshal(visitRequest{Visitor: visitorName, TreeID: treeID, SourceFileType: "Go"})
				if err != nil {
					t.Fatalf("marshal visit request: %v", err)
				}
				if _, rpcErr := s.handleVisit(params); rpcErr != nil {
					t.Fatalf("handleVisit returned error: %+v", rpcErr)
				}
			},
		},
		{
			name: "BatchVisit",
			dispatch: func(t *testing.T, s *server, visitorName, treeID string) {
				params, err := json.Marshal(batchVisitRequest{
					TreeID:         treeID,
					SourceFileType: "Go",
					Visitors:       []batchVisitItem{{Visitor: visitorName}},
				})
				if err != nil {
					t.Fatalf("marshal batch visit request: %v", err)
				}
				if _, rpcErr := s.handleBatchVisit(params); rpcErr != nil {
					t.Fatalf("handleBatchVisit returned error: %+v", rpcErr)
				}
			},
		},
	} {
		t.Run(tc.name, func(t *testing.T) {
			s, _ := newTestServer(t)
			s.registry.Register(&sourceImportsComposite{})
			recipeID := prepareRecipe(t, s, sourceImportsChildName)

			const treeID = "tree-1"
			seedTreeForVisit(t, s, treeID, afterVisitsBefore)

			tc.dispatch(t, s, "edit:"+recipeID, treeID)

			if got := printVisited(t, s, treeID); got != afterVisitsAfter {
				t.Errorf("visited source:\ngot:\n%s\nwant:\n%s", got, afterVisitsAfter)
			}
		})
	}
}

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

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/preconditions"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

const scannerReturnsNilName = "org.openrewrite.go.test.ScannerReturnsNil"
const scannerReturnsChangedTreeName = "org.openrewrite.go.test.ScannerReturnsChangedTree"

type scannerFunc func(t java.Tree) java.Tree

func (f scannerFunc) Visit(t java.Tree, _ any) java.Tree { return f(t) }

type scannerReturnsNil struct{ recipe.ScanningBase }

func (*scannerReturnsNil) Name() string                              { return scannerReturnsNilName }
func (*scannerReturnsNil) DisplayName() string                       { return "Scanner returning nil" }
func (*scannerReturnsNil) Description() string                       { return "Scanner returning nil." }
func (*scannerReturnsNil) InitialValue(*recipe.ExecutionContext) any { return struct{}{} }
func (*scannerReturnsNil) Scanner(any) recipe.TreeVisitor {
	return scannerFunc(func(java.Tree) java.Tree { return nil })
}

type scannerReturnsChangedTree struct{ recipe.ScanningBase }

func (*scannerReturnsChangedTree) Name() string        { return scannerReturnsChangedTreeName }
func (*scannerReturnsChangedTree) DisplayName() string { return "Scanner returning a changed tree" }
func (*scannerReturnsChangedTree) Description() string { return "Scanner returning a changed tree." }
func (*scannerReturnsChangedTree) InitialValue(*recipe.ExecutionContext) any {
	return struct{}{}
}
func (*scannerReturnsChangedTree) Scanner(any) recipe.TreeVisitor {
	return scannerFunc(func(t java.Tree) java.Tree {
		cu := *t.(*golang.CompilationUnit)
		cu.SourcePath = "changed.go"
		return &cu
	})
}

type visitResult struct {
	modified bool
	deleted  bool
}

func dispatchVisit(t *testing.T, s *server, visitorName, treeID string) []visitResult {
	t.Helper()
	params, err := json.Marshal(visitRequest{Visitor: visitorName, TreeID: treeID, SourceFileType: "Go"})
	require.NoError(t, err)
	resp, rpcErr := s.handleVisit(params)
	require.Nil(t, rpcErr)
	return []visitResult{{modified: resp.(*visitResponse).Modified}}
}

func dispatchBatchVisit(t *testing.T, s *server, visitorName, treeID string) []visitResult {
	t.Helper()
	params, err := json.Marshal(batchVisitRequest{
		TreeID:         treeID,
		SourceFileType: "Go",
		Visitors:       []batchVisitItem{{Visitor: visitorName}, {Visitor: visitorName}},
	})
	require.NoError(t, err)
	resp, rpcErr := s.handleBatchVisit(params)
	require.Nil(t, rpcErr)
	var results []visitResult
	for _, r := range resp.(*batchVisitResponse).Results {
		results = append(results, visitResult{modified: r.Modified, deleted: r.Deleted})
	}
	return results
}

// As on the Java and JS peers, whatever a scanner returns is ignored on both
// visit paths: the scan phase neither changes nor deletes a source file.
func TestScannerResultIsIgnored(t *testing.T) {
	for _, dispatch := range []struct {
		name string
		run  func(t *testing.T, s *server, visitorName, treeID string) []visitResult
		want int
	}{
		{name: "Visit", run: dispatchVisit, want: 1},
		{name: "BatchVisit", run: dispatchBatchVisit, want: 2},
	} {
		for _, r := range []recipe.Recipe{&scannerReturnsNil{}, &scannerReturnsChangedTree{}} {
			t.Run(dispatch.name+"/"+r.Name(), func(t *testing.T) {
				// given
				s, _ := newTestServer(t)
				s.registry.Register(r)
				recipeID := prepareRecipe(t, s, r.Name())
				const treeID = "tree-1"
				seedTreeForVisit(t, s, treeID, afterVisitsBefore)

				// when
				results := dispatch.run(t, s, "scan:"+recipeID, treeID)

				// then
				require.Equal(t, make([]visitResult, dispatch.want), results)
				require.Equal(t, "sort.go", s.localObjects[treeID].(*golang.CompilationUnit).SourcePath)
			})
		}
	}
}

var visitPaths = []struct {
	name string
	run  func(t *testing.T, s *server, visitorName, treeID string) []visitResult
}{
	{name: "Visit", run: dispatchVisit},
	{name: "BatchVisit", run: dispatchBatchVisit},
}

var renameFile = scannerFunc(func(t java.Tree) java.Tree {
	cu := *t.(*golang.CompilationUnit)
	cu.SourcePath = "renamed.go"
	return &cu
})

const gatedRenameName = "org.openrewrite.go.test.GatedRename"

type gatedRename struct{ recipe.Base }

func (*gatedRename) Name() string        { return gatedRenameName }
func (*gatedRename) DisplayName() string { return "Gated rename" }
func (*gatedRename) Description() string { return "Renames the file unless its path matches nothing." }
func (*gatedRename) Editor() recipe.TreeVisitor {
	return preconditions.Check(preconditions.HasSourcePath("**/never-matches.go"), renameFile)
}

const gatedScanName = "org.openrewrite.go.test.GatedScan"

type gatedScan struct{ recipe.ScanningBase }

func (*gatedScan) Name() string        { return gatedScanName }
func (*gatedScan) DisplayName() string { return "Gated scan" }
func (*gatedScan) Description() string { return "Records the file unless its path matches nothing." }
func (*gatedScan) InitialValue(*recipe.ExecutionContext) any {
	return &[]string{}
}
func (*gatedScan) Scanner(acc any) recipe.TreeVisitor {
	return preconditions.Check(preconditions.HasSourcePath("**/never-matches.go"),
		scannerFunc(func(t java.Tree) java.Tree {
			scanned := acc.(*[]string)
			*scanned = append(*scanned, t.(*golang.CompilationUnit).SourcePath)
			return t
		}))
}

func TestHoistedPreconditionIsNotReevaluated(t *testing.T) {
	for _, path := range visitPaths {
		t.Run(path.name+"/edit", func(t *testing.T) {
			// given
			s, _ := newTestServer(t)
			s.registry.Register(&gatedRename{})
			recipeID := prepareRecipe(t, s, gatedRenameName)
			const treeID = "tree-1"
			seedTreeForVisit(t, s, treeID, afterVisitsBefore)

			// when
			results := path.run(t, s, "edit:"+recipeID, treeID)

			// then
			require.True(t, results[0].modified)
			require.Equal(t, "renamed.go", s.localObjects[treeID].(*golang.CompilationUnit).SourcePath)
		})
		t.Run(path.name+"/scan", func(t *testing.T) {
			// given
			s, _ := newTestServer(t)
			s.registry.Register(&gatedScan{})
			recipeID := prepareRecipe(t, s, gatedScanName)
			const treeID = "tree-1"
			seedTreeForVisit(t, s, treeID, afterVisitsBefore)

			// when
			path.run(t, s, "scan:"+recipeID, treeID)

			// then
			require.Contains(t, *s.preparedAccumulators[recipeID].(*[]string), "sort.go")
		})
	}
}

const peerGatedRenameName = "org.openrewrite.go.test.PeerGatedRename"

type peerGatedRename struct{ recipe.Base }

func (*peerGatedRename) Name() string        { return peerGatedRenameName }
func (*peerGatedRename) DisplayName() string { return "Peer-gated rename" }
func (*peerGatedRename) Description() string { return "Renames the file if a Go visitor matches it." }
func (*peerGatedRename) Editor() recipe.TreeVisitor {
	return preconditions.Check(scannerFunc(func(t java.Tree) java.Tree { return t }), visitor.Init(&goRename{}))
}

type goRename struct{ visitor.GoVisitor }

func (*goRename) VisitCompilationUnit(cu *golang.CompilationUnit, _ any) java.J {
	return renameFile(cu).(*golang.CompilationUnit)
}

func TestPeerOnlyPreconditionStillGatesTheVisit(t *testing.T) {
	for _, path := range visitPaths {
		t.Run(path.name, func(t *testing.T) {
			// given
			s, _ := newTestServer(t)
			s.registry.Register(&peerGatedRename{})
			prepared := prepareRecipeResponseFor(t, s, peerGatedRenameName)
			const treeID = "tree-1"
			seedTreeForVisit(t, s, treeID, afterVisitsBefore)

			// when
			results := path.run(t, s, prepared.EditVisitor, treeID)

			// then
			requireWire(t, `[`+goLanguageGateWire+`]`, prepared.EditPreconditions)
			require.False(t, results[0].modified)
			require.Equal(t, "sort.go", s.localObjects[treeID].(*golang.CompilationUnit).SourcePath)
		})
	}
}

/*
 * Copyright 2025 the original author or authors.
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

package test

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// markBinaryVisitor attaches a SearchResult marker to every binary expression.
type markBinaryVisitor struct {
	visitor.GoVisitor
	marker tree.SearchResult
}

func (v *markBinaryVisitor) VisitBinary(bin *tree.Binary, p any) tree.J {
	bin = v.GoVisitor.VisitBinary(bin, p).(*tree.Binary)
	bin = bin.WithMarkers(tree.AddMarker(bin.Markers, v.marker))
	return bin
}

func TestCollectSearchResultIDsEmpty(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\n")
	if err != nil {
		t.Fatal(err)
	}
	if got := tree.CollectSearchResultIDs(cu); len(got) != 0 {
		t.Fatalf("expected no search results, got %v", got)
	}
}

func TestCollectSearchResultIDsAfterMark(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\n\nvar x = 1 + 2\n")
	if err != nil {
		t.Fatal(err)
	}
	mark := tree.NewSearchResult("found a binary expr")
	v := &markBinaryVisitor{marker: mark}
	visitor.Init(v)

	result := v.Visit(cu, recipe.NewExecutionContext()).(tree.Tree)
	ids := tree.CollectSearchResultIDs(result)
	if len(ids) != 1 {
		t.Fatalf("expected exactly one search result id, got %d (%v)", len(ids), ids)
	}
	if ids[0] != mark.Ident {
		t.Fatalf("collected id %v does not match marker id %v", ids[0], mark.Ident)
	}
}

func TestCollectSearchResultIDsDedupes(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("a.go", "package main\n\nvar x = 1 + 2 + 3\n")
	if err != nil {
		t.Fatal(err)
	}
	// Same marker (same UUID) attached to two binary expressions: collector
	// should only return it once.
	mark := tree.NewSearchResult("dup")
	v := &markBinaryVisitor{marker: mark}
	visitor.Init(v)

	result := v.Visit(cu, recipe.NewExecutionContext()).(tree.Tree)
	ids := tree.CollectSearchResultIDs(result)
	if len(ids) != 1 {
		t.Fatalf("expected dedup to produce 1 id, got %d (%v)", len(ids), ids)
	}
}

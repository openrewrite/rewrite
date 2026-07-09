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

package visitor

import (
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// Walk performs a read-only, pre-order traversal of the LST rooted at t,
// invoking visit on every node (t included). Returning false from visit
// stops the walk immediately.
//
// Descent is driven by GoVisitor's type-switch dispatch, so Walk stays in
// lockstep with the RPC visitor rather than modelling the tree shape a
// second time. Callers therefore need not enumerate concrete node shapes.
func Walk(t java.Tree, visit func(java.Tree) bool) {
	if t == nil {
		return
	}
	w := &walker{visit: visit}
	w.GoVisitor.Self = w
	defer func() {
		if r := recover(); r != nil {
			if _, stop := r.(walkStop); !stop {
				panic(r)
			}
		}
	}()
	w.Visit(t, nil)
}

// walkStop unwinds the visitor stack when visit requests an early stop.
// GoVisitor has no first-class short-circuit, and PreVisit returning nil
// prunes only the current subtree (leaving siblings to run), so a sentinel
// panic is the only way to abandon the rest of the traversal.
type walkStop struct{}

type walker struct {
	GoVisitor
	visit func(java.Tree) bool
}

func (w *walker) PreVisit(t java.Tree, p any) java.Tree {
	if !w.visit(t) {
		panic(walkStop{})
	}
	return t
}

// CollectSearchResultIDs walks t and returns the IDs of every SearchResult
// and SearchResultMarker found on any node's Markers. The returned slice
// has stable first-seen order; duplicates are dropped.
//
// It rides the VisitMarker dispatch seam, matching the JS and C# batch-visit
// collectors, so every marker on every node (J and non-J alike) is reached
// through the shared visitor rather than a bespoke traversal.
func CollectSearchResultIDs(t java.Tree) []uuid.UUID {
	if t == nil {
		return nil
	}
	c := &searchCollector{seen: make(map[uuid.UUID]struct{})}
	c.GoVisitor.Self = c
	c.Visit(t, nil)
	return c.ids
}

type searchCollector struct {
	GoVisitor
	ids  []uuid.UUID
	seen map[uuid.UUID]struct{}
}

func (c *searchCollector) VisitMarker(marker java.Marker, p any) java.Marker {
	var id uuid.UUID
	switch x := marker.(type) {
	case java.SearchResult:
		id = x.Ident
	case *java.SearchResult:
		id = x.Ident
	case java.SearchResultMarker:
		id = x.Ident
	case *java.SearchResultMarker:
		id = x.Ident
	default:
		return marker
	}
	if _, dup := c.seen[id]; !dup {
		c.seen[id] = struct{}{}
		c.ids = append(c.ids, id)
	}
	return marker
}

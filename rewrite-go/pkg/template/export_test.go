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

package template

import (
	"reflect"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// MatchesViaWalk runs the match through the reflective walk alone.
func (p *GoPattern) MatchesViaWalk(candidate java.J, cursor *visitor.Cursor) bool {
	tree, err := p.getTree()
	if err != nil || tree == nil {
		return false
	}
	cmp := p.comparator(cursor)
	cmp.skipFastPath = true
	return cmp.match(tree, candidate) != nil
}

// Tree exposes the pattern's own parsed tree, so a test can assert how it was
// attributed rather than only what it matches.
func (p *GoPattern) Tree(t require.TestingT) java.J {
	tree, err := p.getTree()
	require.NoError(t, err)
	return tree
}

// MatchViaWalk is MatchesViaWalk keeping the bindings, so a test can hold the
// hand-written comparisons to what the walk captured as well as its answer.
func (p *GoPattern) MatchViaWalk(candidate java.J, cursor *visitor.Cursor) *MatchResult {
	tree, err := p.getTree()
	if err != nil || tree == nil {
		return nil
	}
	cmp := p.comparator(cursor)
	cmp.skipFastPath = true
	return cmp.match(tree, candidate)
}

// Bindings names what a match bound, for comparing two match results.
func (m *MatchResult) Bindings() map[string]bool {
	names := make(map[string]bool, len(m.bindings))
	for name := range m.bindings {
		names[name] = true
	}
	return names
}

// NodeIDs lists every node's ID, so a test can show two trees share none.
func NodeIDs(j java.J) []string {
	var ids []string
	visitor.Walk(j, func(t java.Tree) bool {
		if node, ok := t.(java.J); ok {
			ids = append(ids, node.GetID().String())
		}
		return true
	})
	return ids
}

// TreeOrError exposes the pattern's parse outcome for tests about scaffolding.
func (p *GoPattern) TreeOrError() (java.J, error) { return p.getTree() }

// ComparedFields names the fields the walk reads on a node, in order.
func ComparedFields(node java.J) []string {
	var names []string
	for _, step := range planFor(reflect.TypeOf(node).Elem()) {
		names = append(names, step.name)
	}
	return names
}

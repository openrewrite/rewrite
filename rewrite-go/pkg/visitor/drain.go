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

import "github.com/openrewrite/rewrite/rewrite-go/pkg/tree"

// AfterVisitsProvider is the structural interface a visitor implements
// to expose its DoAfterVisit queue. GoVisitor satisfies it; user-defined
// visitors that embed GoVisitor inherit the methods automatically.
type AfterVisitsProvider interface {
	AfterVisits() []AfterVisitor
	DoAfterVisit(AfterVisitor)
}

// DrainAfterVisits applies any visitors that `editor` queued via
// GoVisitor.DoAfterVisit. After-visits can themselves queue more
// after-visits (transitive); this loops until no provider has anything
// left in its queue. Mirrors JavaVisitor's afterVisit drain semantics.
//
// Returns the (possibly modified) tree. Callers should ALWAYS run this
// after the main editor.Visit so DoAfterVisit-queued follow-ups land.
// Pass `editor` as the recipe's TreeVisitor and the current tree + ctx.
func DrainAfterVisits(editor any, t tree.Tree, ctx any) tree.Tree {
	parent, ok := editor.(AfterVisitsProvider)
	if !ok {
		return t
	}
	for {
		batch := parent.AfterVisits()
		if len(batch) == 0 {
			return t
		}
		for _, v := range batch {
			result := v.Visit(t, ctx)
			if result != nil {
				t = result
			}
			// Forward any after-visits the queued visitor itself
			// produced back onto the parent so the outer loop drains
			// them next iteration.
			if pv, ok := v.(AfterVisitsProvider); ok {
				for _, m := range pv.AfterVisits() {
					parent.DoAfterVisit(m)
				}
			}
		}
	}
}

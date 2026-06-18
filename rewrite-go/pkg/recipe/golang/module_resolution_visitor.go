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

package golang

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// UpdateGoModModel returns a visitor that re-resolves the go.mod's
// GoResolutionResult marker via RefreshModel and replaces it. It is the
// composable counterpart to rewrite-maven's UpdateMavenModel: a recipe that
// mutates dependencies edits go.mod and then schedules this to run afterward —
//
//	v.DoAfterVisit(golang.UpdateGoModModel())
//
// so the refreshed model is visible to subsequent recipes in the same run.
// It works for any edit shape (add, remove, or modify a require/replace) because
// it operates on the final go.mod rather than intercepting the edit.
func UpdateGoModModel() recipe.TreeVisitor {
	return visitor.Init(&updateGoModModelVisitor{})
}

type updateGoModModelVisitor struct {
	visitor.GoVisitor
}

func (v *updateGoModModelVisitor) VisitGoMod(gm *golang.GoMod, p any) java.Tree {
	ctx, ok := p.(*recipe.ExecutionContext)
	if !ok {
		return gm
	}
	marker, ok := RefreshModel(gm, ctx)
	if !ok {
		return gm
	}
	return gm.WithMarkers(replaceResolution(gm.Markers, marker))
}

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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parenthesize"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// ParenthesizeService keeps an edited expression grouping the way it reads.
// Mirrors org.openrewrite.java.ParenthesizeVisitor.
//
//	svc := recipe.Service[*golang.ParenthesizeService](cu)
//	replacement = svc.MaybeParenthesize(replacement, v.Cursor())
type ParenthesizeService struct{}

// MaybeParenthesize groups e for the site it replaces. See parenthesize.Maybe.
func (s *ParenthesizeService) MaybeParenthesize(e java.Expression, site *visitor.Cursor) java.Expression {
	return parenthesize.Maybe(e, site)
}

// ParenthesizeVisitor groups every expression in the tree that needs it. Use it
// on a tree synthesized from parts, where there is no single replaced node to
// hang MaybeParenthesize off.
func (s *ParenthesizeService) ParenthesizeVisitor() recipe.TreeVisitor {
	return parenthesize.NewVisitor()
}

func init() {
	recipe.RegisterService[*ParenthesizeService](func() any { return &ParenthesizeService{} })
}

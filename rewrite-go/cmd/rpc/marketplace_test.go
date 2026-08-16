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
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

// ToListing must count a recipe plus every transitive entry in its RecipeList,
// not just its direct children — the host uses the value as a marketplace sort key.
func TestToListingCountsTransitiveSubRecipes(t *testing.T) {
	desc := recipe.RecipeDescriptor{
		Name: "root",
		RecipeList: []recipe.RecipeDescriptor{
			{
				Name: "middle",
				RecipeList: []recipe.RecipeDescriptor{
					{Name: "leaf"},
				},
			},
		},
	}
	if got := recipe.ToListing(desc).RecipeCount; got != 3 {
		t.Errorf("ToListing(desc).RecipeCount = %d, want 3 (root + middle + leaf)", got)
	}
}

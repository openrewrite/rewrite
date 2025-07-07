/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@Incubating(since = "8.31.0")
@RequiredArgsConstructor
public class RecipeList {
    private final String parentRecipeName;
    private int recipeIndex = 1;

    private List<Recipe> recipes;

    public RecipeList recipe(Recipe.Builder recipe) {
        return addRecipe(recipe.build(parentRecipeName + "$" + recipeIndex++));
    }

    public RecipeList recipe(@NlsRewrite.DisplayName @Language("markdown") String displayName,
                             @NlsRewrite.Description @Language("markdown") String description,
                             TreeVisitor<? extends Tree, ExecutionContext> visitor) {
        return recipe(Recipe.builder(displayName, description).visitor(visitor));
    }

    public RecipeList recipe(org.openrewrite.Recipe recipe) {
        return addRecipe(recipe);
    }

    public List<Recipe> getRecipes() {
        return recipes == null ? emptyList() : recipes;
    }

    private RecipeList addRecipe(Recipe recipe) {
        if (recipes == null) {
            recipes = new ArrayList<>();
        }
        recipes.add(recipe);
        return this;
    }
}

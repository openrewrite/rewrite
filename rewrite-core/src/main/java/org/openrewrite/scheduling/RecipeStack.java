/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.scheduling;

import lombok.Getter;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Recipe;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.openrewrite.Recipe.PANIC;

class RecipeStack {
    private final Map<Recipe, List<Recipe>> recipeLists = new IdentityHashMap<>();

    @SuppressWarnings("NotNullFieldNotInitialized")
    private ArrayDeque<RecipePath> allRecipesStack;

    /**
     * The zero-based position of the recipe that is currently doing a scan/generate/edit.
     */
    @NonFinal
    @Getter
    int recipePosition;

    public <T> @Nullable T reduce(LargeSourceSet sourceSet, Recipe recipe, ExecutionContext ctx,
                                  BiFunction<@Nullable T, List<Recipe>, @Nullable T> consumer, @Nullable T acc) {
        init(recipe);
        AtomicInteger recipePosition = new AtomicInteger(0);
        while (!allRecipesStack.isEmpty()) {
            if (ctx.getMessage(PANIC) != null) {
                break;
            }

            this.recipePosition = recipePosition.getAndIncrement();
            RecipePath recipePath = allRecipesStack.pop();
            if (recipePath.leaf().maxCycles() >= ctx.getCycle()) {
                sourceSet.setRecipe(recipePath);
                acc = consumer.apply(acc, recipePath);
                recurseRecipeList(recipePath);
            } else {
                this.recipePosition = recipePosition.getAndAdd(countRecipes(recipePath.leaf()));
            }
        }
        return acc;
    }

    private void init(Recipe recipe) {
        allRecipesStack = new ArrayDeque<>();
        allRecipesStack.push(new RecipePath(recipe));
    }

    private void recurseRecipeList(RecipePath recipePath) {
        List<Recipe> recipeList = getRecipeList(recipePath.leaf());
        ListIterator<Recipe> it = recipeList.listIterator(recipeList.size());
        while (it.hasPrevious()) {
            allRecipesStack.push(recipePath.child(it.previous()));
        }
    }

    private int countRecipes(Recipe recipe) {
        int count = 0;
        List<Recipe> recipeList = getRecipeList(recipe);
        for (Recipe subRecipe : recipeList) {
            count++;
            count += countRecipes(subRecipe);
        }
        return count;
    }

    private List<Recipe> getRecipeList(Recipe recipe) {
        return recipeLists.computeIfAbsent(recipe, Recipe::getRecipeList);
    }
}

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
import org.openrewrite.ExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Recipe;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.openrewrite.Recipe.PANIC;

class RecipeStack {
    private final Map<Recipe, List<Recipe>> recipeLists = new IdentityHashMap<>();
    private Stack<Stack<Recipe>> allRecipesStack;

    /**
     * The zero-based position of the recipe that is currently doing a scan/generate/edit.
     */
    @NonFinal
    @Getter
    int recipePosition;

    public LargeSourceSet apply(Recipe recipe, LargeSourceSet largeSourceSet, ExecutionContext ctx,
                                BiFunction<LargeSourceSet, Stack<Recipe>, LargeSourceSet> consumer) {
        init(recipe);
        AtomicInteger recipePosition = new AtomicInteger(0);
        while (!allRecipesStack.isEmpty()) {
            if (ctx.getMessage(PANIC) != null) {
                break;
            }
            this.recipePosition = recipePosition.getAndIncrement();
            Stack<Recipe> recipeStack = allRecipesStack.pop();
            largeSourceSet.setRecipe(recipeStack);
            largeSourceSet = consumer.apply(largeSourceSet, recipeStack);
            recurseRecipeList(recipeStack);
        }
        return largeSourceSet;
    }

    private void init(Recipe recipe) {
        allRecipesStack = new Stack<>();
        Stack<Recipe> rootRecipeStack = new Stack<>();
        rootRecipeStack.push(recipe);
        allRecipesStack.push(rootRecipeStack);
    }

    private void recurseRecipeList(Stack<Recipe> recipeStack) {
        List<Recipe> recipeList = getRecipeList(recipeStack);
        for (int i = recipeList.size() - 1; i >= 0; i--) {
            Recipe r = recipeList.get(i);
            Stack<Recipe> nextStack = new Stack<>();
            nextStack.addAll(recipeStack);
            nextStack.push(r);
            allRecipesStack.push(nextStack);
        }
    }

    private List<Recipe> getRecipeList(Stack<Recipe> recipeStack) {
        return recipeLists.computeIfAbsent(recipeStack.peek(), Recipe::getRecipeList);
    }
}

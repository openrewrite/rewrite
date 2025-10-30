/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.marketplace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;
import org.openrewrite.config.RecipeDescriptor;

import java.util.*;

@Getter
@RequiredArgsConstructor
public class RecipeMarketplace {
    static final String ROOT = "ε";

    @NlsRewrite.DisplayName
    private final String displayName;

    @NlsRewrite.DisplayName
    private final String description;

    /**
     * Every category is itself a slightly-more miniature marketplace contained in a larger
     * marketplace.
     */
    private final List<RecipeMarketplace> categories = new ArrayList<>();

    private final List<RecipeListing> recipes = new ArrayList<>();

    public boolean isRoot() {
        return ROOT.equals(displayName);
    }

    public static RecipeMarketplace newEmpty() {
        return new RecipeMarketplace(ROOT, "");
    }

    public @Nullable RecipeDescriptor findRecipe(String name) {
        for (RecipeListing recipe : recipes) {
            if (recipe.getName().equals(name)) {
                return recipe.describe();
            }
        }
        for (RecipeMarketplace category : categories) {
            RecipeDescriptor rd = category.findRecipe(name);
            if (rd != null) {
                return rd;
            }
        }
        return null;
    }

    /**
     * Get all recipes from this marketplace and all subcategories recursively.
     * The returned set is distinct by recipe name i.e., if the same recipe appears
     * in multiple categories, only the first occurrence is included.
     *
     * @return A set of all unique recipes (by name) in this marketplace and its subcategories
     */
    public Set<RecipeListing> getAllRecipes() {
        Map<String, RecipeListing> recipesByName = new LinkedHashMap<>();
        collectAllRecipes(recipesByName);
        return new LinkedHashSet<>(recipesByName.values());
    }

    private void collectAllRecipes(Map<String, RecipeListing> recipesByName) {
        for (RecipeListing recipe : recipes) {
            recipesByName.putIfAbsent(recipe.getName(), recipe);
        }
        for (RecipeMarketplace category : categories) {
            category.collectAllRecipes(recipesByName);
        }
    }

    /**
     * Add a recipe to this marketplace under the specified category path.
     * Categories are specified top-down (shallowest to deepest).
     * Intermediate categories are created as needed.
     *
     * @param recipe       The recipe to add
     * @param categoryPath Category path from shallowest to deepest (e.g., "Java", "Search")
     */
    public void addRecipe(RecipeListing recipe, String... categoryPath) {
        if (categoryPath.length == 0) {
            recipes.add(recipe);
            return;
        }

        // Get the first category in the path
        String firstCategory = categoryPath[0];
        RecipeMarketplace targetCategory = findOrCreateCategory(firstCategory);

        // Build remaining path
        String[] remainingPath = new String[categoryPath.length - 1];
        System.arraycopy(categoryPath, 1, remainingPath, 0, remainingPath.length);

        // Recursively add to the child category
        targetCategory.addRecipe(recipe, remainingPath);
    }

    /**
     * Merge another marketplace into this one. Recursively processes all recipes
     * and subcategories from the source marketplace. If a recipe with the same name
     * already exists in the same category, it will be replaced by the one being merged in.
     *
     * @param other The marketplace to merge into this one.
     */
    public void merge(RecipeMarketplace other) {
        for (RecipeListing recipe : other.getRecipes()) {
            recipes.removeIf(existing -> existing.getName().equals(recipe.getName()));
            recipes.add(recipe);
        }
        for (RecipeMarketplace otherCategory : other.getCategories()) {
            RecipeMarketplace category = findOrCreateCategory(otherCategory.getDisplayName());
            category.merge(otherCategory);
        }
    }

    private RecipeMarketplace findOrCreateCategory(String categoryName) {
        for (RecipeMarketplace category : categories) {
            if (category.getDisplayName().equals(categoryName)) {
                return category;
            }
        }

        RecipeMarketplace newCategory = new RecipeMarketplace(categoryName, "");
        categories.add(newCategory);
        return newCategory;
    }
}

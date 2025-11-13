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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.NlsRewrite;

import java.util.*;

@Incubating(since = "8.66.0")
public class RecipeMarketplace {
    private final @Getter(AccessLevel.PACKAGE) Category root = new Category("Root");
    private final @Getter List<RecipeBundleResolver> resolvers = new ArrayList<>();

    public RecipeMarketplace setResolvers(RecipeBundleResolver... resolvers) {
        this.resolvers.clear();
        Collections.addAll(this.resolvers, resolvers);
        return this;
    }

    public @Nullable RecipeListing findRecipe(String name) {
        return root.findRecipe(name);
    }

    public Set<RecipeListing> getAllRecipes() {
        return root.getAllRecipes();
    }

    public List<Category> getCategories() {
        return root.getCategories();
    }

    public void install(RecipeListing recipe, List<String> categoryPath) {
        root.install(recipe, categoryPath);
    }

    public void install(RecipeBundleReader bundleReader) {
        RecipeMarketplace marketplace = bundleReader.read();
        getRoot().merge(marketplace.getRoot());
        getRoot().updateBundle(marketplace.getAllRecipes(), bundleReader.getBundle());
    }

    @Getter
    @RequiredArgsConstructor
    public class Category {
        @NlsRewrite.DisplayName
        private final String displayName;

        @Setter
        @NlsRewrite.DisplayName
        private String description = "";

        private final List<Category> categories = new ArrayList<>();
        private final List<RecipeListing> recipes = new ArrayList<>();

        public void merge(Category category) {
            for (RecipeListing recipe : category.recipes) {
                recipes.remove(recipe);
                recipes.add(recipe.withMarketplace(RecipeMarketplace.this));
            }
            for (Category subCategory : category.categories) {
                Category existingSubCategory = null;
                for (Category c : categories) {
                    if (c.getDisplayName().equals(subCategory.getDisplayName())) {
                        existingSubCategory = c;
                        break;
                    }
                }
                if (existingSubCategory != null) {
                    existingSubCategory.merge(subCategory);
                } else {
                    categories.add(subCategory);
                }
            }
        }

        void updateBundle(Collection<RecipeListing> recipes, RecipeBundle bundle) {
            for (RecipeListing recipe : recipes) {
                if (this.recipes.contains(recipe)) {
                    this.recipes.remove(recipe);
                    this.recipes.add(recipe.withBundle(bundle));
                }
            }
            for (Category category : categories) {
                category.updateBundle(recipes, bundle);
            }
        }

        public @Nullable RecipeListing findRecipe(String name) {
            for (RecipeListing recipe : recipes) {
                if (recipe.getName().equals(name)) {
                    return recipe;
                }
            }
            for (Category category : categories) {
                RecipeListing rd = category.findRecipe(name);
                if (rd != null) {
                    return rd;
                }
            }
            return null;
        }

        public Set<RecipeListing> getAllRecipes() {
            Set<RecipeListing> recipes = new TreeSet<>();
            getAllRecipesRecursive(recipes);
            return recipes;
        }

        private void getAllRecipesRecursive(Set<RecipeListing> recipes) {
            recipes.addAll(this.recipes);
            for (Category category : categories) {
                category.getAllRecipesRecursive(recipes);
            }
        }

        /**
         * Add a recipe to this category under the specified category path.
         * Categories are specified top-down (shallowest to deepest).
         * Intermediate categories are created as needed.
         *
         * @param recipe       The recipe to add
         * @param categoryPath Category path from shallowest to deepest (e.g., "Java", "Search")
         */
        public void install(RecipeListing recipe, List<String> categoryPath) {
            recipe = recipe.withMarketplace(RecipeMarketplace.this);

            if (categoryPath.isEmpty()) {
                recipes.add(recipe);
                return;
            }

            // Get the first category in the path
            String firstCategory = categoryPath.get(0);
            Category targetCategory = findOrCreateCategory(firstCategory);

            // Recursively add to the child category
            targetCategory.install(recipe, categoryPath.subList(1, categoryPath.size()));
        }

        private Category findOrCreateCategory(String categoryName) {
            for (Category category : categories) {
                if (category.getDisplayName().equals(categoryName)) {
                    return category;
                }
            }

            // FIXME how do we overlay CategoryDescriptor on these so that the descriptions are set?
            Category newCategory = new Category(categoryName);
            categories.add(newCategory);
            return newCategory;
        }
    }
}

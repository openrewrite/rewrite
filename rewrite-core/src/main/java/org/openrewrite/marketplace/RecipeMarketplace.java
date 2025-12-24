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
import org.openrewrite.Incubating;
import org.openrewrite.NlsRewrite;
import org.openrewrite.config.CategoryDescriptor;

import java.util.*;

@Incubating(since = "8.66.0")
public class RecipeMarketplace {
    private final @Getter Category root = new Category("Root",
            "This is the root of all categories. " +
            "When displaying the category hierarchy of a marketplace, " +
            "this is typically not shown.");

    private final @Getter List<RecipeBundleResolver> resolvers = new ArrayList<>();

    public RecipeMarketplace setResolvers(Collection<RecipeBundleResolver> resolvers) {
        this.resolvers.clear();
        this.resolvers.addAll(resolvers);
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

    public void install(RecipeListing recipe, List<CategoryDescriptor> categoryPath) {
        root.install(recipe, categoryPath);
    }

    public Set<RecipeListing> install(RecipeBundleReader bundleReader) {
        RecipeMarketplace marketplace = bundleReader.read();
        RecipeBundle bundle = bundleReader.getBundle();
        uninstall(bundle.getPackageEcosystem(), bundle.getPackageName());
        root.merge(marketplace.getRoot());
        return marketplace.getAllRecipes();
    }

    public void uninstall(String packageEcosystem, String packageName) {
        root.uninstall(packageEcosystem, packageName);
    }

    @Getter
    @RequiredArgsConstructor
    public class Category {
        @NlsRewrite.DisplayName
        private final String displayName;

        @NlsRewrite.DisplayName
        private final String description;

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

        public void uninstall(String packageEcosystem, String packageName) {
            recipes.removeIf(r -> r.getBundle().getPackageName().equals(packageName) &&
                                  r.getBundle().getPackageEcosystem().equals(packageEcosystem));
            for (Category category : categories) {
                category.uninstall(packageEcosystem, packageName);
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
        public void install(RecipeListing recipe, List<CategoryDescriptor> categoryPath) {
            recipe = recipe.withMarketplace(RecipeMarketplace.this);

            if (categoryPath.isEmpty()) {
                recipes.add(recipe);
                return;
            }

            // Get the first category in the path
            CategoryDescriptor firstCategory = categoryPath.get(0);
            Category targetCategory = findOrCreateCategory(firstCategory);

            // Recursively add to the child category
            targetCategory.install(recipe, categoryPath.subList(1, categoryPath.size()));
        }

        private Category findOrCreateCategory(CategoryDescriptor categoryDescriptor) {
            for (Category category : categories) {
                if (category.getDisplayName().equals(categoryDescriptor.getDisplayName())) {
                    return category;
                }
            }
            Category newCategory = new Category(
                    categoryDescriptor.getDisplayName(),
                    categoryDescriptor.getDescription());
            categories.add(newCategory);
            return newCategory;
        }
    }
}

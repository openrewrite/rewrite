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
import lombok.Value;
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

    private static final Comparator<RecipeBundle> BY_COORDINATE =
            Comparator.comparing(RecipeBundle::getPackageEcosystem)
                    .thenComparing(RecipeBundle::getPackageName);

    private final Map<BundleKey, RecipeBundle> bundles = new HashMap<>();

    public @Nullable RecipeListing findRecipe(String name) {
        return root.findRecipe(name);
    }

    public Set<RecipeListing> getAllRecipes() {
        return root.getAllRecipes();
    }

    public Set<RecipeBundle> getBundles() {
        Set<RecipeBundle> sorted = new TreeSet<>(BY_COORDINATE);
        sorted.addAll(bundles.values());
        return sorted;
    }

    public @Nullable RecipeBundle bundleFor(String packageEcosystem, String packageName) {
        return bundles.get(new BundleKey(packageEcosystem, packageName));
    }

    private RecipeBundle intern(RecipeBundle bundle) {
        if (bundle.getPackageEcosystem() == null || bundle.getPackageName() == null) {
            return bundle;
        }
        return bundles.computeIfAbsent(
                new BundleKey(bundle.getPackageEcosystem(), bundle.getPackageName()), k -> bundle);
    }

    @Value
    private static class BundleKey {
        String packageEcosystem;
        String packageName;
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
        bindRecursive(marketplace.getRoot(), bundle);
        uninstall(bundle.getPackageEcosystem(), bundle.getPackageName());
        return merge(marketplace);
    }

    /**
     * Contribute another marketplace's recipes to this one, keeping what is already here. A
     * bundle coordinate already installed blocks every listing the incoming marketplace offers
     * for it, whether that is the same version (a duplicate) or a different one (a farther
     * layer, superseded by the nearer install). Callers express precedence by merging the
     * nearest scope first.
     *
     * @return The listings actually added, so callers report what landed rather than what was
     * offered. Keyed by recipe name, so a recipe filed under several categories counts once --
     * and so do two bundles declaring one name, which no current caller can produce because
     * every merge contributes a single bundle.
     */
    public Set<RecipeListing> merge(RecipeMarketplace marketplace) {
        Set<RecipeListing> added = new LinkedHashSet<>();
        // Snapshot, because interning registers each incoming bundle as its first listing lands
        // and a bundle's recipes span several categories -- a live check would block its own
        // second category.
        root.merge(marketplace.getRoot(), new HashSet<>(bundles.keySet()), added);
        return added;
    }

    private void bindRecursive(Category category, RecipeBundle bundle) {
        category.getRecipes().replaceAll(listing -> listing.withBundle(bundle));
        for (Category child : category.getCategories()) {
            bindRecursive(child, bundle);
        }
    }

    public void uninstall(String packageEcosystem, String packageName) {
        root.uninstall(packageEcosystem, packageName);
        bundles.remove(new BundleKey(packageEcosystem, packageName));
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

        private void merge(Category category, Set<BundleKey> alreadyInstalled, Set<RecipeListing> added) {
            for (RecipeListing recipe : category.recipes) {
                RecipeBundle bundle = recipe.getBundle();
                if (bundle.getPackageEcosystem() != null && bundle.getPackageName() != null &&
                    alreadyInstalled.contains(new BundleKey(bundle.getPackageEcosystem(), bundle.getPackageName()))) {
                    continue;
                }
                RecipeListing installed = recipe.withMarketplace(RecipeMarketplace.this)
                        .withBundle(intern(bundle));
                recipes.add(installed);
                added.add(installed);
            }
            for (Category subCategory : category.categories) {
                Category existingSubCategory = null;
                for (Category c : categories) {
                    if (c.getDisplayName().equalsIgnoreCase(subCategory.getDisplayName())) {
                        existingSubCategory = c;
                        break;
                    }
                }
                if (existingSubCategory != null) {
                    existingSubCategory.merge(subCategory, alreadyInstalled, added);
                } else {
                    Category copy = new Category(subCategory.displayName, subCategory.description);
                    copy.merge(subCategory, alreadyInstalled, added);
                    if (!copy.recipes.isEmpty() || !copy.categories.isEmpty()) {
                        categories.add(copy);
                    }
                }
            }
        }

        private void uninstall(String packageEcosystem, String packageName) {
            recipes.removeIf(r -> Objects.equals(r.getBundle().getPackageName(), packageName) &&
                                  Objects.equals(r.getBundle().getPackageEcosystem(), packageEcosystem));
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
        private void install(RecipeListing recipe, List<CategoryDescriptor> categoryPath) {
            recipe = recipe.withMarketplace(RecipeMarketplace.this)
                    .withBundle(intern(recipe.getBundle()));

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
                if (category.getDisplayName().equalsIgnoreCase(categoryDescriptor.getDisplayName())) {
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

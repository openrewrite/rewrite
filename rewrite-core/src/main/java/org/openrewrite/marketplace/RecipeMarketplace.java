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

    private final Map<BundleKey, RecipeBundle> bundles = new LinkedHashMap<>();

    public @Nullable RecipeListing findRecipe(String name) {
        return root.findRecipe(name);
    }

    public Set<RecipeListing> getAllRecipes() {
        return root.getAllRecipes();
    }

    /** @return Every distinct bundle contributing recipes to this marketplace. */
    public Set<RecipeBundle> getBundles() {
        return new LinkedHashSet<>(bundles.values());
    }

    public @Nullable RecipeBundle bundleFor(String packageEcosystem, String packageName) {
        return bundles.get(new BundleKey(packageEcosystem, packageName));
    }

    /**
     * Canonicalize so every listing from one bundle shares one instance. A bundle with no
     * identity — an inner CSV that omitted the columns — is not registered; install binds it
     * to the resolved bundle before it matters.
     */
    RecipeBundle intern(RecipeBundle bundle) {
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
        return root.merge(marketplace.getRoot());
    }

    /**
     * A reader contributes only its own bundle's recipes, so whatever identity the payload
     * claimed is replaced by the bundle that was actually resolved. Walks the tree rather than
     * getAllRecipes(), which deduplicates by name and would miss a recipe's sibling listings
     * in other categories.
     */
    private void bindRecursive(Category category, RecipeBundle bundle) {
        category.getRecipes().replaceAll(listing -> listing.withBundle(bundle));
        for (Category child : category.getCategories()) {
            bindRecursive(child, bundle);
        }
    }

    public void uninstall(String packageEcosystem, String packageName) {
        root.uninstall(packageEcosystem, packageName);
        // Once a bundle contributes no listings it must not linger in the registry --
        // otherwise a later install() of the same (ecosystem, packageName) at a new version
        // finds the stale key still populated and intern() re-attaches the old instance.
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

        /**
         * @return The listings actually added to this category (or a subcategory), i.e. excluding
         * any that lost the first-wins name collision below. Used by {@link RecipeMarketplace#install}
         * to report only what actually landed, rather than everything the source category offered.
         */
        public Set<RecipeListing> merge(Category category) {
            Set<RecipeListing> added = new LinkedHashSet<>();
            for (RecipeListing recipe : category.recipes) {
                // First-wins, matching findRecipe/getAllRecipes traversal order and this method's own
                // subcategory branch, which keeps the existing node. Callers express precedence by
                // merging the nearest scope first.
                if (!recipes.contains(recipe)) {
                    RecipeListing installed = recipe.withMarketplace(RecipeMarketplace.this)
                            .withBundle(intern(recipe.getBundle()));
                    recipes.add(installed);
                    added.add(installed);
                }
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
                    added.addAll(existingSubCategory.merge(subCategory));
                } else {
                    Category copy = new Category(subCategory.displayName, subCategory.description);
                    added.addAll(copy.merge(subCategory));
                    categories.add(copy);
                }
            }
            return added;
        }

        public void uninstall(String packageEcosystem, String packageName) {
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
        public void install(RecipeListing recipe, List<CategoryDescriptor> categoryPath) {
            installInto(recipe.withMarketplace(RecipeMarketplace.this)
                    .withBundle(intern(recipe.getBundle())), categoryPath);
        }

        /**
         * Recursive descent for {@link #install}. Takes a recipe that has already been bound
         * and interned once at the entry point, so it isn't redundantly re-bound/re-interned at
         * every category depth.
         */
        private void installInto(RecipeListing recipe, List<CategoryDescriptor> categoryPath) {
            if (categoryPath.isEmpty()) {
                recipes.add(recipe);
                return;
            }

            // Get the first category in the path
            CategoryDescriptor firstCategory = categoryPath.get(0);
            Category targetCategory = findOrCreateCategory(firstCategory);

            // Recursively add to the child category
            targetCategory.installInto(recipe, categoryPath.subList(1, categoryPath.size()));
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

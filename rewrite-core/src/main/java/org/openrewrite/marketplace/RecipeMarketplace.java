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
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.NlsRewrite;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.internal.StringUtils;

import java.util.*;

import static java.util.Collections.unmodifiableList;

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
        RecipeBundle bundle = bundleReader.getBundle();
        String packageEcosystem = bundle.getPackageEcosystem();
        String packageName = bundle.getPackageName();
        if (StringUtils.isBlank(packageEcosystem) || StringUtils.isBlank(packageName)) {
            throw new IllegalArgumentException("A bundle must be resolved to an ecosystem and package name " +
                                               "before it can be installed, but got ecosystem '" + packageEcosystem +
                                               "' and package name '" + packageName + "'");
        }
        RecipeMarketplace marketplace = bundleReader.read();
        bindRecursive(marketplace.getRoot(), bundle);
        uninstall(packageEcosystem, packageName);
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
     * offered. Keyed by recipe name, so a recipe filed under several categories counts once,
     * as do two bundles declaring one name.
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
        category.recipes.replaceAll(listing -> listing.withBundle(bundle));
        for (Category child : category.categories) {
            bindRecursive(child, bundle);
        }
    }

    /**
     * @return The listings removed, keyed by recipe name as {@link #merge} keys what it added.
     * Not recoverable by diffing {@link #getAllRecipes()}: that projection is keyed by name, so a
     * removal another bundle's same-named listing was shadowing leaves its size unchanged.
     */
    public Set<RecipeListing> uninstall(String packageEcosystem, String packageName) {
        Set<RecipeListing> removed = new LinkedHashSet<>();
        root.uninstall(packageEcosystem, packageName, removed);
        bundles.remove(new BundleKey(packageEcosystem, packageName));
        return removed;
    }

    @Getter
    public class Category {
        @NlsRewrite.DisplayName
        private final String displayName;

        @NlsRewrite.DisplayName
        private String description;

        private final List<Category> categories = new ArrayList<>();
        private final List<RecipeListing> recipes = new ArrayList<>();

        public Category(@NlsRewrite.DisplayName String displayName, @NlsRewrite.DisplayName String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Categories are keyed by display name, so the first artifact to install one owns the
         * node and later descriptors for the same name are discarded. When the winner carried no
         * description, adopt the first one offered rather than leaving the category undescribed.
         */
        private void describeIfBlank(@Nullable String candidate) {
            if (StringUtils.isBlank(description) && !StringUtils.isBlank(candidate)) {
                description = candidate;
            }
        }

        /**
         * A view, not a copy. Structural change goes through {@link #install} and
         * {@link #uninstall}, which keep the bundle registry in step with the tree.
         */
        public List<Category> getCategories() {
            return unmodifiableList(categories);
        }

        /**
         * @see #getCategories()
         */
        public List<RecipeListing> getRecipes() {
            return unmodifiableList(recipes);
        }

        private void merge(Category category, Set<BundleKey> alreadyInstalled, Set<RecipeListing> added) {
            for (RecipeListing recipe : category.recipes) {
                RecipeBundle bundle = recipe.getBundle();
                String packageEcosystem = bundle.getPackageEcosystem();
                String packageName = bundle.getPackageName();
                if (StringUtils.isBlank(packageEcosystem) || StringUtils.isBlank(packageName)) {
                    throw new IllegalArgumentException("Cannot merge '" + recipe.getName() + "' because its bundle " +
                                                       "has no ecosystem and package name. A marketplace read from " +
                                                       "an inner CSV must be bound to a resolved bundle first.");
                }
                if (alreadyInstalled.contains(new BundleKey(packageEcosystem, packageName))) {
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
                    existingSubCategory.describeIfBlank(subCategory.description);
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

        private void uninstall(String packageEcosystem, String packageName, Set<RecipeListing> removed) {
            recipes.removeIf(r -> {
                if (Objects.equals(r.getBundle().getPackageName(), packageName) &&
                    Objects.equals(r.getBundle().getPackageEcosystem(), packageEcosystem)) {
                    removed.add(r);
                    return true;
                }
                return false;
            });
            for (Category category : categories) {
                category.uninstall(packageEcosystem, packageName, removed);
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
                    category.describeIfBlank(categoryDescriptor.getDescription());
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

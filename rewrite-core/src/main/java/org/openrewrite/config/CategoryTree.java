/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.config;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

/**
 * A hierarchical listing of recipe categories and the recipes that are contained inside of them.
 */
public class CategoryTree<G> {
    private final Object lock = new Object();

    private final CategoryDescriptor descriptor;
    private final Collection<CategoryTree<G>> subtrees = new ArrayList<>();
    private Map<G, Collection<RecipeDescriptor>> recipesByGroup = new HashMap<>();

    private CategoryTree(CategoryDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public static <G> CategoryTree<G> build() {
        return new CategoryTree<>(new CategoryDescriptor("Root", "", "", emptySet()));
    }

    public CategoryDescriptor getDescriptor() {
        return descriptor;
    }

    public Integer getRecipeCount() {
        return recipesByGroup.values().stream().mapToInt(Collection::size).sum() +
                subtrees.stream().mapToInt(CategoryTree::getRecipeCount).sum();
    }

    @Nullable
    public CategoryTree<G> getCategory(String subcategory) {
        String packageName = descriptor.getPackageName();
        synchronized (lock) {
            if ("core".equals(subcategory) && !recipesByGroup.isEmpty()) {
                return syntheticCore();
            }

            for (CategoryTree<G> t : subtrees) {
                String tPackage = t.getDescriptor().getPackageName();
                int endIndex = tPackage.indexOf('.', packageName.length() + 1);
                String test = tPackage.substring(
                        packageName.isEmpty() ? 0 : packageName.length() + 1,
                        endIndex < 0 ? tPackage.length() : endIndex
                );
                if (subcategory.equals(test)) {
                    return t;
                }
            }
        }
        return null;
    }

    @Nullable
    public CategoryTree<G> getCategory(String... subcategories) {
        CategoryTree<G> acc = this;
        for (String subcategory : subcategories) {
            if (acc == null) {
                return null;
            }
            acc = acc.getCategory(subcategory);
        }
        return acc;
    }

    public CategoryTree<G> getCategoryOrThrow(String subcategory) {
        CategoryTree<G> subtree = getCategory(subcategory);
        if (subtree == null) {
            throw new IllegalArgumentException("No subcategory of " +
                    descriptor.getPackageName() + " named '" + subcategory + "'");
        }
        return subtree;
    }

    public CategoryTree<G> getCategoryOrThrow(String... subcategories) {
        CategoryTree<G> acc = this;
        for (String subcategory : subcategories) {
            acc = acc.getCategoryOrThrow(subcategory);
        }
        return acc;
    }

    @Nullable
    public RecipeDescriptor getRecipe(String id) {
        if (id.contains(".")) {
            String[] split = id.split("\\.", 2);
            CategoryTree<G> subcategory = getCategory(split[0]);
            return subcategory == null ? null : subcategory.getRecipe(split[1]);
        }

        return recipesByGroup.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> r.getName().substring(r.getName().lastIndexOf('.') + 1).equals(id))
                .findAny()
                .orElse(null);
    }

    @Nullable
    public G getRecipeGroup(String id) {
        if (id.contains(".")) {
            String[] split = id.split("\\.", 2);
            CategoryTree<G> subcategory = getCategory(split[0]);
            return subcategory == null ? null : subcategory.getRecipeGroup(split[1]);
        }

        return recipesByGroup.entrySet().stream()
                .filter(g -> g.getValue().stream().anyMatch(r -> r.getName().substring(r.getName().lastIndexOf('.') + 1).equals(id)))
                .map(Map.Entry::getKey)
                .findAny()
                .orElse(null);
    }

    public CategoryTree<G> putAll(G group, Environment environment) {
        return putAll(group, environment.listRecipeDescriptors(), environment.listCategoryDescriptors());
    }

    public CategoryTree<G> putAll(G group, Iterable<RecipeDescriptor> recipes, Iterable<CategoryDescriptor> categories) {
        synchronized (lock) {
            removeAll(group);
            for (RecipeDescriptor recipe : recipes) {
                add(group, recipe, categories);
            }
        }
        return this;
    }

    private void add(G group, RecipeDescriptor recipe, Iterable<CategoryDescriptor> categories) {
        String category = recipe.getName().substring(0, recipe.getName().lastIndexOf('.'));
        String packageName = descriptor.getPackageName();
        if (category.equals(packageName)) {
            recipesByGroup.computeIfAbsent(group, g -> new ArrayList<>()).add(recipe);
        } else if (category.startsWith(packageName)) {
            CategoryTree<G> subtree = null;
            for (CategoryTree<G> s : subtrees) {
                if (category.startsWith(s.descriptor.getPackageName())) {
                    subtree = s;
                    break;
                }
            }

            if (subtree == null) {
                int endIndex = category.indexOf('.', packageName.length() + 1);
                String subcategoryPackage = endIndex < 0 ?
                        category :
                        packageName + category.substring(packageName.length(), endIndex);

                CategoryDescriptor subcategoryDescriptor = null;
                for (CategoryDescriptor categoryDescriptor : categories) {
                    if (categoryDescriptor.getPackageName().equals(subcategoryPackage)) {
                        subcategoryDescriptor = categoryDescriptor;
                        break;
                    }
                }

                if (subcategoryDescriptor == null) {
                    subcategoryDescriptor = new CategoryDescriptor(
                            StringUtils.capitalize(subcategoryPackage.substring(subcategoryPackage.lastIndexOf('.') + 1)),
                            subcategoryPackage,
                            "",
                            emptySet()
                    );
                }

                subtree = new CategoryTree<>(subcategoryDescriptor);
                subtrees.add(subtree);
            }

            subtree.add(group, recipe, categories);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public CategoryTree<G> removeAll(G group) {
        synchronized (lock) {
            recipesByGroup.remove(group);
            for (CategoryTree<G> subtree : subtrees) {
                subtree.removeAll(group);
            }
        }
        return this;
    }

    public Collection<RecipeDescriptor> getRecipes() {
        synchronized (lock) {
            return Stream.concat(
                            recipesByGroup.values().stream().flatMap(Collection::stream),
                            subtrees.stream().flatMap(it -> it.getRecipes().stream())
                    )
                    .distinct()
                    .collect(toList());
        }
    }

    @SuppressWarnings("unused")
    public Map<G, Collection<RecipeDescriptor>> getRecipesByGroup() {
        synchronized (lock) {
            return Collections.unmodifiableMap(new HashMap<>(recipesByGroup));
        }
    }

    public Collection<CategoryTree<G>> getSubtrees() {
        synchronized (lock) {
            if (!subtrees.isEmpty() && !recipesByGroup.isEmpty()) {
                /*~~>*/List<CategoryTree<G>> subtreesAndCore = new ArrayList<>(subtrees);
                subtreesAndCore.add(syntheticCore());
                return subtreesAndCore;
            }
            return subtrees;
        }
    }

    @NotNull
    private CategoryTree<G> syntheticCore() {
        CategoryTree<G> core = new CategoryTree<>(
                new CategoryDescriptor(
                        "Core",
                        descriptor.getPackageName() + ".core",
                        "",
                        emptySet()
                )
        );
        core.recipesByGroup = recipesByGroup;
        return core;
    }
}

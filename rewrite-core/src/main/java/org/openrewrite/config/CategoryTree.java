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

import lombok.Value;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.config.CategoryTree.PrintNameStyle.ID;

/**
 * A hierarchical listing of recipe categories and the recipes that are contained inside them.
 *
 * @param <G> A grouping key that can cross category boundaries. Must implement {@link Object#equals} and {@link Object#hashCode},
 *            but there is otherwise no restriction on the type. For example, a grouping key could be a {@link String} that represents
 *            a GAV coordinate of a recipe artifact that contributes recipes to multiple categories.
 */
public class CategoryTree<G> {
    /**
     * This is a synthetic category used so that a category's children are either all subcategories or all recipes,
     * and never a mixture of both.
     */
    static final String CORE = "core";

    final Object lock = new Object();

    /**
     * Groups contributing information about a category. This information is not merged together,
     * so the last group explicitly contributing information about a category controls its details.
     * Should that group ever be removed, it will be popped from this stack, revealing category details
     * from the next group in line.
     */
    private final List<G> groups = new ArrayList<>(3);

    private final Collection<CategoryTree<G>> subtrees = new ArrayList<>();
    private final Map<G, CategoryDescriptor> descriptorsByGroup = new HashMap<>();
    private final Map<G, Collection<RecipeDescriptor>> recipesByGroup = new HashMap<>();

    CategoryTree() {
    }

    private CategoryTree(G group, CategoryDescriptor descriptor) {
        descriptorsByGroup.put(group, descriptor);
        groups.add(group);
    }

    public static class Root<G> extends CategoryTree<G> {
        private static final CategoryDescriptor ROOT_DESCRIPTOR = new CategoryDescriptor(
                "ε", "", "", emptySet(), true,
                CategoryDescriptor.LOWEST_PRECEDENCE, true);

        private Root() {
            super();
        }

        public CategoryTree.Root<G> removeAll(G group) {
            // increase visibility and cast
            return (Root<G>) super.removeAll(group);
        }

        public CategoryTree.Root<G> putAll(G group, Environment environment) {
            synchronized (lock) {
                return removeAll(group)
                        .putRecipes(group, environment.listRecipeDescriptors().toArray(new RecipeDescriptor[0]))
                        .putCategories(group, environment.listCategoryDescriptors().toArray(new CategoryDescriptor[0]));
            }
        }

        public CategoryTree.Root<G> putRecipes(G group, RecipeDescriptor... recipes) {
            synchronized (lock) {
                for (RecipeDescriptor recipe : recipes) {
                    addRecipe(group, recipe);
                }
            }
            return this;
        }

        public synchronized CategoryTree.Root<G> putCategories(G group, CategoryDescriptor... categories) {
            synchronized (lock) {
                for (CategoryDescriptor category : categories) {
                    findOrAddCategory(group, category);
                }
            }
            return this;
        }

        @Override
        public CategoryDescriptor getDescriptor() {
            return ROOT_DESCRIPTOR;
        }

        @Override
        public String toString() {
            return "CategoryTree{ROOT}";
        }

        public String print(PrintOptions printOptions) {
            StringJoiner out = new StringJoiner("\n");
            toString(out, 0, printOptions, new BitSet());
            return out.toString();
        }
    }

    public static <G> CategoryTree.Root<G> build() {
        return new CategoryTree.Root<>();
    }

    public CategoryDescriptor getDescriptor() {
        CategoryDescriptor categoryDescriptor = null;
        int currentPriority = CategoryDescriptor.LOWEST_PRECEDENCE;

        // find the highest priority non-synthetic descriptor, if any
        for (G group : groups) {
            CategoryDescriptor test = descriptorsByGroup.get(group);
            if (!test.isSynthetic() && test.getPriority() > currentPriority) {
                categoryDescriptor = test;
                currentPriority = test.getPriority();
            }
        }

        if (categoryDescriptor == null) {
            // select a synthetic descriptor as a fallback
            for (G group : groups) {
                categoryDescriptor = descriptorsByGroup.get(group);
                break;
            }
            if (categoryDescriptor == null) {
                throw new IllegalStateException("Unable to find a descriptor for category. This represents " +
                                                "a bug in CategoryTree, since it should never occur.");
            }
        }

        return categoryDescriptor;
    }

    public Integer getRecipeCount() {
        int sum = 0;
        for (Collection<RecipeDescriptor> recipeDescriptors : getRecipesByGroup().values()) {
            sum += recipeDescriptors.size();
        }
        for (CategoryTree<G> subtree : subtrees) {
            sum += subtree.getRecipeCount();
        }
        return sum;
    }

    @Nullable
    public CategoryTree<G> getCategory(String subcategory) {
        String packageName = getDescriptor().getPackageName();
        synchronized (lock) {
            String[] split = subcategory.split("\\.", 2);
            for (CategoryTree<G> t : getCategories(false, true)) {
                String tPackage = t.getDescriptor().getPackageName();
                int endIndex = tPackage.indexOf('.', packageName.length() + 1);
                String test = tPackage.substring(
                        packageName.isEmpty() ? 0 : packageName.length() + 1,
                        endIndex < 0 ? tPackage.length() : endIndex
                );
                if (split[0].equals(test)) {
                    if (split.length == 1) {
                        return t;
                    } else {
                        return t.getCategory(split[1]);
                    }
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
                                               getDescriptor().getPackageName() + " named '" + subcategory + "'");
        }
        return subtree;
    }

    public CategoryTree<G> getCategoryOrThrow(String... subcategories) {
        CategoryTree<G> acc = this;
        for (String subcategory : subcategories) {
            for (String subsubcategory : subcategory.split("\\.")) {
                acc = acc.getCategoryOrThrow(subsubcategory);
            }
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
        for (Collection<RecipeDescriptor> recipeDescriptors : recipesByGroup.values()) {
            for (RecipeDescriptor r : recipeDescriptors) {
                if (r.getName().substring(r.getName().lastIndexOf('.') + 1).equals(id)) {
                    return r;
                }
            }
        }
        return null;
    }

    @Nullable
    public G getRecipeGroup(String id) {
        if (id.contains(".")) {
            String[] split = id.split("\\.", 2);
            CategoryTree<G> subcategory = getCategory(split[0]);
            return subcategory == null ? null : subcategory.getRecipeGroup(split[1]);
        }
        for (Map.Entry<G, Collection<RecipeDescriptor>> g : recipesByGroup.entrySet()) {
            for (RecipeDescriptor r : g.getValue()) {
                if (r.getName().substring(r.getName().lastIndexOf('.') + 1).equals(id)) {
                    return g.getKey();
                }
            }
        }
        return null;
    }

    CategoryTree<G> findOrAddCategory(G group, CategoryDescriptor category) {
        String packageName = getDescriptor().getPackageName();
        String categoryPackage = category.getPackageName();

        // same category with a potentially different descriptor coming from this group
        if (categoryPackage.equals(packageName)) {
            if (!groups.contains(group)) {
                groups.add(0, group);

                // might be synthetic, but it's the first so add it
                descriptorsByGroup.put(group, category);
            }
            if (!category.isSynthetic()) {
                // replace a potentially synthetic descriptor with a real one
                descriptorsByGroup.put(group, category);
            }
            return this;
        }

        // subcategory of this category
        if (packageName.isEmpty() || (categoryPackage.startsWith(packageName + ".") &&
                                      categoryPackage.charAt(packageName.length()) == '.')) {
            for (CategoryTree<G> subtree : subtrees) {
                String subtreePackage = subtree.getDescriptor().getPackageName();
                if (subtreePackage.equals(categoryPackage) || categoryPackage.startsWith(subtreePackage + ".")) {
                    if (!subtree.groups.contains(group)) {
                        subtree.groups.add(0, group);
                        subtree.descriptorsByGroup.put(group, new CategoryDescriptor(
                                StringUtils.capitalize(subtreePackage.substring(subtreePackage.lastIndexOf('.') + 1)),
                                subtreePackage,
                                "",
                                emptySet(),
                                false,
                                CategoryDescriptor.LOWEST_PRECEDENCE,
                                true
                        ));
                    }
                    return subtree.findOrAddCategory(group, category);
                }
            }

            String subpackage = packageName.isEmpty() ?
                    category.getPackageName() :
                    category.getPackageName().substring(packageName.length() + 1);
            if (subpackage.contains(".")) {
                String displayName = subpackage.substring(0, subpackage.indexOf('.'));

                StringJoiner intermediatePackage = new StringJoiner(".");
                if (!packageName.isEmpty()) {
                    intermediatePackage.add(packageName);
                }
                intermediatePackage.add(displayName);

                return findOrAddCategory(group, new CategoryDescriptor(
                        StringUtils.capitalize(displayName),
                        intermediatePackage.toString(),
                        "",
                        emptySet(),
                        false,
                        CategoryDescriptor.LOWEST_PRECEDENCE,
                        true
                )).findOrAddCategory(group, category);
            }

            // a direct subcategory of this category
            CategoryTree<G> subtree = new CategoryTree<>(group, category);
            subtrees.add(subtree);
            return subtree;
        } else {
            throw new IllegalStateException("Attempted to add a category with package '" +
                                            category.getPackageName() + "' as a subcategory of '" +
                                            packageName + "'. This represents a bug in CategoryTree, as " +
                                            "it should not be possible to add a category to a CategoryTree root " +
                                            "that cannot be placed somewhere in the tree.");
        }
    }

    void addRecipe(G group, RecipeDescriptor recipe) {
        if (!recipe.getName().contains(".")) {
            throw new IllegalArgumentException("Expected recipe with name '" + recipe.getName() + "' to have " +
                                               "a package, but it did not.");
        }
        String category = recipe.getName().substring(0, recipe.getName().lastIndexOf('.'));
        CategoryTree<G> categoryTree = findOrAddCategory(group, new CategoryDescriptor(
                StringUtils.capitalize(category.substring(category.lastIndexOf('.') + 1)),
                category,
                "",
                emptySet(),
                false,
                CategoryDescriptor.LOWEST_PRECEDENCE,
                true
        ));
        categoryTree.recipesByGroup.computeIfAbsent(group, g -> new CopyOnWriteArrayList<>()).add(recipe);
    }

    @SuppressWarnings("UnusedReturnValue")
    CategoryTree<G> removeAll(G group) {
        synchronized (lock) {
            groups.remove(group);
            descriptorsByGroup.remove(group);
            recipesByGroup.remove(group);
            for (CategoryTree<G> subtree : subtrees) {
                subtree.removeAll(group);
            }
            subtrees.removeIf(subtree -> subtree.groups.isEmpty());
        }
        return this;
    }

    public Collection<RecipeDescriptor> getRecipes() {
        synchronized (lock) {
            if (!subtrees.isEmpty()) {
                return emptyList();
            }
            return getRecipesByGroup().values().stream()
                    .flatMap(Collection::stream)
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

    /**
     * @return The subcategories if this category.
     * @deprecated Use {@link #getCategories()} instead.
     */
    @Deprecated
    public Collection<CategoryTree<G>> getSubtrees() {
        return getCategories(true, true);
    }

    public Collection<CategoryTree<G>> getCategories() {
        return getCategories(true, true);
    }

    /**
     * Used to recursively navigate the whole category tree without
     * any advance knowledge of what categories exist.
     *
     * @return The subcategories of this category.
     */
    public Collection<CategoryTree<G>> getCategories(boolean omitCategoryRoots,
                                                     boolean omitEmptyCategories) {
        synchronized (lock) {
            List<CategoryTree<G>> cats = new ArrayList<>(subtrees.size());
            for (CategoryTree<G> subtree : subtrees) {
                if (omitCategoryRoots && subtree.getDescriptor().isRoot()) {
                    cats.addAll(subtree.getCategories());
                } else if (!omitEmptyCategories || !subtree.getRecipes().isEmpty() ||
                           !subtree.getCategories().isEmpty()) {
                    cats.add(subtree);
                }
            }

            if (!subtrees.isEmpty()) {
                CategoryTree<G> core = maybeAddCore(getDescriptor());
                if (core != null) {
                    cats.add(core);
                }
            }
            return cats;
        }
    }

    @Nullable
    private CategoryTree<G> maybeAddCore(CategoryDescriptor parent) {
        if (recipesByGroup.isEmpty()) {
            return null;
        }

        return new CategoryTree<G>() {
            @Override
            public CategoryDescriptor getDescriptor() {
                return new CategoryDescriptor(
                        "Core",
                        parent.getPackageName() + "." + CORE,
                        "",
                        emptySet(),
                        false,
                        CategoryDescriptor.LOWEST_PRECEDENCE,
                        true
                );
            }

            @Override
            public Map<G, Collection<RecipeDescriptor>> getRecipesByGroup() {
                return recipesByGroup;
            }
        };
    }

    @Override
    public String toString() {
        return "CategoryTree{packageName=" + getDescriptor().getPackageName() + "}";
    }

    void toString(StringJoiner out,
                  int level,
                  PrintOptions printOptions,
                  BitSet lastCategoryMask) {
        if (level > printOptions.getMaxDepth()) {
            return;
        }

        CategoryDescriptor descriptor = getDescriptor();
        if (!printOptions.isOmitCategoryRoots() || !descriptor.isRoot()) {
            StringBuilder line = new StringBuilder();
            printTreeLines(line, level, lastCategoryMask);
            if (level > 0) {
                line.append("|-");
            }
            line.append(descriptor.isRoot() ? "√" : "\uD83D\uDCC1");
            String packageName = descriptor.getPackageName().isEmpty() ? "ε" : descriptor.getPackageName();
            switch (printOptions.getNameStyle()) {
                case DISPLAY_NAME:
                    line.append(descriptor.getDisplayName());
                    break;
                case ID:
                    line.append(packageName);
                    break;
                case BOTH:
                    if (descriptor.getPackageName().isEmpty()) {
                        line.append(packageName);
                    } else {
                        line.append(descriptor.getDisplayName()).append(" (").append(packageName).append(')');
                    }
                    break;
            }
            out.add(line);
        }
        Collection<CategoryTree<G>> categories = getCategories(printOptions.isOmitCategoryRoots(),
                printOptions.isOmitEmptyCategories());
        int i = 0;
        for (CategoryTree<G> subtree : categories) {
            if (++i == categories.size()) {
                lastCategoryMask.set(level, true);
            }
            subtree.toString(out,
                    descriptor.isRoot() && printOptions.isOmitCategoryRoots() ? level : level + 1,
                    printOptions, (BitSet) lastCategoryMask.clone());
        }

        lastCategoryMask.set(level, true);
        level++;
        for (RecipeDescriptor recipe : getRecipes()) {
            StringBuilder line = new StringBuilder();
            printTreeLines(line, level, lastCategoryMask);
            line.append("|-\uD83E\uDD16");
            switch (printOptions.getNameStyle()) {
                case DISPLAY_NAME:
                    line.append(recipe.getDisplayName());
                    break;
                case ID:
                    line.append(recipe.getName());
                    break;
                case BOTH:
                    line.append(recipe.getDisplayName()).append(" (").append(recipe.getName()).append(')');
                    break;
            }
            out.add(line);
        }
    }

    private void printTreeLines(StringBuilder line, int level, BitSet lastCategoryMask) {
        for (int i = 0; i < level - 1; i++) {
            if (lastCategoryMask.get(i)) {
                line.append("   ");
            } else {
                line.append("│  ");
            }
        }
    }

    public enum PrintNameStyle {
        DISPLAY_NAME,
        ID,
        BOTH
    }

    @Value
    public static class PrintOptions {
        int maxDepth;
        boolean omitCategoryRoots;
        boolean omitEmptyCategories;
        PrintNameStyle nameStyle;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int maxDepth = Integer.MAX_VALUE;
            private boolean omitCategoryRoots = true;
            private boolean omitEmptyCategories = true;
            private PrintNameStyle nameStyle = ID;

            public Builder maxDepth(int maxDepth) {
                this.maxDepth = maxDepth;
                return this;
            }

            public Builder omitCategoryRoots(boolean omitCategoryRoots) {
                this.omitCategoryRoots = omitCategoryRoots;
                return this;
            }

            public Builder omitEmptyCategories(boolean omitEmptyCategories) {
                this.omitEmptyCategories = omitEmptyCategories;
                return this;
            }

            public Builder nameStyle(PrintNameStyle nameStyle) {
                this.nameStyle = nameStyle;
                return this;
            }

            public PrintOptions build() {
                return new PrintOptions(maxDepth, omitCategoryRoots, omitEmptyCategories, nameStyle);
            }
        }
    }
}

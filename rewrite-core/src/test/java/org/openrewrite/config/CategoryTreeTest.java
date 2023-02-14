/*
 * Copyright 2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;

import java.net.URI;
import java.util.Collection;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.config.CategoryTreeTest.Group.Group1;
import static org.openrewrite.config.CategoryTreeTest.Group.Group2;

public class CategoryTreeTest {
    private final Environment env = Environment.builder()
      .scanRuntimeClasspath()
      .build();

    private final Environment custom = Environment.builder()
      .load(new YamlResourceLoader(
        requireNonNull(CategoryTreeTest.class.getResourceAsStream("/categories.yml")),
        URI.create("custom.yml"),
        new Properties()
      ))
      .build();

    private CategoryTree.Root<Group> categoryTree() {
        return CategoryTree.<Group>build()
          .putAll(Group1, env)
          .putAll(Group2, custom);
    }

    /**
     * Ensure that removing a group doesn't lop off a subtree belonging to another group
     * when they share a common package prefix.
     */
    @Test
    void removingGroupRemovesIntermediateCategory() {
        CategoryTree.Root<Integer> categoryTree = CategoryTree.build();
        categoryTree.putCategories(1, categoryDescriptor("org.openrewrite.java"));
        categoryTree
          .putRecipes(2, recipeDescriptor("org.openrewrite.java.test"))
          .putCategories(2, categoryDescriptor("org.openrewrite.java.test"));

        // this should leave group 2 subpackages of org.openrewrite.java still accessible
        categoryTree.removeAll(1);

        assertThat(categoryTree.getCategory("org.openrewrite.java.test")).isNotNull();
    }

    @Test
    void print() {
        System.out.println(categoryTree().print(CategoryTree.PrintOptions.builder()
          .omitCategoryRoots(false)
          .omitEmptyCategories(false)
          .nameStyle(CategoryTree.PrintNameStyle.DISPLAY_NAME)
          .build()));
    }

    @Test
    void categoryDescriptorPrecedence() {
        CategoryDescriptor descriptor = categoryDescriptor("org.openrewrite.test");
        CategoryTree<Integer> categoryTree = CategoryTree.<Integer>build()
          .putRecipes(0, recipeDescriptor("org.openrewrite.test"))
          .putCategories(1, descriptor)
          .putCategories(2, descriptor.withDisplayName("new display name"));

        CategoryTree<Integer> found = requireNonNull(categoryTree.getCategory("org.openrewrite.test"));
        assertThat(found.getDescriptor().getDisplayName()).isEqualTo("new display name");
        assertThat(found.getRecipes()).hasSize(1);

        categoryTree.removeAll(2);
        found = requireNonNull(categoryTree.getCategory("org.openrewrite.test"));
        assertThat(found.getDescriptor().getDisplayName()).isEqualTo("Test");
        assertThat(found.getRecipes()).hasSize(1);
    }

    @Test
    void getCategoryThatIsTransitivelyEmpty() {
        CategoryTree<Integer> categoryTree = CategoryTree.<Integer>build()
          .putCategories(1, categoryDescriptor("org.openrewrite.test"));

        assertThat(categoryTree.getCategory("org", "openrewrite")).isNull();
        assertThat(categoryTree.getCategory("org", "openrewrite", "test")).isNull();
    }

    @Test
    void putRecipe() {
        CategoryTree<Integer> categoryTree = CategoryTree.<Integer>build()
          .putRecipes(1, recipeDescriptor("org.openrewrite"));
        assertThat(categoryTree.getRecipe("org.openrewrite.MyRecipe")).isNotNull();
    }

    @NonNull
    private static RecipeDescriptor recipeDescriptor(String packageName) {
        return new RecipeDescriptor(packageName + ".MyRecipe",
          "My recipe", "", emptySet(), null, emptyList(),
          emptyList(), emptyList(), emptyList(), URI.create("https://openrewrite.org"));
    }

    @Test
    void removeCategory() {
        CategoryTree.Root<Group> ct = categoryTree();
        ct.removeAll(Group2);
        assertThat(ct.getCategories().stream().map(sub -> sub.getDescriptor().getPackageName()))
          .doesNotContain("io.moderne.rewrite", "io.moderne.cloud", "io.moderne");
    }

    @Test
    void categoryRoots() {
        CategoryTree.Root<Group> ct = categoryTree();
        assertThat(ct.getCategories().stream().map(sub -> sub.getDescriptor().getPackageName()))
                .contains(
                        "io.moderne.rewrite", "io.moderne.cloud", // because "io.moderne" is marked as a root
                        "org.openrewrite" // because "org" is marked as a root
                );
    }

    @Test
    void getCategory() {
        CategoryTree.Root<Group> ct = categoryTree();
        assertThat(ct.getCategoryOrThrow("org", "openrewrite")).isNotNull();
        assertThat(ct.getCategoryOrThrow("org.openrewrite")).isNotNull();
        assertThat(ct.getCategoryOrThrow("org.openrewrite", "test")).isNotNull();
        assertThat(ct.getCategoryOrThrow("org.openrewrite.test")).isNotNull();
    }

    @Test
    void getRecipeCount() {
        assertThat(categoryTree().getCategoryOrThrow("org", "openrewrite").getRecipeCount())
          .isGreaterThan(5);
    }

    @Test
    void getRecipes() {
        assertThat(categoryTree().getCategoryOrThrow("org.openrewrite.text").getRecipes().size())
          .isGreaterThan(1);
    }

    @Test
    void getRecipesInArtificialCorePackage() {
        Collection<RecipeDescriptor> recipes = requireNonNull(categoryTree().getCategory("org", "openrewrite", "core")).getRecipes();
        assertThat(recipes).isNotEmpty();
    }

    @Test
    void getRecipe() {
        assertThat(categoryTree().getRecipe("org.openrewrite.DeleteSourceFiles")).isNotNull();
    }

    @Test
    void getRecipeGroup() {
        assertThat(categoryTree().getRecipeGroup("org.openrewrite.DeleteSourceFiles"))
          .isEqualTo(Group1);
    }

    private static CategoryDescriptor categoryDescriptor(String packageName) {
        return new CategoryDescriptor(StringUtils.capitalize(packageName.substring(packageName.lastIndexOf('.') + 1)),
          packageName, "", emptySet(),
          false, CategoryDescriptor.DEFAULT_PRECEDENCE, false);
    }

    enum Group {
        Group1,
        Group2
    }
}

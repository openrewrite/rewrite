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

import org.junit.jupiter.api.Test;
import org.openrewrite.config.CategoryDescriptor;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class YamlRecipeBundleReaderTest {
    private static final String YAML = """
      type: specs.openrewrite.org/v1beta/recipe
      name: com.example.text.FindAndReplace
      displayName: Find and replace
      description: Finds and replaces.
      recipeList:
        - org.openrewrite.text.Find:
            find: hello
      """;

    private static RecipeMarketplace read(List<CategoryDescriptor> categoryOverride) {
        RecipeBundle bundle = new RecipeBundle("yaml", "/recipes/example.yml", null, null, null);
        return new YamlRecipeBundleReader(bundle,
                new ByteArrayInputStream(YAML.getBytes(StandardCharsets.UTF_8)),
                URI.create("file:///recipes/example.yml"),
                new Properties(), new RecipeMarketplace(), emptyList(), categoryOverride).read();
    }

    private static CategoryDescriptor category(String name) {
        return new CategoryDescriptor(name, "", "", emptySet(), false, 0, false);
    }

    private static List<String> pathTo(RecipeMarketplace marketplace, String recipeName) {
        return pathTo(marketplace.getRoot(), recipeName, emptyList());
    }

    private static List<String> pathTo(RecipeMarketplace.Category category, String recipeName, List<String> soFar) {
        for (RecipeListing listing : category.getRecipes()) {
            if (listing.getName().equals(recipeName)) {
                return soFar;
            }
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            List<String> deeper = new java.util.ArrayList<>(soFar);
            deeper.add(child.getDisplayName());
            List<String> found = pathTo(child, recipeName, deeper);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Test
    void withoutAnOverrideTheCategoryIsInferredFromTheRecipeName() {
        RecipeMarketplace marketplace = read(emptyList());

        assertThat(marketplace.findRecipe("com.example.text.FindAndReplace")).isNotNull();
        assertThat(pathTo(marketplace, "com.example.text.FindAndReplace"))
                .as("the path comes from the recipe's own package, not from the caller")
                .isNotEmpty();
    }

    @Test
    void anOverrideReplacesTheInferredCategoryPath() {
        RecipeMarketplace marketplace = read(List.of(category("Custom"), category("Nested")));

        assertThat(pathTo(marketplace, "com.example.text.FindAndReplace"))
                .as("the override is a path, shallowest to deepest, and supersedes the inferred one")
                .containsExactly("Custom", "Nested");
    }
}

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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeMarketplaceReaderTest {

    @Test
    void readMinimalMarketplace() {
        @Language("csv") String csv = """
          name,category1,category2
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java
          org.openrewrite.java.format.AutoFormat,Formatting,Java
          org.openrewrite.maven.UpgradeDependencyVersion,Dependencies,Maven
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeMarketplace.Category java = findCategory(marketplace.getRoot(), "Java");
        assertThat(java.getCategories()).hasSize(2);

        RecipeMarketplace.Category cleanup = findCategory(java, "Cleanup");
        assertThat(cleanup.getRecipes())
          .singleElement()
          .extracting("name", "bundle")
          .containsExactly("org.openrewrite.java.cleanup.UnnecessaryParentheses", null);

        RecipeMarketplace.Category formatting = findCategory(java, "Formatting");
        assertThat(formatting.getRecipes())
          .singleElement()
          .extracting("name")
          .isEqualTo("org.openrewrite.java.format.AutoFormat");
    }

    @Test
    void readMarketplaceWithBundleInfo() {
        @Language("csv") String csv = """
          name,displayName,description,category,ecosystem,packageName,version,team
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses,Java Cleanup,Maven,org.openrewrite:rewrite-java,8.0.0,java-team
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        assertThat(marketplace.getCategories().getFirst().getDisplayName()).isEqualTo("Java Cleanup");
        RecipeListing listing = marketplace.getAllRecipes().iterator().next();
        assertThat(listing.getName()).isEqualTo("org.openrewrite.java.cleanup.UnnecessaryParentheses");
        assertThat(listing.getDisplayName()).isEqualTo("Remove Unnecessary Parentheses");
        assertThat(listing.getDescription()).isEqualTo("Removes unnecessary parentheses");

        // Bundle should be created if a RecipeBundleLoader is registered for Maven
        // In test environment without ServiceLoader, bundle will be null
        RecipeBundle bundle = listing.getBundle();
        assertThat(bundle.getPackageEcosystem()).isEqualTo("Maven");
        assertThat(bundle.getPackageName()).isEqualTo("org.openrewrite:rewrite-java");
        assertThat(bundle.getVersion()).isEqualTo("8.0.0");
        assertThat(bundle.getTeam()).isEqualTo("java-team");
    }

    @Test
    void readMarketplaceWithOptions() {
        @Language("csv") String csv = """
          name,displayName,option1Name,option1DisplayName,option1Description,option2Name,option2DisplayName,option2Description,category
          org.openrewrite.maven.UpgradeDependencyVersion,Upgrade Dependency,groupId,Group ID,The group ID of the dependency,artifactId,Artifact ID,The artifact ID of the dependency,Maven
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeListing recipe = marketplace.getAllRecipes().iterator().next();
        assertThat(recipe)
          .extracting("name", "displayName")
          .containsExactly("org.openrewrite.maven.UpgradeDependencyVersion", "Upgrade Dependency");

        assertThat(recipe.getOptions())
          .hasSize(2)
          .extracting("name", "displayName", "description")
          .containsExactly(
            org.assertj.core.api.Assertions.tuple("groupId", "Group ID", "The group ID of the dependency"),
            org.assertj.core.api.Assertions.tuple("artifactId", "Artifact ID", "The artifact ID of the dependency")
          );
    }

    @Test
    void readMarketplaceWithMultipleLevelCategories() {
        @Language("csv") String csv = """
          name,category1,category2,category3
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,Best Practices
          org.openrewrite.java.cleanup.RemoveUnusedImports,Cleanup,Java,Best Practices
          org.openrewrite.java.format.AutoFormat,Formatting,Java,Best Practices
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        // Left is deepest: category1 = Cleanup/Formatting (deepest), category2 = Java, category3 = Best Practices (root)
        RecipeMarketplace.Category root = marketplace.getCategories().getFirst();
        assertThat(root.getDisplayName()).isEqualTo("Best Practices");

        RecipeMarketplace.Category java = root.getCategories().getFirst();
        assertThat(java.getDisplayName()).isEqualTo("Java");
        assertThat(java.getCategories()).hasSize(2);

        assertThat(findCategory(java, "Cleanup").getRecipes()).hasSize(2);
        assertThat(findCategory(java, "Formatting").getRecipes()).hasSize(1);
    }

    @Test
    void readMarketplaceWithoutCategories() {
        @Language("csv") String csv = """
          name,displayName,description
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Parentheses,Remove unnecessary parentheses
          org.openrewrite.java.format.AutoFormat,Auto Format,Automatically format code
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        assertThat(marketplace.getAllRecipes()).hasSize(2);
        assertThat(marketplace.getCategories()).isEmpty();
    }

    private static RecipeMarketplace.Category findCategory(RecipeMarketplace.Category category, String name) {
        return category.getCategories().stream()
          .filter(c -> c.getDisplayName().equals(name))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Category not found: " + name));
    }
}

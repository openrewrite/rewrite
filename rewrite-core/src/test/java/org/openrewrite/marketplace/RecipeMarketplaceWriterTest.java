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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class RecipeMarketplaceWriterTest {

    @Test
    void roundTripMinimalMarketplace() {
        // Create marketplace with two categories under Java
        RecipeMarketplace marketplace = new RecipeMarketplace("Java", "Java recipes");
        marketplace.getCategories().add(createCategory("Cleanup",
                createRecipe("org.openrewrite.java.cleanup.UnnecessaryParentheses")));
        marketplace.getCategories().add(createCategory("Formatting",
                createRecipe("org.openrewrite.java.format.AutoFormat")));

        @Language("csv") String csv = new RecipeMarketplaceWriter().toCsv(marketplace);

        RecipeMarketplace result = new RecipeMarketplaceReader().fromCsv(csv);

        // Should return Java (single top-level category)
        assertThat(result.getDisplayName()).isEqualTo("Java");
        assertThat(result.getCategories()).hasSize(2);
        assertThat(findCategory(result, "Cleanup").getRecipes()).hasSize(1);
        assertThat(findCategory(result, "Formatting").getRecipes()).hasSize(1);
    }

    @Test
    void roundTripWithDisplayNameAndDescription() {
        RecipeMarketplace marketplace = new RecipeMarketplace("Java cleanup", "");
        marketplace.getRecipes().add(new RecipeOffering(
                "org.openrewrite.java.cleanup.UnnecessaryParentheses",
          "Remove unnecessary parentheses",
          "Org.openrewrite.java.cleanup.unnecessaryParentheses",
                "Removes unnecessary parentheses from expressions",
                emptySet(),
                null,
                emptyList(),
                null
        ));

        @Language("csv") String csv = new RecipeMarketplaceWriter().toCsv(marketplace);

        RecipeMarketplace result = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeOffering recipe = (RecipeOffering) result.getRecipes().getFirst();
        assertThat(recipe.getName()).isEqualTo("org.openrewrite.java.cleanup.UnnecessaryParentheses");
        assertThat(recipe.getDisplayName()).isEqualTo("Remove unnecessary parentheses");
        assertThat(recipe.getDescription()).isEqualTo("Removes unnecessary parentheses from expressions");
    }

    @Test
    void roundTripWithOptions() {
        RecipeMarketplace marketplace = new RecipeMarketplace("Maven", "");
        marketplace.getRecipes().add(new RecipeOffering(
                "org.openrewrite.maven.UpgradeDependencyVersion",
          "Upgrade dependency",
          "Org.openrewrite.maven.upgradeDependencyVersion",
                "Upgrades a Maven dependency",
                emptySet(),
                null,
                java.util.List.of(
                        new RecipeOffering.Option("groupId", "Group ID", "The group ID"),
                        new RecipeOffering.Option("artifactId", "Artifact ID", "The artifact ID")
                ),
                null
        ));

        @Language("csv") String csv = new RecipeMarketplaceWriter().toCsv(marketplace);

        RecipeMarketplace result = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeOffering recipe = (RecipeOffering) result.getRecipes().getFirst();
        assertThat(recipe.getOptions())
                .hasSize(2)
                .extracting("name", "displayName", "description")
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("groupId", "Group ID", "The group ID"),
                        org.assertj.core.api.Assertions.tuple("artifactId", "Artifact ID", "The artifact ID")
                );
    }

    @Test
    void roundTripMultipleLevelCategories() {
        // Create: Best Practices > Java > Cleanup > recipes
        RecipeMarketplace bestPractices = new RecipeMarketplace("Best practices", "");
        RecipeMarketplace java = new RecipeMarketplace("Java", "");
        RecipeMarketplace cleanup = new RecipeMarketplace("Cleanup", "");

        cleanup.getRecipes().add(createRecipe("org.openrewrite.java.cleanup.UnnecessaryParentheses"));
        cleanup.getRecipes().add(createRecipe("org.openrewrite.java.cleanup.RemoveUnusedImports"));

        java.getCategories().add(cleanup);
        bestPractices.getCategories().add(java);

        @Language("csv") String csv = new RecipeMarketplaceWriter().toCsv(bestPractices);

        RecipeMarketplace result = new RecipeMarketplaceReader().fromCsv(csv);

        assertThat(result.getDisplayName()).isEqualTo("Best practices");
        RecipeMarketplace resultJava = result.getCategories().getFirst();
        assertThat(resultJava.getDisplayName()).isEqualTo("Java");
        RecipeMarketplace resultCleanup = resultJava.getCategories().getFirst();
        assertThat(resultCleanup.getDisplayName()).isEqualTo("Cleanup");
        assertThat(resultCleanup.getRecipes()).hasSize(2);
    }

    @Test
    void writeCsvWithCorrectCategoryOrder() {
        // Create: Best Practices > Java > Cleanup > recipe
        RecipeMarketplace bestPractices = new RecipeMarketplace("Best practices", "");
        RecipeMarketplace java = new RecipeMarketplace("Java", "");
        RecipeMarketplace cleanup = new RecipeMarketplace("Cleanup", "");

        cleanup.getRecipes().add(createRecipe("org.openrewrite.java.cleanup.UnnecessaryParentheses"));
        java.getCategories().add(cleanup);
        bestPractices.getCategories().add(java);

        String csv = new RecipeMarketplaceWriter().toCsv(bestPractices);

        // CSV should have category1,category2,category3 where left is deepest
        // So: category1=Cleanup, category2=Java, category3=Best practices
        assertThat(csv).contains("category1,category2,category3");
        assertThat(csv).contains("Cleanup,Java,Best practices");
    }

    @Test
    void roundTripWithBundleInfo() {
        RecipeMarketplace marketplace = new RecipeMarketplace("Java", "");
        marketplace.getRecipes().add(new RecipeOffering(
                "org.openrewrite.java.cleanup.UnnecessaryParentheses",
                "Remove unnecessary parentheses",
                "Remove unnecessary parentheses",
                "Removes unnecessary parentheses around things",
                emptySet(),
                null,
                emptyList(),
                new FakeMavenBundle("Maven", "org.openrewrite:rewrite-java", "8.0.0", "java-team")
        ));

        @Language("csv") String csv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Verify CSV contains bundle columns in the right order
        // ecosystem, packageName, version should be first 3 columns
        assertThat(csv).contains("ecosystem,packageName,version,name");
        // team should be at the end
        assertThat(csv).contains(",team\n");
        // Verify data row has correct values
        assertThat(csv).contains("Maven,org.openrewrite:rewrite-java,8.0.0,");
        assertThat(csv).contains(",java-team\n");
    }

    private static RecipeMarketplace createCategory(String name, RecipeOffering... recipes) {
        RecipeMarketplace category = new RecipeMarketplace(name, "");
        for (RecipeOffering recipe : recipes) {
            category.getRecipes().add(recipe);
        }
        return category;
    }

    private static RecipeOffering createRecipe(String name) {
        return new RecipeOffering(
                name,
                name,
                name,
                "",
                emptySet(),
                null,
                emptyList(),
                null
        );
    }

    private static RecipeMarketplace findCategory(RecipeMarketplace marketplace, String name) {
        return marketplace.getCategories().stream()
                .filter(c -> c.getDisplayName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Category not found: " + name));
    }

    private record FakeMavenBundle(String ecosystem, String getPackageName, String getVersion,
                                   String getTeam) implements RecipeBundle {

        @Override
            public String getPackageEcosystem() {
                return ecosystem;
            }

            @Override
            public org.openrewrite.config.RecipeDescriptor describe(RecipeListing listing) {
                throw new UnsupportedOperationException();
            }

            @Override
            public org.openrewrite.Recipe prepare(RecipeListing listing, java.util.Map<String, Object> options) {
                throw new UnsupportedOperationException();
            }
        }
}

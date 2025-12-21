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
import org.openrewrite.config.DataTableDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeMarketplaceReaderTest {

    @Test
    void readMinimalMarketplace() {
        @Language("csv") String csv = """
          name,category1,category2,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.format.AutoFormat,Formatting,Java,maven,org.openrewrite:rewrite-java
          org.openrewrite.maven.UpgradeDependencyVersion,Dependencies,Maven,maven,org.openrewrite:rewrite-maven
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeMarketplace.Category java = findCategory(marketplace.getRoot(), "Java");
        assertThat(java.getCategories()).hasSize(2);

        RecipeMarketplace.Category cleanup = findCategory(java, "Cleanup");
        assertThat(cleanup.getRecipes())
          .singleElement()
          .extracting("name")
          .isEqualTo("org.openrewrite.java.cleanup.UnnecessaryParentheses");

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

        // Bundle is created from CSV data containing ecosystem and packageName
        RecipeBundle bundle = listing.getBundle();
        assertThat(bundle.getPackageEcosystem()).isEqualTo("maven");
        assertThat(bundle.getPackageName()).isEqualTo("org.openrewrite:rewrite-java");
        assertThat(bundle.getVersion()).isEqualTo("8.0.0");
        assertThat(bundle.getTeam()).isEqualTo("java-team");
    }

    @Test
    void readMarketplaceWithOptions() {
        @Language("csv") String csv = """
          name,displayName,options,category,ecosystem,packageName
          org.openrewrite.maven.UpgradeDependencyVersion,Upgrade Dependency,"[{""name"":""groupId"",""type"":""String"",""displayName"":""Group ID"",""description"":""The group ID of the dependency"",""required"":true,""example"":""org.openrewrite""},{""name"":""artifactId"",""type"":""String"",""displayName"":""Artifact ID"",""description"":""The artifact ID of the dependency"",""required"":false}]",Maven,maven,org.openrewrite:rewrite-maven
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeListing recipe = marketplace.getAllRecipes().iterator().next();
        assertThat(recipe)
          .extracting("name", "displayName")
          .containsExactly("org.openrewrite.maven.UpgradeDependencyVersion", "Upgrade Dependency");

        assertThat(recipe.getOptions())
          .hasSize(2)
          .extracting("name", "displayName", "description", "required", "example")
          .containsExactlyInAnyOrder(
            org.assertj.core.api.Assertions.tuple("groupId", "Group ID", "The group ID of the dependency", true, "org.openrewrite"),
            org.assertj.core.api.Assertions.tuple("artifactId", "Artifact ID", "The artifact ID of the dependency", false, null)
          );
    }

    @Test
    void readMarketplaceWithDataTables() {
        @Language("csv") String csv = """
          name,displayName,dataTables,category,ecosystem,packageName
          org.openrewrite.java.dependencies.DependencyList,List Dependencies,"[{""name"":""org.openrewrite.java.dependencies.DependencyListTable"",""displayName"":""Dependencies"",""description"":""Lists all dependencies found in the project""},{""name"":""org.openrewrite.java.dependencies.RepositoryTable"",""displayName"":""Repositories"",""description"":""Lists all repositories""}]",Java,maven,org.openrewrite:rewrite-java
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeListing recipe = marketplace.getAllRecipes().iterator().next();
        assertThat(recipe)
          .extracting("name", "displayName")
          .containsExactly("org.openrewrite.java.dependencies.DependencyList", "List Dependencies");

        assertThat(recipe.getDataTables())
          .hasSize(2)
          .extracting("name", "displayName", "description")
          .containsExactly(
            org.assertj.core.api.Assertions.tuple("org.openrewrite.java.dependencies.DependencyListTable", "Dependencies", "Lists all dependencies found in the project"),
            org.assertj.core.api.Assertions.tuple("org.openrewrite.java.dependencies.RepositoryTable", "Repositories", "Lists all repositories")
          );
    }

    @Test
    void readMarketplaceWithDataTableColumns() {
        @Language("csv") String csv = """
          name,displayName,dataTables,category,ecosystem,packageName
          org.openrewrite.java.dependencies.DependencyList,List Dependencies,"[{""name"":""org.openrewrite.java.dependencies.DependencyListTable"",""displayName"":""Dependencies"",""description"":""Lists all dependencies"",""columns"":[{""name"":""groupId"",""type"":""String"",""displayName"":""Group ID"",""description"":""The dependency group""},{""name"":""artifactId"",""type"":""String"",""displayName"":""Artifact ID""}]}]",Java,maven,org.openrewrite:rewrite-java
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeListing recipe = marketplace.getAllRecipes().iterator().next();
        assertThat(recipe.getDataTables()).hasSize(1);

        DataTableDescriptor dataTable = recipe.getDataTables().get(0);
        assertThat(dataTable.getName()).isEqualTo("org.openrewrite.java.dependencies.DependencyListTable");
        assertThat(dataTable.getColumns())
          .hasSize(2)
          .extracting("name", "type", "displayName", "description")
          .containsExactlyInAnyOrder(
            org.assertj.core.api.Assertions.tuple("groupId", "String", "Group ID", "The dependency group"),
            org.assertj.core.api.Assertions.tuple("artifactId", "String", "Artifact ID", null)
          );
    }

    @Test
    void readMarketplaceWithMultipleLevelCategories() {
        @Language("csv") String csv = """
          name,category1,category2,category3,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,Best Practices,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.cleanup.RemoveUnusedImports,Cleanup,Java,Best Practices,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.format.AutoFormat,Formatting,Java,Best Practices,maven,org.openrewrite:rewrite-java
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
          name,displayName,description,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Parentheses,Remove unnecessary parentheses,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.format.AutoFormat,Auto Format,Automatically format code,maven,org.openrewrite:rewrite-java
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        assertThat(marketplace.getAllRecipes()).hasSize(2);
        assertThat(marketplace.getCategories()).isEmpty();
    }

    @Test
    void readMarketplaceWithCategoryDescriptions() {
        @Language("csv") String csv = """
          name,category1,category1Description,category2,category2Description,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Remove redundant code,Java,Java-related recipes,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.format.AutoFormat,Formatting,Auto-format your code,Java,Java-related recipes,maven,org.openrewrite:rewrite-java
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        // category2 = Java (root level), category1 = Cleanup/Formatting (deeper)
        RecipeMarketplace.Category java = findCategory(marketplace.getRoot(), "Java");
        assertThat(java.getDescription()).isEqualTo("Java-related recipes");

        RecipeMarketplace.Category cleanup = findCategory(java, "Cleanup");
        assertThat(cleanup.getDescription()).isEqualTo("Remove redundant code");

        RecipeMarketplace.Category formatting = findCategory(java, "Formatting");
        assertThat(formatting.getDescription()).isEqualTo("Auto-format your code");
    }

    @Test
    void roundTripWithCategoryDescriptions() {
        @Language("csv") String csv = """
          name,category1,category1Description,category2,category2Description,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Remove redundant code,Java,Java-related recipes,maven,org.openrewrite:rewrite-java
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Parse the written CSV again
        RecipeMarketplace roundTripped = new RecipeMarketplaceReader().fromCsv(writtenCsv);

        RecipeMarketplace.Category java = findCategory(roundTripped.getRoot(), "Java");
        assertThat(java.getDescription()).isEqualTo("Java-related recipes");

        RecipeMarketplace.Category cleanup = findCategory(java, "Cleanup");
        assertThat(cleanup.getDescription()).isEqualTo("Remove redundant code");
    }

    @Test
    void writerOmitsCategoryDescriptionsWhenNonePresent() {
        @Language("csv") String csv = """
          name,category1,category2,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,maven,org.openrewrite:rewrite-java
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Should not contain "Description" headers since no descriptions were provided
        assertThat(writtenCsv).doesNotContain("Description");
    }

    @Test
    void readMarketplaceWithMetadata() {
        @Language("csv") String csv = """
          name,category,ecosystem,packageName,author,recipeCount
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Java,maven,org.openrewrite:rewrite-java,Jon Schneider,42
          org.openrewrite.java.format.AutoFormat,Java,maven,org.openrewrite:rewrite-java,Jon Schneider,
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeListing first = marketplace.getAllRecipes().stream()
          .filter(r -> r.getName().equals("org.openrewrite.java.cleanup.UnnecessaryParentheses"))
          .findFirst()
          .orElseThrow();
        assertThat(first.getMetadata())
          .containsEntry("author", "Jon Schneider")
          .containsEntry("recipeCount", "42");

        RecipeListing second = marketplace.getAllRecipes().stream()
          .filter(r -> r.getName().equals("org.openrewrite.java.format.AutoFormat"))
          .findFirst()
          .orElseThrow();
        // Empty values should not be included in metadata
        assertThat(second.getMetadata())
          .containsEntry("author", "Jon Schneider")
          .doesNotContainKey("recipeCount");
    }

    @Test
    void roundTripWithMetadata() {
        @Language("csv") String csv = """
          name,category,ecosystem,packageName,author,recipeCount
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Java,maven,org.openrewrite:rewrite-java,Jon Schneider,42
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Parse the written CSV again
        RecipeMarketplace roundTripped = new RecipeMarketplaceReader().fromCsv(writtenCsv);

        RecipeListing listing = roundTripped.getAllRecipes().iterator().next();
        assertThat(listing.getMetadata())
          .containsEntry("author", "Jon Schneider")
          .containsEntry("recipeCount", "42");
    }

    @Test
    void writerOmitsMetadataColumnsWhenNonePresent() {
        @Language("csv") String csv = """
          name,category,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Java,maven,org.openrewrite:rewrite-java
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Should only contain known headers
        assertThat(writtenCsv.lines().findFirst().orElseThrow())
          .contains("ecosystem", "packageName", "name", "displayName", "description", "category")
          .doesNotContain("author", "recipeCount");
    }

    @Test
    void metadataColumnsAreSortedAlphabetically() {
        @Language("csv") String csv = """
          name,category,ecosystem,packageName,recipeCount,author
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Java,maven,org.openrewrite:rewrite-java,42,Jon Schneider
          """;

        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(csv);
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Metadata columns should be sorted alphabetically (author before recipeCount)
        String header = writtenCsv.lines().findFirst().orElseThrow();
        int authorIndex = header.indexOf("author");
        int recipeCountIndex = header.indexOf("recipeCount");

        assertThat(authorIndex).isLessThan(recipeCountIndex);
    }

    private static RecipeMarketplace.Category findCategory(RecipeMarketplace.Category category, String name) {
        return category.getCategories().stream()
          .filter(c -> c.getDisplayName().equals(name))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Category not found: " + name));
    }
}

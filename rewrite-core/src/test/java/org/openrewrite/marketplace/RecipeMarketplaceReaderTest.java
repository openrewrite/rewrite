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
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category1,category2,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.format.AutoFormat,Formatting,Java,maven,org.openrewrite:rewrite-java
          org.openrewrite.maven.UpgradeDependencyVersion,Dependencies,Maven,maven,org.openrewrite:rewrite-maven
          """);

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
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName,requestedVersion,version,team
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses,Java Cleanup,Maven,org.openrewrite:rewrite-java,LATEST,8.0.0,java-team
          """);

        assertThat(marketplace.getCategories().getFirst().getDisplayName()).isEqualTo("Java Cleanup");
        RecipeListing listing = marketplace.getAllRecipes().iterator().next();
        assertThat(listing.getName()).isEqualTo("org.openrewrite.java.cleanup.UnnecessaryParentheses");
        assertThat(listing.getDisplayName()).isEqualTo("Remove Unnecessary Parentheses");
        assertThat(listing.getDescription()).isEqualTo("Removes unnecessary parentheses");

        // Bundle is created from CSV data containing ecosystem and packageName
        RecipeBundle bundle = listing.getBundle();
        assertThat(bundle.getPackageEcosystem()).isEqualTo("maven");
        assertThat(bundle.getPackageName()).isEqualTo("org.openrewrite:rewrite-java");
        assertThat(bundle.getRequestedVersion()).isEqualTo("LATEST");
        assertThat(bundle.getVersion()).isEqualTo("8.0.0");
        assertThat(bundle.getTeam()).isEqualTo("java-team");
    }

    @Test
    void readMarketplaceWithOptions() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,options,category,ecosystem,packageName
          org.openrewrite.maven.UpgradeDependencyVersion,Upgrade Dependency,"[{""name"":""groupId"",""type"":""String"",""displayName"":""Group ID"",""description"":""The group ID of the dependency"",""required"":true,""example"":""org.openrewrite""},{""name"":""artifactId"",""type"":""String"",""displayName"":""Artifact ID"",""description"":""The artifact ID of the dependency"",""required"":false}]",Maven,maven,org.openrewrite:rewrite-maven
          """);

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
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,dataTables,category,ecosystem,packageName
          org.openrewrite.java.dependencies.DependencyList,List Dependencies,"[{""name"":""org.openrewrite.java.dependencies.DependencyListTable"",""displayName"":""Dependencies"",""description"":""Lists all dependencies found in the project""},{""name"":""org.openrewrite.java.dependencies.RepositoryTable"",""displayName"":""Repositories"",""description"":""Lists all repositories""}]",Java,maven,org.openrewrite:rewrite-java
          """);

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
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,dataTables,category,ecosystem,packageName
          org.openrewrite.java.dependencies.DependencyList,List Dependencies,"[{""name"":""org.openrewrite.java.dependencies.DependencyListTable"",""displayName"":""Dependencies"",""description"":""Lists all dependencies"",""columns"":[{""name"":""groupId"",""type"":""String"",""displayName"":""Group ID"",""description"":""The dependency group""},{""name"":""artifactId"",""type"":""String"",""displayName"":""Artifact ID""}]}]",Java,maven,org.openrewrite:rewrite-java
          """);

        RecipeListing recipe = marketplace.getAllRecipes().iterator().next();
        assertThat(recipe.getDataTables()).hasSize(1);

        DataTableDescriptor dataTable = recipe.getDataTables().getFirst();
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
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category1,category2,category3,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,Best Practices,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.cleanup.RemoveUnusedImports,Cleanup,Java,Best Practices,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.format.AutoFormat,Formatting,Java,Best Practices,maven,org.openrewrite:rewrite-java
          """);

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
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Parentheses,Remove unnecessary parentheses,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.format.AutoFormat,Auto Format,Automatically format code,maven,org.openrewrite:rewrite-java
          """);

        assertThat(marketplace.getAllRecipes()).hasSize(2);
        assertThat(marketplace.getCategories()).isEmpty();
    }

    @Test
    void readMarketplaceWithCategoryDescriptions() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category1,category1Description,category2,category2Description,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Remove redundant code,Java,Java-related recipes,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.format.AutoFormat,Formatting,Auto-format your code,Java,Java-related recipes,maven,org.openrewrite:rewrite-java
          """);

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
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category1,category1Description,category2,category2Description,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Remove redundant code,Java,Java-related recipes,maven,org.openrewrite:rewrite-java
          """);
        @Language("csv") String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Parse the written CSV again
        RecipeMarketplace roundTripped = new RecipeMarketplaceReader().fromCsv(writtenCsv);

        RecipeMarketplace.Category java = findCategory(roundTripped.getRoot(), "Java");
        assertThat(java.getDescription()).isEqualTo("Java-related recipes");

        RecipeMarketplace.Category cleanup = findCategory(java, "Cleanup");
        assertThat(cleanup.getDescription()).isEqualTo("Remove redundant code");
    }

    @Test
    void writerOmitsCategoryDescriptionsWhenNonePresent() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category1,category2,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,maven,org.openrewrite:rewrite-java
          """);
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Should not contain "Description" headers since no descriptions were provided
        assertThat(writtenCsv).doesNotContain("Description");
    }

    @Test
    void readMarketplaceWithMetadata() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category,ecosystem,packageName,author,recipeCount
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Java,maven,org.openrewrite:rewrite-java,Jon Schneider,42
          org.openrewrite.java.format.AutoFormat,Java,maven,org.openrewrite:rewrite-java,Jon Schneider,
          """);

        RecipeListing first = marketplace.getAllRecipes().stream()
          .filter(r -> r.getName().equals("org.openrewrite.java.cleanup.UnnecessaryParentheses"))
          .findFirst()
          .orElseThrow();
        assertThat(first.getMetadata())
          .containsEntry("author", "Jon Schneider");
        assertThat(first.getRecipeCount()).isEqualTo(42);

        RecipeListing second = marketplace.getAllRecipes().stream()
          .filter(r -> r.getName().equals("org.openrewrite.java.format.AutoFormat"))
          .findFirst()
          .orElseThrow();
        assertThat(second.getMetadata())
          .containsEntry("author", "Jon Schneider");
        // recipeCount defaults to 1 when not specified
        assertThat(second.getRecipeCount()).isEqualTo(1);
    }

    @Test
    void roundTripWithMetadata() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category,ecosystem,packageName,author,recipeCount
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Java,maven,org.openrewrite:rewrite-java,Jon Schneider,42
          """);
        @Language("csv") String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Parse the written CSV again
        RecipeMarketplace roundTripped = new RecipeMarketplaceReader().fromCsv(writtenCsv);

        RecipeListing listing = roundTripped.getAllRecipes().iterator().next();
        assertThat(listing.getMetadata())
          .containsEntry("author", "Jon Schneider");
        assertThat(listing.getRecipeCount()).isEqualTo(42);
    }

    @Test
    void writerOmitsMetadataColumnsWhenNonePresent() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Java,maven,org.openrewrite:rewrite-java
          """);
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Should contain known headers including recipeCount (now a regular field)
        assertThat(writtenCsv.lines().findFirst().orElseThrow())
          .contains("ecosystem", "packageName", "name", "displayName", "description", "recipeCount", "category")
          .doesNotContain("author");
    }

    @Test
    void metadataColumnsAreSortedAlphabetically() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,category,ecosystem,packageName,recipeCount,zebra,author
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Java,maven,org.openrewrite:rewrite-java,42,value,Jon Schneider
          """);
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        // Metadata columns should be sorted alphabetically (author before zebra)
        String header = writtenCsv.lines().findFirst().orElseThrow();
        int authorIndex = header.indexOf("author");
        int zebraIndex = header.indexOf("zebra");

        assertThat(authorIndex).isLessThan(zebraIndex);
    }

    @Test
    void recipeInMultipleCategoriesHasSeparateBundleInstances() {
        // This tests the scenario where a recipe appears in multiple categories
        // (e.g., Java recipes that also work for JavaScript). Each listing should
        // have its own RecipeBundle instance that needs to be updated independently.
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,category1,category2,ecosystem,packageName
          org.openrewrite.java.ChangeMethodName,Change method name,ChangeMethodName,Java,maven,org.openrewrite:rewrite-java
          org.openrewrite.java.ChangeMethodName,Change method name,ChangeMethodName,JavaScript,maven,org.openrewrite:rewrite-java
          """);

        // getAllRecipes() returns a deduplicated set (by recipe name)
        assertThat(marketplace.getAllRecipes()).hasSize(1);

        // But the recipe exists in both Java and JavaScript categories
        RecipeMarketplace.Category java = findCategory(marketplace.getRoot(), "Java");
        RecipeMarketplace.Category javascript = findCategory(marketplace.getRoot(), "JavaScript");

        assertThat(java.getCategories()).extracting("displayName").containsExactly("ChangeMethodName");
        assertThat(javascript.getCategories()).extracting("displayName").containsExactly("ChangeMethodName");

        RecipeListing javaListing = findCategory(java, "ChangeMethodName").getRecipes().getFirst();
        RecipeListing jsListing = findCategory(javascript, "ChangeMethodName").getRecipes().getFirst();

        // These are DIFFERENT RecipeListing instances with DIFFERENT RecipeBundle instances
        assertThat(javaListing).isNotSameAs(jsListing);
        assertThat(javaListing.getBundle()).isNotSameAs(jsListing.getBundle());

        // Simulate what MavenRecipeBundleReader needs to do - update version on ALL bundles.
        // If we only use getAllRecipes(), we'd only update one of them.
        // The fix uses setVersionRecursive to walk the full tree.
        String resolvedVersion = "8.70.0";
        String requestedVersion = "LATEST";

        // Walk the full tree and update all bundles matching the package
        setVersionRecursive(marketplace.getRoot(), "org.openrewrite:rewrite-java", requestedVersion, resolvedVersion);

        // Both listings should now have the version set
        assertThat(javaListing.getBundle().getVersion()).isEqualTo(resolvedVersion);
        assertThat(javaListing.getBundle().getRequestedVersion()).isEqualTo(requestedVersion);
        assertThat(jsListing.getBundle().getVersion()).isEqualTo(resolvedVersion);
        assertThat(jsListing.getBundle().getRequestedVersion()).isEqualTo(requestedVersion);

        // Round-trip through CSV writer to verify versions are preserved
        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);
        assertThat(writtenCsv).contains("LATEST");
        assertThat(writtenCsv).contains("8.70.0");

        // Verify the written CSV has versions for ALL rows, not just some
        RecipeMarketplace roundTripped = new RecipeMarketplaceReader().fromCsv(writtenCsv);
        RecipeMarketplace.Category rtJava = findCategory(roundTripped.getRoot(), "Java");
        RecipeMarketplace.Category rtJs = findCategory(roundTripped.getRoot(), "JavaScript");

        RecipeListing rtJavaListing = findCategory(rtJava, "ChangeMethodName").getRecipes().getFirst();
        RecipeListing rtJsListing = findCategory(rtJs, "ChangeMethodName").getRecipes().getFirst();

        assertThat(rtJavaListing.getBundle().getVersion()).isEqualTo(resolvedVersion);
        assertThat(rtJavaListing.getBundle().getRequestedVersion()).isEqualTo(requestedVersion);
        assertThat(rtJsListing.getBundle().getVersion()).isEqualTo(resolvedVersion);
        assertThat(rtJsListing.getBundle().getRequestedVersion()).isEqualTo(requestedVersion);
    }

    @Test
    void nullLiteralVersionIsTreatedAsNull() {
        // When a CSV contains the literal string "null" for version/requestedVersion,
        // the reader should treat it as null (not the string "null")
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName,requestedVersion,version
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses,Java Cleanup,maven,org.openrewrite:rewrite-java,null,null
          """);

        RecipeListing listing = marketplace.getAllRecipes().iterator().next();
        RecipeBundle bundle = listing.getBundle();
        assertThat(bundle.getRequestedVersion()).isNull();
        assertThat(bundle.getVersion()).isNull();
    }

    @Test
    void roundTripPreservesNullVersions() {
        // Create a marketplace with null versions, write to CSV, read back,
        // and verify versions remain null (not the string "null")
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses,Java Cleanup,maven,org.openrewrite:rewrite-java
          """);

        // Verify initial state has null versions
        RecipeListing listing = marketplace.getAllRecipes().iterator().next();
        assertThat(listing.getBundle().getVersion()).isNull();
        assertThat(listing.getBundle().getRequestedVersion()).isNull();

        // Round-trip through CSV
        @Language("csv") String csv = new RecipeMarketplaceWriter().toCsv(marketplace);
        RecipeMarketplace roundTripped = new RecipeMarketplaceReader().fromCsv(csv);

        RecipeListing rtListing = roundTripped.getAllRecipes().iterator().next();
        assertThat(rtListing.getBundle().getVersion()).isNull();
        assertThat(rtListing.getBundle().getRequestedVersion()).isNull();
    }

    private void setVersionRecursive(RecipeMarketplace.Category category, String packageName,
                                     String requestedVersion, String version) {
        for (RecipeListing recipe : category.getRecipes()) {
            RecipeBundle bundle = recipe.getBundle();
            if (packageName.equals(bundle.getPackageName())) {
                bundle.setVersion(version);
                bundle.setRequestedVersion(requestedVersion);
            }
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            setVersionRecursive(child, packageName, requestedVersion, version);
        }
    }

    @Test
    void mergePreservesMetadata() {
        // Source marketplace has a recipe with metadata in "Java" category
        RecipeMarketplace source = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName,embedding
          org.example.TestRecipe,Test Recipe,A test recipe,Java,maven,org.example:test,abc123
          """);

        // Verify metadata was read
        RecipeListing sourceRecipe = source.getAllRecipes().iterator().next();
        assertThat(sourceRecipe.getMetadata().get("embedding")).isEqualTo("abc123");

        // Target marketplace also has a recipe in "Java" (overlapping category)
        RecipeMarketplace target = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName,embedding
          org.example.OtherRecipe,Other Recipe,Another recipe,Java,maven,org.example:other,xyz789
          """);

        // Merge source into target — both have "Java" category so this exercises
        // the recursive merge path where withMarketplace() is called
        target.getRoot().merge(source.getRoot());

        // Verify both recipes exist and metadata is preserved
        RecipeListing mergedTest = target.findRecipe("org.example.TestRecipe");
        assertThat(mergedTest).isNotNull();
        assertThat(mergedTest.getMetadata().get("embedding"))
          .as("Metadata should be preserved through merge when categories overlap")
          .isEqualTo("abc123");

        RecipeListing mergedOther = target.findRecipe("org.example.OtherRecipe");
        assertThat(mergedOther).isNotNull();
        assertThat(mergedOther.getMetadata().get("embedding")).isEqualTo("xyz789");
    }

    private static RecipeMarketplace.Category findCategory(RecipeMarketplace.Category category, String name) {
        return category.getCategories().stream()
          .filter(c -> c.getDisplayName().equals(name))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Category not found: " + name));
    }
}

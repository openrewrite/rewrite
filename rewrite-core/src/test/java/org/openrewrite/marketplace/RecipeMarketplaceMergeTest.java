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

class RecipeMarketplaceMergeTest {

    @Test
    void mergeAllocatesFreshCategoriesOnTarget() {
        // The receiver must own every Category instance it holds. If `merge` adopts
        // the source's child by reference, a subsequent merge that name-matches
        // into the receiver recurses into and mutates the original source.
        @Language("csv") String csv = """
          name,category1,ecosystem,packageName
          org.example.A,Testing,maven,org.example:test
          """;
        RecipeMarketplace source = new RecipeMarketplaceReader().fromCsv(csv);
        RecipeMarketplace target = new RecipeMarketplace();

        target.getRoot().merge(source.getRoot());

        RecipeMarketplace.Category sourceTesting = findCategory(source.getRoot(), "Testing");
        RecipeMarketplace.Category targetTesting = findCategory(target.getRoot(), "Testing");
        assertThat(targetTesting).isNotSameAs(sourceTesting);
    }

    @Test
    void mergeIntoFreshTargetDoesNotMutateSource() {
        // Layering universal + org into a fresh result must leave both inputs
        // untouched — otherwise long-lived (cached) sources accumulate recipes
        // from every previous merge they were a layer of.
        @Language("csv") String universalCsv = """
          name,category1,ecosystem,packageName
          org.example.universalA,Testing,maven,org.example:test
          org.example.universalB,Testing,maven,org.example:test
          """;
        @Language("csv") String orgCsv = """
          name,category1,ecosystem,packageName
          org.example.orgC,Testing,maven,org.example:test
          """;
        RecipeMarketplace universal = new RecipeMarketplaceReader().fromCsv(universalCsv);
        RecipeMarketplace org = new RecipeMarketplaceReader().fromCsv(orgCsv);

        var universalNamesBefore = universal.getAllRecipes().stream()
                .map(RecipeListing::getName).sorted().toList();
        var orgNamesBefore = org.getAllRecipes().stream()
                .map(RecipeListing::getName).sorted().toList();

        RecipeMarketplace result = new RecipeMarketplace();
        result.getRoot().merge(universal.getRoot());
        result.getRoot().merge(org.getRoot());

        assertThat(universal.getAllRecipes().stream().map(RecipeListing::getName).sorted().toList())
                .as("universal must not gain recipes from later merges")
                .isEqualTo(universalNamesBefore);
        assertThat(org.getAllRecipes().stream().map(RecipeListing::getName).sorted().toList())
                .as("org must not gain recipes from earlier merges")
                .isEqualTo(orgNamesBefore);
        assertThat(result.getAllRecipes()).extracting(RecipeListing::getName)
                .containsExactlyInAnyOrder(
                        "org.example.universalA",
                        "org.example.universalB",
                        "org.example.orgC");
    }

    private static RecipeMarketplace.Category findCategory(
            RecipeMarketplace.Category parent, String displayName) {
        for (RecipeMarketplace.Category c : parent.getCategories()) {
            if (c.getDisplayName().equalsIgnoreCase(displayName)) {
                return c;
            }
        }
        throw new AssertionError("Category not found: " + displayName);
    }
}

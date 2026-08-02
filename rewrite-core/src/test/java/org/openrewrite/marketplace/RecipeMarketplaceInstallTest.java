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
import org.openrewrite.Recipe;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.RecipeDescriptor;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class RecipeMarketplaceInstallTest {

    /** Returns listings whose bundles claim a different package than the one being installed. */
    private static RecipeBundleReader driftingReader(RecipeBundle requested, RecipeBundle claimed) {
        return new RecipeBundleReader() {
            @Override
            public RecipeBundle getBundle() {
                return requested;
            }

            @Override
            public RecipeMarketplace read() {
                RecipeMarketplace m = new RecipeMarketplace();
                RecipeListing listing = new RecipeListing(null, "com.foo.Bar", "Bar", "Bar.",
                        null, emptyList(), emptyList(), 1, claimed);
                m.install(listing, List.of(category("Java")));
                m.install(listing, List.of(category("JavaScript")));
                return m;
            }

            @Override
            public RecipeDescriptor describe(RecipeListing listing) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static CategoryDescriptor category(String name) {
        return new CategoryDescriptor(name, "", "", emptySet(), false, 0, false);
    }

    @Test
    void installBindsEveryListingToTheRequestedBundle() {
        RecipeBundle requested = new RecipeBundle("maven", "co.uk.acme:recipes", "LATEST", "1.2.3", null);
        RecipeBundle claimed = new RecipeBundle("maven", "com.yourorg:starter", null, null, null);

        RecipeMarketplace marketplace = new RecipeMarketplace();
        marketplace.install(driftingReader(requested, claimed));

        // Walk the tree, not getAllRecipes() — both category copies must be bound.
        assertThat(allListings(marketplace.getRoot()))
                .hasSize(2)
                .allSatisfy(l -> {
                    assertThat(l.getBundle().getPackageName()).isEqualTo("co.uk.acme:recipes");
                    assertThat(l.getBundle().getVersion()).isEqualTo("1.2.3");
                });
    }

    private static List<RecipeListing> allListings(RecipeMarketplace.Category category) {
        List<RecipeListing> out = new java.util.ArrayList<>(category.getRecipes());
        for (RecipeMarketplace.Category child : category.getCategories()) {
            out.addAll(allListings(child));
        }
        return out;
    }
}

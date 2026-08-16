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
package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.request.GetMarketplaceResponse;

import java.time.Duration;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class GetMarketplaceResponseTest {

    private static List<List<CategoryDescriptor>> path() {
        return List.of(List.of(new CategoryDescriptor("Cat", "", "", emptySet(), false, 0, false)));
    }

    private static GetMarketplaceResponse.Row lightweightRow(String name, int recipeCount, @Nullable String packageName) {
        return new GetMarketplaceResponse.Row(name, name, name, null, emptyList(), emptyList(),
                recipeCount, path(), packageName);
    }

    /**
     * A marketplace holding a single listing-weight recipe under one category, for emitter tests.
     */
    private static RecipeMarketplace marketplaceWith(RecipeListing listing) {
        RecipeMarketplace marketplace = new RecipeMarketplace();
        marketplace.install(listing, List.of(new CategoryDescriptor("Cat", "", "", emptySet(), false, 0, false)));
        return marketplace;
    }

    private static RecipeListing compositeListing(RecipeBundle bundle) {
        return new RecipeListing(null, "recipe.Composite", "Composite Display", "Composite Description",
                Duration.ofMinutes(3),
                List.of(new OptionDescriptor("opt", "String", "Opt", "Opt description", null, null, false, null)),
                emptyList(), 5, bundle);
    }

    @Test
    void filtersForeignOriginAndAttributesTheRestToRequestedBundle() {
        GetMarketplaceResponse response = new GetMarketplaceResponse();
        // Row from a different package than the one requested: it belongs to that bundle's own reader,
        // so this read must filter it out.
        response.add(lightweightRow("recipe.FromCore", 1, "Core"));
        // Row with no origin: must fall back to the requested bundle.
        response.add(lightweightRow("recipe.Unattributed", 1, null));
        // Row whose origin equals the requested bundle's packageName: attributed to the requested
        // bundle (with its version preserved).
        response.add(lightweightRow("recipe.OwnPinned", 1, "Migration"));

        RecipeBundle requested = new RecipeBundle("nuget", "Migration", null, "1.0.0", null);
        RecipeMarketplace marketplace = response.toMarketplace(requested);

        // Foreign-origin row is excluded — its own bundle's reader contributes it instead.
        assertThat(marketplace.findRecipe("recipe.FromCore")).isNull();

        // Null-origin row falls back to the requested bundle.
        RecipeListing unattributed = marketplace.findRecipe("recipe.Unattributed");
        assertThat(unattributed).isNotNull();
        assertThat(unattributed.getBundle().getPackageName()).isEqualTo("Migration");

        // Own-origin row is attributed to the requested bundle, preserving its version.
        RecipeListing ownPinned = marketplace.findRecipe("recipe.OwnPinned");
        assertThat(ownPinned).isNotNull();
        assertThat(ownPinned.getBundle().getPackageName()).isEqualTo("Migration");
        assertThat(ownPinned.getBundle().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void buildsListingFromLightweightFields() {
        GetMarketplaceResponse response = new GetMarketplaceResponse();
        response.add(new GetMarketplaceResponse.Row(
                "recipe.Light", "Light Display", "Light Description",
                Duration.ofMinutes(5),
                List.of(new OptionDescriptor("opt", "String", "Opt", "Opt description", null, null, false, null)),
                emptyList(), 7, path(), null));

        RecipeBundle bundle = new RecipeBundle("nuget", "Migration", null, "1.0.0", null);
        RecipeListing listing = response.toMarketplace(bundle).findRecipe("recipe.Light");

        assertThat(listing).isNotNull();
        assertThat(listing.getDisplayName()).isEqualTo("Light Display");
        assertThat(listing.getDescription()).isEqualTo("Light Description");
        assertThat(listing.getEstimatedEffortPerOccurrence()).isEqualTo(Duration.ofMinutes(5));
        assertThat(listing.getOptions()).hasSize(1);
        assertThat(listing.getRecipeCount()).isEqualTo(7);
    }

    @Test
    void emitsListingWeightRows() {
        RecipeBundle bundle = new RecipeBundle("nuget", "Migration", null, "1.0.0", null);
        RecipeMarketplace marketplace = marketplaceWith(compositeListing(bundle));

        GetMarketplaceResponse response = GetMarketplaceResponse.fromMarketplace(marketplace);

        assertThat(response).hasSize(1);
        GetMarketplaceResponse.Row row = response.get(0);
        assertThat(row.getName()).isEqualTo("recipe.Composite");
        assertThat(row.getDisplayName()).isEqualTo("Composite Display");
        assertThat(row.getDescription()).isEqualTo("Composite Description");
        assertThat(row.getEstimatedEffortPerOccurrence()).isEqualTo(Duration.ofMinutes(3));
        assertThat(row.getOptions()).hasSize(1);
        assertThat(row.getRecipeCount()).isEqualTo(5);
        assertThat(row.getPackageName()).isEqualTo("Migration");
    }

    @Test
    void roundTripsListingWeightRows() {
        RecipeBundle bundle = new RecipeBundle("nuget", "Migration", null, "1.0.0", null);
        RecipeMarketplace source = marketplaceWith(compositeListing(bundle));

        RecipeListing round = GetMarketplaceResponse.fromMarketplace(source)
                .toMarketplace(bundle).findRecipe("recipe.Composite");

        assertThat(round).isNotNull();
        assertThat(round.getDisplayName()).isEqualTo("Composite Display");
        assertThat(round.getDescription()).isEqualTo("Composite Description");
        assertThat(round.getEstimatedEffortPerOccurrence()).isEqualTo(Duration.ofMinutes(3));
        assertThat(round.getOptions()).hasSize(1);
        assertThat(round.getRecipeCount()).isEqualTo(5);
    }
}

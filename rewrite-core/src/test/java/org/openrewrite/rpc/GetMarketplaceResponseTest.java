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

import org.junit.jupiter.api.Test;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.request.GetMarketplaceResponse;

import java.net.URI;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class GetMarketplaceResponseTest {

    private static RecipeDescriptor descriptor(String name) {
        return new RecipeDescriptor(name, name, name, name, emptySet(), null,
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), URI.create("https://example.com"));
    }

    @Test
    void filtersForeignOriginAndAttributesTheRestToRequestedBundle() {
        GetMarketplaceResponse response = new GetMarketplaceResponse();
        // Row from a different package than the one requested: it belongs to that bundle's own reader,
        // so this read must filter it out.
        response.add(new GetMarketplaceResponse.Row(descriptor("recipe.FromCore"),
                List.of(List.of(new CategoryDescriptor("Cat", "", "", emptySet(), false, 0, false))),
                "Core"));
        // Row with no origin: must fall back to the requested bundle.
        response.add(new GetMarketplaceResponse.Row(descriptor("recipe.Unattributed"),
                List.of(List.of(new CategoryDescriptor("Cat", "", "", emptySet(), false, 0, false))),
                null));
        // Row whose origin equals the requested bundle's packageName: attributed to the requested
        // bundle (with its version preserved).
        response.add(new GetMarketplaceResponse.Row(descriptor("recipe.OwnPinned"),
                List.of(List.of(new CategoryDescriptor("Cat", "", "", emptySet(), false, 0, false))),
                "Migration"));

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
}

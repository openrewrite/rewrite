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
package org.openrewrite.rpc.request;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class GetMarketplaceResponse extends ArrayList<GetMarketplaceResponse.Row> {
    @Value
    public static class Row {
        /**
         * Listing-weight fields. Emitters populate these and leave {@link #descriptor} null; the full
         * descriptor is fetched lazily via {@code PrepareRecipe}.
         */
        @Nullable String name;

        @Nullable @NlsRewrite.DisplayName String displayName;

        @Nullable @NlsRewrite.Description String description;

        @Nullable Duration estimatedEffortPerOccurrence;

        @Nullable List<OptionDescriptor> options;

        @Nullable List<DataTableDescriptor> dataTables;

        /**
         * The count of this recipe plus all of its transitive sub-recipes. Emitters must compute it
         * as {@code 1 + all transitive recipeList entries}; the host uses it as a sort key (see
         * {@link RecipeListing#getRecipeCount()}).
         */
        int recipeCount;

        /**
         * The full, recursive recipe descriptor. Retained for backward compatibility with peers that
         * still emit it, but nullable and no longer emitted by up-to-date engines — prefer the
         * lightweight fields above. Will be removed once nothing emits it.
         */
        @Nullable RecipeDescriptor descriptor;

        List<List<CategoryDescriptor>> categoryPaths;

        /**
         * The package this recipe was contributed by, as reported by the RPC server. Lets the host
         * attribute each row to its true bundle instead of force-tagging every row with the one
         * requested bundle. Null when the server does not report origin (e.g. ecosystems not yet
         * emitting it, or built-in recipes) — such rows fall back to the requested bundle.
         */
        @Nullable String packageName;
    }

    public RecipeMarketplace toMarketplace(RecipeBundle bundle) {
        RecipeMarketplace marketplace = new RecipeMarketplace();
        for (Row recipe : this) {
            // A row carrying a different package's origin belongs to that bundle's own reader, not this
            // one — skip it so each reader contributes only its own recipes (and a later install of one
            // bundle can't resurrect another the host has uninstalled). A null origin (ecosystems not yet
            // emitting it, or built-in recipes) falls back to the requested bundle.
            if (recipe.getPackageName() != null &&
                    !recipe.getPackageName().equals(bundle.getPackageName())) {
                continue;
            }
            RecipeListing listing = toListing(recipe, bundle);
            for (List<CategoryDescriptor> categoryPath : recipe.getCategoryPaths()) {
                marketplace.install(listing, categoryPath);
            }
        }
        return marketplace;
    }

    /**
     * The {@link RecipeListing} a row represents, attributed to {@code bundle}. Prefers the
     * lightweight fields; a null {@link Row#name} means an older peer sent only {@link Row#descriptor},
     * so fall back to deriving the listing from it.
     */
    private static RecipeListing toListing(Row row, RecipeBundle bundle) {
        return row.getName() != null ?
                new RecipeListing(null, row.getName(), row.getDisplayName(), row.getDescription(),
                        row.getEstimatedEffortPerOccurrence(),
                        row.getOptions() != null ? row.getOptions() : emptyList(),
                        row.getDataTables() != null ? row.getDataTables() : emptyList(),
                        row.getRecipeCount(), bundle) :
                RecipeListing.fromDescriptor(row.getDescriptor(), bundle);
    }

    public static GetMarketplaceResponse fromMarketplace(RecipeMarketplace marketplace) {
        Map<String, Row> rowByRecipeId = new LinkedHashMap<>();
        for (RecipeMarketplace.Category category : marketplace.getCategories()) {
            fromCategory(rowByRecipeId, category, new ArrayList<>());
        }
        GetMarketplaceResponse response = new GetMarketplaceResponse();
        response.addAll(rowByRecipeId.values());
        return response;
    }

    /**
     * @deprecated Use {@link #fromMarketplace(RecipeMarketplace)}. The {@code resolvers} argument is
     * no longer needed: rows are emitted listing-weight straight off each {@link RecipeListing}, so no
     * recipe is resolved or described here.
     */
    @Deprecated
    public static GetMarketplaceResponse fromMarketplace(RecipeMarketplace marketplace, List<RecipeBundleResolver> resolvers) {
        return fromMarketplace(marketplace);
    }

    private static void fromCategory(Map<String, Row> rowByRecipeId,
                                     RecipeMarketplace.Category category,
                                     List<CategoryDescriptor> parentCategory) {
        List<CategoryDescriptor> categoryPath = new ArrayList<>(parentCategory);
        categoryPath.add(new CategoryDescriptor(category.getDisplayName(), "",
                category.getDescription(), emptySet(), false, 0, false));
        for (RecipeListing recipe : category.getRecipes()) {
            rowByRecipeId.computeIfAbsent(recipe.getName(), recipeId ->
                    new Row(recipe.getName(), recipe.getDisplayName(), recipe.getDescription(),
                            recipe.getEstimatedEffortPerOccurrence(), recipe.getOptions(),
                            recipe.getDataTables(), recipe.getRecipeCount(), null,
                            new ArrayList<>(), recipe.getBundle().getPackageName())).categoryPaths.add(categoryPath);
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            fromCategory(rowByRecipeId, child, categoryPath);
        }
    }
}

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
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptySet;

public class GetMarketplaceResponse extends ArrayList<GetMarketplaceResponse.Row> {
    @Value
    public static class Row {
        RecipeDescriptor descriptor;
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
            for (List<CategoryDescriptor> categoryPath : recipe.getCategoryPaths()) {
                marketplace.install(RecipeListing.fromDescriptor(recipe.getDescriptor(), bundle), categoryPath);
            }
        }
        return marketplace;
    }

    public static GetMarketplaceResponse fromMarketplace(RecipeMarketplace marketplace, List<RecipeBundleResolver> resolvers) {
        Map<String, Row> rowByRecipeId = new LinkedHashMap<>();
        for (RecipeMarketplace.Category category : marketplace.getCategories()) {
            fromCategory(resolvers, rowByRecipeId, category, new ArrayList<>());
        }
        GetMarketplaceResponse response = new GetMarketplaceResponse();
        response.addAll(rowByRecipeId.values());
        return response;
    }

    private static void fromCategory(List<RecipeBundleResolver> resolvers,
                                     Map<String, Row> rowByRecipeId,
                                     RecipeMarketplace.Category category,
                                     List<CategoryDescriptor> parentCategory) {
        List<CategoryDescriptor> categoryPath = new ArrayList<>(parentCategory);
        categoryPath.add(new CategoryDescriptor(category.getDisplayName(), "",
                category.getDescription(), emptySet(), false, 0, false));
        for (RecipeListing recipe : category.getRecipes()) {
            rowByRecipeId.computeIfAbsent(recipe.getName(), recipeId ->
                    new Row(recipe.describe(resolvers), new ArrayList<>(),
                            recipe.getBundle().getPackageName())).categoryPaths.add(categoryPath);
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            fromCategory(resolvers, rowByRecipeId, child, categoryPath);
        }
    }
}

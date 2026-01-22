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
    }

    public RecipeMarketplace toMarketplace(RecipeBundle bundle) {
        RecipeMarketplace marketplace = new RecipeMarketplace();
        for (Row recipe : this) {
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
                    new Row(recipe.describe(resolvers), new ArrayList<>())).categoryPaths.add(categoryPath);
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            fromCategory(resolvers, rowByRecipeId, child, categoryPath);
        }
    }
}

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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;
import org.openrewrite.config.RecipeDescriptor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class RecipeMarketplace {
    public static final String ROOT = "\u03B5"; // epsilon

    @NlsRewrite.DisplayName
    private final String displayName;

    @NlsRewrite.DisplayName
    private final String description;

    /**
     * Every category is itself a slightly-more miniature marketplace contained in a larger
     * marketplace.
     */
    private final List<RecipeMarketplace> categories = new ArrayList<>();

    private final List<RecipeListing> recipes = new ArrayList<>();

    public boolean isRoot() {
        return ROOT.equals(displayName);
    }

    public @Nullable RecipeDescriptor findRecipe(String name) {
        for (RecipeListing recipe : recipes) {
            if (recipe.getName().equals(name)) {
                return recipe.describe();
            }
        }
        for (RecipeMarketplace category : categories) {
            RecipeDescriptor rd = category.findRecipe(name);
            if (rd != null) {
                return rd;
            }
        }
        return null;
    }
}

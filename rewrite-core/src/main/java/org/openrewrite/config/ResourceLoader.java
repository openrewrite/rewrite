/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.config;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Contributor;
import org.openrewrite.Recipe;
import org.openrewrite.style.NamedStyles;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ResourceLoader {

    enum RecipeDetail {
        CONTRIBUTORS,
        MAINTAINERS,
        EXAMPLES;

        boolean includedIn(RecipeDetail... include) {
            for (RecipeDetail detail : include) {
                if (detail == this) {
                    return true;
                }
            }
            return false;
        }
    }

    default @Nullable Recipe loadRecipe(String recipeName, RecipeDetail... details) {
        return listRecipes().stream()
                .filter(r -> r.getName().equals(recipeName))
                .peek(r -> {
                    if (!RecipeDetail.CONTRIBUTORS.includedIn(details)) {
                        r.setContributors(null);
                    }
                })
                .findFirst()
                .orElse(null);
    }

    Collection<Recipe> listRecipes();

    Collection<RecipeDescriptor> listRecipeDescriptors();

    Collection<NamedStyles> listStyles();

    Collection<CategoryDescriptor> listCategoryDescriptors();

    Map<String, List<Contributor>> listContributors();

    Map<String, List<RecipeExample>> listRecipeExamples();

    Set<License> listLicenses();
}

/*
 * Copyright 2022 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import org.openrewrite.Recipe;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A recipe that exists only to wrap other recipes.
 * Anonymous recipe classes aren't serializable/deserializable so use this, or another named type, instead
 */
@RequiredArgsConstructor
public class CompositeRecipe extends Recipe {
    private final List<Recipe> recipeList;

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return null;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return recipeList;
    }

    @Override
    public List<DataTableDescriptor> getDataTableDescriptors() {
        List<DataTableDescriptor> dataTableDescriptors = null;
        for (Recipe recipe : getRecipeList()) {
            List<DataTableDescriptor> dtds = recipe.getDataTableDescriptors();
            if (!dtds.isEmpty()) {
                if (dataTableDescriptors == null) {
                    dataTableDescriptors = new ArrayList<>();
                }
                for (DataTableDescriptor dtd : dtds) {
                    if (!dataTableDescriptors.contains(dtd)) {
                        dataTableDescriptors.add(dtd);
                    }
                }
            }
        }
        return dataTableDescriptors == null ? super.getDataTableDescriptors() : dataTableDescriptors;
    }
}

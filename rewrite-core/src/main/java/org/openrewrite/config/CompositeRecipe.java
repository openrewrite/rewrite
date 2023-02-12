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

import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A recipe that exists only to wrap other recipes.
 * Anonymous recipe classes aren't serializable/deserializable so use this, or another named type, instead
 */
public class CompositeRecipe extends Recipe {

    private static final Duration DEFAULT_ESTIMATED_EFFORT = Duration.ofMinutes(5);
    private Duration estimatedEffortPerOccurrence;

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        if (estimatedEffortPerOccurrence == null) {
            long total = 0;
            for (Recipe recipe : getRecipeList()) {
                if (isNonzero(recipe.getEstimatedEffortPerOccurrence())) {
                    // Duration arithmetic has poor performance, and this code gets hit a lot with deeply-nested recipes
                    total += recipe.getEstimatedEffortPerOccurrence().toMillis();
                }
            }

            if (total == 0) {
                return DEFAULT_ESTIMATED_EFFORT;
            }

            estimatedEffortPerOccurrence = Duration.ofMillis(total);
        }
        return estimatedEffortPerOccurrence;
    }

    private static boolean isNonzero(@Nullable Duration estimatedEffortPerOccurrence) {
        return estimatedEffortPerOccurrence != null && !estimatedEffortPerOccurrence.equals(Duration.ZERO);
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

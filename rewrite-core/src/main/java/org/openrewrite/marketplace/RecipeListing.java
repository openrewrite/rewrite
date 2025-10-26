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

import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface RecipeListing {
    String getName();

    @NlsRewrite.DisplayName
    String getDisplayName();

    @NlsRewrite.Description
    String getDescription();

    /**
     * @return The bundle that contains this recipe, or null
     * if the source of the recipe is unknown.
     */
    @Nullable
    RecipeBundle getBundle();

    @Nullable
    Duration getEstimatedEffortPerOccurrence();

    List<? extends Option> getOptions();

    default RecipeDescriptor describe() {
        if (getBundle() == null) {
            throw new IllegalStateException("Unable to describe a recipe whose bundle is unknown");
        }
        return getBundle().describe(this);
    }

    default Recipe prepare(Map<String, Object> options) {
        if (getBundle() == null) {
            throw new IllegalStateException("Unable to prepare a recipe whose bundle is unknown");
        }
        return getBundle().prepare(this, options);
    }

    interface Option {
        String getName();

        @NlsRewrite.DisplayName
        String getDisplayName();

        @NlsRewrite.Description
        String getDescription();
    }
}

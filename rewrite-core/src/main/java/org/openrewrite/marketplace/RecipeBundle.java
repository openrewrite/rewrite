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
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;

import java.util.Map;

public interface RecipeBundle {
    String getPackageEcosystem();

    String getPackageName();

    @Nullable String getVersion();

    /**
     * @return Teams partitioning of a recipe marketplace can be used
     * to segment access or visibility of recipes by teams.
     */
    @Nullable
    String getTeam();

    RecipeDescriptor describe(RecipeListing listing);

    Recipe prepare(RecipeListing listing, Map<String, Object> options);
}

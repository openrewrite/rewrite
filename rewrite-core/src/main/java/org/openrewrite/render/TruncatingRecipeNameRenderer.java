/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.render;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Recipe;

@RequiredArgsConstructor
public class TruncatingRecipeNameRenderer implements RecipeNameRenderer {
    private final int maxLength;

    @Override
    public String getDisplayName(Recipe recipe) {
        return truncate(recipe.getDisplayName());
    }

    @Override
    public String getInstanceName(Recipe recipe) {
        return truncate(recipe.getInstanceName());
    }

    private String truncate(String name) {
        if (name.length() > maxLength) {
            if (name.endsWith("\"") || name.endsWith("'") || name.endsWith("`")) {
                return name.substring(0, maxLength - 4).trim() + "..." +
                       name.charAt(name.length() - 1);
            } else {
                return name.substring(0, maxLength - 3).trim() + "...";
            }
        }
        return name;
    }
}

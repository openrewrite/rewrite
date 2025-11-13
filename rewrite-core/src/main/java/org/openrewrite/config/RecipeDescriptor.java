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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Contributor;
import org.openrewrite.Maintainer;
import org.openrewrite.NlsRewrite;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecipeDescriptor {
    @EqualsAndHashCode.Include
    String name;

    @NlsRewrite.DisplayName
    String displayName;

    @NlsRewrite.DisplayName
    String instanceName;

    @NlsRewrite.Description
    String description;

    Set<String> tags;

    @Nullable
    Duration estimatedEffortPerOccurrence;

    @EqualsAndHashCode.Include
    List<OptionDescriptor> options;

    @With
    List<RecipeDescriptor> recipeList;

    @With
    List<DataTableDescriptor> dataTables;

    List<Maintainer> maintainers;

    @Deprecated(/* No longer populated */)
    List<Contributor> contributors;

    List<RecipeExample> examples;

    @Deprecated
    URI source;

    /**
     * @param env Provides a source of category descriptors to build category names from more
     *            than just the name segments.
     * @return A list of category display names inferred from the structure of the recipe name,
     * replacing individual segments of the display name with category descriptor names when those
     * are defined to the provided environment.
     */
    public List<String> inferCategoriesFromName(Environment env) {
        // Extract package from recipe name (everything before the last dot)
        int lastDot = displayName.lastIndexOf('.');
        if (lastDot == -1) {
            return emptyList();
        }

        String packageName = displayName.substring(0, lastDot);

        String[] parts = packageName.split("\\.");
        List<String> categories = new ArrayList<>(parts.length);

        nextPart:
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            String partialPackage = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
            for (CategoryDescriptor categoryDescriptor : env.listCategoryDescriptors()) {
                String categoryPackageName = categoryDescriptor.getPackageName();
                if (categoryPackageName.equals(partialPackage)) {
                    if (categoryDescriptor.isRoot()) {
                        continue nextPart;
                    }
                    categories.add(categoryDescriptor.getDisplayName());
                    continue nextPart;
                }
            }

            if (!part.isEmpty()) {
                String capitalized = Character.toUpperCase(part.charAt(0)) + part.substring(1);
                categories.add(capitalized);
            }
        }

        return categories;
    }
}

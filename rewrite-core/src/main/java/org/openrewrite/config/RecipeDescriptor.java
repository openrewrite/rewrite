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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.intellij.lang.annotations.Language;
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
import static java.util.Collections.emptySet;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecipeDescriptor {
    @EqualsAndHashCode.Include
    private final String name;

    @NlsRewrite.DisplayName
    private final String displayName;

    @NlsRewrite.DisplayName
    private final String instanceName;

    @NlsRewrite.Description
    private final String description;

    private final Set<String> tags;

    @Nullable
    private final Duration estimatedEffortPerOccurrence;

    @EqualsAndHashCode.Include
    private final List<OptionDescriptor> options;

    @With
    private final List<RecipeDescriptor> preconditions;

    @With
    private final List<RecipeDescriptor> recipeList;

    @With
    private final List<DataTableDescriptor> dataTables;

    private final List<Maintainer> maintainers;

    @Deprecated(/* No longer populated */)
    private final List<Contributor> contributors;

    private final List<RecipeExample> examples;

    @Deprecated
    private final URI source;

    /**
     * Recipes constructed locally always pass non-null collections (see
     * {@link org.openrewrite.Recipe#createRecipeDescriptor()}). Polyglot RPC peers
     * may omit empty collections from JSON, so normalize null to empty here to
     * preserve the "never null" contract for collection-valued getters.
     */
    @JsonCreator
    public RecipeDescriptor(
            @JsonProperty("name") String name,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("instanceName") String instanceName,
            @JsonProperty("description") String description,
            @JsonProperty("tags") @Nullable Set<String> tags,
            @JsonProperty("estimatedEffortPerOccurrence") @Nullable Duration estimatedEffortPerOccurrence,
            @JsonProperty("options") @Nullable List<OptionDescriptor> options,
            @JsonProperty("preconditions") @Nullable List<RecipeDescriptor> preconditions,
            @JsonProperty("recipeList") @Nullable List<RecipeDescriptor> recipeList,
            @JsonProperty("dataTables") @Nullable List<DataTableDescriptor> dataTables,
            @JsonProperty("maintainers") @Nullable List<Maintainer> maintainers,
            @JsonProperty("contributors") @Nullable List<Contributor> contributors,
            @JsonProperty("examples") @Nullable List<RecipeExample> examples,
            @JsonProperty("source") URI source) {
        this.name = name;
        this.displayName = displayName;
        this.instanceName = instanceName;
        this.description = description;
        this.tags = tags == null ? emptySet() : tags;
        this.estimatedEffortPerOccurrence = estimatedEffortPerOccurrence;
        this.options = options == null ? emptyList() : options;
        this.preconditions = preconditions == null ? emptyList() : preconditions;
        this.recipeList = recipeList == null ? emptyList() : recipeList;
        this.dataTables = dataTables == null ? emptyList() : dataTables;
        this.maintainers = maintainers == null ? emptyList() : maintainers;
        this.contributors = contributors == null ? emptyList() : contributors;
        this.examples = examples == null ? emptyList() : examples;
        this.source = source;
    }

    @Deprecated
    public RecipeDescriptor(String name, String displayName, String instanceName, String description,
                            Set<String> tags, @Nullable Duration estimatedEffortPerOccurrence,
                            List<OptionDescriptor> options, List<RecipeDescriptor> recipeList,
                            List<DataTableDescriptor> dataTables, List<Maintainer> maintainers,
                            List<Contributor> contributors, List<RecipeExample> examples, URI source) {
        this(name, displayName, instanceName, description, tags, estimatedEffortPerOccurrence,
                options, emptyList(), recipeList, dataTables, maintainers, contributors, examples, source);
    }

    /**
     * @param env Provides a source of category descriptors to build category names from more
     *            than just the name segments.
     * @return A list of category display names inferred from the structure of the recipe name,
     * replacing individual segments of the display name with category descriptor names when those
     * are defined to the provided environment.
     */
    public List<CategoryDescriptor> inferCategoriesFromName(Environment env) {
        // Extract package from recipe name (everything before the last dot)
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return emptyList();
        }

        String packageName = name.substring(0, lastDot);

        String[] parts = packageName.split("\\.");
        List<CategoryDescriptor> categories = new ArrayList<>(parts.length);

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
                    categories.add(categoryDescriptor);
                    continue nextPart;
                }
            }

            if (!part.isEmpty()) {
                @Language("markdown") String capitalized = Character.toUpperCase(part.charAt(0)) + part.substring(1);
                categories.add(new CategoryDescriptor(capitalized, partialPackage, "", emptySet(),
                        false, 0, false));
            }
        }

        return categories;
    }
}

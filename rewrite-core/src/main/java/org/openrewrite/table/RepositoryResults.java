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
package org.openrewrite.table;

import lombok.Value;
import lombok.With;
import org.openrewrite.*;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class RepositoryResults extends DataTable<RepositoryResults.Row> {

    private final Map<RecipeKey, Counter> counters = new HashMap<>();

    public RepositoryResults(Recipe recipe) {
        super(recipe, "Source files that had results",
                "Source files that were modified by the recipe run.");
    }

    @Value
    @With
    public static class Row {
        @Column(displayName = "Root of the recipe that made changes",
                description = "The root recipe that made a change.")
        String rootRecipe;

        @Column(displayName = "First child recipe that made changes",
                description = "The first child recipe that made a change.")
        String recipe;

        @Column(displayName = "Recipe options",
                description = "The options of the first child (or root, if they are equal) recipe that made a change.")
        String recipeOptions;

        @Column(displayName = "Estimated time saving",
                description = "An estimated effort that a developer to fix manually instead of using this recipe," +
                              " in unit of seconds.")
        Long estimatedTimeSaving;

        @Column(displayName = "Occurrences",
                description = "The total number of changes made to this repository.")
        int occurrences;

        @Column(displayName = "Files changed",
                description = "The number of files changed to this repository.")
        int filesChanged;
    }

    public void recordResult(@Nullable SourceFile before, Stack<Recipe> recipeStack, Long effortSeconds) {
        int rootIndex = 0;

        Recipe root = recipeStack.get(rootIndex);
        // Skip any CompositeRecipe wrappers
        while (root instanceof CompositeRecipe) {
            root = recipeStack.get(++rootIndex);
        }
        Recipe recipe = recipeStack.size() < rootIndex + 1 ? root : recipeStack.get(rootIndex + 1);
        String rootName = root == null ? "" : root.getName();
        String recipeName = recipe == null ? "" : recipe.getName();
        String options = recipe == null ? "[]" : recipe.getDescriptor().getOptions().stream()
                .filter(option -> option.getValue() != null)
                .map(option -> {
                    String valueAsString = option.getValue().toString();
                    String quotedValue;
                    if (valueAsString.contains("\"")) {
                        quotedValue = "`" + valueAsString + "`";
                    } else {
                        quotedValue = "\"" + valueAsString + "\"";
                    }
                    return option.getName() + "=" + quotedValue;
                })
                .collect(Collectors.joining(", ", "[", "]"));

        Counter counter = counters.computeIfAbsent(new RecipeKey(rootName, recipeName, options), k -> new Counter());
        if (before == null || counter.beforeFiles.add(before.getSourcePath())) {
            counter.filesChanged++;
        }
        counter.occurrences++;
        counter.estimatedTimeSavings += effortSeconds;
    }

    public void flush(ExecutionContext ctx) {
        maxCycle = ctx.getCycle(); //allow to write to this data table after last cycle
        for (Map.Entry<RecipeKey, Counter> entry : counters.entrySet()) {
            insertRow(ctx, new Row(entry.getKey().getRootName(), entry.getKey().getRecipeName(), entry.getKey().getOptions(), entry.getValue().estimatedTimeSavings, entry.getValue().occurrences, entry.getValue().filesChanged));
        }
    }

    private static class Counter {
        int occurrences = 0;
        long estimatedTimeSavings = 0;
        int filesChanged = 0;
        Set<Path> beforeFiles = new HashSet<>();
    }

    @Value
    private static class RecipeKey {
        String rootName;
        String recipeName;
        String options;
    }

    @Value
    private static class Option {
        String name;
        Object value;
    }

}

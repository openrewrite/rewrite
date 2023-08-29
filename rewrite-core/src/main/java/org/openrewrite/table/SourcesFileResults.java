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
import org.openrewrite.*;
import org.openrewrite.config.RecipeDescriptor;

import java.util.Stack;

public class SourcesFileResults extends DataTable<SourcesFileResults.Row> {

    public SourcesFileResults(Recipe recipe) {
        super(recipe, "Source files that had results",
                "Source files that were modified by the recipe run.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path before the run",
                description = "The source path of the file before the run.")
        String sourcePath;

        @Column(displayName = "Source path after the run",
                description = "A recipe may modify the source path. This is the path after the run.")
        String afterSourcePath;

        @Column(displayName = "Parent of the recipe that made changes",
                description = "In a hierarchical recipe, the parent of the recipe that made a change. Empty if " +
                              "this is the root of a hierarchy or if the recipe is not hierarchical at all.")
        String parentRecipe;

        @Column(displayName = "Recipe that made changes",
                description = "The specific recipe that made a change.")
        String recipe;

        @Column(displayName = "Estimated time saving",
                description = "An estimated effort that a developer to fix manually instead of using this recipe," +
                              " in unit of seconds.")
        Long estimatedTimeSaving;

        @Column(displayName = "Cycle",
                description = "The recipe cycle in which the change was made.")
        int cycle;
    }

    public static SourcesFileResults build(Changeset changeset, int cycle, ExecutionContext ctx) {
        SourcesFileResults resultsTable = new SourcesFileResults(Recipe.noop());
        for (Result result : changeset.getAllResults()) {
            Stack<RecipeDescriptor[]> recipeStack = new Stack<>();

            for (RecipeDescriptor rd : result.getRecipeDescriptorsThatMadeChanges()) {
                recipeStack.push(new RecipeDescriptor[]{null, rd});
            }

            while (!recipeStack.isEmpty()) {
                RecipeDescriptor[] recipeThatMadeChange = recipeStack.pop();

                resultsTable.insertRow(ctx, new SourcesFileResults.Row(
                        result.getBefore() == null ? "" : result.getBefore().getSourcePath().toString(),
                        result.getAfter() == null ? "" : result.getAfter().getSourcePath().toString(),
                        recipeThatMadeChange[0] == null ? "" : recipeThatMadeChange[0].getName(),
                        recipeThatMadeChange[1].getName(),
                        result.getTimeSavings() == null ? 0 : result.getTimeSavings().getSeconds(),
                        cycle
                ));
                for (int i = recipeThatMadeChange[1].getRecipeList().size() - 1; i >= 0; i--) {
                    RecipeDescriptor rd = recipeThatMadeChange[1].getRecipeList().get(i);
                    recipeStack.push(new RecipeDescriptor[]{recipeThatMadeChange[1], rd});
                }
            }
        }

        return resultsTable;
    }
}

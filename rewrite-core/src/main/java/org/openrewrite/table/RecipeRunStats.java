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
package org.openrewrite.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;

public class RecipeRunStats extends DataTable<RecipeRunStats.Row> {

    public RecipeRunStats(Recipe recipe) {
        super(recipe,
                "Recipe performance",
                "Statistics used in analyzing the performance of recipes.");
    }

    public void record(ExecutionContext ctx, Recipe recipe, org.openrewrite.RecipeRunStats runStats) {
        insertRow(ctx, new org.openrewrite.table.RecipeRunStats.Row(
                runStats.getRecipe().getName(),
                runStats.getCalls(),
                runStats.getCumulative().toNanos(),
                runStats.getMax().toNanos(),
                runStats.getOwnGetVisitor().toNanos(),
                runStats.getOwnVisit().toNanos()
        ));
        for (org.openrewrite.RecipeRunStats called : runStats.getCalled()) {
            record(ctx, recipe, called);
        }
    }

    @Value
    public static class Row {
        @Column(displayName = "Recipe that made changes",
                description = "The specific recipe that made a change.")
        String recipe;

        @Column(displayName = "Calls",
                description = "The number of times the recipe ran over all cycles.")
        Integer calls;

        @Column(displayName = "Cumulative time",
                description = "The total time spent across all executions of this recipe in nanoseconds.")
        Long cumulative;

        @Column(displayName = "Max time",
                description = "The max time spent in any one execution of this recipe in nanoseconds.")
        Long max;

        @Column(displayName = "Time running `getVisitor()`",
                description = "The total time spent in running the visitor returned by `Recipe#getVisitor()` for this recipe.")
        Long ownGetVisitor;

        @Column(displayName = "Time running `visit()`",
                description = "The total time spent in running `Recipe#visit()` for this recipe.")
        Long ownVisit;
    }
}

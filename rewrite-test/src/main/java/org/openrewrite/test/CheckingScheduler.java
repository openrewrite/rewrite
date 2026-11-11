/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.test;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.scheduling.RecipeRunCycle;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SearchResults;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import java.util.List;

/**
 * Schedules an in-process run so that the checks {@link RewriteTest} sets on its sources see each cycle.
 */
@RequiredArgsConstructor
final class CheckingScheduler extends RecipeScheduler {
    private final @Nullable UnchangedTreeIdentity unchangedTreeIdentity;
    private final @Nullable AccumulatorContents accumulatorContents;

    static RecipeScheduler of(LargeSourceSet sources) {
        if (!(sources instanceof LargeSourceSetCheckingExpectedCycles)) {
            return new RecipeScheduler();
        }
        LargeSourceSetCheckingExpectedCycles checked = (LargeSourceSetCheckingExpectedCycles) sources;
        return new CheckingScheduler(checked.getUnchangedTreeIdentity(), checked.getAccumulatorContents());
    }

    @Override
    protected RecipeRunCycle<LargeSourceSet> createRecipeRunCycle(Recipe recipe, int cycle, Cursor rootCursor,
                                                                  WatchableExecutionContext ctxWithWatch,
                                                                  RecipeRunStats recipeRunStats,
                                                                  SearchResults searchResults,
                                                                  SourcesFileResults sourceFileResults,
                                                                  SourcesFileErrors errorsTable) {
        if (unchangedTreeIdentity != null) {
            unchangedTreeIdentity.startCycle();
        }
        if (accumulatorContents != null) {
            accumulatorContents.startCycle();
        }
        return new RecipeRunCycle<LargeSourceSet>(recipe, cycle, rootCursor, ctxWithWatch, recipeRunStats,
                searchResults, sourceFileResults, errorsTable, LargeSourceSet::edit) {
            @Override
            public LargeSourceSet scanSources(LargeSourceSet sourceSet) {
                LargeSourceSet scanned = super.scanSources(sourceSet);
                if (accumulatorContents != null) {
                    accumulatorContents.check(rootCursor, ctxWithWatch, "after scanning");
                }
                return scanned;
            }

            @Override
            public LargeSourceSet editSources(LargeSourceSet sourceSet) {
                LargeSourceSet edited = super.editSources(sourceSet);
                // before the scheduler clears the root cursor, and with it the accumulators
                if (accumulatorContents != null) {
                    accumulatorContents.check(rootCursor, ctxWithWatch, "after editing");
                }
                return edited;
            }

            @Override
            protected void recordSourceFileResultAndSearchResults(@Nullable SourceFile before,
                                                                  @Nullable SourceFile after,
                                                                  List<Recipe> recipeStack,
                                                                  ExecutionContext ctx) {
                if (unchangedTreeIdentity != null) {
                    unchangedTreeIdentity.record(before, after, recipeStack, getCycle());
                }
                super.recordSourceFileResultAndSearchResults(before, after, recipeStack, ctx);
            }
        };
    }
}

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
package org.openrewrite;

import org.openrewrite.scheduling.RecipeRunCycle;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.openrewrite.Recipe.PANIC;
import static org.openrewrite.scheduling.WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT;

public class RecipeScheduler {

    public RecipeRun scheduleRun(Recipe recipe,
                                 LargeSourceSet sourceSet,
                                 ExecutionContext ctx,
                                 int maxCycles,
                                 int minCycles) {
        try {
            LargeSourceSet after = runRecipeCycles(recipe, sourceSet, ctx, maxCycles, minCycles);
            return new RecipeRun(
                    after.getChangeset(),
                    ctx.getMessage(ExecutionContext.DATA_TABLES, emptyMap())
            );
        } finally {
            Path workingDirectoryRoot = ctx.getMessage(WORKING_DIRECTORY_ROOT);
            if (workingDirectoryRoot != null) {
                deleteWorkingDirectory(workingDirectoryRoot);
            }
        }
    }

    private LargeSourceSet runRecipeCycles(Recipe recipe, LargeSourceSet sourceSet, ExecutionContext ctx, int maxCycles, int minCycles) {
        WatchableExecutionContext ctxWithWatch = new WatchableExecutionContext(ctx);

        RecipeRunStats recipeRunStats = new RecipeRunStats(Recipe.noop());
        SourcesFileErrors errorsTable = new SourcesFileErrors(Recipe.noop());
        SourcesFileResults sourceFileResults = new SourcesFileResults(Recipe.noop());

        LargeSourceSet after = sourceSet;

        for (int i = 1; i <= maxCycles; i++) {
            if (ctx.getMessage(PANIC) != null) {
                break;
            }

            // this root cursor is shared by all `TreeVisitor` instances used created from `getVisitor` and
            // single source applicable tests so that data can be shared at the root (especially for caching
            // use cases like sharing a `JavaTypeCache` between `JavaTemplate` parsers).
            Cursor rootCursor = new Cursor(null, Cursor.ROOT_VALUE);
            try {
                RecipeRunCycle<LargeSourceSet> cycle = new RecipeRunCycle<>(recipe, i, rootCursor, ctxWithWatch,
                        recipeRunStats, sourceFileResults, errorsTable, LargeSourceSet::edit);
                ctxWithWatch.putCycle(cycle);
                after.beforeCycle(i == maxCycles);

                // pre-transformation scanning phase where there can only be modifications to capture exceptions
                // occurring during the scanning phase
                if (hasScanningRecipe(recipe)) {
                    after = cycle.scanSources(after);
                }

                // transformation phases
                after = cycle.generateSources(after);
                after = cycle.editSources(after);

                boolean anyRecipeCausingAnotherCycle = false;
                for (Recipe madeChanges : cycle.getMadeChangesInThisCycle()) {
                    if (madeChanges.causesAnotherCycle()) {
                        anyRecipeCausingAnotherCycle = true;
                    }
                }

                if (i >= minCycles &&
                    (cycle.getMadeChangesInThisCycle().isEmpty() || !anyRecipeCausingAnotherCycle)) {
                    after.afterCycle(true);
                    break;
                }

                after.afterCycle(i == maxCycles);
                ctxWithWatch.resetHasNewMessages();
            } finally {
                // Clear any messages that were added to the root cursor during the cycle. This is important
                // to avoid leaking memory in the case when a recipe defines a static TreeVisitor. That
                // TreeVisitor will still contain a reference to this rootCursor and any messages in it
                // after recipe execution completes. The pattern of holding a static TreeVisitor isn't
                // recommended, but isn't possible for us to guard against at an API level, and so we are
                // defensive about memory consumption here.
                rootCursor.clearMessages();
            }
        }

        recipeRunStats.flush(ctx);
        return after;
    }

    private boolean hasScanningRecipe(Recipe recipe) {
        if (recipe instanceof ScanningRecipe) {
            return true;
        }
        for (Recipe r : recipe.getRecipeList()) {
            if (hasScanningRecipe(r)) {
                return true;
            }
        }
        return false;
    }

    // Delete any files created in the working directory
    private static void deleteWorkingDirectory(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> files = Files.list(path)) {
                    files.forEach(RecipeScheduler::deleteWorkingDirectory);
                }
            }
            Files.delete(path);
        } catch (IOException ignore) {
        }
    }
}

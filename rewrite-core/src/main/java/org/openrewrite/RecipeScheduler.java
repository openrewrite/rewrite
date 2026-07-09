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

import org.jspecify.annotations.Nullable;
import org.openrewrite.scheduling.RecipeRunStage;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SearchResults;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.newSetFromMap;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Recipe.PANIC;
import static org.openrewrite.scheduling.WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT;

public class RecipeScheduler {
    @Nullable
    private Supplier<Cursor> rootCursorProvider;

    /**
     * Set a provider for the root cursor used in each recipe stage.
     * The provider is called once per stage to create a fresh root cursor.
     * This allows injecting shared state (e.g., a type cache provider) that will be
     * visible to all visitors and template parsers during the stage.
     * <p>
     * If not set, a default root cursor is created with {@code new Cursor(null, Cursor.ROOT_VALUE)}.
     *
     * @param provider supplies a configured root cursor for each stage
     * @return this scheduler for chaining
     */
    public RecipeScheduler rootCursorProvider(Supplier<Cursor> provider) {
        this.rootCursorProvider = provider;
        return this;
    }

    public RecipeRun scheduleRun(Recipe recipe,
                                 LargeSourceSet sourceSet,
                                 ExecutionContext ctx,
                                 int maxStages,
                                 int minStages) {
        try {
            LargeSourceSet after = runRecipeStages(recipe, sourceSet, ctx, maxStages, minStages);
            return new RecipeRun(
                    after.getChangeset(),
                    DataTableExecutionContextView.view(ctx).getDataTableStore()
            );
        } finally {
            Path workingDirectoryRoot = ctx.getMessage(WORKING_DIRECTORY_ROOT);
            if (workingDirectoryRoot != null) {
                deleteWorkingDirectory(workingDirectoryRoot);
            }
        }
    }

    private LargeSourceSet runRecipeStages(Recipe recipe, LargeSourceSet sourceSet, ExecutionContext ctx, int maxStages, int minStages) {
        WatchableExecutionContext ctxWithWatch = new WatchableExecutionContext(ctx);

        RecipeRunStats recipeRunStats = new RecipeRunStats(Recipe.noop());
        SourcesFileErrors errorsTable = new SourcesFileErrors(Recipe.noop());
        SearchResults searchResults = new SearchResults(Recipe.noop());
        SourcesFileResults sourceFileResults = new SourcesFileResults(Recipe.noop());

        LargeSourceSet after = sourceSet;

        Deque<Recipe> stageQueue = new ArrayDeque<>();
        stageQueue.add(recipe);
        List<Recipe> executedStages = new ArrayList<>();
        Set<Recipe> registeredForOnComplete = newSetFromMap(new IdentityHashMap<>());

        try {
            // Each iteration polls and runs exactly one stage, so the loop counter is the stage number.
            for (int stage = 1; !stageQueue.isEmpty(); stage++) {
                if (ctx.getMessage(PANIC) != null) {
                    break;
                }
                Recipe stageRecipe = requireNonNull(stageQueue.poll());
                // First poll of a recipe instance is its lineage's first stage; self-edge continuations
                // re-poll the same instance. Drives onComplete (once) and data-table dedup alike.
                boolean firstStageInLineage = registeredForOnComplete.add(stageRecipe);
                if (firstStageInLineage) {
                    executedStages.add(stageRecipe);
                }
                after = runStage(stageRecipe, stage, firstStageInLineage, after, ctxWithWatch, maxStages, minStages,
                        recipeRunStats, searchResults, sourceFileResults, errorsTable, stageQueue);
            }
        } finally {
            recipeRunStats.flush(ctx);
            for (Recipe stageRecipe : executedStages) {
                recursiveOnComplete(stageRecipe, ctxWithWatch);
            }
        }
        return after;
    }

    private LargeSourceSet runStage(Recipe recipe, int stage, boolean firstStageInLineage, LargeSourceSet after, WatchableExecutionContext ctxWithWatch,
                                    int maxStages, int minStages, RecipeRunStats recipeRunStats,
                                    SearchResults searchResults, SourcesFileResults sourceFileResults,
                                    SourcesFileErrors errorsTable, Deque<Recipe> stageQueue) {
        // this root cursor is shared by all `TreeVisitor` instances used created from `getVisitor` and
        // single source applicable tests so that data can be shared at the root (especially for caching
        // use cases like sharing a `JavaTypeCache` between `JavaTemplate` parsers).
        Cursor rootCursor = rootCursorProvider != null
                ? rootCursorProvider.get()
                : new Cursor(null, Cursor.ROOT_VALUE);
        try {
            RecipeRunStage<LargeSourceSet> runStage = createRecipeRunStage(recipe, stage, rootCursor, ctxWithWatch, recipeRunStats, searchResults, sourceFileResults, errorsTable);
            runStage.setFirstStageInLineage(firstStageInLineage);
            ctxWithWatch.putStage(runStage);
            after.beforeStage();

            // pre-transformation scanning phase where there can only be modifications to capture exceptions
            // occurring during the scanning phase
            after = runStage.scanSources(after);
            // transformation phases
            after = runStage.generateSources(after);
            after = runStage.editSources(after);

            RecipeList next = new RecipeList(recipe.getName());
            collectNextStage(recipe, ctxWithWatch, next);
            List<Recipe> scheduled = next.getRecipes();

            if (!scheduled.isEmpty()) {
                if (stage >= maxStages) {
                    throw new RecipeException("Recipe run exceeded maximum stage count of " + maxStages + ". " +
                                              "This is likely due to a bug in a recipe that is always adding " +
                                              "a nextStage, not allowing the run to converge. Alternatively, the " +
                                              "recipe is too complex by construction to run in the stage count maximum " +
                                              "set by this recipe runtime.");
                }
                stageQueue.add(scheduled.size() == 1 ? scheduled.get(0) : new StageRecipe(scheduled));
            } else if (stage < minStages && stage < maxStages) {
                // Nothing left to run, but the run's stage floor isn't met: run the stage once more to
                // confirm stability (how tests assert idempotency).
                stageQueue.add(recipe);
            }

            // The run's last pass is this one only when nothing further is queued, so the source set's
            // end-of-run bookkeeping does not fire while downstream stages remain. With a single converged
            // stage the queue is empty here, identical to the classic behavior.
            boolean lastStageOfRun = stageQueue.isEmpty();
            after.afterStage(lastStageOfRun);

            if (!lastStageOfRun) {
                ctxWithWatch.resetHasNewMessages();
            }
        } finally {
            // Clear any messages that were added to the root cursor during the stage. This is important
            // to avoid leaking memory in the case when a recipe defines a static TreeVisitor. That
            // TreeVisitor will still contain a reference to this rootCursor and any messages in it
            // after recipe execution completes. The pattern of holding a static TreeVisitor isn't
            // recommended, but isn't possible for us to guard against at an API level, and so we are
            // defensive about memory consumption here.
            rootCursor.clearMessages();
        }
        return after;
    }

    private void collectNextStage(Recipe recipe, ExecutionContext ctx, RecipeList next) {
        recipe.nextStage(next, ctx);
        for (Recipe r : recipe.getRecipeList()) {
            collectNextStage(r, ctx, next);
        }
    }

    protected RecipeRunStage<LargeSourceSet> createRecipeRunStage(Recipe recipe, int cycle, Cursor rootCursor, WatchableExecutionContext ctxWithWatch, RecipeRunStats recipeRunStats, SearchResults searchResults, SourcesFileResults sourceFileResults, SourcesFileErrors errorsTable) {
        return new RecipeRunStage<>(recipe, cycle, rootCursor, ctxWithWatch,
                recipeRunStats, searchResults, sourceFileResults, errorsTable, LargeSourceSet::edit);
    }

    private void recursiveOnComplete(Recipe recipe, ExecutionContext ctx) {
        recipe.onComplete(ctx);
        for (Recipe r : recipe.getRecipeList()) {
            recursiveOnComplete(r, ctx);
        }
    }

    /**
     * A synthetic root recipe wrapping the recipes a stage scheduled for its successor stage, so they run
     * together as one downstream stage rather than as separate full-source passes.
     */
    private static class StageRecipe extends Recipe {
        private final List<Recipe> recipeList;

        private StageRecipe(List<Recipe> recipeList) {
            this.recipeList = recipeList;
        }

        @Override
        public String getDisplayName() {
            return "Stage";
        }

        @Override
        public String getDescription() {
            return "A dynamically scheduled stage of recipes.";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return recipeList;
        }
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

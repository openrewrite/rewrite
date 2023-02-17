/*
 * Copyright 2021 the original author or authors.
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

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Generated;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Recipe.PANIC;
import static org.openrewrite.RecipeSchedulerUtils.addRecipesThatMadeChanges;
import static org.openrewrite.RecipeSchedulerUtils.handleUncaughtException;

/**
 * The scheduler is responsible for executing a {@link Recipe} full lifecycle and
 * reporting a {@link RecipeRun} result.
 */
public interface RecipeScheduler {
    default <T> List<T> mapAsync(List<T> input, UnaryOperator<T> mapFn) {
        @SuppressWarnings("unchecked") CompletableFuture<T>[] futures =
                new CompletableFuture[input.size()];

        int k = 0;
        for (T before : input) {
            Callable<T> updateTreeFn = () -> mapFn.apply(before);
            futures[k++] = schedule(updateTreeFn);
        }

        CompletableFuture.allOf(futures).join();
        return ListUtils.map(input, (j, in) -> futures[j].join());
    }

    default RecipeRun scheduleRun(
            Recipe recipe,
            List<? extends SourceFile> before,
            ExecutionContext ctx,
            int maxCycles,
            int minCycles
    ) {
        org.openrewrite.table.RecipeRunStats runStatsTable = new org.openrewrite.table.RecipeRunStats(Recipe.noop());
        RecipeRunStats runStats = new RecipeRunStats(recipe);
        RecipeRun recipeRun = new RecipeRun(runStats, emptyList(), emptyMap());

        // Guards against recipes that run on source files with the same ID
        Set<UUID> sourceFileIds = new HashSet<>();
        before = ListUtils.map(before, sourceFile -> {
            if (!sourceFileIds.add(sourceFile.getId())) {
                return sourceFile.withId(Tree.randomId());
            }
            return sourceFile;
        });

        DistributionSummary.builder("rewrite.recipe.run")
                .tag("recipe", recipe.getDisplayName())
                .description("The distribution of recipe runs and the size of source file batches given to them to process.")
                .baseUnit("source files")
                .register(Metrics.globalRegistry)
                .record(before.size());

        Map<UUID, Stack<Recipe>> recipeThatAddedOrDeletedSourceFile = new HashMap<>();
        List<? extends SourceFile> acc = before;
        List<? extends SourceFile> after = acc;

        WatchableExecutionContext ctxWithWatch = new WatchableExecutionContext(ctx);
        for (int i = 0; i < maxCycles; i++) {
            if (ctx.getMessage(PANIC) != null) {
                break;
            }

            Stack<Recipe> recipeStack = new Stack<>();
            recipeStack.push(recipe);

            after = scheduleVisit(
                    runStats,
                    recipeStack,
                    acc,
                    ctxWithWatch,
                    null,
                    recipeThatAddedOrDeletedSourceFile
            );
            if (i + 1 >= minCycles && ((after == acc && !ctxWithWatch.hasNewMessages()) || !recipe.causesAnotherCycle())) {
                break;
            }
            acc = after;
            ctxWithWatch.resetHasNewMessages();
        }

        if (after == before) {
            runStatsTable.record(ctx, recipe, runStats);
            return recipeRun.withDataTables(ctx.getMessage(ExecutionContext.DATA_TABLES, emptyMap()));
        }

        List<Result> results = RecipeSchedulerUtils.createAndProcessResults(
                before,
                after,
                ctx,
                recipeThatAddedOrDeletedSourceFile
        );

        runStatsTable.record(ctx, recipe, runStats);
        return recipeRun
                .withResults(results)
                .withDataTables(ctx.getMessage(ExecutionContext.DATA_TABLES, emptyMap()));
    }

    default <S extends SourceFile> List<S> scheduleVisit(
            RecipeRunStats runStats,
            Stack<Recipe> recipeStack,
            List<S> before,
            ExecutionContext ctx,
            @Nullable Map<UUID, Boolean> singleSourceApplicableTestResult,
            Map<UUID, Stack<Recipe>> recipeThatAddedOrDeletedSourceFile
    ) {
        runStats.markCall();
        long startTime = System.nanoTime();
        Recipe recipe = recipeStack.peek();
        assert recipe == runStats.getRecipe() : "Recipe stack should always contain the recipe being run";

        ctx.putCurrentRecipe(recipe);
        if (ctx instanceof WatchableExecutionContext) {
            ((WatchableExecutionContext) ctx).resetHasNewMessages();
        }

        try {
            List<Recipe> applicableTests = recipe.getApplicableTests();
            if (!applicableTests.isEmpty()) {
                boolean anySourceMatch = false;
                for (S s : before) {
                    if (RecipeSchedulerUtils.testAllApplicableTestsMatchSourceFile(s, applicableTests, runStats, this, recipeStack, ctx)) {
                        anySourceMatch = true;
                        break;
                    }
                }

                if (!anySourceMatch) {
                    return before;
                }
            }

            List<Recipe> singleSourceApplicableTests = recipe.getSingleSourceApplicableTests();
            if (!singleSourceApplicableTests.isEmpty()) {
                if (singleSourceApplicableTestResult == null || singleSourceApplicableTestResult.isEmpty()) {
                    if (singleSourceApplicableTestResult == null) {
                        singleSourceApplicableTestResult = new HashMap<>(before.size());
                    }
                }
                for (S s : before) {
                    singleSourceApplicableTestResult.put(
                            s.getId(),
                            RecipeSchedulerUtils.testAllApplicableTestsMatchSourceFile(
                                    s,
                                    singleSourceApplicableTests,
                                    runStats,
                                    this,
                                    recipeStack,
                                    ctx
                            )
                    );
                }
            }
        } catch (Throwable t) {
            return handleUncaughtException(recipeStack, recipeThatAddedOrDeletedSourceFile, before, ctx, recipe, t);
        }

        SourcesFileErrors errorsTable = new SourcesFileErrors(Recipe.noop());
        AtomicBoolean thrownErrorOnTimeout = new AtomicBoolean(false);
        List<S> after;
        final Map<UUID, Boolean> singleSourceApplicableTestResultRef = singleSourceApplicableTestResult;
        boolean hasSingleSourceApplicableTest =
                singleSourceApplicableTestResult != null &&
                        !singleSourceApplicableTestResult.isEmpty();

        if (!recipe.validate(ctx).isValid()) {
            after = before;
        } else {
            long getVisitorStartTime = System.nanoTime();
            after = mapAsync(before, s -> {
                Timer.Builder timer = Timer.builder("rewrite.recipe.visit").tag("recipe", recipe.getDisplayName());
                Timer.Sample sample = Timer.start();

                S afterFile = s;
                try {
                    if (hasSingleSourceApplicableTest &&
                            singleSourceApplicableTestResultRef.containsKey(s.getId()) &&
                            !singleSourceApplicableTestResultRef.get(s.getId())) {
                        return s;
                    }

                    Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
                    if (duration.compareTo(ctx.getRunTimeout(before.size())) > 0) {
                        if (thrownErrorOnTimeout.compareAndSet(false, true)) {
                            RecipeTimeoutException t = new RecipeTimeoutException(recipe);
                            ctx.getOnError().accept(t);
                            ctx.getOnTimeout().accept(t, ctx);
                        }
                        sample.stop(MetricsHelper.successTags(timer, "timeout").register(Metrics.globalRegistry));
                        return s;
                    }

                    if (ctx.getMessage(PANIC) != null) {
                        sample.stop(MetricsHelper.successTags(timer, "panic").register(Metrics.globalRegistry));
                        return s;
                    }

                    TreeVisitor<?, ExecutionContext> visitor = recipe.getVisitor();

                    //noinspection unchecked
                    afterFile = (S) visitor.visitSourceFile(s, ctx);

                    if (afterFile != null && visitor.isAcceptable(afterFile, ctx)) {
                        //noinspection unchecked
                        afterFile = (S) visitor.visit(afterFile, ctx);
                    }
                } catch (Throwable t) {
                    sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
                    ctx.getOnError().accept(t);

                    if (t instanceof RecipeRunException) {
                        RecipeRunException vt = (RecipeRunException) t;

                        //noinspection unchecked
                        afterFile = (S) new FindRecipeRunException(vt).visitNonNull(
                                requireNonNull(afterFile, "afterFile is null"),
                                0
                        );
                    } else if (afterFile != null) {
                        // The applicable test threw an exception, but it was not in a visitor. It cannot be associated to any specific line of code,
                        // and instead we add a marker to the top of the source file to record the exception message.
                        afterFile = Markup.error(afterFile, t);
                    }

                    // Use the original source file to record the error, not the one that may have been modified by the visitor.
                    // This is so the error is associated with the original source file, and it's original source path.
                    if (s != null) {
                        errorsTable.insertRow(ctx, new SourcesFileErrors.Row(
                                s.getSourcePath().toString(),
                                recipe.getName(),
                                ExceptionUtils.sanitizeStackTrace(t, RecipeScheduler.class)
                        ));
                    }
                }

                if (afterFile != null && afterFile != s) {
                    afterFile = addRecipesThatMadeChanges(recipeStack, afterFile);
                    sample.stop(MetricsHelper.successTags(timer, "changed").register(Metrics.globalRegistry));
                } else if (afterFile == null) {
                    recipeThatAddedOrDeletedSourceFile.put(requireNonNull(s).getId(), recipeStack);
                    sample.stop(MetricsHelper.successTags(timer, "deleted").register(Metrics.globalRegistry));
                } else {
                    sample.stop(MetricsHelper.successTags(timer, "unchanged").register(Metrics.globalRegistry));
                }
                return afterFile;
            });
            runStats.ownGetVisitorCompleted(getVisitorStartTime);
        }

        // The type of the list is widened at this point, since a source file type may be generated that isn't
        // of a type that is in the original set of source files (e.g. only XML files are given, and the
        // recipe generates Java code).
        List<SourceFile> afterWidened;
        final Map<UUID, Boolean> lastSingleSourceApplicableTestResult = singleSourceApplicableTestResult;
        final Map<UUID, Boolean> newSingleSourceApplicableTestResult = new HashMap<>();
        try {
            long ownVisitStartTime = System.nanoTime();

            if (hasSingleSourceApplicableTest) {
                // If no files passed the single source applicable test, skip the recipe
                if (singleSourceApplicableTestResult.values().stream().noneMatch(b -> b)) {
                    return after;
                }
            }

            //noinspection unchecked
            afterWidened = recipe.visit((List<SourceFile>) after, ctx);

            if (hasSingleSourceApplicableTest) {
                // update single source applicability test results
                Map<UUID, SourceFile> originalMap = new HashMap<>(after.size());
                for (SourceFile file : after) {
                    originalMap.put(file.getId(), file);
                }

                afterWidened = ListUtils.map(afterWidened, s -> {
                    Boolean singleSourceTestResult = lastSingleSourceApplicableTestResult.get(s.getId());
                    if (singleSourceTestResult != null) {
                        newSingleSourceApplicableTestResult.put(s.getId(), singleSourceTestResult);
                        if (!singleSourceTestResult) {
                            return originalMap.get(s.getId());
                        }
                    } else {
                        // It's a newly generated file
                        newSingleSourceApplicableTestResult.put(s.getId(), true);
                    }
                    return s;
                });
            }

            runStats.ownVisitCompleted(ownVisitStartTime);
        } catch (Throwable t) {
            return handleUncaughtException(recipeStack, recipeThatAddedOrDeletedSourceFile, before, ctx, recipe, t);
        }

        if (afterWidened != after) {
            Map<UUID, SourceFile> originalMap = new HashMap<>(after.size());
            for (SourceFile file : after) {
                originalMap.put(file.getId(), file);
            }
            afterWidened = ListUtils.map(afterWidened, s -> {
                SourceFile original = originalMap.get(s.getId());
                if (original == null) {
                    // a new source file generated
                    recipeThatAddedOrDeletedSourceFile.put(s.getId(), recipeStack);
                } else if (s != original) {
                    return RecipeSchedulerUtils.addRecipesThatMadeChanges(
                            recipeStack,
                            s
                    );
                }
                return s;
            });

            for (SourceFile maybeDeleted : after) {
                if (!afterWidened.contains(maybeDeleted)) {
                    // a source file deleted
                    recipeThatAddedOrDeletedSourceFile.put(maybeDeleted.getId(), recipeStack);
                }
            }
        }

        for (Recipe r : recipe.getRecipeList()) {
            if (ctx.getMessage(PANIC) != null) {
                //noinspection unchecked
                return (List<S>) afterWidened;
            }

            Stack<Recipe> nextStack = new Stack<>();
            nextStack.addAll(recipeStack);
            nextStack.push(r);

            RecipeRunStats nextStats = null;
            for (RecipeRunStats called : runStats.getCalled()) {
                if (called.recipe == r) {
                    nextStats = called;
                    break;
                }
            }

            // when doNext is called conditionally inside a recipe visitor
            if (nextStats == null) {
                nextStats = runStats.addCalledRecipe(r);
            }

            Map<UUID, Boolean> newMap = new HashMap<>(newSingleSourceApplicableTestResult);
            afterWidened = scheduleVisit(
                    nextStats,
                    nextStack,
                    afterWidened,
                    ctx,
                    newMap,
                    recipeThatAddedOrDeletedSourceFile
            );
        }

        runStats.recipeVisitCompleted(startTime);

        //noinspection unchecked
        return (List<S>) afterWidened;
    }

    <T> CompletableFuture<T> schedule(Callable<T> fn);
}

class RecipeSchedulerUtils {
    static List<Result> createAndProcessResults(
            List<? extends SourceFile> before,
            List<? extends SourceFile> after,
            ExecutionContext ctx,
            Map<UUID, Stack<Recipe>> recipeThatAddedOrDeletedSourceFile
    ) {
        Map<UUID, SourceFile> sourceFileIdentities = new HashMap<>();
        for (SourceFile sourceFile : before) {
            sourceFileIdentities.put(sourceFile.getId(), sourceFile);
        }
        List<Result> results = new ArrayList<>();

        // added or changed files
        for (SourceFile s : after) {
            SourceFile original = sourceFileIdentities.get(s.getId());
            if (original != s) {
                if (original == null) {
                    results.add(new Result(null, s, singletonList(recipeThatAddedOrDeletedSourceFile.get(s.getId()))));
                } else {
                    if (original.getMarkers().findFirst(Generated.class).isPresent()) {
                        continue;
                    }

                    results.add(new Result(
                            original,
                            s,
                            s.getMarkers()
                                    .findFirst(RecipesThatMadeChanges.class)
                                    .orElseThrow(() -> new IllegalStateException("SourceFile changed but no recipe " +
                                            "reported making a change"))
                                    .getRecipes()
                    ));
                }
            }
        }

        Set<UUID> afterIds = after.stream().map(SourceFile::getId).collect(Collectors.toSet());

        // removed files
        for (SourceFile s : before) {
            if (!afterIds.contains(s.getId()) && !s.getMarkers().findFirst(Generated.class).isPresent()) {
                results.add(new Result(s, null, singleton(recipeThatAddedOrDeletedSourceFile.get(s.getId()))));
            }
        }

        // Process the Result and add to the results table
        for (Result result : results) {
            SourcesFileResults resultsTable = new SourcesFileResults(Recipe.noop());
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
                        recipeThatMadeChange[1].getName()
                ));
                for (RecipeDescriptor rd : recipeThatMadeChange[1].getRecipeList()) {
                    recipeStack.push(new RecipeDescriptor[]{recipeThatMadeChange[1], rd});
                }
            }
        }
        return results;
    }

    public static <S extends SourceFile> S addRecipesThatMadeChanges(
            Stack<Recipe> recipeStack,
            S afterFile
    ) {
        return afterFile.withMarkers(afterFile.getMarkers().computeByType(
                RecipesThatMadeChanges.create(recipeStack),
                (r1, r2) -> {
                    r1.getRecipes().addAll(r2.getRecipes());
                    return r1;
                }));
    }

    public static <S extends SourceFile> List<S> handleUncaughtException(
            Stack<Recipe> recipeStack,
            Map<UUID, Stack<Recipe>> recipeThatAddedOrDeletedSourceFile,
            List<S> before,
            ExecutionContext ctx,
            Recipe recipe,
            Throwable t
    ) {
        ctx.getOnError().accept(t);
        ctx.putMessage(PANIC, true);

        if (t instanceof RecipeRunException) {
            RecipeRunException vt = (RecipeRunException) t;

            List<S> exceptionMapped = ListUtils.map(before, sourceFile -> {
                //noinspection unchecked
                S afterFile = (S) new FindRecipeRunException(vt).visitNonNull(requireNonNull((SourceFile) sourceFile), 0);
                if (afterFile != sourceFile) {
                    afterFile = addRecipesThatMadeChanges(recipeStack, afterFile);
                }
                return afterFile;
            });
            if (exceptionMapped != before) {
                return exceptionMapped;
            }
        }

        // The applicable test threw an exception, but it was not in a visitor. It cannot be associated to any specific line of code,
        // and instead we add a new file to record the exception message.
        S exception = PlainTextParser.builder().build()
                .parse("Rewrite encountered an uncaught recipe error in " + recipe.getName() + ".")
                .get(0)
                .withSourcePath(Paths.get("recipe-exception-" + ctx.incrementAndGetUncaughtExceptionCount() + ".txt"));
        exception = Markup.error(exception, t);
        recipeThatAddedOrDeletedSourceFile.put(exception.getId(), recipeStack);
        return ListUtils.concat(before, exception);
    }

    /**
     * @return true if the file qualified (file changed) for all applicable tests.
     */
    static <S extends SourceFile> boolean testAllApplicableTestsMatchSourceFile(
            S s,
            List<Recipe> applicableTests,
            RecipeRunStats runStats,
            RecipeScheduler recipeScheduler,
            Stack<Recipe> recipeStack,
            ExecutionContext ctx
    ) {
        List<S> sList = singletonList(s);
        boolean allMatch = true;
        for (Recipe applicableTest : applicableTests) {
            RecipeRunStats nextStats = runStats.addCalledRecipe(applicableTest);
            // We still need the stack to be accurate for the applicable test, so that ExecutionContext.getRecipeStack() is correct.
            Stack<Recipe> stack = new Stack<>();
            stack.addAll(recipeStack);
            stack.push(applicableTest);
            Recipe previousParent = ctx.putParentRecipe(recipeStack.peek());
            // Recursively schedule the recipe to visit the applicable tests
            List<S> next = recipeScheduler.scheduleVisit(
                    nextStats,
                    stack,
                    sList,
                    ctx,
                    null,
                    new HashMap<>()
            );
            ctx.putParentRecipe(previousParent);
            if (sList == next) {
                allMatch = false;
                break;
            }
            // Re-surface any errors generated applying the applicability tests up to the top level
            for (S newS : next) {
                newS.getMarkers().findFirst(Markup.Error.class).ifPresent(m -> {
                    if (m.getException() instanceof RecipeRunException) {
                        throw (RecipeRunException) m.getException();
                    } else {
                        throw new RuntimeException("Applicable Test Failed", m.getException());
                    }
                });
            }
        }
        return allMatch;
    }
}

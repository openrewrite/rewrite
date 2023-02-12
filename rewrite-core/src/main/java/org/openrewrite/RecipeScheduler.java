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
import java.util.function.BiFunction;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Recipe.PANIC;
import static org.openrewrite.RecipeSchedulerUtils.addRecipesThatMadeChanges;
import static org.openrewrite.RecipeSchedulerUtils.handleUncaughtException;
import static org.openrewrite.Tree.randomId;

public interface RecipeScheduler {
    default <T> List<T> mapAsync(List<T> input, BiFunction<T, Integer, T> mapFn) {
        @SuppressWarnings("unchecked") CompletableFuture<T>[] futures =
                new CompletableFuture[input.size()];

        int k = 0;
        for (int i = 0; i < input.size(); i++) {
            T before = input.get(i);
            final int index = i;
            Callable<T> updateTreeFn = () -> mapFn.apply(before, index);
            futures[k++] = schedule(updateTreeFn);
        }

        CompletableFuture.allOf(futures).join();
        return ListUtils.map(input, (j, in) -> futures[j].join());
    }

    default RecipeRun scheduleRun(Recipe recipe,
                                  List<? extends SourceFile> before,
                                  ExecutionContext ctx,
                                  int maxCycles,
                                  int minCycles) {
        org.openrewrite.table.RecipeRunStats runStatsTable = new org.openrewrite.table.RecipeRunStats(Recipe.noop());
        RecipeRunStats runStats = new RecipeRunStats(recipe);
        RecipeRun recipeRun = new RecipeRun(runStats, emptyList(), emptyMap());

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

            after = scheduleVisit(runStats, recipeStack, acc, null, ctxWithWatch, recipeThatAddedOrDeletedSourceFile);
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

                    results.add(new Result(original, s, s.getMarkers()
                            .findFirst(RecipesThatMadeChanges.class)
                            .orElseThrow(() -> new IllegalStateException("SourceFile changed but no recipe reported making a change"))
                            .getRecipes()));
                }
            }
        }

        Set<UUID> afterIds = new HashSet<>();
        for (SourceFile sourceFile : after) {
            afterIds.add(sourceFile.getId());
        }

        // removed files
        for (SourceFile s : before) {
            if (!afterIds.contains(s.getId()) && !s.getMarkers().findFirst(Generated.class).isPresent()) {
                results.add(new Result(s, null, singleton(recipeThatAddedOrDeletedSourceFile.get(s.getId()))));
            }
        }

        for (Result result : results) {
            SourcesFileResults resultsTable = new SourcesFileResults(Recipe.noop());
            for (RecipeDescriptor recipeThatMadeChange : result.getRecipeDescriptorsThatMadeChanges()) {
                resultsTable.insertRow(ctx, new SourcesFileResults.Row(
                        result.getBefore() == null ? "" : result.getBefore().getSourcePath().toString(),
                        result.getAfter() == null ? "" : result.getAfter().getSourcePath().toString(),
                        recipeThatMadeChange.getName()));
            }
        }

        runStatsTable.record(ctx, recipe, runStats);
        return recipeRun
                .withResults(results)
                .withDataTables(ctx.getMessage(ExecutionContext.DATA_TABLES, emptyMap()));
    }

    default <S extends SourceFile> List<S> scheduleVisit(RecipeRunStats runStats,
                                                         Stack<Recipe> recipeStack,
                                                         List<S> before,
                                                         @Nullable List<Boolean> singleSourceApplicableTestResult,
                                                         ExecutionContext ctx,
                                                         Map<UUID, Stack<Recipe>> recipeThatAddedOrDeletedSourceFile) {
        runStats.calls.incrementAndGet();
        long startTime = System.nanoTime();
        Recipe recipe = recipeStack.peek();

        ctx.putCurrentRecipe(recipe);
        if (ctx instanceof WatchableExecutionContext) {
            ((WatchableExecutionContext) ctx).resetHasNewMessages();
        }

        try {
            if (!recipe.getApplicableTests().isEmpty()) {
                boolean anySourceMatch = false;
                for (S s : before) {
                    boolean allMatch = true;
                    for (TreeVisitor<?, ExecutionContext> applicableTest : recipe.getApplicableTests()) {
                        if (applicableTest.visit(s, ctx) == s) {
                            allMatch = false;
                            break;
                        }
                    }
                    if (allMatch) {
                        anySourceMatch = true;
                        break;
                    }
                }

                if (!anySourceMatch) {
                    return before;
                }
            }

            if (!recipe.getSingleSourceApplicableTests().isEmpty()) {
                if (singleSourceApplicableTestResult == null || singleSourceApplicableTestResult.isEmpty()) {
                    if (singleSourceApplicableTestResult == null) {
                        singleSourceApplicableTestResult = new ArrayList<>(before.size());
                    }

                    for (S s : before) {
                        boolean allMatch = true;
                        for (TreeVisitor<?, ExecutionContext> singleSourceApplicableTest : recipe.getSingleSourceApplicableTests()) {
                            if (singleSourceApplicableTest.visit(s, ctx) == s) {
                                allMatch = false;
                                break;
                            }
                        }
                        singleSourceApplicableTestResult.add(allMatch);
                    }
                }
            }
        } catch (Throwable t) {
            return handleUncaughtException(recipeStack, recipeThatAddedOrDeletedSourceFile, before, ctx, recipe, t);
        }

        SourcesFileErrors errorsTable = new SourcesFileErrors(Recipe.noop());
        AtomicBoolean thrownErrorOnTimeout = new AtomicBoolean(false);
        List<S> after;
        final List<Boolean> newSingleSourceApplicableTestResult = singleSourceApplicableTestResult;

        if (!recipe.validate(ctx).isValid()) {
            after = before;
        } else {
            long getVisitorStartTime = System.nanoTime();
            after = mapAsync(before, (s, index) -> {
                Timer.Builder timer = Timer.builder("rewrite.recipe.visit").tag("recipe", recipe.getDisplayName());
                Timer.Sample sample = Timer.start();

                S afterFile = s;
                try {
                    if (newSingleSourceApplicableTestResult != null && !newSingleSourceApplicableTestResult.isEmpty()) {
                        if (!newSingleSourceApplicableTestResult.get(index)) {
                            return s;
                        }
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

                    if (visitor.isAcceptable(s, ctx)) {
                        //noinspection unchecked
                        afterFile = (S) visitor.visit(afterFile, ctx);
                    }
                } catch (Throwable t) {
                    sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
                    ctx.getOnError().accept(t);

                    if (t instanceof RecipeRunException) {
                        RecipeRunException vt = (RecipeRunException) t;

                        //noinspection unchecked
                        afterFile = (S) new FindRecipeRunException(vt).visitNonNull(requireNonNull(afterFile), 0);
                    } else if (afterFile != null) {
                        // The applicable test threw an exception, but it was not in a visitor. It cannot be associated to any specific line of code,
                        // and instead we add a marker to the top of the source file to record the exception message.
                        afterFile = Markup.error(afterFile, t);
                    }

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
            runStats.ownGetVisitor.addAndGet(System.nanoTime() - getVisitorStartTime);
        }

        // The type of the list is widened at this point, since a source file type may be generated that isn't
        // of a type that is in the original set of source files (e.g. only XML files are given, and the
        // recipe generates Java code).
        List<SourceFile> afterWidened;
        try {
            long ownVisitStartTime = System.nanoTime();
            //noinspection unchecked
            afterWidened = recipe.visit((List<SourceFile>) after, ctx);
            runStats.ownVisit.addAndGet(System.nanoTime() - ownVisitStartTime);
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
                    List<Stack<Recipe>> recipeStackList = new ArrayList<>(1);
                    recipeStackList.add(recipeStack);
                    return s.withMarkers(s.getMarkers().computeByType(
                            new RecipesThatMadeChanges(randomId(), recipeStackList),
                            (r1, r2) -> {
                                r1.getRecipes().addAll(r2.getRecipes());
                                return r1;
                            }));
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
                nextStats = new RecipeRunStats(r);
                runStats.getCalled().add(nextStats);
            }

            afterWidened = scheduleVisit(requireNonNull(nextStats),
                nextStack,
                afterWidened,
                singleSourceApplicableTestResult,
                ctx, recipeThatAddedOrDeletedSourceFile);
        }

        long totalTime = System.nanoTime() - startTime;
        runStats.max.compareAndSet(Math.min(runStats.max.get(), totalTime), totalTime);
        runStats.cumulative.addAndGet(totalTime);

        //noinspection unchecked
        return (List<S>) afterWidened;
    }

    <T> CompletableFuture<T> schedule(Callable<T> fn);
}

class RecipeSchedulerUtils {
    public static <S extends SourceFile> S addRecipesThatMadeChanges(Stack<Recipe> recipeStack, S afterFile) {
        List<Stack<Recipe>> recipeStackList = new ArrayList<>(1);
        recipeStackList.add(recipeStack);
        afterFile = afterFile.withMarkers(afterFile.getMarkers().computeByType(
                new RecipesThatMadeChanges(randomId(), recipeStackList),
                (r1, r2) -> {
                    r1.getRecipes().addAll(r2.getRecipes());
                    return r1;
                }));
        return afterFile;
    }

    public static <S extends SourceFile> List<S> handleUncaughtException(Stack<Recipe> recipeStack,
                                                                         Map<UUID, Stack<Recipe>> recipeThatAddedOrDeletedSourceFile,
                                                                         List<S> before,
                                                                         ExecutionContext ctx,
                                                                         Recipe recipe,
                                                                         Throwable t) {
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
}

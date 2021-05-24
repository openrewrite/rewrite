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
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.Recipe.MARKER_ID_PRINTER;
import static org.openrewrite.Recipe.PANIC;
import static org.openrewrite.internal.ListUtils.mapAsync;

public interface RecipeScheduler {
    Logger LOGGER = LoggerFactory.getLogger(RecipeScheduler.class);

    default List<Result> scheduleRun(Recipe recipe,
                                     List<? extends SourceFile> srcFiles,
                                     ExecutionContext ctx,
                                     int maxCycles,
                                     int minCycles) {
        DistributionSummary.builder("rewrite.recipe.run")
                .tag("recipe", recipe.getDisplayName())
                .description("The distribution of recipe runs and the size of source file batches given to them to process.")
                .baseUnit("source files")
                .register(Metrics.globalRegistry)
                .record(srcFiles.size());

        Map<UUID, Recipe> recipeThatDeletedSourceFile = new HashMap<>();

        List<? extends SourceFile> before = srcFiles;
        List<? extends SourceFile> after = before;
        WatchableExecutionContext ctxWithWatch = new WatchableExecutionContext(ctx);
        for (int i = 0; i < maxCycles; i++) {
            after = scheduleVisit(recipe, before, ctxWithWatch, recipeThatDeletedSourceFile);
            if (i + 1 >= minCycles && ((after == before && !ctxWithWatch.hasNewMessages()) || !recipe.causesAnotherCycle())) {
                break;
            }
            before = after;
            ctxWithWatch.resetHasNewMessages();
        }

        if (after == before) {
            return emptyList();
        }

        Map<UUID, SourceFile> sourceFileIdentities = before.stream()
                .collect(toMap(SourceFile::getId, Function.identity()));

        List<Result> results = new ArrayList<>();

        // added or changed files
        for (SourceFile s : after) {
            SourceFile original = sourceFileIdentities.get(s.getId());
            if (original == s) {
                continue;
            }
            if (original == null) {
                results.add(new Result(null, s, singleton(recipeThatDeletedSourceFile.get(s.getId()))));
                continue;
            }

            //printing both the before and after (and including markers in the output) and then comparing the
            //output to determine if a change has been made.
            if (!original.print(MARKER_ID_PRINTER, ctx).equals(s.print(MARKER_ID_PRINTER, ctx))) {
                results.add(new Result(original, s, s.getMarkers()
                        .findFirst(Recipe.RecipeThatMadeChanges.class)
                        .orElseThrow(() -> new IllegalStateException("SourceFile changed but no recipe reported making a change?"))
                        .getRecipes()));
            }
        }
        Set<UUID> afterIds = after.stream()
                .map(SourceFile::getId)
                .collect(toSet());

        // removed files
        for (SourceFile s : before) {
            if (!afterIds.contains(s.getId())) {
                results.add(new Result(s, null, singleton(recipeThatDeletedSourceFile.get(s.getId()))));
            }
        }

        return results;
    }

    default <S extends SourceFile> List<S> scheduleVisit(Recipe recipe,
                                                         List<S> before,
                                                         ExecutionContext ctx,
                                                         Map<UUID, Recipe> recipeThatDeletedSourceFile) {
        long startTime = System.nanoTime();
        AtomicBoolean thrownErrorOnTimeout = new AtomicBoolean(false);

        if (recipe.getApplicableTest() != null) {
            boolean applicable = false;
            for (S s : before) {
                if (recipe.getApplicableTest().visit(s, ctx) != s) {
                    applicable = true;
                    break;
                }
            }

            if (!applicable) {
                return before;
            }
        }

        List<S> after = !recipe.validate(ctx).isValid()
                ? before
                : mapAsync(before, this, s -> {
            Timer.Builder timer = Timer.builder("rewrite.recipe.visit").tag("recipe", recipe.getDisplayName());
            Timer.Sample sample = Timer.start();

            if (recipe.getSingleSourceApplicableTest() != null) {
                if (recipe.getSingleSourceApplicableTest().visit(s, ctx) == s) {
                    sample.stop(MetricsHelper.successTags(timer, s, "skipped").register(Metrics.globalRegistry));
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
                sample.stop(MetricsHelper.successTags(timer, s, "timeout").register(Metrics.globalRegistry));
                return s;
            }

            if (ctx.getMessage(PANIC) != null) {
                return s;
            }

            try {
                @SuppressWarnings("unchecked") S afterFile = (S) recipe.getVisitor().visit(s, ctx);
                if (afterFile != null && afterFile != s) {
                    afterFile = afterFile.withMarkers(afterFile.getMarkers().computeByType(
                            new Recipe.RecipeThatMadeChanges(recipe),
                            (r1, r2) -> {
                                r1.getRecipes().addAll(r2.getRecipes());
                                return r1;
                            }));
                    sample.stop(MetricsHelper.successTags(timer, s, "changed").register(Metrics.globalRegistry));
                } else if (afterFile == null) {
                    recipeThatDeletedSourceFile.put(s.getId(), recipe);
                    sample.stop(MetricsHelper.successTags(timer, s, "deleted").register(Metrics.globalRegistry));
                } else {
                    sample.stop(MetricsHelper.successTags(timer, s, "unchanged").register(Metrics.globalRegistry));
                }
                return afterFile;
            } catch (Throwable t) {
                sample.stop(MetricsHelper.errorTags(timer, s, t).register(Metrics.globalRegistry));
                ctx.getOnError().accept(t);
                return s;
            }
        });

        // The type of the list is widened at this point, since a source file type may be generated that isn't
        // of a type that is in the original set of source files (e.g. only XML files are given, and the
        // recipe generates Java code).

        //noinspection unchecked
        List<SourceFile> afterWidened = recipe.visit((List<SourceFile>) after, ctx);

        for (SourceFile maybeGenerated : afterWidened) {
            //noinspection SuspiciousMethodCalls
            if (!after.contains(maybeGenerated)) {
                // a new source file generated
                recipeThatDeletedSourceFile.put(maybeGenerated.getId(), recipe);
            }
        }

        for (SourceFile maybeDeleted : after) {
            if (!afterWidened.contains(maybeDeleted)) {
                // a source file deleted
                recipeThatDeletedSourceFile.put(maybeDeleted.getId(), recipe);
            }
        }

        for (Recipe r : recipe.getRecipeList()) {
            if (ctx.getMessage(PANIC) != null) {
                //noinspection unchecked
                return (List<S>) afterWidened;
            }
            afterWidened = scheduleVisit(r, afterWidened, ctx, recipeThatDeletedSourceFile);
        }

        //noinspection unchecked
        return (List<S>) afterWidened;
    }

    <T> CompletableFuture<T> schedule(Callable<T> fn);

    CompletionStage<Void> schedule(Runnable fn);

}

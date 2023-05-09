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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.internal.FindRecipeRunException;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Generated;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SourcesFileErrors;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Recipe.PANIC;
import static org.openrewrite.RecipeSchedulerUtils.addRecipesThatMadeChanges;

public interface RecipeScheduler {

    default void afterCycle(SourceSet sourceSet) {
    }

    default RecipeRun scheduleRun(Recipe recipe,
                                  SourceSet sourceSet,
                                  ExecutionContext ctx,
                                  int maxCycles,
                                  int minCycles) {
        WatchableExecutionContext ctxWithWatch = new WatchableExecutionContext(ctx);

        List<Result> results = new ArrayList<>();
        RecipeRunStats recipeRunStats = new RecipeRunStats(Recipe.noop());
        SourcesFileErrors errorsTable = new SourcesFileErrors(Recipe.noop());

        SourceSet acc = sourceSet;
        SourceSet after = acc;

        for (int i = 1; i <= maxCycles; i++) {
            // this root cursor is shared by all `TreeVisitor` instances used created from `getVisitor` and
            // single source applicable tests so that data can be shared at the root (especially for caching
            // use cases like sharing a `JavaTypeCache` between `JavaTemplate` parsers).
            Cursor rootCursor = new Cursor(null, Cursor.ROOT_VALUE);

            RecipeRunCycle cycle = new RecipeRunCycle(this, recipe, rootCursor, ctxWithWatch,
                    recipeRunStats, errorsTable);

            // pre-transformation scanning phase where there can only be modifications to capture exceptions
            // occurring during the scanning phase
            after = cycle.scanSources(acc, i);

            // transformation phases
            after = cycle.generateSources(after, i);
            after = cycle.editSources(after, i);

            if (i >= minCycles &&
                ((after == acc && !ctxWithWatch.hasNewMessages()) || !recipe.causesAnotherCycle())) {
                break;
            }

            afterCycle(sourceSet);
            acc = after;
            ctxWithWatch.resetHasNewMessages();
        }

        return new RecipeRun(
                after.getChangeset(),
                ctx.getMessage(ExecutionContext.DATA_TABLES, emptyMap())
        );
    }

    <T> CompletableFuture<T> schedule(Callable<T> fn);
}

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class RecipeRunCycle {
    RecipeScheduler scheduler;
    Recipe recipe;
    Cursor rootCursor;
    ExecutionContext ctx;
    RecipeRunStats recipeRunStats;
    SourcesFileErrors errorsTable;

    long cycleStartTime = System.nanoTime();
    AtomicBoolean thrownErrorOnTimeout = new AtomicBoolean();

    public SourceSet scanSources(SourceSet before, int cycle) {
        return mapForRecipeRecursivelyIfAllSourceApplicable(before, (recipeStack, sourceFile) -> {
            Recipe recipe = recipeStack.peek();
            if (recipe.maxCycles() < cycle || !recipe.validate(ctx).isValid()) {
                return sourceFile;
            }

            SourceFile after = sourceFile;

            if (recipe instanceof ScanningRecipe) {
                try {
                    //noinspection unchecked
                    ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                    Object acc = scanningRecipe.getAccumulator(rootCursor);
                    recipeRunStats.recordScan(recipe, () -> {
                        TreeVisitor<?, ExecutionContext> scanner = scanningRecipe.getScanner(acc);
                        if (scanner.isAcceptable(sourceFile, ctx)) {
                            scanner.visit(sourceFile, ctx);
                        }
                        return sourceFile;
                    });
                } catch (Throwable t) {
                    after = handleError(recipe, sourceFile, after, t);
                }
            }
            return after;
        });
    }

    public SourceSet generateSources(SourceSet before, int cycle) {
        Stack<Stack<Recipe>> allRecipesStack = initRecipeStack();
        SourceSet acc = before;
        while (!allRecipesStack.isEmpty()) {
            Stack<Recipe> recipeStack = allRecipesStack.pop();
            Recipe recipe = recipeStack.peek();
            if (recipe.maxCycles() < cycle || !recipe.validate(ctx).isValid()) {
                continue;
            }

            if (recipe instanceof ScanningRecipe) {
                //noinspection unchecked
                ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                Collection<? extends SourceFile> generated = scanningRecipe.generate(scanningRecipe.getAccumulator(rootCursor), ctx);
                for (SourceFile g : generated) {
                    addRecipesThatMadeChanges(recipeStack, g);
                }
                acc = acc.concatAll(generated);
            }
            recurseRecipeList(allRecipesStack, recipeStack);
        }

        return acc;
    }

    public SourceSet editSources(SourceSet before, int cycle) {
        return mapForRecipeRecursivelyIfAllSourceApplicable(before, (recipeStack, sourceFile) -> {
            Recipe recipe = recipeStack.peek();
            if (recipe.maxCycles() < cycle || !recipe.validate(ctx).isValid()) {
                return sourceFile;
            }

            SourceFile after = sourceFile;

            try {
                Duration duration = Duration.ofNanos(System.nanoTime() - cycleStartTime);
                if (duration.compareTo(ctx.getMessage(ExecutionContext.RUN_TIMEOUT, Duration.ofMinutes(4))) > 0) {
                    if (thrownErrorOnTimeout.compareAndSet(false, true)) {
                        RecipeTimeoutException t = new RecipeTimeoutException(recipe);
                        ctx.getOnError().accept(t);
                        ctx.getOnTimeout().accept(t, ctx);
                    }
                    return sourceFile;
                }

                if (ctx.getMessage(PANIC) != null) {
                    return sourceFile;
                }

                TreeVisitor<?, ExecutionContext> visitor = recipe.getVisitor();
                visitor.cursor = rootCursor;

                after = recipeRunStats.recordEdit(recipe, () -> {
                    if (visitor.isAcceptable(sourceFile, ctx)) {
                        return (SourceFile) visitor.visit(sourceFile, ctx);
                    }
                    return sourceFile;
                });

                if (after == null) {
                    if (!sourceFile.getMarkers().findFirst(Generated.class).isPresent()) {
                        before.onDelete(sourceFile, recipeStack);
                    }
                }
            } catch (Throwable t) {
                after = handleError(recipe, sourceFile, after, t);
            }
            if (after != null && after != sourceFile) {
                after = addRecipesThatMadeChanges(recipeStack, after);
            }
            return after;
        });
    }

    @Nullable
    private SourceFile handleError(Recipe recipe, SourceFile sourceFile, @Nullable SourceFile after,
                                   Throwable t) {
        ctx.getOnError().accept(t);

        if (t instanceof RecipeRunException) {
            RecipeRunException vt = (RecipeRunException) t;
            after = (SourceFile) new FindRecipeRunException(vt).visitNonNull(requireNonNull(after, "after is null"), 0);
        }

        // Use the original source file to record the error, not the one that may have been modified by the visitor.
        // This is so the error is associated with the original source file, and its original source path.
        errorsTable.insertRow(ctx, new SourcesFileErrors.Row(
                sourceFile.getSourcePath().toString(),
                recipe.getName(),
                ExceptionUtils.sanitizeStackTrace(t, RecipeScheduler.class)
        ));

        return after;
    }

    private SourceSet mapForRecipeRecursivelyIfAllSourceApplicable(SourceSet before, BiFunction<Stack<Recipe>, SourceFile, SourceFile> mapFn) {
        return mapAsync(before, sourceFile -> {
            Stack<Stack<Recipe>> allRecipesStack = initRecipeStack();

            SourceFile acc = sourceFile;
            while (!allRecipesStack.isEmpty()) {
                Stack<Recipe> recipeStack = allRecipesStack.pop();
                acc = mapFn.apply(recipeStack, acc);
                recurseRecipeList(allRecipesStack, recipeStack);
            }

            return acc;
        });
    }

    private SourceSet mapAsync(SourceSet before, UnaryOperator<SourceFile> mapFn) {
        List<CompletableFuture<? extends SourceFile>> futures = new ArrayList<>();

        for (SourceFile sourceFile : before) {
            Callable<SourceFile> updateTreeFn = () -> mapFn.apply(sourceFile);
            futures.add(scheduler.schedule(updateTreeFn));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{})).join();
        Iterator<CompletableFuture<? extends SourceFile>> cfIterator = futures.iterator();
        return before.map((in) -> cfIterator.next().join());
    }

    private Stack<Stack<Recipe>> initRecipeStack() {
        Stack<Stack<Recipe>> allRecipesStack = new Stack<>();
        Stack<Recipe> rootRecipeStack = new Stack<>();
        rootRecipeStack.push(recipe);
        allRecipesStack.push(rootRecipeStack);
        return allRecipesStack;
    }

    private void recurseRecipeList(Stack<Stack<Recipe>> allRecipesStack, Stack<Recipe> recipeStack) {
        List<Recipe> recipeList = recipeStack.peek().getRecipeList();
        for (int i = recipeList.size() - 1; i >= 0; i--) {
            Recipe r = recipeList.get(i);
            if (ctx.getMessage(PANIC) != null) {
                break;
            }
            Stack<Recipe> nextStack = new Stack<>();
            nextStack.addAll(recipeStack);
            nextStack.push(r);
            allRecipesStack.push(nextStack);
        }
    }
}

class RecipeSchedulerUtils {
    public static <S extends SourceFile> S addRecipesThatMadeChanges(List<Recipe> recipeStack, S afterFile) {
        return afterFile.withMarkers(afterFile.getMarkers().computeByType(
                RecipesThatMadeChanges.create(recipeStack),
                (r1, r2) -> {
                    r1.getRecipes().addAll(r2.getRecipes());
                    return r1;
                })
        );
    }
}

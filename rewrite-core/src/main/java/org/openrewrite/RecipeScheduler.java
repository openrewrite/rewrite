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
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Recipe.PANIC;

public class RecipeScheduler {

    public void afterCycle(LargeSourceSet sourceSet) {
    }

    public RecipeRun scheduleRun(Recipe recipe,
                                 LargeSourceSet sourceSet,
                                 ExecutionContext ctx,
                                 int maxCycles,
                                 int minCycles) {
        WatchableExecutionContext ctxWithWatch = new WatchableExecutionContext(ctx);

        RecipeRunStats recipeRunStats = new RecipeRunStats(Recipe.noop());
        SourcesFileErrors errorsTable = new SourcesFileErrors(Recipe.noop());

        LargeSourceSet acc = sourceSet;
        LargeSourceSet after = acc;

        for (int i = 1; i <= maxCycles; i++) {
            // this root cursor is shared by all `TreeVisitor` instances used created from `getVisitor` and
            // single source applicable tests so that data can be shared at the root (especially for caching
            // use cases like sharing a `JavaTypeCache` between `JavaTemplate` parsers).
            Cursor rootCursor = new Cursor(null, Cursor.ROOT_VALUE);

            RecipeRunCycle cycle = new RecipeRunCycle(recipe, rootCursor, ctxWithWatch,
                    recipeRunStats, errorsTable);

            // pre-transformation scanning phase where there can only be modifications to capture exceptions
            // occurring during the scanning phase
            after = cycle.scanSources(after, i);

            // transformation phases
            after = cycle.generateSources(after, i);
            after = cycle.editSources(after, i);

            if (i >= minCycles &&
                ((after == acc && !ctxWithWatch.hasNewMessages()) || !recipe.causesAnotherCycle())) {
                break;
            }

            afterCycle(after);
            acc = after;
            ctxWithWatch.resetHasNewMessages();
        }

        return new RecipeRun(
                after.getChangeset(),
                ctx.getMessage(ExecutionContext.DATA_TABLES, emptyMap())
        );
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    static class RecipeRunCycle {
        Recipe recipe;
        Cursor rootCursor;
        ExecutionContext ctx;
        RecipeRunStats recipeRunStats;
        SourcesFileErrors errorsTable;

        long cycleStartTime = System.nanoTime();
        AtomicBoolean thrownErrorOnTimeout = new AtomicBoolean();

        public LargeSourceSet scanSources(LargeSourceSet sourceSet, int cycle) {
            return mapForRecipeRecursively(sourceSet, (recipeStack, sourceFile) -> {
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
                                scanner.visit(sourceFile, ctx, rootCursor);
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

        public LargeSourceSet generateSources(LargeSourceSet sourceSet, int cycle) {
            List<SourceFile> generatedInThisCycle = new ArrayList<>();

            Stack<Stack<Recipe>> allRecipesStack = initRecipeStack();
            LargeSourceSet acc = sourceSet;
            while (!allRecipesStack.isEmpty()) {
                Stack<Recipe> recipeStack = allRecipesStack.pop();
                Recipe recipe = recipeStack.peek();
                if (recipe.maxCycles() < cycle || !recipe.validate(ctx).isValid()) {
                    continue;
                }

                if (recipe instanceof ScanningRecipe) {
                    //noinspection unchecked
                    ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                    sourceSet.setRecipe(recipeStack);
                    List<SourceFile> generated = new ArrayList<>(scanningRecipe.generate(scanningRecipe.getAccumulator(rootCursor), generatedInThisCycle, ctx));
                    generatedInThisCycle.addAll(generated);
                    generated.replaceAll(source -> addRecipesThatMadeChanges(recipeStack, source));
                    acc = acc.generate(generated);
                }
                recurseRecipeList(allRecipesStack, recipeStack);
            }

            return acc;
        }

        public LargeSourceSet editSources(LargeSourceSet sourceSet, int cycle) {
            return mapForRecipeRecursively(sourceSet, (recipeStack, sourceFile) -> {
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

                    after = recipeRunStats.recordEdit(recipe, () -> {
                        if (visitor.isAcceptable(sourceFile, ctx)) {
                            // propagate shared root cursor
                            return (SourceFile) visitor.visit(sourceFile, ctx, rootCursor);
                        }
                        return sourceFile;
                    });

                    if (after != sourceFile) {
                        if (sourceFile.getMarkers().findFirst(Generated.class).isPresent()) {
                            // skip edits made to generated source files so that they don't show up in a diff
                            // that later fails to apply on a freshly cloned repository
                            return sourceFile;
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

        private LargeSourceSet mapForRecipeRecursively(LargeSourceSet sourceSet, BiFunction<Stack<Recipe>, SourceFile, SourceFile> mapFn) {
            return sourceSet.edit(sourceFile -> {
                Stack<Stack<Recipe>> allRecipesStack = initRecipeStack();

                SourceFile acc = sourceFile;
                while (!allRecipesStack.isEmpty()) {
                    Stack<Recipe> recipeStack = allRecipesStack.pop();
                    sourceSet.setRecipe(recipeStack);
                    acc = mapFn.apply(recipeStack, acc);
                    recurseRecipeList(allRecipesStack, recipeStack);
                }

                return acc;
            });
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

    private static <S extends SourceFile> S addRecipesThatMadeChanges(List<Recipe> recipeStack, S afterFile) {
        return afterFile.withMarkers(afterFile.getMarkers().computeByType(
                RecipesThatMadeChanges.create(recipeStack),
                (r1, r2) -> {
                    r1.getRecipes().addAll(r2.getRecipes());
                    return r1;
                })
        );
    }
}

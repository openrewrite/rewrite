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
package org.openrewrite.scheduling;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.internal.FindRecipeRunException;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Generated;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.Recipe.PANIC;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RecipeRunCycle {
    /**
     * The root recipe that is running, which may contain a recipe list which will
     * also be iterated as part of this cycle.
     */
    Recipe recipe;

    /**
     * The current cycle in the range [1, maxCycles].
     */
    @Getter
    int cycle;

    Cursor rootCursor;
    WatchableExecutionContext ctx;
    RecipeRunStats recipeRunStats;
    SourcesFileResults sourcesFileResults;
    SourcesFileErrors errorsTable;
    Map<Recipe, List<Recipe>> recipeLists = new IdentityHashMap<>();
    long cycleStartTime = System.nanoTime();
    AtomicBoolean thrownErrorOnTimeout = new AtomicBoolean();

    @Getter
    Set<Recipe> madeChangesInThisCycle = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * The zero-based position of the recipe that is currently doing a scan/generate/edit.
     */
    @NonFinal
    @Getter
    int recipePosition;

    public LargeSourceSet scanSources(LargeSourceSet sourceSet, int cycle) {
        return mapForRecipeRecursively(sourceSet, (recipeStack, sourceFile) -> {
            Recipe recipe = recipeStack.peek();
            if (sourceFile == null || recipe.maxCycles() < cycle) {
                return sourceFile;
            }

            SourceFile after = sourceFile;

            if (recipe instanceof ScanningRecipe) {
                try {
                    //noinspection unchecked
                    ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                    Object acc = scanningRecipe.getAccumulator(rootCursor, ctx);
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

        AtomicInteger recipePosition = new AtomicInteger(0);
        Stack<Stack<Recipe>> allRecipesStack = initRecipeStack();
        LargeSourceSet acc = sourceSet;
        while (!allRecipesStack.isEmpty()) {
            if (ctx.getMessage(PANIC) != null) {
                break;
            }

            Stack<Recipe> recipeStack = allRecipesStack.pop();
            Recipe recipe = recipeStack.peek();
            if (recipe.maxCycles() < cycle) {
                continue;
            }

            this.recipePosition = recipePosition.getAndIncrement();
            if (recipe instanceof ScanningRecipe) {
                //noinspection unchecked
                ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                sourceSet.setRecipe(recipeStack);
                List<SourceFile> generated = new ArrayList<>(scanningRecipe.generate(scanningRecipe.getAccumulator(rootCursor, ctx), generatedInThisCycle, ctx));
                generated.replaceAll(source -> addRecipesThatMadeChanges(recipeStack, source));
                generatedInThisCycle.addAll(generated);
                if (!generated.isEmpty()) {
                    madeChangesInThisCycle.add(recipe);
                }
            }
            recurseRecipeList(allRecipesStack, recipeStack);
        }

        acc = acc.generate(generatedInThisCycle);
        return acc;
    }

    public LargeSourceSet editSources(LargeSourceSet sourceSet, int cycle) {
        return mapForRecipeRecursively(sourceSet, (recipeStack, sourceFile) -> {
            Recipe recipe = recipeStack.peek();
            if (sourceFile == null || recipe.maxCycles() < cycle) {
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
                // set root cursor as it is required by the `ScanningRecipe#isAcceptable()`
                visitor.setCursor(rootCursor);

                after = recipeRunStats.recordEdit(recipe, () -> {
                    if (visitor.isAcceptable(sourceFile, ctx)) {
                        // propagate shared root cursor
                        return (SourceFile) visitor.visit(sourceFile, ctx, rootCursor);
                    }
                    return sourceFile;
                });

                if (after != sourceFile) {
                    madeChangesInThisCycle.add(recipe);
                    recordSourceFileResult(sourceFile, after, recipeStack, ctx);
                    if (sourceFile.getMarkers().findFirst(Generated.class).isPresent()) {
                        // skip edits made to generated source files so that they don't show up in a diff
                        // that later fails to apply on a freshly cloned repository
                        return sourceFile;
                    }
                    recipeRunStats.recordSourceFileChanged(sourceFile, after);
                } else if (ctx.hasNewMessages()) {
                    // consider any recipes adding new messages as a changing recipe (which can request another cycle)
                    madeChangesInThisCycle.add(recipe);
                    ctx.resetHasNewMessages();
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

    private void recordSourceFileResult(@Nullable SourceFile before, @Nullable SourceFile after, Stack<Recipe> recipeStack, ExecutionContext ctx) {
        String beforePath = (before == null) ? "" : before.getSourcePath().toString();
        String afterPath = (after == null) ? "" : after.getSourcePath().toString();
        Recipe recipe = recipeStack.peek();
        Long effortSeconds = (recipe.getEstimatedEffortPerOccurrence() == null) ? 0L : recipe.getEstimatedEffortPerOccurrence().getSeconds();
        String parentName = "";
        boolean hierarchical = recipeStack.size() > 1;
        if (hierarchical) {
            parentName = recipeStack.get(recipeStack.size() - 2).getName();
        }
        String recipeName = recipe.getName();
        sourcesFileResults.insertRow(ctx, new SourcesFileResults.Row(
                beforePath,
                afterPath,
                parentName,
                recipeName,
                effortSeconds,
                cycle));
        if (hierarchical) {
            recordSourceFileResult(beforePath, afterPath, recipeStack.subList(0, recipeStack.size() - 1), effortSeconds, ctx);
        }
    }

    private void recordSourceFileResult(@Nullable String beforePath, @Nullable String afterPath, List<Recipe> recipeStack, Long effortSeconds, ExecutionContext ctx) {
        if (recipeStack.size() <= 1) {
            // No reason to record the synthetic root recipe which contains the recipe run
            return;
        }
        String parentName;
        if (recipeStack.size() == 2) {
            // Record the parent name as blank rather than CompositeRecipe when the parent is the synthetic root recipe
            parentName = "";
        } else {
            parentName = recipeStack.get(recipeStack.size() - 2).getName();
        }
        Recipe recipe = recipeStack.get(recipeStack.size() - 1);
        sourcesFileResults.insertRow(ctx, new SourcesFileResults.Row(
                beforePath,
                afterPath,
                parentName,
                recipe.getName(),
                effortSeconds,
                cycle));
        recordSourceFileResult(beforePath, afterPath, recipeStack.subList(0, recipeStack.size() - 1), effortSeconds, ctx);
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

    private LargeSourceSet mapForRecipeRecursively(LargeSourceSet sourceSet,
                                                   BiFunction<Stack<Recipe>, @Nullable SourceFile, @Nullable SourceFile> mapFn) {
        AtomicInteger recipePosition = new AtomicInteger(0);
        return sourceSet.edit(sourceFile -> {
            Stack<Stack<Recipe>> allRecipesStack = initRecipeStack();
            SourceFile acc = sourceFile;
            while (!allRecipesStack.isEmpty()) {
                Stack<Recipe> recipeStack = allRecipesStack.pop();
                sourceSet.setRecipe(recipeStack);
                this.recipePosition = recipePosition.getAndIncrement();
                acc = mapFn.apply(recipeStack, acc);
                if (ctx.getMessage(PANIC) != null) {
                    break;
                }
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
        List<Recipe> recipeList = recipeLists.computeIfAbsent(recipeStack.peek(), Recipe::getRecipeList);
        for (int i = recipeList.size() - 1; i >= 0; i--) {
            Recipe r = recipeList.get(i);
            Stack<Recipe> nextStack = new Stack<>();
            nextStack.addAll(recipeStack);
            nextStack.push(r);
            allRecipesStack.push(nextStack);
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

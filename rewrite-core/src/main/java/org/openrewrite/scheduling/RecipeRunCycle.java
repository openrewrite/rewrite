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
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Recipe.PANIC;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RecipeRunCycle<LSS extends LargeSourceSet> {
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
    BiFunction<LSS, UnaryOperator<SourceFile>, LSS> sourceSetEditor;

    RecipeStack allRecipeStack = new RecipeStack();
    long cycleStartTime = System.nanoTime();
    AtomicBoolean thrownErrorOnTimeout = new AtomicBoolean();

    @Getter
    Set<Recipe> madeChangesInThisCycle = Collections.newSetFromMap(new IdentityHashMap<>());

    public int getRecipePosition() {
        return allRecipeStack.getRecipePosition();
    }

    public LSS scanSources(LSS sourceSet) {
        return sourceSetEditor.apply(sourceSet, sourceFile ->
                allRecipeStack.reduce(sourceSet, recipe, ctx, (source, recipeStack) -> {
                    Recipe recipe = recipeStack.peek();
                    if (source == null) {
                        return null;
                    }

                    SourceFile after = source;

                    if (recipe instanceof ScanningRecipe) {
                        try {
                            //noinspection unchecked
                            ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                            Object acc = scanningRecipe.getAccumulator(rootCursor, ctx);
                            recipeRunStats.recordScan(recipe, () -> {
                                TreeVisitor<?, ExecutionContext> scanner = scanningRecipe.getScanner(acc);
                                if (scanner.isAcceptable(source, ctx)) {
                                    scanner.visit(source, ctx, rootCursor);
                                }
                                return source;
                            });
                        } catch (Throwable t) {
                            after = handleError(recipe, source, after, t);
                        }
                    }
                    return after;
                }, sourceFile)
        );
    }

    public LSS generateSources(LSS sourceSet) {
        List<SourceFile> generatedInThisCycle = allRecipeStack.reduce(sourceSet, recipe, ctx, (acc, recipeStack) -> {
            Recipe recipe = recipeStack.peek();
            if (recipe instanceof ScanningRecipe) {
                //noinspection unchecked
                ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                List<SourceFile> generated = new ArrayList<>(scanningRecipe.generate(scanningRecipe.getAccumulator(rootCursor, ctx), unmodifiableList(acc), ctx));
                generated.replaceAll(source -> addRecipesThatMadeChanges(recipeStack, source));
                acc.addAll(generated);
                if (!generated.isEmpty()) {
                    madeChangesInThisCycle.add(recipe);
                }
            }
            return acc;
        }, new ArrayList<>());

        // noinspection unchecked
        return (LSS) sourceSet.generate(generatedInThisCycle);
    }

    public LSS editSources(LSS sourceSet) {
        // set root cursor as it is required by the `ScanningRecipe#isAcceptable()`
        // propagate shared root cursor
        // skip edits made to generated source files so that they don't show up in a diff
        // that later fails to apply on a freshly cloned repository
        // consider any recipes adding new messages as a changing recipe (which can request another cycle)
        return sourceSetEditor.apply(sourceSet, sourceFile ->
                allRecipeStack.reduce(sourceSet, recipe, ctx, (source, recipeStack) -> {
                    Recipe recipe = recipeStack.peek();
                    if (source == null) {
                        return null;
                    }

                    SourceFile after = source;

                    try {
                        Duration duration = Duration.ofNanos(System.nanoTime() - cycleStartTime);
                        if (duration.compareTo(ctx.getMessage(ExecutionContext.RUN_TIMEOUT, Duration.ofMinutes(4))) > 0) {
                            if (thrownErrorOnTimeout.compareAndSet(false, true)) {
                                RecipeTimeoutException t = new RecipeTimeoutException(recipe);
                                ctx.getOnError().accept(t);
                                ctx.getOnTimeout().accept(t, ctx);
                            }
                            return source;
                        }

                        if (ctx.getMessage(PANIC) != null) {
                            return source;
                        }

                        TreeVisitor<?, ExecutionContext> visitor = recipe.getVisitor();
                        // set root cursor as it is required by the `ScanningRecipe#isAcceptable()`
                        visitor.setCursor(rootCursor);

                        after = recipeRunStats.recordEdit(recipe, () -> {
                            if (visitor.isAcceptable(source, ctx)) {
                                // propagate shared root cursor
                                return (SourceFile) visitor.visit(source, ctx, rootCursor);
                            }
                            return source;
                        });

                        if (after != source) {
                            madeChangesInThisCycle.add(recipe);
                            recordSourceFileResult(source, after, recipeStack, ctx);
                            if (source.getMarkers().findFirst(Generated.class).isPresent()) {
                                // skip edits made to generated source files so that they don't show up in a diff
                                // that later fails to apply on a freshly cloned repository
                                return source;
                            }
                            recipeRunStats.recordSourceFileChanged(source, after);
                        } else if (ctx.hasNewMessages()) {
                            // consider any recipes adding new messages as a changing recipe (which can request another cycle)
                            madeChangesInThisCycle.add(recipe);
                            ctx.resetHasNewMessages();
                        }
                    } catch (Throwable t) {
                        after = handleError(recipe, source, after, t);
                    }
                    if (after != null && after != source) {
                        after = addRecipesThatMadeChanges(recipeStack, after);
                    }
                    return after;
                }, sourceFile)
        );
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

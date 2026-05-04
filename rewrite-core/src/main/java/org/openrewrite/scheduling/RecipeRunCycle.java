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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.internal.FindRecipeRunException;
import org.openrewrite.marker.Markup;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.*;
import org.openrewrite.quark.Quark;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SearchResults;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import org.openrewrite.marker.SearchResult;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RpcRecipe;
import org.openrewrite.rpc.request.BatchVisit;
import org.openrewrite.rpc.request.BatchVisitResponse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.ExecutionContext.SCANNING_MUTATION_VALIDATION;
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
    SearchResults searchResults;
    SourcesFileResults sourcesFileResults;
    SourcesFileErrors errorsTable;
    BiFunction<LSS, UnaryOperator<@Nullable SourceFile>, LSS> sourceSetEditor;

    RecipeStack allRecipeStack = new RecipeStack();
    long cycleStartTime = System.nanoTime();
    AtomicBoolean thrownErrorOnTimeout = new AtomicBoolean();

    @Getter
    Set<Recipe> madeChangesInThisCycle = newSetFromMap(new IdentityHashMap<>());

    public int getRecipePosition() {
        return allRecipeStack.getRecipePosition();
    }

    public LSS scanSources(LSS sourceSet) {
        if (isScanningRequired()) {
            return sourceSetEditor.apply(sourceSet, sourceFile -> {
                BatchState scanBatch = new BatchState();

                SourceFile result = allRecipeStack.reduce(sourceSet, recipe, ctx, (source, recipeStack) -> {
                    Recipe recipe = recipeStack.peek();
                    if (source == null) {
                        return null;
                    }

                    SourceFile after = source;

                    if (recipe instanceof ScanningRecipe) {
                        // Check if this is a batchable RPC scanning recipe
                        RewriteRpc currentRpc = recipe instanceof RpcRecipe ? ((RpcRecipe) recipe).getRpc() : null;
                        String scanVisitorName = recipe instanceof RpcRecipe ? ((RpcRecipe) recipe).getScanVisitor() : null;

                        if (currentRpc != null && scanVisitorName != null) {
                            // Flush if switching to a different RPC instance
                            if (scanBatch.rpc != null && scanBatch.rpc != currentRpc) {
                                flushScanBatch(scanBatch, source);
                            }

                            Recipe nextRecipe = allRecipeStack.getNextRecipe();
                            RewriteRpc nextRpc = nextRecipe instanceof RpcRecipe ? ((RpcRecipe) nextRecipe).getRpc() : null;
                            @Nullable String nextScanVisitor = nextRecipe instanceof RpcRecipe ? ((RpcRecipe) nextRecipe).getScanVisitor() : null;
                            boolean isInBatch = nextRpc == currentRpc && nextScanVisitor != null || scanBatch.rpc == currentRpc;

                            if (isInBatch) {
                                scanBatch.items.add(new BatchVisit.BatchVisitItem(scanVisitorName, null));
                                scanBatch.recipeStacks.add(recipeStack);
                                if (scanBatch.originalBeforeBatch == null) {
                                    scanBatch.originalBeforeBatch = source;
                                }
                                scanBatch.rpc = currentRpc;

                                // If this is the last recipe in the batch, flush now
                                if (nextRpc != currentRpc || nextScanVisitor == null) {
                                    flushScanBatch(scanBatch, source);
                                }
                                return source;
                            }
                        }

                        // Non-RPC or single-recipe path
                        try {
                            //noinspection unchecked
                            ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                            Object acc = scanningRecipe.getAccumulator(rootCursor, ctx);
                            recipeRunStats.recordScan(recipe, () -> {
                                TreeVisitor<?, ExecutionContext> scanner = scanningRecipe.getScanner(acc);
                                if (scanner.isAcceptable(source, ctx)) {
                                    Tree maybeMutated = scanner.visit(source, ctx, rootCursor);
                                    assert maybeMutated == source || !ctx.getMessage(SCANNING_MUTATION_VALIDATION, false) :
                                            "Edits made from within ScanningRecipe.getScanner() are discarded. " +
                                                    "The purpose of a scanner is to aggregate information for use in subsequent phases. " +
                                                    "Use ScanningRecipe.getVisitor() for making edits. " +
                                                    "To disable this warning set TypeValidation.immutableScanning to false in your tests.";
                                }
                                return source;
                            });
                        } catch (Throwable t) {
                            after = handleError(recipeStack, source, after, t);
                            // We don't normally consider anything the scanning phase does to be a change
                            // But this simplifies error reporting so that exceptions can all be handled the same
                            assert after != null;
                            after = addRecipesThatMadeChanges(recipeStack, after);
                        }
                    }
                    return after;
                }, sourceFile);

                // Flush any remaining scan batch
                if (scanBatch.rpc != null && result != null) {
                    flushScanBatch(scanBatch, result);
                }

                return result;
            });
        }
        return sourceSet;
    }

    private void flushScanBatch(BatchState batch, SourceFile source) {
        if (batch.rpc == null || batch.items.isEmpty()) {
            batch.clear();
            return;
        }

        try {
            // Send BatchVisit — no getObject needed for scan phase
            batch.rpc.batchVisit(source, ctx, rootCursor, batch.items);
        } catch (Throwable t) {
            if (!batch.recipeStacks.isEmpty()) {
                handleError(batch.recipeStacks.get(0), source, source, t);
            }
        }

        batch.clear();
    }

    public LSS generateSources(LSS sourceSet) {
        if (isScanningRequired()) {
            List<SourceFile> generatedInThisCycle = allRecipeStack.reduce(sourceSet, recipe, ctx, (acc, recipeStack) -> {
                Recipe recipe = recipeStack.peek();
                if (recipe instanceof ScanningRecipe) {
                    assert acc != null;
                    //noinspection unchecked
                    ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                    // If some sources have already been generated by prior recipes, scan them now
                    // This helps to avoid recipes having inconsistent knowledge of which files exist
                    if (!acc.isEmpty()) {
                        for (SourceFile source : acc) {
                            try {
                                recipeRunStats.recordScan(recipe, () -> {
                                    TreeVisitor<?, ExecutionContext> scanner = scanningRecipe.getScanner(scanningRecipe.getAccumulator(rootCursor, ctx));
                                    if (scanner.isAcceptable(source, ctx)) {
                                        scanner.visit(source, ctx, rootCursor);
                                    }
                                    return source;
                                });
                            } catch (Throwable t) {
                                handleError(recipeStack, source, source, t);
                            }
                        }
                    }
                    try {
                        List<SourceFile> generated = new ArrayList<>(scanningRecipe.generate(scanningRecipe.getAccumulator(rootCursor, ctx), unmodifiableList(acc), ctx));
                        generated.replaceAll(source -> addRecipesThatMadeChanges(recipeStack, source));
                        Set<Path> seenInThisBatch = new HashSet<>();
                        generated.removeIf(source -> {
                            Path sourcePath = source.getSourcePath();
                            if (sourceSet.getBefore(sourcePath) != null) {
                                sourceSet.onGenerateCollision(sourcePath, true);
                                return true;
                            }
                            for (SourceFile existing : acc) {
                                if (existing.getSourcePath().equals(sourcePath)) {
                                    sourceSet.onGenerateCollision(sourcePath, false);
                                    return true;
                                }
                            }
                            if (!seenInThisBatch.add(sourcePath)) {
                                sourceSet.onGenerateCollision(sourcePath, false);
                                return true;
                            }
                            return false;
                        });
                        if (!generated.isEmpty()) {
                            acc.addAll(generated);
                            generated.forEach(source -> recordSourceFileResultAndSearchResults(null, source, recipeStack, ctx));
                            madeChangesInThisCycle.add(recipe);
                        }
                    } catch (Throwable t) {
                        handleError(recipeStack, new Quark(Tree.randomId(), Paths.get("error during generation"), Markers.EMPTY, null, null), null, t);
                    }
                }
                return acc;
            }, new ArrayList<>());

            // noinspection unchecked
            return (LSS) sourceSet.generate(generatedInThisCycle);
        }
        return sourceSet;
    }

    public LSS editSources(LSS sourceSet) {
        //noinspection DataFlowIssue
        return sourceSetEditor.apply(sourceSet, sourceFile -> editSource(sourceSet, sourceFile)
        );
    }

    /**
     * Mutable state for tracking a batch of consecutive same-RPC recipes
     * that will be sent as a single BatchVisit RPC call.
     */
    private static class BatchState {
        @Nullable RewriteRpc rpc;
        final List<BatchVisit.BatchVisitItem> items = new ArrayList<>();
        final List<Stack<Recipe>> recipeStacks = new ArrayList<>();
        @Nullable SourceFile originalBeforeBatch;

        void clear() {
            rpc = null;
            items.clear();
            recipeStacks.clear();
            originalBeforeBatch = null;
        }
    }

    protected @Nullable SourceFile editSource(LSS sourceSet, SourceFile sourceFile) {
        recipeRunStats.recordSourceVisited(sourceFile);
        BatchState batch = new BatchState();

        SourceFile result = allRecipeStack.reduce(sourceSet, recipe, ctx, (source, recipeStack) -> {
            Recipe recipe = recipeStack.peek();
            if (source == null) {
                return null;
            }

            RewriteRpc currentRpc = recipe instanceof RpcRecipe ? ((RpcRecipe) recipe).getRpc() : null;

            // Flush batch if switching to a different RPC or non-RPC recipe
            if (batch.rpc != null && batch.rpc != currentRpc) {
                source = flushBatch(batch, source);
                if (source == null) {
                    return null;
                }
            }

            // Should we batch this recipe?
            // We're "in a batch" if this is an RPC recipe and either:
            //   (a) the next recipe uses the same RPC (batch continues), or
            //   (b) we're already in a batch with this RPC (batch ends here)
            Recipe nextRecipe = allRecipeStack.getNextRecipe();
            RewriteRpc nextRpc = nextRecipe instanceof RpcRecipe ? ((RpcRecipe) nextRecipe).getRpc() : null;
            boolean isInBatch = currentRpc != null && (nextRpc == currentRpc || batch.rpc == currentRpc);

            // Effectively-final copy for inner lambdas (source may have been reassigned by flush above)
            final SourceFile src = source;
            SourceFile after = src;

            try {
                Duration duration = Duration.ofNanos(System.nanoTime() - cycleStartTime);
                if (duration.compareTo(ctx.getMessage(ExecutionContext.RUN_TIMEOUT, Duration.ofMinutes(4))) > 0) {
                    if (thrownErrorOnTimeout.compareAndSet(false, true)) {
                        RecipeTimeoutException t = new RecipeTimeoutException(recipe);
                        ctx.getOnError().accept(wrapForAttribution(recipeStack, src.getSourcePath().toString(), t));
                        ctx.getOnTimeout().accept(t, ctx);
                    }
                    return src;
                }

                if (ctx.getMessage(PANIC) != null) {
                    return src;
                }

                if (isInBatch) {
                    // Batch path: accumulate visitor names instead of executing.
                    // Only batch if the remote actually has a codec for this source file type;
                    // otherwise the BatchVisit RPC fails on the remote side and the resulting
                    // exception gets attached to the source as a Markup$Error, falsely marking
                    // the file as modified.
                    if (currentRpc.getLanguages().contains(src.getClass().getName())) {
                        RpcRecipe rpcRecipe = (RpcRecipe) recipe;
                        batch.items.add(new BatchVisit.BatchVisitItem(rpcRecipe.getEditVisitor(), null));
                        batch.recipeStacks.add(recipeStack);
                        if (batch.originalBeforeBatch == null) {
                            batch.originalBeforeBatch = src;
                        }
                        batch.rpc = currentRpc;
                    }

                    // If this is the last recipe in the batch, flush now
                    if (nextRpc != currentRpc) {
                        return flushBatch(batch, src);
                    }

                    return src; // Continue accumulating
                }

                // Normal (non-batched) path
                TreeVisitor<?, ExecutionContext> visitor = recipe.getVisitor();
                // set root cursor as it is required by the `ScanningRecipe#isAcceptable()`
                visitor.setCursor(rootCursor);

                after = recipeRunStats.recordEdit(recipe, () -> {
                    if (visitor.isAcceptable(src, ctx)) {
                        // propagate shared root cursor
                        //noinspection DataFlowIssue
                        return (SourceFile) visitor.visit(src, ctx, rootCursor);
                    }
                    return src;
                });

                if (after != src) {
                    madeChangesInThisCycle.add(recipe);
                    recordSourceFileResultAndSearchResults(src, after, recipeStack, ctx);
                    if (src.getMarkers().findFirst(Generated.class).isPresent()) {
                        // skip edits made to generated source files so that they don't show up in a diff
                        // that later fails to apply on a freshly cloned repository
                        return src;
                    }
                    recipeRunStats.recordSourceFileChanged(src, after);
                } else if (ctx.hasNewMessages()) {
                    // consider any recipes adding new messages as a changing recipe (which can request another cycle)
                    madeChangesInThisCycle.add(recipe);
                    ctx.resetHasNewMessages();
                }
            } catch (Throwable t) {
                if (isInBatch) {
                    batch.clear();
                }
                after = handleError(recipeStack, src, after, t);
            }
            if (after != null && after != src) {
                after = addRecipesThatMadeChanges(recipeStack, after);
            }
            return after;
        }, sourceFile);

        // Flush any remaining batch at end of recipe list
        if (batch.rpc != null && result != null) {
            result = flushBatch(batch, result);
        }

        return result;
    }

    private @Nullable SourceFile flushBatch(BatchState batch, SourceFile source) {
        if (batch.rpc == null || batch.items.isEmpty()) {
            batch.clear();
            return source;
        }

        RewriteRpc rpc = batch.rpc;
        SourceFile originalBefore = batch.originalBeforeBatch != null ? batch.originalBeforeBatch : source;

        // Send BatchVisit RPC
        BatchVisitResponse response;
        try {
            response = rpc.batchVisit(originalBefore, ctx, rootCursor, batch.items);
        } catch (Throwable t) {
            if (!batch.recipeStacks.isEmpty()) {
                SourceFile beforeError = source;
                if (!(t instanceof RecipeRunException)) {
                    source = Markup.error(source, t);
                }
                source = handleError(batch.recipeStacks.get(0), originalBefore, source, t);
                if (source != null && source != beforeError) {
                    source = addRecipesThatMadeChanges(batch.recipeStacks.get(0), source);
                }
            }
            batch.clear();
            return source;
        }

        // Build attribution map: SearchResult UUID → recipe name
        Map<UUID, String> attributionMap = new HashMap<>();
        boolean anyModified = false;
        boolean deleted = false;

        for (int i = 0; i < response.getResults().size(); i++) {
            BatchVisitResponse.BatchVisitResult r = response.getResults().get(i);
            Stack<Recipe> recipeStack = batch.recipeStacks.get(i);
            Recipe recipe = recipeStack.peek();

            if (r.isModified() || r.isHasNewMessages()) {
                madeChangesInThisCycle.add(recipe);
            }

            if (r.isModified()) {
                anyModified = true;
            }

            if (r.isDeleted()) {
                deleted = true;
                madeChangesInThisCycle.add(recipe);
                break;
            }

            for (String searchResultId : r.getSearchResultIds()) {
                attributionMap.put(UUID.fromString(searchResultId), recipe.getName());
            }
        }

        if (deleted) {
            batch.clear();
            return null;
        }

        SourceFile fetched;
        if (anyModified) {
            // Fetch final tree state from remote
            fetched = rpc.getObject(originalBefore.getId().toString(), originalBefore.getClass().getName());
        } else {
            fetched = source;
        }

        if (anyModified) {
            // Collect ALL search results from the after tree ONCE, grouped by recipe.
            // This is O(treeSize) instead of O(recipes × treeSize).
            Map<String, List<SearchResults.Row>> searchResultsByRecipe =
                    collectAllBatchSearchResults(originalBefore, fetched, attributionMap);

            for (int i = 0; i < batch.recipeStacks.size(); i++) {
                if (response.getResults().get(i).isModified()) {
                    recordBatchSourceFileResultFast(originalBefore, fetched, batch.recipeStacks.get(i),
                            searchResultsByRecipe, ctx);
                }
            }
            if (!originalBefore.getMarkers().findFirst(Generated.class).isPresent()) {
                recipeRunStats.recordSourceFileChanged(originalBefore, fetched);
            }
            for (int i = 0; i < batch.recipeStacks.size(); i++) {
                if (response.getResults().get(i).isModified()) {
                    fetched = addRecipesThatMadeChanges(batch.recipeStacks.get(i), fetched);
                }
            }
        }

        batch.clear();
        return fetched;
    }

    /**
     * Collect all new SearchResult markers from the after tree, grouped by recipe name.
     * <p>
     * Uses a single fenced print of the entire file to extract code snippets for all
     * search results at once, instead of printing each matched subtree individually.
     * For files with many search results (common in batch mode), this reduces the cost
     * from O(searchResults × subtreeSize) prints to O(1) file print.
     * <p>
     * For files with 2 or fewer new search results, falls back to per-result printing
     * to avoid the overhead of parsing the fenced output.
     */
    private Map<String, List<SearchResults.Row>> collectAllBatchSearchResults(
            @Nullable SourceFile before, @Nullable SourceFile after,
            Map<UUID, String> attributionMap) {
        if (after == null) {
            return emptyMap();
        }

        // Collect markers already present in the before tree
        Set<UUID> alreadyPresentIds;
        if (before != null) {
            alreadyPresentIds = new TreeVisitor<Tree, Set<UUID>>() {
                @Override
                public <M extends Marker> M visitMarker(Marker marker, Set<UUID> ctx) {
                    if (marker instanceof SearchResult) {
                        ctx.add(marker.getId());
                    }
                    return super.visitMarker(marker, ctx);
                }
            }.reduce(before, new HashSet<>());
        } else {
            alreadyPresentIds = emptySet();
        }

        // Collect new SearchResult UUIDs and descriptions (single traversal, no printing)
        Map<UUID, String> newSearchResultDescriptions = new LinkedHashMap<>();
        new TreeVisitor<Tree, Integer>() {
            @Override
            public <M extends Marker> M visitMarker(Marker marker, Integer p) {
                if (marker instanceof SearchResult && !alreadyPresentIds.contains(marker.getId())) {
                    SearchResult sr = (SearchResult) marker;
                    if (attributionMap.containsKey(sr.getId())) {
                        newSearchResultDescriptions.put(sr.getId(), sr.getDescription());
                    }
                }
                return super.visitMarker(marker, p);
            }
        }.visit(after, 0);

        if (newSearchResultDescriptions.isEmpty()) {
            return emptyMap();
        }

        String beforePath = (before == null) ? "" : PathUtils.separatorsToUnix(before.getSourcePath().toString());
        String afterPath = PathUtils.separatorsToUnix(after.getSourcePath().toString());
        Map<String, List<SearchResults.Row>> resultsByRecipe = new HashMap<>();

        if (newSearchResultDescriptions.size() <= 2) {
            // Few results: per-result printing is cheaper than parsing fenced output
            for (Map.Entry<UUID, String> entry : newSearchResultDescriptions.entrySet()) {
                String creator = attributionMap.get(entry.getKey());
                if (creator != null) {
                    resultsByRecipe.computeIfAbsent(creator, k -> new ArrayList<>())
                            .add(new SearchResults.Row(beforePath, afterPath, "", entry.getValue(), creator));
                }
            }
        } else {
            // Many results: single fenced print, then extract snippets by UUID
            String fenced = after.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.FENCED));
            for (Map.Entry<UUID, String> entry : newSearchResultDescriptions.entrySet()) {
                UUID id = entry.getKey();
                String creator = attributionMap.get(id);
                if (creator == null) continue;

                String fence = "{{" + id + "}}";
                int start = fenced.indexOf(fence);
                if (start >= 0) {
                    int contentStart = start + fence.length();
                    int end = fenced.indexOf(fence, contentStart);
                    String snippet = (end > contentStart)
                            ? StringUtils.trimIndent(fenced.substring(contentStart, end))
                            : "";
                    resultsByRecipe.computeIfAbsent(creator, k -> new ArrayList<>())
                            .add(new SearchResults.Row(beforePath, afterPath, snippet, entry.getValue(), creator));
                } else {
                    resultsByRecipe.computeIfAbsent(creator, k -> new ArrayList<>())
                            .add(new SearchResults.Row(beforePath, afterPath, "", entry.getValue(), creator));
                }
            }
        }

        return resultsByRecipe;
    }

    /**
     * Records source file results for a batched recipe using pre-collected search results.
     * Avoids the O(recipes × treeSize) cost of traversing the tree per recipe.
     */
    private void recordBatchSourceFileResultFast(@Nullable SourceFile before, @Nullable SourceFile after,
                                                  Stack<Recipe> recipeStack,
                                                  Map<String, List<SearchResults.Row>> searchResultsByRecipe,
                                                  ExecutionContext ctx) {
        String beforePath = (before == null) ? "" : before.getSourcePath().toString();
        String afterPath = (after == null) ? "" : after.getSourcePath().toString();
        Recipe recipe = recipeStack.peek();
        Long effortSeconds = (recipe.getEstimatedEffortPerOccurrence() == null || Result.isLocalAndHasNoChanges(before, after)) ?
                0L : recipe.getEstimatedEffortPerOccurrence().getSeconds();

        String parentName = "";
        boolean hierarchical = recipeStack.size() > 1;
        if (hierarchical) {
            parentName = recipeStack.get(recipeStack.size() - 2).getName();
        }
        sourcesFileResults.insertRow(ctx, new SourcesFileResults.Row(
                beforePath,
                afterPath,
                parentName,
                recipe.getName(),
                effortSeconds,
                cycle));

        // Use pre-collected search results — O(1) lookup instead of O(treeSize) traversal
        List<SearchResults.Row> rows = searchResultsByRecipe.getOrDefault(recipe.getName(), emptyList());
        for (SearchResults.Row row : rows) {
            searchResults.insertRow(ctx, row);
        }

        if (hierarchical) {
            recordSourceFileResult(beforePath, afterPath, recipeStack.subList(0, recipeStack.size() - 1), ctx);
        }
    }

    /**
     * Records source file results and search results for a batched recipe,
     * using the attribution map to correctly assign search results to the recipe
     * that created them (rather than attributing all search results to every
     * recipe in the batch).
     */
    private void recordBatchSourceFileResult(@Nullable SourceFile before, @Nullable SourceFile after,
                                             Stack<Recipe> recipeStack,
                                             Map<UUID, String> attributionMap,
                                             ExecutionContext ctx) {
        String beforePath = (before == null) ? "" : before.getSourcePath().toString();
        String afterPath = (after == null) ? "" : after.getSourcePath().toString();
        Recipe recipe = recipeStack.peek();
        Long effortSeconds = (recipe.getEstimatedEffortPerOccurrence() == null || Result.isLocalAndHasNoChanges(before, after)) ?
                0L : recipe.getEstimatedEffortPerOccurrence().getSeconds();

        String parentName = "";
        boolean hierarchical = recipeStack.size() > 1;
        if (hierarchical) {
            parentName = recipeStack.get(recipeStack.size() - 2).getName();
        }
        sourcesFileResults.insertRow(ctx, new SourcesFileResults.Row(
                beforePath,
                afterPath,
                parentName,
                recipe.getName(),
                effortSeconds,
                cycle));

        // Use the attribution map: only include search results created by this recipe
        String recipeName = recipe.getName();
        List<SearchResults.Row> searchMarkers = collectSearchResults(before, after, recipeName, attributionMap);
        for (SearchResults.Row searchResult : searchMarkers) {
            searchResults.insertRow(ctx, searchResult);
        }

        if (hierarchical) {
            recordSourceFileResult(beforePath, afterPath, recipeStack.subList(0, recipeStack.size() - 1), ctx);
        }
    }

    protected void recordSourceFileResultAndSearchResults(@Nullable SourceFile before, @Nullable SourceFile after, Stack<Recipe> recipeStack, ExecutionContext ctx) {
        String beforePath = (before == null) ? "" : before.getSourcePath().toString();
        String afterPath = (after == null) ? "" : after.getSourcePath().toString();
        Recipe recipe = recipeStack.peek();
        Long effortSeconds = (recipe.getEstimatedEffortPerOccurrence() == null || Result.isLocalAndHasNoChanges(before, after)) ?
                0L : recipe.getEstimatedEffortPerOccurrence().getSeconds();

        String parentName = "";
        boolean hierarchical = recipeStack.size() > 1;
        if (hierarchical) {
            parentName = recipeStack.get(recipeStack.size() - 2).getName();
        }
        sourcesFileResults.insertRow(ctx, new SourcesFileResults.Row(
                beforePath,
                afterPath,
                parentName,
                recipe.getName(),
                effortSeconds,
                cycle));

        List<SearchResults.Row> searchMarkers = collectSearchResults(before, after, recipe.getInstanceName());
        for (SearchResults.Row searchResult : searchMarkers) {
            searchResults.insertRow(ctx, searchResult);
        }

        if (hierarchical) {
            recordSourceFileResult(beforePath, afterPath, recipeStack.subList(0, recipeStack.size() - 1), ctx);
        }
    }

    private void recordSourceFileResult(@Nullable String beforePath, @Nullable String afterPath, List<Recipe> recipeStack, ExecutionContext ctx) {
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
                0L, // Zero here, as we later sum only the recipes that themselves made changes
                cycle));
        recordSourceFileResult(beforePath, afterPath, recipeStack.subList(0, recipeStack.size() - 1), ctx);
    }

    private @Nullable SourceFile handleError(List<Recipe> recipeStack, SourceFile sourceFile, @Nullable SourceFile after,
                                             Throwable t) {
        ctx.getOnError().accept(wrapForAttribution(recipeStack, sourceFile.getSourcePath().toString(), t));

        if (t instanceof RecipeRunException && after != null) {
            try {
                RecipeRunException vt = (RecipeRunException) t;
                after = (SourceFile) new FindRecipeRunException(vt).visitNonNull(after, 0);
            } catch (Throwable ignored) {
                // Tree is too broken for node-level marker — fall back to marking the whole file
                after = Markup.error(after, t);
            }
        }

        // Use the original source file to record the error, not the one that may have been modified by the visitor.
        // This is so the error is associated with the original source file, and its original source path.
        errorsTable.insertRow(ctx, new SourcesFileErrors.Row(
                sourceFile.getSourcePath().toString(),
                recipeStack.get(recipeStack.size() - 1).getName(),
                ExceptionUtils.sanitizeStackTrace(t, RecipeScheduler.class)
        ));

        return after;
    }

    private static RecipeError wrapForAttribution(List<Recipe> recipeStack, @Nullable String sourcePath, Throwable t) {
        return new RecipeError(recipeStack.stream().map(Recipe::getName).collect(toList()), sourcePath, t);
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

    @NonFinal
    @Nullable
    transient Boolean isScanningRecipe;

    private boolean isScanningRequired() {
        if (isScanningRecipe == null) {
            isScanningRecipe = isScanningRequired(recipe);
        }
        return isScanningRecipe;
    }

    private static boolean isScanningRequired(Recipe recipe) {
        if (recipe instanceof ScanningRecipe) {
            // DeclarativeRecipe is technically a ScanningRecipe, but it only needs the
            // scanning phase if it or one of its sub-recipes or preconditions is a ScanningRecipe
            if (recipe instanceof DeclarativeRecipe) {
                for (Recipe precondition : ((DeclarativeRecipe) recipe).getPreconditions()) {
                    if (isScanningRequired(precondition)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        for (Recipe r : recipe.getRecipeList()) {
            if (isScanningRequired(r)) {
                return true;
            }
        }
        return false;
    }

    private List<SearchResults.Row> collectSearchResults(@Nullable SourceFile before, @Nullable SourceFile after, String recipeName) {
        return collectSearchResults(before, after, recipeName, null);
    }

    /**
     * Collects new SearchResult markers from the after tree.
     *
     * @param attributionMap When non-null, only includes search results whose UUID maps
     *                       to the given recipeName in the attribution map. This prevents
     *                       double-counting in batches where multiple recipes'
     *                       search results coexist in the same tree.
     */
    private List<SearchResults.Row> collectSearchResults(@Nullable SourceFile before, @Nullable SourceFile after,
                                                         String recipeName, @Nullable Map<UUID, String> attributionMap) {
        if (after == null) {
            return emptyList();
        }
        Set<SearchResult> alreadyPresentMarkers;
        if (before != null) {
            alreadyPresentMarkers = new TreeVisitor<Tree, Set<SearchResult>>() {
                @Override
                public <M extends Marker> M visitMarker(Marker marker, Set<SearchResult> ctx) {
                    if (marker instanceof SearchResult) {
                        ctx.add((SearchResult) marker);
                    }
                    return super.visitMarker(marker, ctx);
                }
            }.reduce(before, newSetFromMap(new IdentityHashMap<>()));
        } else {
            alreadyPresentMarkers = emptySet();
        }

        return new TreeVisitor<Tree, List<SearchResults.Row>>() {
            @Override
            public <M extends Marker> M visitMarker(Marker marker, List<SearchResults.Row> ctx) {
                if (marker instanceof SearchResult && !alreadyPresentMarkers.contains(marker)) {
                    SearchResult sr = (SearchResult) marker;
                    // If an attribution map is provided, only include search results
                    // that were created by this specific recipe
                    if (attributionMap != null) {
                        String creator = attributionMap.get(sr.getId());
                        if (creator == null || !creator.equals(recipeName)) {
                            return super.visitMarker(marker, ctx);
                        }
                    }
                    Cursor cursor = getCursor();
                    if (!(cursor.getValue() instanceof Tree)) {
                        cursor = cursor.getParentTreeCursor();
                    }
                    if (cursor.getValue() instanceof Tree) {
                        ctx.add(new SearchResults.Row(
                                (before == null) ? "" : PathUtils.separatorsToUnix(before.getSourcePath().toString()),
                                PathUtils.separatorsToUnix(after.getSourcePath().toString()),
                                StringUtils.trimIndent(((Tree) cursor.getValue()).print(getCursor(), new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED))),
                                sr.getDescription(),
                                recipeName));
                    }
                }
                return super.visitMarker(marker, ctx);
            }
        }.reduce(after, new ArrayList<>());
    }
}

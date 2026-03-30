/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc.request;

import io.moderne.jsonrpc.JsonRpcMethod;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.rpc.internal.PreparedRecipeCache;
import org.openrewrite.scheduling.RecipeRunCycle;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SearchResults;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import java.util.*;
import java.util.function.BiFunction;

@Value
public class BatchVisit implements RpcRequest {
    String sourceFileType;
    String treeId;
    String p;
    @Nullable List<String> cursor;
    List<BatchVisitItem> visitors;

    @Value
    public static class BatchVisitItem {
        String visitor;
        @Nullable Map<String, Object> visitorOptions;
    }

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<BatchVisit> {
        private final Map<String, Object> localObjects;
        private final PreparedRecipeCache preparedRecipes;
        private final BiFunction<String, @Nullable String, ?> getObject;
        private final BiFunction<@Nullable List<String>, @Nullable String, Cursor> getCursor;

        @Override
        protected Object handle(BatchVisit request) throws Exception {
            Tree tree = (Tree) getObject.apply(request.getTreeId(), request.getSourceFileType());
            Object p = getVisitorP(request);
            Cursor cursor = getCursor.apply(request.getCursor(), request.getSourceFileType());

            List<BatchVisitResponse.BatchVisitResult> results = new ArrayList<>();
            Set<String> knownIds = collectSearchResultIds(tree);

            for (BatchVisitItem item : request.getVisitors()) {
                // Instantiate and run visitor
                TreeVisitor<?, Object> visitor = preparedRecipes.instantiateVisitor(
                        item.getVisitor(), item.getVisitorOptions());
                Tree after = visitor.visit(tree, p, cursor);

                boolean modified = after != tree;
                boolean deleted = after == null;

                // Diff SearchResult IDs against the running set
                List<String> searchResultIds;
                if (deleted) {
                    searchResultIds = Collections.emptyList();
                } else {
                    Set<String> afterIds = collectSearchResultIds(after);
                    afterIds.removeAll(knownIds);
                    searchResultIds = new ArrayList<>(afterIds);
                    knownIds.addAll(searchResultIds);
                }

                results.add(new BatchVisitResponse.BatchVisitResult(
                        modified, deleted, false, searchResultIds));

                if (deleted) {
                    localObjects.remove(request.getTreeId());
                    break;
                }

                if (modified) {
                    tree = after;
                }
            }

            // Store final tree in localObjects for subsequent GetObject
            if (tree != null) {
                localObjects.put(tree.getId().toString(), tree);
            }

            return new BatchVisitResponse(results);
        }

        private Set<String> collectSearchResultIds(@Nullable Tree tree) {
            if (tree == null) {
                return new HashSet<>();
            }
            return new TreeVisitor<Tree, Set<String>>() {
                @Override
                public <M extends Marker> M visitMarker(Marker marker, Set<String> ctx) {
                    if (marker instanceof SearchResult) {
                        ctx.add(((SearchResult) marker).getId().toString());
                    }
                    return super.visitMarker(marker, ctx);
                }
            }.reduce(tree, new HashSet<>());
        }

        private Object getVisitorP(BatchVisit request) {
            Object p = getObject.apply(request.getP(), request.getSourceFileType());
            if (p instanceof ExecutionContext) {
                // Use the first visitor to determine recipe context
                if (!request.getVisitors().isEmpty()) {
                    String visitorName = request.getVisitors().get(0).getVisitor();
                    if (visitorName.startsWith("scan:") || visitorName.startsWith("edit:")) {
                        Recipe recipe = preparedRecipes.getInstantiated().get(
                                visitorName.substring("edit:".length()));
                        WatchableExecutionContext ctx = new WatchableExecutionContext((ExecutionContext) p);
                        ctx.putCycle(new RecipeRunCycle<>(recipe, 0, new Cursor(null, Cursor.ROOT_VALUE), ctx,
                                new RecipeRunStats(Recipe.noop()), new SearchResults(Recipe.noop()),
                                new SourcesFileResults(Recipe.noop()), new SourcesFileErrors(Recipe.noop()),
                                LargeSourceSet::edit));
                        ctx.putCurrentRecipe(recipe);
                        return ctx;
                    }
                }
            }
            return p;
        }
    }
}

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
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.internal.PreparedRecipeCache;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;

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
        private final RewriteRpc rpc;
        private final Map<String, Object> localObjects;
        private final PreparedRecipeCache preparedRecipes;
        private final BiFunction<String, @Nullable String, ?> getObject;
        private final BiFunction<@Nullable List<String>, @Nullable String, Cursor> getCursor;

        @Override
        protected Object handle(BatchVisit request) throws Exception {
            // Make this connection discoverable via RewriteRpc.current() while the visitors run.
            return rpc.withCurrent(() -> doHandle(request));
        }

        private Object doHandle(BatchVisit request) {
            Tree tree = (Tree) getObject.apply(request.getTreeId(), request.getSourceFileType());
            Object p = getObject.apply(request.getP(), request.getSourceFileType());
            Cursor cursor = getCursor.apply(request.getCursor(), request.getSourceFileType());

            List<BatchVisitResponse.BatchVisitResult> results = new ArrayList<>();
            Set<String> knownIds = collectSearchResultIds(tree);

            for (BatchVisitItem item : request.getVisitors()) {
                // Bind each item to its own recipe, while retaining the cycle shared by the batch.
                Object visitorP = Visit.visitorP(p, item.getVisitor(), preparedRecipes);
                TreeVisitor<?, Object> visitor = preparedRecipes.instantiateVisitor(
                        item.getVisitor(), item.getVisitorOptions());
                Tree after = visitor.visit(tree, visitorP, cursor);

                boolean modified = after != tree;
                boolean deleted = after == null;

                // Diff SearchResult IDs against the running set
                List<String> searchResultIds;
                if (deleted) {
                    searchResultIds = emptyList();
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
                    return new BatchVisitResponse(results);
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

    }
}

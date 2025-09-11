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
import org.openrewrite.rpc.internal.PreparedRecipeCache;
import org.openrewrite.scheduling.RecipeRunCycle;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Value
public class Visit implements RpcRequest {
    String visitor;

    @Nullable
    Map<String, Object> visitorOptions;

    String treeId;

    /**
     * An ID of the p value stored in the caller's local object cache.
     */
    String p;

    /**
     * A list of IDs representing the cursor whose objects are stored in the
     * caller's local object cache.
     */
    @Nullable
    List<String> cursor;

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<Visit> {
        private final Map<String, Object> localObjects;

        private final PreparedRecipeCache preparedRecipes;

        private final Function<String, ?> getObject;
        private final Function<@Nullable List<String>, Cursor> getCursor;

        @Override
        protected Object handle(Visit request) throws Exception {
            Tree before = (Tree) getObject.apply(request.getTreeId());
            Object p = getVisitorP(request);
            TreeVisitor<?, Object> visitor = preparedRecipes.instantiateVisitor(request.getVisitor(),
                    request.getVisitorOptions());
            Tree after = visitor.visit(before, p, getCursor.apply(request.getCursor()));
            if (after == null) {
                localObjects.remove(before.getId().toString());
            } else {
                localObjects.put(after.getId().toString(), after);
            }

            return new VisitResponse(before != after);
        }

        private Object getVisitorP(Visit request) {
            Object p = getObject.apply(request.getP());
            if (p instanceof ExecutionContext) {
                String visitorName = request.getVisitor();

                if (visitorName.startsWith("scan:") || visitorName.startsWith("edit:")) {
                    Recipe recipe = preparedRecipes.getInstantiated().get(visitorName.substring(
                            "edit:".length() /* 'scan:' has same length*/));
                    // This is really probably particular to the Java implementation,
                    // because we are carrying forward the legacy of cycles that are likely to be
                    // removed from OpenRewrite in the future.
                    WatchableExecutionContext ctx = new WatchableExecutionContext((ExecutionContext) p);
                    ctx.putCycle(new RecipeRunCycle<>(recipe, 0, new Cursor(null, Cursor.ROOT_VALUE), ctx,
                            new RecipeRunStats(Recipe.noop()), new SourcesFileResults(Recipe.noop()),
                            new SourcesFileErrors(Recipe.noop()), LargeSourceSet::edit));
                    ctx.putCurrentRecipe(recipe);
                    return ctx;
                }
            }
            return p;
        }
    }
}

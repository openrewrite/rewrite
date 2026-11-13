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
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.internal.PreparedRecipeCache;
import org.openrewrite.scheduling.RecipeRunCycle;
import org.openrewrite.scheduling.WatchableExecutionContext;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SearchResults;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Value
public class Visit implements RpcRequest {
    String visitor;

    @Nullable String sourceFileType;

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
        private final RewriteRpc rpc;
        private final Map<String, Object> localObjects;
        private final PreparedRecipeCache preparedRecipes;
        private final BiFunction<String, @Nullable String, ?> getObject;
        private final BiFunction<@Nullable List<String>, @Nullable String, Cursor> getCursor;

        @Override
        protected Object handle(Visit request) throws Exception {
            // Make this connection discoverable via RewriteRpc.current() while the visitor runs.
            return rpc.withCurrent(() -> {
                Tree before = (Tree) getObject.apply(request.getTreeId(), request.getSourceFileType());
                Object p = getVisitorP(request);
                TreeVisitor<?, Object> visitor = preparedRecipes.instantiateVisitor(request.getVisitor(),
                        request.getVisitorOptions());
                Tree after = visitor.visit(before, p, getCursor.apply(request.getCursor(),
                        request.getSourceFileType()));
                if (after == null) {
                    localObjects.remove(before.getId().toString());
                } else {
                    localObjects.put(after.getId().toString(), after);
                }

                return new VisitResponse(before != after);
            });
        }

        private Object getVisitorP(Visit request) {
            Object p = getObject.apply(request.getP(), request.getSourceFileType());
            return visitorP(p, request.getVisitor(), preparedRecipes);
        }
    }

    private static final String RPC_CYCLE = Visit.class.getName() + ".cycle";

    enum Phase { SCAN, GENERATE, EDIT }

    static Object visitorP(Object p, String visitor, PreparedRecipeCache preparedRecipes) {
        if (p instanceof ExecutionContext && (visitor.startsWith("scan:") || visitor.startsWith("edit:"))) {
            Recipe recipe = preparedRecipes.getInstantiated().get(visitor.substring("edit:".length()));
            return recipeContext((ExecutionContext) p, recipe,
                    visitor.startsWith("scan:") ? Phase.SCAN : Phase.EDIT, preparedRecipes);
        }
        return p;
    }

    /** One remote scheduling cycle spans all recipes' scan, generate and edit requests. */
    static ExecutionContext recipeContext(ExecutionContext p, Recipe recipe, Phase phase,
                                          PreparedRecipeCache preparedRecipes) {
        WatchableExecutionContext ctx = new WatchableExecutionContext(p);
        RpcCycle state = ctx.getMessage(RPC_CYCLE);
        RecipeRunCycle<?> cycle = ctx.getMessage(ExecutionContext.CURRENT_CYCLE);
        if (state != null && state.cycle != cycle) {
            state.clearAccumulators(preparedRecipes);
            state = null;
        }
        if (state == null) {
            if (cycle == null) {
                cycle = newCycle(recipe, ctx, 1);
                ctx.putCycle(cycle);
            }
            state = new RpcCycle(cycle);
            ctx.putMessage(RPC_CYCLE, state);
        }
        // A recipe scans before generating once per cycle. Its next scan/generate after generation or
        // editing starts a new cycle, including when a precondition skipped its edit. A different
        // recipe's first scan/generate still belongs to this cycle.
        Phase previous = state.phases.get(recipe);
        if (phase != Phase.EDIT && previous != null && previous != Phase.SCAN) {
            state.clearAccumulators(preparedRecipes);
            state = new RpcCycle(newCycle(recipe, ctx, state.cycle.getCycle() + 1));
            ctx.putCycle(state.cycle);
            ctx.putMessage(RPC_CYCLE, state);
        }
        state.phases.put(recipe, phase);
        ctx.putCurrentRecipe(recipe);
        return ctx;
    }

    private static RecipeRunCycle<?> newCycle(Recipe recipe, WatchableExecutionContext ctx, int cycle) {
        return new RecipeRunCycle<>(recipe, cycle, new Cursor(null, Cursor.ROOT_VALUE), ctx,
                new RecipeRunStats(Recipe.noop()), new SearchResults(Recipe.noop()),
                new SourcesFileResults(Recipe.noop()), new SourcesFileErrors(Recipe.noop()),
                LargeSourceSet::edit);
    }

    private static final class RpcCycle {
        final RecipeRunCycle<?> cycle;
        final Map<Recipe, Phase> phases = new IdentityHashMap<>();

        private RpcCycle(RecipeRunCycle<?> cycle) {
            this.cycle = cycle;
        }

        private void clearAccumulators(PreparedRecipeCache preparedRecipes) {
            for (Recipe recipe : phases.keySet()) {
                preparedRecipes.getRecipeCursors().remove(recipe);
            }
        }
    }
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static org.openrewrite.ExecutionContext.CURRENT_RECIPE;

@Value
public class Generate implements RpcRequest {
    String id;

    /**
     * An ID of the p value stored in the caller's local object cache.
     */
    String p;

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<Generate> {
        private final Map<String, Object> localObjects;
        private final PreparedRecipeCache preparedRecipes;
        private final BiFunction<String, @Nullable String, ?> getObject;

        @Override
        protected Object handle(Generate request) throws Exception {
            Recipe recipe = preparedRecipes.getInstantiated().get(request.getId());

            ExecutionContext ctx = (ExecutionContext) getObject.apply(request.getP(), null);
            if (ctx.getMessage(CURRENT_RECIPE) == null) {
                WatchableExecutionContext wctx = new WatchableExecutionContext(ctx);
                wctx.putCycle(new RecipeRunCycle<>(recipe, 0, new Cursor(null, Cursor.ROOT_VALUE), wctx,
                        new RecipeRunStats(Recipe.noop()), new SourcesFileResults(Recipe.noop()),
                        new SourcesFileErrors(Recipe.noop()), LargeSourceSet::edit));
                ctx.putCurrentRecipe(recipe);
            }

            if (recipe instanceof ScanningRecipe) {
                //noinspection unchecked
                ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
                Object acc = scanningRecipe.getAccumulator(preparedRecipes.getRecipeCursors().computeIfAbsent(recipe,
                        r -> new Cursor(null, Cursor.ROOT_VALUE)), ctx);
                Collection<? extends SourceFile> generated = scanningRecipe.generate(acc, ctx);

                GenerateResponse response = new GenerateResponse(
                        new ArrayList<>(generated.size()),
                        new ArrayList<>(generated.size())
                );
                for (SourceFile g : generated) {
                    localObjects.put(g.getId().toString(), g);
                    response.getIds().add(g.getId().toString());
                    response.getSourceFileTypes().add(g.getClass().getName());
                }
                return response;
            }
            return new GenerateResponse(emptyList(), emptyList());
        }
    }
}

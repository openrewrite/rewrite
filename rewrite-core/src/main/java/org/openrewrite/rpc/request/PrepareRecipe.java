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
import io.moderne.jsonrpc.internal.SnowflakeId;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.rpc.internal.PreparedRecipeCache;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;

@Value
public class PrepareRecipe implements RpcRequest {
    String id;
    Map<String, Object> options;

    public interface Loader {
        Recipe load(String id, Map<String, Object> options) throws Exception;
    }

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<PrepareRecipe> {
        private final PreparedRecipeCache preparedRecipes;
        private final AtomicReference<@Nullable Loader> recipeLoader;

        @Override
        protected Object handle(PrepareRecipe request) throws Exception {
            Loader loader = recipeLoader.get();
            if (loader == null) {
                loader = (recipeId, options) -> new RecipeLoader(null).load(recipeId, options);
            }
            Recipe recipe = loader.load(request.id, request.getOptions());
            String instanceId = SnowflakeId.generateId();
            preparedRecipes.getInstantiated().put(instanceId, recipe);
            return new PrepareRecipeResponse(
                    instanceId,
                    recipe.getDescriptor(),
                    "edit:" + instanceId,
                    // Making this non-empty would only be valuable if a non-Java process was controlling
                    // recipe execution and there would then be some benefit to it preempting the execution
                    // of the edit visitor in this Java RPC process. Same for the scan precondition visitor below.
                    emptyList(),
                    recipe instanceof ScanningRecipe ? "scan:" + instanceId : null,
                    emptyList()
            );
        }
    }
}

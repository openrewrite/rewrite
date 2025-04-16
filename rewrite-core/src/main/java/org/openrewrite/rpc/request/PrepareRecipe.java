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
import org.openrewrite.Recipe;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.internal.RecipeLoader;

import java.util.Map;

@Value
public class PrepareRecipe implements RpcRequest {
    String id;
    Map<String, Object> options;

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<PrepareRecipe> {
        private final Map<String, Recipe> preparedRecipes;

        @Override
        protected Object handle(PrepareRecipe request) throws Exception {
            Recipe recipe = new RecipeLoader(null).load(request.getId(), request.getOptions());
            String instanceId = SnowflakeId.generateId();
            preparedRecipes.put(instanceId, recipe);
            return new PrepareRecipeResponse(instanceId, recipe.getDescriptor(),
                    "edit:" + instanceId,
                    recipe instanceof ScanningRecipe ? "scan:" + instanceId : null);
        }
    }
}

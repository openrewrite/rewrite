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

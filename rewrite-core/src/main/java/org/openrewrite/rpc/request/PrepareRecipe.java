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
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.lang.NullUtils;
import org.openrewrite.rpc.internal.PreparedRecipeCache;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;

@Value
public class PrepareRecipe implements RpcRequest {
    String id;
    Map<String, Object> options;

    /**
     * Set by hosts that understand {@link PrepareRecipeResponse#getCausesAnotherCycle()}. Older hosts reject
     * unknown response fields, so remotes only send it when asked.
     */
    @Nullable
    Boolean acceptsCausesAnotherCycle;

    public interface Loader {
        Recipe load(String id, Map<String, Object> options) throws Exception;
    }

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<PrepareRecipe> {
        private final PreparedRecipeCache preparedRecipes;
        private final Loader recipeLoader;

        @Override
        protected Object handle(PrepareRecipe request) throws Exception {
            Recipe recipe = recipeLoader.load(request.id, request.getOptions());
            return prepareTree(recipe, preparedRecipes, Boolean.TRUE.equals(request.acceptsCausesAnotherCycle));
        }

        static PrepareRecipeResponse prepareTree(Recipe recipe, PreparedRecipeCache preparedRecipes,
                                                 boolean acceptsCausesAnotherCycle) {
            String instanceId = SnowflakeId.generateId();
            preparedRecipes.getInstantiated().put(instanceId, recipe);
            List<PrepareRecipeResponse> children = new ArrayList<>();
            for (Recipe child : recipe.getRecipeList()) {
                children.add(prepareTree(child, preparedRecipes, acceptsCausesAnotherCycle));
            }
            RecipeDescriptor descriptor = recipe.getDescriptor();

            // Names come from the descriptor, required-ness from where Recipe#validate() gets it.
            // The two disagree: `@Option.required()` defaults to true and the descriptor copies it
            // verbatim, while NullUtils skips a @Nullable field before looking at @Option at all.
            // The descriptor alone would also reject an option declared on a getter or constructor
            // parameter outright, since it hardcodes a null value for those.
            Set<String> requiredFields = new HashSet<>();
            for (Field field : NullUtils.findNonNullFields(recipe.getClass())) {
                requiredFields.add(field.getName());
            }
            List<PrepareRecipeResponse.OptionValidation> validations = new ArrayList<>();
            for (OptionDescriptor option : descriptor.getOptions()) {
                if (option.getValue() == null && requiredFields.contains(option.getName())) {
                    validations.add(new PrepareRecipeResponse.OptionValidation(option.getName(), "is required"));
                }
            }

            return new PrepareRecipeResponse(
                    instanceId,
                    descriptor,
                    "edit:" + instanceId,
                    // Making this non-empty would only be valuable if a non-Java process was controlling
                    // recipe execution and there would then be some benefit to it preempting the execution
                    // of the edit visitor in this Java RPC process. Same for the scan precondition visitor below.
                    emptyList(),
                    recipe instanceof ScanningRecipe ? "scan:" + instanceId : null,
                    emptyList(),
                    null,
                    children,
                    acceptsCausesAnotherCycle && recipe.causesAnotherCycle() ? true : null,
                    validations.isEmpty() ? null : validations);
        }
    }
}

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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.RecipeDescriptor;

import java.util.List;
import java.util.Map;

@Value
public class PrepareRecipeResponse {
    /**
     * The ID that the remote is using to refer to a
     * specific instance of the recipe.
     */
    String id;

    RecipeDescriptor descriptor;
    String editVisitor;
    List<Precondition> editPreconditions;

    @Nullable
    String scanVisitor;

    List<Precondition> scanPreconditions;

    /**
     * When non-null, the remote declares that this recipe delegates entirely
     * to a Java recipe. The host should load the recipe locally via the
     * marketplace instead of wrapping it in an RpcRecipe.
     */
    @Nullable
    DelegatesTo delegatesTo;

    /**
     * The fully-prepared child recipe responses returned by the server as part of the
     * whole-tree prepare response. When non-null and non-empty, the host builds
     * {@link org.openrewrite.rpc.RpcRecipe} children locally from these nodes instead
     * of making individual PrepareRecipe RPC calls.
     */
    @Nullable
    List<PrepareRecipeResponse> recipeList;

    @Value
    public static class DelegatesTo {
        String recipeName;
        Map<String, Object> options;
    }

    /**
     * Either a leaf (a single visitor identified by {@code visitorName} +
     * {@code visitorOptions}) or a composite of nested preconditions joined
     * by {@code op} ({@code "or"}, {@code "and"}, {@code "not"}). When
     * {@code op} is null the entry is a leaf and the visitor fields are
     * required; when {@code op} is set, {@code operands} carries the children
     * and the visitor fields are ignored. The composite form mirrors Java's
     * {@link org.openrewrite.Preconditions#or}/{@code and}/{@code not} so
     * remote languages can express the same gate shapes the Java side does.
     */
    @Value
    public static class Precondition {
        @Nullable
        String visitorName;

        @Nullable
        Map<String, Object> visitorOptions;

        @Nullable
        String op;

        @Nullable
        List<Precondition> operands;
    }
}

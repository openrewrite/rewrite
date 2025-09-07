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

    @Value
    public static class Precondition {
        String visitorName;
        Map<String, Object> visitorOptions;
    }
}

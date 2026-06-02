/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.GetMarketplaceResponse;

import java.util.Collections;
import java.util.List;

/**
 * Response to an {@code InstallRecipes} RPC request.
 * <p>
 * {@code recipes} carries the descriptor rows for recipes attributed to the
 * just-installed distribution, so the caller can construct a
 * {@link org.openrewrite.marketplace.RecipeMarketplace} bound to the bundle
 * without a follow-up {@code GetMarketplace} round trip. The follow-up call
 * was the source of the prior over-attribution bug — it returned the entire
 * singleton marketplace, which on a Python RPC server includes built-in
 * {@code openrewrite} recipes plus any recipes from previously-installed
 * sibling packages, all of which were then incorrectly tagged with the
 * just-requested bundle.
 */
@Value
public class InstallRecipesResponse {
    int recipesInstalled;
    @Nullable String version;
    @Nullable List<GetMarketplaceResponse.Row> recipes;

    public List<GetMarketplaceResponse.Row> recipesOrEmpty() {
        return recipes == null ? Collections.emptyList() : recipes;
    }
}

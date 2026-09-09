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

/**
 * Response to an {@code InstallRecipes} RPC request. The reader gets the bundle's recipes from a
 * follow-up {@code GetMarketplace} call, whose rows now carry each recipe's origin
 * {@code packageName} so the host attributes each to its own bundle — so this response only needs
 * to report how many recipes were installed and the resolved version.
 */
@Value
public class InstallRecipesResponse {
    int recipesInstalled;
    @Nullable String version;
}

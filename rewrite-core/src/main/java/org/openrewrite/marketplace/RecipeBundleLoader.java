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
package org.openrewrite.marketplace;

import org.jspecify.annotations.Nullable;

/**
 * Factory interface for creating RecipeBundle instances from CSV data.
 * Implementations are passed to RecipeMarketplaceReader constructor for specific package ecosystems (e.g., "Maven", "NPM", "YAML").
 * Implementations may require runtime dependencies (e.g., ExecutionContext) and should be constructed by the calling code.
 */
public interface RecipeBundleLoader {
    /**
     * @return The package ecosystem this factory handles (e.g., "Maven", "NPM", "YAML").
     * Case-insensitive matching is used.
     */
    String getEcosystem();

    /**
     * Create a bundle from CSV data.
     *
     * @param packageName The package name/identifier
     * @param version The package version
     * @param team Optional team identifier
     * @return A RecipeBundle instance, or null if this factory cannot create a bundle for the given parameters
     */
    @Nullable
    RecipeBundle createBundle(String packageName, String version, @Nullable String team);
}

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
package org.openrewrite.javascript.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

@Value
public class InstallRecipesResponse {
    int recipesInstalled;
    @Nullable String version;
    /**
     * Registry URLs that {@code npm install} touched while installing this bundle and
     * its transitive closure. Derived from the {@code resolved} field of each entry in
     * the resulting {@code package-lock.json}. URLs are returned as the worker observed
     * them (e.g. {@code https://registry.npmjs.org}); normalization is the caller's
     * responsibility.
     */
    @Nullable Set<String> resolvedFromRepositories;

    public Set<String> resolvedFromRepositoriesOrEmpty() {
        return resolvedFromRepositories == null ? Collections.emptySet() : resolvedFromRepositories;
    }
}

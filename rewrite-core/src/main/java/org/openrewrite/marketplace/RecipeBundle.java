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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.RpcRequest;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecipeBundle implements RpcRequest {
    @EqualsAndHashCode.Include @Nullable String packageEcosystem;
    @EqualsAndHashCode.Include @Nullable String packageName;

    /**
     * May be a dynamic constraint like LATEST or 0.2.0-SNAPSHOT, resolved in a way that is
     * {@link RecipeBundleResolver} specific. Null when no constraint was requested.
     */
    @With @Nullable String requestedVersion;

    @With @Nullable String version;
    @Nullable String team;

    /**
     * This may seem a bit backwards here, but the intent is for resolution to be repeatable.
     * Only when version is null do we resolve the requested version. That resolved version
     * will then be set on version so that subsequent installations of the same bundle result
     * in a repeatable outcome.
     */
    public @Nullable String getVersion() {
        return version == null ? requestedVersion : version;
    }

    /**
     * @return Bundle that corresponds to {@link org.openrewrite.config.Environment.Builder#scanRuntimeClasspath(String...)}.
     */
    public static RecipeBundle runtimeClasspath() {
        return new RecipeBundle("runtime", "",
                null, null, null);
    }
}

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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.RpcRequest;

@Data
@AllArgsConstructor
public class RecipeBundle implements RpcRequest {
    String packageEcosystem;
    String packageName;
    @Nullable String requestedVersion;
    @Nullable String version;
    @Nullable String team;

    /**
     * A requested version could be a dynamic constraint like LATEST or 0.2.0-SNAPSHOT. The way
     * in which a dynamic constraint is resolved is {@link RecipeBundleResolver} specific, but
     * the possibility of dynamic version constraints is a concept that several resolvers share.
     * <br>
     * To prevent subtle bugs where, for example, version is set but requested version is not and
     * a resolver is attempting to read the version on this bundle, we fall back on the version
     * if a bundle has been constructed with a version but no requested version.
     *
     * @return The requested version, or {@link #getVersion()} if no requested version has been set.
     */
    public @Nullable String getRequestedVersion() {
        return requestedVersion == null ? version : requestedVersion;
    }

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

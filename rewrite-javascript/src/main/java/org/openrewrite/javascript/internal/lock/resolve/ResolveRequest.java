/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.javascript.internal.lock.resolve;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.Map;

/**
 * The input to a {@link LockResolver}: the edited manifests to resolve, the existing lock (for the version
 * details and lock-format specifics a resolver must preserve, e.g. berry's {@code cacheKey}), and registry access.
 */
@Value
public class ResolveRequest {

    /**
     * The edited {@code package.json} content of each workspace importer, keyed by its importer directory
     * ({@code ""} is the root). A single-package project is just {@code {"": rootManifest}}.
     */
    Map<String, String> importerManifests;

    /** The lock as it stands before this edit; {@code null} only when there is nothing to preserve. */
    @Nullable String existingLock;

    NodeRegistries registries;

    NpmRegistryClient client;
}

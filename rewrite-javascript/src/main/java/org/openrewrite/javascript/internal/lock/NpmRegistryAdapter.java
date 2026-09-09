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
package org.openrewrite.javascript.internal.lock;

import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.Set;

/**
 * Adapts the live {@link NpmRegistryClient}/{@link NodeRegistries} to the {@link Registry} surface a graph builder
 * needs, routing each package name to its configured registry. The client caches per run, so repeated lookups
 * during resolution do not re-hit the network.
 */
public final class NpmRegistryAdapter implements Registry {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;

    public NpmRegistryAdapter(NodeRegistries registries, NpmRegistryClient client) {
        this.registries = registries;
        this.client = client;
    }

    @Override
    public Set<String> versions(String name) {
        return client.getPackument(registries.registryFor(name), name).getVersions();
    }

    @Override
    public VersionManifest manifest(String name, String version) {
        return client.getManifest(registries.registryFor(name), name, version);
    }
}

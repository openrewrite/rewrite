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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.lock.yarnclassic.PackumentSource;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

/**
 * The packuments a Yarn classic install reads, through the run's {@link NpmRegistryClient}, from the registry yarn's
 * own configuration names for each package. The host's credentials go only to the registry the host configured them
 * for.
 */
final class YarnClassicRegistryPackuments implements PackumentSource {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;

    YarnClassicRegistryPackuments(NodeRegistries registries, NpmRegistryClient client) {
        this.registries = registries;
        this.client = client;
    }

    @Override
    public @Nullable String packument(String registry, String name) {
        NodeRegistry configured = registries.registryFor(name);
        NodeRegistry target = trimSlash(configured.getUrl()).equals(trimSlash(registry)) ? configured :
                new NodeRegistry(null, registry, null, null, null, null, false, null, configured.isStrictSsl(), false);
        try {
            return client.getPackumentText(target, name, false);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

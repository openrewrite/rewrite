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
import org.openrewrite.javascript.internal.lock.berry.RegistrySource;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

/**
 * The full packuments and the tarballs a Yarn Berry install reads, from the registry Yarn's own config names; the
 * host's credentials go only to the registry the host configured them for.
 */
final class YarnBerryRegistrySource implements RegistrySource {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;

    YarnBerryRegistrySource(NodeRegistries registries, NpmRegistryClient client) {
        this.registries = registries;
        this.client = client;
    }

    @Override
    public @Nullable String packument(String registry, String name) {
        NodeRegistry configured = registries.registryFor(name);
        NodeRegistry target = trimSlash(configured.getUrl()).equals(trimSlash(registry)) ? configured :
                anonymous(registry, configured);
        try {
            return client.getPackumentText(target, name, true);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public byte @Nullable [] tarball(String url, String name, String version) {
        NodeRegistry configured = registries.registryFor(name);
        NodeRegistry target = url.startsWith(trimSlash(configured.getUrl()) + "/") ? configured :
                anonymous(url, configured);
        try {
            return client.getTarball(target, name, version, url);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private static NodeRegistry anonymous(String url, NodeRegistry configured) {
        return new NodeRegistry(null, url, null, null, null, null, false, null, configured.isStrictSsl(), false);
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

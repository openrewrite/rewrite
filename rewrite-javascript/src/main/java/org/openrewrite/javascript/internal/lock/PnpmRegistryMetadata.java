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
import org.openrewrite.javascript.internal.lock.pnpm.RegistryMetadata;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

/**
 * The packuments and attestations a pnpm install reads, from the registry pnpm's own config names for each package;
 * the host's credentials go only to the registry the host configured them for.
 */
final class PnpmRegistryMetadata implements RegistryMetadata {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;

    PnpmRegistryMetadata(NodeRegistries registries, NpmRegistryClient client) {
        this.registries = registries;
        this.client = client;
    }

    @Override
    public @Nullable String packument(String registry, String name, boolean fullMetadata) {
        try {
            return client.getPackumentText(target(registry, name), name, fullMetadata);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public @Nullable String attestations(String registry, String name, String version) {
        String base = registry.endsWith("/") ? registry : registry + "/";
        String url = base + "-/npm/v1/attestations/" + (name.startsWith("@") ? name.replace("/", "%2F") : name) +
                     "@" + version;
        try {
            return client.getDocumentText(target(registry, name), name, version, url);
        } catch (NodeRegistryException e) {
            return null;
        }
    }

    @Override
    public @Nullable String document(String url) {
        NodeRegistry anonymous = new NodeRegistry(null, url, null, null, null, null, false, null, true, false);
        try {
            return client.getDocumentText(anonymous, url, null, url);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND ||
                e.getReason() == NodeRegistryException.Reason.VERSION_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private NodeRegistry target(String registry, String name) {
        NodeRegistry configured = registries.registryFor(name);
        return trimSlash(configured.getUrl()).equals(trimSlash(registry)) ? configured :
                new NodeRegistry(null, registry, null, null, null, null, false, null, configured.isStrictSsl(), false);
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

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
import org.openrewrite.javascript.internal.lock.bun.ManifestSource;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

/**
 * The packuments a Bun install reads, through the run's {@link NpmRegistryClient}, from the registry Bun's own
 * configuration names for each package, with the credentials bun gives that registry.
 */
final class BunRegistryManifests implements ManifestSource {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;

    BunRegistryManifests(NodeRegistries registries, NpmRegistryClient client) {
        this.registries = registries;
        this.client = client;
    }

    @Override
    public @Nullable String packument(String registry, String name, boolean extended) {
        try {
            return client.getPackumentText(scope(name, registry), name, extended);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    /**
     * ini.zig loadNpmrc gives each credential of the user's .npmrc to every registry on its host, a later one's over
     * an earlier one's, whatever their paths: the host's registries are that .npmrc, the default's first.
     */
    NodeRegistry scope(String name, String url) {
        String host = host(url);
        NodeRegistry scope = null;
        for (NodeRegistry candidate : registries.all()) {
            if (!host.equals(host(candidate.getUrl()))) {
                continue;
            }
            scope = scope == null ? candidate.withUrl(url) : scope
                    .withAuthToken(later(candidate.getAuthToken(), scope.getAuthToken()))
                    .withUsername(later(candidate.getUsername(), scope.getUsername()))
                    .withPassword(later(candidate.getPassword(), scope.getPassword()))
                    .withAuthBase64(later(candidate.getAuthBase64(), scope.getAuthBase64()));
        }
        return scope != null ? scope : new NodeRegistry(null, url, null, null, null, null, false, null,
                registries.registryFor(name).isStrictSsl(), false);
    }

    private static @Nullable String later(@Nullable String later, @Nullable String earlier) {
        return later != null ? later : earlier;
    }

    // url.zig's host: the authority after any userinfo, its port included
    private static String host(String url) {
        int scheme = url.indexOf("://");
        String rest = scheme < 0 ? url : url.substring(scheme + 3);
        int slash = rest.indexOf('/');
        String authority = slash < 0 ? rest : rest.substring(0, slash);
        return authority.substring(authority.lastIndexOf('@') + 1);
    }
}

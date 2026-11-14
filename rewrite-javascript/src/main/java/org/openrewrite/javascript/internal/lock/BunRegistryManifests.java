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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.lock.bun.ManifestSource;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * The packuments a Bun install reads, through the run's {@link NpmRegistryClient}, from the registry Bun's own
 * configuration names for each package, with the credentials bun gives that registry.
 */
final class BunRegistryManifests implements ManifestSource {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;
    private final Map<String, String> hostNetwork;

    BunRegistryManifests(NodeRegistries registries, NpmRegistryClient client) {
        this(registries, client, Collections.emptyMap());
    }

    BunRegistryManifests(NodeRegistries registries, NpmRegistryClient client, Map<String, String> hostNetwork) {
        this.registries = registries;
        this.client = client;
        this.hostNetwork = hostNetwork;
    }

    @Override
    public ManifestSource withNetworkSettings(Map<String, ?> settings) {
        NetworkPolicy.requireDirect("Bun", settings);
        return this;
    }

    @Override
    public @Nullable String packument(String registry, String name, boolean extended) {
        NetworkPolicy.requireDirect("Bun", hostNetwork, client.sendsThroughHostNetwork());
        try {
            NodeRegistry request = scope(name, registry);
            if (hasAuthorization(request) && hasCredentialPath(registry)) {
                throw new CannotKnow("Bun registry credential path cannot be safely reconstructed from the host registry configuration");
            }
            return client.getPackumentText(request, name, extended);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private static boolean hasAuthorization(NodeRegistry registry) {
        // Match the client's header precedence, including empty credentials.
        if (registry.getAuthToken() != null) {
            return !StringUtils.isBlank(registry.getAuthToken());
        }
        if (registry.getAuthBase64() != null) {
            return !StringUtils.isBlank(registry.getAuthBase64());
        }
        return !StringUtils.isBlank(registry.getUsername());
    }

    private static boolean hasCredentialPath(String registry) {
        try {
            String path = URI.create(registry).getRawPath();
            return path != null && !path.isEmpty() && !"/".equals(path);
        } catch (IllegalArgumentException e) {
            throw new CannotKnow("Bun registry credential path is invalid");
        }
    }

    /**
     * The host projection exposes credentials by registry host, a later one's over an earlier one's. It does not
     * retain the native npmrc auth-key path: live requests for a non-root credential path are declined above.
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

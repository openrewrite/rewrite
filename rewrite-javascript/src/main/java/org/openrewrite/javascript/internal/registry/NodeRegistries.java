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
package org.openrewrite.javascript.internal.registry;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.NodeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The registries resolved for a project: npm routes each name to one registry, a scoped name to its
 * {@code @scope:registry} when configured, everything else to the default.
 */
@Value
public class NodeRegistries {
    NodeRegistry defaultRegistry;

    /** Scoped registries keyed by {@code @scope} (e.g. {@code @angular}). */
    Map<String, NodeRegistry> byScope;

    @Nullable
    String proxy;

    @Nullable
    String httpsProxy;

    @Nullable
    String noProxy;

    /** The single registry a package name resolves against. */
    public NodeRegistry registryFor(String packageName) {
        String scope = Urls.scopeOf(packageName);
        if (scope != null) {
            NodeRegistry scoped = byScope.get(scope);
            if (scoped != null) {
                return scoped;
            }
        }
        return defaultRegistry;
    }

    /**
     * The registry a request for {@code packageName} to {@code url} goes through, by the package's scope: the host's
     * for that name when {@code url} is its URL or under it; otherwise {@code url} without credentials.
     */
    public NodeRegistry registryFor(String packageName, String url) {
        NodeRegistry configured = registryFor(packageName);
        return under(url, configured) ? at(url, configured) : withoutCredentials(packageName, url);
    }

    /**
     * The registry a request to {@code url} goes through by its URL, as npm and pnpm pick credentials: of the host's at
     * or above {@code url}, the longest holding credentials, else the longest; otherwise {@code url} without them.
     */
    public NodeRegistry registryAt(String packageName, String url) {
        NodeRegistry match = null;
        for (NodeRegistry candidate : all()) {
            if (under(url, candidate) && (match == null || outranks(candidate, match))) {
                match = candidate;
            }
        }
        return match != null ? at(url, match) : withoutCredentials(packageName, url);
    }

    // npm's regFromURI and pnpm's getAuthHeaderByURI walk up to the longest registry key holding credentials
    private static boolean outranks(NodeRegistry candidate, NodeRegistry match) {
        if (hasCredentials(candidate) != hasCredentials(match)) {
            return hasCredentials(candidate);
        }
        return trimSlash(candidate.getUrl()).length() > trimSlash(match.getUrl()).length();
    }

    /**
     * A token, a base64 auth or a username with its password.
     */
    public static boolean hasCredentials(NodeRegistry registry) {
        return registry.getAuthToken() != null || registry.getAuthBase64() != null ||
               registry.getUsername() != null && registry.getPassword() != null;
    }

    private static boolean under(String url, NodeRegistry registry) {
        String base = trimSlash(registry.getUrl());
        String target = trimSlash(url);
        return target.equals(base) || target.startsWith(base + "/");
    }

    private static NodeRegistry at(String url, NodeRegistry registry) {
        return trimSlash(url).equals(trimSlash(registry.getUrl())) ? registry : registry.withUrl(url);
    }

    private NodeRegistry withoutCredentials(String packageName, String url) {
        return new NodeRegistry(null, url, null, null, null, null, false, null,
                registryFor(packageName).isStrictSsl(), false);
    }

    /** The default registry, then the scoped ones in their configured order. */
    public List<NodeRegistry> all() {
        List<NodeRegistry> all = new ArrayList<>();
        all.add(defaultRegistry);
        all.addAll(byScope.values());
        return all;
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

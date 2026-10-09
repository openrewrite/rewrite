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

import java.util.Map;

/**
 * The registries resolved for a project. Unlike PyPI, npm routes each package name to exactly one
 * registry: a scoped name to its {@code @scope:registry} when configured, everything else to the
 * default.
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
     * The registry a request for {@code packageName} to {@code url} goes through: the host's for that name when
     * {@code url} is its URL or under it, since the host's credentials go nowhere else; otherwise {@code url} without.
     */
    public NodeRegistry registryFor(String packageName, String url) {
        NodeRegistry configured = registryFor(packageName);
        String base = trimSlash(configured.getUrl());
        String target = trimSlash(url);
        if (target.equals(base)) {
            return configured;
        }
        if (target.startsWith(base + "/")) {
            return configured.withUrl(url);
        }
        return new NodeRegistry(null, url, null, null, null, null, false, null, configured.isStrictSsl(), false);
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

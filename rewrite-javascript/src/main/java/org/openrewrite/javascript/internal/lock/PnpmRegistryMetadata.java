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
import org.openrewrite.javascript.internal.lock.pnpm.PnpmAuthHeaders;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmOptions;
import org.openrewrite.javascript.internal.lock.pnpm.RegistryMetadata;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.Collections;
import java.util.Map;

/**
 * The packuments and attestations a pnpm install reads, from the registry pnpm's own config names for each package.
 */
final class PnpmRegistryMetadata implements RegistryMetadata {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;
    private final @Nullable PnpmAuthHeaders auth;
    private final Map<String, String> requestEnvironment;

    PnpmRegistryMetadata(NodeRegistries registries, NpmRegistryClient client) {
        this(registries, client, Collections.emptyMap());
    }

    PnpmRegistryMetadata(NodeRegistries registries, NpmRegistryClient client, Map<String, String> requestEnvironment) {
        this(registries, client, null, requestEnvironment);
    }

    private PnpmRegistryMetadata(NodeRegistries registries, NpmRegistryClient client, @Nullable PnpmAuthHeaders auth,
                                 Map<String, String> requestEnvironment) {
        this.requestEnvironment = requestEnvironment;
        this.auth = auth;
        this.registries = registries;
        this.client = client;
    }

    @Override
    public RegistryMetadata withOptions(PnpmOptions options) {
        return new PnpmRegistryMetadata(registries, client.withTransport(options.transport(requestEnvironment)), options.authHeaders(), requestEnvironment);
    }

    private NodeRegistry request(String name, String url) {
        if (auth == null) {
            throw new IllegalStateException("pnpm registry requests require the store controller's config");
        }
        NodeRegistry host = registries.registryAt(name, url);
        String authorization = auth.authorization(url, name);
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        String basic = authorization != null && authorization.startsWith("Basic ") ? authorization.substring(6) : null;
        return new NodeRegistry(null, url, token, null, null, basic, false, host.getCafile(), host.isStrictSsl(), false);
    }

    @Override
    public @Nullable String packument(String registry, String name, boolean fullMetadata) {
        try {
            return client.getPackumentText(request(name, registry), name, fullMetadata);
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
            return client.getDocumentText(request(name, registry), name, version, url);
        } catch (NodeRegistryException e) {
            // fetchAttestationPublishedAt answers undefined for any failure, and pnpm reads the full document instead
            return null;
        }
    }

    @Override
    public @Nullable String document(String url) {
        try {
            return client.getDocumentText(request("", url), url, null, url);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND ||
                e.getReason() == NodeRegistryException.Reason.VERSION_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }
}

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
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

/**
 * A port's requests through the host's client, with the Authorization header the port computes and the host's
 * transport settings for the url.
 */
final class HostRegistrySource implements RegistrySource {

    private static final String BEARER = "Bearer ";
    private static final String BASIC = "Basic ";

    private final NodeRegistries registries;
    private final NpmRegistryClient client;

    HostRegistrySource(NodeRegistries registries, NpmRegistryClient client) {
        this.registries = registries;
        this.client = client;
    }

    @Override
    public @Nullable String packument(String registry, String name, boolean full, @Nullable String authorization) {
        try {
            return client.getPackumentText(request(name, registry, authorization), name, full);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
        try {
            return client.getTarball(request(name, url, authorization), name, version, url);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.VERSION_NOT_FOUND ||
                e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private NodeRegistry request(String name, String url, @Nullable String authorization) {
        NodeRegistry host = registries.registryAt(name, url);
        String token = authorization != null && authorization.startsWith(BEARER) ?
                authorization.substring(BEARER.length()) : null;
        String basic = authorization != null && authorization.startsWith(BASIC) ?
                authorization.substring(BASIC.length()) : null;
        return new NodeRegistry(null, url, token, null, null, basic, false, host.getCafile(), host.isStrictSsl(),
                false);
    }
}

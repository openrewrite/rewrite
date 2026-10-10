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
import org.openrewrite.javascript.internal.lock.yarnclassic.RegistrySource;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

/**
 * The packuments and tarballs a Yarn classic install reads, from the registry yarn's own config names.
 */
final class YarnClassicRegistryPackuments implements RegistrySource {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;

    YarnClassicRegistryPackuments(NodeRegistries registries, NpmRegistryClient client) {
        this.registries = registries;
        this.client = client;
    }

    @Override
    public @Nullable String packument(String registry, String name) {
        try {
            return client.getPackumentText(registries.registryFor(name, registry), name, false);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public byte @Nullable [] tarball(String url, String name, String version) {
        try {
            return client.getTarball(registries.registryFor(name, url), name, version, url);
        } catch (NodeRegistryException e) {
            if (e.getReason() == NodeRegistryException.Reason.VERSION_NOT_FOUND ||
                e.getReason() == NodeRegistryException.Reason.PACKAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }
}

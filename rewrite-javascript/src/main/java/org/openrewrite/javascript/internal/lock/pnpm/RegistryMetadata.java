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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * The registry documents a pnpm install reads, and the host's registry configuration.
 */
public interface RegistryMetadata {

    /**
     * The host's registry configuration as the user's .npmrc pnpm reads under the project's: {@code registry} and
     * each {@code @scope:registry}; empty when the host names none.
     */
    default Map<String, String> userConfig() {
        return Collections.emptyMap();
    }

    /**
     * {@code GET <registry><name>}: the abbreviated document, or the full one when {@code fullMetadata}; null when the
     * registry answers 404.
     */
    @Nullable
    String packument(String registry, String name, boolean fullMetadata);

    /**
     * {@code GET <registry>-/npm/v1/attestations/<name>@<version>}; null when the registry answers with an error or
     * the request fails.
     */
    @Nullable
    String attestations(String registry, String name, String version);

    /**
     * {@code GET <url>} of a document outside the npm registry (a Node.js release index or its checksums); null when
     * the server answers with an error.
     */
    @Nullable
    String document(String url);
}

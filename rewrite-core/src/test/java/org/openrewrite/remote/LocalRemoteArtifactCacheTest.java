/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.remote;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRemoteArtifactCacheTest {

    @Test
    void hashUriProducesSha256Hex() {
        String hash = LocalRemoteArtifactCache.hashUri(URI.create("https://example.com/artifact.jar"));
        assertThat(hash)
          .hasSize(64)
          .matches("[0-9a-f]+");
    }

    @Test
    void hashUriIsDeterministic() {
        URI uri = URI.create("https://example.com/artifact.jar");
        assertThat(LocalRemoteArtifactCache.hashUri(uri))
          .isEqualTo(LocalRemoteArtifactCache.hashUri(uri));
    }

    @Test
    void hashUriDiffersForDifferentUris() {
        String hash1 = LocalRemoteArtifactCache.hashUri(URI.create("https://example.com/a.jar"));
        String hash2 = LocalRemoteArtifactCache.hashUri(URI.create("https://example.com/b.jar"));
        assertThat(hash1).isNotEqualTo(hash2);
    }
}

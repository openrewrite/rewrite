/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("OptionalAssignedToNull")
class MavenPomBytesCacheTest {

    private static final ResolvedGroupArtifactVersion GAV =
            new ResolvedGroupArtifactVersion(null, "com.foo", "test", "1.0.0", null);

    private static byte[] xml() {
        return "<project><modelVersion>4.0.0</modelVersion></project>".getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void inMemoryTriState() {
        InMemoryMavenPomCache cache = new InMemoryMavenPomCache();

        // unknown
        assertThat(cache.getPomBytes(GAV)).isNull();

        // known-absent
        cache.putPomBytes(GAV, null);
        assertThat(cache.getPomBytes(GAV)).isNotNull().isEmpty();

        // hit
        byte[] bytes = xml();
        cache.putPomBytes(GAV, bytes);
        Optional<byte[]> hit = cache.getPomBytes(GAV);
        assertThat(hit).isPresent();
        assertThat(hit.get()).isEqualTo(bytes);
    }

    @Test
    void rocksdbPersistsAcrossReopen(@TempDir Path tempDir) throws Exception {
        String pathString = tempDir.resolve(".rewrite-cache").toString();
        byte[] bytes = xml();
        try {
            RocksdbMavenPomCache cache = new RocksdbMavenPomCache(tempDir);
            assertThat(cache.getPomBytes(GAV)).isNull();
            cache.putPomBytes(GAV, bytes);
            RocksdbMavenPomCache.closeCache(pathString);

            cache = new RocksdbMavenPomCache(tempDir);
            Optional<byte[]> cached = cache.getPomBytes(GAV);
            assertThat(cached).isPresent();
            assertThat(cached.get()).isEqualTo(bytes);
        } finally {
            RocksdbMavenPomCache.closeCache(pathString);
        }
    }

    @Test
    void rocksdbDropsNegatives(@TempDir Path tempDir) throws Exception {
        String pathString = tempDir.resolve(".rewrite-cache").toString();
        try {
            RocksdbMavenPomCache cache = new RocksdbMavenPomCache(tempDir);
            cache.putPomBytes(GAV, null);
            // Negatives are never persisted, so the entry reads back as unknown, never known-absent.
            assertThat(cache.getPomBytes(GAV)).isNull();
        } finally {
            RocksdbMavenPomCache.closeCache(pathString);
        }
    }

    @Test
    void rocksdbBytesRegionIsDistinctFromPomRegion(@TempDir Path tempDir) throws Exception {
        String pathString = tempDir.resolve(".rewrite-cache").toString();
        try {
            RocksdbMavenPomCache cache = new RocksdbMavenPomCache(tempDir);
            cache.putPomBytes(GAV, xml());
            // The parsed-Pom region shares the store; a colliding key would make getPom try to deserialize raw XML.
            assertThat(cache.getPom(GAV)).isNull();
        } finally {
            RocksdbMavenPomCache.closeCache(pathString);
        }
    }

    @Test
    void compositeBackfillsL1OnPositiveL2Hit() throws MavenDownloadingException {
        InMemoryMavenPomCache l1 = new InMemoryMavenPomCache();
        InMemoryMavenPomCache l2 = new InMemoryMavenPomCache();
        CompositeMavenPomCache composite = new CompositeMavenPomCache(l1, l2);

        byte[] bytes = xml();
        l2.putPomBytes(GAV, bytes);
        assertThat(l1.getPomBytes(GAV)).isNull();

        Optional<byte[]> read = composite.getPomBytes(GAV);
        assertThat(read).isPresent();
        assertThat(read.get()).isEqualTo(bytes);

        Optional<byte[]> backfilled = l1.getPomBytes(GAV);
        assertThat(backfilled).isPresent();
        assertThat(backfilled.get()).isEqualTo(bytes);
    }

    @Test
    void compositeDoesNotBackfillNegativeL2() throws MavenDownloadingException {
        InMemoryMavenPomCache l1 = new InMemoryMavenPomCache();
        InMemoryMavenPomCache l2 = new InMemoryMavenPomCache();
        CompositeMavenPomCache composite = new CompositeMavenPomCache(l1, l2);

        l2.putPomBytes(GAV, null);
        assertThat(composite.getPomBytes(GAV)).isNotNull().isEmpty();
        assertThat(l1.getPomBytes(GAV)).isNull();
    }

    @Test
    void defaultMethodsMakeLegacyImplementationsCompile() throws MavenDownloadingException {
        MavenPomCache minimal = new MavenPomCache() {
            @Override
            public ResolvedPom getResolvedDependencyPom(ResolvedGroupArtifactVersion dependency) {
                return null;
            }

            @Override
            public void putResolvedDependencyPom(ResolvedGroupArtifactVersion dependency, ResolvedPom resolved) {
            }

            @Override
            public Optional<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav) {
                return null;
            }

            @Override
            public void putMavenMetadata(URI repo, GroupArtifactVersion gav, MavenMetadata metadata) {
            }

            @Override
            public Optional<Pom> getPom(ResolvedGroupArtifactVersion gav) {
                return null;
            }

            @Override
            public void putPom(ResolvedGroupArtifactVersion gav, Pom pom) {
            }

            @Override
            public Optional<MavenRepository> getNormalizedRepository(MavenRepository repository) {
                return null;
            }

            @Override
            public void putNormalizedRepository(MavenRepository repository, MavenRepository normalized) {
            }
        };

        // The new region defaults to unknown / no-op without the implementation supplying it.
        assertThat(minimal.getPomBytes(GAV)).isNull();
        minimal.putPomBytes(GAV, xml());
        assertThat(minimal.getPomBytes(GAV)).isNull();
    }
}

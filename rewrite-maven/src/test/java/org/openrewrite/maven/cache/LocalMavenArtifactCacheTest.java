/*
 * Copyright 2020 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.maven.tree.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class LocalMavenArtifactCacheTest {

    LocalMavenArtifactCache cache;

    @BeforeEach
    void setup() {

    }

    @Test
    void saveFileWhenInputStringExists(@TempDir Path tempDir) {
        cache = new LocalMavenArtifactCache(tempDir);
        Path output = cache.putArtifact(releaseDependency(), new ByteArrayInputStream("hi".getBytes()), Throwable::printStackTrace);
        assertThat(output).exists();
    }

    @Test
    void dontCreateFileWhenInputStringNull(@TempDir Path tempDir) {
        cache = new LocalMavenArtifactCache(tempDir);
        Path output = cache.putArtifact(releaseDependency(), null, Throwable::printStackTrace);
        assertThat(output).isNull();
        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void artifactIsNotVisibleWhileItIsWritten(@TempDir Path tempDir) throws Exception {
        cache = new LocalMavenArtifactCache(tempDir);
        ResolvedDependency dependency = releaseDependency();
        byte[] jar = "the whole jar".getBytes(StandardCharsets.UTF_8);
        CountDownLatch halfWritten = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);

        FutureTask<Path> put = new FutureTask<>(() -> cache.putArtifact(dependency,
                pausingHalfway(jar, halfWritten, resume), Throwable::printStackTrace));
        Thread writer = new Thread(put);
        writer.setDaemon(true);
        writer.start();
        try {
            assertThat(halfWritten.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(cache.getArtifact(dependency))
                    .as("another reader of the cache must not get half a jar")
                    .isNull();
        } finally {
            resume.countDown();
        }

        Path written = put.get(10, TimeUnit.SECONDS);
        assertThat(cache.getArtifact(dependency)).isEqualTo(written);
        assertThat(written).hasBinaryContent(jar);
    }

    @Test
    void failedWriteLeavesNoArtifact(@TempDir Path tempDir) throws Exception {
        cache = new LocalMavenArtifactCache(tempDir);
        ResolvedDependency dependency = releaseDependency();
        List<Throwable> errors = new ArrayList<>();

        assertThat(cache.putArtifact(dependency, brokenDownload(), errors::add)).isNull();
        assertThat(errors).singleElement().extracting(Throwable::getMessage).isEqualTo("Connection reset");
        assertThat(cache.getArtifact(dependency))
                .as("a later lookup must download the artifact again, not take what was written of it")
                .isNull();
        try (Stream<Path> files = Files.walk(tempDir)) {
            assertThat(files.filter(Files::isRegularFile)).isEmpty();
        }
    }

    @Test
    void releaseAnotherWriterPutFirstIsUsedWhenThisWriteFails(@TempDir Path tempDir) throws Exception {
        cache = new LocalMavenArtifactCache(tempDir);
        ResolvedDependency release = releaseDependency();
        byte[] jar = "the whole jar".getBytes(StandardCharsets.UTF_8);
        List<Throwable> errors = new ArrayList<>();

        Path artifact = cache.computeArtifact(release, () -> {
            // Another writer puts the release while this one downloads it, and then this write fails, as its move
            // does on Windows while the other writer's jar is open
            cache.putArtifact(release, new ByteArrayInputStream(jar), errors::add);
            return brokenDownload();
        }, errors::add);

        assertThat(artifact).isEqualTo(cache.getArtifact(release)).hasBinaryContent(jar);
        assertThat(errors).isEmpty();
        try (Stream<Path> files = Files.walk(tempDir)) {
            assertThat(files.filter(Files::isRegularFile)).containsExactly(artifact);
        }
    }

    @Test
    void releaseCanBePutAgainWhileItsJarIsOpen(@TempDir Path tempDir) throws Exception {
        cache = new LocalMavenArtifactCache(tempDir);
        ResolvedDependency release = releaseDependency();
        byte[] jar = "the whole jar".getBytes(StandardCharsets.UTF_8);
        List<Throwable> errors = new ArrayList<>();
        Path first = cache.putArtifact(release, new ByteArrayInputStream(jar), errors::add);

        // Opened as JarFile and URLClassLoader open jars, which on Windows keeps them from being replaced
        try (InputStream open = new FileInputStream(first.toFile())) {
            assertThat(cache.putArtifact(release, new ByteArrayInputStream(jar), errors::add))
                    .isEqualTo(first)
                    .hasBinaryContent(jar);
        }
        assertThat(errors).isEmpty();
        try (Stream<Path> files = Files.walk(tempDir)) {
            assertThat(files.filter(Files::isRegularFile)).containsExactly(first);
        }
    }

    @Test
    void failedSnapshotRefreshIsReported(@TempDir Path tempDir) throws Exception {
        cache = new LocalMavenArtifactCache(tempDir);
        ResolvedDependency snapshot = snapshotDependency();
        Path first = cache.computeArtifact(snapshot,
                () -> new ByteArrayInputStream("v1".getBytes(StandardCharsets.UTF_8)),
                Throwable::printStackTrace);
        List<Throwable> errors = new ArrayList<>();

        Path second = cache.computeArtifact(snapshot, LocalMavenArtifactCacheTest::brokenDownload, errors::add);

        assertThat(errors)
                .as("the cached jar may be an older build of the SNAPSHOT")
                .singleElement().extracting(Throwable::getMessage).isEqualTo("Connection reset");
        assertThat(second).isEqualTo(first);
        assertThat(Files.readString(second)).isEqualTo("v1");
    }

    @Test
    void nonDatedSnapshotIsAlwaysRefreshed(@TempDir Path tempDir) throws Exception {
        cache = new LocalMavenArtifactCache(tempDir);
        ResolvedDependency snapshot = snapshotDependency();

        // First install: cache the artifact
        Path first = cache.computeArtifact(snapshot,
                () -> new ByteArrayInputStream("v1".getBytes(StandardCharsets.UTF_8)),
                Throwable::printStackTrace);
        assertThat(first).exists();
        assertThat(Files.readString(first)).isEqualTo("v1");

        // Second install with updated content: should overwrite the cached file
        Path second = cache.computeArtifact(snapshot,
                () -> new ByteArrayInputStream("v2".getBytes(StandardCharsets.UTF_8)),
                Throwable::printStackTrace);
        assertThat(second).exists();
        assertThat(second).isEqualTo(first);
        assertThat(Files.readString(second)).isEqualTo("v2");
    }

    @Test
    void snapshotSuffixedDatedVersionIsAlwaysRefreshed(@TempDir Path tempDir) throws Exception {
        cache = new LocalMavenArtifactCache(tempDir);
        // Simulates a bug where datedSnapshotVersion is set to the SNAPSHOT version string
        // instead of null — should still be treated as mutable
        ResolvedDependency snapshot = ResolvedDependency.builder()
                .gav(new ResolvedGroupArtifactVersion(null, "org.example", "my-recipes",
                        "1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT"))
                .requested(Dependency.builder()
                        .gav(new GroupArtifactVersion("org.example", "my-recipes", "1.0.0-SNAPSHOT"))
                        .build())
                .dependencies(emptyList())
                .licenses(emptyList())
                .depth(0)
                .build();

        Path first = cache.computeArtifact(snapshot,
                () -> new ByteArrayInputStream("v1".getBytes(StandardCharsets.UTF_8)),
                Throwable::printStackTrace);
        assertThat(first).exists();
        assertThat(Files.readString(first)).isEqualTo("v1");

        Path second = cache.computeArtifact(snapshot,
                () -> new ByteArrayInputStream("v2".getBytes(StandardCharsets.UTF_8)),
                Throwable::printStackTrace);
        assertThat(second).isEqualTo(first);
        assertThat(Files.readString(second)).isEqualTo("v2");
    }

    @Test
    void datedSnapshotIsNotRefreshed(@TempDir Path tempDir) throws Exception {
        cache = new LocalMavenArtifactCache(tempDir);
        ResolvedDependency dated = datedSnapshotDependency();

        // First install
        Path first = cache.computeArtifact(dated,
                () -> new ByteArrayInputStream("v1".getBytes(StandardCharsets.UTF_8)),
                Throwable::printStackTrace);
        assertThat(first).exists();
        assertThat(Files.readString(first)).isEqualTo("v1");

        // Second install: should return cached file without re-downloading
        Path second = cache.computeArtifact(dated,
                () -> new ByteArrayInputStream("v2".getBytes(StandardCharsets.UTF_8)),
                Throwable::printStackTrace);
        assertThat(second).isEqualTo(first);
        assertThat(Files.readString(second)).isEqualTo("v1");
    }

    /**
     * Serves the start of a jar, then fails.
     */
    private static InputStream brokenDownload() {
        return new SequenceInputStream(
                new ByteArrayInputStream("the start of a jar".getBytes(StandardCharsets.UTF_8)),
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("Connection reset");
                    }
                });
    }

    /**
     * Serves the first half of {@code content}, then waits for {@code resume} before serving the rest.
     */
    private static InputStream pausingHalfway(byte[] content, CountDownLatch halfWritten, CountDownLatch resume) {
        return new InputStream() {
            private final int half = content.length / 2;
            private int position;

            @Override
            public int read() throws IOException {
                byte[] b = new byte[1];
                return read(b, 0, 1) < 0 ? -1 : b[0] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (position == half) {
                    // The caller wrote the first half out before reading again
                    halfWritten.countDown();
                    try {
                        resume.await();
                    } catch (InterruptedException e) {
                        throw new InterruptedIOException();
                    }
                }
                if (position == content.length) {
                    return -1;
                }
                int n = Math.min(len, (position < half ? half : content.length) - position);
                System.arraycopy(content, position, b, off, n);
                position += n;
                return n;
            }
        };
    }

    private static ResolvedDependency snapshotDependency() {
        return ResolvedDependency.builder()
                .gav(new ResolvedGroupArtifactVersion(null, "org.example", "my-recipes", "1.0.0-SNAPSHOT", null))
                .requested(Dependency.builder()
                        .gav(new GroupArtifactVersion("org.example", "my-recipes", "1.0.0-SNAPSHOT"))
                        .build())
                .dependencies(emptyList())
                .licenses(emptyList())
                .depth(0)
                .build();
    }

    private static ResolvedDependency datedSnapshotDependency() {
        return ResolvedDependency.builder()
                .gav(new ResolvedGroupArtifactVersion("https://repo.example.com", "org.example", "my-recipes",
                        "1.0.0-SNAPSHOT", "1.0.0-20260128.120000-1"))
                .requested(Dependency.builder()
                        .gav(new GroupArtifactVersion("org.example", "my-recipes", "1.0.0-SNAPSHOT"))
                        .build())
                .dependencies(emptyList())
                .licenses(emptyList())
                .depth(0)
                .build();
    }

    private static ResolvedDependency releaseDependency() {
        return ResolvedDependency.builder()
                .gav(new ResolvedGroupArtifactVersion("https://repo1.maven.org/maven2", "org.openrewrite.recipe",
                        "rewrite-testing-frameworks", "1.6.0", null))
                .requested(Dependency.builder()
                        .gav(new GroupArtifactVersion("org.openrewrite.recipe", "rewrite-testing-frameworks", "1.6.0"))
                        .build())
                .dependencies(emptyList())
                .licenses(emptyList())
                .depth(0)
                .build();
    }
}

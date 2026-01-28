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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        Path output = cache.putArtifact(findDependency(), new ByteArrayInputStream("hi".getBytes()), Throwable::printStackTrace);
        assertThat(output).exists();
    }

    @Test
    void dontCreateFileWhenInputStringNull(@TempDir Path tempDir) {
        cache = new LocalMavenArtifactCache(tempDir);
        Path output = cache.putArtifact(findDependency(), null, Throwable::printStackTrace);
        assertThat(output).isNull();
        assertThat(tempDir).isEmptyDirectory();
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

    private static ResolvedDependency findDependency() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ResolvedGroupArtifactVersion recipeGav = new ResolvedGroupArtifactVersion(
          "https://repo1.maven.org/maven2",
          "org.openrewrite.recipe",
          "rewrite-testing-frameworks",
          "1.6.0", null);

        MavenParser mavenParser = MavenParser.builder().build();
        SourceFile parsed = mavenParser.parse(ctx,
                """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>maven-downloader-test</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>%s</groupId>
                          <artifactId>%s</artifactId>
                          <version>%s</version>
                      </dependency>
                  </dependencies>
              </project>
              """.formatted(recipeGav.getGroupId(), recipeGav.getArtifactId(), recipeGav.getVersion()).formatted()
        ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"));

        MavenResolutionResult mavenModel = parsed.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
        assertThat(mavenModel.getDependencies()).isNotEmpty();
        List<ResolvedDependency> runtimeDependencies = mavenModel.getDependencies().get(Scope.Runtime);
        return runtimeDependencies.getFirst();
    }
}

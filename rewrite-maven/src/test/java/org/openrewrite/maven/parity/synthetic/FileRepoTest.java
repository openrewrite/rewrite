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
package org.openrewrite.maven.parity.synthetic;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Scope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomXml;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.dependencies;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * {@code file://} repository semantics beyond slice A's fixtures: {@code maven-metadata-local.xml}
 * is the metadata source, a jar-packaging pom without a (non-empty) sibling jar is unusable, and
 * a pom-less jar still resolves via a synthesized pom (a2 §1.2, a4 catalog #63/#64).
 */
class FileRepoTest {
    private static final String G = "org.parity.synthetic";

    @TempDir
    Path tmp;

    private Path artifact(String repo, String artifactId, String version, @Nullable String pomBody, byte @Nullable [] jar) throws IOException {
        Path dir = Files.createDirectories(tmp.resolve(repo).resolve("org/parity/synthetic").resolve(artifactId).resolve(version));
        if (pomBody != null) {
            Files.writeString(dir.resolve(artifactId + "-" + version + ".pom"), pomBody);
        }
        if (jar != null) {
            Files.write(dir.resolve(artifactId + "-" + version + ".jar"), jar);
        }
        return dir;
    }

    private MavenRepository fileRepo(String name) {
        return MavenRepository.builder().id(name).uri(tmp.resolve(name).toUri().toString()).knownToExist(true).build();
    }

    @Test
    void mavenMetadataLocalIsTheMetadataSource() throws IOException {
        artifact("repo", "rel", "2.0", pomXml(G, "rel", "2.0"), new byte[]{1});
        Path artifactRoot = tmp.resolve("repo/org/parity/synthetic/rel");
        //language=xml
        Files.writeString(artifactRoot.resolve("maven-metadata-local.xml"), """
          <metadata>
              <groupId>org.parity.synthetic</groupId>
              <artifactId>rel</artifactId>
              <versioning>
                  <latest>2.0</latest>
                  <release>2.0</release>
                  <versions>
                      <version>1.0</version>
                      <version>2.0</version>
                  </versions>
              </versioning>
          </metadata>
          """);
        // Decoy in the non-local name: reading it would resolve LATEST to a version that is not there
        //language=xml
        Files.writeString(artifactRoot.resolve("maven-metadata.xml"), """
          <metadata>
              <groupId>org.parity.synthetic</groupId>
              <artifactId>rel</artifactId>
              <versioning>
                  <latest>9.9</latest>
                  <versions>
                      <version>9.9</version>
                  </versions>
              </versioning>
          </metadata>
          """);

        // Legacy-pinned: legacy treats any file:// repo like a local repository and reads
        // maven-metadata-local.xml (a2 §1.2); the engine reads the remote maven-metadata.xml name,
        // as real Maven does for a file:// remote repository.
        SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
          rootPom(dependencies(G + ":rel:LATEST")),
          SyntheticHarness.legacyPinned(ctx -> ctx.setRepositories(List.of(fileRepo("repo")))), tmp);

        assertThat(resolution.failed()).isFalse();
        assertThat(resolution.marker().getDependencies().get(Scope.Compile).get(0).getGav().getVersion())
          .isEqualTo("2.0");
    }

    /** Slice A tripped over this while building fixtures; pinned explicitly here (a4 #64). */
    @Test
    void jarPackagingPomWithoutSiblingJarIsUnusable() throws IOException {
        artifact("repo1", "x", "1.0", pomXml(G, "x", "1.0"), null);
        artifact("repo2", "x", "1.0", pomXml(G, "x", "1.0"), new byte[]{1});

        SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
          rootPom(dependencies(G + ":x:1.0")),
          ctx -> ctx.setRepositories(List.of(fileRepo("repo1"), fileRepo("repo2"))), tmp);

        assertThat(resolution.failed()).isFalse();
        assertThat(resolution.snapshot().getJson().at("/scopes/Compile/0/repo").asText())
          .isEqualTo("<path>/repo2/");
    }

    @Test
    void emptyJarMeansUnusable() throws IOException {
        artifact("repo1", "x", "1.0", pomXml(G, "x", "1.0"), new byte[0]);
        artifact("repo2", "x", "1.0", pomXml(G, "x", "1.0"), new byte[]{1});

        SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
          rootPom(dependencies(G + ":x:1.0")),
          ctx -> ctx.setRepositories(List.of(fileRepo("repo1"), fileRepo("repo2"))), tmp);

        assertThat(resolution.failed()).isFalse();
        assertThat(resolution.snapshot().getJson().at("/scopes/Compile/0/repo").asText())
          .isEqualTo("<path>/repo2/");
    }

    @Test
    void pomPackagingNeedsNoJar() throws IOException {
        artifact("repo1", "parent-only", "1.0", pomXml(G, "parent-only", "1.0", "<packaging>pom</packaging>"), null);

        SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
          rootPom("""
            <dependencies>
                <dependency>
                    <groupId>org.parity.synthetic</groupId>
                    <artifactId>parent-only</artifactId>
                    <version>1.0</version>
                    <type>pom</type>
                </dependency>
            </dependencies>
            """),
          ctx -> ctx.setRepositories(List.of(fileRepo("repo1"))), tmp);

        assertThat(resolution.failed()).isFalse();
        assertThat(resolution.snapshot().getJson().at("/scopes/Compile/0/gav").asText())
          .isEqualTo(G + ":parent-only:1.0");
    }

    @Test
    void pomlessJarResolvesThroughSynthesizedPom() throws IOException {
        artifact("repo1", "nopom", "1.0", null, new byte[]{1});

        // Legacy-pinned: pom-less-jar stub synthesis is legacy transport; the engine deliberately
        // fails a jar dependency whose pom is missing (matching real Maven).
        SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
          rootPom(dependencies(G + ":nopom:1.0")),
          SyntheticHarness.legacyPinned(ctx -> ctx.setRepositories(List.of(fileRepo("repo1")))), tmp);

        assertThat(resolution.failed()).isFalse();
        var node = resolution.snapshot().getJson().at("/scopes/Compile/0");
        assertThat(node.get("gav").asText()).isEqualTo(G + ":nopom:1.0");
        assertThat(node.get("children")).isEmpty();
    }
}

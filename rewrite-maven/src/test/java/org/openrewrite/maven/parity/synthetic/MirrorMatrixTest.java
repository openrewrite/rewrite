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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Parser;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Scope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomPath;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomXml;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.dependencies;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.repositories;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * Settings-mirror matching matrix ({@code *}, {@code external:*}, {@code !} exclusions, id
 * lists), configured through {@link MavenSettings} on the {@link MavenExecutionContextView}
 * exactly as hosts do. Mirrored origin repositories are declared in the pom (context-injected
 * repositories are dropped once settings are present) and use the reserved {@code .invalid} TLD
 * so an accidental mirror miss fails instead of hitting the network.
 */
class MirrorMatrixTest {
    private static final String EXTERNAL_URL = "http://parity.invalid/maven";
    private static final String G = "org.parity.synthetic";

    private static Consumer<MavenExecutionContextView> mirrors(String... mirrorsXml) {
        return ctx -> {
            MavenSettings settings = MavenSettings.parse(Parser.Input.fromString(Paths.get("settings.xml"),
              //language=xml
              "<settings><mirrors>%s</mirrors></settings>".formatted(String.join("", mirrorsXml))), ctx);
            ctx.setMavenSettings(settings);
        };
    }

    private static String mirror(String id, String url, String mirrorOf) {
        return "<mirror><id>%s</id><url>%s</url><mirrorOf>%s</mirrorOf></mirror>".formatted(id, url, mirrorOf);
    }

    @Test
    void wildcardMirrorsEverything() {
        try (MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            mirrorRepo.serve(pomPath(G, "x", "1.0", "1.0"), pomXml(G, "x", "1.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(repositories("origin=" + EXTERNAL_URL) + dependencies(G + ":x:1.0")),
              mirrors(mirror("mirror", mirrorRepo.url(), "*")));

            assertThat(resolution.failed()).isFalse();
            assertThat(mirrorRepo.artifactRequests()).contains("GET " + pomPath(G, "x", "1.0", "1.0"));
            assertThat(resolution.snapshot().getJson().at("/scopes/Compile/0/repo").asText())
              .isEqualTo("http://<local>/");
        }
    }

    @Test
    void externalWildcardSkipsLocalhostRepos() {
        try (MockMavenRepo internal = new MockMavenRepo(); MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            internal.serve(pomPath(G, "x", "1.0", "1.0"), pomXml(G, "x", "1.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(repositories("internal=" + internal.url()) + dependencies(G + ":x:1.0")),
              mirrors(mirror("mirror", mirrorRepo.url(), "external:*")));

            assertThat(resolution.failed()).isFalse();
            assertThat(internal.artifactRequests()).contains("GET " + pomPath(G, "x", "1.0", "1.0"));
            assertThat(mirrorRepo.requests()).isEmpty();
        }
    }

    @Test
    void externalWildcardMirrorsExternalRepos() {
        try (MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            mirrorRepo.serve(pomPath(G, "x", "1.0", "1.0"), pomXml(G, "x", "1.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(repositories("ext=" + EXTERNAL_URL) + dependencies(G + ":x:1.0")),
              mirrors(mirror("mirror", mirrorRepo.url(), "external:*")));

            assertThat(resolution.failed()).isFalse();
            assertThat(mirrorRepo.artifactRequests()).contains("GET " + pomPath(G, "x", "1.0", "1.0"));
        }
    }

    @Test
    void exclusionEscapesWildcard() {
        try (MockMavenRepo excluded = new MockMavenRepo(); MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            excluded.serve(pomPath(G, "x", "1.0", "1.0"), pomXml(G, "x", "1.0"));
            mirrorRepo.serve(pomPath(G, "y", "1.0", "1.0"), pomXml(G, "y", "1.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(repositories("excluded=" + excluded.url(), "ext-origin=" + EXTERNAL_URL) +
                dependencies(G + ":x:1.0", G + ":y:1.0")),
              mirrors(mirror("mirror", mirrorRepo.url(), "*,!excluded")));

            assertThat(resolution.failed()).isFalse();
            // x came from the excluded repo directly; y from the mirror standing in for ext-origin
            assertThat(excluded.artifactRequests()).contains("GET " + pomPath(G, "x", "1.0", "1.0"));
            assertThat(mirrorRepo.artifactRequests())
              .contains("GET " + pomPath(G, "y", "1.0", "1.0"))
              .doesNotContain("GET " + pomPath(G, "x", "1.0", "1.0"));
        }
    }

    @Test
    void mirrorOfListMatchesListedRepos() {
        try (MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            mirrorRepo.serve(pomPath(G, "x", "1.0", "1.0"), pomXml(G, "x", "1.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(repositories("repoA=" + EXTERNAL_URL) + dependencies(G + ":x:1.0")),
              mirrors(mirror("mirror", mirrorRepo.url(), "repoA,repoB")));

            assertThat(resolution.failed()).isFalse();
            assertThat(mirrorRepo.artifactRequests()).contains("GET " + pomPath(G, "x", "1.0", "1.0"));
        }
    }

    @Test
    void mirrorOfListLeavesUnlistedRepos() {
        try (MockMavenRepo unlisted = new MockMavenRepo(); MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            unlisted.serve(pomPath(G, "x", "1.0", "1.0"), pomXml(G, "x", "1.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(repositories("repoC=" + unlisted.url()) + dependencies(G + ":x:1.0")),
              mirrors(mirror("mirror", mirrorRepo.url(), "repoA,repoB")));

            assertThat(resolution.failed()).isFalse();
            assertThat(unlisted.artifactRequests()).contains("GET " + pomPath(G, "x", "1.0", "1.0"));
            assertThat(mirrorRepo.requests()).isEmpty();
        }
    }

    @Test
    void localRepositoryIsNeverMirrored(@TempDir Path localRepo) throws IOException {
        Path artifactDir = Files.createDirectories(localRepo.resolve("org/parity/synthetic/x/1.0"));
        Files.writeString(artifactDir.resolve("x-1.0.pom"), pomXml(G, "x", "1.0"));
        Files.write(artifactDir.resolve("x-1.0.jar"), new byte[]{1});

        try (MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":x:1.0")),
              mirrors(mirror("mirror", mirrorRepo.url(), "*"))
                .andThen(ctx -> {
                    ctx.setAddLocalRepository(true);
                    ctx.setLocalRepository(MavenRepository.builder()
                      .id("local").uri(localRepo.toUri().toString()).knownToExist(true).build());
                }),
              localRepo);

            assertThat(resolution.failed()).isFalse();
            assertThat(mirrorRepo.requests()).isEmpty();
            assertThat(resolution.snapshot().getJson().at("/scopes/Compile/0/repo").asText())
              .startsWith("<path>/");
        }
    }

    @Test
    void firstMatchingMirrorWins() {
        try (MockMavenRepo first = new MockMavenRepo(); MockMavenRepo second = new MockMavenRepo()) {
            first.serve(pomPath(G, "x", "1.0", "1.0"), pomXml(G, "x", "1.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(repositories("repoA=" + EXTERNAL_URL) + dependencies(G + ":x:1.0")),
              mirrors(mirror("named", first.url(), "repoA"), mirror("wildcard", second.url(), "*")));

            assertThat(resolution.failed()).isFalse();
            assertThat(first.artifactRequests()).contains("GET " + pomPath(G, "x", "1.0", "1.0"));
            assertThat(second.requests()).isEmpty();
        }
    }

    /**
     * Applying a mirror replaces the repository's release/snapshot policy with the mirror's:
     * a snapshots-disabled mirror makes the mirrored repository unusable for snapshot GAVs, so
     * the (transitive) snapshot dependency fails without a single snapshot request being sent.
     */
    @Test
    void mirrorPolicyOverridesRepositoryPolicy() {
        try (MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            mirrorRepo.serve(pomPath(G, "rel", "1.0", "1.0"),
              pomXml(G, "rel", "1.0", dependencies(G + ":snap:1.0-SNAPSHOT")));

            SyntheticHarness.Session session = new SyntheticHarness.Session(
              mirrors("<mirror><id>mirror</id><url>%s</url><mirrorOf>origin</mirrorOf><snapshots>false</snapshots></mirror>"
                .formatted(mirrorRepo.url())));
            SyntheticHarness.Resolution resolution = session.resolve(rootPom(
              //language=xml
              """
                <repositories>
                    <repository>
                        <id>origin</id>
                        <url>%s</url>
                        <snapshots><enabled>true</enabled></snapshots>
                    </repository>
                </repositories>
                """.formatted(EXTERNAL_URL) +
                dependencies(G + ":rel:1.0")));

            // L-P0-004 fix: the marker survives the snapshot dependency failure; the error is surfaced
            assertThat(resolution.failed()).isFalse();
            assertThat(resolution.errored()).isTrue();
            assertThat(resolution.errors()).isNotEmpty();
            assertThat(mirrorRepo.artifactRequests())
              .contains("GET " + pomPath(G, "rel", "1.0", "1.0"))
              .noneMatch(r -> r.contains("SNAPSHOT"));
            assertThat(session.listener.getEvents())
              .anyMatch(e -> "downloadError".equals(e.getType()) && e.getKey().startsWith(G + ":snap:1.0-SNAPSHOT"));
        }
    }

    @Test
    void mirroredRepositoryAttributionUsesMirrorUriAndId() {
        try (MockMavenRepo mirrorRepo = new MockMavenRepo()) {
            mirrorRepo.serve(pomPath(G, "x", "1.0", "1.0"), pomXml(G, "x", "1.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(repositories("origin=" + EXTERNAL_URL) + dependencies(G + ":x:1.0")),
              mirrors(mirror("corporate-mirror", mirrorRepo.url(), "*")));

            MavenRepository repo = resolution.marker().getDependencies().get(Scope.Compile).get(0).getRepository();
            assertThat(repo).isNotNull();
            assertThat(repo.getId()).isEqualTo("corporate-mirror");
            assertThat(repo.getUri()).isEqualTo(mirrorRepo.url());
        }
    }
}

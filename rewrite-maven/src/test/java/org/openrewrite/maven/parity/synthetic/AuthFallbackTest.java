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

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;

import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomPath;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomXml;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.dependencies;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.repositories;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * Credential resolution by settings server id, basic auth, and the authenticated-then-anonymous
 * retry on 4xx (replicating Maven transport behavior, a4 catalog #58). The authenticated
 * repository is declared in the pom because context-injected repositories are dropped once a
 * settings object is present.
 */
class AuthFallbackTest {
    private static final String G = "org.parity.synthetic";
    private static final String X_POM = pomPath(G, "x", "1.0", "1.0");
    private static final String BASIC = "Basic " + Base64.getEncoder().encodeToString("admin:secret".getBytes());

    /** 200 only when the request carries exactly the expected basic-auth header, 401 otherwise. */
    private static MockMavenRepo authenticatedRepo() {
        return new MockMavenRepo().serve(X_POM, request ->
          BASIC.equals(request.getHeader("Authorization")) ?
            new MockResponse().setResponseCode(200).setBody(pomXml(G, "x", "1.0")) :
            new MockResponse().setResponseCode(401));
    }

    /** Inverted server: rejects credentialed requests, accepts anonymous ones. */
    private static MockMavenRepo anonymousOnlyRepo() {
        return new MockMavenRepo().serve(X_POM, request -> request.getHeader("Authorization") != null ?
          new MockResponse().setResponseCode(401) :
          new MockResponse().setResponseCode(200).setBody(pomXml(G, "x", "1.0")));
    }

    private static String pomWithRepo(MockMavenRepo repo) {
        return rootPom(repositories("auth-repo=" + repo.url()) + dependencies(G + ":x:1.0"));
    }

    private static Consumer<MavenExecutionContextView> settingsWithServer(String serverId, String username, String password) {
        return ctx -> {
            MavenSettings settings = MavenSettings.parse(Parser.Input.fromString(Paths.get("settings.xml"),
              //language=xml
              """
                <settings>
                    <servers>
                        <server>
                            <id>%s</id>
                            <username>%s</username>
                            <password>%s</password>
                        </server>
                    </servers>
                </settings>
                """.formatted(serverId, username, password)), ctx);
            ctx.setMavenSettings(settings);
        };
    }

    @Test
    void anonymousRequestRejectedWithoutCredentials() {
        try (MockMavenRepo repo = authenticatedRepo()) {
            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":x:1.0")),
              ctx -> ctx.setRepositories(List.of(repo.repo("auth-repo"))));

            assertThat(resolution.failed()).isFalse();
            assertThat(resolution.errored()).isTrue();
            assertThat(resolution.errors()).isNotEmpty();
            // 401 is a deterministic client error: pom GET, jar HEAD probe, then negatively cached
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests()).containsExactly(
                  "GET " + X_POM,
                  "HEAD " + X_POM.replace(".pom", ".jar"));
            }
        }
    }

    @Test
    void credentialsResolvedByServerId() {
        try (MockMavenRepo repo = authenticatedRepo()) {
            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              pomWithRepo(repo), settingsWithServer("auth-repo", "admin", "secret"));

            assertThat(resolution.failed()).isFalse();
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.artifactRequests()).containsExactly("GET " + X_POM);
                assertThat(repo.recordedArtifacts().get(0).getHeader("Authorization")).isEqualTo(BASIC);
            }
            assertThat(resolution.snapshot().getJson().at("/scopes/Compile/0/gav").asText())
              .isEqualTo(G + ":x:1.0");
        }
    }

    @Test
    void mismatchedServerIdSendsNoCredentials() {
        try (MockMavenRepo repo = authenticatedRepo()) {
            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              pomWithRepo(repo), settingsWithServer("some-other-repo", "admin", "secret"));

            assertThat(resolution.failed()).isFalse();
            assertThat(resolution.errored()).isTrue();
            assertThat(repo.recordedArtifacts()).isNotEmpty()
              .allMatch(r -> r.getHeader("Authorization") == null);
        }
    }

    @Test
    void authenticatedRetriedAnonymouslyOnClientError() {
        try (MockMavenRepo repo = anonymousOnlyRepo()) {
            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              pomWithRepo(repo), settingsWithServer("auth-repo", "admin", "secret"));

            assertThat(resolution.failed()).isFalse();
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.artifactRequests()).containsExactly("GET " + X_POM, "GET " + X_POM);
                assertThat(repo.recordedArtifacts().get(0).getHeader("Authorization")).isEqualTo(BASIC);
                assertThat(repo.recordedArtifacts().get(1).getHeader("Authorization")).isNull();
            }
        }
    }

    /**
     * Unresolvable {@code ${...}} placeholder credentials still resolve anonymously — but on the
     * POM path the legacy engine first sends the literal placeholder as basic auth and only
     * recovers through the anonymous retry, unlike {@code MavenArtifactDownloader} (and Maven),
     * which skip unresolved credentials up front. Ledger L-P0-007 pins the divergence.
     */
    @Test
    void unresolvedPlaceholderCredentialsFallBackToAnonymous() {
        try (MockMavenRepo repo = anonymousOnlyRepo()) {
            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              pomWithRepo(repo),
              settingsWithServer("auth-repo", "${env.PARITY_NO_SUCH_USER}", "${env.PARITY_NO_SUCH_PASSWORD}"));

            assertThat(resolution.failed()).isFalse();
            // L-P0-007: the placeholder leaks to the server before the anonymous retry succeeds. Legacy-scoped: under the
            // shadow oracle the engine (Maven) skips unresolved credentials up front, so the two engines' request logs
            // differ in shape and interleave — the L-P0-007 divergence itself is what flips at Phase 5.
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.artifactRequests()).containsExactly("GET " + X_POM, "GET " + X_POM);
                assertThat(repo.recordedArtifacts().get(0).getHeader("Authorization"))
                  .isEqualTo("Basic " + Base64.getEncoder().encodeToString(
                    "${env.PARITY_NO_SUCH_USER}:${env.PARITY_NO_SUCH_PASSWORD}".getBytes()));
                assertThat(repo.recordedArtifacts().get(1).getHeader("Authorization")).isNull();
            }
        }
    }
}

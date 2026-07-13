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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomPath;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomXml;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.dependencies;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * Failure-caching semantics: deterministic 4xx negatively cached in the pom cache, retryable
 * statuses (408/425/429, 5xx) never cached. These hold identically on the engine path
 * ({@code CacheBridge}'s tri-state pom-bytes region + {@code HttpSenderTransporter.classify}),
 * with a single descriptor read instead of legacy's per-scope passes and no pom-less-jar HEAD
 * probe. Dead-endpoint bookkeeping in the context's unreachable-endpoint set is legacy-scoped
 * (a2 §1.8); the engine keeps its own session-scoped memory.
 */
class NegativeCachingTest {
    private static final String G = "org.parity.synthetic";
    private static final String MISSING_POM = pomPath(G, "missing", "1.0", "1.0");

    @Test
    void deterministicNotFoundIsNegativelyCached() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            // The engine memoizes a whole resolution per context, so cross-resolution cache semantics
            // are observed with a fresh context sharing the pom cache — the recipe-loop-adjacent shape.
            InMemoryMavenPomCache cache = new InMemoryMavenPomCache();
            Consumer<MavenExecutionContextView> customize = ctx -> {
                ctx.setRepositories(List.of(repo.repo("mock")));
                ctx.setPomCache(cache);
            };
            String pom = rootPom(dependencies(G + ":missing:1.0"));

            SyntheticHarness.Resolution first = new SyntheticHarness.Session(customize).resolve(pom);
            // Partial-failure contract: the marker survives with resolvable scopes; the failure is surfaced as an error
            assertThat(first.failed()).isFalse();
            assertThat(first.errored()).isTrue();
            // One pom GET for the whole resolution: the engine reads the descriptor once and the
            // deterministic 404 is negatively cached; no pom-less-jar HEAD probe on the engine path
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests()).containsExactly("GET " + MISSING_POM);
            }

            SyntheticHarness.Resolution second = new SyntheticHarness.Session(customize).resolve(pom);
            assertThat(second.failed()).isFalse();
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests()).hasSize(1); // zero new requests
                assertThat(second.errors()).hasSize(1); // the cached failure is still surfaced
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 425, 429, 500, 503})
    void retryableStatusesAreNotCached(int status) {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve(MISSING_POM, request -> new MockResponse().setResponseCode(status));
            InMemoryMavenPomCache cache = new InMemoryMavenPomCache();
            Consumer<MavenExecutionContextView> customize = ctx -> {
                ctx.setRepositories(List.of(repo.repo("mock")));
                ctx.setPomCache(cache);
            };
            String pom = rootPom(dependencies(G + ":missing:1.0"));

            SyntheticHarness.Resolution first = new SyntheticHarness.Session(customize).resolve(pom);
            assertThat(first.failed()).isFalse();
            assertThat(first.errored()).isTrue();
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests()).containsExactly("GET " + MISSING_POM);
            }

            // Nothing cached: the transfer error is never negatively cached, so a fresh context
            // sharing the pom cache re-requests
            new SyntheticHarness.Session(customize).resolve(pom);
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests()).hasSize(2);
            }
        }
    }

    @Test
    void deadEndpointRecordedOnceAndSkippedForTheRestOfTheRun() {
        int deadPort = closedPort();
        MavenRepository deadOne = MavenRepository.builder()
          .id("dead-one").uri("http://localhost:" + deadPort + "/repo-one").build();
        // Same host:port under a different id and path: skipped without any connection attempt
        MavenRepository deadTwo = MavenRepository.builder()
          .id("dead-two").uri("http://localhost:" + deadPort + "/repo-two").build();

        try (MockMavenRepo live = new MockMavenRepo()) {
            live.serve(pomPath(G, "a", "1.0", "1.0"), pomXml(G, "a", "1.0"));
            live.serve(pomPath(G, "b", "1.0", "1.0"), pomXml(G, "b", "1.0"));

            // Legacy-pinned: the ctx unreachable-endpoint set and repositoryAccessFailed events are legacy
            // downloader observables; the engine's dead-endpoint memory is session-scoped inside
            // HttpSenderTransporter and deliberately not surfaced on the context.
            SyntheticHarness.Session session = new SyntheticHarness.Session(SyntheticHarness.legacyPinned(
              ctx -> ctx.setRepositories(List.of(deadOne, deadTwo, live.repo("live")))));
            SyntheticHarness.Resolution resolution = session.resolve(
              rootPom(dependencies(G + ":a:1.0", G + ":b:1.0")));

            assertThat(resolution.failed()).isFalse();
            assertThat(session.ctx.getUnreachableEndpoints()).containsExactly("localhost:" + deadPort);
            assertThat(live.artifactRequests()).contains(
              "GET " + pomPath(G, "a", "1.0", "1.0"),
              "GET " + pomPath(G, "b", "1.0", "1.0"));

            var events = resolution.snapshot().getJson().get("events");
            // dead-one was probed and failed; dead-two's endpoint was already known dead, and
            // subsequent artifacts hit the cached failure of both
            assertThat(events.fieldNames()).toIterable()
              .anyMatch(e -> e.startsWith("repositoryAccessFailed:http://<local>/repo-one"))
              .anyMatch(e -> e.startsWith("repositoryAccessFailedPreviously:http://<local>/repo-two"))
              .noneMatch(e -> e.startsWith("repositoryAccessFailed:http://<local>/repo-two"));
        }
    }

    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

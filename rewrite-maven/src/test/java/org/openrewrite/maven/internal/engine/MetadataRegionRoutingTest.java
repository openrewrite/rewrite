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
package org.openrewrite.maven.internal.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.DependencyNode;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Non-pinned metadata reads during collection route through {@code MavenPomCache}'s metadata region ({@link
 * RegionMetadataResolver}), like the pom-bytes flow: a cold collect populates the region from the network, and a fresh
 * collect (fresh {@code RepositorySystem}/session/LRM) resolving the same {@code RELEASE} metaversion is served from the
 * warm region with zero metadata network. Closes phase3-results-a deviation #3.
 */
class MetadataRegionRoutingTest {

    private static final String G = "test";

    @Test
    void metaversionResolvesFromWarmRegionWithoutNetwork(@TempDir Path tmp) throws Exception {
        String metaXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<metadata><groupId>test</groupId><artifactId>c</artifactId><versioning>" +
                "<latest>1.0</latest><release>1.0</release><versions><version>1.0</version></versions>" +
                "<lastUpdated>20260101000000</lastUpdated></versioning></metadata>";
        try (MockMavenServer server = new MockMavenServer()) {
            server.metadata(G, "c", metaXml)
                    .pom(G, "c", "1.0", project("c", "jar", ""));
            String rootXml = project("root", "jar", deps(dep("c", "RELEASE")));

            InMemoryMavenPomCache cache = new InMemoryMavenPomCache();
            ExecutionContext ctx = ctx(cache);

            // Pass 1 (cold): resolves RELEASE over the network and primes the metadata region.
            EngineCollectOutcome cold = collect(server, tmp.resolve("cold"), ctx, rootXml);
            assertTrue(hasArtifact(cold.getRoot(), "c", "1.0"), "cold collect resolves RELEASE to 1.0");
            assertTrue(server.requests.stream().anyMatch(r -> r.contains("maven-metadata")),
                    () -> "cold collect reads GA metadata over the network: " + server.requests);

            // The write path populated the pluggable region.
            Optional<MavenMetadata> region =
                    cache.getMavenMetadata(URI.create(server.baseUrl()), new GroupArtifactVersion(G, "c", null));
            assertNotNull(region, "region entry written");
            assertTrue(region.isPresent());
            assertEquals("1.0", region.get().getVersioning().getRelease());

            // Pass 2 (warm region, fresh collector → fresh RepositorySystem/session/LRM): served from the region.
            server.requests.clear();
            EngineCollectOutcome warm = collect(server, tmp.resolve("warm"), ctx, rootXml);
            assertTrue(hasArtifact(warm.getRoot(), "c", "1.0"), "warm collect still resolves RELEASE to 1.0");
            assertTrue(server.requests.stream().noneMatch(r -> r.contains("maven-metadata")),
                    () -> "the metaversion must resolve from the region, not the network: " + server.requests);
        }
    }

    // ---- harness (mirrors EngineDependencyCollectorTest) ----

    private EngineCollectOutcome collect(MockMavenServer server, Path tmp, ExecutionContext ctx, String rootXml)
            throws Exception {
        Model model = buildRootModel(server, tmp, ctx, rootXml);
        try (EngineDependencyCollector collector = new EngineDependencyCollector()) {
            return collector.collect(model, requested(rootXml), repos(server), settings(), emptyReactor(),
                    tmp.resolve("collect"), ctx);
        }
    }

    private Model buildRootModel(MockMavenServer server, Path tmp, ExecutionContext ctx, String rootXml)
            throws Exception {
        try (MavenEngine engine = new MavenEngine();
             CloseableSession session = engine.newSession(tmp.resolve("eff-lrm"), SessionConfig.forSender(sender()))) {
            EngineEffectivePom service = new EngineEffectivePom(engine.getRepositorySystem(), session,
                    repos(server), null);
            EngineModelBuildingOutcome outcome = service.build(rootXml.getBytes(StandardCharsets.UTF_8),
                    requested(rootXml), settings(), emptyReactor(), ctx);
            assertTrue(outcome.isSuccess(), () -> "root effective model build failed: " + outcome.getFailure());
            return outcome.getResult().getEffectiveModel();
        }
    }

    private static boolean hasArtifact(@org.jspecify.annotations.Nullable DependencyNode root, String artifactId,
                                       String version) {
        if (root == null) {
            return false;
        }
        Deque<DependencyNode> stack = new ArrayDeque<>();
        Set<DependencyNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        stack.push(root);
        while (!stack.isEmpty()) {
            DependencyNode n = stack.pop();
            if (!seen.add(n)) {
                continue;
            }
            if (n.getArtifact() != null && artifactId.equals(n.getArtifact().getArtifactId()) &&
                version.equals(n.getArtifact().getVersion())) {
                return true;
            }
            for (DependencyNode c : n.getChildren()) {
                stack.push(c);
            }
        }
        return false;
    }

    private static HttpUrlConnectionSender sender() {
        return new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10));
    }

    private static List<MavenRepository> repos(MockMavenServer server) {
        return singletonList(new MavenRepository("mock", server.baseUrl(), "true", "true", false, null, null, null, null));
    }

    private static ExecutionContext ctx(InMemoryMavenPomCache cache) {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenExecutionContextView.view(ctx).setPomCache(cache);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(sender());
        return ctx;
    }

    private static EffectiveSettings settings() {
        return new EffectiveSettings(emptyList(), emptyList(), emptyMap());
    }

    private static ReactorWorkspace emptyReactor() {
        return new ReactorWorkspace(emptyMap(), (Function<Path, byte[]>) p -> null);
    }

    private static Pom requested(String rootXml) {
        return RawPom.parse(new ByteArrayInputStream(rootXml.getBytes(StandardCharsets.UTF_8)), null)
                .toPom(Paths.get("pom.xml"), null);
    }

    private static String project(String artifactId, String packaging, String body) {
        return "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion><groupId>" + G + "</groupId>" +
                "<artifactId>" + artifactId + "</artifactId><version>1</version>" +
                "<packaging>" + packaging + "</packaging>" + body + "</project>";
    }

    private static String deps(String... dep) {
        return "<dependencies>" + String.join("", dep) + "</dependencies>";
    }

    private static String dep(String artifactId, String version) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId>" +
                "<version>" + version + "</version></dependency>";
    }
}

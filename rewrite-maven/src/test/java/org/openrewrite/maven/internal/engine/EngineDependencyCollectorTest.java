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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.DependencyNode;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end coverage of {@link EngineDependencyCollector} against the real shaded engine: the root effective model is
 * built by {@link EngineEffectivePom} (Phase 2) and handed to a single verbose collect over MockWebServer, exercising
 * {@link EngineDescriptorReader} (transitive descriptors via the same model builder) and {@link PinnedVersionResolver}.
 * Mirrors {@code EngineEffectivePomTest}'s hermetic patterns and the provenance-spike acceptance assertions.
 */
class EngineDependencyCollectorTest {

    private static final String G = "test";

    // (a) Multi-level transitive graph: winner/loser retained with NODE_DATA_WINNER, depth + declaring parent intact.
    @Test
    void winnerLoserAndDepthAreIntact(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("c", "1.0"))))
                    .pom(G, "b", "1", project("b", "jar", deps(dep("c", "2.0"))))
                    .pom(G, "c", "1.0", project("c", "jar", ""))
                    .pom(G, "c", "2.0", project("c", "jar", ""));
            String rootXml = project("root", "jar", deps(dep("a", "1"), dep("b", "1")));

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), emptyReactor(), rootXml);

            List<NodeInfo> all = walk(outcome.getRoot());
            List<NodeInfo> cNodes = withArtifactId(all, "c");
            assertEquals(2, cNodes.size(), "verbose STANDARD retains the losing 'c'");

            NodeInfo winner = cNodes.stream().filter(n -> winnerOf(n.node) == null).findFirst().orElseThrow(AssertionError::new);
            NodeInfo loser = cNodes.stream().filter(n -> winnerOf(n.node) != null).findFirst().orElseThrow(AssertionError::new);
            assertEquals("1.0", winner.node.getArtifact().getVersion());
            assertEquals(2, winner.depth);
            assertEquals("a", winner.parentId, "c:1.0 was declared by a and wins (nearest, first-declared)");
            assertEquals("2.0", loser.node.getArtifact().getVersion());
            assertEquals("b", loser.parentId);
            assertEquals("1.0", winnerOf(loser.node).getArtifact().getVersion(), "NODE_DATA_WINNER points at the winner");
            assertEquals(1, withArtifactId(all, "a").get(0).depth);
            assertEquals("root", withArtifactId(all, "a").get(0).parentId);
        }
    }

    // (b) Conflict at unequal depth: the nearer (direct) version wins over the deeper transitive.
    @Test
    void nearerVersionWinsAtUnequalDepth(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("c", "1.0"))))
                    .pom(G, "c", "1.0", project("c", "jar", ""))
                    .pom(G, "c", "2.0", project("c", "jar", ""));
            String rootXml = project("root", "jar", deps(dep("c", "2.0"), dep("a", "1")));

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), emptyReactor(), rootXml);

            List<NodeInfo> cNodes = withArtifactId(walk(outcome.getRoot()), "c");
            NodeInfo winner = cNodes.stream().filter(n -> winnerOf(n.node) == null).findFirst().orElseThrow(AssertionError::new);
            NodeInfo loser = cNodes.stream().filter(n -> winnerOf(n.node) != null).findFirst().orElseThrow(AssertionError::new);
            assertEquals("2.0", winner.node.getArtifact().getVersion(), "the direct c:2.0 (depth 1) is nearest");
            assertEquals(1, winner.depth);
            assertEquals("1.0", loser.node.getArtifact().getVersion());
            assertEquals(2, loser.depth);
        }
    }

    // (c) Exclusions prune matching transitives — exact g:a and the '*' wildcard (Maven's stock semantics, no glob).
    @Test
    void exclusionsPruneExactAndWildcard(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("c", "1.0"))))
                    .pom(G, "b", "1", project("b", "jar", deps(dep("d", "1.0"))))
                    .pom(G, "c", "1.0", project("c", "jar", ""))
                    .pom(G, "d", "1.0", project("d", "jar", ""));
            // a excludes exactly test:c; b excludes test:* (wildcard artifactId).
            String rootXml = project("root", "jar",
                    "<dependencies>" +
                    depWithExclusions("a", "1", exclusion(G, "c")) +
                    depWithExclusions("b", "1", exclusion(G, "*")) +
                    "</dependencies>");

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), emptyReactor(), rootXml);

            List<NodeInfo> all = walk(outcome.getRoot());
            assertFalse(withArtifactId(all, "a").isEmpty());
            assertFalse(withArtifactId(all, "b").isEmpty());
            assertTrue(withArtifactId(all, "c").isEmpty(), "exact exclusion test:c pruned c");
            assertTrue(withArtifactId(all, "d").isEmpty(), "wildcard exclusion test:* pruned d");
        }
    }

    // (d) Optional is kept for a direct dependency and skipped for a transitive one (OptionalDependencySelector.fromDirect).
    @Test
    void optionalDirectKeptTransitiveSkipped(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar",
                            deps(optionalDep("x", "1"), dep("y", "1"))))
                    .pom(G, "x", "1", project("x", "jar", ""))
                    .pom(G, "y", "1", project("y", "jar", ""));
            String rootXml = project("root", "jar", deps(optionalDep("a", "1")));

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), emptyReactor(), rootXml);

            List<NodeInfo> all = walk(outcome.getRoot());
            assertFalse(withArtifactId(all, "a").isEmpty(), "optional direct dependency kept");
            assertFalse(withArtifactId(all, "y").isEmpty(), "non-optional transitive kept");
            assertTrue(withArtifactId(all, "x").isEmpty(), "optional transitive skipped");
        }
    }

    // (e) Root dependencyManagement applied to a transitive: managed version/scope/exclusion visible as premanaged state.
    @Test
    void managedVersionScopeExclusionVisibleAsPremanagedState(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("c", "1.0"))))
                    .pom(G, "c", "1.5", project("c", "jar", deps(dep("extra", "1"))))
                    .pom(G, "extra", "1", project("extra", "jar", ""));
            String managedC = "<dependency><groupId>" + G + "</groupId><artifactId>c</artifactId><version>1.5</version>" +
                    "<scope>runtime</scope><exclusions>" + exclusion(G, "extra") + "</exclusions></dependency>";
            String rootXml = project("root", "jar",
                    deps(dep("a", "1")) +
                    "<dependencyManagement><dependencies>" + managedC + "</dependencies></dependencyManagement>");

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), emptyReactor(), rootXml);

            DependencyNode c = withArtifactId(walk(outcome.getRoot()), "c").get(0).node;
            assertEquals("1.5", c.getArtifact().getVersion(), "managed version applied");
            assertEquals("1.0", DependencyManagerUtils.getPremanagedVersion(c), "premanaged (original) version exposed");
            assertTrue((c.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0);
            assertEquals("runtime", c.getDependency().getScope(), "managed scope applied");
            assertEquals("compile", DependencyManagerUtils.getPremanagedScope(c));
            assertTrue((c.getManagedBits() & DependencyNode.MANAGED_SCOPE) != 0);
            assertTrue((c.getManagedBits() & DependencyNode.MANAGED_EXCLUSIONS) != 0, "managed exclusion recorded");
            assertTrue(withArtifactId(walk(outcome.getRoot()), "extra").isEmpty(), "managed exclusion pruned 'extra'");
        }
    }

    // (f) A pinned transitive snapshot resolves to its dated build with no metadata read.
    @Test
    void pinnedSnapshotHonoredTransitivelyWithoutMetadata(@TempDir Path tmp) throws Exception {
        String dated = "1.0-20260101.120000-1";
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("s", "1.0-SNAPSHOT"))))
                    .snapshotPom(G, "s", "1.0-SNAPSHOT", dated, project("s", "jar", ""));
            String rootXml = project("root", "jar", deps(dep("a", "1")));

            ExecutionContext ctx = ctx(new InMemoryMavenPomCache());
            MavenExecutionContextView.view(ctx).setPinnedSnapshotVersions(singletonList(
                    new ResolvedGroupArtifactVersion(server.baseUrl(), G, "s", "1.0-SNAPSHOT", dated)));

            EngineCollectOutcome outcome = collect(server, tmp, ctx, emptyReactor(), rootXml);

            assertFalse(withArtifactId(walk(outcome.getRoot()), "s").isEmpty(), "the pinned snapshot was collected");
            assertTrue(outcome.getDirectFailures().isEmpty());
            assertTrue(server.requests.stream().noneMatch(r -> r.contains("maven-metadata")),
                    () -> "pinning must short-circuit metadata: " + server.requests);
        }
    }

    // (g) A transitive dependency that is a reactor member is served from the workspace, never the repository.
    @Test
    void reactorMemberServedFromWorkspaceAsTransitive(@TempDir Path tmp) throws Exception {
        Path rPath = Paths.get("r", "pom.xml");
        String rXml = project("r", "jar", deps(dep("leaf", "1")));
        Map<Path, byte[]> bytesByPath = new HashMap<>();
        bytesByPath.put(rPath, rXml.getBytes(StandardCharsets.UTF_8));
        Map<Path, Pom> projectPoms = new HashMap<>();
        projectPoms.put(rPath, parse(rXml, rPath));
        ReactorWorkspace reactor = new ReactorWorkspace(projectPoms, bytesByPath::get);

        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("r", "1"))))
                    .pom(G, "leaf", "1", project("leaf", "jar", ""));
            String rootXml = project("root", "jar", deps(dep("a", "1")));

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), reactor, rootXml);

            List<NodeInfo> all = walk(outcome.getRoot());
            assertFalse(withArtifactId(all, "r").isEmpty(), "reactor member present as a transitive");
            assertFalse(withArtifactId(all, "leaf").isEmpty(), "its transitive resolved");
            assertTrue(server.requests.stream().noneMatch(r -> r.contains("/r/1/")),
                    () -> "the reactor member's pom must come from the workspace: " + server.requests);
        }
    }

    // (h) A dependency cycle is recorded and tolerated; the collect completes.
    @Test
    void dependencyCycleRecordedAndTolerated(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("b", "1"))))
                    .pom(G, "b", "1", project("b", "jar", deps(dep("a", "1"))));
            String rootXml = project("root", "jar", deps(dep("a", "1")));

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), emptyReactor(), rootXml);

            assertNotNull(outcome.getRoot(), "collect completes despite the cycle");
            assertFalse(outcome.getCycles().isEmpty(), "the a<->b cycle is recorded in CollectResult.getCycles()");
            assertTrue(outcome.getDirectFailures().isEmpty());
        }
    }

    // (i) A jar-typed transitive whose POM 404s fails at any depth (legacy + real Maven both fail it — L-P3-D-003);
    // the collect still completes over the partial graph so the resolvable scopes are projected.
    @Test
    void missingJarTransitiveFails(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("gone", "1"))));
            String rootXml = project("root", "jar", deps(dep("a", "1")));

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), emptyReactor(), rootXml);

            assertNotNull(outcome.getRoot(), "collect completes over the partial graph");
            assertTrue(outcome.getDirectFailures().stream().anyMatch(e -> e.getFailedOn() != null &&
                            "gone".equals(e.getFailedOn().getArtifactId())),
                    () -> "the unavailable jar transitive is a failure: " + outcome.getDirectFailures());
        }
    }

    @Test
    void missingDirectFails(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "present", "1", project("present", "jar", ""));
            String rootXml = project("root", "jar", deps(dep("present", "1"), dep("gone", "1")));

            EngineCollectOutcome outcome = collect(server, tmp, ctx(new InMemoryMavenPomCache()), emptyReactor(), rootXml);

            assertNotNull(outcome.getRoot());
            assertEquals(1, outcome.getDirectFailures().size(), () -> "the missing direct dependency fails");
            MavenDownloadingException failure = outcome.getDirectFailures().get(0);
            assertEquals("gone", failure.getRoot().getArtifactId());
        }
    }

    // (j) A warm re-collect on the same session cache performs zero network requests.
    @Test
    void warmReCollectPerformsNoNetwork(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             EngineDependencyCollector collector = new EngineDependencyCollector()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("c", "1.0"))))
                    .pom(G, "c", "1.0", project("c", "jar", ""));
            String rootXml = project("root", "jar", deps(dep("a", "1")));
            ExecutionContext ctx = ctx(new InMemoryMavenPomCache());
            Model model = buildRootModel(server, tmp, ctx, emptyReactor(), rootXml);

            collector.collect(model, requested(rootXml), repos(server), settings(), emptyReactor(), tmp.resolve("scratch"), ctx);
            int afterCold = server.requestCount();
            assertTrue(afterCold > 0);

            EngineCollectOutcome warm = collector.collect(model, requested(rootXml), repos(server), settings(),
                    emptyReactor(), tmp.resolve("scratch"), ctx);
            assertFalse(withArtifactId(walk(warm.getRoot()), "c").isEmpty());
            assertEquals(afterCold, server.requestCount(), () -> "warm re-collect hit the network: " + server.requests);
        }
    }

    // Perf sanity (5): the warm re-collect rebuilds no effective models — the descriptor pool serves the second pass.
    @Test
    void warmReCollectRebuildsNoModels(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             EngineDependencyCollector collector = new EngineDependencyCollector()) {
            server.pom(G, "a", "1", project("a", "jar", deps(dep("c", "1.0"))))
                    .pom(G, "c", "1.0", project("c", "jar", ""));
            String rootXml = project("root", "jar", deps(dep("a", "1")));
            ExecutionContext ctx = ctx(new InMemoryMavenPomCache());
            Model model = buildRootModel(server, tmp, ctx, emptyReactor(), rootXml);

            EngineCollectOutcome cold = collector.collect(model, requested(rootXml), repos(server), settings(),
                    emptyReactor(), tmp.resolve("scratch"), ctx);
            assertTrue(cold.getDescriptorReads() > 0, "the cold collect builds descriptors");

            EngineCollectOutcome warm = collector.collect(model, requested(rootXml), repos(server), settings(),
                    emptyReactor(), tmp.resolve("scratch"), ctx);
            assertEquals(0, warm.getDescriptorReads(), "the warm collect rebuilds no models (served from the DataPool)");
        }
    }

    // ---- harness ----

    private EngineCollectOutcome collect(MockMavenServer server, Path tmp, ExecutionContext ctx,
                                         ReactorWorkspace reactor, String rootXml) throws Exception {
        Model model = buildRootModel(server, tmp, ctx, reactor, rootXml);
        try (EngineDependencyCollector collector = new EngineDependencyCollector()) {
            return collector.collect(model, requested(rootXml), repos(server), settings(), reactor,
                    tmp.resolve("collect"), ctx);
        }
    }

    private Model buildRootModel(MockMavenServer server, Path tmp, ExecutionContext ctx, ReactorWorkspace reactor,
                                 String rootXml) throws Exception {
        try (MavenEngine engine = new MavenEngine();
             CloseableSession session = engine.newSession(tmp.resolve("eff-lrm"), SessionConfig.forSender(sender()))) {
            EngineEffectivePom service = new EngineEffectivePom(engine.getRepositorySystem(), session,
                    repos(server), null);
            EngineModelBuildingOutcome outcome = service.build(rootXml.getBytes(StandardCharsets.UTF_8),
                    requested(rootXml), settings(), reactor, ctx);
            assertTrue(outcome.isSuccess(), () -> "root effective model build failed: " + outcome.getFailure());
            return outcome.getResult().getEffectiveModel();
        }
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
        return parse(rootXml, Paths.get("pom.xml"));
    }

    private static Pom parse(String xml, Path path) {
        return RawPom.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null).toPom(path, null);
    }

    // ---- graph walk ----

    private static final class NodeInfo {
        final DependencyNode node;
        final int depth;
        final @Nullable String parentId;

        NodeInfo(DependencyNode node, int depth, @Nullable String parentId) {
            this.node = node;
            this.depth = depth;
            this.parentId = parentId;
        }
    }

    private static List<NodeInfo> walk(DependencyNode root) {
        List<NodeInfo> out = new ArrayList<>();
        walk(root, 0, null, out, Collections.newSetFromMap(new IdentityHashMap<>()));
        return out;
    }

    private static void walk(DependencyNode node, int depth, @Nullable String parentId,
                             List<NodeInfo> out, Set<DependencyNode> visited) {
        if (!visited.add(node)) {
            return;
        }
        out.add(new NodeInfo(node, depth, parentId));
        String id = node.getArtifact() == null ? null : node.getArtifact().getArtifactId();
        for (DependencyNode child : node.getChildren()) {
            walk(child, depth + 1, id, out, visited);
        }
    }

    private static List<NodeInfo> withArtifactId(List<NodeInfo> nodes, String artifactId) {
        List<NodeInfo> out = new ArrayList<>();
        for (NodeInfo ni : nodes) {
            if (ni.node.getArtifact() != null && artifactId.equals(ni.node.getArtifact().getArtifactId())) {
                out.add(ni);
            }
        }
        return out;
    }

    private static @Nullable DependencyNode winnerOf(DependencyNode node) {
        Object w = node.getData().get(ConflictResolver.NODE_DATA_WINNER);
        return w instanceof DependencyNode ? (DependencyNode) w : null;
    }

    // ---- pom XML builders ----

    private static String project(String artifactId, String packaging, String body) {
        return "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<groupId>" + G + "</groupId>" +
                "<artifactId>" + artifactId + "</artifactId>" +
                "<version>1</version>" +
                "<packaging>" + packaging + "</packaging>" +
                body + "</project>";
    }

    private static String deps(String... dep) {
        return "<dependencies>" + String.join("", dep) + "</dependencies>";
    }

    private static String dep(String artifactId, String version) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId>" +
                "<version>" + version + "</version></dependency>";
    }

    private static String optionalDep(String artifactId, String version) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId>" +
                "<version>" + version + "</version><optional>true</optional></dependency>";
    }

    private static String depWithExclusions(String artifactId, String version, String... exclusion) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId>" +
                "<version>" + version + "</version><exclusions>" + String.join("", exclusion) + "</exclusions></dependency>";
    }

    private static String exclusion(String groupId, String artifactId) {
        return "<exclusion><groupId>" + groupId + "</groupId><artifactId>" + artifactId + "</artifactId></exclusion>";
    }
}

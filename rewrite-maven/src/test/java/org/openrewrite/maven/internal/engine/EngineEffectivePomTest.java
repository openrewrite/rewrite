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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelBuildingResult;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.openrewrite.maven.internal.MavenParsingException;
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

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end coverage of {@link EngineEffectivePom} against the real shaded engine transport: a parent chain +
 * single/multi-level BOM imports served over MockWebServer (proving CacheBridge + HttpSenderTransporter + scratch LRM),
 * the warm bytes-region and known-absent short-circuits, Maven-identical cycle/self-parent failures, and the reactor +
 * epoch staleness guard.
 */
class EngineEffectivePomTest {

    private static final String G = "com.example";

    private static HttpUrlConnectionSender sender() {
        return new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10));
    }

    // (a) Effective model from a parent chain + single- and multi-level BOM import, over the real transport.
    @Test
    void effectiveModelFromParentChainAndBomImports(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            server.pom(G, "grandparent", "1", project("grandparent", "", "pom", dm(managed("libgp", "1.0"))))
                    .pom(G, "parent", "1", project("parent", parent("grandparent"), "pom",
                            "<properties><prop.from.parent>P</prop.from.parent></properties>" + dm(managed("libp", "1.0"))))
                    .pom(G, "bom", "1", project("bom", "", "pom", dm(managed("libbom", "2.0"))))
                    .pom(G, "bom-parent", "1", project("bom-parent", "", "pom", dm(managed("libouter", "3.0"))))
                    .pom(G, "bom-outer", "1", project("bom-outer", parent("bom-parent"), "pom", ""));

            String rootXml = project("app", parent("parent"), "jar",
                    dm(importBom("bom") + importBom("bom-outer")) +
                    "<dependencies>" + dep("libp") + dep("libbom") + "</dependencies>");

            EngineModelBuildingOutcome outcome = build(engine, server, tmp, ctx(new InMemoryMavenPomCache()),
                    emptyReactor(), rootXml);

            assertTrue(outcome.isSuccess(), () -> "expected success, got " + outcome.getFailure());
            Model effective = outcome.getResult().getEffectiveModel();

            // Inheritance from the parent chain.
            assertEquals("P", effective.getProperties().getProperty("prop.from.parent"));
            assertEquals("1.0", managed(effective, "libp").getVersion());
            assertEquals("1.0", managed(effective, "libgp").getVersion());
            // BOM-imported managed entries, single and multi-level.
            assertEquals("2.0", managed(effective, "libbom").getVersion());
            assertEquals("3.0", managed(effective, "libouter").getVersion());
            // Managed versions injected into the declared dependencies.
            assertEquals("1.0", dependency(effective, "libp").getVersion());
            assertEquals("2.0", dependency(effective, "libbom").getVersion());

            // InputLocation attribution: parent-declared vs BOM-declared.
            assertEquals(G + ":parent:1", managed(effective, "libp").getLocation("version").getSource().getModelId());
            assertEquals(G + ":bom:1", managed(effective, "libbom").getLocation("version").getSource().getModelId());

            // Raw lineage.
            ModelBuildingResult result = outcome.getResult();
            assertTrue(result.getModelIds().contains(G + ":app:1"));
            assertTrue(result.getModelIds().contains(G + ":parent:1"));

            // gav -> repository attribution for everything resolved over the wire.
            Set<String> served = new HashSet<>();
            outcome.getServedBy().forEach((gav, repo) -> {
                served.add(gav.toString());
                assertEquals(server.baseUrl(), repo.getUri());
            });
            assertTrue(served.containsAll(Arrays.asList(
                    G + ":grandparent:1", G + ":parent:1", G + ":bom:1", G + ":bom-outer:1", G + ":bom-parent:1")),
                    () -> "served=" + served);

            // Model building reads poms only, and only the five remote poms (the root is supplied as bytes).
            assertEquals(5, pomGets(server), () -> "requests=" + server.requests);
        }
    }

    // (b) A second build with a fresh engine but a warm MavenPomCache performs ZERO network requests.
    @Test
    void warmBytesRegionServesSecondBuildWithZeroNetwork(@TempDir Path tmp) throws Exception {
        InMemoryMavenPomCache warmCache = new InMemoryMavenPomCache();
        ExecutionContext ctx = ctx(warmCache);
        String rootXml = project("app", parent("parent"), "jar", "");

        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "parent", "1", project("parent", "", "pom", dm(managed("libp", "1.0"))));

            try (MavenEngine cold = new MavenEngine()) {
                EngineModelBuildingOutcome outcome = build(cold, server, tmp.resolve("cold"), ctx, emptyReactor(), rootXml);
                assertTrue(outcome.isSuccess());
            }
            int afterCold = server.requestCount();
            assertEquals(1, pomGets(server));

            // Fresh engine (fresh aether cache) forces the ModelResolver -> CacheBridge -> bytes-region path.
            try (MavenEngine warm = new MavenEngine()) {
                EngineModelBuildingOutcome outcome = build(warm, server, tmp.resolve("warm"), ctx, emptyReactor(), rootXml);
                assertTrue(outcome.isSuccess());
                assertEquals("1.0", managed(outcome.getResult().getEffectiveModel(), "libp").getVersion());
            }
            assertEquals(afterCold, server.requestCount(), () -> "warm build hit the network: " + server.requests);
        }
    }

    // (c) A known-absent bytes entry short-circuits with no I/O and maps to a downloading failure.
    @Test
    void knownAbsentBytesEntryShortCircuitsWithoutNetwork(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            InMemoryMavenPomCache cache = new InMemoryMavenPomCache();
            // Record the parent as known-absent for this repository.
            cache.putPomBytes(new ResolvedGroupArtifactVersion(server.baseUrl(), G, "parent", "1", null), null);

            String rootXml = project("app", parent("parent"), "jar", "");
            EngineModelBuildingOutcome outcome = build(engine, server, tmp, ctx(cache), emptyReactor(), rootXml);

            assertFalse(outcome.isSuccess());
            assertInstanceOf(MavenDownloadingException.class, outcome.getFailure());
            assertEquals(0, server.requestCount(), () -> "known-absent must not touch the network: " + server.requests);
        }
    }

    // (d) Parent cycle and self-parent fail Maven-identically (no leniency), mapped to MavenParsingException.
    @Test
    void parentCycleFailsMavenIdentically(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            server.pom(G, "a", "1", project("a", parent("b"), "pom", ""))
                    .pom(G, "b", "1", project("b", parent("a"), "pom", ""));

            String rootXml = project("a", parent("b"), "pom", "");
            EngineModelBuildingOutcome outcome = build(engine, server, tmp, ctx(new InMemoryMavenPomCache()),
                    emptyReactor(), rootXml);

            assertFalse(outcome.isSuccess());
            assertInstanceOf(MavenParsingException.class, outcome.getFailure());
            assertTrue(outcome.getFailure().getMessage().contains("cycle"),
                    () -> "message=" + outcome.getFailure().getMessage());
        }
    }

    @Test
    void selfParentFailsMavenIdentically(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            String rootXml = project("s", parent("s"), "pom", "");
            EngineModelBuildingOutcome outcome = build(engine, server, tmp, ctx(new InMemoryMavenPomCache()),
                    emptyReactor(), rootXml);

            assertFalse(outcome.isSuccess());
            assertInstanceOf(MavenParsingException.class, outcome.getFailure());
            assertTrue(outcome.getFailure().getMessage().contains("groupId:artifactId"),
                    () -> "message=" + outcome.getFailure().getMessage());
            assertEquals(0, server.requestCount(), "self-parent aborts at raw validation, before any resolution");
        }
    }

    // (e) Reactor parent beats remote; an in-place mutation is only picked up after an epoch bump.
    @Test
    void reactorParentBeatsRemoteAndEpochBumpInvalidatesStaleModel(@TempDir Path tmp) throws Exception {
        Path parentPath = Paths.get("parent", "pom.xml");
        Map<Path, byte[]> bytesByPath = new HashMap<>();
        bytesByPath.put(parentPath, reactorParent("workspace1").getBytes(StandardCharsets.UTF_8));

        Map<Path, Pom> projectPoms = new HashMap<>();
        projectPoms.put(parentPath, parse(reactorParent("workspace1"), parentPath));
        ReactorWorkspace reactor = new ReactorWorkspace(projectPoms, bytesByPath::get);

        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            // A different remote parent proves the workspace copy wins (and is never fetched).
            server.pom(G, "parent", "1", project("parent", "", "pom", "<properties><owner>remote</owner></properties>"));

            String rootXml = project("app", parent("parent"), "jar", "");
            ExecutionContext ctx = ctx(new InMemoryMavenPomCache());
            try (CloseableSession session = engine.newSession(tmp.resolve("lrm"), SessionConfig.forSender(sender()))) {
                EngineEffectivePom service = new EngineEffectivePom(engine.getRepositorySystem(), session,
                        repos(server), tmp.resolve("materialize"));

                EngineModelBuildingOutcome first = service.build(rootXml.getBytes(StandardCharsets.UTF_8),
                        requestedPom(), settings(), reactor, ctx);
                assertEquals("workspace1", first.getResult().getEffectiveModel().getProperties().getProperty("owner"));
                assertEquals(0, server.requestCount(), "reactor parent must beat the remote one");

                // In-place mutation without an epoch bump: the cached model is still served.
                bytesByPath.put(parentPath, reactorParent("workspace2").getBytes(StandardCharsets.UTF_8));
                EngineModelBuildingOutcome stale = service.build(rootXml.getBytes(StandardCharsets.UTF_8),
                        requestedPom(), settings(), reactor, ctx);
                assertEquals("workspace1", stale.getResult().getEffectiveModel().getProperties().getProperty("owner"),
                        "without an epoch bump the mutated pom must not be re-read");

                // Epoch bump invalidates the cached raw/effective model.
                reactor.bumpEpoch();
                EngineModelBuildingOutcome fresh = service.build(rootXml.getBytes(StandardCharsets.UTF_8),
                        requestedPom(), settings(), reactor, ctx);
                assertEquals("workspace2", fresh.getResult().getEffectiveModel().getProperties().getProperty("owner"),
                        "the epoch bump must force a re-read of the mutated pom");
                assertEquals(0, server.requestCount(), "the whole exchange stays inside the reactor");
            }
        }
    }

    // (f) ${revision} parent resolution through the workspace.
    @Test
    void revisionParentResolvedThroughWorkspace(@TempDir Path tmp) throws Exception {
        Path parentPath = Paths.get("parent", "pom.xml");
        String parentXml = "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<groupId>" + G + "</groupId><artifactId>rparent</artifactId><version>${revision}</version>" +
                "<packaging>pom</packaging>" +
                "<properties><revision>1.0.0</revision></properties>" + dm(managed("libr", "9.9")) + "</project>";
        Map<Path, byte[]> bytesByPath = new HashMap<>();
        bytesByPath.put(parentPath, parentXml.getBytes(StandardCharsets.UTF_8));
        Map<Path, Pom> projectPoms = new HashMap<>();
        projectPoms.put(parentPath, parse(parentXml, parentPath));
        ReactorWorkspace reactor = new ReactorWorkspace(projectPoms, bytesByPath::get);

        String rootXml = "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<parent><groupId>" + G + "</groupId><artifactId>rparent</artifactId><version>${revision}</version></parent>" +
                "<artifactId>rchild</artifactId><packaging>jar</packaging></project>";

        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            EngineModelBuildingOutcome outcome = build(engine, server, tmp, ctx(new InMemoryMavenPomCache()), reactor, rootXml);

            assertTrue(outcome.isSuccess(), () -> "expected success, got " + outcome.getFailure());
            Model effective = outcome.getResult().getEffectiveModel();
            assertEquals("1.0.0", effective.getVersion());
            assertEquals("9.9", managed(effective, "libr").getVersion());
            assertEquals(0, server.requestCount(), "the ${revision} parent resolves entirely through the workspace");
        }
    }

    // ---- harness ----

    private EngineModelBuildingOutcome build(MavenEngine engine, MockMavenServer server, Path tmp, ExecutionContext ctx,
                                             ReactorWorkspace reactor, String rootXml) throws Exception {
        try (CloseableSession session = engine.newSession(tmp.resolve("lrm"), SessionConfig.forSender(sender()))) {
            EngineEffectivePom service = new EngineEffectivePom(engine.getRepositorySystem(), session,
                    repos(server), tmp.resolve("materialize"));
            return service.build(rootXml.getBytes(StandardCharsets.UTF_8), requestedPom(), settings(), reactor, ctx);
        }
    }

    private static List<MavenRepository> repos(MockMavenServer server) {
        return singletonList(new MavenRepository("mock", server.baseUrl(), "true", "true", false, null, null, null, null));
    }

    private static ExecutionContext ctx(InMemoryMavenPomCache cache) {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenExecutionContextView.view(ctx).setPomCache(cache);
        return ctx;
    }

    private static EffectiveSettings settings() {
        return new EffectiveSettings(Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
    }

    private static ReactorWorkspace emptyReactor() {
        return new ReactorWorkspace(Collections.emptyMap(), (Function<Path, byte[]>) p -> null);
    }

    private static Pom requestedPom() {
        return Pom.builder()
                .sourcePath(Paths.get("pom.xml"))
                .gav(new ResolvedGroupArtifactVersion(null, G, "root", "1", null))
                .build();
    }

    private static Pom parse(String xml, Path path) {
        return RawPom.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null).toPom(path, null);
    }

    private static long pomGets(MockMavenServer server) {
        return server.requests.stream().filter(r -> r.startsWith("GET") && r.endsWith(".pom")).count();
    }

    private static String reactorParent(String owner) {
        return project("parent", "", "pom", "<properties><owner>" + owner + "</owner></properties>" + dm(managed("libw", "1.0")));
    }

    // ---- pom XML builders ----

    private static String project(String artifactId, String parentBlock, String packaging, String body) {
        return "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<groupId>" + G + "</groupId>" +
                "<artifactId>" + artifactId + "</artifactId>" +
                "<version>1</version>" +
                "<packaging>" + packaging + "</packaging>" +
                parentBlock + body + "</project>";
    }

    private static String parent(String artifactId) {
        return "<parent><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId><version>1</version></parent>";
    }

    private static String dm(String entries) {
        return "<dependencyManagement><dependencies>" + entries + "</dependencies></dependencyManagement>";
    }

    private static String managed(String artifactId, String version) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId><version>" + version + "</version></dependency>";
    }

    private static String importBom(String artifactId) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId><version>1</version><type>pom</type><scope>import</scope></dependency>";
    }

    private static String dep(String artifactId) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId></dependency>";
    }

    private static Dependency managed(Model model, String artifactId) {
        for (Dependency d : model.getDependencyManagement().getDependencies()) {
            if (artifactId.equals(d.getArtifactId())) {
                return d;
            }
        }
        throw new AssertionError("managed dependency not found: " + artifactId);
    }

    private static Dependency dependency(Model model, String artifactId) {
        for (Dependency d : model.getDependencies()) {
            if (artifactId.equals(d.getArtifactId())) {
                return d;
            }
        }
        throw new AssertionError("dependency not found: " + artifactId);
    }
}

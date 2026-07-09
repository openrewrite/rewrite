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
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * L-P2-E-003 regression: a BOM that imports a NESTED BOM whose version is a property (e.g. spring-boot-dependencies
 * importing {@code infinispan-bom:${infinispan.version}}) must resolve the nested import to the version the importing BOM
 * pins — not an older one whose managed set leaks in. Minimal, hermetic stand-in for the spring-boot-dependencies:3.2.4
 * shadow failures.
 */
class NestedImportBomTest {

    private static final String G = "com.example";

    @Test
    void nestedPropertyVersionedImportBomResolvesToThePinnedVersion(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            // The nested BOM at two versions: only 1.0 manages `legacy-only`, only 2.0 manages `current-only`.
            server.pom(G, "inner-bom", "1.0", project("inner-bom", "pom",
                            dm(managed("legacy-only", "1.0"))))
                    .pom(G, "inner-bom", "2.0", project("inner-bom", "pom",
                            dm(managed("current-only", "2.0"))))
                    // The outer BOM pins inner.version=2.0 and imports inner-bom:${inner.version}.
                    .pom(G, "outer-bom", "1.0", project("outer-bom", "pom",
                            "<properties><inner.version>2.0</inner.version></properties>" +
                            dm(importBom("inner-bom", "${inner.version}"))));

            String rootXml = project("app", "jar", dm(importBom("outer-bom", "1.0")));

            Model effective = build(engine, server, tmp, rootXml);

            assertNotNull(managed(effective, "current-only"), "the nested import BOM resolved to the pinned 2.0");
            assertNull(managed(effective, "legacy-only"),
                    () -> "the nested import BOM must NOT resolve to 1.0; DM=" + gavs(effective));
        }
    }

    // The importing app carries a COMPETING inner.version — the nested import must still resolve in the outer BOM's own
    // property context (2.0), not leak the root app's value (1.0).
    @Test
    void nestedImportUsesTheImportingBomsPropertyNotTheRootApps(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            server.pom(G, "inner-bom", "1.0", project("inner-bom", "pom", dm(managed("legacy-only", "1.0"))))
                    .pom(G, "inner-bom", "2.0", project("inner-bom", "pom", dm(managed("current-only", "2.0"))))
                    .pom(G, "outer-bom", "1.0", project("outer-bom", "pom",
                            "<properties><inner.version>2.0</inner.version></properties>" +
                            dm(importBom("inner-bom", "${inner.version}"))));

            String rootXml = project("app", "jar",
                    "<properties><inner.version>1.0</inner.version></properties>" +
                    dm(importBom("outer-bom", "1.0")));

            Model effective = build(engine, server, tmp, rootXml);

            assertNotNull(managed(effective, "current-only"),
                    () -> "the nested import must use the outer BOM's inner.version=2.0; DM=" + gavs(effective));
            assertNull(managed(effective, "legacy-only"),
                    () -> "the root app's inner.version=1.0 must not leak into the nested import; DM=" + gavs(effective));
        }
    }

    // L-P2-E-003 (root cause): a transitively imported BOM carries a <profile> activated by a property-value NEGATION
    // (`release-mode != downstream`, active when the property is unset). Maven activates it and includes its managed
    // entries; the engine must too. (Legacy's env-var-based activation drops them — the parity flip, masked by
    // `dm-superset:$.pom.dependencyManagement`.) This is the minimal stand-in for infinispan-bom's `community` profile
    // inside spring-boot-dependencies:3.2.4.
    @Test
    void profileNegationActivatedImportsAreIncluded(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer();
             MavenEngine engine = new MavenEngine()) {
            String communityProfile =
                    "<profiles><profile><id>community</id>" +
                    "<activation><property><name>release-mode</name><value>!downstream</value></property></activation>" +
                    dm(managed("profile-managed", "1.0")) +
                    "</profile></profiles>";
            server.pom(G, "inner-bom", "1.0", project("inner-bom", "pom",
                            dm(managed("always-managed", "1.0")) + communityProfile))
                    .pom(G, "outer-bom", "1.0", project("outer-bom", "pom", dm(importBom("inner-bom", "1.0"))));

            String rootXml = project("app", "jar", dm(importBom("outer-bom", "1.0")));

            Model effective = build(engine, server, tmp, rootXml);

            assertNotNull(managed(effective, "always-managed"), "the always-managed entry is present");
            assertNotNull(managed(effective, "profile-managed"),
                    () -> "the property-negation-activated profile's managed entry must be included; DM=" + gavs(effective));
        }
    }

    // ---- harness ----

    private Model build(MavenEngine engine, MockMavenServer server, Path tmp, String rootXml) throws Exception {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenExecutionContextView.view(ctx).setPomCache(new InMemoryMavenPomCache());
        Pom requested = RawPom.parse(new ByteArrayInputStream(rootXml.getBytes(StandardCharsets.UTF_8)), null)
                .toPom(Paths.get("pom.xml"), null);
        List<MavenRepository> repositories = singletonList(
                new MavenRepository("mock", server.baseUrl(), "true", "true", false, null, null, null, null));
        EffectiveSettings settings = new EffectiveSettings(emptyList(), emptyList(), emptyMap());
        ReactorWorkspace reactor = new ReactorWorkspace(emptyMap(), (Function<Path, byte[]>) p -> null);
        try (CloseableSession session = engine.newSession(tmp.resolve("lrm"), SessionConfig.forSender(sender()))) {
            EngineEffectivePom service = new EngineEffectivePom(
                    engine.getRepositorySystem(), session, repositories, null);
            EngineModelBuildingOutcome outcome = service.build(
                    rootXml.getBytes(StandardCharsets.UTF_8), requested, settings, reactor, ctx);
            assertTrue(outcome.isSuccess(), () -> "model build failed: " + outcome.getFailure());
            return outcome.getResult().getEffectiveModel();
        }
    }

    private static HttpUrlConnectionSender sender() {
        return new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10));
    }

    private static Dependency managed(Model model, String artifactId) {
        if (model.getDependencyManagement() == null) {
            return null;
        }
        for (Dependency d : model.getDependencyManagement().getDependencies()) {
            if (artifactId.equals(d.getArtifactId())) {
                return d;
            }
        }
        return null;
    }

    private static String gavs(Model model) {
        StringBuilder sb = new StringBuilder();
        for (Dependency d : model.getDependencyManagement().getDependencies()) {
            sb.append(d.getArtifactId()).append(':').append(d.getVersion()).append(' ');
        }
        return sb.toString();
    }

    private static String project(String artifactId, String packaging, String body) {
        return "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<groupId>" + G + "</groupId>" +
                "<artifactId>" + artifactId + "</artifactId>" +
                "<version>1</version>" +
                "<packaging>" + packaging + "</packaging>" +
                body + "</project>";
    }

    private static String dm(String entries) {
        return "<dependencyManagement><dependencies>" + entries + "</dependencies></dependencyManagement>";
    }

    private static String managed(String artifactId, String version) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId><version>" +
                version + "</version></dependency>";
    }

    private static String importBom(String artifactId, String version) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId><version>" +
                version + "</version><type>pom</type><scope>import</scope></dependency>";
    }
}

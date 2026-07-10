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
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.internal.engine.EngineFixtureHarness.GraphResolution;
import org.openrewrite.maven.parity.ParityHarness;
import org.openrewrite.maven.tree.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit probes on {@link DependencyGraphMapper} output: the instance-identity contracts (§4.2 / a3 §2) and the
 * partial-failure aggregation shape. The full fixture differential lives in
 * {@code org.openrewrite.maven.parity.EngineDependencyGraphComparisonTest}; this pins the invariants a snapshot diff
 * cannot see (object {@code ==} sharing) and the failure path a hermetic fixture cannot easily stage.
 */
class DependencyGraphMapperTest {

    private static final String G = "test";

    // (b) requested is threaded at depth 0 (== the requested-pom Dependency); within a scope the flat list and the
    // nested child graph share instances; across scopes the same coordinate is a distinct instance.
    @Test
    void instanceIdentityContracts() {
        GraphResolution resolution = EngineFixtureHarness.resolveGraph("conflict-unequal-depth");
        MavenResolutionResult mrr = new MavenResolutionResult(UUID.randomUUID(), null, resolution.getPom(),
                emptyList(), null, resolution.getDependencies(), null, resolution.getActiveProfiles(),
                resolution.getInjectedProperties());

        List<ResolvedDependency> compile = mrr.getDependencies().get(Scope.Compile);
        List<Dependency> requested = mrr.getPom().getRequestedDependencies();

        // depth-0 requested threading: the requested is a requested-pom instance, and getResolvedDependency resolves it
        // by reference (to some scope's instance — it scans scopes in reverse-ordinal order, as legacy does).
        for (ResolvedDependency direct : compile) {
            if (direct.getDepth() == 0) {
                assertTrue(requested.stream().anyMatch(r -> r == direct.getRequested()),
                        () -> "depth-0 requested must be a requested-pom instance: " + direct.getGav());
                ResolvedDependency looked = mrr.getResolvedDependency(direct.getRequested());
                assertNotNull(looked, () -> "getResolvedDependency must resolve by reference: " + direct.getGav());
                assertEquals(direct.getGav(), looked.getGav());
            }
        }

        // within-scope sharing: a parent's nested child is the very object in the flat list.
        ResolvedDependency a = byArtifact(compile, "a");
        assertFalse(a.getDependencies().isEmpty(), "a has a transitive (c)");
        ResolvedDependency nestedC = a.getDependencies().get(0);
        assertSame(nestedC, byArtifact(compile, "c"), "nested child == flat-list instance within a scope");

        // cross-scope non-sharing: same coordinate, distinct instance per scope.
        List<ResolvedDependency> runtime = mrr.getDependencies().get(Scope.Runtime);
        ResolvedDependency compileA = byArtifact(compile, "a");
        ResolvedDependency runtimeA = byArtifact(runtime, "a");
        assertEquals(compileA.getGav(), runtimeA.getGav());
        assertNotSame(compileA, runtimeA, "instances are never shared across scopes");
    }

    // (c) The per-scope membership matrix on a fixture with all four scopes + optional + provided matches legacy exactly.
    @Test
    void perScopeMembershipMatchesLegacy() {
        Map<Scope, List<ResolvedDependency>> engine = EngineFixtureHarness.resolveGraph("scope-matrix").getDependencies();
        Map<Scope, List<ResolvedDependency>> legacy = ParityHarness.resolve("scope-matrix").getMarker().getDependencies();

        for (Scope scope : new Scope[]{Scope.Compile, Scope.Runtime, Scope.Test, Scope.Provided}) {
            assertEquals(coordinates(legacy.get(scope)), coordinates(engine.get(scope)),
                    () -> "scope " + scope + " membership must match legacy");
        }
        // Sanity: the four scopes are genuinely different (the fixture exercises scope filtering, not a degenerate case).
        assertThat(coordinates(engine.get(Scope.Compile))).contains("c-dep", "c-trans", "o-dep")
                .doesNotContain("r-dep", "t-dep", "p-dep");
        assertThat(coordinates(engine.get(Scope.Test))).contains("t-dep", "r-dep");
        assertThat(coordinates(engine.get(Scope.Provided))).contains("p-dep").doesNotContain("t-dep");
    }

    // (d) A direct dependency failing in one scope keeps the complete model: the resolvable scopes are fully populated
    // (partialResult), the failing scope is omitted, and the error is surfaced deduped by (root GA, failed GAV).
    @Test
    void partialFailureKeepsCompleteModelAndSurfacesError(@TempDir Path tmp) throws Exception {
        try (MockMavenServer server = new MockMavenServer()) {
            server.pom(G, "present", "1", project("present", "jar", ""));
            // test:missing is never served → its descriptor 404s.
            String rootXml = project("root", "jar",
                    "<dependencies>" +
                    dep("present", "1") +
                    "<dependency><groupId>" + G + "</groupId><artifactId>missing</artifactId><version>1</version>" +
                    "<scope>test</scope></dependency>" +
                    "</dependencies>");

            MavenDownloadingExceptions thrown = null;
            Map<Scope, List<ResolvedDependency>> partial = null;
            ExecutionContext ctx = ctx();
            try {
                map(server, tmp, ctx, rootXml);
                fail("expected a MavenDownloadingExceptions for the missing test-scoped dependency");
            } catch (MavenDownloadingExceptions e) {
                thrown = e;
                assertNotNull(e.getPartialResult(), "the complete model is carried as partialResult");
                partial = e.getPartialResult().getDependencies();
            }

            // The error is surfaced once, attributed to the failed dependency.
            assertEquals(1, thrown.getExceptions().size(), "one exception, deduped across scopes");
            MavenDownloadingException failure = thrown.getExceptions().get(0);
            assertEquals("missing", failure.getRoot().getArtifactId());

            // Resolvable scopes are populated; the failing Test scope is omitted (mirrors the legacy per-scope catch).
            assertThat(partial).containsKeys(Scope.Compile, Scope.Runtime, Scope.Provided).doesNotContainKey(Scope.Test);
            assertThat(coordinates(partial.get(Scope.Compile))).containsExactly("present");
        }
    }

    // ---- harness ----

    private void map(MockMavenServer server, Path tmp, ExecutionContext ctx, String rootXml) throws Exception {
        Pom requested = parse(rootXml);
        List<MavenRepository> repositories = singletonList(
                new MavenRepository("mock", server.baseUrl(), "true", "true", false, null, null, null, null));
        EffectiveSettings settings = new EffectiveSettings(emptyList(), emptyList(), emptyMap());
        ReactorWorkspace reactor = new ReactorWorkspace(emptyMap(), (Function<Path, byte[]>) p -> null);

        try (MavenEngine engine = new MavenEngine();
             CloseableSession session = engine.newSession(tmp.resolve("lrm"), SessionConfig.forSender(sender()))) {
            EngineEffectivePom effectivePom = new EngineEffectivePom(
                    engine.getRepositorySystem(), session, repositories, null);
            EngineModelBuildingOutcome outcome = effectivePom.build(
                    rootXml.getBytes(StandardCharsets.UTF_8), requested, settings, reactor, ctx);
            assertTrue(outcome.isSuccess(), () -> "root model build failed: " + outcome.getFailure());
            Model effective = outcome.getResult().getEffectiveModel();
            BomGavAttributor attributor = new BomGavAttributor(effectivePom, settings, reactor, ctx,
                    MavenExecutionContextView.view(ctx).getPomCache());
            ResolvedPom pom = new EffectivePomMapper(MavenExecutionContextView.view(ctx).getPomCache(), attributor, reactor)
                    .map(outcome, requested, emptyList(), emptyMap());

            try (EngineDependencyCollector collector = new EngineDependencyCollector()) {
                EngineCollectOutcome collectOutcome = collector.collect(
                        effective, requested, repositories, settings, reactor, tmp.resolve("collect"), ctx);
                new DependencyGraphMapper(MavenExecutionContextView.view(ctx).getPomCache())
                        .map(collectOutcome, pom, requested, ctx);
            }
        }
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenExecutionContextView.view(ctx).setPomCache(new InMemoryMavenPomCache());
        HttpSenderExecutionContextView.view(ctx).setHttpSender(sender());
        return ctx;
    }

    private static HttpUrlConnectionSender sender() {
        return new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10));
    }

    private static Pom parse(String xml) {
        return RawPom.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null).toPom(Paths.get("pom.xml"), null);
    }

    private static List<String> coordinates(List<ResolvedDependency> dependencies) {
        List<String> out = new ArrayList<>();
        for (ResolvedDependency d : dependencies) {
            out.add(d.getArtifactId());
        }
        Collections.sort(out);
        return out;
    }

    private static ResolvedDependency byArtifact(List<ResolvedDependency> dependencies, String artifactId) {
        return dependencies.stream().filter(d -> artifactId.equals(d.getArtifactId())).findFirst().orElseThrow(AssertionError::new);
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

    private static String dep(String artifactId, String version) {
        return "<dependency><groupId>" + G + "</groupId><artifactId>" + artifactId + "</artifactId>" +
                "<version>" + version + "</version></dependency>";
    }
}

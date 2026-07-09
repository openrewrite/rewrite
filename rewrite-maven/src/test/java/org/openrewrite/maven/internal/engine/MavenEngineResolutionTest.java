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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.ResolutionEngineSelector;
import org.openrewrite.maven.tree.*;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

class MavenEngineResolutionTest {

    // ---- PomXmlRegistry ----

    @Test
    void registryStoresAndReadsByRequestedPom() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        Path path = Paths.get("a/pom.xml");
        ResolvedGroupArtifactVersion gav = new ResolvedGroupArtifactVersion(null, "g", "a", "1", null);
        Pom source = Pom.builder().sourcePath(path).gav(gav).build();
        byte[] xml = "<project/>".getBytes(StandardCharsets.UTF_8);

        PomXmlRegistry.put(ctx, source, xml);

        // An equal (or identical) requested pom reads the stored bytes...
        assertThat(PomXmlRegistry.get(ctx, source)).isEqualTo(xml);
        assertThat(PomXmlRegistry.get(ctx, Pom.builder().sourcePath(path).gav(gav).build())).isEqualTo(xml);
        // ...but a mutated (non-equal) pom does not, so a re-resolution never reads stale bytes.
        assertThat(PomXmlRegistry.get(ctx, source.withName("changed"))).isNull();
        assertThat(PomXmlRegistry.get(ctx, Pom.builder().sourcePath(Paths.get("other/pom.xml")).gav(gav).build())).isNull();
        assertThat(PomXmlRegistry.pathSource(ctx).apply(path)).isEqualTo(xml);
    }

    @Test
    void registryEpochIsMonotonicAndInjectedPropertiesRoundTrip() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        assertThat(PomXmlRegistry.epoch(ctx)).isZero();
        assertThat(PomXmlRegistry.bumpEpoch(ctx)).isEqualTo(1);
        assertThat(PomXmlRegistry.bumpEpoch(ctx)).isEqualTo(2);
        assertThat(PomXmlRegistry.epoch(ctx)).isEqualTo(2);

        assertThat(PomXmlRegistry.injectedProperties(ctx)).isEmpty();
        PomXmlRegistry.setInjectedProperties(ctx, singletonMap("k", "v"));
        assertThat(PomXmlRegistry.injectedProperties(ctx)).containsEntry("k", "v");
    }

    // ---- mode routing (LEGACY / MAVEN / SHADOW) ----

    @Test
    void legacyModeProducesLegacyPluginOrder() {
        ResolvedPom pom = resolve("plugins-executions", "legacy").getPom();
        // L-P0-003: legacy re-appends the parent-merged enforcer to the end.
        assertThat(pluginGas(pom)).containsExactly("org.parity.plugins:own-plugin", "org.apache.maven.plugins:maven-enforcer-plugin");
    }

    @Test
    void mavenModeProducesEnginePluginOrder() {
        ResolvedPom pom = resolve("plugins-executions", "maven").getPom();
        // The engine yields Maven-correct inherited-first order (the L-P0-003 flip), proving MAVEN routing returns the engine pom.
        assertThat(pluginGas(pom)).containsExactly("org.apache.maven.plugins:maven-enforcer-plugin", "org.parity.plugins:own-plugin");
    }

    @Test
    void shadowModeReturnsLegacyResultAndMasksTheExpectedPluginFlip() {
        // The only legacy-vs-engine $.pom diff on this fixture is the ledgered L-P0-002/003 plugin flip; masks absorb it,
        // so shadow does not throw and returns the LEGACY plugin order (shadow never changes behavior).
        ResolvedPom pom = resolve("plugins-executions", "shadow").getPom();
        assertThat(pluginGas(pom)).containsExactly("org.parity.plugins:own-plugin", "org.apache.maven.plugins:maven-enforcer-plugin");
    }

    // ---- shadow comparison: report both outcomes ----

    @Test
    void identicalPomsDoNotDiff() {
        MavenResolutionResult mrr = resolve("parent-chain", "legacy");
        MavenEngineResolution.assertShadowParity(mrr.getPom().getRequested(), emptyList(), emptyMap(),
                new MavenEngineResolution.Attempt(mrr.getPom(), null),
                new MavenEngineResolution.Attempt(mrr.getPom(), null));
    }

    @Test
    void differentPomsAreAnUnexplainedDiff() {
        MavenResolutionResult a = resolve("parent-chain", "legacy");
        MavenResolutionResult b = resolve("bom-import-single", "legacy");
        assertThatThrownBy(() -> MavenEngineResolution.assertShadowParity(a.getPom().getRequested(), emptyList(), emptyMap(),
                new MavenEngineResolution.Attempt(a.getPom(), null),
                new MavenEngineResolution.Attempt(b.getPom(), null)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("$.pom");
    }

    @Test
    void oneEngineThrowingWhileTheOtherSucceedsIsReportedAsAnOutcomeMismatch() {
        MavenResolutionResult mrr = resolve("parent-chain", "legacy");
        MavenDownloadingException failure = new MavenDownloadingException(
                "boom", null, new GroupArtifactVersion("g", "a", "1"));
        assertThatThrownBy(() -> MavenEngineResolution.assertShadowParity(mrr.getPom().getRequested(), emptyList(), emptyMap(),
                new MavenEngineResolution.Attempt(mrr.getPom(), null),
                new MavenEngineResolution.Attempt(null, failure)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("outcome mismatch")
                .hasMessageContaining("legacy: resolved")
                .hasMessageContaining("engine: threw MavenDownloadingException");
    }

    @Test
    void bothEnginesThrowingIsAConsistentOutcome() {
        MavenResolutionResult mrr = resolve("parent-chain", "legacy");
        MavenDownloadingException failure = new MavenDownloadingException(
                "boom", null, new GroupArtifactVersion("g", "a", "1"));
        // Both failed: the effective-pom outcome agrees, so no assertion (the failing scopes are Phase-3's concern).
        MavenEngineResolution.assertShadowParity(mrr.getPom().getRequested(), emptyList(), emptyMap(),
                new MavenEngineResolution.Attempt(null, failure),
                new MavenEngineResolution.Attempt(null, failure));
    }

    // ---- dependency-graph routing (LEGACY / MAVEN / SHADOW at the resolveDependencies level) ----

    @Test
    void dependencyResolutionRoutesAcrossModesAndAgrees() {
        Map<Scope, List<String>> legacy = scopeGavs(resolve("scope-matrix", "legacy"));
        Map<Scope, List<String>> maven = scopeGavs(resolve("scope-matrix", "maven"));
        // Every scope is populated on both the legacy and the engine (MAVEN) path, and the engine's projection agrees
        // with legacy on this clean fixture — proving MAVEN routes resolveDependencies through the collect+map.
        assertThat(legacy).containsKeys(Scope.Compile, Scope.Runtime, Scope.Test, Scope.Provided);
        assertThat(maven).isEqualTo(legacy);
    }

    @Test
    void shadowDependencyModeReturnsLegacyWithoutThrowing() {
        // SHADOW routes both engines through resolveDependencies, diffs pom+scopes+errors, and (masks absorbing the
        // ledgered flips) returns the legacy graph. Reaching here without an AssertionError also proves the re-entrancy
        // guard: the legacy pass calls pom.resolveDependencies(scope) per scope while the facade is dispatching, and
        // those nested calls run legacy rather than re-collecting/re-comparing.
        Map<Scope, List<String>> shadow = scopeGavs(resolve("scope-matrix", "shadow"));
        Map<Scope, List<String>> legacy = scopeGavs(resolve("scope-matrix", "legacy"));
        assertThat(shadow).isEqualTo(legacy);
    }

    @Test
    void mavenModeNeverInvokesTheLegacyAllScopesLambda() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ResolutionEngineSelector.ENGINE_KEY, "legacy");
        MavenExecutionContextView.view(ctx).setPomCache(new InMemoryMavenPomCache());
        // A dependency-free pom resolves offline on the engine, so the routing is observable without any download.
        ResolvedPom pom = new ResolvedPom(
                Pom.builder().sourcePath(Paths.get("pom.xml"))
                        .gav(new ResolvedGroupArtifactVersion(null, "com.example", "leaf", "1.0.0", null)).build(),
                emptyList());
        MavenPomDownloader downloader = new MavenPomDownloader(emptyMap(), ctx);

        Map<Scope, List<ResolvedDependency>> sentinel = singletonMap(Scope.Compile, emptyList());
        // LEGACY dispatches to the lambda and returns it verbatim.
        assertThatCode(() -> assertThat(MavenEngineResolution.dependencyGraph(pom, emptyList(), downloader, ctx, () -> sentinel))
                .isSameAs(sentinel)).doesNotThrowAnyException();

        // MAVEN must never run the legacy lambda (it routes to the engine instead).
        ctx.putMessage(ResolutionEngineSelector.ENGINE_KEY, "maven");
        assertThatCode(() -> MavenEngineResolution.dependencyGraph(pom, emptyList(), downloader, ctx, () -> {
            throw new AssertionError("legacy lambda must not run in MAVEN mode");
        })).doesNotThrowAnyException();
    }

    // ---- helpers ----

    private static Map<Scope, List<String>> scopeGavs(MavenResolutionResult mrr) {
        Map<Scope, List<String>> out = new java.util.LinkedHashMap<>();
        mrr.getDependencies().forEach((scope, deps) -> out.put(scope, deps.stream()
                .map(d -> d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion())
                .collect(java.util.stream.Collectors.toList())));
        return out;
    }

    private static List<String> pluginGas(ResolvedPom pom) {
        return pom.getPlugins().stream().map(p -> p.getGroupId() + ":" + p.getArtifactId()).collect(java.util.stream.Collectors.toList());
    }

    private static MavenResolutionResult resolve(String fixture, String engine) {
        Path fixtureDir = fixturesRoot().resolve(fixture);
        Path repoDir = fixtureDir.resolve("repo");
        ExecutionContext base = new InMemoryExecutionContext(Throwable::printStackTrace);
        base.putMessage(ResolutionEngineSelector.ENGINE_KEY, engine);
        MavenExecutionContextView ctx = MavenExecutionContextView.view(base);
        ctx.setPomCache(new InMemoryMavenPomCache());
        ctx.setRepositories(singletonList(MavenRepository.builder()
                .id("fixture").uri(repoDir.toUri().toString()).knownToExist(true).build()));
        ctx.setAddCentralRepository(false);
        ctx.setAddLocalRepository(false);

        MavenParser parser = MavenParser.builder()
                .property("parity.repo.url", repoDir.toUri().toString())
                .build();
        SourceFile doc = parser.parseInputs(singletonList(Parser.Input.fromFile(fixtureDir.resolve("pom.xml"))), fixtureDir, ctx)
                .findFirst().orElseThrow(() -> new IllegalStateException("did not parse " + fixture));
        return doc.getMarkers().findFirst(MavenResolutionResult.class)
                .orElseThrow(() -> new IllegalStateException("no MavenResolutionResult for " + fixture));
    }

    private static Path fixturesRoot() {
        URL url = MavenEngineResolutionTest.class.getResource("/parity/fixtures");
        try {
            return Paths.get(java.util.Objects.requireNonNull(url).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}

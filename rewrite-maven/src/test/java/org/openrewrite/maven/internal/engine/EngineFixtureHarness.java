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

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.maven.tree.Scope;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * Drives the real shaded engine ({@link EngineEffectivePom} + {@link EffectivePomMapper} + {@link BomGavAttributor})
 * over the hermetic {@code parity/fixtures/}, mirroring {@link org.openrewrite.maven.parity.ParityHarness}: the fixture
 * {@code repo/} is the only repository (a {@code file://} repo through the engine's {@code FileTransporterFactory}), the
 * parser property {@code parity.repo.url} is supplied as both a user property (Maven interpolation) and an injected
 * property (the {@code getProperties()} overlay), and only {@code profile-activation} activates a profile.
 */
public final class EngineFixtureHarness {

    private static final Map<String, List<String>> ACTIVE_PROFILES = Map.of("profile-activation", List.of("explicit"));

    private EngineFixtureHarness() {
    }

    public static EngineResolution resolve(String fixture) {
        Path fixtureDir = fixturesRoot().resolve(fixture);
        Path repoDir = fixtureDir.resolve("repo");
        Path pomPath = fixtureDir.resolve("pom.xml");
        String repoUri = repoDir.toUri().toString();

        List<String> activeProfiles = ACTIVE_PROFILES.getOrDefault(fixture, emptyList());
        Map<String, String> injected = Map.of("parity.repo.url", repoUri);

        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenExecutionContextView.view(ctx).setPomCache(new InMemoryMavenPomCache());

        try {
            byte[] xml = Files.readAllBytes(pomPath);
            Pom requested = RawPom.parse(new ByteArrayInputStream(xml), null).toPom(pomPath, null);
            // Bake injected properties into the project pom's own properties exactly as MavenParser/ParityHarness do
            // (MavenParser ~L83 putAll), so the engine sees the same requested-pom overlay the legacy side compares against.
            if (requested.getProperties().isEmpty()) {
                requested = requested.withProperties(new LinkedHashMap<>());
            }
            requested.getProperties().putAll(injected);
            List<MavenRepository> repositories = singletonList(MavenRepository.builder()
                    .id("fixture").uri(repoUri).knownToExist(true).build());
            EffectiveSettings settings = new EffectiveSettings(emptyList(), activeProfiles, injected);
            ReactorWorkspace reactor = new ReactorWorkspace(emptyMap(), p -> null);

            Path scratch = Files.createTempDirectory("rewrite-engine-");
            try (MavenEngine engine = new MavenEngine();
                 CloseableSession session = engine.newSession(scratch.resolve("lrm"), SessionConfig.forSender(sender()))) {
                EngineEffectivePom service = new EngineEffectivePom(
                        engine.getRepositorySystem(), session, repositories, null);
                EngineModelBuildingOutcome outcome = service.build(xml, requested, settings, reactor, ctx);
                if (!outcome.isSuccess()) {
                    throw new IllegalStateException("Engine failed to resolve fixture " + fixture, outcome.getFailure());
                }
                BomGavAttributor attributor = new BomGavAttributor(
                        service, settings, reactor, ctx, MavenExecutionContextView.view(ctx).getPomCache());
                EffectivePomMapper mapper = new EffectivePomMapper(
                        MavenExecutionContextView.view(ctx).getPomCache(), attributor, reactor);
                ResolvedPom pom = mapper.map(outcome, requested, activeProfiles, injected);
                return new EngineResolution(fixture, fixtureDir, pom, requested, activeProfiles, injected, ctx);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The slice-B path: resolve the effective pom (B1 + B2 mappers) then run one verbose collect ({@link
     * EngineDependencyCollector}) and project it into per-scope {@link ResolvedDependency} lists ({@link
     * DependencyGraphMapper}). A direct-dependency failure is caught and its {@code partialResult} dependencies returned,
     * mirroring the complete-model contract the facade will surface.
     */
    public static GraphResolution resolveGraph(String fixture) {
        Path fixtureDir = fixturesRoot().resolve(fixture);
        Path repoDir = fixtureDir.resolve("repo");
        Path pomPath = fixtureDir.resolve("pom.xml");
        String repoUri = repoDir.toUri().toString();

        List<String> activeProfiles = ACTIVE_PROFILES.getOrDefault(fixture, emptyList());
        Map<String, String> injected = Map.of("parity.repo.url", repoUri);

        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenExecutionContextView.view(ctx).setPomCache(new InMemoryMavenPomCache());

        try {
            byte[] xml = Files.readAllBytes(pomPath);
            Pom requested = RawPom.parse(new ByteArrayInputStream(xml), null).toPom(pomPath, null);
            if (requested.getProperties().isEmpty()) {
                requested = requested.withProperties(new LinkedHashMap<>());
            }
            requested.getProperties().putAll(injected);
            List<MavenRepository> repositories = singletonList(MavenRepository.builder()
                    .id("fixture").uri(repoUri).knownToExist(true).build());
            EffectiveSettings settings = new EffectiveSettings(emptyList(), activeProfiles, injected);
            ReactorWorkspace reactor = new ReactorWorkspace(emptyMap(), p -> null);

            Path scratch = Files.createTempDirectory("rewrite-engine-graph-");
            ResolvedPom pom;
            Model effective;
            try (MavenEngine engine = new MavenEngine();
                 CloseableSession session = engine.newSession(scratch.resolve("lrm"), SessionConfig.forSender(sender()))) {
                EngineEffectivePom service = new EngineEffectivePom(
                        engine.getRepositorySystem(), session, repositories, null);
                EngineModelBuildingOutcome outcome = service.build(xml, requested, settings, reactor, ctx);
                if (!outcome.isSuccess()) {
                    throw new IllegalStateException("Engine failed to resolve fixture " + fixture, outcome.getFailure());
                }
                effective = requireNonNull(outcome.getResult()).getEffectiveModel();
                BomGavAttributor attributor = new BomGavAttributor(
                        service, settings, reactor, ctx, MavenExecutionContextView.view(ctx).getPomCache());
                EffectivePomMapper mapper = new EffectivePomMapper(
                        MavenExecutionContextView.view(ctx).getPomCache(), attributor, reactor);
                pom = mapper.map(outcome, requested, activeProfiles, injected);
            }

            Map<Scope, List<ResolvedDependency>> dependencies;
            MavenDownloadingExceptions failure = null;
            try (EngineDependencyCollector collector = new EngineDependencyCollector()) {
                EngineCollectOutcome collectOutcome = collector.collect(
                        effective, requested, repositories, settings, reactor, scratch.resolve("collect"), ctx);
                DependencyGraphMapper graphMapper =
                        new DependencyGraphMapper(MavenExecutionContextView.view(ctx).getPomCache());
                try {
                    dependencies = graphMapper.map(collectOutcome, pom, requested, ctx);
                } catch (MavenDownloadingExceptions e) {
                    failure = e;
                    dependencies = e.getPartialResult() == null ? new LinkedHashMap<>() :
                            e.getPartialResult().getDependencies();
                }
            }
            return new GraphResolution(fixture, fixtureDir, pom, requested, activeProfiles, injected, dependencies, failure, ctx);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HttpUrlConnectionSender sender() {
        return new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10));
    }

    private static Path fixturesRoot() {
        URL url = requireNonNull(EngineFixtureHarness.class.getResource("/parity/fixtures"), "parity/fixtures not on the test classpath");
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Value
    public static class EngineResolution {
        String fixture;
        Path fixtureDir;
        ResolvedPom pom;
        Pom requested;
        List<String> activeProfiles;
        Map<String, String> injectedProperties;
        ExecutionContext ctx;
    }

    @Value
    public static class GraphResolution {
        String fixture;
        Path fixtureDir;
        ResolvedPom pom;
        Pom requested;
        List<String> activeProfiles;
        Map<String, String> injectedProperties;
        Map<Scope, List<ResolvedDependency>> dependencies;
        @org.jspecify.annotations.Nullable MavenDownloadingExceptions failure;
        ExecutionContext ctx;
    }
}

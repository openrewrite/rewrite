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
package org.openrewrite.maven.parity;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Resolves the hermetic fixtures under {@code src/test/resources/parity/fixtures/}: each fixture
 * is a project {@code pom.xml} plus a {@code repo/} directory served as a {@code file://} maven
 * repository. No network access, ever: the fixture repository is the only repository, and both
 * Maven Central and the local repository are disabled. Every resolution uses a completely fresh
 * context and in-memory pom cache.
 */
public class ParityHarness {
    private static final Map<String, List<String>> ACTIVE_PROFILES = Map.of("profile-activation", List.of("explicit"));

    public static List<String> fixtureNames() {
        try (Stream<Path> dirs = Files.list(fixturesRoot())) {
            return dirs.filter(Files::isDirectory)
                    .map(dir -> dir.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path fixturesRoot() {
        URL url = requireNonNull(ParityHarness.class.getResource("/parity/fixtures"), "parity/fixtures not on the test classpath");
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Resolution resolve(String fixture) {
        Path fixtureDir = fixturesRoot().resolve(fixture);
        Path repoDir = fixtureDir.resolve("repo");

        List<Throwable> errors = new ArrayList<>();
        RecordingResolutionListener listener = new RecordingResolutionListener();
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext(errors::add));
        ctx.setPomCache(new InMemoryMavenPomCache());
        ctx.setRepositories(singletonList(MavenRepository.builder()
                .id("fixture")
                .uri(repoDir.toUri().toString())
                .knownToExist(true)
                .build()));
        ctx.setAddCentralRepository(false);
        ctx.setAddLocalRepository(false);
        ctx.setResolutionListener(listener);

        MavenParser parser = MavenParser.builder()
                .activeProfiles(ACTIVE_PROFILES.getOrDefault(fixture, List.of()).toArray(new String[0]))
                .property("parity.repo.url", repoDir.toUri().toString())
                .build();
        SourceFile doc = parser.parseInputs(singletonList(Parser.Input.fromFile(fixtureDir.resolve("pom.xml"))), fixtureDir, ctx)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fixture " + fixture + " did not parse"));
        MavenResolutionResult marker = doc.getMarkers().findFirst(MavenResolutionResult.class)
                .orElseThrow(() -> new IllegalStateException("Fixture " + fixture + " resolved without a MavenResolutionResult marker; errors: " + errors));

        return new Resolution(fixture, marker, errors, listener, ctx, new SnapshotNormalizer(fixtureDir));
    }

    public static class Resolution {
        private final String fixture;
        private final MavenResolutionResult marker;
        private final List<Throwable> errors;
        private final RecordingResolutionListener listener;
        private final MavenExecutionContextView ctx;
        private final SnapshotNormalizer normalizer;
        private ResolutionSnapshot snapshot;

        Resolution(String fixture, MavenResolutionResult marker, List<Throwable> errors,
                   RecordingResolutionListener listener, MavenExecutionContextView ctx, SnapshotNormalizer normalizer) {
            this.fixture = fixture;
            this.marker = marker;
            this.errors = errors;
            this.listener = listener;
            this.ctx = ctx;
            this.normalizer = normalizer;
        }

        public String getFixture() {
            return fixture;
        }

        public MavenResolutionResult getMarker() {
            return marker;
        }

        public MavenExecutionContextView getCtx() {
            return ctx;
        }

        // Memoized: the resolve() identity probe appends events to the live listener
        public ResolutionSnapshot snapshot() {
            if (snapshot == null) {
                snapshot = ResolutionSnapshot.of(marker, errors, listener.getEvents(), normalizer, ctx);
            }
            return snapshot;
        }
    }
}

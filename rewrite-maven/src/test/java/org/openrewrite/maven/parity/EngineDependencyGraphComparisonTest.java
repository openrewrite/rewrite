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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.maven.internal.engine.EngineFixtureHarness;
import org.openrewrite.maven.internal.engine.EngineFixtureHarness.GraphResolution;
import org.openrewrite.maven.internal.parity.ResolutionDiff;
import org.openrewrite.maven.internal.parity.ResolutionSnapshot;
import org.openrewrite.maven.internal.parity.SnapshotNormalizer;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The slice-B acceptance gate: for every {@code parity/fixtures/} set, resolve dependencies with the LEGACY engine (via
 * {@link ParityHarness}) and with the collect + {@code DependencyGraphMapper} path (via {@link EngineFixtureHarness}),
 * snapshot both through the same {@link ResolutionSnapshot} projection, and diff the {@code $.scopes} subtree (per-scope
 * ordered lists, nested-graph shape, depth, requested threading, licenses, effectiveExclusions, datedSnapshotVersion).
 * The bar is ZERO UNEXPLAINED diffs: every surviving diff must match a declared per-fixture expectation.
 */
class EngineDependencyGraphComparisonTest {

    /** Per-fixture expected {@code $.scopes} diff path-prefixes, each with a short label for its cause. */
    private static final Map<String, List<ExpectedDiff>> EXPECTED = Map.of(
            "classifiers", List.of(
                    // The engine is Maven-correct and resolves BOTH classifier variants (conflict key is
                    // g:a:classifier:type); legacy dedups directly-declared deps by g:a only (last declaration wins), so
                    // the no-classifier `multi:1.0` shadows out and only `multi:1.1:tests` survives. Every scope list
                    // therefore gains an entry under the engine.
                    new ExpectedDiff("$.scopes", "classifier-conflict-key")),
            "profile-activation", List.of(
                    // Active-profile dependencies are ordered by profile DECLARATION order under Maven
                    // (jdk-any before explicit), whereas legacy reverses profiles into precedence order and so
                    // contributes explicit-dep before jdk-dep. Pure ordering flip across all four scope lists.
                    new ExpectedDiff("$.scopes", "profile-declaration-order")),
            "relocation", List.of(
                    // `oldg:1.0` declares a <distributionManagement><relocation> to `newg`. Maven (and the
                    // engine's real maven-resolver descriptor read) follows it, resolving `newg:1.0`; legacy never reads
                    // <relocation>, so it keeps `oldg:1.0`. Engine is Maven-correct, legacy is the outlier (this is the
                    // real cause of the "org.apache.commons:commons-io vs commons-io:commons-io" divergence family).
                    new ExpectedDiff("$.scopes", "relocation-following")));

    static List<String> fixtures() {
        return ParityHarness.fixtureNames();
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void scopesMatchLegacyExceptExplainedFlips(String fixture) {
        ResolutionSnapshot legacy = ParityHarness.resolve(fixture).snapshot();

        GraphResolution engine = EngineFixtureHarness.resolveGraph(fixture);
        MavenResolutionResult wrapper = new MavenResolutionResult(
                UUID.randomUUID(), null, engine.getPom(), emptyList(), null,
                engine.getDependencies(), null, engine.getActiveProfiles(), engine.getInjectedProperties());
        ResolutionSnapshot engineSnapshot = ResolutionSnapshot.of(
                wrapper, emptyList(), emptyList(), new SnapshotNormalizer(engine.getFixtureDir()), null);

        List<ResolutionDiff.Entry> scopeDiffs = new ArrayList<>();
        for (ResolutionDiff.Entry entry : ResolutionDiff.between(legacy, engineSnapshot).getEntries()) {
            if (entry.getPath().startsWith("$.scopes")) {
                scopeDiffs.add(entry);
            }
        }

        List<ExpectedDiff> expected = EXPECTED.getOrDefault(fixture, emptyList());
        List<ResolutionDiff.Entry> unexplained = new ArrayList<>();
        List<ResolutionDiff.Entry> explained = new ArrayList<>();
        for (ResolutionDiff.Entry entry : scopeDiffs) {
            if (expected.stream().anyMatch(e -> entry.getPath().startsWith(e.getPathPrefix()))) {
                explained.add(entry);
            } else {
                unexplained.add(entry);
            }
        }

        assertThat(unexplained)
                .as("Unexplained $.scopes diffs for fixture '%s' (legacy=left, engine=right):%n%s", fixture, render(unexplained))
                .isEmpty();

        if (!expected.isEmpty()) {
            assertThat(explained)
                    .as("Fixture '%s' declares expected flips but produced none — expectation is stale", fixture)
                    .isNotEmpty();
        }
    }

    private static String render(List<ResolutionDiff.Entry> entries) {
        StringBuilder s = new StringBuilder();
        for (ResolutionDiff.Entry entry : entries) {
            s.append("  ").append(entry.getPath()).append(": ")
                    .append(entry.getLeft()).append(" != ").append(entry.getRight()).append('\n');
        }
        return s.toString();
    }

    private static class ExpectedDiff {
        private final String pathPrefix;
        private final String label;

        ExpectedDiff(String pathPrefix, String label) {
            this.pathPrefix = pathPrefix;
            this.label = label;
        }

        String getPathPrefix() {
            return pathPrefix;
        }
    }
}

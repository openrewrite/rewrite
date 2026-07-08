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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.maven.internal.engine.EngineFixtureHarness;
import org.openrewrite.maven.internal.engine.EngineFixtureHarness.EngineResolution;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The B2 acceptance gate: for every {@code parity/fixtures/} set, resolve the POM section with the LEGACY engine
 * (via {@link ParityHarness}) and with B1 + the B2 mappers (via {@link EngineFixtureHarness}), snapshot both through the
 * same {@link ResolutionSnapshot} projection, and diff the {@code $.pom} subtree only (scopes stay legacy-only until
 * Phase 3). The bar is ZERO UNEXPLAINED diffs: every surviving diff must match a declared per-fixture expectation that
 * cites a ledger row.
 */
class EngineResolvedPomComparisonTest {

    /**
     * Per-fixture expected {@code $.pom} diff path-prefixes, each citing its ledger row. Anything not covered here is an
     * UNEXPLAINED diff and fails the gate.
     */
    private static final Map<String, List<ExpectedDiff>> EXPECTED = Map.of(
            "plugins-executions", List.of(
                    // L-P0-002 (execution goal order via HashSet) + L-P0-003 (parent-merged plugin moved to end): the
                    // whole effective plugin list re-orders and the enforcer `shared` goals re-order under Maven's merge.
                    new ExpectedDiff("$.pom.plugins", "L-P0-002/L-P0-003")));

    static List<String> fixtures() {
        return ParityHarness.fixtureNames();
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void pomSectionMatchesLegacyExceptExplainedFlips(String fixture) {
        ResolutionSnapshot legacy = ParityHarness.resolve(fixture).snapshot();

        EngineResolution engine = EngineFixtureHarness.resolve(fixture);
        MavenResolutionResult wrapper = new MavenResolutionResult(
                UUID.randomUUID(), null, engine.getPom(), emptyList(), null,
                new LinkedHashMap<>(), null, engine.getActiveProfiles(), engine.getInjectedProperties());
        ResolutionSnapshot engineSnapshot = ResolutionSnapshot.of(
                wrapper, emptyList(), emptyList(), new SnapshotNormalizer(engine.getFixtureDir()), null);

        List<ResolutionDiff.Entry> pomDiffs = new ArrayList<>();
        for (ResolutionDiff.Entry entry : ResolutionDiff.between(legacy, engineSnapshot).getEntries()) {
            if (entry.getPath().startsWith("$.pom")) {
                pomDiffs.add(entry);
            }
        }

        List<ExpectedDiff> expected = EXPECTED.getOrDefault(fixture, emptyList());
        List<ResolutionDiff.Entry> unexplained = new ArrayList<>();
        List<ResolutionDiff.Entry> explained = new ArrayList<>();
        for (ResolutionDiff.Entry entry : pomDiffs) {
            if (expected.stream().anyMatch(e -> entry.getPath().startsWith(e.getPathPrefix()))) {
                explained.add(entry);
            } else {
                unexplained.add(entry);
            }
        }

        assertThat(unexplained)
                .as("Unexplained $.pom diffs for fixture '%s' (legacy=left, engine=right):%n%s", fixture, render(unexplained))
                .isEmpty();

        // A declared expectation must correspond to a real flip, otherwise the ledger citation is stale.
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
        private final String ledgerId;

        ExpectedDiff(String pathPrefix, String ledgerId) {
            this.pathPrefix = pathPrefix;
            this.ledgerId = ledgerId;
        }

        String getPathPrefix() {
            return pathPrefix;
        }
    }
}

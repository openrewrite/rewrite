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

import org.openrewrite.maven.internal.parity.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Old-vs-old gate: every fixture resolves twice with completely fresh, isolated caches and
 * contexts; the two snapshots must have an empty diff. Doubles as a nondeterminism audit of the
 * legacy engine — findings become ledger entries and masks, never silent tolerance.
 */
class DeterminismTest {

    static List<String> fixtures() {
        return ParityHarness.fixtureNames();
    }

    @Test
    void fixturesDiscovered() {
        assertThat(fixtures()).hasSizeGreaterThanOrEqualTo(11);
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void resolveTwiceIdentically(String fixture) {
        ResolutionSnapshot first = ParityHarness.resolve(fixture).snapshot();
        ResolutionSnapshot second = ParityHarness.resolve(fixture).snapshot();

        ResolutionDiff diff = ResolutionDiff.between(first, second).masked(SnapshotNormalizer.loadMasks());
        assertThat(diff.isEmpty())
          .as(() -> "Legacy engine resolved fixture '%s' nondeterministically:%n%s".formatted(fixture, diff.render()))
          .isTrue();
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void resolutionsAreHermeticAndErrorFree(String fixture) {
        ResolutionSnapshot snapshot = ParityHarness.resolve(fixture).snapshot();
        assertThat(snapshot.getJson().get("errors")).isEmpty();
        // Any surviving absolute path or port would make snapshots machine-dependent
        assertThat(snapshot.toJson()).doesNotContain("file:///Users", "file:///home", "file:///tmp", "localhost:");
    }
}

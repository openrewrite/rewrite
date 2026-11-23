/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.pnpm;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What semver-range-intersect 0.3.1's {@code intersect} answers for each row (pnpm/generate/semver-range-intersect.js
 * prints them), through pnpm's {@code safeIntersect}.
 */
class SemverRangeIntersectTest {

    @Test
    void anUnsatisfiableSetBesideASatisfiableOneLeavesTheSatisfiableOne() {
        assertThat(ResolveDependencies.safeIntersect(Arrays.asList("^1.0.0 || >=3.0.0 <2.0.0", "1")))
          .isEqualTo(">=1.0.0 <2.0.0-0");
    }

    @Test
    void anUnsatisfiableRangeIntersectsNothing() {
        assertThat(ResolveDependencies.safeIntersect(Arrays.asList(">=3.0.0 <2.0.0", "*"))).isNull();
    }

    @Test
    void overlappingCaretsNarrowToTheHigherLowerBound() {
        assertThat(ResolveDependencies.safeIntersect(Arrays.asList("^1.0.0", "^1.2.0"))).isEqualTo(">=1.2.0 <2.0.0-0");
    }

    @Test
    void disjointCaretsIntersectNothing() {
        assertThat(ResolveDependencies.safeIntersect(Arrays.asList("^1.0.0", "^2.0.0"))).isNull();
    }
}

/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.semver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LatestPatchTest {
    private final LatestPatch latestPatch = new LatestPatch(null);
    private final LatestPatch latestMetadataPatch = new LatestPatch("-fred");

    @Test
    void isValidWhenCurrentIsNull() {
        assertThat(latestPatch.isValid(null, "1.0.0")).isTrue();
    }

    @Test
    void isValid() {
        assertThat(latestPatch.isValid("1.0.0", "1.0.0")).isTrue();
        assertThat(latestPatch.isValid("1.0.0", "1.0.0.1")).isTrue();
        assertThat(latestPatch.isValid("1.0.0", "1.0.1")).isTrue();
        assertThat(latestPatch.isValid("1.0", "1.0.1")).isTrue();
        assertThat(latestPatch.isValid("1.0.0", "1.1.0")).isFalse();
        assertThat(latestPatch.isValid("1.0.0", "2.0.0")).isFalse();
        assertThat(latestPatch.isValid("1.0.x", "1.0.1")).isTrue();
        assertThat(latestPatch.isValid("1.0.x", "1.1.0")).isFalse();
    }

    @Test
    void noSnapshots() {
        assertThat(latestPatch.isValid("1.0.0", "1.0.1-SNAPSHOT")).isFalse();
    }

    @Test
    void staysWithinPatchRangeWhenMinorIsAbsentOrNonNumeric() {
        // Bare major: pin minor to 0, so only 1.0.x patches are eligible.
        assertThat(latestPatch.isValid("1", "1.0.1")).isTrue();
        assertThat(latestPatch.isValid("1", "1.1.0")).isFalse();
        assertThat(latestPatch.isValid("1", "2.0.0")).isFalse();
        assertThat(latestPatch.upgrade("1", List.of("1.0.5", "1.1.0", "2.0.0"))).contains("1.0.5");

        // Qualifier in the minor position: treat as 1.0.x, not "any 1.x".
        assertThat(latestPatch.isValid("1.Final", "1.0.1")).isTrue();
        assertThat(latestPatch.isValid("1.Final", "1.9.9")).isFalse();
        assertThat(latestPatch.isValid("1.Final", "2.0.0")).isFalse();
        assertThat(latestPatch.upgrade("1.Final", List.of("1.0.5", "1.9.9", "2.0.0"))).contains("1.0.5");
    }

    @Test
    void xRangeWildcardMinorAllowsAnyMinorWithinTheMajor() {
        // A wildcard in the minor position ("2.x"/"2.+") deliberately leaves the minor unspecified,
        // so any minor within the major is eligible, but a higher major is not.
        for (String current : List.of("2.x", "2.X", "2.+", "2.*")) {
            assertThat(latestPatch.isValid(current, "2.1.0")).as(current).isTrue();
            assertThat(latestPatch.isValid(current, "2.9.9")).as(current).isTrue();
            assertThat(latestPatch.isValid(current, "3.0.0")).as(current).isFalse();
            assertThat(latestPatch.upgrade(current, List.of("2.1.0", "2.9.9", "3.0.0"))).contains("2.9.9");
        }
    }

    @Test
    void upgrade() {
        var upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.0"));
        assertThat(upgrade).isEmpty();

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.11.0"));
        assertThat(upgrade).isEmpty();
        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.9"));
        assertThat(upgrade).isEmpty();

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.11"));
        assertThat(upgrade).isPresent();
        assertThat(upgrade.get()).isEqualTo("2.10.11");

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.10.3.23"));
        assertThat(upgrade).isEmpty();

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.10.2.25"));
        assertThat(upgrade).isEmpty();

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.10.3.25"));
        assertThat(upgrade).isPresent();
        assertThat(upgrade.get()).isEqualTo("2.10.10.3.25");

        assertThat(latestPatch.upgrade("1.0.x", List.of("1.0.1"))).isPresent();
        assertThat(latestPatch.upgrade("1.0.x", List.of("1.1.0"))).isNotPresent();

        // X-range versions without numeric minor - should not throw ValidationException
        assertThat(latestPatch.upgrade("2.+", List.of("2.1"))).isPresent();
        assertThat(latestPatch.upgrade("2.x", List.of("2.1"))).isPresent();
        assertThat(latestPatch.upgrade("2.+", List.of("3.0"))).isNotPresent();
    }

    @Test
    void compare() {
        assertThat(latestPatch.compare("1.0", "1.0.1", "1.0.2")).isLessThan(0);
        assertThat(latestPatch.compare("1.0", "1.0.0.1", "1.0.1")).isLessThan(0);
        assertThat(latestPatch.compare(null, "1.0", "latest.patch")).isNegative();
        assertThat(latestPatch.compare(null, "latest.patch", "1.0")).isPositive();
        // Test X-range versions without numeric minor - these should not throw ValidationException
        assertThat(latestPatch.compare("2.+", "2.0", "2.1")).isNegative();
        assertThat(latestPatch.compare("2.x", "2.0", "2.1")).isNegative();
    }

    @Test
    void overflowingVersionSegment() {
        // Version numbers that exceed Integer.MAX_VALUE should not throw NumberFormatException
        assertThat(latestPatch.isValid("1.202302104298", "1.202302104298")).isTrue();
        assertThat(latestPatch.isValid("1.202302104298", "1.202302104298.1")).isTrue();
        assertThat(latestPatch.isValid("1.202302104298", "1.202302104299")).isFalse();
    }

    @Test
    void metadataValid() {
        assertThat(latestMetadataPatch.isValid("1.0.0-fred", "1.0.4-fred")).isTrue();
        assertThat(latestMetadataPatch.isValid("1.0-fred", "1.0.1-fred")).isTrue();
        assertThat(latestMetadataPatch.isValid("1.0.0-fred", "1.0.4-not-fred")).isFalse();
    }

    @Test
    void metadataUpgrade() {
        var upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.0-fred"));
        assertThat(upgrade).isEmpty();

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.11.0-fred"));
        assertThat(upgrade).isEmpty();
        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.9-fred"));
        assertThat(upgrade).isEmpty();

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.11-fred"));
        assertThat(upgrade).isPresent();
        assertThat(upgrade.get()).isEqualTo("2.10.11-fred");

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.10.3.23-fred"));
        assertThat(upgrade).isEmpty();

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.10.2.25-fred"));
        assertThat(upgrade).isEmpty();

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.10.3.25-fred"));
        assertThat(upgrade).isPresent();
        assertThat(upgrade.get()).isEqualTo("2.10.10.3.25-fred");
    }

}

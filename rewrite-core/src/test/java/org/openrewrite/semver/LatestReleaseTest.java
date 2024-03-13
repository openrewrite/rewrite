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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.semver.LatestRelease.normalizeVersion;

class LatestReleaseTest {
    private final LatestRelease latestRelease = new LatestRelease(null);

    @Test
    void isValidWhenCurrentIsNull() {
        assertThat(latestRelease.isValid(null, "1.0.0")).isTrue();
    }

    @Test
    void nonNumericPartsValid() {
        assertThat(latestRelease.isValid("1.0", "1.1.1.1")).isTrue();
        assertThat(latestRelease.isValid("1.0", "1.0.RELEASE")).isTrue();
        assertThat(latestRelease.isValid("1.0", "1.0.0.Final")).isTrue();
        assertThat(latestRelease.isValid("1.0", "1.1.1")).isTrue();
        assertThat(latestRelease.isValid("1.0", "1.1")).isTrue();
        assertThat(latestRelease.isValid("1.0", "1")).isTrue();
        assertThat(latestRelease.isValid("1.0", "1.1.1.1.1")).isTrue();

        assertThat(latestRelease.isValid("1.0", "1.1.1.1.1-SNAPSHOT")).isFalse();
        assertThat(latestRelease.isValid("1.0", "1.1.0-SNAPSHOT")).isFalse();
        assertThat(latestRelease.isValid("1.0", "1.1.a")).isFalse();
        assertThat(latestRelease.isValid("1.0", "1.1.1.1.a")).isFalse();
        assertThat(latestRelease.isValid("1.0", "2.0.0.Alpha2")).isFalse();

        assertThat(latestRelease.compare(null, "1.0", "1.1.a")).isLessThan(0);
        assertThat(latestRelease.compare(null, "1.0", "1.1.1.1.a")).isLessThan(0);
    }

    @Test
    void others() {
        assertThat(latestRelease.compare(null, "2.5.6.SEC03", "6.0.4")).isLessThan(0);
    }

    @Test
    void differentMicroVersions() {
        assertThat(latestRelease.compare("1.0", "1.1.1.1", "1.1.1.2")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1", "1.1.1.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1.1.1.1", "2")).isLessThan(0);
    }

    @Test
    void differentPatchVersions() {
        assertThat(latestRelease.compare("1.0", "1.1.1.1", "1.1.2.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1.1.1", "1.1.2")).isLessThan(0);
    }

    @Test
    void differentMinorVersions() {
        assertThat(latestRelease.compare("1.0", "1.1.1.1", "1.2.1.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1.1.1", "1.2.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1.1", "1.2")).isLessThan(0);
    }

    @Test
    void differentMajorVersions() {
        assertThat(latestRelease.compare("1.0", "1.1.1.1", "2.1.1.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1.1.1", "2.1.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1.1", "2.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1", "2")).isLessThan(0);
    }

    @Test
    void differentNumberOfParts() {
        assertThat(latestRelease.compare("1.0", "1.1.1", "1.1.1.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1.1", "1.1.1")).isLessThan(0);
        assertThat(latestRelease.compare("1.0", "1", "1.1")).isLessThan(0);
    }

    @Test
    void preReleases() {
        assertThat(latestRelease.isValid("1.0", "1.1.0-Alpha")).isFalse();
        assertThat(latestRelease.isValid("1.0", "1.1.0-Alpha1")).isFalse();
        assertThat(latestRelease.isValid("1.0", "1.1.0-Alpha.1")).isFalse();
        assertThat(latestRelease.isValid("1.0", "1.1.0-Alpha-1")).isFalse();
        assertThat(latestRelease.isValid("1.0", "1.1.0-Alpha=1")).isFalse();
    }

    @Test
    void guavaVariants() {
        assertThat(latestRelease.compare("1.0", "25.0-jre", "29.0-jre")).isLessThan(0);
    }

    @Test
    @Disabled("https://github.com/openrewrite/rewrite/issues/1204")
        // feel free to move this under "guavaVariants", todo
    void guavaVariantsMetadataBoundaries() {
        assertThat(latestRelease.compare("1.0", "25", "25.0-jre")).isEqualTo(0);
    }

    @Test
    void matchMetadata() {
        assertThat(new LatestRelease("-jre").isValid("1.0", "29.0.0.0-jre")).isTrue();
        assertThat(new LatestRelease("-jre").isValid("1.0", "29.0-jre")).isTrue();
        assertThat(new LatestRelease("-jre").isValid("1.0", "29.0")).isFalse();
        assertThat(new LatestRelease("-jre").isValid("1.0", "29.0-android")).isFalse();
        assertThat(new LatestRelease("").isValid("1.0", "29.0")).isTrue();
        assertThat(new LatestRelease("").isValid("1.0", "29.0-jre")).isFalse();
        assertThat(new LatestRelease(null).isValid("1.0", "29.0-jre")).isFalse();
    }

    @Test
    void normalizeVersionStripReleaseSuffix() {
        assertThat(normalizeVersion("1.5.1.2.RELEASE")).isEqualTo("1.5.1.2");
        assertThat(normalizeVersion("1.5.1.RELEASE")).isEqualTo("1.5.1");
        assertThat(normalizeVersion("1.5.1.FINAL")).isEqualTo("1.5.1");
        assertThat(normalizeVersion("1.5.1.Final")).isEqualTo("1.5.1");
    }

    @Test
    void normalizeVersionToHaveMajorMinorPatch() {
        assertThat(normalizeVersion("29.0")).isEqualTo("29.0.0");
        assertThat(normalizeVersion("29.0-jre")).isEqualTo("29.0.0-jre");
        assertThat(normalizeVersion("29-jre")).isEqualTo("29.0.0-jre");
    }

    @Test
    void datedSnapshotVersions() {
        assertThat(latestRelease.compare(null, "7.17.0-20211102.000501-28",
          "7.17.0-20211102.012229-29")).isLessThan(0);
    }

    @Test
    void matchCustomMetadata() {
        assertThat(new LatestRelease(".Final-custom-\\d+").isValid(null, "3.2.9.Final-custom-00003")).isTrue();
    }
}

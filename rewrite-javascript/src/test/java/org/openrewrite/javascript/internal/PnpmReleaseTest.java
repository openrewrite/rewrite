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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which pnpm releases the recipes write for: each range's first and last release recorded with the same writes
 * (pnpm-writes, the pnpm-releases probes), the release below each range recorded writing otherwise.
 */
class PnpmReleaseTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      9.5.0                | 9
      9.15.9               | 9
      10.32.1              | 10
      10.34.6              | 10
      11.0.6               | 11
      11.2.2               | 11
      11.3.0               | 11
      11.0.5               | 11
      11.16.0              | 11
      >=10.32.1 <=10.34.6  | 10
      11.0.6 - 11.2.2      | 11
      ^11.0.6              | -1
      ~9.15.0              | -1
      ^10.32.1             | -1
      >=9                  | -1
      9.4.0                | 9
      10.31.0              | 10
      10.0.0               | 10
      11.0.2               | 11
      11.28.5              | 11
      12.0.0               | 12
      """)
    void aReleaseIsWrittenForWhenEveryOneItCanBeIsRecorded(String version, int major) {
        PnpmRelease release = PnpmRelease.named(version);
        assertThat(release.getMajor()).isEqualTo(major);
        // pnpm 11's writes are recorded for the releases the engine reproduces (PnpmReleases.PROVEN)
        boolean recorded = !version.matches("9\\.4\\.0|10\\.31\\.0|10\\.0\\.0|11\\.0\\.2|11\\.28\\.5|12\\.0\\.0") &&
                           major > 0;
        assertThat(release.declined() == null).as(version).isEqualTo(recorded);
        assertThat(release.writer() == null).isEqualTo(!recorded || major == 9);
    }

    @Test
    void theRootNamesTheReleaseWhenPnpmSwitchesToIt() {
        assertThat(PnpmRelease.of("{}", null)).hasToString("pnpm 11.2.2");
        assertThat(PnpmRelease.of("{\"packageManager\":\"pnpm@10.34.6\"}", null)).hasToString("pnpm 10.34.6");
        assertThat(PnpmRelease.of("{\"packageManager\":\"pnpm@10.0.0\"}", singletonMap("pmOnFail", "ignore"))
          .declined()).contains("corepack starts pnpm 10.0.0 and pnpm run directly installs with 11.2.2");
        assertThat(PnpmRelease.of("{\"packageManager\":5}", null).declined())
          .startsWith("pnpm cannot read the release the project names");
    }

    @Test
    void eachMajorWritesAsItsRecordingsShow() {
        PnpmRelease pnpm9 = PnpmRelease.named("9.15.9");
        PnpmRelease pnpm10 = PnpmRelease.named("10.34.6");
        PnpmRelease pnpm11 = PnpmRelease.named("11.2.2");
        assertThat(pnpm9.cleansUpUnusedCatalogs()).isFalse();
        assertThat(pnpm10.cleansUpUnusedCatalogs()).isTrue();
        assertThat(pnpm10.cleanupKeepsOverrideReferences()).isFalse();
        assertThat(pnpm11.cleanupKeepsOverrideReferences()).isTrue();
    }
}

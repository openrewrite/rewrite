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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.assertj.core.api.Assertions.assertThat;

class Bun144SemverTest {
    @ParameterizedTest
    @CsvFileSource(resources = "/lock/support/bun/1.4.4/semver.tsv", delimiter = '\t')
    void matchesTheActualRuntime(String version, String range, boolean expected) {
        BunRelease release = BunRelease.of("1.4.4");
        assertThat(SemverQuery.parse(range, release).satisfies(SemverVersion.parse(version, release).version.min()))
          .as("%s satisfies %s", version, range).isEqualTo(expected);
    }
}

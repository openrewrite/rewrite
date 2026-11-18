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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calls of src/util/semver.js from the actual Yarn 1.22.22 bundle, loaded with generate/yarn-bundle.js.
 */
class SemverTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1.2.3-beta.0|^1.0.0|false|true",
            "2.0.0-beta.0|^1.0.0|false|false",
            "2.0.0-beta.0|>=1.0.0|false|true",
            "2.0.0-beta.0|<2.0.0|false|false",
            "2.0.0-beta.0|<2.0.0-rc.0|false|true",
            "0.0.0-beta.0|*|false|true",
            "1.0.0|no range|false|false",
            "v1.2.3|^1.0.0|false|true",
            "01.2.3|^1.0.0|true|true",
            "1.2.3|^6.0.0 OR ^1.0.0|false|true"
    })
    void satisfiesWithPrereleases(String version, String range, boolean loose, boolean expected) {
        assertThat(Semver.satisfiesWithPrereleases(version, range.replace("OR", "||"), loose)).isEqualTo(expected);
    }
}

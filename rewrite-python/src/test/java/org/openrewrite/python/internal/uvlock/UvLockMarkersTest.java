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
package org.openrewrite.python.internal.uvlock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UvLockMarkersTest {

    // rows follow uv 0.13.4's python_version_to_full_version and only_release_trimmed
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "python_version < '4'            | python_full_version < '4'",
      "python_version < '4.0'          | python_full_version < '4'",
      "python_version <= '3.11'        | python_full_version < '3.12'",
      "python_version > '3'            | python_full_version >= '3.1'",
      "python_version == '3.0'         | python_full_version == '3.0.*'",
      "python_full_version < '3.11.0a6' | python_full_version < '3.11'",
      "python_full_version == '3.11.0.*' | python_full_version == '3.11.0.*'",
    })
    void versionClausesRecordAsUvLowersThem(String marker, String recorded) {
        assertThat(UvLockMarkers.recordMarker(marker, null)).isEqualTo(recorded);
    }
}

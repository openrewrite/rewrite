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

import org.junit.jupiter.api.Test;
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
      "python_full_version == '3.11.0.*' | python_full_version >= '3.11.0' and python_full_version < '3.11.1'",
    })
    void versionClausesRecordAsUvLowersThem(String marker, String recorded) {
        assertThat(UvLockMarkers.recordMarker(marker, null)).isEqualTo(recorded);
    }

    @Test
    void clausesRecordWithPythonFirstAndExtrasLast() {
        assertThat(UvLockMarkers.recordMarker(
          "sys_platform == 'linux' and platform_machine == 'x86_64' and python_full_version < '3.13' and os_name == 'posix'",
          "extra == 'cli'"))
          .isEqualTo("python_full_version < '3.13' and os_name == 'posix' and platform_machine == 'x86_64' and sys_platform == 'linux' and extra == 'cli'");
    }

    @Test
    void aMarkerPairingValuesThatNeverHoldTogetherIsKeptAsRecorded() {
        String recorded = "(os_name == 'nt' and platform_machine != 'aarch64' and sys_platform == 'linux') or " +
                          "(os_name == 'nt' and platform_python_implementation != 'CPython' and sys_platform == 'linux') or " +
                          "(os_name == 'nt' and sys_platform != 'darwin' and sys_platform != 'linux')";
        assertThat(UvLockMarkers.recordMarkerAsRecorded(recorded)).isEqualTo(recorded);

        // a marker a declaration states is pruned, since no lock recorded it before uv pruned such a pair
        assertThat(UvLockMarkers.recordMarker("os_name == 'nt' and sys_platform == 'linux'", null))
                .isNotEqualTo("os_name == 'nt' and sys_platform == 'linux'");

        assertThat(UvLockMarkers.recordMarker("python_version >= '3.9' and os_name == 'nt'", null))
                .isEqualTo("python_full_version >= '3.9' and os_name == 'nt'");
    }

    @Test
    void aDisjunctionOfParenthesizedConjunctionsParses() {
        // a fork marker uv writes for the PyPy and emscripten branches of one Python range
        assertThat(UvLockMarkers.parseDisjunction(
          "(python_full_version >= '3.15' and platform_python_implementation == 'PyPy') or " +
          "(python_full_version >= '3.15' and sys_platform == 'emscripten')"))
          .hasSize(2)
          .allSatisfy(term -> assertThat(term).extracting(c -> c[0]).contains("python_full_version"));
    }

    @Test
    void parenthesizedConjunctionInsideATermFlattens() {
        assertThat(UvLockMarkers.parseDisjunction("(python_version >= '3.11' and sys_platform != 'win32') and extra == 'dev'"))
          .singleElement()
          .satisfies(term -> assertThat(term).extracting(c -> c[0])
            .containsExactly("python_version", "sys_platform", "extra"));

        // a disjunction nested inside a conjunction would need distributing, which the parser does not do
        assertThat(UvLockMarkers.parseDisjunction("(sys_platform == 'linux' or sys_platform == 'darwin') and extra == 'dev'"))
          .isNull();
    }
}

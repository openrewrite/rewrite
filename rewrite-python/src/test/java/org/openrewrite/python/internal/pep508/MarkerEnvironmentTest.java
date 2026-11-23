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
package org.openrewrite.python.internal.pep508;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkerEnvironmentTest {

    @Test
    void lockDefaultsFromPythonVersion() {
        MarkerEnvironment env = MarkerEnvironment.lockDefaults("3.11", null);
        assertThat(env.get("python_version")).isEqualTo("3.11");
        assertThat(env.get("python_full_version")).isEqualTo("3.11.0");
        assertThat(env.get("sys_platform")).isEqualTo("linux");
        assertThat(env.get("os_name")).isEqualTo("posix");
        assertThat(env.get("platform_system")).isEqualTo("Linux");
        assertThat(env.get("platform_machine")).isEqualTo("x86_64");
        assertThat(env.get("platform_python_implementation")).isEqualTo("CPython");
        assertThat(env.get("implementation_name")).isEqualTo("cpython");
        assertThat(env.get("platform_release")).isNull();
        assertThat(env.get("platform_version")).isNull();
        assertThat(env.get("extra")).isNull();
    }

    @Test
    void lockDefaultsFromFullVersionOnly() {
        MarkerEnvironment env = MarkerEnvironment.lockDefaults(null, "3.12.4");
        assertThat(env.get("python_version")).isEqualTo("3.12");
        assertThat(env.get("python_full_version")).isEqualTo("3.12.4");
        assertThat(env.get("implementation_version")).isEqualTo("3.12.4");
    }

    @Test
    void lockDefaultsWithBothVersions() {
        MarkerEnvironment env = MarkerEnvironment.lockDefaults("3.10", "3.10.13");
        assertThat(env.get("python_version")).isEqualTo("3.10");
        assertThat(env.get("python_full_version")).isEqualTo("3.10.13");
    }

    @Test
    void lockDefaultsWithUnknownPython() {
        MarkerEnvironment env = MarkerEnvironment.lockDefaults(null, null);
        assertThat(env.get("python_version")).isNull();
        assertThat(env.get("python_full_version")).isNull();
        assertThat(env.get("sys_platform")).isEqualTo("linux");
    }

    @Test
    void unknownVariableNamesReturnNull() {
        MarkerEnvironment env = MarkerEnvironment.lockDefaults("3.11", null);
        assertThat(env.get("extras")).isNull();
        assertThat(env.get("dependency_groups")).isNull();
        assertThat(env.get("not_a_variable")).isNull();
    }
}

/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.python.rpc;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

class PythonRewriteRpcBuilderTest {
    private static final String VERSION = "8.84.9";
    private static final Path PYTHON = Paths.get("python3");
    private static final Path TARGET = Paths.get("pip-packages");

    @Test
    void profilingDisabledByDefault() {
        assertThat(PythonRewriteRpc.builder().profilingEnabled()).isFalse();
    }

    @Test
    void profilingDisabledForBlankAddress() {
        assertThat(PythonRewriteRpc.builder()
                .environment(singletonMap("PYROSCOPE_SERVER_ADDRESS", "  "))
                .profilingEnabled()).isFalse();
    }

    @Test
    void profilingEnabledWhenAddressSet() {
        assertThat(PythonRewriteRpc.builder()
                .environment(singletonMap("PYROSCOPE_SERVER_ADDRESS", "http://localhost:4040"))
                .profilingEnabled()).isTrue();
    }

    @Test
    void plainSpecWhenProfilingDisabled() {
        List<String> specs = PythonRewriteRpc.Builder.openrewriteInstallSpecs(VERSION, false);
        assertThat(specs).containsExactly("openrewrite==" + VERSION);
        assertThat(specs).noneMatch(s -> s.contains("[profiling]"));
    }

    @Test
    void profilingSpecTriedFirstWithPlainFallback() {
        // When profiling is enabled the extra is attempted first, but a plain install is
        // always available as a fallback so an unbuildable pyroscope-io can't block the core install.
        List<String> specs = PythonRewriteRpc.Builder.openrewriteInstallSpecs(VERSION, true);
        assertThat(specs).containsExactly(
                "openrewrite[profiling]==" + VERSION,
                "openrewrite==" + VERSION);
    }

    @Test
    void pipInstallCommandContainsTargetUpgradeAndSpec() {
        List<String> command = PythonRewriteRpc.Builder.pipInstallCommand(PYTHON, TARGET, "openrewrite==" + VERSION);
        assertThat(command)
                .containsSubsequence("-m", "pip", "install", "--upgrade")
                .contains("openrewrite==" + VERSION)
                .doesNotContain("openrewrite[profiling]==" + VERSION);
        assertThat(command).anyMatch(arg -> arg.startsWith("--target="));
    }
}

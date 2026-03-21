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
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.UvExecutor;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SetupCfgParserTest {

    @Test
    void acceptsSetupCfg() {
        SetupCfgParser parser = new SetupCfgParser();
        assertThat(parser.accept(Paths.get("setup.cfg"))).isTrue();
        assertThat(parser.accept(Paths.get("pyproject.toml"))).isFalse();
        assertThat(parser.accept(Paths.get("requirements.txt"))).isFalse();
        assertThat(parser.accept(Paths.get("other.cfg"))).isFalse();
    }

    @Test
    void builderCreatesDslName() {
        SetupCfgParser.Builder builder = SetupCfgParser.builder();
        assertThat(builder.getDslName()).isEqualTo("setup.cfg");
        assertThat(builder.build()).isInstanceOf(SetupCfgParser.class);
    }

    @Test
    void markerContainsDependenciesFromFreeze(@TempDir Path tempDir) throws IOException {
        assumeTrue(UvExecutor.findUvExecutable() != null, "uv is not installed");

        Files.writeString(tempDir.resolve("setup.cfg"), """
                [metadata]
                name = myapp
                version = 1.0.0

                [options]
                install_requires =
                    requests>=2.28.0
                """);
        // setup.py is required for uv pip install to recognize the project
        Files.writeString(tempDir.resolve("setup.py"), "from setuptools import setup; setup()");

        SetupCfgParser parser = new SetupCfgParser();
        Parser.Input input = Parser.Input.fromFile(tempDir.resolve("setup.cfg"));
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                tempDir,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers()
                .findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();

        assertThat(marker.getResolvedDependencies()).isNotEmpty();
        assertThat(marker.getResolvedDependencies().stream().map(ResolvedDependency::getName))
                .contains("requests");
        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Uv);
    }
}

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
import org.openrewrite.python.internal.PackageManagerExecutor;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
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
    void markerContainsDependenciesWithoutWritingIntoProject(@TempDir Path tempDir) throws Exception {
        assumeTrue(PackageManagerExecutor.UV.find() != null, "uv is not installed");

        // Unique content, so a workspace cached by an earlier run cannot skip the resolution.
        Files.writeString(tempDir.resolve("setup.cfg"), """
                # %s
                [metadata]
                name = myapp
                version = 1.0.0

                [options]
                package_dir =
                    = src
                packages = myapp
                install_requires =
                    requests>=2.28.0
                """.formatted(UUID.randomUUID()));
        // setup.py is required for uv pip install to recognize the project
        Files.writeString(tempDir.resolve("setup.py"), "from setuptools import setup; setup()");
        Files.createDirectories(tempDir.resolve("src/myapp"));
        Files.writeString(tempDir.resolve("src/myapp/__init__.py"), "");
        List<Path> projectFiles = listFiles(tempDir);

        SetupCfgParser parser = new SetupCfgParser();
        Parser.Input input = Parser.Input.fromFile(tempDir.resolve("setup.cfg"));
        List<SourceFile> parsed = parser.parseInputs(
                singletonList(input),
                tempDir,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(toList());

        assertThat(parsed).hasSize(1);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers()
                .findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();

        assertThat(marker.getResolvedDependencies()).isNotEmpty();
        assertThat(marker.getResolvedDependencies().stream().map(ResolvedDependency::getName))
                .contains("requests");
        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Uv);
        assertThat(listFiles(tempDir)).isEqualTo(projectFiles);
    }

    private static List<Path> listFiles(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.map(dir::relativize).sorted().collect(toList());
        }
    }
}

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
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PyProjectTomlParserTest {

    @Test
    void acceptsPyprojectToml() {
        PyProjectTomlParser parser = new PyProjectTomlParser();
        assertThat(parser.accept(Paths.get("pyproject.toml"))).isTrue();
        assertThat(parser.accept(Paths.get("src/pyproject.toml"))).isTrue();
        assertThat(parser.accept(Paths.get("Cargo.toml"))).isFalse();
        assertThat(parser.accept(Paths.get("file.toml"))).isFalse();
    }

    @Test
    void parsesPyprojectTomlWithMarker() {
        String pyprojectToml = """
                [project]
                name = "my-project"
                version = "1.0.0"
                description = "A test project"
                requires-python = ">=3.10"
                dependencies = [
                    "requests>=2.28.0",
                    "click>=8.0",
                ]

                [build-system]
                requires = ["hatchling"]
                build-backend = "hatchling.build"
                """;

        PyProjectTomlParser parser = new PyProjectTomlParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("pyproject.toml"),
                pyprojectToml
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(Toml.Document.class);

        Toml.Document doc = (Toml.Document) parsed.get(0);
        PythonResolutionResult marker = doc.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getName()).isEqualTo("my-project");
        assertThat(marker.getVersion()).isEqualTo("1.0.0");
        assertThat(marker.getDescription()).isEqualTo("A test project");
        assertThat(marker.getRequiresPython()).isEqualTo(">=3.10");
        assertThat(marker.getBuildBackend()).isEqualTo("hatchling.build");

        assertThat(marker.getDependencies()).hasSize(2);
        assertThat(marker.getDependencies().get(0).getName()).isEqualTo("requests");
        assertThat(marker.getDependencies().get(0).getVersionConstraint()).isEqualTo(">=2.28.0");
        assertThat(marker.getDependencies().get(1).getName()).isEqualTo("click");

        assertThat(marker.getBuildRequires()).hasSize(1);
        assertThat(marker.getBuildRequires().get(0).getName()).isEqualTo("hatchling");
    }

    @Test
    void parsesWithResolvedDependencies(@TempDir Path tempDir) throws IOException {
        String pyprojectToml = """
                [project]
                name = "my-project"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]

                [build-system]
                requires = ["hatchling"]
                build-backend = "hatchling.build"
                """;

        String uvLock = """
                version = 1
                requires-python = ">=3.10"

                [[package]]
                name = "requests"
                version = "2.31.0"
                source = { registry = "https://pypi.org/simple" }
                dependencies = [
                    { name = "certifi", specifier = ">=2017.4.17" },
                ]

                [[package]]
                name = "certifi"
                version = "2024.2.2"
                source = { registry = "https://pypi.org/simple" }
                """;

        // Write files to temp directory
        Files.write(tempDir.resolve("pyproject.toml"), pyprojectToml.getBytes());
        Files.write(tempDir.resolve("uv.lock"), uvLock.getBytes());

        PyProjectTomlParser parser = new PyProjectTomlParser();
        Parser.Input input = Parser.Input.fromFile(tempDir.resolve("pyproject.toml"));
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                tempDir,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        Toml.Document doc = (Toml.Document) parsed.get(0);
        PythonResolutionResult marker = doc.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();

        // Check resolved dependencies
        assertThat(marker.getResolvedDependencies()).hasSize(2);
        assertThat(marker.getResolvedDependency("requests")).isNotNull();
        assertThat(marker.getResolvedDependency("requests").getVersion()).isEqualTo("2.31.0");
        assertThat(marker.getResolvedDependency("certifi")).isNotNull();
        assertThat(marker.getResolvedDependency("certifi").getVersion()).isEqualTo("2024.2.2");

        // Check package manager is detected from uv.lock
        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Uv);

        // Check declared dependency is linked to resolved
        Dependency requestsDep = marker.getDependencies().get(0);
        assertThat(requestsDep.getName()).isEqualTo("requests");
        assertThat(requestsDep.getResolved()).isNotNull();
        assertThat(requestsDep.getResolved().getVersion()).isEqualTo("2.31.0");
    }

    @Test
    void parsesWithoutLockFile() {
        String pyprojectToml = """
                [project]
                name = "no-lock"
                version = "0.1.0"
                dependencies = ["requests>=2.28.0"]
                """;

        PyProjectTomlParser parser = new PyProjectTomlParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("pyproject.toml"),
                pyprojectToml
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        Toml.Document doc = (Toml.Document) parsed.get(0);
        PythonResolutionResult marker = doc.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getName()).isEqualTo("no-lock");
        assertThat(marker.getPackageManager()).isNull();
        assertThat(marker.getResolvedDependencies()).isEmpty();
    }

    @Test
    void parsesLicenseAndDependencyGroups() {
        String pyprojectToml = """
                [project]
                name = "licensed-project"
                version = "1.0.0"
                license = "Apache-2.0"
                dependencies = ["requests>=2.28.0"]

                [dependency-groups]
                dev = ["pytest>=7.0", "mypy>=1.0"]
                test = ["coverage>=7.0"]
                """;

        PyProjectTomlParser parser = new PyProjectTomlParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("pyproject.toml"),
                pyprojectToml
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        Toml.Document doc = (Toml.Document) parsed.get(0);
        PythonResolutionResult marker = doc.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getLicense()).isEqualTo("Apache-2.0");
        assertThat(marker.getDependencyGroups()).hasSize(2);
        assertThat(marker.getDependencyGroups()).containsKeys("dev", "test");
        assertThat(marker.getDependencyGroups().get("dev")).hasSize(2);
    }

    @Test
    void builderCreatesDslName() {
        PyProjectTomlParser.Builder builder = PyProjectTomlParser.builder();
        assertThat(builder.getDslName()).isEqualTo("pyproject.toml");
        assertThat(builder.build()).isInstanceOf(PyProjectTomlParser.class);
    }
}

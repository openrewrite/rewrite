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
package org.openrewrite.python.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PythonDependencyParserTest {

    @Test
    void parsePep508SimpleName() {
        Dependency dep = PythonDependencyParser.parsePep508("requests");
        assertThat(dep).isNotNull();
        assertThat(dep.getName()).isEqualTo("requests");
        assertThat(dep.getVersionConstraint()).isNull();
        assertThat(dep.getExtras()).isNull();
        assertThat(dep.getMarker()).isNull();
    }

    @Test
    void parsePep508WithVersion() {
        Dependency dep = PythonDependencyParser.parsePep508("requests>=2.28.0");
        assertThat(dep).isNotNull();
        assertThat(dep.getName()).isEqualTo("requests");
        assertThat(dep.getVersionConstraint()).isEqualTo(">=2.28.0");
        assertThat(dep.getExtras()).isNull();
        assertThat(dep.getMarker()).isNull();
    }

    @Test
    void parsePep508WithExtras() {
        Dependency dep = PythonDependencyParser.parsePep508("requests[security]>=2.28.0");
        assertThat(dep).isNotNull();
        assertThat(dep.getName()).isEqualTo("requests");
        assertThat(dep.getVersionConstraint()).isEqualTo(">=2.28.0");
        assertThat(dep.getExtras()).containsExactly("security");
        assertThat(dep.getMarker()).isNull();
    }

    @Test
    void parsePep508WithMultipleExtras() {
        Dependency dep = PythonDependencyParser.parsePep508("requests[security,socks]>=2.28.0");
        assertThat(dep).isNotNull();
        assertThat(dep.getName()).isEqualTo("requests");
        assertThat(dep.getExtras()).containsExactly("security", "socks");
    }

    @Test
    void parsePep508WithMarker() {
        Dependency dep = PythonDependencyParser.parsePep508("requests>=2.28.0; python_version>='3.8'");
        assertThat(dep).isNotNull();
        assertThat(dep.getName()).isEqualTo("requests");
        assertThat(dep.getVersionConstraint()).isEqualTo(">=2.28.0");
        assertThat(dep.getMarker()).isEqualTo("python_version>='3.8'");
    }

    @Test
    void parsePep508Full() {
        Dependency dep = PythonDependencyParser.parsePep508(
                "requests[security,socks]>=2.28.0,<3.0; python_version>='3.8'");
        assertThat(dep).isNotNull();
        assertThat(dep.getName()).isEqualTo("requests");
        assertThat(dep.getVersionConstraint()).isEqualTo(">=2.28.0,<3.0");
        assertThat(dep.getExtras()).containsExactly("security", "socks");
        assertThat(dep.getMarker()).isEqualTo("python_version>='3.8'");
    }

    @Test
    void parsePep508WithDashes() {
        Dependency dep = PythonDependencyParser.parsePep508("my-cool-package>=1.0");
        assertThat(dep).isNotNull();
        assertThat(dep.getName()).isEqualTo("my-cool-package");
        assertThat(dep.getVersionConstraint()).isEqualTo(">=1.0");
    }

    @Test
    void createMarkerFromPyprojectToml() {
        String toml = "" +
                "[project]\n" +
                "name = \"my-project\"\n" +
                "version = \"1.0.0\"\n" +
                "description = \"A test project\"\n" +
                "requires-python = \">=3.10\"\n" +
                "dependencies = [\n" +
                "    \"requests>=2.28.0\",\n" +
                "    \"click>=8.0\",\n" +
                "]\n" +
                "\n" +
                "[build-system]\n" +
                "requires = [\"hatchling\"]\n" +
                "build-backend = \"hatchling.build\"\n";

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getName()).isEqualTo("my-project");
        assertThat(marker.getVersion()).isEqualTo("1.0.0");
        assertThat(marker.getDescription()).isEqualTo("A test project");
        assertThat(marker.getRequiresPython()).isEqualTo(">=3.10");
        assertThat(marker.getBuildBackend()).isEqualTo("hatchling.build");
        assertThat(marker.getBuildRequires()).hasSize(1);
        assertThat(marker.getBuildRequires().get(0).getName()).isEqualTo("hatchling");
        assertThat(marker.getDependencies()).hasSize(2);
        assertThat(marker.getDependencies().get(0).getName()).isEqualTo("requests");
        assertThat(marker.getDependencies().get(0).getVersionConstraint()).isEqualTo(">=2.28.0");
        assertThat(marker.getDependencies().get(1).getName()).isEqualTo("click");
    }

    @Test
    void createMarkerWithOptionalDependencies() {
        String toml = "" +
                "[project]\n" +
                "name = \"my-project\"\n" +
                "version = \"1.0.0\"\n" +
                "dependencies = [\"requests>=2.28.0\"]\n" +
                "\n" +
                "[project.optional-dependencies]\n" +
                "dev = [\"pytest>=7.0\", \"black>=22.0\"]\n" +
                "docs = [\"sphinx>=5.0\"]\n";

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getOptionalDependencies()).hasSize(2);
        assertThat(marker.getOptionalDependencies()).containsKeys("dev", "docs");

        List<Dependency> devDeps = marker.getOptionalDependencies().get("dev");
        assertThat(devDeps).hasSize(2);
        assertThat(devDeps.get(0).getName()).isEqualTo("pytest");
        assertThat(devDeps.get(1).getName()).isEqualTo("black");

        List<Dependency> docsDeps = marker.getOptionalDependencies().get("docs");
        assertThat(docsDeps).hasSize(1);
        assertThat(docsDeps.get(0).getName()).isEqualTo("sphinx");
    }

    @Test
    void createMarkerMinimal() {
        String toml = "" +
                "[project]\n" +
                "name = \"minimal\"\n" +
                "version = \"0.1.0\"\n";

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getName()).isEqualTo("minimal");
        assertThat(marker.getDependencies()).isEmpty();
        assertThat(marker.getOptionalDependencies()).isEmpty();
        assertThat(marker.getBuildRequires()).isEmpty();
    }

    private static Toml.Document parseToml(String content) {
        TomlParser parser = new TomlParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("pyproject.toml"),
                content
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(Toml.Document.class);
        return (Toml.Document) parsed.get(0);
    }
}

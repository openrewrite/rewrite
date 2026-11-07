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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
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
        String toml = """
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
        String toml = """
          [project]
          name = "my-project"
          version = "1.0.0"
          dependencies = ["requests>=2.28.0"]
          
          [project.optional-dependencies]
          dev = ["pytest>=7.0", "black>=22.0"]
          docs = ["sphinx>=5.0"]
          """;

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
        String toml = """
          [project]
          name = "minimal"
          version = "0.1.0"
          """;

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getName()).isEqualTo("minimal");
        assertThat(marker.getDependencies()).isEmpty();
        assertThat(marker.getOptionalDependencies()).isEmpty();
        assertThat(marker.getDependencyGroups()).isEmpty();
        assertThat(marker.getBuildRequires()).isEmpty();
        assertThat(marker.getLicense()).isNull();
    }

    @Test
    void createMarkerWithSpdxLicense() {
        String toml = """
          [project]
          name = "my-project"
          version = "1.0.0"
          license = "MIT"
          """;

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getLicense()).isEqualTo("MIT");
    }

    @Test
    void createMarkerWithDeprecatedLicenseTable() {
        String toml = """
          [project]
          name = "my-project"
          version = "1.0.0"
          license = {text = "Apache-2.0"}
          """;

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getLicense()).isEqualTo("Apache-2.0");
    }

    @Test
    void createMarkerWithDependencyGroups() {
        String toml = """
          [project]
          name = "my-project"
          version = "1.0.0"
          
          [dependency-groups]
          dev = ["pytest>=7.0", "mypy>=1.0"]
          test = ["coverage>=7.0"]
          """;

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getDependencyGroups()).hasSize(2);
        assertThat(marker.getDependencyGroups()).containsKeys("dev", "test");

        List<Dependency> devGroup = marker.getDependencyGroups().get("dev");
        assertThat(devGroup).hasSize(2);
        assertThat(devGroup.get(0).getName()).isEqualTo("pytest");
        assertThat(devGroup.get(1).getName()).isEqualTo("mypy");

        List<Dependency> testGroup = marker.getDependencyGroups().get("test");
        assertThat(testGroup).hasSize(1);
        assertThat(testGroup.get(0).getName()).isEqualTo("coverage");
    }

    @Test
    void detectsPoetryPackageManager() {
        String toml = """
          [project]
          name = "poetry-project"
          version = "1.0.0"

          [tool.poetry]
          name = "poetry-project"
          """;

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Poetry);
    }

    @Test
    void detectsPoetryFromSubTable() {
        String toml = """
          [project]
          name = "poetry-project"
          version = "1.0.0"

          [tool.poetry.dependencies]
          python = "^3.10"
          """;

        Toml.Document doc = parseToml(toml);
        PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, null);

        assertThat(marker).isNotNull();
        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Poetry);
    }

    @Test
    void poetryPythonConstraintIsRequiresPythonWithoutProjectTable() {
        // PacktPublishing/LLM-Engineers-Handbook
        String toml = """
          [tool.poetry]
          name = "llm-engineering"
          version = "0.1.0"
          description = ""
          authors = ["iusztinpaul <p.b.iusztin@gmail.com>"]
          license = "MIT"
          readme = "README.md"

          [tool.poetry.dependencies]
          python = "~3.11"
          zenml = { version = "0.74.0", extras = ["server"] }
          pymongo = "^4.6.2"

          [build-system]
          requires = ["poetry-core"]
          build-backend = "poetry.core.masonry.api"
          """;

        PythonResolutionResult marker = PythonDependencyParser.createMarker(parseToml(toml), null);

        assertThat(marker).isNotNull();
        assertThat(marker.getName()).isEqualTo("llm-engineering");
        assertThat(marker.getVersion()).isEqualTo("0.1.0");
        assertThat(marker.getLicense()).isEqualTo("MIT");
        assertThat(marker.getRequiresPython()).isEqualTo(">=3.11,<3.12");
        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Poetry);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", nullValues = "null", value = {
      // confident-ai/deepeval
      ">=3.9, <4.0 -> >=3.9, <4.0",
      // trypromptly/LLMStack
      ">=3.10,<4 -> >=3.10,<4",
      // xusenlinzy/api-for-open-llm libs/langchain_llm
      ">=3.8.1,<4.0 -> >=3.8.1,<4.0",
      "^3.10 -> >=3.10,<4.0",
      "^3.10.2 -> >=3.10.2,<4.0.0",
      "^3 -> >=3,<4",
      "~3.10 -> >=3.10,<3.11",
      "~3.10.2 -> >=3.10.2,<3.11.0",
      "~=3.10 -> ~=3.10",
      "3.11 -> ==3.11",
      "3.11.* -> ==3.11.*",
      ">=3.9 <3.13 -> >=3.9,<3.13",
      ">= 3.9, ^3.10 -> >=3.9,>=3.10,<4.0",
      "* -> null",
      "^2.7 || ^3.6 -> null",
    })
    void poetryPythonConstraintTranslatesToPep440(String poetry, @Nullable String pep440) {
        String toml = """
          [tool.poetry]
          name = "demo"

          [tool.poetry.dependencies]
          python = "%s"
          """.formatted(poetry);

        PythonResolutionResult marker = PythonDependencyParser.createMarker(parseToml(toml), null);

        assertThat(marker).isNotNull();
        assertThat(marker.getRequiresPython()).isEqualTo(pep440);
        if (pep440 != null) {
            assertThat(PythonVersionSpecifierSet.parse(pep440)).isNotNull();
        }
    }

    @Test
    void projectRequiresPythonWinsOverPoetryPython() {
        String toml = """
          [project]
          name = "demo"
          requires-python = ">=3.12"

          [tool.poetry.dependencies]
          python = "^3.10"
          """;

        PythonResolutionResult marker = PythonDependencyParser.createMarker(parseToml(toml), null);

        assertThat(marker).isNotNull();
        assertThat(marker.getRequiresPython()).isEqualTo(">=3.12");
    }

    @Test
    void poetryPythonFillsInForProjectTableWithoutRequiresPython() {
        String toml = """
          [project]
          name = "demo"
          dependencies = ["requests>=2.28"]

          [tool.poetry.dependencies]
          python = "^3.10"
          """;

        PythonResolutionResult marker = PythonDependencyParser.createMarker(parseToml(toml), null);

        assertThat(marker).isNotNull();
        assertThat(marker.getName()).isEqualTo("demo");
        assertThat(marker.getRequiresPython()).isEqualTo(">=3.10,<4.0");
    }

    @Test
    void toolSettingsAloneGetNoMarker() {
        // huggingface/datasets declares its Python version in setup.py, not here
        String toml = """
          [tool.ruff]
          line-length = 119

          [tool.pytest.ini_options]
          filterwarnings = ["ignore::DeprecationWarning"]
          """;

        assertThat(PythonDependencyParser.createMarker(parseToml(toml), null)).isNull();
    }

    private static Toml.Document parseToml(String content) {
        TomlParser parser = new TomlParser();
        Parser.Input input = Parser.Input.fromString(
          Paths.get("pyproject.toml"),
          content
        );
        List<SourceFile> parsed = parser.parseInputs(
          singletonList(input),
          null,
          new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(toList());

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(Toml.Document.class);
        return (Toml.Document) parsed.get(0);
    }
}

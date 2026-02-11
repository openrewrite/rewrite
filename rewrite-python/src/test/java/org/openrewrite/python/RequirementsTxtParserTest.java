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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.UvExecutor;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RequirementsTxtParserTest {

    @Test
    void acceptsRequirementsTxt() {
        RequirementsTxtParser parser = new RequirementsTxtParser();
        assertThat(parser.accept(Paths.get("requirements.txt"))).isTrue();
        assertThat(parser.accept(Paths.get("requirements-dev.txt"))).isTrue();
        assertThat(parser.accept(Paths.get("requirements.in"))).isTrue();
        assertThat(parser.accept(Paths.get("pyproject.toml"))).isFalse();
        assertThat(parser.accept(Paths.get("file.txt"))).isFalse();
    }

    @Test
    void parsesSimpleDependencies() {
        String requirements = """
                requests>=2.28.0
                click>=8.0
                flask==2.3.0
                """;

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(PlainText.class);

        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();

        List<Dependency> deps = marker.getDependencies();
        assertThat(deps).hasSize(3);
        assertThat(deps.get(0).getName()).isEqualTo("requests");
        assertThat(deps.get(0).getVersionConstraint()).isEqualTo(">=2.28.0");
        assertThat(deps.get(1).getName()).isEqualTo("click");
        assertThat(deps.get(1).getVersionConstraint()).isEqualTo(">=8.0");
        assertThat(deps.get(2).getName()).isEqualTo("flask");
        assertThat(deps.get(2).getVersionConstraint()).isEqualTo("==2.3.0");
    }

    @Test
    void parsesWithExtrasAndMarkers() {
        String requirements = """
                requests[security]>=2.28.0; python_version>='3.8'
                """;

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();

        List<Dependency> deps = marker.getDependencies();
        assertThat(deps).hasSize(1);
        assertThat(deps.get(0).getName()).isEqualTo("requests");
        assertThat(deps.get(0).getExtras()).containsExactly("security");
        assertThat(deps.get(0).getVersionConstraint()).isEqualTo(">=2.28.0");
        assertThat(deps.get(0).getMarker()).isEqualTo("python_version>='3.8'");
    }

    @Test
    void skipsCommentsAndBlankLines() {
        String requirements = """
                # This is a comment
                requests>=2.28.0

                # Another comment
                click>=8.0
                """;

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getDependencies()).hasSize(2);
    }

    @Test
    void skipsOptionLines() {
        String requirements = """
                -i https://pypi.org/simple
                --index-url https://pypi.org/simple
                -r other-requirements.txt
                --extra-index-url https://extra.pypi.org/simple
                -e .
                -f https://download.pytorch.org/whl/torch_stable.html
                --find-links https://example.com
                --no-binary :all:
                --only-binary :none:
                --pre
                --trusted-host example.com
                --no-deps
                -c constraints.txt
                --constraint constraints.txt
                requests>=2.28.0
                """;

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getDependencies()).hasSize(1);
        assertThat(marker.getDependencies().get(0).getName()).isEqualTo("requests");
    }

    @Test
    void parsesEmptyFile() {
        String requirements = "";

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(PlainText.class);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNull();
    }

    @Test
    void builderCreatesDslName() {
        RequirementsTxtParser.Builder builder = RequirementsTxtParser.builder();
        assertThat(builder.getDslName()).isEqualTo("requirements.txt");
        assertThat(builder.build()).isInstanceOf(RequirementsTxtParser.class);
    }

    @Test
    void handlesLineContinuations() {
        String requirements = """
                requests\\
                >=2.28.0
                click>=8.0
                """;

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getDependencies()).hasSize(2);
        assertThat(marker.getDependencies().get(0).getName()).isEqualTo("requests");
        assertThat(marker.getDependencies().get(0).getVersionConstraint()).isEqualTo(">=2.28.0");
    }

    @Test
    void parsesFreezeOutputIncludingTransitives() {
        String freezeContent = """
                certifi==2024.2.2
                charset-normalizer==3.3.2
                idna==3.6
                requests==2.31.0
                urllib3==2.2.1
                """;

        List<ResolvedDependency> resolved = RequirementsTxtParser.parseFreezeLines(freezeContent);
        assertThat(resolved).hasSize(5);
        assertThat(resolved.get(0).getName()).isEqualTo("certifi");
        assertThat(resolved.get(0).getVersion()).isEqualTo("2024.2.2");
        assertThat(resolved.get(3).getName()).isEqualTo("requests");
        assertThat(resolved.get(3).getVersion()).isEqualTo("2.31.0");

        // pip freeze is flat — no transitive dependency graph info
        for (ResolvedDependency dep : resolved) {
            assertThat(dep.getDependencies()).isNull();
        }
    }

    @Test
    void resolvedDependenciesLinkedToDeclaredDeps() {
        // Declared: only "requests", but freeze includes its transitives
        List<Dependency> declared = RequirementsTxtParser.parseDependencies("requests>=2.28.0\n");
        assertThat(declared).hasSize(1);

        String freezeContent = """
                certifi==2024.2.2
                charset-normalizer==3.3.2
                idna==3.6
                requests==2.31.0
                urllib3==2.2.1
                """;
        List<ResolvedDependency> resolved = RequirementsTxtParser.parseFreezeLines(freezeContent);
        assertThat(resolved).hasSize(5);

        // The full resolved list includes transitives (certifi, charset-normalizer, idna, urllib3)
        // even though only "requests" was declared
        assertThat(resolved.stream().map(ResolvedDependency::getName))
                .containsExactly("certifi", "charset-normalizer", "idna", "requests", "urllib3");
    }

    @Test
    void markerContainsTransitiveDependencies() {
        assumeTrue(UvExecutor.findUvExecutable() != null, "uv is not installed");

        String requirements = "requests>=2.28.0\n";

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();

        // Declared: only "requests"
        assertThat(marker.getDependencies()).hasSize(1);
        assertThat(marker.getDependencies().get(0).getName()).isEqualTo("requests");

        // Resolved should include requests AND its transitives (certifi, urllib3, etc.)
        assertThat(marker.getResolvedDependencies()).hasSizeGreaterThan(1);
        assertThat(marker.getResolvedDependencies().stream().map(ResolvedDependency::getName))
                .contains("requests", "certifi", "urllib3");

        // The declared "requests" dep should be linked to its resolved version
        Dependency requestsDep = marker.getDependencies().get(0);
        assertThat(requestsDep.getResolved()).isNotNull();
        assertThat(requestsDep.getResolved().getName()).isEqualTo("requests");
        assertThat(requestsDep.getResolved().getVersion()).isNotEmpty();
    }

    @Test
    void setsPathAndPackageManager() {
        String requirements = """
                requests>=2.28.0
                """;

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        PlainText text = (PlainText) parsed.get(0);
        PythonResolutionResult marker = text.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getPath()).isEqualTo("requirements.txt");
        assertThat(marker.getPackageManager()).isNotNull();
        assertThat(marker.getPackageManager()).isIn(
                PythonResolutionResult.PackageManager.Pip,
                PythonResolutionResult.PackageManager.Uv
        );
    }
}

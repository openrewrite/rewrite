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
    void dependenciesFromResolvedCreatesLinkedEntries() {
        String freezeContent = """
                certifi==2024.2.2
                requests==2.31.0
                """;

        List<ResolvedDependency> resolved = RequirementsTxtParser.parseFreezeLines(freezeContent);
        List<Dependency> deps = RequirementsTxtParser.dependenciesFromResolved(resolved);

        assertThat(deps).hasSize(2);
        assertThat(deps.get(0).getName()).isEqualTo("certifi");
        assertThat(deps.get(0).getVersionConstraint()).isEqualTo("==2024.2.2");
        assertThat(deps.get(0).getResolved()).isSameAs(resolved.get(0));
        assertThat(deps.get(1).getName()).isEqualTo("requests");
        assertThat(deps.get(1).getResolved()).isSameAs(resolved.get(1));
    }

    @Test
    void markerContainsDependenciesFromFreeze() {
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

        // Dependencies come from freeze — includes requests AND its transitives
        assertThat(marker.getDependencies()).hasSizeGreaterThan(1);
        assertThat(marker.getDependencies().stream().map(Dependency::getName))
                .contains("requests", "certifi", "urllib3");

        // Each dependency is linked to its resolved version
        for (Dependency dep : marker.getDependencies()) {
            assertThat(dep.getResolved()).isNotNull();
            assertThat(dep.getResolved().getName()).isEqualTo(dep.getName());
        }

        // Resolved list matches dependencies
        assertThat(marker.getResolvedDependencies()).hasSizeGreaterThan(1);
        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Uv);
    }

    @Test
    void builderCreatesDslName() {
        RequirementsTxtParser.Builder builder = RequirementsTxtParser.builder();
        assertThat(builder.getDslName()).isEqualTo("requirements.txt");
        assertThat(builder.build()).isInstanceOf(RequirementsTxtParser.class);
    }
}

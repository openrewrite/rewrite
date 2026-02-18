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
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void dependenciesFromResolvedWithoutGraphTreatsAllAsDirect() {
        String freezeContent = """
                certifi==2024.2.2
                requests==2.31.0
                """;

        List<ResolvedDependency> resolved = RequirementsTxtParser.parseFreezeLines(freezeContent);
        // Without a linked graph (dependencies are null), all are treated as direct
        List<Dependency> deps = RequirementsTxtParser.dependenciesFromResolved(resolved);

        assertThat(deps).hasSize(2);
        assertThat(deps.get(0).getName()).isEqualTo("certifi");
        assertThat(deps.get(0).getVersionConstraint()).isEqualTo("==2024.2.2");
        assertThat(deps.get(0).getResolved()).isSameAs(resolved.get(0));
        assertThat(deps.get(1).getName()).isEqualTo("requests");
        assertThat(deps.get(1).getResolved()).isSameAs(resolved.get(1));
    }

    @Test
    void dependenciesFromResolvedExcludesTransitives() {
        // Simulate a linked graph: requests depends on certifi
        ResolvedDependency certifi = new ResolvedDependency("certifi", "2024.2.2", null, null);
        ResolvedDependency requests = new ResolvedDependency("requests", "2.31.0", null, List.of(certifi));

        List<ResolvedDependency> resolved = List.of(certifi, requests);
        List<Dependency> deps = RequirementsTxtParser.dependenciesFromResolved(resolved);

        // Only requests should be a direct dependency (certifi has incoming edge from requests)
        assertThat(deps).hasSize(1);
        assertThat(deps.get(0).getName()).isEqualTo("requests");
        assertThat(deps.get(0).getResolved()).isSameAs(requests);
    }

    @Test
    void linkDependenciesFromMetadataBuildsGraph(@TempDir Path tempDir) throws IOException {
        // Create a fake site-packages with METADATA files
        Path sitePackages = tempDir.resolve(".venv/lib/python3.12/site-packages");

        Path requestsDist = sitePackages.resolve("requests-2.31.0.dist-info");
        Files.createDirectories(requestsDist);
        Files.write(requestsDist.resolve("METADATA"), """
                Metadata-Version: 2.4
                Name: requests
                Version: 2.31.0
                Requires-Dist: certifi>=2017.4.17
                Requires-Dist: charset_normalizer<4,>=2
                Requires-Dist: PySocks!=1.5.7,>=1.5.6; extra == "socks"
                """.getBytes(StandardCharsets.UTF_8));

        Path certifiDist = sitePackages.resolve("certifi-2024.2.2.dist-info");
        Files.createDirectories(certifiDist);
        Files.write(certifiDist.resolve("METADATA"), """
                Metadata-Version: 2.4
                Name: certifi
                Version: 2024.2.2
                """.getBytes(StandardCharsets.UTF_8));

        Path charsetDist = sitePackages.resolve("charset_normalizer-3.3.2.dist-info");
        Files.createDirectories(charsetDist);
        Files.write(charsetDist.resolve("METADATA"), """
                Metadata-Version: 2.4
                Name: charset-normalizer
                Version: 3.3.2
                """.getBytes(StandardCharsets.UTF_8));

        List<ResolvedDependency> resolved = RequirementsTxtParser.parseFreezeLines("""
                certifi==2024.2.2
                charset-normalizer==3.3.2
                requests==2.31.0
                """);

        List<ResolvedDependency> linked = RequirementsTxtParser.linkDependenciesFromMetadata(resolved, tempDir);

        assertThat(linked).hasSize(3);

        // requests should have certifi and charset_normalizer as dependencies
        ResolvedDependency requests = linked.stream()
                .filter(d -> d.getName().equals("requests")).findFirst().orElseThrow();
        assertThat(requests.getDependencies()).hasSize(2);
        assertThat(requests.getDependencies().stream().map(ResolvedDependency::getName))
                .containsExactlyInAnyOrder("certifi", "charset-normalizer");

        // certifi should have no dependencies
        ResolvedDependency certifi = linked.stream()
                .filter(d -> d.getName().equals("certifi")).findFirst().orElseThrow();
        assertThat(certifi.getDependencies()).isNull();

        // charset-normalizer should have no dependencies
        ResolvedDependency charset = linked.stream()
                .filter(d -> d.getName().equals("charset-normalizer")).findFirst().orElseThrow();
        assertThat(charset.getDependencies()).isNull();
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

        // Only direct (root) dependencies should be in getDependencies()
        // requests is the only root — certifi, urllib3 etc. are transitives
        assertThat(marker.getDependencies()).hasSize(1);
        assertThat(marker.getDependencies().get(0).getName()).isEqualTo("requests");
        assertThat(marker.getDependencies().get(0).getResolved()).isNotNull();

        // The resolved graph should include all packages with linked dependencies
        assertThat(marker.getResolvedDependencies()).hasSizeGreaterThan(1);
        assertThat(marker.getResolvedDependencies().stream().map(ResolvedDependency::getName))
                .contains("requests", "certifi", "urllib3");

        // requests should have transitive dependencies linked
        ResolvedDependency requests = marker.getResolvedDependencies().stream()
                .filter(r -> r.getName().equals("requests")).findFirst().orElse(null);
        assertThat(requests).isNotNull();
        assertThat(requests.getDependencies()).isNotNull();
        assertThat(requests.getDependencies().stream().map(ResolvedDependency::getName))
                .contains("certifi", "urllib3");

        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Uv);
    }

    @Test
    void builderCreatesDslName() {
        RequirementsTxtParser.Builder builder = RequirementsTxtParser.builder();
        assertThat(builder.getDslName()).isEqualTo("requirements.txt");
        assertThat(builder.build()).isInstanceOf(RequirementsTxtParser.class);
    }
}

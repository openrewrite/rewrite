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
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.PackageManagerExecutor;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
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
    void dependenciesFromResolvedWithoutGraphTreatsAllAsDirect() {
        List<ResolvedDependency> resolved = List.of(
                new ResolvedDependency("certifi", "2024.2.2", null, null),
                new ResolvedDependency("requests", "2.31.0", null, null));
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
    void dependenciesFromResolvedTreatsDeclaredPackagesAsDirect() {
        ResolvedDependency certifi = new ResolvedDependency("certifi", "2024.2.2", null, null);
        ResolvedDependency requests = new ResolvedDependency("requests", "2.31.0", null, List.of(certifi));

        List<ResolvedDependency> resolved = List.of(certifi, requests);
        Set<String> declared = RequirementsTxtParser.parseDeclaredPackageNames(
                "certifi==2024.2.2\nrequests==2.31.0\n");
        List<Dependency> deps = RequirementsTxtParser.dependenciesFromResolved(resolved, declared);

        assertThat(deps).hasSize(2);
        assertThat(deps.get(0).getName()).isEqualTo("certifi");
        assertThat(deps.get(1).getName()).isEqualTo("requests");
    }

    @Test
    void declaredPackagesAreDirectAndUndeclaredTransitivesAreExcluded() {
        ResolvedDependency urllib3 = new ResolvedDependency("urllib3", "2.2.1", null, null);
        ResolvedDependency charsetNormalizer = new ResolvedDependency("charset-normalizer", "3.3.2", null, null);
        ResolvedDependency certifi = new ResolvedDependency("certifi", "2024.2.2", null, null);
        ResolvedDependency requests = new ResolvedDependency("requests", "2.31.0", null,
                List.of(certifi, urllib3, charsetNormalizer));

        List<ResolvedDependency> resolved = List.of(urllib3, charsetNormalizer, certifi, requests);
        Set<String> declared = RequirementsTxtParser.parseDeclaredPackageNames(
                "requests==2.31.0\ncertifi==2024.2.2\n");
        List<Dependency> deps = RequirementsTxtParser.dependenciesFromResolved(resolved, declared);

        assertThat(deps).hasSize(2);
        assertThat(deps.stream().map(Dependency::getName))
                .containsExactly("certifi", "requests");
    }

    @Test
    void parseDeclaredPackageNamesExtractsNames() {
        Set<String> names = RequirementsTxtParser.parseDeclaredPackageNames("""
                # This is a comment
                requests>=2.28.0
                certifi==2024.2.2
                charset-normalizer<4,>=2
                Jinja2~=3.1.5
                -r other-requirements.txt
                aiohttp==3.13.3

                langchain-core==1.2.12
                """);
        assertThat(names).containsExactlyInAnyOrder(
                "requests", "certifi", "charset_normalizer", "jinja2",
                "aiohttp", "langchain_core");
    }

    @Test
    void aFileDeclaresWhatItsIncludesDeclare(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("requirements.txt"), "-r requirements/base.txt\n");
        Files.createDirectories(tempDir.resolve("requirements"));
        Files.writeString(tempDir.resolve("requirements/base.txt"), "click\n");
        Path venv = tempDir.resolve(".venv");
        Path distInfo = venv.resolve("lib/python3.12/site-packages/click-8.1.7.dist-info");
        Files.createDirectories(distInfo);
        Files.writeString(distInfo.resolve("METADATA"), "Metadata-Version: 2.4\nName: click\nVersion: 8.1.7\n");

        SourceFile parsed = new RequirementsTxtParser(venv).parseInputs(
                singletonList(Parser.Input.fromFile(tempDir.resolve("requirements.txt"))), tempDir,
                new InMemoryExecutionContext()).findFirst().orElseThrow();

        assertThat(parsed.getMarkers().findFirst(PythonResolutionResult.class).orElseThrow().getDependencies())
                .extracting(Dependency::getName).containsExactly("click");
    }

    @Test
    void anEnvWithoutPackagesLeavesTheFileWithoutAMarker(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("requirements.txt"), "six ; python_version < '3.0'\n");
        Path venv = tempDir.resolve(".venv");
        Files.createDirectories(venv.resolve("lib/python3.12/site-packages"));

        SourceFile parsed = new RequirementsTxtParser(venv).parseInputs(
                singletonList(Parser.Input.fromFile(tempDir.resolve("requirements.txt"))), tempDir,
                new InMemoryExecutionContext()).findFirst().orElseThrow();

        assertThat(parsed.getMarkers().findFirst(ParseExceptionResult.class)).isEmpty();
        assertThat(parsed.getMarkers().findFirst(PythonResolutionResult.class)).isEmpty();
    }

    @Test
    void markerContainsDependenciesFromFreeze() {
        assumeTrue(PackageManagerExecutor.UV.find() != null, "uv is not installed");

        String requirements = "requests>=2.28.0\n";

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("requirements.txt"),
                requirements
        );
        List<SourceFile> parsed = parser.parseInputs(
                singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(toList());

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
    void unresolvableRequirementsAttachesParseExceptionMarker() {
        // A non-existent package will fail to install regardless of whether uv is present
        // (without uv, workspace creation fails; with uv, the install itself fails).
        String requirements = "this-package-definitely-does-not-exist-zzz==9.9.9\n";

        RequirementsTxtParser parser = new RequirementsTxtParser();
        Parser.Input input = Parser.Input.fromString(Paths.get("requirements.txt"), requirements);
        List<SourceFile> parsed = parser.parseInputs(
                singletonList(input),
                null,
                new InMemoryExecutionContext(t -> {})
        ).collect(toList());

        assertThat(parsed).hasSize(1);
        SourceFile sf = parsed.get(0);
        ParseExceptionResult marker = sf.getMarkers().findFirst(ParseExceptionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getMessage()).contains("PythonResolutionResult");
        // Should not have a successful resolution marker
        assertThat(sf.getMarkers().findFirst(PythonResolutionResult.class)).isEmpty();
    }

    @Test
    void builderCreatesDslName() {
        RequirementsTxtParser.Builder builder = RequirementsTxtParser.builder();
        assertThat(builder.getDslName()).isEqualTo("requirements.txt");
        assertThat(builder.build()).isInstanceOf(RequirementsTxtParser.class);
    }
}

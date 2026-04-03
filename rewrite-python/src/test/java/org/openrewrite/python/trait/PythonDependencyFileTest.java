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
package org.openrewrite.python.trait;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.python.Assertions.pyproject;
import static org.openrewrite.python.Assertions.requirementsTxt;

class PythonDependencyFileTest implements RewriteTest {

    // region Helper methods

    private static PythonResolutionResult createMarker(List<Dependency> dependencies,
                                                       List<ResolvedDependency> resolved) {
        return new PythonResolutionResult(
                randomId(), "test-project", "1.0.0", null, null,
                ".", null, null,
                Collections.emptyList(), dependencies,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyList(),
                resolved, null, null
        );
    }

    private static Toml.Document parseToml(String content, PythonResolutionResult marker) {
        TomlParser parser = new TomlParser();
        Parser.Input input = Parser.Input.fromString(Paths.get("pyproject.toml"), content);
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());
        Toml.Document doc = (Toml.Document) parsed.get(0);
        return doc.withMarkers(doc.getMarkers().addIfAbsent(marker));
    }

    private static PlainText createRequirementsTxt(String content, PythonResolutionResult marker) {
        return new PlainText(
                randomId(), Paths.get("requirements.txt"),
                Markers.EMPTY.addIfAbsent(marker),
                "UTF-8", false, null, null, content, null
        );
    }

    private static Cursor rootCursor(Object value) {
        return new Cursor(new Cursor(null, Cursor.ROOT_VALUE), value);
    }

    private static PyProjectFile pyProjectTrait(Toml.Document doc, PythonResolutionResult marker) {
        return new PyProjectFile(rootCursor(doc), marker);
    }

    private static RequirementsFile requirementsTrait(PlainText pt, PythonResolutionResult marker) {
        return new RequirementsFile(rootCursor(pt), marker);
    }

    /**
     * A recipe that applies {@link PythonDependencyFile#withDependencySearchMarkers} via the trait matcher.
     */
    private static Recipe searchMarkersRecipe(Map<String, String> packageMessages) {
        return RewriteTest.toRecipe(() -> new TreeVisitor<Tree, ExecutionContext>() {
            final PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();

            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                PythonDependencyFile trait = matcher.test(getCursor());
                if (trait != null) {
                    return trait.withDependencySearchMarkers(packageMessages, ctx).getTree();
                }
                return tree;
            }
        });
    }

    // endregion

    @Nested
    class RewritePep508SpecTest {
        @Test
        void simpleUpgrade() {
            String result = PythonDependencyFile.rewritePep508Spec("requests>=2.28.0", "requests", "2.31.0");
            assertThat(result).isEqualTo("requests>=2.31.0");
        }

        @Test
        void preservesExtras() {
            String result = PythonDependencyFile.rewritePep508Spec("requests[security]>=2.28.0", "requests", "2.31.0");
            assertThat(result).isEqualTo("requests[security]>=2.31.0");
        }

        @Test
        void preservesEnvironmentMarker() {
            String result = PythonDependencyFile.rewritePep508Spec(
                    "pywin32>=300; sys_platform=='win32'", "pywin32", "306");
            assertThat(result).isEqualTo("pywin32>=306; sys_platform=='win32'");
        }

        @Test
        void preservesExtrasAndMarker() {
            String result = PythonDependencyFile.rewritePep508Spec(
                    "requests[security]>=2.28.0; python_version>='3.8'", "requests", "2.31.0");
            assertThat(result).isEqualTo("requests[security]>=2.31.0; python_version>='3.8'");
        }

        @Test
        void nameOnly() {
            String result = PythonDependencyFile.rewritePep508Spec("requests", "requests", "2.31.0");
            assertThat(result).isEqualTo("requests>=2.31.0");
        }
    }

    @Nested
    class UpdateResolvedVersionsTest {
        @Test
        void updatesMatchingVersions() {
            ResolvedDependency requests = new ResolvedDependency("requests", "2.28.0", null, null);
            ResolvedDependency flask = new ResolvedDependency("flask", "2.0.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(), Arrays.asList(requests, flask));

            Map<String, String> updates = new HashMap<>();
            updates.put("requests", "2.31.0");

            PythonResolutionResult updated = PythonDependencyFile.updateResolvedVersions(marker, updates);

            assertThat(updated.getResolvedDependencies()).hasSize(2);
            assertThat(updated.getResolvedDependencies().get(0).getVersion()).isEqualTo("2.31.0");
            assertThat(updated.getResolvedDependencies().get(1).getVersion()).isEqualTo("2.0.0");
        }

        @Test
        void returnsOriginalWhenNoChanges() {
            ResolvedDependency requests = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(), Collections.singletonList(requests));

            Map<String, String> updates = new HashMap<>();
            updates.put("nonexistent", "1.0.0");

            PythonResolutionResult updated = PythonDependencyFile.updateResolvedVersions(marker, updates);

            assertThat(updated).isSameAs(marker);
        }

        @Test
        void returnsOriginalWhenVersionUnchanged() {
            ResolvedDependency requests = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(), Collections.singletonList(requests));

            Map<String, String> updates = new HashMap<>();
            updates.put("requests", "2.28.0");

            PythonResolutionResult updated = PythonDependencyFile.updateResolvedVersions(marker, updates);

            assertThat(updated).isSameAs(marker);
        }
    }

    @Nested
    class MatcherTest {
        @Test
        void matchesPyProjectToml() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.31.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(), Collections.singletonList(resolved));
            Toml.Document doc = parseToml("[project]\nname = \"test\"\ndependencies = [\"requests>=2.28.0\"]", marker);

            PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();
            PythonDependencyFile result = matcher.test(rootCursor(doc));

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(PyProjectFile.class);
        }

        @Test
        void matchesRequirementsTxt() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.31.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(), Collections.singletonList(resolved));
            PlainText pt = createRequirementsTxt("requests>=2.28.0", marker);

            PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();
            PythonDependencyFile result = matcher.test(rootCursor(pt));

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(RequirementsFile.class);
        }

        @Test
        void doesNotMatchWithoutMarker() {
            TomlParser parser = new TomlParser();
            Parser.Input input = Parser.Input.fromString(Paths.get("pyproject.toml"),
                    "[project]\nname = \"test\"");
            Toml.Document doc = (Toml.Document) parser.parseInputs(
                    Collections.singletonList(input), null,
                    new InMemoryExecutionContext(Throwable::printStackTrace)
            ).collect(Collectors.toList()).get(0);

            PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();
            assertThat(matcher.test(rootCursor(doc))).isNull();
        }

        @Test
        void doesNotMatchNonPythonFile() {
            PlainText pt = new PlainText(
                    randomId(), Paths.get("readme.txt"),
                    Markers.EMPTY, "UTF-8", false, null, null, "hello", null
            );

            PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();
            assertThat(matcher.test(rootCursor(pt))).isNull();
        }
    }

    @Nested
    class PyProjectFileTest {

        @Test
        void upgradesDependencyVersion() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            Dependency dep = new Dependency("requests", ">=2.28.0", null, null, resolved);
            PythonResolutionResult marker = createMarker(Collections.singletonList(dep),
                    Collections.singletonList(resolved));

            String toml = "[project]\nname = \"test\"\ndependencies = [\n    \"requests>=2.28.0\",\n]";
            Toml.Document doc = parseToml(toml, marker);
            PyProjectFile trait = pyProjectTrait(doc, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            PyProjectFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            Toml.Document result = (Toml.Document) upgraded.getTree();
            String printed = result.printAll();
            assertThat(printed).contains("\"requests>=2.31.0\"");
            assertThat(printed).doesNotContain("\"requests>=2.28.0\"");
        }

        @Test
        void upgradePreservesExtras() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            String toml = "[project]\nname = \"test\"\ndependencies = [\n    \"requests[security]>=2.28.0\",\n]";
            Toml.Document doc = parseToml(toml, marker);
            PyProjectFile trait = pyProjectTrait(doc, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            PyProjectFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            String printed = ((Toml.Document) upgraded.getTree()).printAll();
            assertThat(printed).contains("\"requests[security]>=2.31.0\"");
        }

        @Test
        void upgradeNoOpWhenPackageNotFound() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            String toml = "[project]\nname = \"test\"\ndependencies = [\n    \"requests>=2.28.0\",\n]";
            Toml.Document doc = parseToml(toml, marker);
            PyProjectFile trait = pyProjectTrait(doc, marker);

            Map<String, String> upgrades = Collections.singletonMap("nonexistent", "1.0.0");
            PyProjectFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            assertThat(upgraded).isSameAs(trait);
        }

        @Test
        void upgradeUpdatesResolvedVersionsInMarker() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            String toml = "[project]\nname = \"test\"\ndependencies = [\n    \"requests>=2.28.0\",\n]";
            Toml.Document doc = parseToml(toml, marker);
            PyProjectFile trait = pyProjectTrait(doc, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            PyProjectFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            assertThat(upgraded.getMarker().getResolvedDependencies().get(0).getVersion()).isEqualTo("2.31.0");
        }

        @Test
        void searchMarkersOnVulnerableDependency() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            String toml = "[project]\nname = \"test\"\ndependencies = [\n    \"requests>=2.28.0\",\n    \"flask>=2.0\",\n]";
            Toml.Document doc = parseToml(toml, marker);
            PyProjectFile trait = pyProjectTrait(doc, marker);

            Map<String, String> vulnerabilities = Collections.singletonMap("requests", "CVE-2023-1234");
            ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
            PyProjectFile marked = trait.withDependencySearchMarkers(vulnerabilities, ctx);

            Toml.Document result = (Toml.Document) marked.getTree();
            new org.openrewrite.toml.TomlVisitor<Integer>() {
                @Override
                public Toml visitLiteral(Toml.Literal literal, Integer p) {
                    if (literal.getValue().toString().contains("requests")) {
                        assertThat(literal.getMarkers().findFirst(SearchResult.class)).isPresent();
                    }
                    return literal;
                }
            }.visit(result, 0);
            assertThat(result).isNotSameAs(doc);
        }

        @Test
        void searchMarkersNoOpWhenNoMatch() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            String toml = "[project]\nname = \"test\"\ndependencies = [\n    \"requests>=2.28.0\",\n]";
            Toml.Document doc = parseToml(toml, marker);
            PyProjectFile trait = pyProjectTrait(doc, marker);

            Map<String, String> vulnerabilities = Collections.singletonMap("nonexistent", "CVE-2023-9999");
            ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
            PyProjectFile marked = trait.withDependencySearchMarkers(vulnerabilities, ctx);

            assertThat(marked).isSameAs(trait);
        }

        @Test
        void upgradeMultipleDependencies() {
            ResolvedDependency requests = new ResolvedDependency("requests", "2.28.0", null, null);
            ResolvedDependency flask = new ResolvedDependency("flask", "2.0.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Arrays.asList(requests, flask));

            String toml = "[project]\nname = \"test\"\ndependencies = [\n    \"requests>=2.28.0\",\n    \"flask>=2.0.0\",\n]";
            Toml.Document doc = parseToml(toml, marker);
            PyProjectFile trait = pyProjectTrait(doc, marker);

            Map<String, String> upgrades = new HashMap<>();
            upgrades.put("requests", "2.31.0");
            upgrades.put("flask", "3.0.0");
            PyProjectFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            String printed = ((Toml.Document) upgraded.getTree()).printAll();
            assertThat(printed).contains("\"requests>=2.31.0\"");
            assertThat(printed).contains("\"flask>=3.0.0\"");
        }

        @Test
        void doesNotUpgradeDependenciesOutsideProjectSection() {
            ResolvedDependency resolved = new ResolvedDependency("setuptools", "68.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            String toml = "[build-system]\nrequires = [\"setuptools>=67.0\"]\n\n[project]\nname = \"test\"\ndependencies = []";
            Toml.Document doc = parseToml(toml, marker);
            PyProjectFile trait = pyProjectTrait(doc, marker);

            Map<String, String> upgrades = Collections.singletonMap("setuptools", "69.0");
            PyProjectFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            // build-system is not inside [project], so it should not be upgraded
            assertThat(upgraded).isSameAs(trait);
        }
    }

    @Nested
    class RequirementsFileTest {

        @Test
        void upgradesDependencyVersion() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("requests>=2.28.0\nflask>=2.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            PlainText result = (PlainText) upgraded.getTree();
            assertThat(result.getText()).contains("requests>=2.31.0");
            assertThat(result.getText()).contains("flask>=2.0");
        }

        @Test
        void upgradePreservesExtras() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("requests[security]>=2.28.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            assertThat(((PlainText) upgraded.getTree()).getText()).isEqualTo("requests[security]>=2.31.0");
        }

        @Test
        void upgradePreservesEnvironmentMarkers() {
            ResolvedDependency resolved = new ResolvedDependency("pywin32", "300", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("pywin32>=300; sys_platform=='win32'", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("pywin32", "306");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            assertThat(((PlainText) upgraded.getTree()).getText())
                    .isEqualTo("pywin32>=306; sys_platform=='win32'");
        }

        @Test
        void upgradeSkipsComments() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("# this is a comment\nrequests>=2.28.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            String text = ((PlainText) upgraded.getTree()).getText();
            assertThat(text).startsWith("# this is a comment\n");
            assertThat(text).contains("requests>=2.31.0");
        }

        @Test
        void upgradeSkipsFlags() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("-r base.txt\nrequests>=2.28.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            String text = ((PlainText) upgraded.getTree()).getText();
            assertThat(text).startsWith("-r base.txt\n");
            assertThat(text).contains("requests>=2.31.0");
        }

        @Test
        void upgradeNoOpWhenPackageNotFound() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("requests>=2.28.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("nonexistent", "1.0.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            assertThat(upgraded).isSameAs(trait);
        }

        @Test
        void upgradeUpdatesResolvedVersionsInMarker() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("requests>=2.28.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            assertThat(upgraded.getMarker().getResolvedDependencies().get(0).getVersion()).isEqualTo("2.31.0");
        }

        @Test
        void upgradePreservesLeadingWhitespace() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("  requests>=2.28.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            assertThat(((PlainText) upgraded.getTree()).getText()).isEqualTo("  requests>=2.31.0");
        }

        @Test
        void addsDependencyToEnd() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("requests>=2.28.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> additions = Collections.singletonMap("flask", "3.0.0");
            RequirementsFile added = trait.withAddedDependencies(additions, null, null);

            String text = ((PlainText) added.getTree()).getText();
            assertThat(text).isEqualTo("requests>=2.28.0\nflask>=3.0.0");
        }

        @Test
        void addDependencyNoOpWhenAlreadyPresent() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("requests>=2.28.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> additions = Collections.singletonMap("requests", "2.31.0");
            RequirementsFile added = trait.withAddedDependencies(additions, null, null);

            assertThat(added).isSameAs(trait);
        }

        @Test
        void searchMarkersOnVulnerableDependency() {
            rewriteRun(
                    spec -> spec.recipe(searchMarkersRecipe(
                            Collections.singletonMap("requests", "CVE-2023-1234"))),
                    requirementsTxt(
                            "requests>=2.28.0\nflask>=2.0",
                            "~~>requests>=2.28.0\nflask>=2.0"
                    )
            );
        }

        @Test
        void searchMarkersNoOpWhenNoMatch() {
            rewriteRun(
                    spec -> spec.recipe(searchMarkersRecipe(
                            Collections.singletonMap("nonexistent", "CVE-2023-9999"))),
                    requirementsTxt("requests>=2.28.0")
            );
        }

        @Test
        void upgradeMultipleDependencies() {
            ResolvedDependency requests = new ResolvedDependency("requests", "2.28.0", null, null);
            ResolvedDependency flask = new ResolvedDependency("flask", "2.0.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Arrays.asList(requests, flask));

            PlainText pt = createRequirementsTxt("requests>=2.28.0\nflask>=2.0.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = new HashMap<>();
            upgrades.put("requests", "2.31.0");
            upgrades.put("flask", "3.0.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            String text = ((PlainText) upgraded.getTree()).getText();
            assertThat(text).contains("requests>=2.31.0");
            assertThat(text).contains("flask>=3.0.0");
        }

        @Test
        void upgradeHandlesEmptyLines() {
            ResolvedDependency resolved = new ResolvedDependency("requests", "2.28.0", null, null);
            PythonResolutionResult marker = createMarker(Collections.emptyList(),
                    Collections.singletonList(resolved));

            PlainText pt = createRequirementsTxt("requests>=2.28.0\n\nflask>=2.0", marker);
            RequirementsFile trait = requirementsTrait(pt, marker);

            Map<String, String> upgrades = Collections.singletonMap("requests", "2.31.0");
            RequirementsFile upgraded = trait.withUpgradedVersions(upgrades, null, null);

            String text = ((PlainText) upgraded.getTree()).getText();
            assertThat(text).isEqualTo("requests>=2.31.0\n\nflask>=2.0");
        }
    }

    @Nested
    class PyProjectSearchMarkersTest {

        @Test
        void searchMarkersViaMatcher() {
            rewriteRun(
                    spec -> spec.recipe(searchMarkersRecipe(
                            Collections.singletonMap("requests", "CVE-2023-1234"))),
                    pyproject(
                            """
                                [project]
                                name = "test"
                                dependencies = [
                                    "requests>=2.28.0",
                                    "flask>=2.0",
                                ]
                                """,
                            """
                                [project]
                                name = "test"
                                dependencies = [
                                    ~~(CVE-2023-1234)~~>"requests>=2.28.0",
                                    "flask>=2.0",
                                ]
                                """
                    )
            );
        }

        @Test
        void searchMarkersNoOpViaMatcher() {
            rewriteRun(
                    spec -> spec.recipe(searchMarkersRecipe(
                            Collections.singletonMap("nonexistent", "CVE-2023-9999"))),
                    pyproject(
                            """
                                [project]
                                name = "test"
                                dependencies = [
                                    "requests>=2.28.0",
                                ]
                                """
                    )
            );
        }
    }
}

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
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class PipfileFileTest implements RewriteTest {

    private static PythonResolutionResult createMarker(List<Dependency> dependencies) {
        return new PythonResolutionResult(
                randomId(), null, null, null, null,
                "Pipfile", null, null,
                Collections.emptyList(), dependencies,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), PythonResolutionResult.PackageManager.Pipenv, null
        );
    }

    private static Toml.Document parsePipfile(String content, PythonResolutionResult marker) {
        TomlParser parser = new TomlParser();
        Parser.Input input = Parser.Input.fromString(Paths.get("Pipfile"), content);
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());
        Toml.Document doc = (Toml.Document) parsed.get(0);
        return doc.withMarkers(doc.getMarkers().addIfAbsent(marker));
    }

    private static Cursor rootCursor(Object value) {
        return new Cursor(new Cursor(null, Cursor.ROOT_VALUE), value);
    }

    private static PipfileFile trait(Toml.Document doc, PythonResolutionResult marker) {
        return new PipfileFile(rootCursor(doc), marker);
    }

    @Nested
    class MatcherTest {
        @Test
        void matchesPipfile() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);

            PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();
            PythonDependencyFile result = matcher.test(rootCursor(doc));

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(PipfileFile.class);
        }

        @Test
        void doesNotMatchWithoutMarker() {
            TomlParser parser = new TomlParser();
            Parser.Input input = Parser.Input.fromString(Paths.get("Pipfile"), "[packages]\nrequests = \"*\"");
            Toml.Document doc = (Toml.Document) parser.parseInputs(
                    Collections.singletonList(input), null,
                    new InMemoryExecutionContext(Throwable::printStackTrace)
            ).collect(Collectors.toList()).get(0);

            PipfileFile.Matcher matcher = new PipfileFile.Matcher();
            assertThat(matcher.test(rootCursor(doc))).isNull();
        }

        @Test
        void doesNotMatchPyprojectToml() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            TomlParser parser = new TomlParser();
            Parser.Input input = Parser.Input.fromString(Paths.get("pyproject.toml"), "[project]\nname = \"test\"");
            Toml.Document doc = (Toml.Document) parser.parseInputs(
                    Collections.singletonList(input), null,
                    new InMemoryExecutionContext(Throwable::printStackTrace)
            ).collect(Collectors.toList()).get(0);
            doc = doc.withMarkers(doc.getMarkers().addIfAbsent(marker));

            PipfileFile.Matcher matcher = new PipfileFile.Matcher();
            assertThat(matcher.test(rootCursor(doc))).isNull();
        }
    }

    @Nested
    class UpgradeVersionTest {
        @Test
        void upgradeSimpleVersion() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile upgraded = t.withUpgradedVersions(
                    Collections.singletonMap("requests", ">=2.31.0"), null, null);

            String printed = ((Toml.Document) upgraded.getTree()).printAll();
            assertThat(printed).contains("requests = \">=2.31.0\"");
        }

        @Test
        void upgradeInDevPackages() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile(
                    "[packages]\nflask = \"*\"\n\n[dev-packages]\npytest = \">=7.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile upgraded = t.withUpgradedVersions(
                    Collections.singletonMap("pytest", ">=8.0"), "dev-packages", null);

            String printed = ((Toml.Document) upgraded.getTree()).printAll();
            assertThat(printed).contains("pytest = \">=8.0\"");
            assertThat(printed).contains("flask = \"*\"");
        }

        @Test
        void noOpWhenNotFound() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile upgraded = t.withUpgradedVersions(
                    Collections.singletonMap("nonexistent", ">=1.0"), null, null);

            assertThat(upgraded).isSameAs(t);
        }
    }

    @Nested
    class AddDependencyTest {
        @Test
        void addToPackages() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile added = t.withAddedDependencies(
                    Collections.singletonMap("flask", ">=2.0"), "packages", null);

            String printed = ((Toml.Document) added.getTree()).printAll();
            assertThat(printed).contains("flask = \">=2.0\"");
            assertThat(printed).contains("requests = \">=2.28.0\"");
        }

        @Test
        void addToDevPackages() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile(
                    "[packages]\nrequests = \"*\"\n\n[dev-packages]\npytest = \">=7.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile added = t.withAddedDependencies(
                    Collections.singletonMap("mypy", ">=1.0"), "dev-packages", null);

            String printed = ((Toml.Document) added.getTree()).printAll();
            assertThat(printed).contains("mypy = \">=1.0\"");
        }

        @Test
        void noOpWhenAlreadyPresent() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile added = t.withAddedDependencies(
                    Collections.singletonMap("requests", ">=2.31.0"), "packages", null);

            assertThat(added).isSameAs(t);
        }
    }

    @Nested
    class RemoveDependencyTest {
        @Test
        void removeFromPackages() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"\nflask = \"*\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile removed = t.withRemovedDependencies(
                    Collections.singleton("flask"), "packages", null);

            String printed = ((Toml.Document) removed.getTree()).printAll();
            assertThat(printed).contains("requests = \">=2.28.0\"");
            assertThat(printed).doesNotContain("flask");
        }

        @Test
        void noOpWhenNotFound() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile removed = t.withRemovedDependencies(
                    Collections.singleton("nonexistent"), "packages", null);

            assertThat(removed).isSameAs(t);
        }
    }

    @Nested
    class ChangeDependencyTest {
        @Test
        void renamePackage() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile changed = t.withChangedDependency("requests", "httpx", null);

            String printed = ((Toml.Document) changed.getTree()).printAll();
            assertThat(printed).contains("httpx = \">=2.28.0\"");
            assertThat(printed).doesNotContain("requests");
        }

        @Test
        void renameWithNewVersion() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);
            PipfileFile t = trait(doc, marker);

            PipfileFile changed = t.withChangedDependency("requests", "httpx", ">=0.24.0");

            String printed = ((Toml.Document) changed.getTree()).printAll();
            assertThat(printed).contains("httpx = \">=0.24.0\"");
        }
    }

    @Nested
    class SearchMarkersTest {
        @Test
        void markVulnerableDependency() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile(
                    "[packages]\nrequests = \">=2.28.0\"\nflask = \"*\"", marker);
            PipfileFile t = trait(doc, marker);

            ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
            PipfileFile marked = t.withDependencySearchMarkers(
                    Collections.singletonMap("requests", "CVE-2023-1234"), ctx);

            Toml.Document result = (Toml.Document) marked.getTree();
            boolean[] found = {false};
            new org.openrewrite.toml.TomlVisitor<Integer>() {
                @Override
                public Toml visitKeyValue(Toml.KeyValue keyValue, Integer p) {
                    if (keyValue.getKey() instanceof Toml.Identifier &&
                            "requests".equals(((Toml.Identifier) keyValue.getKey()).getName()) &&
                            keyValue.getMarkers().findFirst(SearchResult.class).isPresent()) {
                        found[0] = true;
                    }
                    return keyValue;
                }
            }.visit(result, 0);
            assertThat(found[0]).as("requests should have SearchResult marker").isTrue();
        }

        @Test
        void noOpWhenNoMatch() {
            PythonResolutionResult marker = createMarker(Collections.emptyList());
            Toml.Document doc = parsePipfile("[packages]\nrequests = \">=2.28.0\"", marker);
            PipfileFile t = trait(doc, marker);

            ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
            PipfileFile marked = t.withDependencySearchMarkers(
                    Collections.singletonMap("nonexistent", "CVE-2023-9999"), ctx);

            assertThat(marked).isSameAs(t);
        }
    }
}

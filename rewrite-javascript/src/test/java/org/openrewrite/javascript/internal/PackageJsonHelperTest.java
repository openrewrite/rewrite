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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class PackageJsonHelperTest {

    @Test
    void refreshMarkerRebuildsDeclaredDependencies() {
        // Build a minimal package.json with one dep, attach a marker, then ask
        // refreshMarker to re-derive the marker's dependency list.
        String json = "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"dependencies\": {\n" +
                "    \"lodash\": \"^4.17.21\",\n" +
                "    \"uuid\": \"^9.0.0\"\n" +
                "  }\n" +
                "}\n";
        Json.Document doc = parsePackageJson(json);
        NodeResolutionResult marker = new NodeResolutionResult(
                UUID.randomUUID(), "x", null, null, ".",
                null,
                asList(new Dependency("uuid", "^9.0.0", null)),     // stale
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<NodeResolutionResult.ResolvedDependency>emptyList(),
                NodeResolutionResult.PackageManager.Npm,
                null, null);
        doc = doc.withMarkers(doc.getMarkers().add(marker));

        SourceFile refreshed = PackageJsonHelper.refreshMarker(doc);
        NodeResolutionResult result = refreshed.getMarkers().findFirst(NodeResolutionResult.class).orElseThrow();
        List<Dependency> deps = result.getDependencies();

        assertThat(deps).extracting(Dependency::getName).containsExactly("lodash", "uuid");
        assertThat(deps).extracting(Dependency::getVersionConstraint).containsExactly("^4.17.21", "^9.0.0");
    }

    @Test
    void addDependencyAppendsToExistingScope() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"dependencies\": {\n" +
                "    \"uuid\": \"^9.0.0\"\n" +
                "  }\n" +
                "}\n");
        Json.Document modified = PackageJsonHelper.addDependency(doc, "lodash", "^4.17.21", "dependencies");
        assertThat(modified.printAll()).isEqualTo(
                "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"dependencies\": {\n" +
                "    \"uuid\": \"^9.0.0\",\n" +
                "    \"lodash\": \"^4.17.21\"\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void addDependencyCreatesMissingScope() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"name\": \"x\"\n" +
                "}\n");
        Json.Document modified = PackageJsonHelper.addDependency(doc, "lodash", "^4.17.21", "dependencies");
        assertThat(modified.printAll()).isEqualTo(
                "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"dependencies\": {\n" +
                "    \"lodash\": \"^4.17.21\"\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void removeDependencyDropsMember() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"dependencies\": {\n" +
                "    \"lodash\": \"^4.17.21\",\n" +
                "    \"uuid\": \"^9.0.0\"\n" +
                "  }\n" +
                "}\n");
        Json.Document modified = PackageJsonHelper.removeDependency(
                doc, "lodash", new java.util.LinkedHashSet<>(java.util.Arrays.asList("dependencies")));
        assertThat(modified.printAll()).isEqualTo(
                "{\n" +
                "  \"dependencies\": {\n" +
                "    \"uuid\": \"^9.0.0\"\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void removeDependencyRemovesEmptyScope() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"dependencies\": {\n" +
                "    \"lodash\": \"^4.17.21\"\n" +
                "  }\n" +
                "}\n");
        Json.Document modified = PackageJsonHelper.removeDependency(
                doc, "lodash", new java.util.LinkedHashSet<>(java.util.Arrays.asList("dependencies")));
        assertThat(modified.printAll()).isEqualTo(
                "{\n" +
                "  \"name\": \"x\"\n" +
                "}\n");
    }

    @Test
    void upgradeVersionUpdatesValue() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"dependencies\": {\n" +
                "    \"lodash\": \"^4.17.20\"\n" +
                "  }\n" +
                "}\n");
        Json.Document modified = PackageJsonHelper.upgradeVersion(
                doc,
                java.util.Collections.singletonList(new MatchedDependency("lodash", "dependencies", "^4.17.20")),
                "^4.17.21");
        assertThat(modified.printAll()).isEqualTo(
                "{\n" +
                "  \"dependencies\": {\n" +
                "    \"lodash\": \"^4.17.21\"\n" +
                "  }\n" +
                "}\n");
    }

    private static Json.Document parsePackageJson(String content) {
        JsonParser parser = new JsonParser();
        return (Json.Document) parser.parseInputs(
                Collections.singletonList(
                        org.openrewrite.Parser.Input.fromString(Paths.get("package.json"), content)),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace))
                .findFirst().orElseThrow();
    }
}

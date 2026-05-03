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

    @Test
    void changeDependencyRenamesAndUpdatesVersion() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"dependencies\": {\n" +
                "    \"old-pkg\": \"^1.0.0\"\n" +
                "  }\n" +
                "}\n");
        Json.Document modified = PackageJsonHelper.changeDependency(
                doc, "old-pkg", "new-pkg", "^2.0.0", "dependencies");
        assertThat(modified.printAll()).isEqualTo(
                "{\n" +
                "  \"dependencies\": {\n" +
                "    \"new-pkg\": \"^2.0.0\"\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void changeDependencyPreservesVersionWhenNewVersionNull() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"dependencies\": {\n" +
                "    \"old-pkg\": \"^1.0.0\"\n" +
                "  }\n" +
                "}\n");
        Json.Document modified = PackageJsonHelper.changeDependency(
                doc, "old-pkg", "new-pkg", null, null);
        assertThat(modified.printAll()).contains("\"new-pkg\": \"^1.0.0\"");
        assertThat(modified.printAll()).doesNotContain("old-pkg");
    }

    @Test
    void parseDependencyPathPnpmStyle() {
        java.util.List<DependencyPathSegment> segs =
                PackageJsonOverrides.parsePath("express>accepts");
        assertThat(segs).extracting(DependencyPathSegment::getName).containsExactly("express", "accepts");
    }

    @Test
    void parseDependencyPathScopedPackage() {
        java.util.List<DependencyPathSegment> segs =
                PackageJsonOverrides.parsePath("@types/node>foo");
        assertThat(segs).extracting(DependencyPathSegment::getName).containsExactly("@types/node", "foo");
    }

    @Test
    void upgradeTransitiveAddsNpmOverride() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"dependencies\": { \"lodash\": \"^4.17.20\" }\n" +
                "}\n");
        Json.Document modified = PackageJsonHelper.upgradeTransitive(
                doc, NodeResolutionResult.PackageManager.Npm, "lodash", "^4.17.21",
                java.util.Collections.<DependencyPathSegment>emptyList());
        assertThat(modified.printAll()).contains("\"overrides\"");
        assertThat(modified.printAll()).contains("\"lodash\": \"^4.17.21\"");
    }

    @Test
    void compileGlobPatternMatchesWildcard() {
        java.util.regex.Pattern p = PackageJsonHelper.compileGlobPattern("@types/*");
        assertThat(p.matcher("@types/node").matches()).isTrue();
        assertThat(p.matcher("react").matches()).isFalse();
    }

    @Test
    void overlayResolvedDepsRelinksDeclaredDep() {
        String json = "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"dependencies\": { \"lodash\": \"^4.17.21\" }\n" +
                "}\n";
        Json.Document doc = parsePackageJson(json);
        NodeResolutionResult marker = new NodeResolutionResult(
                UUID.randomUUID(), "x", null, null, ".",
                null,
                asList(new Dependency("lodash", "^4.17.21", null)),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<NodeResolutionResult.ResolvedDependency>emptyList(),
                NodeResolutionResult.PackageManager.Npm,
                null, null);
        Json.Document withMarker = doc.withMarkers(doc.getMarkers().add(marker));

        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/lodash\": { \"version\": \"4.17.21\", \"license\": \"MIT\" }\n" +
                "  }\n" +
                "}";
        SourceFile result = PackageJsonHelper.overlayResolvedDeps(
                withMarker, lock, NodeResolutionResult.PackageManager.Npm);

        NodeResolutionResult resultMarker = result.getMarkers()
                .findFirst(NodeResolutionResult.class).orElseThrow();
        assertThat(resultMarker.getResolvedDependencies()).hasSize(1);
        assertThat(resultMarker.getResolvedDependencies().get(0).getLicense()).isEqualTo("MIT");
        assertThat(resultMarker.getDependencies().get(0).getResolved()).isNotNull();
        assertThat(resultMarker.getDependencies().get(0).getResolved().getName()).isEqualTo("lodash");
        assertThat(resultMarker.getDependencies().get(0).getResolved().getVersion()).isEqualTo("4.17.21");
    }

    @Test
    void overlayResolvedDepsReturnsUnchangedWhenNoMarker() {
        Json.Document doc = parsePackageJson("{\"name\":\"x\"}");
        SourceFile result = PackageJsonHelper.overlayResolvedDeps(
                doc, "{\"packages\":{}}", NodeResolutionResult.PackageManager.Npm);
        assertThat(result).isSameAs(doc);
    }

    @Test
    void editAndRegenerateSkipsOverlayForUnsupportedPm() {
        // For yarn/pnpm, regen happens (or fails) but no overlay runs — marker resolvedDependencies
        // stays as it was. We test the npm/Bun positive path via integTest in Task 9/10.
        // This unit test pins the negative path: an edit of a YarnClassic package.json should
        // leave the marker's resolvedDependencies untouched even when overlayResolvedDeps would
        // throw on the lock content (we pass "no lock" so no overlay is even attempted).
        String json = "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"dependencies\": { \"lodash\": \"^4.17.21\" }\n" +
                "}\n";
        Json.Document doc = parsePackageJson(json);
        NodeResolutionResult.ResolvedDependency staleResolved = new NodeResolutionResult.ResolvedDependency(
                "lodash", "4.17.20", null, null, null, null, null, null);
        NodeResolutionResult marker = new NodeResolutionResult(
                UUID.randomUUID(), "x", null, null, ".",
                null,
                asList(new Dependency("lodash", "^4.17.21", staleResolved)),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                asList(staleResolved),
                NodeResolutionResult.PackageManager.YarnClassic,
                null, null);
        Json.Document withMarker = doc.withMarkers(doc.getMarkers().add(marker));

        // Use editAndRegenerate with capturedLockContent=null so no regen runs.
        // The overlay path is skipped; the document round-trips unchanged.
        PackageJsonHelper.EditAndRegenerateResult result = PackageJsonHelper.editAndRegenerate(
                withMarker,
                d -> PackageJsonHelper.addDependency(d, "uuid", "^9.0.0", "dependencies"),
                null,
                null);

        assertThat(result.isChanged()).isTrue();
        NodeResolutionResult after = result.getModifiedPackageJson().getMarkers()
                .findFirst(NodeResolutionResult.class).orElseThrow();
        // Stale resolved dep is preserved (we did not overlay).
        assertThat(after.getResolvedDependencies()).hasSize(1);
        assertThat(after.getResolvedDependencies().get(0).getVersion()).isEqualTo("4.17.20");
    }

    @Test
    void overlayResolvedDepsDispatchesYarnClassic() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"dependencies\": { \"lodash\": \"^4.17.21\" }\n" +
                "}\n");
        NodeResolutionResult marker = new NodeResolutionResult(
                UUID.randomUUID(), "x", null, null, ".", null,
                asList(new Dependency("lodash", "^4.17.21", null)),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<NodeResolutionResult.ResolvedDependency>emptyList(),
                NodeResolutionResult.PackageManager.YarnClassic,
                null, null);
        Json.Document withMarker = doc.withMarkers(doc.getMarkers().add(marker));

        String yarnLock = "lodash@^4.17.21:\n" +
                "  version \"4.17.21\"\n";
        SourceFile result = PackageJsonHelper.overlayResolvedDeps(
                withMarker, yarnLock, NodeResolutionResult.PackageManager.YarnClassic);
        NodeResolutionResult m = result.getMarkers()
                .findFirst(NodeResolutionResult.class).orElseThrow();
        assertThat(m.getResolvedDependencies()).hasSize(1);
        assertThat(m.getResolvedDependencies().get(0).getName()).isEqualTo("lodash");
        assertThat(m.getDependencies().get(0).getResolved()).isNotNull();
    }

    @Test
    void overlayResolvedDepsDispatchesYarnBerry() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"dependencies\": { \"lodash\": \"^4.17.21\" }\n" +
                "}\n");
        NodeResolutionResult marker = new NodeResolutionResult(
                UUID.randomUUID(), "x", null, null, ".", null,
                asList(new Dependency("lodash", "^4.17.21", null)),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<NodeResolutionResult.ResolvedDependency>emptyList(),
                NodeResolutionResult.PackageManager.YarnBerry,
                null, null);
        Json.Document withMarker = doc.withMarkers(doc.getMarkers().add(marker));

        String yarnLock = "__metadata:\n" +
                "  version: 6\n" +
                "\n" +
                "\"lodash@npm:^4.17.21\":\n" +
                "  version: 4.17.21\n" +
                "  resolution: \"lodash@npm:4.17.21\"\n";
        SourceFile result = PackageJsonHelper.overlayResolvedDeps(
                withMarker, yarnLock, NodeResolutionResult.PackageManager.YarnBerry);
        NodeResolutionResult m = result.getMarkers()
                .findFirst(NodeResolutionResult.class).orElseThrow();
        assertThat(m.getResolvedDependencies()).hasSize(1);
        assertThat(m.getDependencies().get(0).getResolved()).isNotNull();
    }

    @Test
    void overlayResolvedDepsDispatchesPnpm() {
        Json.Document doc = parsePackageJson(
                "{\n" +
                "  \"dependencies\": { \"lodash\": \"^4.17.21\" }\n" +
                "}\n");
        NodeResolutionResult marker = new NodeResolutionResult(
                UUID.randomUUID(), "x", null, null, ".", null,
                asList(new Dependency("lodash", "^4.17.21", null)),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<Dependency>emptyList(),
                Collections.<NodeResolutionResult.ResolvedDependency>emptyList(),
                NodeResolutionResult.PackageManager.Pnpm,
                null, null);
        Json.Document withMarker = doc.withMarkers(doc.getMarkers().add(marker));

        String pnpmLock = "lockfileVersion: '6.0'\n" +
                "\n" +
                "dependencies:\n" +
                "  lodash:\n" +
                "    specifier: ^4.17.21\n" +
                "    version: 4.17.21\n" +
                "\n" +
                "packages:\n" +
                "  /lodash@4.17.21:\n" +
                "    resolution: {integrity: sha512-x}\n";
        SourceFile result = PackageJsonHelper.overlayResolvedDeps(
                withMarker, pnpmLock, NodeResolutionResult.PackageManager.Pnpm);
        NodeResolutionResult m = result.getMarkers()
                .findFirst(NodeResolutionResult.class).orElseThrow();
        assertThat(m.getResolvedDependencies()).hasSize(1);
        assertThat(m.getResolvedDependencies().get(0).getVersion()).isEqualTo("4.17.21");
        assertThat(m.getDependencies().get(0).getResolved()).isNotNull();
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

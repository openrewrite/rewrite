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
package org.openrewrite.javascript;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.javascript.internal.PackageManagerExecutor;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;
import org.openrewrite.json.tree.Json;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.npm;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.pnpm;
import static org.openrewrite.javascript.Assertions.yarnBerry;
import static org.openrewrite.test.SourceSpecs.text;

class AddDependencyResolvedOverlayTest implements RewriteTest {

    private static final String PACKAGE_JSON = "{\n" +
            "  \"name\": \"x\",\n" +
            "  \"dependencies\": {\n" +
            "    \"uuid\": \"^9.0.0\"\n" +
            "  }\n" +
            "}\n";

    @Test
    void addDependencyOverlaysResolvedVersion(@TempDir Path tempDir) {
        // Ensure workspace exists so we can read the initial lock file.
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(PACKAGE_JSON);
        Path lockFilePath = workspace.resolve("package-lock.json");
        String lockFileContent;
        try {
            lockFileContent = Files.readString(lockFilePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read package-lock.json from workspace", e);
        }

        AtomicReference<NodeResolutionResult> capturedAfterMarker = new AtomicReference<>();

        rewriteRun(
                spec -> spec.recipe(new AddDependency("lodash", "^4.17.21", "dependencies")),
                npm(tempDir,
                        packageJson(
                                PACKAGE_JSON,
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"uuid\": \"^9.0.0\",\n" +
                                "    \"lodash\": \"^4.17.21\"\n" +
                                "  }\n" +
                                "}\n",
                                spec -> spec.afterRecipe(d -> {
                                    if (d instanceof Json.Document) {
                                        capturedAfterMarker.set(((Json.Document) d).getMarkers()
                                                .findFirst(NodeResolutionResult.class).orElse(null));
                                    }
                                })),
                        text(lockFileContent, s -> s.path("package-lock.json")
                                // Let the lock file change; we don't validate its exact after-content here.
                                .after(after -> after)
                                .noTrim())));

        NodeResolutionResult after = capturedAfterMarker.get();
        assertThat(after).as("modified package.json must have a marker").isNotNull();

        // The new lodash dep should now be in the resolved deps.
        ResolvedDependency lodash = after.getResolvedDependency("lodash");
        assertThat(lodash).as("lodash should be resolved post-edit").isNotNull();
        // ^4.17.21 allows any 4.x.x >= 4.17.21; npm may resolve to a newer patch or minor.
        assertThat(lodash.getVersion()).matches("4\\.\\d+\\.\\d+");

        // The declared lodash Dependency entry should have its `resolved` populated.
        assertThat(after.getDependencies())
                .filteredOn(d -> "lodash".equals(d.getName()))
                .singleElement()
                .satisfies(d -> assertThat(d.getResolved()).isNotNull());
    }

    @Test
    void addDependencyOverlaysResolvedVersionYarnBerry(@TempDir Path tempDir) throws IOException {
        Assumptions.assumeTrue(PackageManagerExecutor.YARN.find() != null,
                "yarn not installed");
        String packageJsonContent = "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"packageManager\": \"yarn@4.0.2\",\n" +
                "  \"dependencies\": {\n" +
                "    \"uuid\": \"^9.0.0\"\n" +
                "  }\n" +
                "}\n";
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(packageJsonContent, PackageManager.YarnBerry);
        String lockFileContent = Files.readString(workspace.resolve("yarn.lock"));

        AtomicReference<NodeResolutionResult> capturedAfterMarker = new AtomicReference<>();

        rewriteRun(
                spec -> spec.recipe(new AddDependency("lodash", "^4.17.21", "dependencies")),
                yarnBerry(tempDir,
                        packageJson(
                                packageJsonContent,
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"packageManager\": \"yarn@4.0.2\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"uuid\": \"^9.0.0\",\n" +
                                "    \"lodash\": \"^4.17.21\"\n" +
                                "  }\n" +
                                "}\n",
                                spec -> spec.afterRecipe(d -> {
                                    if (d instanceof Json.Document) {
                                        capturedAfterMarker.set(((Json.Document) d).getMarkers()
                                                .findFirst(NodeResolutionResult.class).orElse(null));
                                    }
                                })),
                        text(lockFileContent, s -> s.path("yarn.lock")
                                .after(after -> after)
                                .noTrim())));

        NodeResolutionResult after = capturedAfterMarker.get();
        assertThat(after).as("modified package.json must have a marker").isNotNull();

        NodeResolutionResult.ResolvedDependency lodash = after.getResolvedDependency("lodash");
        assertThat(lodash).as("lodash should be resolved post-edit").isNotNull();
        assertThat(lodash.getVersion()).matches("4\\.\\d+\\.\\d+");

        assertThat(after.getDependencies())
                .filteredOn(d -> "lodash".equals(d.getName()))
                .singleElement()
                .satisfies(d -> assertThat(d.getResolved()).isNotNull());
    }

    @Test
    void addDependencyOverlaysResolvedVersionPnpm(@TempDir Path tempDir) throws IOException {
        Assumptions.assumeTrue(PackageManagerExecutor.PNPM.find() != null,
                "pnpm not installed");
        String packageJsonContent = "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"packageManager\": \"pnpm@8.15.4\",\n" +
                "  \"dependencies\": {\n" +
                "    \"uuid\": \"^9.0.0\"\n" +
                "  }\n" +
                "}\n";
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(packageJsonContent, PackageManager.Pnpm);
        String lockFileContent = Files.readString(workspace.resolve("pnpm-lock.yaml"));

        AtomicReference<NodeResolutionResult> capturedAfterMarker = new AtomicReference<>();

        rewriteRun(
                spec -> spec.recipe(new AddDependency("lodash", "^4.17.21", "dependencies")),
                pnpm(tempDir,
                        packageJson(
                                packageJsonContent,
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"packageManager\": \"pnpm@8.15.4\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"uuid\": \"^9.0.0\",\n" +
                                "    \"lodash\": \"^4.17.21\"\n" +
                                "  }\n" +
                                "}\n",
                                spec -> spec.afterRecipe(d -> {
                                    if (d instanceof Json.Document) {
                                        capturedAfterMarker.set(((Json.Document) d).getMarkers()
                                                .findFirst(NodeResolutionResult.class).orElse(null));
                                    }
                                })),
                        text(lockFileContent, s -> s.path("pnpm-lock.yaml")
                                .after(after -> after)
                                .noTrim())));

        NodeResolutionResult after = capturedAfterMarker.get();
        assertThat(after).as("modified package.json must have a marker").isNotNull();

        NodeResolutionResult.ResolvedDependency lodash = after.getResolvedDependency("lodash");
        assertThat(lodash).as("lodash should be resolved post-edit").isNotNull();
        assertThat(lodash.getVersion()).matches("4\\.\\d+\\.\\d+");

        assertThat(after.getDependencies())
                .filteredOn(d -> "lodash".equals(d.getName()))
                .singleElement()
                .satisfies(d -> assertThat(d.getResolved()).isNotNull());
    }
}

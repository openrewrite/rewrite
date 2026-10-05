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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.javascript.internal.PackageManagerExecutor;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.npm;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.test.SourceSpecs.text;

/**
 * PM-gated parity cross-check: exercises the recipe against a real {@code npm} workspace. Native
 * PM-free regeneration is covered by {@code UpgradeDependencyVersionLockRegenTest} in {@code src/test};
 * this suite skips when npm is not on the PATH.
 */
class UpgradeDependencyVersionTest implements RewriteTest {

    @BeforeEach
    void requirePackageManager() {
        Assumptions.assumeTrue(PackageManagerExecutor.NPM.find() != null, "npm not installed");
    }

    @Test
    void upgradesExactMatch(@TempDir Path tempDir) {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("lodash", null, "^4.17.21")),
                npm(tempDir,
                        packageJson(
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"lodash\": \"^4.17.20\"\n" +
                                "  }\n" +
                                "}\n",
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"lodash\": \"^4.17.21\"\n" +
                                "  }\n" +
                                "}\n")));
    }

    @Test
    void noOpWhenAlreadyAtTargetVersion(@TempDir Path tempDir) {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("lodash", null, "^4.17.21")),
                npm(tempDir,
                        packageJson(
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"lodash\": \"^4.17.21\"\n" +
                                "  }\n" +
                                "}\n")));
    }

    /**
     * A chained glob upgrade (as in {@code UpgradeToAngular21}) sets an invalid {@code 8.x} constraint on
     * {@code @angular/build} before a later step sets a valid {@code 21.x}.
     */
    @Test
    void chainedGlobUpgradeSelfHealsAfterAnIntermediateInvalidVersion(@TempDir Path tempDir) {
        @Language("json")
        String packageJson = "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"devDependencies\": {\n" +
                "    \"@angular/build\": \"^20.0.0\"\n" +
                "  }\n" +
                "}\n";

        // Materialize the workspace so we can seed each recipe's scanner with a real lock file; without a
        // captured lock the recipe skips regeneration entirely and neither the bug nor the fix surfaces.
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(packageJson);
        String lockFileContent;
        try {
            lockFileContent = new String(Files.readAllBytes(workspace.resolve("package-lock.json")));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read package-lock.json from workspace", e);
        }

        AtomicReference<Json.Document> capturedAfter = new AtomicReference<>();

        rewriteRun(
                spec -> spec.recipes(
                                new UpgradeDependencyVersion(null, "@angular/*", "8.x"),
                                new UpgradeDependencyVersion(null, "@angular/*", "21.x"))
                        .cycles(1)
                        .expectedCyclesThatMakeChanges(1),
                npm(tempDir,
                        packageJson(
                                packageJson,
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"devDependencies\": {\n" +
                                "    \"@angular/build\": \"21.x\"\n" +
                                "  }\n" +
                                "}\n",
                                s -> s.afterRecipe(capturedAfter::set)),
                        text(lockFileContent, s -> s.path("package-lock.json").after(actual -> actual).noTrim())));

        Json.Document after = capturedAfter.get();
        assertThat(after).as("package.json should have been visited").isNotNull();

        // The intermediate 8.x failure left no warning behind (the fix): package.json is clean.
        assertThat(after.getMarkers().findFirst(Markup.Warn.class))
                .as("no lock-regeneration warning should survive")
                .isEmpty();

        // @angular/build resolves to a real 21.x release, proving the final step's regeneration succeeded.
        NodeResolutionResult marker = after.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getResolvedDependency("@angular/build"))
                .as("@angular/build should resolve to a real 21.x release after regeneration")
                .isNotNull()
                .satisfies(d -> assertThat(d.getVersion()).startsWith("21."));
    }
}

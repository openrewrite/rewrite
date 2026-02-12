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
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.python.Assertions.pyproject;
import static org.openrewrite.python.Assertions.uv;

class UpgradeTransitiveDependencyVersionTest implements RewriteTest {

    @Test
    void addConstraintForTransitiveDep(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("certifi", ">=2024.1.1")),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]

                [tool.uv]
                constraint-dependencies = []
                """,
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]

                [tool.uv]
                constraint-dependencies = ["certifi>=2024.1.1"]
                """
            )
          )
        );
    }

    @Test
    void upgradeExistingConstraint(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("certifi", ">=2024.1.1")),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]

                [tool.uv]
                constraint-dependencies = [
                    "certifi>=2023.1.1",
                ]
                """,
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]

                [tool.uv]
                constraint-dependencies = [
                    "certifi>=2024.1.1",
                ]
                """
            )
          )
        );
    }

    @Test
    void skipWhenDirectDependency(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("requests", ">=2.31.0")),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]

                [tool.uv]
                constraint-dependencies = []
                """
            )
          )
        );
    }

    @Test
    void skipWhenNotInResolvedTree() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("nonexistent", ">=1.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.uv]
              constraint-dependencies = []
              """
          )
        );
    }

    @Test
    void skipWhenConstraintAlreadyMatches(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("certifi", ">=2023.1.1")),
          uv(tempDir,
            pyproject(
              """
                [project]
                name = "myapp"
                version = "1.0.0"
                dependencies = [
                    "requests>=2.28.0",
                ]

                [tool.uv]
                constraint-dependencies = [
                    "certifi>=2023.1.1",
                ]
                """
            )
          )
        );
    }

    @Test
    void addToConstraintDependencies() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency("certifi", ">=2024.1.1", "tool.uv.constraint-dependencies", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [tool.uv]
              constraint-dependencies = [
                  "urllib3>=2.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [tool.uv]
              constraint-dependencies = [
                  "urllib3>=2.0",
                  "certifi>=2024.1.1",
              ]
              """
          )
        );
    }

    @Test
    void removeFromOverrideDependencies() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("urllib3", "tool.uv.override-dependencies", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [tool.uv]
              override-dependencies = [
                  "urllib3>=2.0",
                  "certifi>=2024.1.1",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [tool.uv]
              override-dependencies = [
                  "certifi>=2024.1.1",
              ]
              """
          )
        );
    }

    @Test
    void upgradeVersionInConstraintDependencies() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("certifi", ">=2024.1.1", "tool.uv.constraint-dependencies", null)),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [tool.uv]
              constraint-dependencies = [
                  "certifi>=2023.1.1",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = ["requests>=2.28.0"]

              [tool.uv]
              constraint-dependencies = [
                  "certifi>=2024.1.1",
              ]
              """
          )
        );
    }

    // --- PDM tests ---

    @Test
    void addPdmOverrideForTransitiveDependency() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("certifi", ">=2024.1.1")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.pdm.overrides]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.pdm.overrides]
              certifi = ">=2024.1.1"
              """
          )
        );
    }

    @Test
    void addPdmOverrideToExistingEntries() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("certifi", ">=2024.1.1")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.pdm.overrides]
              urllib3 = ">=2.0"
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.pdm.overrides]
              urllib3 = ">=2.0"
              certifi = ">=2024.1.1"
              """
          )
        );
    }

    @Test
    void upgradePdmOverride() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("certifi", ">=2024.1.1")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.pdm.overrides]
              certifi = ">=2023.1.1"
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.pdm.overrides]
              certifi = ">=2024.1.1"
              """
          )
        );
    }

    @Test
    void skipPdmWhenDirectDependency() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("requests", ">=2.31.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.pdm.overrides]
              """
          )
        );
    }

    @Test
    void skipPdmWhenOverrideAlreadyMatches() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("certifi", ">=2024.1.1")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]

              [tool.pdm.overrides]
              certifi = ">=2024.1.1"
              """
          )
        );
    }

    // --- Fallback tests (unknown package manager) ---

    @Test
    void addDirectDependencyForUnknownPackageManager() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("certifi", ">=2024.1.1")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
                  "certifi>=2024.1.1",
              ]
              """
          )
        );
    }

    @Test
    void skipFallbackWhenDirectDependency() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("requests", ">=2.31.0")),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "requests>=2.28.0",
              ]
              """
          )
        );
    }
}

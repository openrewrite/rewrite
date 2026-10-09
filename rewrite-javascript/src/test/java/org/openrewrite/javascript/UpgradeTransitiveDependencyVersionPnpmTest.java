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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * The pnpm dialect nests its overrides under {@code pnpm.overrides}; the entry is edited in place and left
 * untouched once it holds the requested value, so the recipe settles in one cycle.
 */
class UpgradeTransitiveDependencyVersionPnpmTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", null));
    }

    @Test
    void pnpmOverrideConvergesInOneCycle() {
        rewriteRun(
                packageJson(
                        """
                        {
                          "name": "consumer",
                          "version": "1.0.0",
                          "dependencies": {
                            "acme-logger": "~1.4.1"
                          }
                        }
                        """,
                        """
                        {
                          "name": "consumer",
                          "version": "1.0.0",
                          "dependencies": {
                            "acme-logger": "~1.4.1"
                          },
                          "pnpm": {
                            "overrides": {
                              "acme-transitive": "~2.0.0"
                            }
                          }
                        }
                        """,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")))
        );
    }

    @Test
    void anEmptyPnpmObjectGainsTheOverrides() {
        rewriteRun(
                packageJson(
                        """
                        {
                          "name": "consumer",
                          "dependencies": {
                            "acme-logger": "~1.4.1"
                          },
                          "pnpm": {}
                        }
                        """,
                        """
                        {
                          "name": "consumer",
                          "dependencies": {
                            "acme-logger": "~1.4.1"
                          },
                          "pnpm": {
                            "overrides": {
                              "acme-transitive": "~2.0.0"
                            }
                          }
                        }
                        """,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")))
        );
    }
}

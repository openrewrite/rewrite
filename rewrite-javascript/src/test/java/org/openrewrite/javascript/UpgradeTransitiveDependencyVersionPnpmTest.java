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
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
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

    /**
     * The key the dialect writes into exists but holds something other than an object, so there is
     * nowhere to write. Appending would leave the manifest with two members of that name.
     */
    @Test
    void aNonObjectPnpmKeyIsDeclinedAndMarked() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1),
                packageJson(
                        """
                        {
                          "name": "consumer",
                          "dependencies": {
                            "acme-logger": "~1.4.1"
                          },
                          "pnpm": "hoist"
                        }
                        """,
                        null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the declaration is left exactly as it was")
                                    .contains("\"pnpm\": \"hoist\"")
                                    .doesNotContain("\"acme-transitive\":");
                            assertThat(actual.indexOf("\"pnpm\""))
                                    .as("exactly one `pnpm` member survives")
                                    .isEqualTo(actual.lastIndexOf("\"pnpm\""));
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the decline is marked on the manifest")
                                .hasValueSatisfying(warn -> assertThat(warn.getMessage())
                                        .contains("pnpm")
                                        .contains("not an object"))))
        );
    }

    @Test
    void aNonObjectOverridesKeyIsDeclinedAndMarked() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(1),
                packageJson(
                        """
                        {
                          "name": "consumer",
                          "dependencies": {
                            "acme-logger": "~1.4.1"
                          },
                          "overrides": []
                        }
                        """,
                        null,
                        nodeResolutionResult(PackageManager.Npm, dependency("acme-logger", "~1.4.1")),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the declaration is left exactly as it was")
                                    .contains("\"overrides\": []")
                                    .doesNotContain("\"acme-transitive\":");
                            assertThat(actual.indexOf("\"overrides\""))
                                    .as("exactly one `overrides` member survives")
                                    .isEqualTo(actual.lastIndexOf("\"overrides\""));
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the decline is marked on the manifest")
                                .hasValueSatisfying(warn -> assertThat(warn.getMessage())
                                        .contains("overrides")
                                        .contains("not an object"))))
        );
    }

    @Test
    void aReferenceInALaterScopeStillBlocksTheOverride() {
        rewriteRun(
                packageJson(
                        """
                        {
                          "name": "consumer",
                          "peerDependencies": {
                            "acme-transitive": "^1.0.0"
                          },
                          "devDependencies": {
                            "acme-transitive": "catalog:"
                          }
                        }
                        """,
                        null,
                        nodeResolutionResult(PackageManager.Pnpm))
        );
    }
}

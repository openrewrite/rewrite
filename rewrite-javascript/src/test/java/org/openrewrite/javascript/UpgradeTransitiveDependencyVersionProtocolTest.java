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
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.pnpmLock;

/**
 * An override is a global instruction: it wins over whatever constraint the dependency graph resolves.
 * Where the manifest holds a specifier protocol rather than a version, the constraint lives behind that
 * reference, and an override written beside it would silently win over something nobody named. So the
 * override is declined, and the decline is reported rather than left silent.
 * <p>
 * A scoped override is different: {@code foo>acme-transitive} pins the copy under {@code foo} and never
 * reaches the reference-held constraint of the direct declaration, so it is written as usual.
 */
class UpgradeTransitiveDependencyVersionProtocolTest implements RewriteTest {

    private static final String REFERENCE_IN_A_LATER_SCOPE = """
            {
              "name": "consumer",
              "peerDependencies": {
                "acme-transitive": "^1.0.0"
              },
              "devDependencies": {
                "acme-transitive": "catalog:"
              }
            }
            """;

    @Test
    void aReferenceInALaterScopeStillBlocksTheOverride() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", null))
                        .expectedCyclesThatMakeChanges(1)
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("sourcePath", "declaredIn", "protocol", "currentValue")
                                        .containsExactly(tuple("package.json", "package.json", "catalog:", "catalog:"))),
                packageJson(REFERENCE_IN_A_LATER_SCOPE, null,
                        nodeResolutionResult(PackageManager.Pnpm),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the declaration is left exactly as it was")
                                    .contains("\"acme-transitive\": \"catalog:\"")
                                    .doesNotContain("\"pnpm\"");
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the decline is marked on the manifest")
                                .hasValueSatisfying(warn -> assertThat(warn.getMessage())
                                        .contains("acme-transitive")
                                        .contains("catalog:")
                                        .contains("silently win"))))
        );
    }

    @Test
    void aScopedOverrideIsWrittenBesideAReference() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", "foo"))
                        .afterRecipe(ChangeDependencyProtocolTest::assertNoSkipRows),
                packageJson(REFERENCE_IN_A_LATER_SCOPE,
                        """
                        {
                          "name": "consumer",
                          "peerDependencies": {
                            "acme-transitive": "^1.0.0"
                          },
                          "devDependencies": {
                            "acme-transitive": "catalog:"
                          },
                          "pnpm": {
                            "overrides": {
                              "foo>acme-transitive": "~2.0.0"
                            }
                          }
                        }
                        """,
                        nodeResolutionResult(PackageManager.Pnpm),
                        s -> s.afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("a scoped override is not a decline, so nothing is marked")
                                .isEmpty()))
        );
    }

    /**
     * The guard reads the document, not the marker. A marker is free to report a resolved version where
     * the manifest holds a reference, and an override written on that basis would win over a constraint
     * nobody named.
     */
    @Test
    void aResolvedMarkerDoesNotUnblockTheGuard() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", null))
                        .expectedCyclesThatMakeChanges(1),
                packageJson(
                        """
                        {
                          "name": "consumer",
                          "dependencies": {
                            "acme-transitive": "catalog:"
                          }
                        }
                        """,
                        null,
                        // The marker claims a resolved range the manifest does not hold.
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-transitive", "~1.4.1")),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the override is still declined")
                                    .contains("\"acme-transitive\": \"catalog:\"")
                                    .doesNotContain("\"pnpm\"");
                            return actual;
                        }))
        );
    }

    /**
     * Nothing was declined: the override is already there and already correct. Re-running an upgrade
     * that has landed is the ordinary case in a monorepo where {@code catalog:} is the norm, and it must
     * not turn every such manifest into a changed file carrying a false warning.
     */
    @Test
    void anOverrideAlreadyAtTheRequestedVersionIsNotADecline() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", null))
                        .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyProtocolsSkipped.class))
                                .as("nothing needed doing, which is not the same as being declined")
                                .isEmpty()),
                packageJson(
                        """
                        {
                          "name": "consumer",
                          "dependencies": {
                            "acme-transitive": "catalog:"
                          },
                          "pnpm": {
                            "overrides": {
                              "acme-transitive": "~2.0.0"
                            }
                          }
                        }
                        """,
                        null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-transitive", "catalog:")))
        );
    }

    /**
     * {@code PackageJsonOverrides.parsePath} can return no segments for a non-null path, which
     * {@code upgradeTransitive} treats as a global override. The reporting has to agree, or the decline
     * is silent again.
     */
    @Test
    void aPathThatParsesToNoSegmentsIsStillReported() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", ">"))
                        .expectedCyclesThatMakeChanges(1)
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("packageName", "protocol")
                                        .containsExactly(tuple("acme-transitive", "catalog:"))),
                packageJson(REFERENCE_IN_A_LATER_SCOPE, null,
                        nodeResolutionResult(PackageManager.Pnpm),
                        s -> s.after(actual -> {
                            assertThat(actual).doesNotContain("\"pnpm\"");
                            return actual;
                        }))
        );
    }

    private static final String ROOT = """
            {
              "name": "root",
              "private": true
            }
            """;

    private static String member(String constraint) {
        return """
                {
                  "name": "member-a",
                  "dependencies": {
                    "acme-logger": "%s"
                  }
                }
                """.formatted(constraint);
    }

    /**
     * An override is honoured only in the root manifest and then applies to the whole graph, so a root
     * entry would silently win over the member's reference. Two rows, because two manifests would each
     * have received an override and each was declined.
     */
    @Test
    void aWorkspaceRootDoesNotOverrideAMemberReference() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
                        .expectedCyclesThatMakeChanges(1)
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("sourcePath", "declaredIn", "protocol")
                                        .containsExactlyInAnyOrder(
                                                tuple("package.json", "packages/a/package.json", "catalog:"),
                                                tuple("packages/a/package.json", "packages/a/package.json", "catalog:"))),
                packageJson(ROOT, null,
                        nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json")),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the root gains no override")
                                    .doesNotContain("\"pnpm\":");
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the root's decline names the member that declared the reference")
                                .hasValueSatisfying(warn -> assertThat(warn.getMessage())
                                        .contains("packages/a/package.json")))),
                packageJson(member("catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/a/package.json").after(actual -> {
                            assertThat(actual)
                                    .as("the member gains no override either")
                                    .contains("\"acme-logger\": \"catalog:\"")
                                    .doesNotContain("\"pnpm\":");
                            return actual;
                        }))
        );
    }

    @Test
    void anOverrideScopedToAMemberDoesNotOverrideItsReference() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", "member-a"))
                        .expectedCyclesThatMakeChanges(1)
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("sourcePath", "declaredIn", "protocol")
                                        .containsExactlyInAnyOrder(
                                                tuple("package.json", "packages/a/package.json", "catalog:"),
                                                tuple("packages/a/package.json", "packages/a/package.json", "catalog:"))),
                packageJson(ROOT, null,
                        nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json")),
                        s -> s.after(actual -> {
                            assertThat(actual).as("the root gains no override").doesNotContain("\"pnpm\":");
                            return actual;
                        })),
                packageJson(member("catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/a/package.json").after(actual -> {
                            assertThat(actual).as("the member gains no override either").doesNotContain("\"pnpm\":");
                            return actual;
                        }))
        );
    }

    @Test
    void aWorkspaceRootStillOverridesWhenNoMemberHoldsAReference() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
                        .afterRecipe(ChangeDependencyProtocolTest::assertNoSkipRows),
                packageJson(ROOT,
                        """
                        {
                          "name": "root",
                          "private": true,
                          "pnpm": {
                            "overrides": {
                              "acme-logger": "~2.0.0"
                            }
                          }
                        }
                        """,
                        nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json"))),
                packageJson(member("~1.4.1"),
                        """
                        {
                          "name": "member-a",
                          "dependencies": {
                            "acme-logger": "~1.4.1"
                          },
                          "pnpm": {
                            "overrides": {
                              "acme-logger": "~2.0.0"
                            }
                          }
                        }
                        """,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")),
                        s -> s.path("packages/a/package.json"))
        );
    }

    private static final String WORKSPACE_LOCK = """
            lockfileVersion: '9.0'

            importers:

              .: {}

              packages/a:
                dependencies:
                  acme-logger:
                    specifier: 'catalog:'
                    version: 1.4.1
            """;

    /**
     * The lock is regenerated from the manifests, so a decline that only covers the manifest visit would
     * still let the override reach the lock: the root's own document declares nothing, so there is no
     * backstop in {@code upgradeTransitive} the way there is for the same-manifest case. A lock carrying
     * an override the manifest says was never written is the worst outcome of the two.
     */
    @Test
    void aDeclinedWorkspaceRootDoesNotReachTheLockEither() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
                        .expectedCyclesThatMakeChanges(1)
                        .afterRecipe(run -> assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class))
                                .as("nothing was written, so no lock regeneration was attempted")
                                .isEmpty()),
                packageJson(ROOT, null,
                        nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json")),
                        s -> s.after(actual -> {
                            assertThat(actual).as("the root gains no override").doesNotContain("\"pnpm\":");
                            return actual;
                        })),
                packageJson(member("catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/a/package.json").after(actual -> {
                            assertThat(actual).as("the member gains none either").doesNotContain("\"pnpm\":");
                            return actual;
                        })),
                pnpmLock(WORKSPACE_LOCK, null)
        );
    }

    /**
     * Without the document read this is the case that writes a wrong override: the member's marker
     * claims a resolved range while its manifest holds a reference, and nothing in the root's own
     * document contradicts it.
     */
    @Test
    void aMemberWhoseMarkerHidesItsReferenceStillBlocksTheRoot() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
                        .expectedCyclesThatMakeChanges(1),
                packageJson(ROOT, null,
                        nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json")),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the root gains no override")
                                    .doesNotContain("\"pnpm\":");
                            return actual;
                        })),
                packageJson(member("catalog:"), null,
                        // The marker claims a resolved range the member's manifest does not hold.
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")),
                        s -> s.path("packages/a/package.json").after(actual -> {
                            assertThat(actual)
                                    .as("the member is left alone too")
                                    .contains("\"acme-logger\": \"catalog:\"")
                                    .doesNotContain("\"pnpm\":");
                            return actual;
                        }))
        );
    }
}

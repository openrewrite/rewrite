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
import org.openrewrite.SourceFile;
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
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * An override would win over a declaration of the package through a value that is no version range (a catalog or
 * workspace reference, an alias, a location), so the recipe declines it with a row and writes nothing.
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

    private static final String WORKSPACE = """
      packages:
        - packages/*
      """;

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

    @Test
    void aReferenceInALaterScopeStillBlocksTheOverride() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", null))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
              assertThat(rows).extracting("sourcePath", "declaredIn", "dependencyScope", "protocol", "currentValue")
                .containsExactly(tuple("package.json", "package.json", "devDependencies", "catalog:", "catalog:"))),
          packageJson(REFERENCE_IN_A_LATER_SCOPE, null, nodeResolutionResult(PackageManager.Pnpm),
            s -> s.afterRecipe(UpgradeTransitiveDependencyVersionProtocolTest::noWarning)),
          yaml(WORKSPACE, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aScopedOverrideIsWrittenBesideAReference() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", "foo"))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyProtocolsSkipped.class)).isEmpty()),
          packageJson(REFERENCE_IN_A_LATER_SCOPE, null, nodeResolutionResult(PackageManager.Pnpm),
            s -> s.afterRecipe(UpgradeTransitiveDependencyVersionProtocolTest::noWarning)),
          yaml(WORKSPACE, """
            packages:
              - packages/*
            overrides:
              foo>acme-transitive: ~2.0.0
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    @Test
    void theDocumentDecidesNotTheMarker() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", null))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
              assertThat(rows).extracting("declaredIn", "protocol").containsExactly(tuple("package.json", "catalog:"))),
          packageJson("""
            {
              "name": "consumer",
              "dependencies": {
                "acme-transitive": "catalog:"
              }
            }
            """, null, nodeResolutionResult(PackageManager.Pnpm, dependency("acme-transitive", "~1.4.1"))),
          yaml(WORKSPACE, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void anOverrideAlreadyAtTheRequestedVersionIsNotADecline() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", null))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyProtocolsSkipped.class)).isEmpty()),
          packageJson("""
            {
              "name": "consumer",
              "dependencies": {
                "acme-transitive": "catalog:"
              }
            }
            """, null, nodeResolutionResult(PackageManager.Pnpm, dependency("acme-transitive", "catalog:"))),
          yaml("""
            packages:
              - packages/*
            overrides:
              acme-transitive: ~2.0.0
            """, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aPathThatParsesToNoSegmentsIsAGlobalOverride() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", ">"))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
              assertThat(rows).extracting("packageName", "protocol").containsExactly(
                tuple("acme-transitive", "catalog:"))),
          packageJson(REFERENCE_IN_A_LATER_SCOPE, null, nodeResolutionResult(PackageManager.Pnpm)),
          yaml(WORKSPACE, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aWorkspaceRootDoesNotOverrideAMemberReference() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
              assertThat(rows).extracting("sourcePath", "declaredIn", "protocol")
                .containsExactly(tuple("package.json", "packages/a/package.json", "catalog:"))),
          packageJson(ROOT, null, nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json"))),
          packageJson(member("catalog:"), null,
            nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
            s -> s.path("packages/a/package.json")),
          yaml(WORKSPACE, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void anOverrideScopedToAMemberDoesNotOverrideItsReference() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", "member-a"))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
              assertThat(rows).extracting("sourcePath", "declaredIn", "protocol")
                .containsExactly(tuple("package.json", "packages/a/package.json", "catalog:"))),
          packageJson(ROOT, null, nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json"))),
          packageJson(member("catalog:"), null,
            nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
            s -> s.path("packages/a/package.json")),
          yaml(WORKSPACE, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void anOverrideScopedToAnotherParentIsWrittenBesideAMemberReference() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", "other"))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyProtocolsSkipped.class)).isEmpty()),
          packageJson(ROOT, null, nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json"))),
          packageJson(member("catalog:"), null,
            nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
            s -> s.path("packages/a/package.json")),
          yaml(WORKSPACE, """
            packages:
              - packages/*
            overrides:
              other>acme-logger: ~2.0.0
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    @Test
    void aWorkspaceRootStillOverridesWhenNoMemberHoldsAReference() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyProtocolsSkipped.class)).isEmpty()),
          packageJson(ROOT, null, nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json"))),
          packageJson(member("~1.4.1"), null,
            nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")),
            s -> s.path("packages/a/package.json")),
          yaml(WORKSPACE, """
            packages:
              - packages/*
            overrides:
              acme-logger: ~2.0.0
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    @Test
    void aDeclinedWorkspaceRootDoesNotReachTheLockEither() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class)).isEmpty()),
          packageJson(ROOT, null, nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json"))),
          packageJson(member("catalog:"), null,
            nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
            s -> s.path("packages/a/package.json")),
          yaml(WORKSPACE, s -> s.path("pnpm-workspace.yaml")),
          pnpmLock("""
            lockfileVersion: '9.0'

            importers:

              .: {}

              packages/a:
                dependencies:
                  acme-logger:
                    specifier: 'catalog:'
                    version: 1.4.1
            """, null)
        );
    }

    @Test
    void aMemberWhoseMarkerHidesItsReferenceStillBlocksTheRoot() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
              assertThat(rows).extracting("declaredIn").containsExactly("packages/a/package.json")),
          packageJson(ROOT, null, nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json"))),
          packageJson(member("catalog:"), null,
            nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")),
            s -> s.path("packages/a/package.json")),
          yaml(WORKSPACE, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aWorkspaceReferenceBlocksTheOverrideToo() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-logger", "~2.0.0", null))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
              assertThat(rows).extracting("protocol", "currentValue")
                .containsExactly(tuple("workspace:", "workspace:*"))),
          packageJson(ROOT, null, nodeResolutionResult(PackageManager.Pnpm, singletonList("packages/a/package.json"))),
          packageJson(member("workspace:*"), null, nodeResolutionResult(PackageManager.Pnpm),
            s -> s.path("packages/a/package.json")),
          yaml(WORKSPACE, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    private static void noWarning(SourceFile sf) {
        assertThat(sf.getMarkers().findFirst(Markup.Warn.class)).isEmpty();
    }
}

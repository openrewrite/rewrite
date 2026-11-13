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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * A removal in a pnpm workspace with catalogs, as {@code pnpm remove} wrote it in {@code lock/catalogs/pnpm}: the
 * catalog loses the entries no project references only under {@code cleanupUnusedCatalogs}.
 */
class RemoveDependencyCatalogTest implements RewriteTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
      catalog-remove-manual                | wrappy
      catalog-remove-manual-cleanup        | wrappy
      catalog-remove-prefer                | wrappy
      catalog-remove-prefer-cleanup        | wrappy
      catalog-remove-strict                | wrappy
      catalog-remove-strict-cleanup        | wrappy
      catalog-remove-named-cleanup         | is-number
      catalog-remove-last-cleanup          | wrappy
      catalog-remove-empties-scope-install | wrappy
      catalog-remove-cleanup-capitalized-true | wrappy
      catalog-remove-cleanup-quoted-false  | wrappy
      catalog-remove-cleanup-no            | wrappy
      catalog-cleanup-comments-middle-pnpm11 | wrappy
      catalog-cleanup-comments-first-pnpm11 | is-number
      catalog-cleanup-comments-last-pnpm11 | wrappy
      catalog-cleanup-override-reference-pnpm11 | ms
      """)
    void removedAsPnpmRemovedIt(String scenario, String name) {
        CatalogRecording recording = new CatalogRecording("pnpm", scenario);
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency(name, null)).executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new RemoveDependency(name, null));
    }

    @Test
    void aProjectTheWorkspaceDoesNotListKeepsNoEntry() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-remove-cleanup-unlisted-project")
          .fromTheGivenWorkspace();
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("wrappy", null)).executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new RemoveDependency("wrappy", null));
    }

    @Test
    void declinedWhenTheCleanupWouldRewriteAFlowStyleCatalog() {
        String member = """
          {
            "name": "a",
            "dependencies": {
              "wrappy": "catalog:"
            }
          }
          """;
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("wrappy", null)),
          packageJson(member, null, nodeResolutionResult(PackageManager.Pnpm), s -> s.path("packages/a/package.json")
            .noTrim().after(actual -> actual).afterRecipe(doc -> {
                assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                  .isEqualTo(member);
                assertThat(doc.getMarkers().findFirst(Markup.Warn.class)).hasValueSatisfying(w ->
                  assertThat(w.getMessage()).contains("flow-style catalogs"));
            })),
          yaml("""
            packages:
              - packages/*
            cleanupUnusedCatalogs: true
            catalog: {wrappy: 1.0.2}
            """, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    // the cleanup is pnpm's write of the whole file, not made where it holds what the yaml port does not read
    @Test
    void declinedWhenTheCleanupWouldRewriteALayoutNotWrittenAsPnpmWritesIt() {
        String member = """
          {
            "name": "a",
            "dependencies": {
              "wrappy": "catalog:"
            }
          }
          """;
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("wrappy", null)),
          packageJson(member, null, nodeResolutionResult(PackageManager.Pnpm), s -> s.path("packages/a/package.json")
            .noTrim().after(actual -> actual).afterRecipe(doc -> assertThat(doc.getMarkers()
              .findFirst(Markup.Warn.class)).hasValueSatisfying(w -> assertThat(w.getMessage())
              .contains("pnpm-workspace.yaml has an anchor or a tag")))),
          yaml("""
            packages:
              - &members packages/*
            cleanupUnusedCatalogs: true
            catalog:
              wrappy: 1.0.2
            """, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aCleanupForAReleaseNoRecordingCoversDeclinesTheEdit() {
        String member = """
          {
            "name": "a",
            "dependencies": {
              "ms": "2.1.3",
              "wrappy": "catalog:"
            }
          }
          """;
        String why = "cleanupUnusedCatalogs makes pnpm rewrite pnpm-workspace.yaml, and pnpm 10.0.0 is not a release " +
          "whose writes are recorded (" + org.openrewrite.javascript.internal.PnpmRelease.recorded() +
          "), so this edit was not made";
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("wrappy", null))
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .extracting(NodeDependencyEditsDeclined.Row::getReason).isEqualTo(why)),
          packageJson("""
            {
              "name": "root",
              "packageManager": "pnpm@10.0.0"
            }
            """, null, nodeResolutionResult(PackageManager.Pnpm)),
          packageJson(member, null, nodeResolutionResult(PackageManager.Pnpm), s -> s.path("packages/a/package.json")
            .noTrim().after(actual -> actual).afterRecipe(doc -> {
                assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                  .isEqualTo(member);
                assertThat(doc.getMarkers().findFirst(Markup.Warn.class)).hasValueSatisfying(w ->
                  assertThat(w.getMessage()).isEqualTo(why));
            })),
          yaml("""
            packages:
              - packages/*
            cleanupUnusedCatalogs: true
            catalog:
              wrappy: 1.0.2
            """, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    // pnpm 11.2.2 rejects an override that is no string before it cleans up (catalog-cleanup-override-number: "The
    // value of overrides.is-number should be a string, but got number"), so the workspace is left as it is
    @Test
    void noCleanupIsWrittenForAWorkspacePnpmRejects() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("wrappy", null))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyEditsDeclined.class)).isEmpty()),
          packageJson("""
            {
              "name": "a",
              "dependencies": {
                "ms": "2.1.3",
                "wrappy": "catalog:"
              }
            }
            """, """
            {
              "name": "a",
              "dependencies": {
                "ms": "2.1.3"
              }
            }
            """, nodeResolutionResult(PackageManager.Pnpm), s -> s.path("packages/a/package.json")),
          yaml("""
            packages:
              - packages/*
            cleanupUnusedCatalogs: true
            overrides:
              is-number: 7
            catalog:
              wrappy: 1.0.2
            """, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    // pnpm's cleanup counts a member's package.yaml, whose dependencies the recipes do not read
    @Test
    void declinedWhenTheCleanupCountsAMemberInAnotherManifestFormat() {
        String member = """
          {
            "name": "a",
            "dependencies": {
              "wrappy": "catalog:"
            }
          }
          """;
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("wrappy", null)),
          packageJson(member, null, nodeResolutionResult(PackageManager.Pnpm), s -> s.path("packages/a/package.json")
            .noTrim().after(actual -> actual).afterRecipe(doc -> {
                assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                  .isEqualTo(member);
                assertThat(doc.getMarkers().findFirst(Markup.Warn.class)).hasValueSatisfying(w ->
                  assertThat(w.getMessage()).contains("packages/b/package.yaml"));
            })),
          text("name: b\ndependencies:\n  ms: 'catalog:'\n", s -> s.path("packages/b/package.yaml")),
          yaml("""
            packages:
              - packages/*
            cleanupUnusedCatalogs: true
            catalog:
              ms: 2.1.3
              wrappy: 1.0.2
            """, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aRemovalAfterAnUpgradeOfTheCatalogInTheSameRun() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-compose-update-then-remove");
        rewriteRun(
          spec -> spec.recipes(new UpgradeDependencyVersion("is-number", null, "^7.0.0"),
              new RemoveDependency("wrappy", null))
            .executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new UpgradeDependencyVersion("is-number", null, "^7.0.0"),
          new RemoveDependency("wrappy", null));
    }

    @Test
    void aWorkspaceInAnotherLayoutIsCleanedAsPnpmWritesIt() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-layout-remove-cleanup");
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("wrappy", null)).executionContext(recording.ctx()),
          recording.sources()
        );
    }

    @Test
    void aDependencyAnEarlierRecipeAddedInTheSameCycle() {
        rewriteRun(
          spec -> spec.recipes(new AddDependency("left-pad", "^1.3.0", null),
              new AddDependency("right-pad", "^1.0.0", null), new RemoveDependency("left-pad", null))
            // the addition restores what the removal of the previous cycle took away
            .cycles(1).expectedCyclesThatMakeChanges(1),
          packageJson("""
            {
              "name": "consumer",
              "dependencies": {
                "is-number": "^7.0.0"
              }
            }
            """, """
            {
              "name": "consumer",
              "dependencies": {
                "is-number": "^7.0.0",
                "right-pad": "^1.0.0"
              }
            }
            """, nodeResolutionResult(PackageManager.Npm))
        );
    }
}

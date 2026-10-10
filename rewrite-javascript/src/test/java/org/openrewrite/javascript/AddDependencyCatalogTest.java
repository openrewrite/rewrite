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
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * In a pnpm workspace with catalogs the dependency is added as {@code pnpm add} added it in {@code lock/catalogs/pnpm},
 * under each {@code catalogMode} and {@code saveCatalogName}, with and without {@code cleanupUnusedCatalogs}.
 */
class AddDependencyCatalogTest implements RewriteTest {

    @ParameterizedTest(name = "{0}-{1}")
    @CsvSource(delimiter = '|', textBlock = """
      in-catalog     | manual         | wrappy    | 1.0.2
      in-catalog     | manual-cleanup | wrappy    | 1.0.2
      in-catalog     | prefer         | wrappy    | 1.0.2
      in-catalog     | prefer-cleanup | wrappy    | 1.0.2
      in-catalog     | strict         | wrappy    | 1.0.2
      in-catalog     | strict-cleanup | wrappy    | 1.0.2
      not-in-catalog | manual         | is-number | 7.0.0
      not-in-catalog | manual-cleanup | is-number | 7.0.0
      not-in-catalog | prefer         | is-number | 7.0.0
      not-in-catalog | prefer-cleanup | is-number | 7.0.0
      not-in-catalog | strict         | is-number | 7.0.0
      not-in-catalog | strict-cleanup | is-number | 7.0.0
      mismatch       | manual         | wrappy    | 1.0.1
      mismatch       | manual-cleanup | wrappy    | 1.0.1
      mismatch       | prefer         | wrappy    | 1.0.1
      mismatch       | prefer-cleanup | wrappy    | 1.0.1
      """)
    void addedAsPnpmAddedIt(String shape, String mode, String name, String version) {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-add-root-declares-" + shape + "-" + mode);
        rewriteRun(
          spec -> spec.recipe(new AddDependency(name, version, null)).executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new AddDependency(name, version, null));
    }

    // recorded with pnpm 11.2.2, which writes the member's dependencies sorted, whatever order they were in
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      catalog-add-beside-dependency                | is-number | 6.0.0
      catalog-add-beside-dependency-not-in-catalog | left-pad  | 1.3.0
      catalog-add-unsorted                         | is-number | 6.0.0
      """)
    void addedBesideOtherDependenciesInTheOrderPnpmWritesThem(String scenario, String name, String version) {
        CatalogRecording recording = new CatalogRecording("pnpm", scenario);
        rewriteRun(
          spec -> spec.recipe(new AddDependency(name, version, null)).executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new AddDependency(name, version, null));
    }

    // recorded with pnpm 9.15.9, whose add writes the version, and 10.34.6, which follows the default catalog as 11
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      catalog-add-pnpm9  | 9.15.9
      catalog-add-pnpm10 | 10.34.6
      """)
    void addedAsTheReleaseTheProjectNamesAddsIt(String scenario, String release) {
        CatalogRecording recording = new CatalogRecording("pnpm", scenario);
        rewriteRun(
          spec -> spec.recipe(new AddDependency("is-number", "6.0.0", null)).executionContext(recording.ctx()),
          recording.sourcesWhoseInstallFails("the lock pnpm " + release + " writes")
        );
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      catalog-add-save-name-manual               | wrappy    | 1.0.2
      catalog-add-save-name-prefer               | wrappy    | 1.0.2
      catalog-add-save-name-strict-in-catalog    | wrappy    | 1.0.2
      catalog-add-save-name-prefer-mismatch      | wrappy    | 1.0.2
      catalog-add-save-name-manual-mismatch      | wrappy    | 1.0.2
      catalog-add-save-name-default-holds        | wrappy    | 1.0.2
      catalog-add-save-name-default-differs      | wrappy    | 1.0.2
      catalog-add-save-name-prefer-default-holds | wrappy    | 1.0.2
      catalog-add-save-name-default              | wrappy    | 1.0.2
      catalog-add-save-name-null                 | wrappy    | 1.0.2
      catalog-add-mode-capitalized-mismatch      | wrappy    | 1.0.1
      catalog-add-mode-capitalized-not-in-catalog | is-number | 7.0.0
      catalog-add-mode-null-mismatch             | wrappy    | 1.0.1
      """)
    void savedWhereSaveCatalogNameAndCatalogModeSayAsPnpmAddedIt(String scenario, String name, String version) {
        CatalogRecording recording = new CatalogRecording("pnpm", scenario);
        rewriteRun(
          spec -> spec.recipe(new AddDependency(name, version, null)).executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new AddDependency(name, version, null));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      catalog-add-root-declares-mismatch-strict         | 1.0.1  | rejects
      catalog-add-root-declares-mismatch-strict-cleanup | 1.0.1  | rejects
      catalog-add-save-name-strict-mismatch             | 1.0.2  | rejects
      catalog-add-prefer-range                          | ^1.0.0 | fails
      """)
    void declinedWherePnpmAddStops(String scenario, String version, String how) {
        String detail = "rejects".equals(how) ? "pnpm rejects this edit: [ERR_PNPM_CATALOG_VERSION_MISMATCH] Wanted " +
          "dependency outside the version range defined in catalog" :
          "pnpm fails on this edit: TypeError: Invalid Version: ^1.0.0";
        CatalogRecording recording = new CatalogRecording("pnpm", scenario);
        rewriteRun(
          spec -> spec.recipe(new AddDependency("wrappy", version, null)).executionContext(recording.ctx())
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .extracting(NodeDependencyEditsDeclined.Row::getReason)
              .isEqualTo("wrappy@" + version + " was not added: " + detail))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class)).isEmpty()),
          recording.sources("packages/a/package.json", detail)
        );
    }

    // PnpmOptions.releaseMajor leaves a range open between majors to the registry, which pnpm 9 and 10 answer apart
    @Test
    void aReleaseRangeOpenBetweenMajorsIsDeclined() {
        String manifest = """
          {
            "name": "root",
            "devEngines": {
              "packageManager": {
                "name": "pnpm",
                "version": ">=9"
              }
            }
          }
          """;
        String why = "is-number@6.0.0 was not added: what pnpm add of the release the project names writes is " +
          "not known";
        rewriteRun(
          spec -> spec.recipe(new AddDependency("is-number", "6.0.0", null))
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .extracting(NodeDependencyEditsDeclined.Row::getReason).isEqualTo(why)),
          packageJson(manifest, null, nodeResolutionResult(PackageManager.Pnpm), s -> s.noTrim().after(actual -> actual)
            .afterRecipe(doc -> {
                assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                  .isEqualTo(manifest);
                assertThat(doc.getMarkers().findFirst(Markup.Warn.class)).hasValueSatisfying(w ->
                  assertThat(w.getMessage()).isEqualTo(why));
            })),
          yaml("""
            catalog:
              is-number: 6.0.0
            """, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void anEmptySaveCatalogNameIsWrittenAsPnpmAddWritesItAndItsInstallRejects() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-add-save-name-empty");
        rewriteRun(
          spec -> spec.recipe(new AddDependency("wrappy", "1.0.2", null)).executionContext(recording.ctx())
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).contains("ERR_PNPM_CATALOG_ENTRY_NOT_FOUND_FOR_SPEC"))),
          recording.sourcesWhoseInstallFails("ERR_PNPM_CATALOG_ENTRY_NOT_FOUND_FOR_SPEC")
        );
    }

    @Test
    void anAdditionAfterAnUpgradeOfTheCatalogInTheSameRun() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-compose-update-then-add");
        rewriteRun(
          spec -> spec.recipes(new UpgradeDependencyVersion("wrappy", null, "1.0.2"),
              new AddDependency("wrappy", "1.0.2", null))
            .executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new UpgradeDependencyVersion("wrappy", null, "1.0.2"),
          new AddDependency("wrappy", "1.0.2", null));
    }

    @Test
    void aWorkspaceInAnotherLayoutGetsTheEntryInPlace() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-layout-add-prefer");
        rewriteRun(
          spec -> spec.recipe(new AddDependency("wrappy", "1.0.2", null)).executionContext(recording.ctx()),
          recording.sourcesWithTheWorkspaceEditedInPlace("""
            # workspace
            packages:
            - packages/*

            catalogMode: prefer

            catalog:
                is-number: 7.0.0
                wrappy: 1.0.2
            """)
        );
    }

    @Test
    void anAdditionAnEarlierRecipeMadeRoomForInTheSameCycle() {
        rewriteRun(
          spec -> spec.recipes(new RemoveDependency("left-pad", null), new AddDependency("left-pad", "^2.0.0", null))
            .cycles(1).expectedCyclesThatMakeChanges(1),
          packageJson("""
            {
              "name": "consumer",
              "dependencies": {
                "left-pad": "^1.3.0"
              }
            }
            """, """
            {
              "name": "consumer",
              "dependencies": {
                "left-pad": "^2.0.0"
              }
            }
            """, nodeResolutionResult(PackageManager.Npm))
        );
    }

    @Test
    void aScopeThatIsNotAnObjectIsDeclinedAndMarked() {
        String manifest = """
          {
            "name": "consumer",
            "devDependencies": null
          }
          """;
        rewriteRun(
          spec -> spec.recipe(new AddDependency("acme-logger", "^1.0.0", "devDependencies")),
          packageJson(manifest, null, nodeResolutionResult(PackageManager.Npm), s -> s.noTrim().after(actual -> actual)
            .afterRecipe(doc -> {
                assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                  .isEqualTo(manifest);
                assertThat(doc.getMarkers().findFirst(Markup.Warn.class)).hasValueSatisfying(w ->
                  assertThat(w.getMessage()).isEqualTo("`devDependencies` is not an object, so acme-logger was not " +
                    "added"));
            }))
        );
    }
}

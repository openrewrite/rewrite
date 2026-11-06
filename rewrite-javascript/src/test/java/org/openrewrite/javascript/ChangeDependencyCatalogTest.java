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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * A rename of a {@code catalog:} reference keeps the reference and gives the catalog the new name's entry, as the
 * recordings under {@code lock/catalogs/} were installed; a value pointing elsewhere is declined, marked and recorded.
 */
class ChangeDependencyCatalogTest implements RewriteTest {

    @ParameterizedTest(name = "{0}/{1}")
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
      pnpm       | catalog-rename         | wrappy | 7.0.0
      pnpm       | catalog-rename-cleanup | wrappy | 7.0.0
      pnpm       | catalog-rename-named   | ms     | ^7.0.0
      bun        | catalog-rename         | ms     | 7.0.0
      yarn-berry | catalog-yarnrc-rename  | ms     | 7.0.0
      """)
    void catalogReferenceRenamed(String family, String scenario, String from, @Nullable String version) {
        CatalogRecording recording = new CatalogRecording(family, scenario);
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency(from, "is-number", version, null))
            .executionContext(recording.ctx())
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyProtocolsSkipped.class)).isEmpty()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new ChangeDependency(from, "is-number", version, null));
    }

    private static String manifest(String value) {
        return """
          {
            "name": "consumer",
            "version": "1.0.0",
            "dependencies": {
              "acme-logger": "%s"
            }
          }
          """.formatted(value);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", delimiter = '|', textBlock = """
      workspace:^                       | workspace: | null   | rename it where the value points first
      workspace:^                       | workspace: | ^2.0.0 | rename it where the value points first
      patch:acme-logger@1.4.1#fix.patch | patch:     | ^2.0.0 | rename it where the value points first
      patch:acme-logger@1.4.1#fix.patch | patch:     | null   | rename it where the value points first
      npm:@acme/other@^1.0.0            | npm:       | null   | rename it where the value points first
      npm:@acme/other@^1.0.0            | npm:       | ^2.0.0 | rename it where the value points first
      jsr:@acme/logger@^1.0.0           | jsr:       | ^2.0.0 | rename it where the value points first
      future:x                          | future:    | ^2.0.0 | rename it where the value points first
      catalog:                          | catalog:   | null   | its catalog holds no entry for it
      github:me/old-fork                | github:    | null   | pass a newVersion to replace it with a registry version
      """)
    void declinedAndMarkedUntilItSettles(String value, String protocol, @Nullable String newVersion, String remedy) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("acme-logger", "acme-log", newVersion, null))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getProtocol()).isEqualTo(protocol);
                assertThat(rows.get(0).getCurrentValue()).isEqualTo(value);
                assertThat(rows.get(0).getRequestedVersion()).isEqualTo(newVersion == null ? "" : newVersion);
            }),
          packageJson(manifest(value), null, nodeResolutionResult(PackageManager.Pnpm), s -> s.noTrim()
            .after(actual -> actual).afterRecipe(doc -> {
                assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                  .isEqualTo(manifest(value));
                assertThat(doc.getMarkers().getMarkers()).filteredOn(Markup.Warn.class::isInstance).singleElement()
                  .satisfies(w -> assertThat(((Markup.Warn) w).getMessage()).contains("acme-logger is declared as " +
                    value + " (" + protocol + ")").contains("left unchanged: " + remedy).doesNotContain("Exception"));
            }))
        );
    }

    @Test
    void aRenameInACatalogOfAReleaseNoRecordingCoversIsDeclinedAndMarked() {
        String manifest = """
          {
            "name": "consumer",
            "packageManager": "pnpm@10.0.0",
            "dependencies": {
              "acme-logger": "catalog:"
            }
          }
          """;
        String why = "acme-logger was not renamed: pnpm 10.0.0 is not a release whose writes are recorded (" +
          org.openrewrite.javascript.internal.PnpmRelease.recorded() + ")";
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("acme-logger", "acme-log", null, null))
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .extracting(NodeDependencyEditsDeclined.Row::getReason).isEqualTo(why)),
          packageJson(manifest, null, nodeResolutionResult(PackageManager.Pnpm), s -> s.noTrim()
            .after(actual -> actual).afterRecipe(doc -> {
                assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                  .isEqualTo(manifest);
                assertThat(doc.getMarkers().findFirst(Markup.Warn.class)).hasValueSatisfying(w ->
                  assertThat(w.getMessage()).isEqualTo(why));
            })),
          yaml("""
            catalog:
              acme-logger: ~1.4.1
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    @Test
    void aLocationIsMigratedWhenANewVersionIsGiven() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("acme-logger", "acme-log", "^2.0.0", null))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyProtocolsSkipped.class)).isEmpty()),
          packageJson(manifest("github:me/old-fork"), manifest("^2.0.0").replace("acme-logger", "acme-log"),
            nodeResolutionResult(PackageManager.Pnpm))
        );
    }

    @Test
    void anEntryTheNewNameAlreadyHasIsKept() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("wrappy", "is-number", null, null)),
          packageJson("""
            {
              "name": "a",
              "dependencies": {
                "wrappy": "catalog:"
              }
            }
            """, """
            {
              "name": "a",
              "dependencies": {
                "is-number": "catalog:"
              }
            }
            """, nodeResolutionResult(PackageManager.Pnpm), s -> s.path("packages/a/package.json")),
          packageJson("""
            {
              "name": "b",
              "dependencies": {
                "is-number": "catalog:"
              }
            }
            """, null, nodeResolutionResult(PackageManager.Pnpm), s -> s.path("packages/b/package.json")),
          yaml("""
            packages:
              - packages/*
            catalog:
              is-number: 7.0.0
              wrappy: 1.0.2
            """, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aRenameInAMemberAndAnUpgradeAtTheRootInOneRun() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-compose-rename-member-upgrade-root");
        rewriteRun(
          spec -> spec.recipes(new ChangeDependency("wrappy", "is-number", "7.0.0", null),
              new UpgradeDependencyVersion("ms", null, "2.1.3"))
            .executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new ChangeDependency("wrappy", "is-number", "7.0.0", null),
          new UpgradeDependencyVersion("ms", null, "2.1.3"));
        recording.assertEveryVisitOrder(new UpgradeDependencyVersion("ms", null, "2.1.3"),
          new ChangeDependency("wrappy", "is-number", "7.0.0", null));
    }

    @Test
    void anUpgradeOfTheEntryAnEarlierRenameAdded() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-compose-rename-then-upgrade-above");
        rewriteRun(
          spec -> spec.recipes(new ChangeDependency("wrappy", "is-number", "6.0.0", null),
              new UpgradeDependencyVersion("is-number", null, "7.0.0"))
            .executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new ChangeDependency("wrappy", "is-number", "6.0.0", null),
          new UpgradeDependencyVersion("is-number", null, "7.0.0"));
    }

    @Test
    void aDependencyAnEarlierRecipeAddedInTheSameCycle() {
        rewriteRun(
          spec -> spec.recipes(new AddDependency("left-pad", "^1.3.0", null),
              new ChangeDependency("left-pad", "pad-left", "^2.0.0", null))
            // the addition restores what the change of the previous cycle renamed away
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
                "pad-left": "^2.0.0"
              }
            }
            """, nodeResolutionResult(PackageManager.Npm))
        );
    }

    @Test
    void anOrdinaryConstraintIsReplaced() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("acme-logger", "acme-log", "^2.0.0", null)),
          packageJson(manifest("~1.4.1"), manifest("^2.0.0").replace("acme-logger", "acme-log"),
            nodeResolutionResult(PackageManager.Pnpm))
        );
    }
}

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
import org.openrewrite.Recipe;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * A {@code catalog:} reference is upgraded in its catalog entry, and the lock follows as the package manager wrote it
 * in the recordings under {@code lock/catalogs/}; a value that is no version range is left alone and recorded.
 */
class UpgradeDependencyVersionCatalogTest implements RewriteTest {

    @ParameterizedTest(name = "{0}/{1}")
    @CsvSource(delimiter = '|', textBlock = """
      pnpm       | catalog-entry-one-importer                     | wrappy    | 1.0.2
      pnpm       | catalog-entry-two-importers                    | wrappy    | 1.0.2
      pnpm       | catalog-entry-named                            | is-number | ^7.0.0
      pnpm       | catalog-entry-default-spelling                 | wrappy    | 1.0.2
      pnpm       | catalog-update-shared-latest-quoted            | wrappy    | 1.0.2
      pnpm       | catalog-update-shared-latest-in-member-install | wrappy    | 1.0.2
      bun        | catalog-workspaces-catalog-edit                | ms        | 2.1.3
      bun        | catalog-workspaces-catalogs-named-edit         | is-number | ^7.0.0
      bun        | catalog-top-level-fallback-edit                | ms        | 2.1.3
      bun        | catalog-top-level-named-fallback-edit          | is-number | ^7.0.0
      bun        | catalog-workspaces-object-top-level-edit       | ms        | 2.1.3
      bun        | catalog-both-places-edit-workspaces            | ms        | 2.1.2
      yarn-berry | catalog-yarnrc-default-edit                    | ms        | 2.1.3
      yarn-berry | catalog-yarnrc-named-edit                      | is-number | ^7.0.0
      """)
    void catalogEntryUpgradedAsThePackageManagerWroteIt(String family, String scenario, String name, String version) {
        CatalogRecording recording = new CatalogRecording(family, scenario);
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion(name, null, version)).executionContext(recording.ctx())
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeDependencyProtocolsSkipped.class)).isEmpty()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new UpgradeDependencyVersion(name, null, version));
    }

    @ParameterizedTest(name = "{0}/{1}")
    @CsvSource(delimiter = '|', textBlock = """
      pnpm       | catalog-entry-quoted                        | is-number | >=7.0.0 | wrappy | 1.0.2
      yarn-berry | catalog-yarnrc-quoted-edit                  | is-number | >=7.0.0 | ms     | 2.1.3
      bun        | catalog-workspaces-catalog-two-entries-edit | is-number | ^7.0.0  | ms     | 2.1.3
      """)
    void twoUpgradesInOneRunEachReadTheCatalogAsTheOtherLeftIt(String family, String scenario, String first,
                                                               String firstVersion, String second,
                                                               String secondVersion) {
        CatalogRecording recording = new CatalogRecording(family, scenario);
        rewriteRun(
          spec -> spec.recipes(new UpgradeDependencyVersion(first, null, firstVersion),
              new UpgradeDependencyVersion(second, null, secondVersion))
            .executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new UpgradeDependencyVersion(first, null, firstVersion),
          new UpgradeDependencyVersion(second, null, secondVersion));
    }

    @ParameterizedTest(name = "{0}/{1}: {2}, then {4}")
    @CsvSource(delimiter = '|', textBlock = """
      bun        | catalog-compose-entry-then-member     | ms        | 2.1.3  | is-number | ^7.0.0
      bun        | catalog-compose-entry-then-member     | is-number | ^7.0.0 | ms        | 2.1.3
      yarn-berry | catalog-compose-entry-then-member     | ms        | 2.1.3  | is-number | ^7.0.0
      yarn-berry | catalog-compose-entry-then-member     | is-number | ^7.0.0 | ms        | 2.1.3
      pnpm       | catalog-compose-plain-member-and-root | is-number | ^7.0.0 | wrappy    | 1.0.2
      pnpm       | catalog-compose-plain-member-and-root | wrappy    | 1.0.2  | is-number | ^7.0.0
      """)
    void twoUpgradesInOneRunEachRegenerateWithTheOthersManifest(String family, String scenario, String first,
                                                                String firstVersion, String second,
                                                                String secondVersion) {
        CatalogRecording recording = new CatalogRecording(family, scenario);
        rewriteRun(
          spec -> spec.recipes(new UpgradeDependencyVersion(first, null, firstVersion),
              new UpgradeDependencyVersion(second, null, secondVersion))
            .executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new UpgradeDependencyVersion(first, null, firstVersion),
          new UpgradeDependencyVersion(second, null, secondVersion));
    }

    @Test
    void anEntryAnEarlierRecipeAddedInTheSameRun() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-compose-add-then-update");
        rewriteRun(
          spec -> spec.recipes(new AddDependency("is-number", "6.0.0", null),
              new UpgradeDependencyVersion("is-number", null, "7.0.0"))
            .executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new AddDependency("is-number", "6.0.0", null),
          new UpgradeDependencyVersion("is-number", null, "7.0.0"));
    }

    @Test
    void aWorkspaceInAnotherLayoutIsWrittenAsPnpmWritesIt() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-layout-update-latest");
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("wrappy", null, "1.0.2")).executionContext(recording.ctx()),
          recording.sources()
        );
    }

    @Test
    void valuesThatAreNoVersionRangeAreLeftAloneAndRecorded() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion(null, "acme-*", "^2.0.0"))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> assertThat(rows).extracting(
                NodeDependencyProtocolsSkipped.Row::getPackageName, NodeDependencyProtocolsSkipped.Row::getProtocol)
              .containsExactly(
                tuple("acme-alias", "npm:"),
                tuple("acme-fork", "github:"),
                tuple("acme-git-range", "github:"),
                tuple("acme-invalid", ""),
                tuple("acme-link", "link:"),
                tuple("acme-member", "workspace:"),
                tuple("acme-missing", "catalog:"),
                tuple("acme-tarball", "https:"))),
          packageJson("""
            {
              "name": "consumer",
              "dependencies": {
                "acme-alias": "npm:@acme/other@^1.0.0",
                "acme-current": "catalog:",
                "acme-fork": "acme/fork#v1",
                "acme-git-range": "github:acme/x#semver:^1.0.0",
                "acme-invalid": "foo\\\\bar",
                "acme-link": "link:../acme",
                "acme-member": "workspace:^",
                "acme-missing": "catalog:",
                "acme-tarball": "https://example.com/acme.tgz"
              }
            }
            """, null, nodeResolutionResult(PackageManager.Pnpm, dependency("acme-current", "^1.0.0"))),
          yaml("""
            packages:
              - packages/*
            catalog:
              acme-current: ^2.0.0
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    @Test
    void anEntryThatIsNoVersionRangeIsRecordedNotRewritten() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion(null, "acme-*", "^2.0.0"))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> assertThat(rows)
              .extracting(NodeDependencyProtocolsSkipped.Row::getPackageName)
              .containsExactly("acme-alias", "acme-folded")),
          packageJson("""
            {
              "name": "consumer",
              "dependencies": {
                "acme-alias": "catalog:",
                "acme-folded": "catalog:"
              }
            }
            """, null, nodeResolutionResult(PackageManager.Pnpm)),
          yaml("""
            catalog:
              acme-alias: 'npm:@acme/fork@^1.4.1'
              acme-folded: >
                ~1.4.1
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    // pnpm 10.0.0 reads no overrides from pnpm-workspace.yaml and rejects one without packages (pnpm-releases)
    @Test
    void aCatalogEntryOfAReleaseNoRecordingCoversIsDeclined() {
        String manifest = """
          {
            "name": "consumer",
            "packageManager": "pnpm@10.0.0",
            "dependencies": {
              "acme-logger": "catalog:"
            }
          }
          """;
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0"))
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .extracting(NodeDependencyEditsDeclined.Row::getReason).isEqualTo("acme-logger's catalog entry was not " +
                "upgraded: pnpm 10.0.0 is not a release whose writes are recorded (9.5.0 - 9.15.9, 10.32.1 - " +
                "10.34.6, 11.0.6 - 11)")),
          packageJson(manifest, null, nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "1.4.1")),
            s -> s.noTrim()),
          yaml("""
            catalog:
              acme-logger: ~1.4.1
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    @Test
    void theDocumentDecidesNotTheMarker() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0")),
          packageJson("""
            {
              "name": "consumer",
              "dependencies": {
                "acme-logger": "catalog:"
              }
            }
            """, null, nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "1.4.1"))),
          yaml("""
            catalog:
              acme-logger: ~1.4.1
            """, """
            catalog:
              acme-logger: ~1.5.0
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    @Test
    void withoutALockThePackageManagerComesFromTheManifest() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, ">=2.0.0")),
          packageJson("""
            {
              "name": "consumer",
              "packageManager": "pnpm@11.2.2",
              "dependencies": {
                "acme-logger": "catalog:"
              }
            }
            """, null, nodeResolutionResult((PackageManager) null)),
          yaml("""
            catalog:
              acme-logger: ~1.4.1
            """, """
            catalog:
              acme-logger: '>=2.0.0'
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    // corepack 0.35.0 refuses a range as devEngines.packageManager's version (recorded)
    @Test
    void withoutALockASpecCorepackRejectsDeclinesWithItsMessage() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0"))
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .extracting(NodeDependencyEditsDeclined.Row::getReason).asString()
              .startsWith("acme-logger's catalog entry was not upgraded: ").contains("expected a semver version")),
          packageJson("""
            {
              "name": "consumer",
              "devEngines": {"packageManager": {"name": "pnpm", "version": "^10"}},
              "dependencies": {
                "acme-logger": "catalog:"
              }
            }
            """, null, nodeResolutionResult((PackageManager) null)),
          yaml("""
            catalog:
              acme-logger: ~1.4.1
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }

    // the catalog entry moves and the override of the same package is declined, whichever recipe runs first
    @ParameterizedTest(name = "{0}/{1}")
    @CsvSource(delimiter = '|', textBlock = """
      pnpm       | catalog-entry-one-importer      | wrappy    | 1.0.2
      pnpm       | catalog-entry-named             | is-number | ^7.0.0
      bun        | catalog-workspaces-catalog-edit | ms        | 2.1.3
      yarn-berry | catalog-yarnrc-named-edit       | is-number | ^7.0.0
      """)
    void anOverrideOfACatalogDeclaredPackageLeavesTheUpgradeToTheCatalog(String family, String scenario, String name,
                                                                          String version) {
        Recipe upgrade = new UpgradeDependencyVersion(name, null, version);
        Recipe override = new UpgradeTransitiveDependencyVersion(name, version.replace("^", ""), null);
        CatalogRecording recording = new CatalogRecording(family, scenario);
        rewriteRun(
          spec -> spec.recipes(upgrade, override).executionContext(recording.ctx())
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> assertThat(rows).allSatisfy(row -> {
                assertThat(row.getPackageName()).isEqualTo(name);
                assertThat(row.getProtocol()).isEqualTo("catalog:");
            })),
          recording.sources()
        );
        new CatalogRecording(family, scenario).assertEveryVisitOrder(override, upgrade);
        new CatalogRecording(family, scenario).assertEveryVisitOrder(upgrade, override);
    }

    // recorded with pnpm 11.2.2: without a lock it still counts every project its packages match, so the cleanup
    // keeps the entry only a member references
    @Test
    void withoutALockTheCleanupCountsTheProjectsTheWorkspaceLists() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("ms", null, "2.1.3")),
          packageJson("""
            {
              "name": "root",
              "packageManager": "pnpm@11.2.2",
              "dependencies": {
                "ms": "^2.0.0"
              }
            }
            """, """
            {
              "name": "root",
              "packageManager": "pnpm@11.2.2",
              "dependencies": {
                "ms": "2.1.3"
              }
            }
            """, nodeResolutionResult((PackageManager) null)),
          packageJson("""
            {
              "name": "a",
              "dependencies": {
                "wrappy": "catalog:"
              }
            }
            """, null, nodeResolutionResult((PackageManager) null), s -> s.path("packages/a/package.json")),
          yaml("""
            packages:
              - packages/*
            cleanupUnusedCatalogs: true
            catalog:
              wrappy: ^1.0.0
            """, s -> s.path("pnpm-workspace.yaml").noTrim())
        );
    }
}

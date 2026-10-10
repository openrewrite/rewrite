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
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * In a pnpm workspace with catalogs the dependency is added as {@code pnpm add} added it in the recordings under
 * {@code catalogs/pnpm}, under each {@code catalogMode}, with and without {@code cleanupUnusedCatalogs}.
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
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      strict
      strict-cleanup
      """)
    void strictModeRejectsAVersionOutsideTheCatalog(String mode) {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-add-root-declares-mismatch-" + mode);
        rewriteRun(
          spec -> spec.recipe(new AddDependency("wrappy", "1.0.1", null)).executionContext(recording.ctx())
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).contains("ERR_PNPM_CATALOG_VERSION_MISMATCH"))),
          recording.sources("packages/a/package.json", "ERR_PNPM_CATALOG_VERSION_MISMATCH")
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

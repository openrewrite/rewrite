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
import org.openrewrite.test.RewriteTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

class UpgradeDependencyVersionsTest implements RewriteTest {

    @Test
    void upgradesMatchingPackagesTogetherWithExistingRangeAndPeerSemantics() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersions(Map.of("@angular/*", "16.x", "typescript", "5.0.x"), null)),
          packageJson(
            """
              {
                "dependencies": {
                  "@angular/core": "15.x",
                  "@angular/common": "15.x",
                  "@angular/router": "17.x",
                  "unrelated": "1.0.0"
                },
                "devDependencies": {"typescript": "4.9.x"},
                "peerDependencies": {"@angular/core": ">=14"}
              }
              """,
            """
              {
                "dependencies": {
                  "@angular/core": "16.x",
                  "@angular/common": "16.x",
                  "@angular/router": "17.x",
                  "unrelated": "1.0.0"
                },
                "devDependencies": {"typescript": "5.0.x"},
                "peerDependencies": {"@angular/core": ">=14"}
              }
              """,
            nodeResolutionResult(PackageManager.Npm))
        );
    }

    @Test
    void skippedProtocolsAreReportedByTheBatchRecipe() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersions(Map.of("@angular/*", "16.x"), false))
            .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> {
                  assertThat(row.getPackageName()).isEqualTo("@angular/core");
                  assertThat(row.getRequestedVersion()).isEqualTo("16.x");
              })),
          packageJson("""
            {"dependencies":{"@angular/core":"file:../core"}}
            """, null, nodeResolutionResult(PackageManager.Npm))
        );
    }

    @Test
    void upgradesCatalogEntriesTogetherUsingTheExistingCatalogRules() {
        CatalogRecording recording = new CatalogRecording("bun", "catalog-workspaces-catalog-two-entries-edit");
        UpgradeDependencyVersions recipe = new UpgradeDependencyVersions(Map.of("is-number", "^7.0.0", "ms", "2.1.3"), null);
        rewriteRun(spec -> spec.recipe(recipe).executionContext(recording.ctx()), recording.sources());
        recording.assertEveryVisitOrder(recipe);
    }

    @Test
    void upgradesRootAndWorkspaceMemberDependenciesInTheSameBatch() {
        CatalogRecording recording = new CatalogRecording("pnpm", "catalog-compose-plain-member-and-root");
        UpgradeDependencyVersions recipe = new UpgradeDependencyVersions(Map.of("is-number", "^7.0.0", "wrappy", "1.0.2"), null);
        rewriteRun(spec -> spec.recipe(recipe).executionContext(recording.ctx()), recording.sources());
        recording.assertEveryVisitOrder(recipe);
    }

    @Test
    void anEmptyBatchDoesNothing() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersions(Map.of(), true)),
          packageJson("{\"dependencies\":{\"typescript\":\"4.9.x\"}}", null, nodeResolutionResult(PackageManager.Npm))
        );
    }
}

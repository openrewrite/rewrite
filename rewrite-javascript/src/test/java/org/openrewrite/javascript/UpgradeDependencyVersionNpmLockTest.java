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
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.javascript.internal.npmlock.NpmLockEngineTest.RoutedHttp;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.javascript.internal.npmlock.NpmLockWriterTest.resource;

/**
 * End-to-end recipe runs over the recorded fixtures: the recipe edits the manifest,
 * the native engine rewrites the lock byte-identically to real npm, and failures
 * surface as warnings on both files plus a data table row — all pure JVM, offline.
 */
class UpgradeDependencyVersionNpmLockTest implements RewriteTest {

    private static NodeResolutionResult npmMarker(Dependency... dependencies) {
        return new NodeResolutionResult(
          UUID.randomUUID(), "fixture", "1.0.0", null, ".",
          null,
          asList(dependencies),
          emptyList(), emptyList(), emptyList(), emptyList(),
          emptyList(),
          PackageManager.Npm,
          null, null);
    }

    private static ExecutionContext offlineCtx(String scenario, String... packages) {
        RoutedHttp http = new RoutedHttp();
        for (String pkg : packages) {
            http.route(pkg, scenario);
        }
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        return ctx;
    }

    @Test
    void upgradesManifestAndLockNatively() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("is-number", null, "^6.0.0"))
            .executionContext(offlineCtx("upgrade-leaf", "is-number")),
          json(
            resource("/npmlock/upgrade-leaf/package.json.before"),
            s -> {
                s.path("package.json");
                s.markers(npmMarker(new Dependency("is-number", "^4.0.0", null)));
                s.noTrim().after(actual -> resource("/npmlock/upgrade-leaf/package.json"));
            }
          ),
          json(
            resource("/npmlock/upgrade-leaf/package-lock.before.json"),
            s -> s.path("package-lock.json").noTrim()
              .after(actual -> resource("/npmlock/upgrade-leaf/package-lock.after.json"))
          )
        );
    }

    @Test
    void cascadeFailureWarnsBothFilesAndRecordsRow() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("is-odd", null, "^3.0.1"))
            .executionContext(offlineCtx("cascade-fails", "is-odd"))
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getSourcePath()).isEqualTo("package-lock.json");
                assertThat(rows.get(0).getPackageName()).isEqualTo("is-odd");
                assertThat(rows.get(0).getReason()).isEqualTo("RESOLUTION_REQUIRED");
            }),
          json(
            resource("/npmlock/cascade-fails/package.json.before"),
            s -> {
                s.path("package.json");
                s.markers(npmMarker(new Dependency("is-odd", "^2.0.0", null)));
                s.noTrim().after(actual -> {
                    assertThat(actual)
                      .startsWith("/*~~(lock regeneration failed: RESOLUTION_REQUIRED")
                      .endsWith(resource("/npmlock/cascade-fails/package.json"));
                    return actual;
                });
                s.afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                  .as("manifest carries the lock-regeneration-failure warning")
                  .isPresent());
            }
          ),
          json(
            resource("/npmlock/cascade-fails/package-lock.before.json"),
            s -> {
                s.path("package-lock.json");
                s.noTrim().after(actual -> {
                    assertThat(actual)
                      .startsWith("/*~~(lock regeneration failed: RESOLUTION_REQUIRED")
                      .endsWith(resource("/npmlock/cascade-fails/package-lock.before.json"));
                    return actual;
                });
                s.afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                  .as("the untouched lock also carries the warning")
                  .isPresent());
            }
          )
        );
    }
}

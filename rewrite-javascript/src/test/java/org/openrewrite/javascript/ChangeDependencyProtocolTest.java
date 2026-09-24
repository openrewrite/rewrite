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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.marker.Markup;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * A {@code catalog:} or {@code workspace:} value is a reference whose constraint is held in another file
 * and keyed on the package's current name. Renaming the declaration leaves that reference dangling and
 * the manifest stops installing, and overwriting the value discards the constraint outright. Neither is
 * recoverable from the manifest alone, so the declaration is left exactly as it is and the file is marked
 * with why.
 */
class ChangeDependencyProtocolTest implements RewriteTest {

    private static final String CATALOG_MANIFEST = "{\n" +
            "  \"name\": \"consumer\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"acme-logger\": \"catalog:\"\n" +
            "  }\n" +
            "}\n";

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "^2.0.0")
    void renameIsRefusedForAProtocolValue(@Nullable String newVersion) {
        rewriteRun(
                spec -> spec.recipe(new ChangeDependency("acme-logger", "acme-log", newVersion, null))
                        .expectedCyclesThatMakeChanges(1)
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> {
                            assertThat(rows).hasSize(1);
                            assertThat(rows.get(0).getPackageName()).isEqualTo("acme-logger");
                            assertThat(rows.get(0).getProtocol()).isEqualTo("catalog:");
                            assertThat(rows.get(0).getCurrentValue()).isEqualTo("catalog:");
                        }),
                // `after` is the printed source including the marker comment, so it is asserted on rather
                // than spelled out: what matters is that the declaration itself is byte-identical.
                packageJson(CATALOG_MANIFEST, null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the declaration is left exactly as it was")
                                    .contains("\"acme-logger\": \"catalog:\"")
                                    .doesNotContain("\"acme-log\":")
                                    .doesNotContain("^2.0.0");
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the refusal is marked on the manifest")
                                .hasValueSatisfying(error -> assertThat(error.getMessage())
                                        .contains("acme-logger")
                                        .contains("catalog:")
                                        .contains("left unchanged"))))
        );
    }

    @Test
    void anOrdinaryConstraintIsStillReplaced() {
        String before = "{\n" +
                "  \"name\": \"consumer\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"dependencies\": {\n" +
                "    \"acme-logger\": \"~1.4.1\"\n" +
                "  }\n" +
                "}\n";
        String after = before.replace("\"acme-logger\": \"~1.4.1\"", "\"acme-log\": \"^2.0.0\"");

        rewriteRun(
                spec -> spec.recipe(new ChangeDependency("acme-logger", "acme-log", "^2.0.0", null)),
                packageJson(before, after,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")))
        );
    }
}

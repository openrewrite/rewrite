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
import org.openrewrite.DataTable;
import org.openrewrite.DataTableStore;
import org.openrewrite.RecipeRun;
import org.openrewrite.marker.Markup;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * An indirection specifier such as {@code catalog:} or {@code workspace:} is a reference whose
 * constraint is held in another file and keyed on the package's current name. Renaming the declaration
 * leaves that reference dangling and the manifest stops installing, and overwriting the value discards
 * the constraint outright. Neither is recoverable from the manifest alone, so the declaration is left
 * exactly as it is and the file is marked with why.
 * <p>
 * A location specifier such as {@code github:} only says where to fetch the same package from, so an
 * explicit new version migrates it to the registry rather than discarding anything.
 */
class ChangeDependencyProtocolTest implements RewriteTest {

    private static String manifest(String declaredValue) {
        return """
                {
                  "name": "consumer",
                  "version": "1.0.0",
                  "dependencies": {
                    "acme-logger": "%s"
                  }
                }
                """.formatted(declaredValue);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "catalog:,                           catalog:,   null",
            "catalog:,                           catalog:,   ^2.0.0",
            "workspace:^,                        workspace:, null",
            "workspace:^,                        workspace:, ^2.0.0",
            "patch:acme-logger@1.4.1#fix.patch,  patch:,     null",
            "patch:acme-logger@1.4.1#fix.patch,  patch:,     ^2.0.0",
            "npm:@acme/other@^1.0.0,             npm:,       null",
            "npm:@acme/other@^1.0.0,             npm:,       ^2.0.0",
            // A location specifier is refused too when no new version is given: its value points at the
            // old package, so carrying it over to the new name installs the wrong thing.
            "github:me/old-fork,                 github:,    null"
    })
    void renameIsRefusedForAProtocolValue(String declaredValue, String expectedProtocol, @Nullable String newVersion) {
        rewriteRun(
                spec -> spec.recipe(new ChangeDependency("acme-logger", "acme-log", newVersion, null))
                        .expectedCyclesThatMakeChanges(1)
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> {
                            assertThat(rows).hasSize(1);
                            assertThat(rows.get(0).getPackageName()).isEqualTo("acme-logger");
                            assertThat(rows.get(0).getProtocol()).isEqualTo(expectedProtocol);
                            assertThat(rows.get(0).getCurrentValue()).isEqualTo(declaredValue);
                        }),
                // `after` is the printed source including the marker comment, so it is asserted on rather
                // than spelled out: what matters is that the declaration itself is byte-identical.
                packageJson(manifest(declaredValue), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", declaredValue)),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the declaration is left exactly as it was")
                                    .contains("\"acme-logger\": \"" + declaredValue + "\"")
                                    .doesNotContain("\"acme-log\":")
                                    .doesNotContain("^2.0.0");
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the refusal is marked on the manifest")
                                .hasValueSatisfying(error -> assertThat(error.getMessage())
                                        .contains("acme-logger")
                                        .contains(expectedProtocol)
                                        .contains("left unchanged"))))
        );
    }

    @Test
    void aLocationSpecifierIsMigratedWhenANewVersionIsGiven() {
        String before = """
                {
                  "name": "consumer",
                  "version": "1.0.0",
                  "dependencies": {
                    "old-fork": "github:me/old-fork"
                  }
                }
                """;
        String after = before.replace("\"old-fork\": \"github:me/old-fork\"", "\"new-pkg\": \"^2.0.0\"");

        rewriteRun(
                spec -> spec.recipe(new ChangeDependency("old-fork", "new-pkg", "^2.0.0", null))
                        .afterRecipe(ChangeDependencyProtocolTest::assertNoSkipRows),
                packageJson(before, after,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("old-fork", "github:me/old-fork")),
                        s -> s.afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("a migration is not a refusal, so nothing is marked")
                                .isEmpty()))
        );
    }

    @Test
    void anOrdinaryConstraintIsStillReplaced() {
        String before = """
                {
                  "name": "consumer",
                  "version": "1.0.0",
                  "dependencies": {
                    "acme-logger": "~1.4.1"
                  }
                }
                """;
        String after = before.replace("\"acme-logger\": \"~1.4.1\"", "\"acme-log\": \"^2.0.0\"");

        rewriteRun(
                spec -> spec.recipe(new ChangeDependency("acme-logger", "acme-log", "^2.0.0", null)),
                packageJson(before, after,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")))
        );
    }

    /** {@link org.openrewrite.test.RecipeSpec#dataTable} fails when the table is absent, so absence is asserted here. */
    static void assertNoSkipRows(RecipeRun run) {
        DataTableStore store = run.getDataTableStore();
        for (DataTable<?> dt : store.getDataTables()) {
            if (dt.getType().equals(NodeDependencyProtocolsSkipped.Row.class)) {
                try (Stream<?> rows = store.getRows(dt.getName(), dt.getGroup())) {
                    List<?> collected = rows.collect(toList());
                    assertThat(collected).as("no skip was reported").isEmpty();
                }
            }
        }
    }
}

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
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * An override the package manager would not read changes nothing: pnpm 11.2.2 ignores package.json
 * {@code pnpm.overrides} and bun 1.3.10 has no nested overrides.
 */
class UpgradeTransitiveDependencyVersionTest implements RewriteTest {

    private static final String PKG = "{\n" +
            "  \"name\": \"p\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"is-odd\": \"3.0.1\"\n" +
            "  }\n" +
            "}\n";

    @Test
    void pnpmReadsOverridesOnlyFromItsWorkspaceManifest() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null))
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getDetail()).contains("pnpm-workspace.yaml");
            }),
          packageJson(PKG, null, nodeResolutionResult(PackageManager.Pnpm, dependency("is-odd", "3.0.1")), s -> {
          })
        );
    }

    @Test
    void bunHasNoNestedOverrides() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", "is-odd"))
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getDetail()).contains("nested overrides");
            }),
          packageJson(PKG, null, nodeResolutionResult(PackageManager.Bun, dependency("is-odd", "3.0.1")), s -> {
          })
        );
    }
}

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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * A dependency's value in a {@code package.json} is not always a version constraint. pnpm 9.5+ moves the
 * constraint into a {@code catalog:} in {@code pnpm-workspace.yaml} and leaves the bare marker
 * {@code catalog:} behind in the manifest, and both pnpm and Yarn accept further protocols
 * ({@code workspace:}, {@code patch:}, {@code portal:}, {@code npm:}) in the same position. Overwriting
 * one takes the package out of whatever held its constraint, so the recipe leaves it alone and reports it.
 */
class UpgradeDependencyVersionCatalogTest implements RewriteTest {

    private static final String PACKAGE_JSON = "{\n" +
            "  \"name\": \"consumer\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"acme-logger\": \"catalog:\",\n" +
            "    \"acme-lib\": \"workspace:^\"\n" +
            "  }\n" +
            "}\n";

    private static final String WORKSPACE_YAML = "packages:\n" +
            "  - '.'\n" +
            "catalog:\n" +
            "  acme-logger: '~1.4.1'\n";

    @Test
    void protocolReferencesAreLeftAloneAndReported() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion(null, "acme-*", "~1.5.0"))
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> {
                            assertThat(rows).hasSize(2);
                            assertThat(rows).allSatisfy(row -> {
                                assertThat(row.getSourcePath()).isEqualTo("package.json");
                                assertThat(row.getDependencyScope()).isEqualTo("dependencies");
                                assertThat(row.getRequestedVersion()).isEqualTo("~1.5.0");
                            });
                            assertThat(rows).extracting("packageName", "protocol", "currentValue")
                                    .containsExactlyInAnyOrder(
                                            tuple("acme-logger", "catalog:", "catalog:"),
                                            tuple("acme-lib", "workspace:", "workspace:^"));
                        }),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "catalog:"),
                                dependency("acme-lib", "workspace:^"))),
                yaml(WORKSPACE_YAML, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aMarkerClaimingAResolvedVersionStillCannotOverwriteTheManifest() {
        // The recipe filters by the marker, but the overwrite happens against the manifest literal. Were
        // the marker ever to resolve `catalog:` to the version behind it, filtering alone would stop
        // protecting; the manifest must still come back untouched.
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0")),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "~1.4.1"),
                                dependency("acme-lib", "workspace:^"))),
                yaml(WORKSPACE_YAML, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    @Disabled("A0 only stops the overwrite; writing the bump through to the catalog is the follow-up step")
    void catalogEntryIsUpgradedInTheWorkspaceFile() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0")),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "catalog:"),
                                dependency("acme-lib", "workspace:^"))),
                yaml(WORKSPACE_YAML, WORKSPACE_YAML.replace("'~1.4.1'", "'~1.5.0'"),
                        s -> s.path("pnpm-workspace.yaml"))
        );
    }
}

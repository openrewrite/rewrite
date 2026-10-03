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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * The column `NpmCatalogTest > versionInACatalog — OpenRewrite` runs both dependency recipes composed,
 * over its four cells: pnpm and Yarn Berry, each with the default and a named catalog.
 * Both must behave: `UpgradeDependencyVersion` follows the reference into the catalog file, and
 * `UpgradeTransitiveDependencyVersion` declines to pin a dependency whose version position holds a
 * reference. The manifest must come out byte-identical, with no `overrides`, `resolutions` or
 * `pnpm.overrides` block, and only the entry's scalar may change. The transitive recipe marks its
 * decline even here, where the other recipe did follow the reference: its own job, pinning the
 * transitive copy, was still not done.
 */
class UpgradeDependencyVersionCatalogColumnTest implements RewriteTest {

    private static final String PKG = "acme-logger";
    private static final String OLD_VERSION = "~1.4.1";
    private static final String NEW_VERSION = "~1.4.2-osera-00001";

    private static final String MANIFEST = """
            {
              "name": "consumer",
              "version": "1.0.0",
              "dependencies": {
                "acme-logger": "%s"
              }
            }
            """;

    private static final String DEFAULT_CATALOG = """
            packages:
              - '.'
            catalog:
              acme-logger: '%s'
            """;

    private static final String NAMED_CATALOG = """
            packages:
              - '.'
            catalogs:
              stable:
                acme-logger: '%s'
            """;

    static Stream<Arguments> spellings() {
        return Stream.of(
                Arguments.of(PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:", DEFAULT_CATALOG),
                Arguments.of(PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:stable", NAMED_CATALOG),
                Arguments.of(PackageManager.YarnBerry, ".yarnrc.yml", "catalog:", DEFAULT_CATALOG),
                Arguments.of(PackageManager.YarnBerry, ".yarnrc.yml", "catalog:stable", NAMED_CATALOG));
    }

    @ParameterizedTest(name = "{0} {2}")
    @MethodSource("spellings")
    void versionInACatalog(PackageManager pm, String workspaceFile, String reference, String catalog) {
        rewriteRun(
                spec -> spec.recipe(new CompositeRecipe(asList(
                        new UpgradeDependencyVersion(PKG, null, NEW_VERSION),
                        new UpgradeTransitiveDependencyVersion(PKG, NEW_VERSION, null)))),
                // `after` is the printed source including the marker comment, so it is asserted on rather
                // than spelled out: what matters is that the declaration itself is byte-identical.
                packageJson(MANIFEST.formatted(reference), null,
                        nodeResolutionResult(pm, dependency(PKG, reference)),
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the manifest is byte-identical and gains no override block")
                                    .contains("\"acme-logger\": \"" + reference + "\"")
                                    .doesNotContain("\"overrides\"")
                                    .doesNotContain("\"resolutions\"")
                                    .doesNotContain("\"pnpm\"");
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the transitive recipe's decline is reported, even though the catalog entry moved")
                                .hasValueSatisfying(warn -> assertThat(warn.getMessage())
                                        .contains(PKG)
                                        .contains(reference)))),
                yaml(catalog.formatted(OLD_VERSION), catalog.formatted(NEW_VERSION),
                        s -> s.path(workspaceFile))
        );
    }
}

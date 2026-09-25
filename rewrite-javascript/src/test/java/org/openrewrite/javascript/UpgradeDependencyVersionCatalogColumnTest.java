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
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * The column `NpmCatalogTest > versionInACatalog — OpenRewrite` runs both dependency recipes composed.
 * Both must behave: `UpgradeDependencyVersion` follows the reference into the catalog file, and
 * `UpgradeTransitiveDependencyVersion` declines to pin a dependency whose version position holds a
 * reference. The manifest must come out byte-identical, with no `overrides`, `resolutions` or
 * `pnpm.overrides` block, and only the entry's scalar may change.
 */
class UpgradeDependencyVersionCatalogColumnTest implements RewriteTest {

    private static final String PKG = "acme-logger";
    private static final String NEW_VERSION = "~1.4.2-osera-00001";

    private static final String MANIFEST = "{\n" +
            "  \"name\": \"consumer\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"" + PKG + "\": \"%s\"\n" +
            "  }\n" +
            "}\n";

    private static final String DEFAULT_CATALOG = "packages:\n" +
            "  - '.'\n" +
            "catalog:\n" +
            "  " + PKG + ": '%s'\n";

    private static final String NAMED_CATALOG = "packages:\n" +
            "  - '.'\n" +
            "catalogs:\n" +
            "  stable:\n" +
            "    " + PKG + ": '%s'\n";

    static Stream<Arguments> spellings() {
        return Stream.of(
                Arguments.of("PnpmDefault", PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:", DEFAULT_CATALOG),
                Arguments.of("PnpmNamed", PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:stable", NAMED_CATALOG),
                Arguments.of("YarnDefault", PackageManager.YarnBerry, ".yarnrc.yml", "catalog:", DEFAULT_CATALOG),
                Arguments.of("YarnNamed", PackageManager.YarnBerry, ".yarnrc.yml", "catalog:stable", NAMED_CATALOG));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("spellings")
    void versionInACatalog(String cell, PackageManager pm, String workspaceFile, String reference, String catalog) {
        rewriteRun(
                spec -> spec.recipe(new CompositeRecipe(asList(
                        new UpgradeDependencyVersion(PKG, null, NEW_VERSION),
                        new UpgradeTransitiveDependencyVersion(PKG, NEW_VERSION, null)))),
                packageJson(String.format(MANIFEST, reference), null,
                        nodeResolutionResult(pm, dependency(PKG, reference))),
                yaml(String.format(catalog, "~1.4.1"), String.format(catalog, NEW_VERSION),
                        s -> s.path(workspaceFile))
        );
    }
}

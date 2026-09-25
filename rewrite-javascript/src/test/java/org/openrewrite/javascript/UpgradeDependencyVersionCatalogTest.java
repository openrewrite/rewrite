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

import static java.util.Arrays.asList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * A dependency's value in a {@code package.json} is not always a version constraint. pnpm 9.5+ and
 * Yarn 4.10+ move the constraint into a catalog and leave a {@code catalog:} reference behind in the
 * manifest. Writing a version over that reference takes the package out of the catalog: this member
 * moves and every other member stays, and the one declaration that kept them in step is gone.
 * <p>
 * So the recipe follows the reference and edits the catalog entry instead, but only when it can
 * account for every consumer of that entry. When it cannot, it leaves the manifest and the entry
 * alone and reports the skip, which keeps every member in step at the cost of the upgrade. Protocols
 * with no such declaration to follow ({@code workspace:}, {@code patch:}, {@code portal:},
 * {@code npm:}) are always left alone.
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

    private static final String NAMED_WORKSPACE_YAML = "packages:\n" +
            "  - '.'\n" +
            "catalogs:\n" +
            "  strict:\n" +
            "    acme-logger: '~1.4.1'\n";

    private static final String ROOT_JSON = "{\n" +
            "  \"name\": \"root\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"private\": true\n" +
            "}\n";

    private static final String MEMBER_JSON = "{\n" +
            "  \"name\": \"member\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"acme-logger\": \"catalog:\"\n" +
            "  }\n" +
            "}\n";

    private static final String MEMBERS_WORKSPACE_YAML = "packages:\n" +
            "  - 'packages/*'\n" +
            "catalog:\n" +
            "  acme-logger: '~1.4.1'\n";

    private static String referencing(String catalogReference) {
        return PACKAGE_JSON.replace("\"catalog:\"", '"' + catalogReference + '"');
    }

    @Test
    void pnpmDefaultCatalogEntryIsUpgraded() {
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

    @Test
    void pnpmNamedCatalogEntryIsUpgraded() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0")),
                packageJson(referencing("catalog:strict"), null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "catalog:strict"),
                                dependency("acme-lib", "workspace:^"))),
                yaml(NAMED_WORKSPACE_YAML, NAMED_WORKSPACE_YAML.replace("'~1.4.1'", "'~1.5.0'"),
                        s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void yarnDefaultCatalogEntryIsUpgraded() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0")),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.YarnBerry,
                                dependency("acme-logger", "catalog:"),
                                dependency("acme-lib", "workspace:^"))),
                yaml(WORKSPACE_YAML, WORKSPACE_YAML.replace("'~1.4.1'", "'~1.5.0'"),
                        s -> s.path(".yarnrc.yml"))
        );
    }

    @Test
    void yarnNamedCatalogEntryIsUpgraded() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0")),
                packageJson(referencing("catalog:strict"), null,
                        nodeResolutionResult(PackageManager.YarnBerry,
                                dependency("acme-logger", "catalog:strict"),
                                dependency("acme-lib", "workspace:^"))),
                yaml(NAMED_WORKSPACE_YAML, NAMED_WORKSPACE_YAML.replace("'~1.4.1'", "'~1.5.0'"),
                        s -> s.path(".yarnrc.yml"))
        );
    }

    /**
     * The case that separates a real implementation from one that hardcodes the safe branch: with a
     * single consumer there is nothing to protect and any implementation passes. Here the workspace
     * declares a second member whose manifest never reached the recipe, so it cannot know whether that
     * member also references the entry. Moving the entry might move it; the recipe refuses instead.
     */
    @Test
    void aCatalogEntryIsLeftAloneWhenADeclaredMemberIsMissingFromTheSourceSet() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0"))
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("sourcePath", "packageName", "protocol", "currentValue")
                                        .containsExactly(
                                                tuple("packages/a/package.json", "acme-logger", "catalog:", "catalog:"))),
                packageJson(ROOT_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                asList("packages/a/package.json", "packages/b/package.json"))),
                packageJson(MEMBER_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/a/package.json")),
                // packages/b/package.json is declared above but not supplied.
                yaml(MEMBERS_WORKSPACE_YAML, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    /**
     * Both members are present and both reference the entry, but only one of them resolved, so only one
     * can be matched and upgraded. Moving the entry would move the other member to a version nothing
     * asked on its behalf, so the recipe refuses and both stay on `~1.4.1`.
     */
    @Test
    void aSharedCatalogEntryIsLeftAloneWhenAConsumerIsNotBeingUpgraded() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0"))
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("sourcePath", "packageName", "protocol", "currentValue")
                                        .containsExactly(
                                                tuple("packages/a/package.json", "acme-logger", "catalog:", "catalog:"))),
                packageJson(ROOT_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                asList("packages/a/package.json", "packages/b/package.json"))),
                packageJson(MEMBER_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/a/package.json")),
                packageJson(MEMBER_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm),
                        s -> s.path("packages/b/package.json")),
                yaml(MEMBERS_WORKSPACE_YAML, s -> s.path("pnpm-workspace.yaml"))
        );
    }

    /** Both members reference the entry and both are matched, so the entry can move and take both with it. */
    @Test
    void aSharedCatalogEntryIsUpgradedWhenEveryConsumerIsBeingUpgraded() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0")),
                packageJson(ROOT_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                asList("packages/a/package.json", "packages/b/package.json"))),
                packageJson(MEMBER_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/a/package.json")),
                packageJson(MEMBER_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/b/package.json")),
                yaml(MEMBERS_WORKSPACE_YAML, MEMBERS_WORKSPACE_YAML.replace("'~1.4.1'", "'~1.5.0'"),
                        s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void protocolsWithNothingToFollowAreLeftAloneAndReported() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion(null, "acme-*", "~1.5.0"))
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows -> {
                            assertThat(rows).hasSize(1);
                            assertThat(rows).extracting("packageName", "protocol", "currentValue")
                                    .containsExactly(tuple("acme-lib", "workspace:", "workspace:^"));
                        }),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "catalog:"),
                                dependency("acme-lib", "workspace:^"))),
                yaml(WORKSPACE_YAML, WORKSPACE_YAML.replace("'~1.4.1'", "'~1.5.0'"),
                        s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aCatalogReferenceWithNoEntryToFollowIsLeftAlone() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, "~1.5.0")),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "catalog:"),
                                dependency("acme-lib", "workspace:^"))),
                yaml("packages:\n  - '.'\n", s -> s.path("pnpm-workspace.yaml"))
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
}

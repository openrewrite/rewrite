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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.pnpmLock;
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

    private static final String OLD = "~1.4.1";
    private static final String NEW = "~1.5.0";

    /** One dependency, declared by reference; {@code %s} is that reference. */
    private static final String MANIFEST = "{\n" +
            "  \"name\": \"consumer\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"acme-logger\": \"%s\"\n" +
            "  }\n" +
            "}\n";

    /** A catalog reference beside a protocol that has no catalog to follow. */
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
            "  acme-logger: '" + OLD + "'\n";

    private static final String NAMED_WORKSPACE_YAML = "packages:\n" +
            "  - '.'\n" +
            "catalogs:\n" +
            "  stable:\n" +
            "    acme-logger: '" + OLD + "'\n";

    private static final String YARNRC = "catalog:\n" +
            "  acme-logger: '" + OLD + "'\n";

    private static final String NAMED_YARNRC = "catalogs:\n" +
            "  stable:\n" +
            "    acme-logger: '" + OLD + "'\n";

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
            "  acme-logger: '" + OLD + "'\n";

    private static final String PNPM_LOCK = "lockfileVersion: '9.0'\n" +
            "\n" +
            "catalogs:\n" +
            "  default:\n" +
            "    acme-logger:\n" +
            "      specifier: '" + OLD + "'\n" +
            "      version: 1.4.1\n" +
            "\n" +
            "importers:\n" +
            "\n" +
            "  .:\n" +
            "    dependencies:\n" +
            "      acme-logger:\n" +
            "        specifier: 'catalog:'\n" +
            "        version: 1.4.1\n";

    static Stream<Arguments> spellings() {
        return Stream.of(
                Arguments.of(PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:", WORKSPACE_YAML),
                Arguments.of(PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:stable", NAMED_WORKSPACE_YAML),
                Arguments.of(PackageManager.YarnBerry, ".yarnrc.yml", "catalog:", YARNRC),
                Arguments.of(PackageManager.YarnBerry, ".yarnrc.yml", "catalog:stable", NAMED_YARNRC));
    }

    @ParameterizedTest(name = "{0} {2}")
    @MethodSource("spellings")
    void catalogEntryIsUpgraded(PackageManager pm, String workspaceFile, String reference, String catalog) {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW)),
                packageJson(String.format(MANIFEST, reference), null,
                        nodeResolutionResult(pm, dependency("acme-logger", reference))),
                yaml(catalog, catalog.replace(OLD, NEW), s -> s.path(workspaceFile))
        );
    }

    static Stream<Arguments> constraintsAndRenderings() {
        return Stream.of(
                Arguments.of("~1.5.0", "  acme-logger: ~1.5.0"),
                Arguments.of(">=2.0.0", "  acme-logger: '>=2.0.0'"),
                Arguments.of("*", "  acme-logger: '*'"),
                Arguments.of("2", "  acme-logger: '2'"),
                Arguments.of("2.0", "  acme-logger: '2.0'"));
    }

    /**
     * An unquoted entry has to gain quotes whenever YAML would read the new constraint as something
     * other than a string. Two ways that happens: the value opens with an indicator, so the file stops
     * parsing at all ({@code >=2.0.0} reads as a folded block scalar, {@code *} as an alias); or the
     * value parses but resolves to another type, so the catalog holds a number where a version belongs
     * ({@code 2} is a valid npm range and reads as an integer). A constraint YAML already reads as a
     * string keeps the style it found.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("constraintsAndRenderings")
    void anUnquotedEntryGainsQuotesOnlyWhenTheConstraintNeedsThem(String newVersion, String expectedEntry) {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, newVersion)),
                packageJson(String.format(MANIFEST, "catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:"))),
                yaml("catalog:\n  acme-logger: 1.4.1\n",
                        "catalog:\n" + expectedEntry + "\n",
                        s -> s.path("pnpm-workspace.yaml"))
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
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW))
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
     * asked on its behalf, so the recipe refuses and both stay on the old constraint.
     */
    @Test
    void aSharedCatalogEntryIsLeftAloneWhenAConsumerIsNotBeingUpgraded() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW))
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
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW)),
                packageJson(ROOT_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                asList("packages/a/package.json", "packages/b/package.json"))),
                packageJson(MEMBER_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/a/package.json")),
                packageJson(MEMBER_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:")),
                        s -> s.path("packages/b/package.json")),
                yaml(MEMBERS_WORKSPACE_YAML, MEMBERS_WORKSPACE_YAML.replace(OLD, NEW),
                        s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void protocolsWithNothingToFollowAreLeftAloneAndReported() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion(null, "acme-*", NEW))
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("packageName", "protocol", "currentValue")
                                        .containsExactly(tuple("acme-lib", "workspace:", "workspace:^"))),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "catalog:"),
                                dependency("acme-lib", "workspace:^"))),
                yaml(WORKSPACE_YAML, WORKSPACE_YAML.replace(OLD, NEW), s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aCatalogReferenceWithNoEntryToFollowIsLeftAlone() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW)),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "catalog:"),
                                dependency("acme-lib", "workspace:^"))),
                yaml("packages:\n  - '.'\n", s -> s.path("pnpm-workspace.yaml"))
        );
    }

    /**
     * The entry moves but the lock cannot follow, and pnpm does not report the disagreement: a
     * frozen-lockfile install still succeeds and still installs the old version (pnpm/pnpm#9369). So a
     * silent stale lock would be a change that looks applied and is not. Refuse loudly instead.
     */
    @Test
    void aCatalogEditRefusesLoudlyWhenItLeavesALockBehind() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW))
                        .dataTable(NodeLockRegenerationFailures.Row.class, rows -> {
                            assertThat(rows).hasSize(1);
                            assertThat(rows.get(0).getSourcePath()).isEqualTo("package.json");
                            assertThat(rows.get(0).getPackageName()).isEqualTo("acme-logger");
                            assertThat(rows.get(0).getReason()).isEqualTo("UNSUPPORTED_ENTRY_TYPE");
                        }),
                packageJson(String.format(MANIFEST, "catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:"))),
                yaml(WORKSPACE_YAML, WORKSPACE_YAML.replace(OLD, NEW), s -> s.path("pnpm-workspace.yaml")),
                pnpmLock(PNPM_LOCK, null,
                        s -> s.afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the lock left behind by the catalog edit carries the warning").isPresent()))
        );
    }

    @Test
    void aMarkerClaimingAResolvedVersionStillCannotOverwriteTheManifest() {
        // The recipe filters by the marker, but the overwrite happens against the manifest literal. Were
        // the marker ever to resolve `catalog:` to the version behind it, filtering alone would stop
        // protecting; the manifest must still come back untouched.
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW)),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", OLD),
                                dependency("acme-lib", "workspace:^"))),
                yaml(WORKSPACE_YAML, s -> s.path("pnpm-workspace.yaml"))
        );
    }
}

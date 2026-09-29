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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
 * So the recipe follows the reference and edits the catalog entry instead, which moves every member
 * sharing it at once. Where there is no entry to follow it leaves the manifest alone and reports the
 * skip, as it does for protocols with no such declaration behind them ({@code workspace:},
 * {@code patch:}, {@code portal:}, {@code npm:}).
 */
class UpgradeDependencyVersionCatalogTest implements RewriteTest {

    private static final String OLD = "~1.4.1";
    private static final String NEW = "~1.5.0";

    /** One dependency, declared by reference; {@code %s} is that reference. */
    private static final String MANIFEST = """
            {
              "name": "consumer",
              "version": "1.0.0",
              "dependencies": {
                "acme-logger": "%s"
              }
            }
            """;

    /** A catalog reference beside a protocol that has no catalog to follow. */
    private static final String PACKAGE_JSON = """
            {
              "name": "consumer",
              "version": "1.0.0",
              "dependencies": {
                "acme-logger": "catalog:",
                "acme-lib": "workspace:^"
              }
            }
            """;

    private static final String WORKSPACE_YAML = """
            packages:
              - '.'
            catalog:
              acme-logger: '%s'
            """;

    private static final String NAMED_WORKSPACE_YAML = """
            packages:
              - '.'
            catalogs:
              stable:
                acme-logger: '%s'
            """;

    private static final String YARNRC = """
            catalog:
              acme-logger: '%s'
            """;

    private static final String NAMED_YARNRC = """
            catalogs:
              stable:
                acme-logger: '%s'
            """;

    private static final String ROOT_JSON = """
            {
              "name": "root",
              "version": "1.0.0",
              "private": true
            }
            """;

    private static final String MEMBER_JSON = """
            {
              "name": "member",
              "version": "1.0.0",
              "dependencies": {
                "acme-logger": "catalog:"
              }
            }
            """;

    private static final String MEMBERS_WORKSPACE_YAML = """
            packages:
              - 'packages/*'
            catalog:
              acme-logger: '%s'
            """;

    private static final String PNPM_LOCK = """
            lockfileVersion: '9.0'

            catalogs:
              default:
                acme-logger:
                  specifier: '%s'
                  version: 1.4.1

            importers:

              .:
                dependencies:
                  acme-logger:
                    specifier: 'catalog:'
                    version: 1.4.1
            """;

    static Stream<Arguments> spellings() {
        return Stream.of(
                Arguments.of(PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:", WORKSPACE_YAML),
                Arguments.of(PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:default", WORKSPACE_YAML),
                Arguments.of(PackageManager.Pnpm, "pnpm-workspace.yaml", "catalog:stable", NAMED_WORKSPACE_YAML),
                Arguments.of(PackageManager.YarnBerry, ".yarnrc.yml", "catalog:", YARNRC),
                Arguments.of(PackageManager.YarnBerry, ".yarnrc.yml", "catalog:stable", NAMED_YARNRC));
    }

    @ParameterizedTest(name = "{0} {2}")
    @MethodSource("spellings")
    void catalogEntryIsUpgraded(PackageManager pm, String workspaceFile, String reference, String catalog) {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW)),
                packageJson(MANIFEST.formatted(reference), null,
                        nodeResolutionResult(pm, dependency("acme-logger", reference))),
                yaml(catalog.formatted(OLD), catalog.formatted(NEW), s -> s.path(workspaceFile))
        );
    }

    /**
     * That the quoting decision reaches the file: a constraint YAML already reads as a string keeps the
     * style it found, one it would read as something else gains quotes. Which constraints fall on which
     * side is {@link org.openrewrite.javascript.internal.NodeCatalogsTest}'s job.
     */
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(quoteCharacter = '"', value = {
            "~1.5.0,  ~1.5.0",
            ">=2.0.0, '>=2.0.0'"})
    void anUnquotedEntryGainsQuotesOnlyWhenTheConstraintNeedsThem(String newVersion, String rendered) {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, newVersion)),
                packageJson(MANIFEST.formatted("catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:"))),
                yaml("catalog:\n  acme-logger: 1.4.1\n",
                        "catalog:\n  acme-logger: " + rendered + "\n",
                        s -> s.path("pnpm-workspace.yaml"))
        );
    }

    /**
     * One entry, two members, one edit. Moving the entry moves both, which is the point of a catalog and
     * the reason the reference is followed rather than replaced member by member.
     */
    @Test
    void aSharedCatalogEntryMovesEveryMemberAtOnce() {
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
                yaml(MEMBERS_WORKSPACE_YAML.formatted(OLD), MEMBERS_WORKSPACE_YAML.formatted(NEW),
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
                yaml(WORKSPACE_YAML.formatted(OLD), WORKSPACE_YAML.formatted(NEW), s -> s.path("pnpm-workspace.yaml"))
        );
    }

    @Test
    void aCatalogReferenceWithNoEntryToFollowIsLeftAlone() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW))
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("packageName", "protocol", "currentValue")
                                        .containsExactly(tuple("acme-logger", "catalog:", "catalog:"))),
                packageJson(PACKAGE_JSON, null,
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("acme-logger", "catalog:"),
                                dependency("acme-lib", "workspace:^"))),
                yaml("packages:\n  - '.'\n", s -> s.path("pnpm-workspace.yaml"))
        );
    }

    /**
     * npm accepts {@code "express": "expressjs/express"} as shorthand for GitHub, with no scheme to see.
     * It is still a specifier, so it is left alone and reported as the protocol it expands to.
     */
    @Test
    void aSchemelessSpecifierIsLeftAloneAndReported() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW))
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("packageName", "protocol", "currentValue")
                                        .containsExactly(tuple("acme-logger", "github:", "acme/logger#v1"))),
                packageJson(MANIFEST.formatted("acme/logger#v1"), null,
                        nodeResolutionResult(PackageManager.Npm, dependency("acme-logger", "acme/logger#v1")))
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
                packageJson(MANIFEST.formatted("catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:"))),
                yaml(WORKSPACE_YAML.formatted(OLD), WORKSPACE_YAML.formatted(NEW), s -> s.path("pnpm-workspace.yaml")),
                pnpmLock(PNPM_LOCK.formatted(OLD), null,
                        s -> s.afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("the lock left behind by the catalog edit carries the warning").isPresent()))
        );
    }

    /**
     * An entry that already holds the requested constraint is not an edit, so nothing is stale and there
     * is nothing to report. Re-running an upgrade that has already landed is the ordinary case.
     */
    @Test
    void aCatalogEntryAlreadyAtTheNewVersionIsLeftAlone() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW))
                        .afterRecipe(run -> assertThat(
                                run.getDataTableRows(NodeDependencyProtocolsSkipped.class))
                                .as("nothing needed doing, which is not the same as being skipped")
                                .isEmpty()),
                packageJson(MANIFEST.formatted("catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:"))),
                yaml(WORKSPACE_YAML.formatted(NEW), s -> s.path("pnpm-workspace.yaml")),
                pnpmLock(PNPM_LOCK.formatted(NEW), null)
        );
    }

    /** A block scalar or an entry that is itself a reference is declined, and a decline is a skip to report. */
    @ParameterizedTest
    @ValueSource(strings = {">\n    ~1.4.1", "'npm:@acme/logger-fork@^1.4.1'"})
    void aCatalogEntryThatCannotBeRewrittenIsReported(String entry) {
        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("acme-logger", null, NEW))
                        .dataTable(NodeDependencyProtocolsSkipped.Row.class, rows ->
                                assertThat(rows).extracting("packageName", "protocol")
                                        .containsExactly(tuple("acme-logger", "catalog:"))),
                packageJson(MANIFEST.formatted("catalog:"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "catalog:"))),
                yaml("catalog:\n  acme-logger: " + entry + "\n", s -> s.path("pnpm-workspace.yaml"))
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
                yaml(WORKSPACE_YAML.formatted(OLD), s -> s.path("pnpm-workspace.yaml"))
        );
    }
}

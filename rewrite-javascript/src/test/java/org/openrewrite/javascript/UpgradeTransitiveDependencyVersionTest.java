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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Preconditions;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.AddKeyValue;
import org.openrewrite.json.ChangeValue;
import org.openrewrite.json.JsonParser;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.text.FindAndReplace;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.yaml.ChangePropertyValue;
import org.openrewrite.yaml.YamlParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;
import static org.openrewrite.javascript.Assertions.pnpmLock;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Each package manager's override written by the recipe at the workspace root, byte for byte as recorded in
 * {@code lock/upgrade-transitive/<family>/<scenario>}: the files the recording installed, and the lock it wrote.
 */
class UpgradeTransitiveDependencyVersionTest implements RewriteTest {

    private static final String RESOURCES = "lock/upgrade-transitive/";
    private static final String MEMBER = "packages/a/package.json";
    private static final String APPS_MEMBER = "apps/a/package.json";

    @Test
    void npmGlobal() {
        golden("npm/override-recipe-global", PackageManager.Npm, "^7.0.0", null);
    }

    @Test
    void npmPath() {
        golden("npm/override-recipe-nested", PackageManager.Npm, "^7.0.0", "is-odd");
    }

    @Test
    void npmPathWhoseParentIsNotInTheTree() {
        golden("npm/override-recipe-path-parent-absent", PackageManager.Npm, "^7.0.0", "a>is-odd");
    }

    @Test
    void npmBesideAnOverrideAlreadyPresent() {
        golden("npm/override-recipe-global-noop", PackageManager.Npm, "^6.0.0", null);
    }

    @Test
    void npmAtTheRootOfAMember() {
        golden("npm/override-recipe-member", PackageManager.Npm, "7.0.0", null);
    }

    @Test
    void pnpmGlobalCreatesTheWorkspaceManifest() {
        golden("pnpm/override-recipe-global", PackageManager.Pnpm, "7.0.0", null);
    }

    @Test
    void pnpmPath() {
        golden("pnpm/override-recipe-path", PackageManager.Pnpm, "7.0.0", "is-odd");
    }

    @Test
    void pnpmBesideAnOverrideAlreadyPresent() {
        golden("pnpm/override-recipe-present", PackageManager.Pnpm, "7.0.0", null);
    }

    @Test
    void pnpmAtTheRootOfAMember() {
        golden("pnpm/override-recipe-member", PackageManager.Pnpm, "7.0.0", null);
    }

    @Test
    void pnpmKeepsAnEarlierRecipesEditOfTheWorkspaceManifest() {
        Scenario s = new Scenario("pnpm/override-recipe-path");
        String workspace = requireNonNull(s.file("workspace"))
          .replace("strictPeerDependencies: false", "strictPeerDependencies: true");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-odd").recipes(
            new ChangePropertyValue("strictPeerDependencies", "true", null, null, null, null),
            new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", "is-odd")),
          s.root(PackageManager.Pnpm, s.file("pkg")),
          yaml(s.file("workspace-before"), workspace, spec -> spec.path("pnpm-workspace.yaml").noTrim()),
          s.lock(PackageManager.Pnpm, s.file("after"))
        );
    }

    @Test
    void pnpmLockReadsAnEarlierRecipesEditOfTheWorkspaceManifestVisitedBeforeIt() {
        Scenario s = new Scenario("pnpm/override-recipe-after-workspace-edit");
        inOrder(s, new CompositeRecipe(Arrays.asList(
          new ChangePropertyValue("autoInstallPeers", "false", null, null, null, null),
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null))), Arrays.asList(
          s.source("pnpm-workspace.yaml", "workspace-before", "workspace"),
          s.manifest("package.json", PackageManager.Pnpm, null, "pkg-before", "pkg"),
          s.source("pnpm-lock.yaml", "before", "after")));
    }

    @Test
    void pnpmAtTheRootOfAWorkspaceWhoseProjectsKeepTheirOwnLocks() {
        golden("pnpm/override-recipe-unshared", PackageManager.Pnpm, "7.0.0", null);
    }

    @Test
    void pnpmWorkspaceWhoseProjectsKeepTheirOwnLocksInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-unshared");
        String member = requireNonNull(s.file("tree-before/" + MEMBER));
        inEveryOrder(s, new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null), Arrays.asList(
          s.manifest("package.json", PackageManager.Pnpm, singletonList(MEMBER), "pkg-before", "pkg"),
          new Source(MEMBER, member, member, nodeResolutionResult(PackageManager.Pnpm, dependencies(member))),
          s.source("pnpm-workspace.yaml", "workspace-before", "workspace"),
          s.source("pnpm-lock.yaml", "probes/before-root-lock", "probes/after-root-lock"),
          s.source("packages/a/pnpm-lock.yaml", "before", "after")));
    }

    @Test
    void pnpmAfterAnEarlierRecipeChangedTheRootInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-compose");
        inEveryOrder(s, new CompositeRecipe(Arrays.asList(new UpgradeDependencyVersion("is-odd", null, "3.0.1"),
            new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null))),
          s.workspaceSources(PackageManager.Pnpm));
    }

    // recorded with pnpm 11.2.2: the lock already holds an overrides: section, which the new override joins
    @Test
    void pnpmBesideAnOverridesSectionTheLockAlreadyHoldsInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-beside-overrides-section");
        inEveryOrder(s, new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null),
          s.workspaceSources(PackageManager.Pnpm));
    }

    @Test
    void pnpmTwoOverridesInOneRunInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-two-pins");
        inEveryOrder(s, new CompositeRecipe(Arrays.asList(
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null),
          new UpgradeTransitiveDependencyVersion("left-pad", "1.3.0", null))), s.workspaceSources(PackageManager.Pnpm));
    }

    @Test
    void npmAfterAnEarlierRecipeChangedAMemberInEveryVisitOrder() {
        memberCompose(new Scenario("npm/override-recipe-member-compose"), PackageManager.Npm);
    }

    @Test
    void pnpmAfterAnEarlierRecipeChangedAMemberInEveryVisitOrder() {
        memberCompose(new Scenario("pnpm/override-recipe-member-compose"), PackageManager.Pnpm);
    }

    @Test
    void bunAfterAnEarlierRecipeChangedAMemberInEveryVisitOrder() {
        memberCompose(new Scenario("bun/override-recipe-member-compose"), PackageManager.Bun);
    }

    @Test
    void yarnClassicAfterAnEarlierRecipeChangedAMemberInEveryVisitOrder() {
        memberCompose(new Scenario("yarn-classic/override-recipe-member-compose"), PackageManager.YarnClassic);
    }

    // recorded with Bun 1.3.10: the root's override and a member's range in one install
    @Test
    void bunBeforeALaterRecipeChangesAMemberInEveryVisitOrder() {
        Recipe override = new UpgradeTransitiveDependencyVersion("wrappy", "1.0.1", null);
        Recipe upgrade = new UpgradeDependencyVersion("is-number", null, "^7.0.0");
        new CatalogRecording("upgrade-transitive", "bun", "override-recipe-then-member-upgrade")
          .assertEveryVisitOrder(override, upgrade);
        new CatalogRecording("upgrade-transitive", "bun", "override-recipe-then-member-upgrade")
          .assertEveryVisitOrder(upgrade, override);
    }

    @Test
    void pnpmAfterAnEarlierRecipeChangedTheReleaseInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-release-changed-earlier");
        s.release = "9.15.9";
        Recipe recipe = new CompositeRecipe(Arrays.asList(new ChangeValue("$.packageManager", "\"pnpm@11.2.2\""),
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null)));
        for (List<Source> order : orders(s.workspaceSources(PackageManager.Pnpm))) {
            List<String> paths = order.stream().map(source -> source.path).collect(toList());
            int root = paths.indexOf("package.json");
            if (root < paths.indexOf("pnpm-workspace.yaml") && root < paths.indexOf("pnpm-lock.yaml")) {
                inOrder(s, recipe, order);
            } else {
                overturned(s, recipe, order, "it is now pnpm-workspace.yaml for pnpm 11");
            }
        }
    }

    @Test
    void pnpmPinThenRepinCreatingTheWorkspaceManifestInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-repin");
        inEveryOrder(s, new CompositeRecipe(Arrays.asList(
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null),
          new UpgradeTransitiveDependencyVersion("is-number", "6.0.0", null))), Arrays.asList(
          s.manifest("package.json", PackageManager.Pnpm, null, "pkg-before", "pkg"),
          s.source("pnpm-lock.yaml", "before", "after"),
          s.generated("pnpm-workspace.yaml", "workspace")));
    }

    @Test
    void pnpmRepinBackToTheWorkspaceManifestAsItWasRegeneratesTheLockInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-repin-again");
        inEveryOrder(s, new CompositeRecipe(Arrays.asList(
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null),
          new UpgradeTransitiveDependencyVersion("is-number", "6.0.0", null))), Arrays.asList(
          s.manifest("package.json", PackageManager.Pnpm, null, "pkg-before", "pkg"),
          s.source("pnpm-workspace.yaml", "workspace-before", "workspace"),
          s.source("pnpm-lock.yaml", "before", "after")));
    }

    @Test
    void pnpmWorkspaceManifestAnEarlierRecipeLeftNotEditableInPlaceIsDeclinedOrWarnedInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-two-pins");
        Recipe recipe = new CompositeRecipe(Arrays.asList(new FindAndReplace("packages:\n  - apps/*",
          "packages:\n  - &apps apps/*", null, null, null, null, "pnpm-workspace.yaml", null),
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null)));
        for (List<Source> order : orders(s.workspaceSources(PackageManager.Pnpm))) {
            Outcome outcome = run(s, recipe, parse(order));
            String visited = visited(order);
            List<String> paths = order.stream().map(source -> source.path).collect(toList());
            boolean lockFirst = paths.indexOf("pnpm-lock.yaml") < paths.indexOf("pnpm-workspace.yaml");
            if (lockFirst) {
                assertThat(outcome.rows).as("visiting %s", visited).singleElement()
                  .extracting(NodeLockRegenerationFailures.Row::getReason)
                  .isEqualTo(LockFileRegeneration.Reason.RESOLUTION_REQUIRED.name());
                assertThat(outcome.file("pnpm-workspace.yaml").getMarkers().findFirst(Markup.Warn.class))
                  .as("visiting %s", visited).hasValueSatisfying(warn -> assertThat(warn.getMessage())
                    .contains("after this recipe wrote the lock from its earlier revision"));
            } else {
                assertThat(outcome.rows).as("visiting %s", visited).isEmpty();
                assertThat(outcome.declines).as("visiting %s", visited).singleElement()
                  .extracting(NodeDependencyEditsDeclined.Row::getReason).asString()
                  .contains("an anchor or a tag");
            }
            assertThat(print(outcome.file("pnpm-workspace.yaml"))).as("visiting %s", visited)
              .isEqualTo("packages:\n  - &apps apps/*\n");
            assertThat(print(outcome.file("package.json"))).as("visiting %s", visited)
              .isEqualTo(s.file("pkg-before"));
            // a lock visited first was written by the decision the anchored revision overturns, so it is warned: pnpm
            // 11.2.2's lock for the block-style workspace with the override (override-recipe-one-pin)
            if (lockFirst) {
                assertThat(print(outcome.file("pnpm-lock.yaml"))).as("visiting %s", visited)
                  .isEqualTo(new Scenario("pnpm/override-recipe-one-pin").file("after"));
            } else {
                assertThat(print(outcome.file("pnpm-lock.yaml"))).as("visiting %s", visited)
                  .isEqualTo(s.file("before"));
            }
        }
    }

    // a recipe outside the composition changes the root: a lock written before the root's visit is warned, one row
    @Test
    void npmAfterAPlainEditOfTheRootInEveryVisitOrder() {
        Scenario s = new Scenario("npm/override-recipe-compose");
        Recipe recipe = new CompositeRecipe(Arrays.asList(new ChangeValue("$.dependencies.is-odd", "\"3.0.1\""),
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null)));
        for (List<Source> order : orders(s.workspaceSources(PackageManager.Npm))) {
            List<String> paths = order.stream().map(source -> source.path).collect(toList());
            if (paths.indexOf("package.json") < paths.indexOf("package-lock.json")) {
                inOrder(s, recipe, order);
                continue;
            }
            String visited = visited(order);
            Outcome outcome = run(s, recipe, parse(order));
            assertThat(print(outcome.file("package.json"))).as("visiting %s", visited).isEqualTo(s.file("pkg"));
            assertThat(outcome.file("package.json").getMarkers().findFirst(Markup.Warn.class)).as("visiting %s",
              visited).hasValueSatisfying(warn -> assertThat(warn.getMessage()).contains("wrote the lock from"));
            assertThat(outcome.rows).as("visiting %s", visited).singleElement()
              .extracting(NodeLockRegenerationFailures.Row::getReason)
              .isEqualTo(LockFileRegeneration.Reason.RESOLUTION_REQUIRED.name());
        }
    }

    @Test
    void pnpmAfterAPlainEditOfTheRootInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-compose");
        Recipe recipe = new CompositeRecipe(Arrays.asList(new ChangeValue("$.dependencies.is-odd", "\"3.0.1\""),
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null)));
        for (List<Source> order : orders(s.workspaceSources(PackageManager.Pnpm))) {
            List<String> paths = order.stream().map(source -> source.path).collect(toList());
            if (paths.indexOf("package.json") < paths.indexOf("pnpm-lock.yaml")) {
                inOrder(s, recipe, order);
                continue;
            }
            String visited = visited(order);
            Outcome outcome = run(s, recipe, parse(order));
            assertThat(print(outcome.file("package.json"))).as("visiting %s", visited).isEqualTo(s.file("pkg"));
            assertThat(outcome.file("package.json").getMarkers().findFirst(Markup.Warn.class)).as("visiting %s",
              visited).hasValueSatisfying(warn -> assertThat(warn.getMessage()).contains("wrote the lock from"));
            assertThat(outcome.rows).as("visiting %s", visited).singleElement()
              .extracting(NodeLockRegenerationFailures.Row::getReason)
              .isEqualTo(LockFileRegeneration.Reason.RESOLUTION_REQUIRED.name());
        }
    }

    // the same for UpgradeDependencyVersion, which shares the engine: a plain edit adds the override npm recorded
    @Test
    void upgradeDependencyVersionAfterAPlainEditOfTheRootInEveryVisitOrder() {
        Scenario s = new Scenario("npm/override-recipe-compose");
        Recipe recipe = new CompositeRecipe(Arrays.asList(new RootOnly(new AddKeyValue("$", "overrides",
          "{\"is-number\": \"7.0.0\"}", null)), new UpgradeDependencyVersion("is-odd", null, "3.0.1")));
        for (List<Source> order : orders(s.workspaceSources(PackageManager.Npm))) {
            List<String> paths = order.stream().map(source -> source.path).collect(toList());
            String visited = visited(order);
            Outcome outcome = run(s, recipe, parse(order));
            SourceFile root = outcome.file("package.json");
            if (paths.indexOf("package.json") < paths.indexOf("package-lock.json")) {
                assertThat(print(outcome.file("package-lock.json"))).as("visiting %s", visited)
                  .isEqualTo(s.file("after"));
                noWarning(root);
                assertThat(outcome.rows).as("visiting %s", visited).isEmpty();
                continue;
            }
            assertThat(root.getMarkers().findFirst(Markup.Warn.class)).as("visiting %s", visited)
              .hasValueSatisfying(warn -> assertThat(warn.getMessage()).contains("wrote the lock from"));
            assertThat(outcome.rows).as("visiting %s", visited).singleElement()
              .extracting(NodeLockRegenerationFailures.Row::getKind).isEqualTo("CANNOT_KNOW");
        }
    }

    @Test
    void npmAfterAnEarlierRecipeChangedTheRootInEveryVisitOrder() {
        Scenario s = new Scenario("npm/override-recipe-compose");
        inEveryOrder(s, new CompositeRecipe(Arrays.asList(new UpgradeDependencyVersion("is-odd", null, "3.0.1"),
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null))), s.workspaceSources(PackageManager.Npm));
    }

    @Test
    void pnpm10NullPnpmFieldBesideResolutionsWritesResolutions() {
        otherPnpmRelease("pnpm/override-pnpm10-recipe-null-pnpm-resolutions", null, "10.34.6");
    }

    @Test
    void pnpm9NullPnpmFieldBesideResolutionsWritesResolutions() {
        otherPnpmRelease("pnpm/override-pnpm9-recipe-null-pnpm-resolutions", null, "9.15.9");
    }

    /**
     * pnpm 11.2.2 run directly installs itself beside devEngines naming 10.34.6 with onFail ignore, and corepack 0.35.0
     * starts 10.34.6 there (both recorded): which release writes is the host's to say, so the override is declined.
     */
    @Test
    void pnpmWhoseNamedReleaseCorepackWouldStartIsDeclined() {
        Scenario s = new Scenario("pnpm/override-on-fail-ignore");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", null)
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getReason()).contains("corepack starts pnpm 10.34.6"))),
          s.root(PackageManager.Pnpm, s.file("pkg-before")),
          s.lock(PackageManager.Pnpm, null)
        );
    }

    @Test
    void bunGlobal() {
        golden("bun/override-recipe-global", PackageManager.Bun, "7.0.0", null);
    }

    @Test
    void bunPathIsDeclined() {
        Scenario s = new Scenario("bun/override-recipe-path-nested");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-odd")
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getReason()).isEqualTo("bun does not support nested overrides"))),
          s.root(PackageManager.Bun, s.file("pkg-before")),
          s.lock(PackageManager.Bun, null)
        );
    }

    @Test
    void bunBesideTheResolutionsAlreadyPresent() {
        golden("bun/override-recipe-present", PackageManager.Bun, "7.0.0", null);
    }

    @Test
    void bunAtTheRootOfAMember() {
        golden("bun/override-recipe-member", PackageManager.Bun, "7.0.0", null);
    }

    @Test
    void yarnClassicGlobal() {
        golden("yarn-classic/override-recipe-global", PackageManager.YarnClassic, "7.0.0", null);
    }

    @Test
    void yarnClassicPath() {
        golden("yarn-classic/override-recipe-path", PackageManager.YarnClassic, "7.0.0", "is-odd");
    }

    @Test
    void yarnClassicBesideAResolutionAlreadyPresent() {
        golden("yarn-classic/override-recipe-present", PackageManager.YarnClassic, "7.0.0", null);
    }

    @Test
    void yarnClassicAtTheRootOfAMember() {
        golden("yarn-classic/override-recipe-member", PackageManager.YarnClassic, "7.0.0", null);
    }

    @Test
    void yarnBerryGlobal() {
        golden("yarn-berry/override-recipe-global", PackageManager.YarnBerry, "7.0.0", null);
    }

    @Test
    void yarnBerryPath() {
        golden("yarn-berry/override-recipe-path", PackageManager.YarnBerry, "7.0.0", "is-odd");
    }

    @Test
    void yarnBerryBesideAResolutionAlreadyPresent() {
        golden("yarn-berry/override-recipe-present", PackageManager.YarnBerry, "7.0.0", null);
    }

    @Test
    void yarnBerryAtTheRootOfAMember() {
        golden("yarn-berry/override-recipe-member", PackageManager.YarnBerry, "7.0.0", null);
    }

    /**
     * A host that parses a workspace member beside its installed node_modules marks it as npm's. It is still its
     * workspace's member: the override is the root's, where that package manager reads it, and the member gets none
     * of npm's.
     */
    @ParameterizedTest
    @CsvSource({"pnpm, Pnpm", "bun, Bun", "yarn-classic, YarnClassic", "yarn-berry, YarnBerry"})
    void aMemberMarkedAsNpmsIsStillItsWorkspacesMember(String family, PackageManager pm) {
        golden(family + "/override-recipe-member", pm, PackageManager.Npm, "7.0.0", null, null);
    }

    /**
     * In the same workspace an edit of a member's own dependency goes to the workspace's lock, whose regeneration
     * reports the workspace manifest it cannot read; the member is not a project of npm's with no lock to write.
     */
    @Test
    void anEditOfAMemberOfAWorkspaceWhoseManifestIsUnreadFailsWithTheWorkspacesLock() {
        Scenario s = new Scenario("pnpm/override-recipe-member");
        String member = requireNonNull(s.file("tree-before/" + MEMBER));
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("is-odd", null, "^4.0.0")).executionContext(s.ctx())
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> {
                  assertThat(row.getSourcePath()).isEqualTo(MEMBER);
                  assertThat(row.getKind()).isEqualTo(LockFileRegeneration.Kind.CANNOT_KNOW.toString());
                  assertThat(row.getDetail())
                    .isEqualTo("the engine cannot know pnpm-workspace.yaml (its contents were not read)");
              })),
          s.root(PackageManager.Pnpm, s.file("pkg-before")),
          packageJson(member, null, nodeResolutionResult(PackageManager.Npm, dependencies(member)),
            spec -> spec.path(MEMBER).noTrim().after(actual -> actual)),
          SourceSpecs.other(s.file("workspace-before"), spec -> spec.path("pnpm-workspace.yaml")),
          pnpmLock(requireNonNull(s.file("before")), null, spec -> spec.path("pnpm-lock.yaml").noTrim()
            .after(actual -> {
                assertThat(actual).endsWith(requireNonNull(s.file("before")));
                return actual;
            }))
        );
    }

    /**
     * A member the host parsed without its marker is its workspace's member too: its package manager reads its
     * manifest for the lock, and the override is the root's.
     */
    @ParameterizedTest
    @CsvSource({"npm, Npm", "pnpm, Pnpm", "bun, Bun", "yarn-classic, YarnClassic", "yarn-berry, YarnBerry"})
    void aMemberParsedWithoutItsMarkerIsStillItsWorkspacesMember(String family, PackageManager pm) {
        golden(family + "/override-recipe-member", pm, null, "7.0.0", null, null);
    }

    /**
     * urql's shape: the workspace manifest is a parse error and the members are marked as npm's. No member is edited
     * as a root of its own, and the root reports what it cannot know.
     */
    @Test
    void membersOfAWorkspaceWhoseManifestIsUnreadAreNotEditedAsRootsOfTheirOwn() {
        Scenario s = new Scenario("pnpm/override-recipe-member");
        String member = requireNonNull(s.file("tree-before/" + MEMBER));
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", null)
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> {
                  assertThat(row.getSourcePath()).isEqualTo("package.json");
                  assertThat(row.getDetail())
                    .isEqualTo("the engine cannot know pnpm-workspace.yaml (its contents were not read)");
              })),
          s.root(PackageManager.Pnpm, s.file("pkg-before")),
          packageJson(member, null, nodeResolutionResult(PackageManager.Npm, dependencies(member)),
            spec -> spec.path(MEMBER).noTrim()),
          SourceSpecs.other(s.file("workspace-before"), spec -> spec.path("pnpm-workspace.yaml")),
          s.lock(PackageManager.Pnpm, null)
        );
    }

    @Test
    void pnpm10WritesTheWorkspaceManifestAndTheLockIsCannotKnow() {
        otherPnpmRelease("pnpm/override-pnpm10-recipe-workspace", "is-odd", "10.34.6");
    }

    @Test
    void pnpm10WithAPnpmFieldWritesPnpmOverrides() {
        otherPnpmRelease("pnpm/override-pnpm10-recipe-pnpm-field", null, "10.34.6");
    }

    @Test
    void pnpm10BesideResolutionsWritesPnpmOverrides() {
        otherPnpmRelease("pnpm/override-pnpm10-recipe-beside-resolutions", null, "10.34.6");
    }

    @Test
    void pnpm10WithAPnpmFieldBesideWorkspaceOverridesWritesTheWorkspaceManifest() {
        otherPnpmRelease("pnpm/override-pnpm10-recipe-pnpm-field-workspace-overrides", null, "10.34.6");
    }

    @Test
    void pnpm9WritesPnpmOverrides() {
        otherPnpmRelease("pnpm/override-pnpm9-reads-pnpm-field", null, "9.15.9");
    }

    @Test
    void pnpmCannotReadTwoParents() {
        Scenario s = new Scenario("pnpm/override-recipe-path");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-even>is-odd")
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getReason()).contains("ERR_PNPM_INVALID_SELECTOR"))),
          s.root(PackageManager.Pnpm, s.file("pkg-before")),
          yaml(s.file("workspace-before"), spec -> spec.path("pnpm-workspace.yaml").noTrim()),
          s.lock(PackageManager.Pnpm, null)
        );
    }

    @Test
    void aWorkspaceManifestHeldWithoutItsContentsIsCannotKnow() {
        Scenario s = new Scenario("pnpm/override-recipe-path");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", null)
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail())
                .isEqualTo("the engine cannot know pnpm-workspace.yaml (its contents were not read)"))),
          s.root(PackageManager.Pnpm, s.file("pkg-before")),
          SourceSpecs.other(s.file("workspace-before"), spec -> spec.path("pnpm-workspace.yaml")),
          s.lock(PackageManager.Pnpm, null)
        );
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      Pnpm | {"packageManager": "pnpm@10.34.6", "pnpm": "hoist"}                 | pnpm is not an object
      Pnpm | {"packageManager": "pnpm@10.34.6", "pnpm": {"overrides": "none"}} | pnpm.overrides is not an object
      Npm  | {"overrides": []}                                                  | overrides is not an object
      """)
    void anOverridesFieldThatIsNoObjectIsDeclinedWithOneRowAndNothingElse(PackageManager pm, String manifest,
                                                                          String reason) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null))
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .extracting(NodeDependencyEditsDeclined.Row::getReason).isEqualTo("the root's " + reason))
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class)).isEmpty()),
          packageJson(manifest, null, nodeResolutionResult(pm),
            s -> s.afterRecipe(UpgradeTransitiveDependencyVersionTest::noWarning))
        );
    }

    @Test
    void yarnBerryCannotReadTwoParents() {
        Scenario s = new Scenario("yarn-berry/override-recipe-path");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-even>is-odd")
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getReason()).contains("YN0057"))),
          s.root(PackageManager.YarnBerry, s.file("pkg-before")),
          s.lock(PackageManager.YarnBerry, null)
        );
    }

    // Yarn 4.10.3 applies is-odd@^3.0.0/is-number nowhere: its lock beside that resolution is the one before it
    @Test
    void yarnBerryCannotMatchARangedParent() {
        Scenario s = new Scenario("yarn-berry/override-key-ranged-parent");
        assertThat(s.file("pkg")).contains("\"is-odd@^3.0.0/is-number\": \"7.0.0\"");
        assertThat(s.file("after")).isEqualTo(s.file("before"));
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-odd@^3.0.0")
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getReason()).contains("exact version only, not is-odd@^3.0.0"))),
          s.root(PackageManager.YarnBerry, s.file("pkg-before")),
          s.lock(PackageManager.YarnBerry, null)
        );
    }

    @Test
    void npmRejectingTheOverrideKeepsTheEditAndWarns() {
        Scenario s = new Scenario("npm/override-recipe-conflicts-direct");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", null)
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).contains("EOVERRIDE"))),
          s.root(PackageManager.Npm, s.file("pkg"), true),
          packageLock(s.file("before"), null, spec -> spec.noTrim().after(actual -> {
              assertThat(actual).contains("EOVERRIDE").endsWith(requireNonNull(s.file("before")));
              return actual;
          }))
        );
    }

    // yarn 1.22.22 applies **/is-odd@3.0.0/is-number nowhere: is-odd's is-number stays 6.0.0 beside that resolution
    @Test
    void yarnClassicCannotMatchAVersionedParent() {
        Scenario s = new Scenario("yarn-classic/override-key-versioned-parent");
        assertThat(s.file("pkg")).contains("\"**/is-odd@3.0.0/is-number\": \"7.0.0\"");
        assertThat(s.file("after")).contains("is-number@^6.0.0:\n  version \"6.0.0\"");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-odd@3.0.0")
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getReason()).contains("is-odd@3.0.0"))),
          s.root(PackageManager.YarnClassic, s.file("pkg-before")),
          s.lock(PackageManager.YarnClassic, null)
        );
    }

    // recorded with npm 11.17.0: the version is already the object's ".", so npm installs nothing new
    @Test
    void npmOverrideOfAnObjectAlreadyPinnedUnderDotIsLeftAlone() {
        Scenario s = new Scenario("npm/override-recipe-object-value-pinned");
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("is-odd", "0.1.2", null))
            .executionContext(s.ctx()),
          s.root(PackageManager.Npm, s.file("pkg")),
          s.lock(PackageManager.Npm, s.file("after"))
        );
    }

    // recorded with yarn 1.22.22, which applies the first resolution whose glob matches: the one in place is edited
    @Test
    void yarnClassicEditsTheResolutionOfTheSameGlobInPlace() {
        golden("yarn-classic/override-recipe-glob-key-present", PackageManager.YarnClassic, "7.0.0", null);
    }

    // recorded with Bun 1.3.10, which applies the resolution written after "**/is-number"
    @Test
    void bunAppliesAResolutionWrittenBesideAGlobKey() {
        golden("bun/override-recipe-glob-key-present", PackageManager.Bun, "7.0.0", null);
    }

    // recorded with Yarn 4.10.3 and pnpm 11.2.2: the override scoped to is-odd still applies there, and is named
    @Test
    void yarnBerryNamesAResolutionScopedToAParentThatStillApplies() {
        golden("yarn-berry/override-recipe-parent-key-present", PackageManager.YarnBerry, "7.0.0", null,
          "Yarn applies is-odd/is-number before a global override of is-number");
    }

    // recorded with yarn 1.22.22, which applies the first resolution whose glob matches: is-odd's is-number stays 6.0.0
    @Test
    void yarnClassicNamesAResolutionScopedToAParentListedBeforeTheGlobalOne() {
        golden("yarn-classic/override-recipe-parent-key-before-global", PackageManager.YarnClassic, "7.0.0", null,
          "yarn applies is-odd/is-number before a global override of is-number");
    }

    // pnpm 11.2.2's root-finder stops at a misspelt workspace manifest, findWorkspaceDir rejects it
    @Test
    void pnpmStopsAtAMisspeltWorkspaceManifest() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null))
            .dataTable(NodeDependencyEditsDeclined.Row.class, rows -> assertThat(rows).singleElement()
              .extracting(NodeDependencyEditsDeclined.Row::getReason).isEqualTo("pnpm finds pnpm-workspace.yml " +
                "where it reads pnpm-workspace.yaml (ERR_PNPM_BAD_WORKSPACE_MANIFEST_NAME)")),
          packageJson("{\"name\":\"p\",\"dependencies\":{\"is-odd\":\"3.0.1\"}}", null,
            nodeResolutionResult(PackageManager.Pnpm), s -> s.noTrim()),
          text("packages:\n  - .\n", s -> s.path("pnpm-workspace.yml"))
        );
    }

    @Test
    void pnpmNamesAnOverrideScopedToAParentThatStillApplies() {
        golden("pnpm/override-recipe-parent-selector-present", PackageManager.Pnpm, "7.0.0", null,
          "pnpm applies is-odd>is-number before a global override of is-number");
    }

    private void golden(String scenario, PackageManager pm, String version, @Nullable String path) {
        golden(scenario, pm, version, path, null);
    }

    private void golden(String scenario, PackageManager pm, String version, @Nullable String path,
                        @Nullable String declined) {
        golden(scenario, pm, pm, version, path, declined);
    }

    // memberPm: the package manager the member's marker names; null for a member parsed without its marker
    private void golden(String scenario, PackageManager pm, @Nullable PackageManager memberPm, String version,
                        @Nullable String path, @Nullable String declined) {
        Scenario s = new Scenario(scenario);
        List<SourceSpecs> sources = new ArrayList<>();
        sources.add(s.root(pm, s.file("pkg")));
        String member = s.file("tree-before/" + MEMBER);
        if (member != null) {
            sources.add(memberPm == null ? json(member, spec -> spec.path(MEMBER).noTrim()) :
              packageJson(member, null, nodeResolutionResult(memberPm, dependencies(member)),
                spec -> spec.path(MEMBER).noTrim()));
        }
        if (s.file("workspace") != null) {
            sources.add(s.workspace());
        }
        String lockDir = s.file("lock-dir");
        if (lockDir == null) {
            sources.add(s.lock(pm, s.file("after")));
        } else {
            sources.add(s.lock(pm, "probes/before-root-lock", s.file("probes/after-root-lock"), "pnpm-lock.yaml"));
            sources.add(s.lock(pm, "before", s.file("after"), lockDir.trim() + "/pnpm-lock.yaml"));
        }
        rewriteRun(spec -> {
            if (declined == null) {
                s.spec(spec, version, path).afterRecipe(run ->
                  assertThat(run.getDataTableRows(NodeDependencyEditsDeclined.class)).isEmpty());
            } else {
                s.spec(spec, version, path).dataTable(NodeDependencyEditsDeclined.Row.class, rows ->
                  assertThat(rows).singleElement().extracting(NodeDependencyEditsDeclined.Row::getReason)
                    .isEqualTo(declined));
            }
        }, sources.toArray(new SourceSpecs[0]));
    }

    private static void memberCompose(Scenario s, PackageManager pm) {
        inEveryOrder(s, new CompositeRecipe(Arrays.asList(new UpgradeDependencyVersion("is-odd", null, "3.0.1"),
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null))), s.workspaceSources(pm));
    }

    // every order of the sources the run visits; a generated one is expected in each
    private static void inEveryOrder(Scenario s, Recipe recipe, List<Source> sources) {
        List<Source> generated = sources.stream().filter(source -> source.before == null).collect(toList());
        List<Source> parsed = sources.stream().filter(source -> source.before != null).collect(toList());
        for (List<Source> order : orders(parsed)) {
            order.addAll(generated);
            inOrder(s, recipe, order);
        }
    }

    // RewriteTest visits the sources grouped by parser, so this run fixes the order itself: one cycle, as a host
    // runs the recipe, then a fresh run over its output, which changes nothing
    private static void inOrder(Scenario s, Recipe recipe, List<Source> order) {
        String visited = visited(order);
        Outcome first = run(s, recipe, parse(order));
        for (Source source : order) {
            SourceFile out = first.file(source.path);
            noWarning(out);
            assertThat(out.printAll()).as("%s, visiting %s", source.path, visited).isEqualTo(source.after);
        }
        assertThat(first.generated).as("generated, visiting %s", visited).containsExactlyInAnyOrderElementsOf(
          order.stream().filter(source -> source.before == null).map(source -> Paths.get(source.path))
            .collect(toList()));
        assertThat(first.rows).as("visiting %s", visited).isEmpty();
        assertThat(first.declines).as("visiting %s", visited).isEmpty();
        Outcome second = run(s, recipe, new ArrayList<>(first.files.values()));
        for (Map.Entry<Path, SourceFile> file : second.files.entrySet()) {
            assertThat(file.getValue().printAll()).as("a second run over %s, visiting %s", file.getKey(), visited)
              .isEqualTo(first.files.get(file.getKey()).printAll());
        }
        assertThat(second.generated).as("a second run, visiting %s", visited).isEmpty();
    }

    // files written by a decision an earlier recipe overturned later in the visit: nothing more is written, the
    // root is warned, one row
    private static void overturned(Scenario s, Recipe recipe, List<Source> order, String overturnedTo) {
        String visited = visited(order);
        Outcome outcome = run(s, recipe, parse(order));
        for (Source source : order) {
            String expected = "package.json".equals(source.path) ? source.after : source.before;
            assertThat(print(outcome.file(source.path))).as("%s, visiting %s", source.path, visited)
              .isEqualTo(expected);
        }
        assertThat(outcome.file("package.json").getMarkers().findFirst(Markup.Warn.class)).as("visiting %s", visited)
          .hasValueSatisfying(warn -> assertThat(warn.getMessage()).contains(overturnedTo));
        assertThat(outcome.rows).as("visiting %s", visited).hasSize(1);
    }

    private static Outcome run(Scenario s, Recipe recipe, List<SourceFile> before) {
        Outcome outcome = new Outcome();
        for (SourceFile source : before) {
            outcome.files.put(source.getSourcePath(), source);
        }
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(before), s.ctx(), 1, 1);
        for (Result result : run.getChangeset().getAllResults()) {
            SourceFile after = requireNonNull(result.getAfter());
            if (result.getBefore() == null) {
                outcome.generated.add(after.getSourcePath());
            }
            outcome.files.put(after.getSourcePath(), after);
        }
        outcome.rows = run.getDataTableRows(NodeLockRegenerationFailures.class);
        outcome.declines = run.getDataTableRows(NodeDependencyEditsDeclined.class);
        return outcome;
    }

    private static List<SourceFile> parse(List<Source> order) {
        List<SourceFile> parsed = new ArrayList<>();
        for (Source source : order) {
            if (source.before != null) {
                parsed.add(source.parse());
            }
        }
        return parsed;
    }

    private static String visited(List<Source> order) {
        return order.stream().filter(source -> source.before != null).map(source -> source.path)
          .collect(joining(", "));
    }

    private static String print(SourceFile file) {
        return file.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }

    /**
     * The files after a run, by path, those it generated, and its failure rows.
     */
    private static final class Outcome {
        final Map<Path, SourceFile> files = new LinkedHashMap<>();
        final List<Path> generated = new ArrayList<>();
        List<NodeLockRegenerationFailures.Row> rows = new ArrayList<>();
        List<NodeDependencyEditsDeclined.Row> declines = new ArrayList<>();

        SourceFile file(String path) {
            return requireNonNull(files.get(Paths.get(path)), path);
        }
    }

    private static <T> List<List<T>> orders(List<T> items) {
        List<List<T>> orders = new ArrayList<>();
        if (items.isEmpty()) {
            orders.add(new ArrayList<>());
        }
        for (int i = 0; i < items.size(); i++) {
            List<T> rest = new ArrayList<>(items);
            T first = rest.remove(i);
            for (List<T> order : orders(rest)) {
                order.add(0, first);
                orders.add(order);
            }
        }
        return orders;
    }

    // the edit is kept and the lock warned: pnpm 11.2.2 hands the install to the release the project names
    private void otherPnpmRelease(String scenario, @Nullable String path, String release) {
        Scenario s = new Scenario(scenario);
        s.release = release;
        List<SourceSpecs> sources = new ArrayList<>();
        sources.add(s.root(PackageManager.Pnpm, s.file("pkg"), true));
        if (s.file("workspace") != null) {
            sources.add(s.workspace());
        }
        sources.add(pnpmLock(s.file("before"), null, spec -> spec.noTrim().afterRecipe(lock ->
          assertThat(lock.getMarkers().findFirst(Markup.Warn.class)).hasValueSatisfying(warn ->
            assertThat(warn.getMessage()).contains("the lock pnpm " + release + " writes")))));
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", path)
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).contains("the lock pnpm " + release + " writes"))),
          sources.toArray(new SourceSpecs[0])
        );
    }

    private static void noWarning(SourceFile file) {
        assertThat(file.getMarkers().findFirst(Markup.Warn.class)).as("a regenerated file must not warn").isEmpty();
    }

    private static Dependency[] dependencies(String manifest) {
        try {
            Map<String, Object> pkg = new ObjectMapper().readValue(manifest, new TypeReference<Map<String, Object>>() {
            });
            @SuppressWarnings("unchecked")
            Map<String, String> deps = (Map<String, String>) pkg.getOrDefault("dependencies", new LinkedHashMap<>());
            List<Dependency> out = new ArrayList<>();
            for (Map.Entry<String, String> dep : deps.entrySet()) {
                out.add(dependency(dep.getKey(), dep.getValue()));
            }
            return out.toArray(new Dependency[0]);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Runs a recipe on the root package.json only.
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    static class RootOnly extends Recipe {
        Recipe recipe;

        @Override
        public String getDisplayName() {
            return "Edit the root package.json only";
        }

        @Override
        public String getDescription() {
            return "Runs a recipe on the root package.json only.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(new FindSourceFiles("package.json").getVisitor(), recipe.getVisitor());
        }
    }

    /**
     * A file of a run in a fixed visit order: its text before (null for one the run generates) and the text the
     * recording ends with.
     */
    private static final class Source {
        final String path;
        final @Nullable String before;
        final String after;
        final @Nullable NodeResolutionResult marker;

        Source(String path, @Nullable String before, String after, @Nullable NodeResolutionResult marker) {
            this.path = path;
            this.before = before;
            this.after = after;
            this.marker = marker;
        }

        SourceFile parse() {
            Parser parser = path.endsWith(".json") ? JsonParser.builder().build() : path.endsWith(".yaml") ?
              YamlParser.builder().build() : PlainTextParser.builder().build();
            SourceFile parsed = parser.parse(new InMemoryExecutionContext(t -> {
                throw new RuntimeException(t);
            }), requireNonNull(before)).findFirst().orElseThrow(IllegalStateException::new)
              .withSourcePath(Paths.get(path));
            return marker == null ? parsed : parsed.withMarkers(parsed.getMarkers().add(marker));
        }
    }

    /**
     * A recording: its files, and a registry serving its {@code http/} by path whatever the registry's host.
     */
    private static final class Scenario {
        final String dir;
        @Nullable String release;

        Scenario(String scenario) {
            this.dir = RESOURCES + scenario + "/";
        }

        RecipeSpec spec(RecipeSpec spec, String version, @Nullable String path) {
            return spec.recipe(new UpgradeTransitiveDependencyVersion("is-number", version, path))
              .executionContext(ctx());
        }

        SourceSpecs root(PackageManager pm, @Nullable String after) {
            return root(pm, after, false);
        }

        // a manifest the edit changes is warned when the lock is not regenerated
        SourceSpecs root(PackageManager pm, @Nullable String after, boolean warned) {
            String before = requireNonNull(file("pkg-before"));
            List<String> members = file("tree-before/" + MEMBER) == null ? null : singletonList(MEMBER);
            NodeResolutionResult marker = nodeResolutionResult(pm, members, dependencies(before));
            if (warned && !before.equals(after)) {
                return packageJson(before, null, marker, spec -> spec.noTrim().after(actual -> {
                    assertThat(actual).contains("lock regeneration failed").endsWith(requireNonNull(after));
                    return actual;
                }));
            }
            return packageJson(before, before.equals(after) ? null : after, marker, spec -> spec.noTrim()
              .afterRecipe(UpgradeTransitiveDependencyVersionTest::noWarning));
        }

        Source source(String path, String before, String after) {
            return new Source(path, requireNonNull(file(before)), requireNonNull(file(after)), null);
        }

        Source generated(String path, String after) {
            return new Source(path, null, requireNonNull(file(after)), null);
        }

        Source manifest(String path, PackageManager pm, @Nullable List<String> members, String before, String after) {
            String text = requireNonNull(file(before));
            return new Source(path, text, requireNonNull(file(after)), nodeResolutionResult(pm, members,
              dependencies(text)));
        }

        // the root, its member apps/a (sorting before the root), pnpm-workspace.yaml when recorded, the root's lock
        List<Source> workspaceSources(PackageManager pm) {
            String member = requireNonNull(file("tree-before/" + APPS_MEMBER));
            String memberAfter = file("tree/" + APPS_MEMBER);
            List<Source> sources = new ArrayList<>();
            sources.add(manifest("package.json", pm, singletonList(APPS_MEMBER), "pkg-before", "pkg"));
            sources.add(new Source(APPS_MEMBER, member, memberAfter == null ? member : memberAfter,
              nodeResolutionResult(pm, dependencies(member))));
            if (file("workspace-before") != null) {
                sources.add(source("pnpm-workspace.yaml", "workspace-before", "workspace"));
            }
            sources.add(source(lockPath(pm), "before", "after"));
            return sources;
        }

        SourceSpecs workspace() {
            String before = file("workspace-before");
            String after = file("workspace");
            return before != null && before.equals(after) ?
              yaml(before, spec -> spec.path("pnpm-workspace.yaml").noTrim()) :
              yaml(before, after, spec -> spec.path("pnpm-workspace.yaml").noTrim());
        }

        SourceSpecs lock(PackageManager pm, @Nullable String after) {
            return lock(pm, "before", after, lockPath(pm));
        }

        String lockPath(PackageManager pm) {
            return pm == PackageManager.Npm ? "package-lock.json" : pm == PackageManager.Pnpm ? "pnpm-lock.yaml" :
              pm == PackageManager.Bun ? "bun.lock" : "yarn.lock";
        }

        SourceSpecs lock(PackageManager pm, String beforeFile, @Nullable String after, String lockPath) {
            String before = requireNonNull(file(beforeFile));
            String expected = before.equals(after) ? null : after;
            switch (pm) {
                case Npm:
                    return packageLock(before, expected, spec -> spec.path(lockPath).noTrim()
                      .afterRecipe(UpgradeTransitiveDependencyVersionTest::noWarning));
                case Pnpm:
                    return pnpmLock(before, expected, spec -> spec.path(lockPath).noTrim()
                      .afterRecipe(UpgradeTransitiveDependencyVersionTest::noWarning));
                default:
                    return expected == null ? text(before, spec -> spec.path(lockPath).noTrim()) :
                            text(before, expected, spec -> spec.path(lockPath).noTrim()
                              .afterRecipe(UpgradeTransitiveDependencyVersionTest::noWarning));
            }
        }

        ExecutionContext ctx() {
            ExecutionContext ctx = new InMemoryExecutionContext(t -> {
                throw new RuntimeException(t);
            });
            HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
                String accept = String.valueOf(request.getRequestHeaders().get("Accept"));
                byte[] body = route(request.getUrl().getPath(), !accept.contains("vnd.npm.install-v1"));
                return new HttpSender.Response(body == null ? 404 : 200,
                  new ByteArrayInputStream(body == null ? new byte[0] : body), () -> {
                });
            });
            NodeExecutionContextView.view(ctx)
              .setRegistries(singletonList(new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null,
                false, null, true, false)))
              .setNodeVersion("v24.19.0")
              .setPlatform("darwin", "arm64", null);
            return ctx;
        }

        private byte @Nullable [] route(String rawPath, boolean full) {
            String path;
            try {
                path = URLDecoder.decode(rawPath.substring(1), "UTF-8");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (path.contains("/-/")) {
                return bytes("http/" + path.substring(path.lastIndexOf('/') + 1));
            }
            String[] parts = path.split("/");
            int nameParts = path.startsWith("@") ? 2 : 1;
            String name = String.join("/", Arrays.copyOfRange(parts, 0, nameParts));
            if (parts.length > nameParts) {
                return bytes("http/" + name + "-" + parts[nameParts]);
            }
            if (release != null && (name.equals("pnpm") || name.equals("@pnpm/exe"))) {
                return releaseDocument(name);
            }
            String packument = file("http/" + name);
            return packument == null ? null : (full ? withVersionDocuments(name, packument) : packument)
              .getBytes(StandardCharsets.UTF_8);
        }

        // what a full packument holds of the versions recorded: their version documents
        private String withVersionDocuments(String name, String packument) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode doc = (ObjectNode) mapper.readTree(packument);
                if (doc.get("versions") instanceof ObjectNode) {
                    ObjectNode versions = (ObjectNode) doc.get("versions");
                    for (Iterator<String> it = versions.fieldNames(); it.hasNext(); ) {
                        String version = it.next();
                        String manifest = file("http/" + name + "-" + version);
                        if (manifest != null) {
                            versions.set(version, mapper.readTree(manifest));
                        }
                    }
                }
                return mapper.writeValueAsString(doc);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // pnpm and @pnpm/exe at the one release, as the port resolves the release a project names
        private byte[] releaseDocument(String name) {
            String file = name.substring(name.indexOf('/') + 1);
            return ("{\"name\":\"" + name + "\",\"dist-tags\":{\"latest\":\"" + release + "\"},\"versions\":{\"" +
                    release + "\":{\"name\":\"" + name + "\",\"version\":\"" + release + "\",\"dist\":{\"integrity\":" +
                    "\"sha512-AA==\",\"tarball\":\"https://registry.npmjs.org/" + name + "/-/" + file + "-" + release +
                    ".tgz\"}}}}").getBytes(StandardCharsets.UTF_8);
        }

        @Nullable
        String file(String name) {
            byte[] bytes = bytes(name);
            return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
        }

        private byte @Nullable [] bytes(String name) {
            try (InputStream in = UpgradeTransitiveDependencyVersionTest.class.getClassLoader()
              .getResourceAsStream(dir + name)) {
                if (in == null) {
                    return null;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}

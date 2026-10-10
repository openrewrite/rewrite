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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.JsonParser;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;
import static org.openrewrite.javascript.Assertions.pnpmLock;
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

    @Test
    void pnpmTwoOverridesInOneRunInEveryVisitOrder() {
        Scenario s = new Scenario("pnpm/override-recipe-two-pins");
        inEveryOrder(s, new CompositeRecipe(Arrays.asList(
          new UpgradeTransitiveDependencyVersion("is-number", "7.0.0", null),
          new UpgradeTransitiveDependencyVersion("left-pad", "1.3.0", null))), s.workspaceSources(PackageManager.Pnpm));
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

    @Test
    void pnpmWhoseNamedReleaseLetsThePortedOneInstall() {
        golden("pnpm/override-on-fail-ignore", PackageManager.Pnpm, "7.0.0", null);
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
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).isEqualTo("bun does not support nested overrides"))),
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
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).contains("ERR_PNPM_INVALID_SELECTOR"))),
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

    @Test
    void yarnBerryCannotReadTwoParents() {
        Scenario s = new Scenario("yarn-berry/override-recipe-path");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-even>is-odd")
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).contains("YN0057"))),
          s.root(PackageManager.YarnBerry, s.file("pkg-before")),
          s.lock(PackageManager.YarnBerry, null)
        );
    }

    @Test
    void yarnBerryCannotMatchARangedParent() {
        Scenario s = new Scenario("yarn-berry/override-recipe-path");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-odd@^3.0.0")
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).contains("exact version only, not is-odd@^3.0.0"))),
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

    @Test
    void yarnClassicCannotMatchAVersionedParent() {
        Scenario s = new Scenario("yarn-classic/override-recipe-path");
        rewriteRun(
          spec -> s.spec(spec, "7.0.0", "is-odd@3.0.1")
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> assertThat(row.getDetail()).contains("is-odd@3.0.1"))),
          s.root(PackageManager.YarnClassic, s.file("pkg-before")),
          s.lock(PackageManager.YarnClassic, null)
        );
    }

    private void golden(String scenario, PackageManager pm, String version, @Nullable String path) {
        Scenario s = new Scenario(scenario);
        List<SourceSpecs> sources = new ArrayList<>();
        sources.add(s.root(pm, s.file("pkg")));
        String member = s.file("tree-before/" + MEMBER);
        if (member != null) {
            sources.add(packageJson(member, null, nodeResolutionResult(pm, dependencies(member)),
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
        rewriteRun(spec -> s.spec(spec, version, path), sources.toArray(new SourceSpecs[0]));
    }

    private static void inEveryOrder(Scenario s, Recipe recipe, List<Source> sources) {
        for (List<Source> order : orders(sources)) {
            inOrder(s, recipe, order);
        }
    }

    // RewriteTest visits the sources grouped by parser, so this run fixes the order itself, over two cycles
    private static void inOrder(Scenario s, Recipe recipe, List<Source> order) {
        List<SourceFile> before = new ArrayList<>();
        for (Source source : order) {
            before.add(source.parse());
        }
        Map<Path, SourceFile> after = new HashMap<>();
        for (Result result : recipe.run(new InMemoryLargeSourceSet(before), s.ctx(), 2, 2).getChangeset()
          .getAllResults()) {
            if (result.getAfter() != null) {
                after.put(result.getAfter().getSourcePath(), result.getAfter());
            }
        }
        String visited = order.stream().map(source -> source.path).collect(joining(", "));
        for (Source source : order) {
            SourceFile out = after.get(Paths.get(source.path));
            if (out != null) {
                noWarning(out);
            }
            assertThat(out == null ? source.before : out.printAll()).as("%s, visiting %s", source.path, visited)
              .isEqualTo(source.after);
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
     * A file of a run in a fixed visit order: its text before and the text the recording ends with.
     */
    private static final class Source {
        final String path;
        final String before;
        final String after;
        final @Nullable NodeResolutionResult marker;

        Source(String path, String before, String after, @Nullable NodeResolutionResult marker) {
            this.path = path;
            this.before = before;
            this.after = after;
            this.marker = marker;
        }

        SourceFile parse() {
            Parser parser = path.endsWith(".json") ? JsonParser.builder().build() : YamlParser.builder().build();
            SourceFile parsed = parser.parse(new InMemoryExecutionContext(t -> {
                throw new RuntimeException(t);
            }), before).findFirst().orElseThrow(IllegalStateException::new).withSourcePath(Paths.get(path));
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

        Source manifest(String path, PackageManager pm, @Nullable List<String> members, String before, String after) {
            String text = requireNonNull(file(before));
            return new Source(path, text, requireNonNull(file(after)), nodeResolutionResult(pm, members,
              dependencies(text)));
        }

        // the root, its member apps/a (sorting before the root), pnpm-workspace.yaml when recorded, the root's lock
        List<Source> workspaceSources(PackageManager pm) {
            String member = requireNonNull(file("tree-before/" + APPS_MEMBER));
            List<Source> sources = new ArrayList<>();
            sources.add(manifest("package.json", pm, singletonList(APPS_MEMBER), "pkg-before", "pkg"));
            sources.add(new Source(APPS_MEMBER, member, member, nodeResolutionResult(pm, dependencies(member))));
            if (file("workspace-before") != null) {
                sources.add(source("pnpm-workspace.yaml", "workspace-before", "workspace"));
            }
            sources.add(source(pm == PackageManager.Npm ? "package-lock.json" : "pnpm-lock.yaml", "before", "after"));
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
            return lock(pm, "before", after, pm == PackageManager.Npm ? "package-lock.json" :
              pm == PackageManager.Pnpm ? "pnpm-lock.yaml" : pm == PackageManager.Bun ? "bun.lock" : "yarn.lock");
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

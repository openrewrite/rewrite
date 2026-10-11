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
package org.openrewrite.javascript.internal;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class NodeDependencyScanTest {

    private static final Function<Path, @Nullable String> SCANNED = path -> null;

    @Test
    void everyVisitedPathIsHeldAndOnlyAReadPackageManagerFileHasContents() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path read = Paths.get("app/patches/ms@2.1.3.patch");
        Path unread = Paths.get("app/patches/debug@4.4.3.patch");
        Path source = Paths.get("app/src/index.js");

        boolean readKept = NodeDependencyScan.capturePackageManagerFile(acc,
          PlainText.builder().sourcePath(read).text("--- a/index.js\n+++ b/index.js\n").build());
        boolean unreadKept = NodeDependencyScan.capturePackageManagerFile(acc,
          new Quark(Tree.randomId(), unread, Markers.EMPTY, null, null));
        boolean sourceKept = NodeDependencyScan.capturePackageManagerFile(acc,
          PlainText.builder().sourcePath(source).text("x").build());

        assertThat(readKept).isTrue();
        assertThat(unreadKept).isTrue();
        assertThat(sourceKept).isFalse();
        assertThat(acc.paths).containsExactlyInAnyOrder(read, unread, source);
        assertThat(acc.packageManagerFiles).containsOnlyKeys(read);
    }

    /**
     * A file the host's parser gave up on is a parse error holding its text, which the package manager's own reader
     * judges; one the host could not read either, as when its parser process died first, holds none and is unread.
     */
    @Test
    void aParseErrorHasTheContentsItHolds() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path withText = Paths.get("pnpm-workspace.yaml");
        Path withoutText = Paths.get("examples/pnpm-workspace.yaml");

        NodeDependencyScan.capturePackageManagerFile(acc, parseError(withText, "packages:\n  - &a 'packages/*'\n"));
        NodeDependencyScan.capturePackageManagerFile(acc, parseError(withoutText, ""));

        assertThat(acc.paths).containsExactlyInAnyOrder(withText, withoutText);
        assertThat(acc.packageManagerFiles).containsOnly(entry(withText, "packages:\n  - &a 'packages/*'\n"));
    }

    private static ParseError parseError(Path path, String text) {
        return new ParseError(Tree.randomId(), Markers.EMPTY, path, null, null, false, null, text, null);
    }

    /**
     * Yarn classic reads .yarnrc and .npmrc from every directory above the project as well as its own.
     */
    @Test
    void theDirectoriesAboveTheProjectGiveTheirFilesAndPaths() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path manifest = Paths.get("repo/app/package.json");
        NodeDependencyScan.ProjectState ps = new NodeDependencyScan.ProjectState();
        ps.capturedPackageJson = PlainText.builder().sourcePath(manifest).text("{\"name\":\"p\"}").build();
        acc.projects.put(manifest, ps);
        acc.lockToPackage.put(Paths.get("repo/app/yarn.lock"), manifest);
        Path parentYarnrc = Paths.get("repo/.yarnrc");
        Path rootNpmrc = Paths.get(".npmrc");
        Path ownNpmrc = Paths.get("repo/app/.npmrc");
        Path readme = Paths.get("README.md");
        for (Path path : new Path[]{parentYarnrc, rootNpmrc, ownNpmrc, readme, Paths.get("repo/other/.npmrc"),
          Paths.get("repo/docs/guide.md")}) {
            NodeDependencyScan.capturePackageManagerFile(acc, PlainText.builder().sourcePath(path).text("x=y").build());
        }

        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, manifest);

        assertThat(files.getPackageManagerFiles()).containsOnlyKeys(parentYarnrc, rootNpmrc, ownNpmrc);
        assertThat(files.getPaths()).containsExactlyInAnyOrder(parentYarnrc, rootNpmrc, ownNpmrc, readme);
    }

    /**
     * A package manager maps its project's workspaces over the package.json files under the project, and those of each
     * package.json above it over the ones under that; Yarn classic also reads the packageManager of the nearest.
     */
    @Test
    void everyPackageJsonUnderTheProjectAndUnderAPackageJsonAboveItIsKept() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path manifest = Paths.get("repo/app/package.json");
        acc.lockToPackage.put(Paths.get("repo/app/yarn.lock"), manifest);
        Path parent = Paths.get("repo/package.json");
        Path top = Paths.get("package.json");
        Path other = Paths.get("repo/other/package.json");
        Path nested = Paths.get("repo/app/lib/package.json");
        String[] names = {"app", "repo", "top", "other", "lib"};
        Path[] paths = {manifest, parent, top, other, nested};
        for (int i = 0; i < paths.length; i++) {
            NodeDependencyScan.ProjectState ps = new NodeDependencyScan.ProjectState();
            ps.capturedPackageJson = PlainText.builder().sourcePath(paths[i]).text("{\"name\":\"" + names[i] + "\"}")
              .build();
            acc.projects.put(paths[i], ps);
            acc.paths.add(paths[i]);
        }

        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, manifest);

        assertThat(files.getManifests()).containsOnlyKeys(manifest, nested);
        assertThat(files.getPackageManagerFiles()).containsOnlyKeys(parent, top, other);
        assertThat(files.getPackageManagerFiles().get(parent)).isEqualTo("{\"name\":\"repo\"}");
    }

    /**
     * A file:, link: or portal: dependency, and a bare path, make the package manager read the package.json of the
     * directory it names, and that one's in turn, wherever the repository holds them.
     */
    @Test
    void thePackageJsonOfADirectoryADependencyNamesIsKeptWhereverItIs() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path manifest = Paths.get("apps/web/package.json");
        acc.lockToPackage.put(Paths.get("apps/web/package-lock.json"), manifest);
        Path lib = Paths.get("libs/ui/package.json");
        Path theme = Paths.get("libs/theme/package.json");
        Path linked = Paths.get("tools/lint/package.json");
        Path unread = Paths.get("libs/icons/package.json");
        Path unrelated = Paths.get("libs/unrelated/package.json");
        String[] texts = {
          "{\"name\":\"web\",\"dependencies\":{\"ui\":\"file:../../libs/ui\",\"lint\":\"link:../../tools/lint\"," +
          "\"icons\":\"../../libs/icons\",\"outside\":\"file:../../../elsewhere\",\"left-pad\":\"^1.3.0\"}}",
          "{\"name\":\"ui\",\"dependencies\":{\"theme\":\"file:../theme\"}}",
          "{\"name\":\"theme\"}", "{\"name\":\"lint\"}", "{\"name\":\"unrelated\"}"};
        Path[] paths = {manifest, lib, theme, linked, unrelated};
        for (int i = 0; i < paths.length; i++) {
            NodeDependencyScan.ProjectState ps = new NodeDependencyScan.ProjectState();
            ps.capturedPackageJson = new JsonParser().parse(texts[i]).findFirst().orElseThrow()
              .withSourcePath(paths[i]);
            acc.projects.put(paths[i], ps);
            acc.paths.add(paths[i]);
        }
        acc.paths.add(unread);

        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, manifest);

        assertThat(files.getManifests()).containsOnlyKeys(manifest);
        assertThat(files.getPackageManagerFiles()).containsOnlyKeys(lib, theme, linked);
        assertThat(files.getPaths()).containsExactlyInAnyOrder(manifest, lib, theme, linked, unread);
    }

    /**
     * npm, Yarn classic, pnpm and Bun run in a member with a lock of its own install at the workspace root and write
     * its lock; where the package manager's walk says so, the regeneration runs it in the member.
     */
    @ParameterizedTest
    @CsvSource({"Npm, package-lock.json", "YarnClassic, yarn.lock", "Pnpm, pnpm-lock.yaml", "Bun, bun.lock"})
    void aMemberWithItsOwnLockRegeneratesTheWorkspaceRootsLock(PackageManager pm, String lockName) {
        NodeDependencyScan.Accumulator acc = workspaceWithMemberLock(pm, pm, lockName, lockName);
        if (pm == PackageManager.Pnpm) {
            NodeDependencyScan.capturePackageManagerFile(acc, PlainText.builder()
              .sourcePath(Paths.get("pnpm-workspace.yaml")).text("packages:\n  - 'packages/*'\n").build());
        }
        Path rootLock = Paths.get(lockName);
        Path memberLock = Paths.get("packages/a/" + lockName);

        NodeDependencyScan.linkWorkspaceMembers(acc);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(rootLock);
        assertThat(files.getManifests()).containsOnlyKeys(ROOT, MEMBER);
        assertThat(acc.projects.get(MEMBER).capturedLockContent).isEqualTo("root lock");
        assertThat(NodeDependencyScan.lockImporters(acc, rootLock)).containsExactly(ROOT, MEMBER);
        assertThat(NodeDependencyScan.lockImporters(acc, memberLock)).isEmpty();
    }

    /**
     * A host that parses a manifest beside its installed node_modules, with no lock there, marks it as npm's. Under
     * a pnpm workspace it is pnpm's where pnpm, run in its directory, installs at the workspace root or stops on the
     * way (a workspace manifest held without its contents); npm's where pnpm installs it on its own, or where it
     * holds a package-lock.json.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "packages/* | false | Pnpm | pnpm-lock.yaml",
      "unread     | false | Pnpm | pnpm-lock.yaml",
      "apps/*     | false | Npm  |",
      "packages/* | true  | Npm  | packages/a/package-lock.json"
    })
    void aManifestMarkedAsNpmsIsInstalledByTheWorkspaceWhoseWalkLeadsThere(String listed, boolean ownLock,
                                                                         PackageManager installs,
                                                                         @Nullable String lock) {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        NodeDependencyScan.captureLock(acc,
          PlainText.builder().sourcePath(Paths.get("pnpm-lock.yaml")).text("root lock").build());
        if (ownLock) {
            NodeDependencyScan.captureLock(acc, PlainText.builder()
              .sourcePath(Paths.get("packages/a/package-lock.json")).text("member lock").build());
        }
        acc.projects.computeIfAbsent(ROOT, k -> new NodeDependencyScan.ProjectState()).capturedPackageJson =
          PlainText.builder().sourcePath(ROOT).text("{\"name\":\"root\",\"private\":true}")
            .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm)))).build();
        acc.projects.computeIfAbsent(MEMBER, k -> new NodeDependencyScan.ProjectState()).capturedPackageJson =
          PlainText.builder().sourcePath(MEMBER).text("{\"name\":\"a\"}")
            .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Npm)))).build();
        acc.paths.addAll(Arrays.asList(ROOT, MEMBER));
        Path workspace = Paths.get("pnpm-workspace.yaml");
        NodeDependencyScan.capturePackageManagerFile(acc, "unread".equals(listed) ?
          new Quark(Tree.randomId(), workspace, Markers.EMPTY, null, null) :
          PlainText.builder().sourcePath(workspace).text("packages:\n  - '" + listed + "'\n").build());

        NodeDependencyScan.linkWorkspaceMembers(acc);

        assertThat(NodeDependencyScan.packageManager(acc, MEMBER)).isEqualTo(installs);
        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(lock == null ? null : Paths.get(lock));
        assertThat(NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER).getPackageManager()).isEqualTo(installs);
        assertThat(NodeDependencyScan.packageManager(acc, ROOT)).isEqualTo(PackageManager.Pnpm);
    }

    /**
     * Which lock yarn 1.22.22, bun 1.3.10 and npm 11.17.0 write when run in packages/a, both it and the root holding
     * one.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "YarnClassic | packages/*   | false |              | yarn.lock",
      "YarnClassic | ./packages/* | false |              | packages/a/yarn.lock",
      "YarnClassic | packages/*   | true  |              | packages/a/yarn.lock",
      "YarnClassic | packages/*   | false | .yarnrc      | yarn.lock",
      "YarnClassic | packages/*   | false | .npmrc       | yarn.lock",
      "Bun         | packages/*   | true  |              | packages/a/bun.lock",
      "Bun         | packages/*   | false | bunfig.toml  | bun.lock",
      "Bun         | packages/*   | false | .npmrc       | bun.lock",
      "Npm         | packages/*   | true  |              | package-lock.json"
    })
    void aMemberWithItsOwnLockGoesWhereThePackageManagersWalkInstalls(PackageManager pm, String pattern, boolean mid,
                                                                     String configFile, String written) {
        String lockName = Paths.get(written).getFileName().toString();
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        NodeDependencyScan.captureLock(acc,
          PlainText.builder().sourcePath(Paths.get(lockName)).text("root lock").build());
        NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get("packages/a/" + lockName))
          .text("member lock").build());
        acc.projects.get(ROOT).capturedPackageJson = PlainText.builder().sourcePath(ROOT)
          .text("{\"name\":\"root\",\"private\":true,\"workspaces\":[\"" + pattern + "\"]}")
          .markers(Markers.build(singletonList(nodeResolutionResult(pm, singletonList("packages/a/package.json")))))
          .build();
        acc.projects.get(MEMBER).capturedPackageJson = PlainText.builder().sourcePath(MEMBER)
          .text("{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\"}}")
          .markers(Markers.build(singletonList(nodeResolutionResult(pm)))).build();
        if (mid) {
            Path packages = Paths.get("packages/package.json");
            NodeDependencyScan.ProjectState ps = new NodeDependencyScan.ProjectState();
            ps.capturedPackageJson = PlainText.builder().sourcePath(packages)
              .text("{\"name\":\"mid\",\"private\":true,\"workspaces\":[\"x/*\"]}")
              .markers(Markers.build(singletonList(nodeResolutionResult(pm, emptyList())))).build();
            acc.projects.put(packages, ps);
        }
        if (configFile != null) {
            NodeDependencyScan.capturePackageManagerFile(acc, PlainText.builder()
              .sourcePath(Paths.get("packages/a/" + configFile)).text("registry=https://registry.npmmirror.com/\n")
              .build());
        }

        NodeDependencyScan.linkWorkspaceMembers(acc);

        Path lock = Paths.get(written);
        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(lock);
        assertThat(acc.projects.get(MEMBER).capturedLockContent).isEqualTo(lock.getNameCount() == 1 ? "root lock" :
          "member lock");
        assertThat(NodeDependencyScan.lockImporters(acc, lock)).contains(MEMBER);
    }

    /**
     * Which lock pnpm 11.2.2 writes run in a project below the root: the root's when listed and shared, else its own;
     * under lockfileDir its own, which without one is cannot-know (run at the root, the root's).
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "packages/a | shared   | true  | pnpm-lock.yaml |",
      "packages/a | shared   | false | pnpm-lock.yaml |",
      "packages/a | unshared | true  | packages/a/pnpm-lock.yaml |",
      "packages/a | unshared | false | |",
      "packages/a | none     | true  | packages/a/pnpm-lock.yaml |",
      "tools/x    | shared   | true  | tools/x/pnpm-lock.yaml |",
      "tools/x    | unshared | true  | tools/x/pnpm-lock.yaml |",
      "packages/a | here     | true  | packages/a/pnpm-lock.yaml |",
      "packages/a | here     | false | pnpm-lock.yaml | run in packages/a it installs into packages/a/pnpm-lock.yaml",
      "tools/x    | here     | true  | tools/x/pnpm-lock.yaml |",
      "tools/x    | here     | false | |"
    })
    void aPnpmProjectGoesToTheLockItsWorkspaceInstallsItInto(String dir, String workspace, boolean ownLock,
                                                            String written, String reported) {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path project = Paths.get(dir + "/package.json");
        NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get("pnpm-lock.yaml"))
          .text("root lock").build());
        if (ownLock) {
            NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get(dir + "/pnpm-lock.yaml"))
              .text("own lock").build());
        }
        acc.projects.get(ROOT).capturedPackageJson = PlainText.builder().sourcePath(ROOT)
          .text("{\"name\":\"root\",\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm,
            singletonList("packages/a/package.json")))))
          .build();
        NodeDependencyScan.ProjectState ps = acc.projects.computeIfAbsent(project,
          k -> new NodeDependencyScan.ProjectState());
        ps.capturedPackageJson = PlainText.builder().sourcePath(project)
          .text("{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\"}}")
          .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm)))).build();
        if (!"none".equals(workspace)) {
            NodeDependencyScan.capturePackageManagerFile(acc, PlainText.builder()
              .sourcePath(Paths.get("pnpm-workspace.yaml"))
              .text("packages:\n  - 'packages/*'\n" + ("unshared".equals(workspace) ?
                "sharedWorkspaceLockfile: false\n" : "here".equals(workspace) ? "lockfileDir: .\n" : "")).build());
        }

        NodeDependencyScan.linkWorkspaceMembers(acc);

        Path lock = written == null ? null : Paths.get(written);
        assertThat(NodeDependencyScan.lockFor(acc, project)).isEqualTo(lock);
        assertThat(acc.projects.get(project).capturedLockContent).isEqualTo(lock == null ? null :
          lock.getNameCount() == 1 ? "root lock" : "own lock");
        if (reported != null) {
            ExecutionContext ctx = new InMemoryExecutionContext();
            LockFileRegeneration.Result result = PackageJsonHelper.regenerateLockContent(ps.capturedPackageJson,
              ps.capturedLockContent, NodeDependencyScan.projectFiles(acc, SCANNED, project), emptyMap(), ctx);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains(reported);
        }
    }

    /**
     * Each way pnpm's walk from a member stops before it picks a lock: the edit goes to the workspace root's lock,
     * whose regeneration walks from the member again and reports it, shared or not, with or without a member lock.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      ".npmrc              | registry=${REG}                          | the environment (${REG}",
      ".npmrc              | //r.example/:tokenHelper=x               | TOKEN_HELPER_IN_PROJECT_CONFIG",
      ".npmrc              | //r.example/:_auth=x                     | ERR_PNPM_AUTH_INVALID_BASE64",
      ".npmrc              | unread                                   | .npmrc (its contents were not read)",
      "pnpm-workspace.yaml | packages: [                              | pnpm fails on this edit",
      "pnpm-workspace.yaml | packages: 5                              | packages field is not an array",
      "pnpm-workspace.yaml | packages: [packages/*]\\ncatalog: [1]    | Expected catalog field to be an object",
      "pnpm-workspace.yaml | packages: [packages/*, ../shared/*]      | ../shared",
      "pnpm-workspace.yaml | packages: [packages/*]\\noverrides: [1]  | ERR_PNPM_INVALID_OVERRIDES",
      "pnpm-workspace.yaml | packages: [packages/*]\\noverrides: {x: $x} | ERR_PNPM_CANNOT_RESOLVE_OVERRIDE_VERSION",
      "pnpm-workspace.yaml | packages: [packages/*]\\nx: ${REG}       | the environment (${REG}",
      "pnpm-workspace.yaml | packages: [packages/*]\\nregistries: 5   | registries",
      "pnpm-workspace.yaml | packages: [packages/*]\\nlockfileDir: x  | lockfileDir x",
      "pnpm-workspace.yaml | packages: [packages/*]\\nlockfileDir: 5  | lockfileDir (number",
      "pnpm-workspace.yaml | packages: [packages/*]\\nlockfileDir: /x | lockfileDir /x",
      "pnpm-workspace.yaml | unread | pnpm-workspace.yaml (its contents were not read)",
      "pnpm-workspace.yml  | packages: [packages/*]                   | ERR_PNPM_BAD_WORKSPACE_MANIFEST_NAME",
      "packages/a/.pnpm-workspace.yaml | packages: [.]                | ERR_PNPM_BAD_WORKSPACE_MANIFEST_NAME",
      "package.json        | {not json                                | ERR_INVALID_PACKAGE_CONFIG",
      "package.json | {\"devEngines\":{\"runtime\":{\"name\":\"node\",\"version\":\">=x.y\"}}} | Invalid comparator"
    })
    void aPnpmWalkThatStopsGoesToTheWorkspaceLockWhoseRegenerationReportsWhy(String file, String content,
                                                                             String reported) {
        String text = content.replace("\\n", "\n");
        for (boolean shared : new boolean[]{true, false}) {
            for (boolean ownLock : new boolean[]{false, true}) {
                NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
                NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get("pnpm-lock.yaml"))
                  .text("root lock").build());
                if (ownLock) {
                    NodeDependencyScan.captureLock(acc, PlainText.builder()
                      .sourcePath(Paths.get("packages/a/pnpm-lock.yaml")).text("own lock").build());
                }
                acc.projects.get(ROOT).capturedPackageJson = PlainText.builder().sourcePath(ROOT)
                  .text("package.json".equals(file) ? text : "{\"name\":\"root\",\"private\":true}")
                  .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm,
                    singletonList("packages/a/package.json")))))
                  .build();
                SourceFile member = PlainText.builder().sourcePath(MEMBER)
                  .text("{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\"}}")
                  .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm)))).build();
                acc.projects.computeIfAbsent(MEMBER, k -> new NodeDependencyScan.ProjectState())
                  .capturedPackageJson = member;
                String unshared = shared ? "" : "\nsharedWorkspaceLockfile: false\n";
                if ("pnpm-workspace.yaml".equals(file)) {
                    capture(acc, file, "unread".equals(text) ? text : text + unshared);
                } else if ("pnpm-workspace.yml".equals(file)) {
                    capture(acc, file, text + unshared);
                } else {
                    capture(acc, "pnpm-workspace.yaml", "packages:\n  - 'packages/*'\n" + unshared);
                    if (!"package.json".equals(file)) {
                        capture(acc, file, text);
                    }
                }

                NodeDependencyScan.linkWorkspaceMembers(acc);
                ExecutionContext ctx = new InMemoryExecutionContext();
                LockFileRegeneration.Result result = PackageJsonHelper.regenerateLockContent(member,
                  acc.projects.get(MEMBER).capturedLockContent, NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER),
                  emptyMap(), ctx);

                assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(Paths.get("pnpm-lock.yaml"));
                assertThat(acc.projects.get(MEMBER).capturedLockContent).isEqualTo("root lock");
                assertThat(result).isNotNull();
                assertThat(result.isSuccess()).isFalse();
                assertThat(result.getErrorMessage()).contains(reported);
            }
        }
    }

    /**
     * Without a lock at the workspace root, a walk that stops leaves the member's own lock, whose regeneration reads
     * the same files and reports it; a member without one has no lock to keep in step, as any lockless project.
     */
    @Test
    void aPnpmWalkThatStopsWhereTheRootHoldsNoLock() {
        for (boolean ownLock : new boolean[]{false, true}) {
            NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
            if (ownLock) {
                NodeDependencyScan.captureLock(acc, PlainText.builder()
                  .sourcePath(Paths.get("packages/a/pnpm-lock.yaml")).text("own lock").build());
            }
            NodeDependencyScan.ProjectState root = acc.projects.computeIfAbsent(ROOT,
              k -> new NodeDependencyScan.ProjectState());
            root.capturedPackageJson = PlainText.builder().sourcePath(ROOT).text("{\"name\":\"root\",\"private\":true}")
              .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm,
                singletonList("packages/a/package.json")))))
              .build();
            SourceFile member = PlainText.builder().sourcePath(MEMBER)
              .text("{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\"}}")
              .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm)))).build();
            acc.projects.computeIfAbsent(MEMBER, k -> new NodeDependencyScan.ProjectState()).capturedPackageJson =
              member;
            capture(acc, "pnpm-workspace.yaml", "packages: [");

            NodeDependencyScan.linkWorkspaceMembers(acc);

            assertThat(NodeDependencyScan.lockFor(acc, MEMBER))
              .isEqualTo(ownLock ? Paths.get("packages/a/pnpm-lock.yaml") : null);
            if (ownLock) {
                ExecutionContext ctx = new InMemoryExecutionContext();
                LockFileRegeneration.Result result = PackageJsonHelper.regenerateLockContent(member,
                  acc.projects.get(MEMBER).capturedLockContent, NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER),
                  emptyMap(), ctx);
                assertThat(result).isNotNull();
                assertThat(result.getErrorMessage()).contains("pnpm fails on this edit");
            }
        }
    }

    /**
     * The marker's .npmrc is the repository root's, so it stands in for an unread root .npmrc in the walk from a
     * member as in the regeneration: unshared, the member's own lock.
     */
    @ParameterizedTest
    @CsvSource({"true, true, pnpm-lock.yaml", "true, false, pnpm-lock.yaml",
      "false, true, packages/a/pnpm-lock.yaml"})
    void theMarkersNpmrcStandsInForTheRepositoryRootsInTheWalk(boolean shared, boolean ownLock, String written) {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get("pnpm-lock.yaml"))
          .text("root lock").build());
        if (ownLock) {
            NodeDependencyScan.captureLock(acc, PlainText.builder()
              .sourcePath(Paths.get("packages/a/pnpm-lock.yaml")).text("own lock").build());
        }
        acc.projects.get(ROOT).capturedPackageJson = PlainText.builder().sourcePath(ROOT)
          .text("{\"name\":\"root\",\"private\":true}")
          .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm,
            singletonList("packages/a/package.json")))))
          .build();
        acc.projects.computeIfAbsent(MEMBER, k -> new NodeDependencyScan.ProjectState()).capturedPackageJson =
          PlainText.builder().sourcePath(MEMBER)
            .text("{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\"}}")
            .markers(Markers.build(singletonList(nodeResolutionResult(PackageManager.Pnpm).withNpmrcConfigs(
              singletonList(new NodeResolutionResult.Npmrc(NodeResolutionResult.NpmrcScope.Project,
                singletonMap("registry", "https://registry.npmjs.org/")))))))
            .build();
        capture(acc, "pnpm-workspace.yaml", "packages:\n  - 'packages/*'\n" +
                                            (shared ? "" : "sharedWorkspaceLockfile: false\n"));
        capture(acc, ".npmrc", "unread");

        NodeDependencyScan.linkWorkspaceMembers(acc);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(Paths.get(written));
    }

    private static void capture(NodeDependencyScan.Accumulator acc, String path, String text) {
        NodeDependencyScan.capturePackageManagerFile(acc, "unread".equals(text) ?
          new Quark(Tree.randomId(), Paths.get(path), Markers.EMPTY, null, null) :
          PlainText.builder().sourcePath(Paths.get(path)).text(text).build());
    }

    /**
     * Yarn Berry's project is the nearest directory holding a yarn.lock: the member's own.
     */
    @Test
    void aYarnBerryMemberWithItsOwnLockIsItsOwnProject() {
        NodeDependencyScan.Accumulator acc = workspaceWithMemberLock(PackageManager.YarnBerry, PackageManager.YarnBerry,
          "yarn.lock", "yarn.lock");
        Path memberLock = Paths.get("packages/a/yarn.lock");

        NodeDependencyScan.linkWorkspaceMembers(acc);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(memberLock);
        assertThat(files.getManifests()).containsOnlyKeys(MEMBER);
        assertThat(files.getPackageManagerFiles()).containsOnlyKeys(ROOT);
        assertThat(acc.projects.get(MEMBER).capturedLockContent).isEqualTo("member lock");
        assertThat(NodeDependencyScan.lockImporters(acc, Paths.get("yarn.lock"))).containsExactly(ROOT);
        assertThat(NodeDependencyScan.lockImporters(acc, memberLock)).containsExactly(MEMBER);
    }

    /**
     * Recorded in a member with its own lock under a root that lists it and holds another's: no held lock takes the
     * edit, but where npm rewrites a yarn.lock or bun migrates a pnpm-lock.yaml there, the member's lock reports why.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "Npm         | YarnClassic | the lock npm writes: it installs at the workspace root ../..",
      "Npm         | Pnpm        |",
      "Npm         | Bun         |",
      "YarnClassic | Npm         |",
      "YarnClassic | Pnpm        |",
      "YarnClassic | Bun         |",
      "Pnpm        | Npm         |",
      "Pnpm        | YarnClassic |",
      "Pnpm        | Bun         |",
      "Bun         | Npm         |",
      "Bun         | YarnClassic |",
      "Bun         | Pnpm        | the lock bun writes: it installs at the workspace root ../.."
    })
    void aMemberWhoseRootHoldsOnlyAnotherPackageManagersLock(PackageManager pm, PackageManager rootPm,
                                                             @Nullable String reported) {
        String rootLock = LockFileRegeneration.forPackageManager(rootPm).getLockFile();
        Path memberLock = Paths.get("packages/a/" + LockFileRegeneration.forPackageManager(pm).getLockFile());
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        captureFile(acc, rootLock, "root lock");
        captureFile(acc, memberLock.toString(), "member lock");
        project(acc, ROOT, "{\"name\":\"root\",\"private\":true,\"workspaces\":[\"packages/*\"]}",
          nodeResolutionResult(rootPm, singletonList("packages/a/package.json")));
        SourceFile member = project(acc, MEMBER,
          "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\"}}",
          nodeResolutionResult(pm));
        if (pm == PackageManager.Pnpm || rootPm == PackageManager.Pnpm) {
            capture(acc, "pnpm-workspace.yaml", "packages:\n  - packages/*\n");
        }

        NodeDependencyScan.linkWorkspaceMembers(acc);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(reported == null ? null : memberLock);
        assertThat(NodeDependencyScan.lockImporters(acc, Paths.get(rootLock))).containsExactly(ROOT);
        if (reported == null) {
            assertThat(NodeDependencyScan.lockImporters(acc, memberLock)).isEmpty();
            return;
        }
        ExecutionContext ctx = new InMemoryExecutionContext();
        LockFileRegeneration.Result result = PackageJsonHelper.regenerateLockContent(member,
          acc.projects.get(MEMBER).capturedLockContent, NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER),
          emptyMap(), ctx);
        assertThat(result).isNotNull();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(LockFileRegeneration.Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).startsWith("the engine cannot know " + reported);
    }

    /**
     * Recorded in a member with its own lock under a root that lists it: npm rewrites the root's npm-shrinkwrap.json,
     * bun a bun.lockb; bun migrates a package-lock.json or yarn.lock before a pnpm-lock.yaml.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "Npm | npm-shrinkwrap.json                   | member | it installs at the workspace root ../..",
      "Npm | npm-shrinkwrap.json package-lock.json | root   | it reads and writes the npm-shrinkwrap.json beside",
      "Bun | bun.lockb                             | member | it installs at the workspace root ../..",
      "Bun | pnpm-lock.yaml package-lock.json      |        |",
      "Bun | pnpm-lock.yaml yarn.lock              |        |"
    })
    void aMemberWhoseRootHoldsFilesItsPackageManagerReadsThere(PackageManager pm, String held,
                                                               @Nullable String written, @Nullable String reported) {
        String lockName = LockFileRegeneration.forPackageManager(pm).getLockFile();
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        for (String file : held.split(" ")) {
            if (file.equals("npm-shrinkwrap.json") || file.equals("bun.lockb")) {
                capture(acc, file, "unread");
            } else {
                captureFile(acc, file, "root lock");
            }
        }
        if (held.contains("pnpm-lock.yaml")) {
            capture(acc, "pnpm-workspace.yaml", "packages:\n  - packages/*\n");
        }
        captureFile(acc, "packages/a/" + lockName, "member lock");
        project(acc, ROOT, "{\"name\":\"root\",\"private\":true,\"workspaces\":[\"packages/*\"]}",
          nodeResolutionResult(pm, singletonList("packages/a/package.json")));
        SourceFile member = project(acc, MEMBER,
          "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\"}}",
          nodeResolutionResult(pm));

        NodeDependencyScan.linkWorkspaceMembers(acc);

        Path lock = written == null ? null : Paths.get("member".equals(written) ? "packages/a/" + lockName : lockName);
        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(lock);
        if (reported == null) {
            return;
        }
        ExecutionContext ctx = new InMemoryExecutionContext();
        LockFileRegeneration.Result result = PackageJsonHelper.regenerateLockContent(member,
          acc.projects.get(MEMBER).capturedLockContent, NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER),
          emptyMap(), ctx);
        assertThat(result).isNotNull();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(LockFileRegeneration.Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).startsWith("the engine cannot know the lock " +
                                                               (pm == PackageManager.Npm ? "npm" : "bun") +
                                                               " writes: " + reported);
    }

    /**
     * Which lock npm 11.17.0, yarn 1.22.22, Yarn 4.10.3, pnpm 11.2.2 and bun 1.3.10 write run in a member, recorded;
     * none where it writes none the host holds. Yarn rejects an unlisted member, as the root lock's regeneration does.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "Npm         | packages/* | true  | true  | package-lock.json",
      "Npm         | packages/* | true  | false | package-lock.json",
      "Npm         | libs/*     | true  | false |",
      "Npm         | packages/* | false | true  |",
      "YarnClassic | packages/* | true  | true  | yarn.lock",
      "YarnClassic | packages/* | true  | false | yarn.lock",
      "YarnClassic | libs/*     | true  | false |",
      "YarnClassic | packages/* | false | true  |",
      "YarnBerry   | packages/* | true  | true  | packages/a/yarn.lock",
      "YarnBerry   | packages/* | true  | false | yarn.lock",
      "YarnBerry   | libs/*     | true  | false | yarn.lock",
      "YarnBerry   | packages/* | false | true  | packages/a/yarn.lock",
      "Pnpm        | packages/* | true  | true  | pnpm-lock.yaml",
      "Pnpm        | packages/* | true  | false | pnpm-lock.yaml",
      "Pnpm        | libs/*     | true  | false |",
      "Pnpm        | packages/* | false | true  |",
      "Bun         | packages/* | true  | true  | bun.lock",
      "Bun         | packages/* | true  | false | bun.lock",
      "Bun         | libs/*     | true  | false |",
      "Bun         | packages/* | false | true  |"
    })
    void eachPackageManagerRunInAMemberWritesTheLockItsOwnWalkFinds(PackageManager pm, String pattern,
                                                                    boolean rootLock, boolean memberLock,
                                                                    String written) {
        String lockName = LockFileRegeneration.forPackageManager(pm).getLockFile();
        String lockText = pm == PackageManager.YarnBerry ? "__metadata:\n  version: 8\n  cacheKey: 10c0\n" : "lock";
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        if (rootLock) {
            captureFile(acc, lockName, lockText);
        }
        if (memberLock) {
            captureFile(acc, "packages/a/" + lockName, lockText);
        }
        boolean listed = "packages/*".equals(pattern);
        String root = "{\"name\":\"root\",\"private\":true,\"dependencies\":{\"ms\":\"2.1.3\"}" +
                      (pm == PackageManager.Pnpm ? "" : ",\"workspaces\":[\"" + pattern + "\"]") +
                      (pm == PackageManager.YarnBerry ? ",\"packageManager\":\"yarn@4.10.3\"" : "") + "}";
        project(acc, ROOT, root, nodeResolutionResult(pm, listed ? singletonList("packages/a/package.json") :
          emptyList()));
        SourceFile member = project(acc, MEMBER,
          "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\"}}",
          nodeResolutionResult(pm));
        if (pm == PackageManager.Pnpm) {
            capture(acc, "pnpm-workspace.yaml", "packages:\n  - " + pattern + "\n");
        }

        NodeDependencyScan.linkWorkspaceMembers(acc);

        Path lock = written == null ? null : Paths.get(written);
        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(lock);
        assertThat(acc.projects.get(MEMBER).capturedLockContent).isEqualTo(lock == null ? null : lockText);
        if (pm == PackageManager.YarnBerry && !listed) {
            ExecutionContext ctx = new InMemoryExecutionContext();
            LockFileRegeneration.Result result = PackageJsonHelper.regenerateLockContent(member,
              acc.projects.get(MEMBER).capturedLockContent, NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER),
              emptyMap(), ctx);
            assertThat(result).isNotNull();
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(LockFileRegeneration.Kind.PM_REJECTS);
            assertThat(result.getFailure().getDetail()).isEqualTo("yarn rejects this edit: Usage Error: The nearest " +
                                                                  "package directory (packages/a) doesn't seem to be " +
                                                                  "part of the project declared in .");
        }
    }

    /**
     * A walk from a member without a lock that stops before it picks one goes to the nearest lock, whose regeneration
     * walks from the member again and reports it.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "Npm         | package.json          | the engine cannot know the package's files",
      "YarnClassic | packages/package.json | yarn fails on this edit: SyntaxError: packages/package.json"
    })
    void aWalkThatStopsGoesToTheNearestLockWhoseRegenerationReportsWhy(PackageManager pm, String stops,
                                                                      String reported) {
        String lockName = LockFileRegeneration.forPackageManager(pm).getLockFile();
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        captureFile(acc, lockName, "root lock");
        String workspaces = "\"private\":true,\"workspaces\":[\"packages/*\"]";
        project(acc, ROOT, "{\"name\":\"root\"," + workspaces + (stops.equals("package.json") ?
          ",\"directories\":{\"bin\":\"bin\"}}" : "}"), nodeResolutionResult(pm,
          singletonList("packages/a/package.json")));
        if (!stops.equals("package.json")) {
            project(acc, Paths.get(stops), "{not json", nodeResolutionResult(pm));
        }
        SourceFile member = project(acc, MEMBER, "{\"name\":\"a\",\"version\":\"1.0.0\"}", nodeResolutionResult(pm));

        NodeDependencyScan.linkWorkspaceMembers(acc);
        ExecutionContext ctx = new InMemoryExecutionContext();
        LockFileRegeneration.Result result = PackageJsonHelper.regenerateLockContent(member,
          acc.projects.get(MEMBER).capturedLockContent, NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER),
          emptyMap(), ctx);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(Paths.get(lockName));
        assertThat(result).isNotNull();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getDetail()).startsWith(reported);
    }

    /**
     * npm's walk maps the root's workspaces over every package.json under it, so one the host holds without its
     * contents, away from the edited project, is unknown to the member's regeneration.
     */
    @Test
    void aPackageJsonUnderADirectoryAboveTheHostDidNotReadReachesTheWalk() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        String lock = "{\"name\":\"a\",\"version\":\"1.0.0\",\"lockfileVersion\":3,\"requires\":true," +
                      "\"packages\":{\"\":{\"name\":\"a\",\"version\":\"1.0.0\"}}}\n";
        captureFile(acc, "packages/a/package-lock.json", lock);
        project(acc, ROOT, "{\"name\":\"root\",\"private\":true,\"workspaces\":[\"libs/*\"]}",
          nodeResolutionResult(PackageManager.Npm, emptyList()));
        Path unread = Paths.get("libs/x/package.json");
        NodeDependencyScan.capturePackageManagerFile(acc,
          new Quark(Tree.randomId(), unread, Markers.EMPTY, null, null));
        SourceFile member = project(acc, MEMBER, "{\"name\":\"a\",\"version\":\"1.0.0\"}",
          nodeResolutionResult(PackageManager.Npm));

        NodeDependencyScan.linkWorkspaceMembers(acc);
        ExecutionContext ctx = new InMemoryExecutionContext();
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER);
        LockFileRegeneration.Result result = PackageJsonHelper.regenerateLockContent(member,
          acc.projects.get(MEMBER).capturedLockContent, files, emptyMap(), ctx);

        assertThat(files.getPaths()).contains(unread);
        assertThat(result).isNotNull();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(LockFileRegeneration.Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).contains("libs/x/package.json (its contents were not read)");
    }

    /**
     * bun 1.3.10 reads a patch file wherever patchedDependencies names it, above the lock's directory too
     * ({@code open-patched-above-lock}).
     */
    @Test
    void aPatchFileAboveTheLockReachesTheRegeneration() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path manifest = Paths.get("apps/web/package.json");
        captureFile(acc, "apps/web/bun.lock", "lock");
        project(acc, manifest, "{\"name\":\"web\",\"patchedDependencies\":{\"ms@2.1.3\":" +
                               "\"../../patches/ms@2.1.3.patch\"}}", nodeResolutionResult(PackageManager.Bun));
        capture(acc, "patches/ms@2.1.3.patch", "--- a/index.js\n+++ b/index.js\n");

        NodeDependencyScan.linkWorkspaceMembers(acc);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, manifest);

        assertThat(files.getPackageManagerFiles()).containsKey(Paths.get("patches/ms@2.1.3.patch"));
    }

    /**
     * pnpm installs every project of a workspace whose projects keep their own locks in one run, so a member's
     * regeneration gets the other projects' locks; npm's gets none.
     */
    @ParameterizedTest
    @CsvSource({"Pnpm, pnpm-lock.yaml, true", "Npm, package-lock.json, false"})
    void theOtherProjectsLocksGoWhereThePackageManagerReadsThem(PackageManager pm, String lockName, boolean read) {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        captureFile(acc, lockName, "root lock");
        captureFile(acc, "packages/a/" + lockName, "a lock");
        captureFile(acc, "packages/b/" + lockName, "b lock");
        project(acc, ROOT, "{\"name\":\"root\",\"private\":true}", nodeResolutionResult(pm));
        project(acc, MEMBER, "{\"name\":\"a\",\"version\":\"1.0.0\"}", nodeResolutionResult(pm));
        project(acc, Paths.get("packages/b/package.json"), "{\"name\":\"b\",\"version\":\"1.0.0\"}",
          nodeResolutionResult(pm));
        capture(acc, "pnpm-workspace.yaml", "packages:\n  - packages/*\nsharedWorkspaceLockfile: false\n");

        NodeDependencyScan.linkWorkspaceMembers(acc);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, MEMBER);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(Paths.get("packages/a/" + lockName));
        if (read) {
            assertThat(files.getPackageManagerFiles()).containsEntry(Paths.get(lockName), "root lock")
              .containsEntry(Paths.get("packages/b/" + lockName), "b lock")
              .doesNotContainKey(Paths.get("packages/a/" + lockName));
        } else {
            assertThat(files.getPackageManagerFiles().keySet()).noneMatch(p -> p.endsWith(lockName));
        }
    }

    private static void captureFile(NodeDependencyScan.Accumulator acc, String path, String text) {
        SourceFile file = PlainText.builder().sourcePath(Paths.get(path)).text(text).build();
        NodeDependencyScan.capturePackageManagerFile(acc, file);
        NodeDependencyScan.captureLock(acc, file);
    }

    private static SourceFile project(NodeDependencyScan.Accumulator acc, Path path, String text,
                                      NodeResolutionResult marker) {
        SourceFile manifest = PlainText.builder().sourcePath(path).text(text)
          .markers(Markers.build(singletonList(marker))).build();
        NodeDependencyScan.capturePackageManagerFile(acc, manifest);
        acc.projects.computeIfAbsent(path, k -> new NodeDependencyScan.ProjectState()).capturedPackageJson = manifest;
        return manifest;
    }

    private static final Path ROOT = Paths.get("package.json");
    private static final Path MEMBER = Paths.get("packages/a/package.json");

    private static NodeDependencyScan.Accumulator workspaceWithMemberLock(PackageManager rootPm,
      PackageManager memberPm,
                                                                        String rootLock, String memberLock) {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        NodeDependencyScan.captureLock(acc,
          PlainText.builder().sourcePath(Paths.get(rootLock)).text("root lock").build());
        NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get("packages/a/" + memberLock))
          .text("member lock").build());
        acc.projects.get(ROOT).capturedPackageJson = PlainText.builder().sourcePath(ROOT)
          .text("{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .markers(Markers.build(singletonList(nodeResolutionResult(rootPm, singletonList("packages/a/package.json")))))
          .build();
        acc.projects.get(MEMBER).capturedPackageJson = PlainText.builder().sourcePath(MEMBER).text("{\"name\":\"a\"}")
          .markers(Markers.build(singletonList(nodeResolutionResult(memberPm)))).build();
        return acc;
    }

    private static final List<String> CANDIDATES = Arrays.asList(".npmrc", ".yarnrc", ".yarnrc.yml",
      "pnpm-workspace.yaml", "patches/a.patch", "bunfig.toml", ".pnpmfile.cjs", ".pnpmfile.mjs", ".env", ".env.local",
      ".env.production", ".env.production.local", ".env.development", ".env.development.local", ".env.test",
      ".env.test.local", ".env.staging", "README.md");

    /**
     * Each package manager's regeneration gets the files it reads: bun 1.3.10 the four .env files its install loads,
     * pnpm a pnpmfile by its path only. Whichever the scan visits first, no file only Bun reads is kept elsewhere.
     */
    @ParameterizedTest
    @CsvSource({
      "Npm,         package-lock.json, .npmrc",
      "YarnClassic, yarn.lock,         .npmrc .yarnrc .yarnrc.yml",
      "YarnBerry,   yarn.lock,         .yarnrc.yml patches/a.patch",
      "Pnpm,        pnpm-lock.yaml,    .npmrc pnpm-workspace.yaml patches/a.patch",
      "Bun,         bun.lock,          .npmrc patches/a.patch bunfig.toml .env.production.local .env.local " +
        ".env.production .env"
    })
    void theScanKeepsTheFilesTheProjectsPackageManagerReads(PackageManager pm, String lockName, String read) {
        Path manifest = Paths.get("app/package.json");
        List<String> readAnywhere = Arrays.asList(".npmrc", ".yarnrc", ".yarnrc.yml", "pnpm-workspace.yaml",
          "patches/a.patch");
        for (boolean lockFirst : new boolean[]{true, false}) {
            NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
            SourceFile lock = PlainText.builder().sourcePath(Paths.get("app/" + lockName)).text("lock").build();
            if (lockFirst) {
                NodeDependencyScan.captureLock(acc, lock);
            }
            for (String name : CANDIDATES) {
                capture(acc, "app/" + name, name.startsWith(".env") ? "NPM_CONFIG_TOKEN=secret" : "");
            }
            if (!lockFirst) {
                NodeDependencyScan.captureLock(acc, lock);
            }
            acc.projects.get(manifest).capturedPackageJson = PlainText.builder().sourcePath(manifest)
              .text("{\"name\":\"app\"}").markers(Markers.build(singletonList(nodeResolutionResult(pm)))).build();

            NodeDependencyScan.linkWorkspaceMembers(acc);
            NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, manifest);

            List<String> kept = new ArrayList<>(readAnywhere);
            for (String name : read.split(" ")) {
                if (!kept.contains(name)) {
                    kept.add(name);
                }
            }
            assertThat(acc.packageManagerFiles).as("lock first: %s", lockFirst).containsOnlyKeys(paths("app/", kept));
            assertThat(acc.besideLock).isEmpty();
            assertThat(files.getPackageManagerFiles()).containsOnlyKeys(paths("app/", Arrays.asList(read.split(" "))));
            assertThat(files.getPaths()).containsAll(paths("app/", CANDIDATES));
        }
    }

    /**
     * bun 1.3.10 run in a workspace member installs at the root and loads the root's .env files, never those beside
     * a lock of the member's own.
     */
    @Test
    void aBunMembersOwnLockKeepsNoEnvFile() {
        NodeDependencyScan.Accumulator acc = workspaceWithMemberLock(PackageManager.Bun, PackageManager.Bun,
          "bun.lock", "bun.lock");
        capture(acc, ".env", "NPM_CONFIG_TOKEN=root");
        capture(acc, "bunfig.toml", "[install]\n");
        capture(acc, "packages/a/.env", "NPM_CONFIG_TOKEN=member");
        capture(acc, "packages/a/bunfig.toml", "[install]\n");

        NodeDependencyScan.linkWorkspaceMembers(acc);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(Paths.get("bun.lock"));
        assertThat(acc.packageManagerFiles).containsOnlyKeys(Paths.get(".env"), Paths.get("bunfig.toml"));
    }

    /**
     * pnpm 11.2.2 reads a workspace member's package.yaml or package.json5 where it finds one; the scan keeps them for
     * the project of a package manager that reads them, under its directory as it keeps a patch.
     */
    @ParameterizedTest
    @CsvSource({"Pnpm, pnpm-lock.yaml, true", "Npm, package-lock.json, false"})
    void aMembersManifestInAnotherFormatIsKeptWhereItsPackageManagerReadsIt(PackageManager pm, String lockName,
                                                                           boolean kept) {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get(lockName)).text("lock").build());
        capture(acc, "packages/c/package.yaml", "name: c\n");
        capture(acc, "packages/d/package.json5", "{name: 'd'}\n");
        acc.projects.get(ROOT).capturedPackageJson = PlainText.builder().sourcePath(ROOT).text("{\"private\":true}")
          .markers(Markers.build(singletonList(nodeResolutionResult(pm)))).build();

        NodeDependencyScan.linkWorkspaceMembers(acc);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, SCANNED, ROOT);

        List<Path> manifests = Arrays.asList(Paths.get("packages/c/package.yaml"),
          Paths.get("packages/d/package.json5"));
        if (kept) {
            assertThat(files.getPackageManagerFiles()).containsKeys(manifests.toArray(new Path[0]));
        } else {
            assertThat(files.getPackageManagerFiles()).doesNotContainKeys(manifests.toArray(new Path[0]));
        }
        assertThat(files.getPaths()).containsAll(manifests);
    }

    private static List<Path> paths(String dir, List<String> names) {
        List<Path> paths = new ArrayList<>();
        for (String name : names) {
            paths.add(Paths.get(dir + name));
        }
        return paths;
    }
}

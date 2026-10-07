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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class NodeDependencyScanTest {

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

        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, new InMemoryExecutionContext(),
          manifest);

        assertThat(files.getPackageManagerFiles()).containsOnlyKeys(parentYarnrc, rootNpmrc, ownNpmrc);
        assertThat(files.getPaths()).containsExactlyInAnyOrder(parentYarnrc, rootNpmrc, ownNpmrc, readme);
    }

    /**
     * Yarn classic reads the packageManager of the nearest package.json from the project's directory up.
     */
    @Test
    void thePackageJsonOfEachDirectoryAboveTheProjectIsKept() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path manifest = Paths.get("repo/app/package.json");
        acc.lockToPackage.put(Paths.get("repo/app/yarn.lock"), manifest);
        Path parent = Paths.get("repo/package.json");
        Path top = Paths.get("package.json");
        String[] names = {"app", "repo", "top", "other", "lib"};
        Path[] paths = {manifest, parent, top, Paths.get("repo/other/package.json"),
          Paths.get("repo/app/lib/package.json")};
        for (int i = 0; i < paths.length; i++) {
            NodeDependencyScan.ProjectState ps = new NodeDependencyScan.ProjectState();
            ps.capturedPackageJson = PlainText.builder().sourcePath(paths[i]).text("{\"name\":\"" + names[i] + "\"}")
              .build();
            acc.projects.put(paths[i], ps);
        }

        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, new InMemoryExecutionContext(),
          manifest);

        assertThat(files.getManifests()).containsOnlyKeys(manifest);
        assertThat(files.getPackageManagerFiles()).containsOnlyKeys(parent, top);
        assertThat(files.getPackageManagerFiles().get(parent)).isEqualTo("{\"name\":\"repo\"}");
    }

    /**
     * Run in a workspace member that has a lock of its own, npm, Yarn classic, pnpm and Bun install at the workspace
     * root and write its lock, leaving the member's as it was; where the package manager's own walk says so, the
     * regeneration runs it in the member.
     */
    @ParameterizedTest
    @CsvSource({"Npm, package-lock.json", "YarnClassic, yarn.lock", "Pnpm, pnpm-lock.yaml", "Bun, bun.lock"})
    void aMemberWithItsOwnLockRegeneratesTheWorkspaceRootsLock(PackageManager pm, String lockName) {
        NodeDependencyScan.Accumulator acc = workspaceWithMemberLock(pm, pm, lockName, lockName);
        Path rootLock = Paths.get(lockName);
        Path memberLock = Paths.get("packages/a/" + lockName);

        NodeDependencyScan.linkWorkspaceMembers(acc);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, new InMemoryExecutionContext(),
          MEMBER);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(rootLock);
        assertThat(files.getManifests()).containsOnlyKeys(ROOT, MEMBER);
        assertThat(files.getWorkingDirectory()).isEqualTo(pm == PackageManager.Pnpm ? null : Paths.get("packages/a"));
        assertThat(acc.projects.get(MEMBER).capturedLockContent).isEqualTo("root lock");
        assertThat(NodeDependencyScan.lockImporters(acc, rootLock)).containsExactly(ROOT, MEMBER);
        assertThat(NodeDependencyScan.lockImporters(acc, memberLock)).isEmpty();
    }

    /**
     * Which lock yarn 1.22.22, bun 1.3.10 and npm 11.17.0 write when run in packages/a, both it and the root holding
     * one: the root's (workspaces as given), with or without packages/package.json in between (workspaces
     * {@code ["x/*"]}) and a config file in packages/a. Yarn classic's walk stops at the first workspaces list and
     * matches it with micromatch 2.3.11, which keeps a leading {@code ./}; Bun's stops at the first parent with
     * workspaces; npm's at the nearest that lists the project. Yarn reads the config files from where it runs, which
     * the root's regeneration is given as its working directory.
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
        NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get(lockName)).text("root lock").build());
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
     * Yarn Berry's project is the nearest directory holding a yarn.lock: the member's own.
     */
    @Test
    void aYarnBerryMemberWithItsOwnLockIsItsOwnProject() {
        NodeDependencyScan.Accumulator acc = workspaceWithMemberLock(PackageManager.YarnBerry, PackageManager.YarnBerry,
          "yarn.lock", "yarn.lock");
        Path memberLock = Paths.get("packages/a/yarn.lock");

        NodeDependencyScan.linkWorkspaceMembers(acc);
        NodeDependencyScan.ProjectFiles files = NodeDependencyScan.projectFiles(acc, new InMemoryExecutionContext(),
          MEMBER);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(memberLock);
        assertThat(files.getManifests()).containsOnlyKeys(MEMBER);
        assertThat(files.getPackageManagerFiles()).containsOnlyKeys(ROOT);
        assertThat(files.getWorkingDirectory()).isNull();
        assertThat(acc.projects.get(MEMBER).capturedLockContent).isEqualTo("member lock");
        assertThat(NodeDependencyScan.lockImporters(acc, Paths.get("yarn.lock"))).containsExactly(ROOT);
        assertThat(NodeDependencyScan.lockImporters(acc, memberLock)).containsExactly(MEMBER);
    }

    /**
     * A root holding no lock of the member's package manager is not where that lock is written: the member's own
     * goes to the engine, which finds the root above it.
     */
    @Test
    void aMemberKeepsItsOwnLockWhenTheRootHoldsNoneOfItsPackageManager() {
        NodeDependencyScan.Accumulator acc = workspaceWithMemberLock(PackageManager.YarnClassic, PackageManager.Npm,
          "yarn.lock", "package-lock.json");
        Path memberLock = Paths.get("packages/a/package-lock.json");

        NodeDependencyScan.linkWorkspaceMembers(acc);

        assertThat(NodeDependencyScan.lockFor(acc, MEMBER)).isEqualTo(memberLock);
        assertThat(acc.projects.get(MEMBER).capturedLockContent).isEqualTo("member lock");
        assertThat(NodeDependencyScan.lockImporters(acc, Paths.get("yarn.lock"))).containsExactly(ROOT);
        assertThat(NodeDependencyScan.lockImporters(acc, memberLock)).containsExactly(MEMBER);
    }

    private static final Path ROOT = Paths.get("package.json");
    private static final Path MEMBER = Paths.get("packages/a/package.json");

    private static NodeDependencyScan.Accumulator workspaceWithMemberLock(PackageManager rootPm, PackageManager memberPm,
                                                                        String rootLock, String memberLock) {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        NodeDependencyScan.captureLock(acc, PlainText.builder().sourcePath(Paths.get(rootLock)).text("root lock").build());
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

    @Test
    void theEnvFilesAnInstallLoadsAreKept() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        for (String name : new String[]{".env", ".env.local", ".env.production", ".env.production.local",
          ".env.development", ".env.test"}) {
            NodeDependencyScan.capturePackageManagerFile(acc,
              PlainText.builder().sourcePath(Paths.get("app/" + name)).text("NPM_CONFIG_REGISTRY=https://r/").build());
        }

        assertThat(acc.packageManagerFiles).containsOnlyKeys(Paths.get("app/.env"), Paths.get("app/.env.local"),
          Paths.get("app/.env.production"), Paths.get("app/.env.production.local"));
    }
}

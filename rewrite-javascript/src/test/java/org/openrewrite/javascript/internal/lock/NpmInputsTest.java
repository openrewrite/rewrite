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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.npm.NpmOptions;
import org.openrewrite.javascript.internal.lock.npm.PackumentSource;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class NpmInputsTest {

    private static final String MEMBER = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\"\n}\n";
    private static final String MEMBER_LOCK = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  " +
                                              "\"lockfileVersion\": 3,\n  \"requires\": true,\n  \"packages\": {\n" +
                                              "    \"\": {\n      \"name\": \"a\",\n      \"version\": \"1.0.0\"\n" +
                                              "    }\n  }\n}\n";

    private static final PackumentSource NO_REGISTRY = new PackumentSource() {
        @Override
        public Map<String, Object> packument(String registry, String name, boolean fullMetadata) {
            throw new AssertionError("no packument is needed: " + name);
        }

        @Override
        public Map<String, Object> manifest(String registry, String name, String version) {
            throw new AssertionError("no manifest is needed: " + name);
        }
    };

    @Test
    void anEditOfTheLocksOwnManifestWalksThePackageJsonAboveItNearestFirst() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\"}")
          .lock("{}")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\"}")
          .packageManagerFile(Paths.get("package.json"), "{\"name\":\"top\"}")
          .packageManagerFile(Paths.get("repo/package.json"), "{\"name\":\"repo\"}")
          .packageManagerFile(Paths.get("repo/.npmrc"), "x=y")
          .packageManagerFile(Paths.get("repo/other/package.json"), "{\"name\":\"other\"}")
          .build();

        Inputs inputs = NpmInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getParentTrees().keySet()).containsExactly("packages/a", "repo/packages/a");
        assertThat(inputs.getParentTrees().get("packages/a")).containsOnly(entry("", "{\"name\":\"repo\"}"),
          entry("other", "{\"name\":\"other\"}"), entry("packages/a", "{\"name\":\"a\"}"));
        assertThat(inputs.getParentTrees().get("repo/packages/a")).containsOnly(entry("", "{\"name\":\"top\"}"),
          entry("repo", "{\"name\":\"repo\"}"), entry("repo/other", "{\"name\":\"other\"}"),
          entry("repo/packages/a", "{\"name\":\"a\"}"));
    }

    /**
     * npm run in a member walks from there, nearest first: the root the host sent the edit to, then the directories
     * above it.
     */
    @Test
    void aMemberEditWalksFromTheMember() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\"}")
          .lock("{}")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/package.json"), "{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\"}")
          .packageManagerFile(Paths.get("package.json"), "{\"workspaces\":[\"repo\"]}")
          .build();

        Inputs inputs = NpmInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getRunsIn()).isEqualTo("packages/a");
        assertThat(inputs.getParentTrees().keySet()).containsExactly("packages/a", "repo/packages/a");
        assertThat(inputs.getParentTrees().get("packages/a")).containsOnly(
          entry("", "{\"private\":true,\"workspaces\":[\"packages/*\"]}"), entry("packages/a", "{\"name\":\"a\"}"));
        assertThat(inputs.getParentTrees().get("repo/packages/a")).containsOnly(
          entry("", "{\"workspaces\":[\"repo\"]}"), entry("repo", "{\"private\":true,\"workspaces\":[\"packages/*\"]}"),
          entry("repo/packages/a", "{\"name\":\"a\"}"));
    }

    /**
     * {@code npm prefix} (npm 11.17.0) run in packages/a, which holds its own lock, under these files, and the error
     * its walk raises where it fails ({@code npm-arborist/generate/npm-prefix-cases.js}).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
      "bin-dir-no-workspaces|{\"name\":\"root\",\"directories\":{\"bin\":\"bin\"}}||installs here",
      "bin-dir-lists|{\"name\":\"root\",\"directories\":{\"bin\":\"bin\"},\"workspaces\":[\"packages/*\"]}||" +
      "installs at ../..",
      "bin-dir-not-listing|{\"name\":\"root\",\"directories\":{\"bin\":\"bin\"},\"workspaces\":[\"libs/*\"]}||" +
      "installs here",
      "bin-dir-with-files-lists|{\"name\":\"root\",\"directories\":{\"bin\":\"bin\"}," +
      "\"workspaces\":[\"packages/*\"]}|bin/tool.js=#!/usr/bin/env node|installs at ../..",
      "bin-dir-number-lists|{\"name\":\"root\",\"directories\":{\"bin\":1},\"workspaces\":[\"packages/*\"]}||" +
      "installs here",
      "bin-dir-array-lists|{\"name\":\"root\",\"directories\":{\"bin\":[\"bin\"]},\"workspaces\":[\"packages/*\"]}|" +
      "|installs here",
      "mid-bin-dir-root-lists|{\"name\":\"root\",\"workspaces\":[\"packages/*\"]}|" +
      "packages/package.json={\"name\":\"mid\",\"directories\":{\"bin\":\"bin\"}}|installs at ../..",
      "unlisted-duplicate-names|{\"name\":\"root\",\"workspaces\":[\"libs/*\"]}|" +
      "libs/x/package.json={\"name\":\"dup\"};libs/y/package.json={\"name\":\"dup\"}|rejects EDUPLICATEWORKSPACE",
      "listed-duplicate-of-project|{\"name\":\"root\",\"workspaces\":[\"packages/*\"]}|" +
      "packages/b/package.json={\"name\":\"a\"}|rejects EDUPLICATEWORKSPACE",
      "unlisted-folder-names-collide|{\"name\":\"root\",\"workspaces\":[\"libs/*\",\"other/*\"]}|" +
      "libs/x/package.json={};other/x/package.json={}|rejects EDUPLICATEWORKSPACE",
      "unlisted-member-unparsable|{\"name\":\"root\",\"workspaces\":[\"libs/*\"]}|libs/x/package.json={|" +
      "rejects EJSONPARSE",
      "unlisted-member-null|{\"name\":\"root\",\"workspaces\":[\"libs/*\"]}|libs/x/package.json=null|" +
      "fails Can not normalize without content",
      "unlisted-member-bin-dir-number|{\"name\":\"root\",\"workspaces\":[\"libs/*\"]}|" +
      "libs/x/package.json={\"name\":\"x\",\"directories\":{\"bin\":1}}|fails ref.replace is not a function",
      "unlisted-member-bin-dir|{\"name\":\"root\",\"workspaces\":[\"libs/*\"]}|" +
      "libs/x/package.json={\"name\":\"x\",\"directories\":{\"bin\":\"bin\"}}|installs here",
      "listed-member-unparsable|{\"name\":\"root\",\"workspaces\":[\"packages/*\"]}|packages/b/package.json={|" +
      "rejects EJSONPARSE",
      "workspaces-not-an-array|{\"name\":\"root\",\"workspaces\":\"packages/*\"}||rejects EWORKSPACESCONFIG",
      "unlisted-duplicate-under-node-modules|{\"name\":\"root\",\"workspaces\":[\"libs/**\"]}|" +
      "libs/x/package.json={\"name\":\"dup\"};libs/x/node_modules/y/package.json={\"name\":\"dup\"}|installs here",
      "unlisted-member-name-number|{\"name\":\"root\",\"workspaces\":[\"libs/*\"]}|" +
      "libs/x/package.json={\"name\":5}|installs here",
      "listed-plus-member-name-number|{\"name\":\"root\",\"workspaces\":[\"packages/*\",\"libs/*\"]}|" +
      "libs/x/package.json={\"name\":5}|installs at ../.."
    })
    void npmMapsTheWorkspacesOfEachPackageJsonAboveOverWhatIsUnderIt(String name, String root,
                                                                     @Nullable String files, String outcome) {
        Request.RequestBuilder request = member().packageManagerFile(Paths.get("package.json"), root);
        if (files != null) {
            for (String file : files.split(";")) {
                request.packageManagerFile(Paths.get(file.substring(0, file.indexOf('='))),
                  file.substring(file.indexOf('=') + 1));
            }
        }
        LockDirectory dir = LockDirectory.of(request.build());

        Result result = new NpmLockRegenerator(NpmInputs.from(dir, HostRegistries.NONE), NO_REGISTRY)
          .regenerate();
        InstallRoot installRoot = NpmInputs.PORT.installRoot(dir);

        if ("installs here".equals(outcome)) {
            assertThat(result.getLockFileContent()).isEqualTo(MEMBER_LOCK);
            assertThat(installRoot.getDepth()).isEqualTo(0);
            assertThat(installRoot.isStopped()).isFalse();
            return;
        }
        assertThat(result.getFailure()).isNotNull();
        if ("installs at ../..".equals(outcome)) {
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
            assertThat(result.getFailure().getDetail()).contains("it installs at the workspace root ../..");
            assertThat(installRoot.getDepth()).isEqualTo(2);
        } else if (outcome.startsWith("rejects ")) {
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
            assertThat(result.getFailure().getDetail()).contains("(" + outcome.substring("rejects ".length()) + ")");
            assertThat(installRoot.isStopped()).isTrue();
        } else {
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_FAILS);
            assertThat(result.getFailure().getDetail()).endsWith(outcome.substring("fails ".length()));
            assertThat(installRoot.isStopped()).isTrue();
        }
    }

    /**
     * A package.json npm would map at a directory above that the host holds without its contents is unknown.
     */
    @Test
    void aPackageJsonAboveTheHostDidNotReadIsCannotKnow() {
        LockDirectory dir = LockDirectory.of(member()
          .packageManagerFile(Paths.get("package.json"), "{\"name\":\"root\",\"workspaces\":[\"libs/*\"]}")
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("package.json"), Paths.get("libs/x/package.json"),
            Paths.get("packages/a/package.json"))))
          .build());

        Result result = new NpmLockRegenerator(NpmInputs.from(dir, HostRegistries.NONE), NO_REGISTRY)
          .regenerate();

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).isEqualTo("the engine cannot know libs/x/package.json (its " +
                                                              "contents were not read)");
        assertThat(NpmInputs.PORT.installRoot(dir).isStopped()).isTrue();
    }

    /**
     * npm reads the package.json of each directory above to see whether its workspaces hold the project: one the host
     * holds without its contents is unknown.
     */
    @Test
    void aDirectoryAboveWhosePackageJsonTheHostDidNotReadIsCannotKnow() {
        LockDirectory dir = LockDirectory.of(member()
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("package.json"), Paths.get("packages/a/package.json"))))
          .build());

        Result result = new NpmLockRegenerator(NpmInputs.from(dir, HostRegistries.NONE), NO_REGISTRY)
          .regenerate();

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).contains("package.json (its contents were not read)");
        assertThat(NpmInputs.PORT.installRoot(dir).isStopped()).isTrue();
    }

    @Test
    void theHostsRegistriesAreNpmsUserConfig() {
        HostInputs host = HostRegistries.NONE.toBuilder().registry(new NodeRegistry(null,
          "https://registry.npmmirror.com/", null, null, null, null, false, null, true, false)).build();

        Inputs inputs = NpmInputs.from(LockDirectory.of(member().build()), host);

        assertThat(inputs.getUserNpmrc()).isEqualTo("registry=https://registry.npmmirror.com/\n");
    }

    // npm 11.17.0 run in repo/mid/app under a repo/.npmrc naming a mirror asks npmjs, or the registry its user config
    // names: the marker's project npmrc is that root's file, which npm does not read there
    @Test
    void aNestedProjectsUserConfigIsTheMarkersUserNpmrcNotTheRootsProjectNpmrc(@TempDir Path home) {
        Npmrc root = new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://registry.npmmirror.com/"));
        Npmrc user = new Npmrc(NpmrcScope.User, singletonMap("registry", "https://registry.yarnpkg.com/"));

        assertThat(userRegistry(home, "mid/app", root)).isEqualTo("https://registry.npmjs.org/");
        assertThat(userRegistry(home, "mid/app", root, user)).isEqualTo("https://registry.yarnpkg.com/");
    }

    // npm 11.17.0 with an empty user config and NPM_CONFIG_GLOBALCONFIG naming yarnpkg asks yarnpkg
    @Test
    void theMarkersGlobalNpmrcIsNpmsUserLevelConfigToo(@TempDir Path home) {
        Npmrc global = new Npmrc(NpmrcScope.Global, singletonMap("registry", "https://registry.yarnpkg.com/"));

        assertThat(userRegistry(home, "", global)).isEqualTo("https://registry.yarnpkg.com/");
        assertThat(userRegistry(home, "mid/app", global)).isEqualTo("https://registry.yarnpkg.com/");
    }

    private static String userRegistry(Path home, String dir, Npmrc... marker) {
        Request request = Request.builder()
          .packageJson("{\"name\":\"app\"}")
          .lock(MEMBER_LOCK)
          .packageJsonPath(Paths.get(dir, "package.json"))
          .marker(nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(Arrays.asList(marker)))
          .build();
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).npmrc(Arrays.asList(marker))
          .system(HostRegistries.environment(home)).build();
        Inputs inputs = NpmInputs.from(LockDirectory.of(request), host);
        return NpmOptions.fromNpmrc(inputs.getUserNpmrc(), inputs.getNpmrc()).registryFor("is-number");
    }

    private static Request.RequestBuilder member() {
        return Request.builder()
          .packageJson(MEMBER)
          .lock(MEMBER_LOCK)
          .packageJsonPath(Paths.get("packages/a/package.json"))
          .manifest(Paths.get("packages/a/package.json"), MEMBER);
    }

    /**
     * npm reads the .npmrc of the directory it installs in: one the host holds without its contents is unknown, as
     * for the other package managers.
     */
    @Test
    void anNpmrcTheHostHoldsButDidNotReadIsCannotKnow() {
        Request.RequestBuilder request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .lock("{\"name\":\"a\",\"version\":\"1.0.0\",\"lockfileVersion\":3,\"requires\":true,\"packages\":" +
                "{\"\":{\"name\":\"a\",\"version\":\"1.0.0\"}}}\n")
          .packageJsonPath(Paths.get("app/package.json"))
          .manifest(Paths.get("app/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/.npmrc"))));

        Inputs unread = NpmInputs.from(LockDirectory.of(request.build()), HostRegistries.NONE);
        Result result = new NpmLockRegenerator(unread, NO_REGISTRY).regenerate();

        assertThat(unread.getPaths()).contains(".npmrc");
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).isEqualTo("the engine cannot know .npmrc (its contents were not " +
                                                              "read)");

        Inputs read = NpmInputs.from(LockDirectory.of(request.packageManagerFile(Paths.get("app/.npmrc"),
          "legacy-peer-deps=true\n").build()), HostRegistries.NONE);
        assertThat(new NpmLockRegenerator(read, NO_REGISTRY).regenerate().isSuccess()).isTrue();
    }

    /**
     * npm reads a yarn.lock beside its lock and rewrites it (open-yarn-lock-incremental's after-yarn): the host holds
     * it without its contents.
     */
    @Test
    void aYarnLockBesideTheLockIsCannotKnow() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .lock("{\"name\":\"a\",\"version\":\"1.0.0\",\"lockfileVersion\":3,\"requires\":true,\"packages\":" +
                "{\"\":{\"name\":\"a\",\"version\":\"1.0.0\"}}}\n")
          .packageJsonPath(Paths.get("app/package.json"))
          .manifest(Paths.get("app/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/yarn.lock"))))
          .build();

        Result result = new NpmLockRegenerator(NpmInputs.from(LockDirectory.of(request), HostRegistries.NONE),
          NO_REGISTRY).regenerate();

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail())
          .isEqualTo("the engine cannot know yarn.lock (its contents were not read)");
    }

    /**
     * npm 11.17.0 run beside an npm-shrinkwrap.json and a package-lock.json rewrites the shrinkwrap and leaves the lock
     * as it was (shrinkwrap.js load).
     */
    @Test
    void anNpmShrinkwrapBesideTheLockIsTheLockNpmWrites() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .lock("{\"name\":\"a\",\"version\":\"1.0.0\",\"lockfileVersion\":3,\"requires\":true,\"packages\":" +
                "{\"\":{\"name\":\"a\",\"version\":\"1.0.0\"}}}\n")
          .packageJsonPath(Paths.get("app/package.json"))
          .manifest(Paths.get("app/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("app/package.json"),
            Paths.get("app/npm-shrinkwrap.json"))))
          .build();

        Result result = new NpmLockRegenerator(NpmInputs.from(LockDirectory.of(request), HostRegistries.NONE),
          NO_REGISTRY).regenerate();

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).isEqualTo("the engine cannot know the lock npm writes: it reads " +
                                                              "and writes the npm-shrinkwrap.json beside this lock " +
                                                              "in its place");
    }
}

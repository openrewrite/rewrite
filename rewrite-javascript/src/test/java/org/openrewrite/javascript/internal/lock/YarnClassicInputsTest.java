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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.NodeRegistryCredentials;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.yarnclassic.RegistrySource;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class YarnClassicInputsTest {

    private static final RegistrySource NO_REGISTRY = new RegistrySource() {
        @Override
        public @Nullable String packument(String registry, String name, @Nullable String authorization) {
            return null;
        }

        @Override
        public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
            return null;
        }
    };

    @Test
    void memberEditPlacesManifestsAndReadsTheRootConfig() {
        Request.RequestBuilder builder = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"2.0.0\"}")
          .lock("# yarn lockfile v1\n")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/package.json"), "{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .manifest(Paths.get("repo/local/package.json"), "{\"name\":\"local\",\"version\":\"1.0.0\"}")
          .packageManagerFile(Paths.get("repo/.yarnrc"), "registry \"https://registry.npmjs.org\"\n")
          .packageManagerFile(Paths.get("repo/.npmrc"), "registry=https://registry.npmmirror.com/\n")
          .packageManagerFile(Paths.get("repo/packages/a/.yarnrc"), "registry \"https://example.com\"\n");

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(builder.build()), Host.UNKNOWN, null, null);

        assertThat(inputs.getLock()).isEqualTo("# yarn lockfile v1\n");
        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a", "local");
        assertThat(inputs.getManifests().get("packages/a")).isEqualTo("{\"name\":\"a\",\"version\":\"2.0.0\"}");
        assertThat(inputs.getConfigFiles()).containsOnlyKeys(".yarnrc", ".npmrc", "packages/a/.yarnrc");
        assertThat(inputs.getConfigFiles().get(".yarnrc")).isEqualTo("registry \"https://registry.npmjs.org\"\n");
        assertThat(inputs.getConfigFiles().get(".npmrc")).isEqualTo("registry=https://registry.npmmirror.com/\n");
        assertThat(inputs.getPaths()).isNull();
        assertThat(inputs.getEnv()).isNull();
        assertThat(inputs.getCwd()).isEqualTo("packages/a");
        Inputs fromRoot = YarnClassicInputs.from(
          LockDirectory.of(builder.packageJsonPath(Paths.get("repo/package.json"))
          .packageJson("{\"private\":true,\"workspaces\":[\"packages/*\"]}").build()), Host.UNKNOWN, null, null);
        assertThat(fromRoot.getCwd()).isEmpty();
        assertThat(fromRoot.getConfigFiles()).containsOnlyKeys(".yarnrc", ".npmrc");
    }

    @Test
    void thePathsUnderTheRootAreTheProjectsPaths() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("repo/package.json"))
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("repo/package.json"), Paths.get("repo/.yarnrc"),
            Paths.get("other/.npmrc"))))
          .build();

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN, null, null);

        assertThat(inputs.getPaths()).containsExactlyInAnyOrder("package.json", ".yarnrc");
        assertThat(inputs.getConfigFiles()).isEmpty();
        assertThat(new YarnClassicLockRegenerator(inputs, NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: the engine cannot know .yarnrc (its contents were not read)");
    }

    /**
     * yarn reads .yarnrc and .npmrc from every directory above the project (npm-registry.js
     * {@code getPossibleConfigLocations}, rc.js), so a repository's own config reaches a project below it.
     */
    @Test
    void theDirectoriesAboveTheProjectGiveTheirConfigFiles() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("repo/web/app/package.json"))
          .packageManagerFile(Paths.get("repo/web/.yarnrc"), "registry \"https://registry.npmjs.org\"\n")
          .packageManagerFile(Paths.get(".npmrc"), "registry=https://registry.npmmirror.com/\n")
          .packageManagerFile(Paths.get("repo/other/.npmrc"), "registry=https://example.com/\n")
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("repo/web/app/package.json"),
            Paths.get("repo/web/.yarnrc"), Paths.get("repo/.yarnrc.yml"), Paths.get(".npmrc"),
            Paths.get("README.md"), Paths.get("repo/other/.npmrc"), Paths.get("repo/web/src/index.js"))))
          .build();

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN, null, null);

        assertThat(inputs.getConfigFiles()).containsOnlyKeys("../.yarnrc", "../../../.npmrc");
        assertThat(inputs.getPaths()).containsExactlyInAnyOrder("package.json", "../.yarnrc", "../../.yarnrc.yml",
          "../../../.npmrc", "../../../README.md");
        assertThat(new YarnClassicLockRegenerator(inputs, NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: the engine cannot know ../../.yarnrc.yml (its contents were not read)");
    }

    /**
     * yarn installs at the nearest directory above the project whose workspaces list the project's path from it
     * (config.js {@code findWorkspaceRoot}, {@code open-parent-workspaces-lists-project}).
     */
    @Test
    void theProjectsDirectoryDecidesWhetherARepositorysWorkspacesListIt() {
        Request.RequestBuilder request = Request.builder()
          .packageJson("{\"name\":\"app\",\"version\":\"1.0.0\"}")
          .lock("# yarn lockfile v1\n\n\n")
          .packageJsonPath(Paths.get("web/app/package.json"))
          .packageManagerFile(Paths.get("package.json"), "{\"private\":true,\"workspaces\":[\"web/*\"]}");

        Inputs listed = YarnClassicInputs.from(LockDirectory.of(request.build()), Host.UNKNOWN, null, null);

        assertThat(listed.getDirectory()).isEqualTo("web/app");
        assertThat(new YarnClassicLockRegenerator(listed, NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: the engine cannot know the lock yarn writes for this edit: yarn installs " +
                     "at the workspace root ../../, whose workspaces list web/app, and writes ../../yarn.lock, not " +
                     "this project's");

        Inputs unlisted = YarnClassicInputs.from(
          LockDirectory.of(request.packageJsonPath(Paths.get("tools/app/package.json")).build()), Host.UNKNOWN, null,
          null);

        assertThat(unlisted.getDirectory()).isEqualTo("tools/app");
        assertThat(new YarnClassicLockRegenerator(unlisted, NO_REGISTRY).regenerate().isSuccess())
          .isTrue();
    }

    /**
     * yarn run in a member that a lockless root lists installs at that root and creates its lock, leaving the
     * member's own lock as it is ({@code probes/sr-walk/yarn-rootless-own}): the walk answers the root.
     */
    @Test
    void aMemberTheRootListsInstallsAtTheRootThoughItHoldsNoLock() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"ms\":\"2.1.3\"}}")
          .lock("# yarn lockfile v1\n\n\n")
          .packageJsonPath(Paths.get("packages/a/package.json"))
          .packageManagerFile(Paths.get("package.json"), "{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .build();

        InstallRoot root = YarnClassicInputs.installRoot(LockDirectory.of(request));

        assertThat(root.isStopped()).isFalse();
        assertThat(root.getDepth()).isEqualTo(2);
    }

    /**
     * The host's registries are the home directory's .npmrc ({@code user-npmrc-registry-bump}); npm's default,
     * the host's answer when nothing names a registry, stays unnamed.
     */
    @Test
    void theHostsRegistriesAreTheHomeDirectorysNpmrc() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("package.json"))
          .build();
        Map<String, NodeRegistry> scopes = new LinkedHashMap<>();
        scopes.put("@types", registry("https://registry.npmmirror.com/"));

        Inputs mirrored = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN,
          new NodeRegistries(registry("https://registry.npmmirror.com/"), scopes, null, null, null), null);
        Inputs unnamed = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN,
          new NodeRegistries(registry("https://registry.npmjs.org/"), new LinkedHashMap<>(), null, null, null), null);

        assertThat(mirrored.getHomeFiles()).containsOnlyKeys(".npmrc");
        assertThat(mirrored.getHomeFiles().get(".npmrc"))
          .isEqualTo("registry=https://registry.npmmirror.com/\n@types:registry=https://registry.npmmirror.com/\n");
        assertThat(mirrored.getConfigFiles()).isEmpty();
        assertThat(unnamed.getHomeFiles().get(".npmrc")).isEmpty();
    }

    /**
     * The host's credentials are the home .npmrc's, keyed by registry as yarn's getRegistryOption reads them, with
     * always-auth, so yarn sends them with every request to their registry (ConfigTest.sendsTheCredentialsYarnSends).
     */
    @Test
    void theHostsCredentialsAreTheHomeDirectorysNpmrc() {
        Map<String, NodeRegistry> scopes = new LinkedHashMap<>();
        scopes.put("@corp", new NodeRegistry("@corp", "https://corp.example.com/", "corp-token", null, null, null,
          false, null, true, false));
        scopes.put("@open", new NodeRegistry("@open", "https://open.example.com/", null, null, null, null, true, null,
          true, false));
        NodeRegistry registry = new NodeRegistry(null, "https://r.example/npm", null, "u", "p", null, false, null,
          true, false);

        NodeRegistries user = new NodeRegistries(registry, scopes, null, null, null);
        assertThat(YarnClassicInputs.userNpmrc(user, user)).isEqualTo(
          "registry=https://r.example/npm\n//r.example/npm/:username=\"u\"\n//r.example/npm/:_password=\"cA==\"\n" +
          "//r.example/npm/:always-auth=true\n@corp:registry=https://corp.example.com/\n" +
          "//corp.example.com/:_authToken=\"corp-token\"\n//corp.example.com/:always-auth=true\n" +
          "@open:registry=https://open.example.com/\n//open.example.com/:always-auth=true\n");
    }

    /**
     * The host's token for the private registry a project's .npmrc names goes with an unscoped package's request too,
     * which yarn makes with credentials only under always-auth ({@code request-authorization.jsonl}).
     */
    @Test
    void theHostsCredentialsGoWithEveryRequestToTheirRegistry(@TempDir Path home) {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"^1.0.0\"}}")
          .lock("# yarn lockfile v1\n\n\n")
          .packageJsonPath(Paths.get("package.json"))
          .packageManagerFile(Paths.get(".npmrc"), "registry=https://corp.example/\n")
          .marker(nodeResolutionResult(PackageManager.YarnClassic).withNpmrcConfigs(singletonList(
            new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://corp.example/")))))
          .build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        NodeExecutionContextView.view(ctx).setRegistryCredentials(singletonList(
          new NodeRegistryCredentials("corp.example", "corp-token", null, null)));

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN,
          YarnClassicInputs.userConfig(ctx, request.getMarker(), scratch(home)),
          RegistryDiscovery.discover(ctx, request.getMarker(), scratch(home)));

        assertThat(inputs.getHomeFiles().get(".npmrc"))
          .isEqualTo("//corp.example/:_authToken=\"corp-token\"\n//corp.example/:always-auth=true\n");
        assertThat(asked(inputs)).containsExactly(entry("https://corp.example/a", "Bearer corp-token"));
    }

    private static Map<String, @Nullable String> asked(Inputs inputs) {
        Map<String, @Nullable String> asked = new LinkedHashMap<>();
        new YarnClassicLockRegenerator(inputs, new RegistrySource() {
            @Override
            public @Nullable String packument(String registry, String name, @Nullable String authorization) {
                asked.put(registry + name, authorization);
                return null;
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version,
                                             @Nullable String authorization) {
                return null;
            }
        }).regenerate();
        return asked;
    }

    /**
     * A registry only the project's .npmrc names keeps the credentials the host holds for it, as a user's own .npmrc
     * holds them, while the registry the package comes from stays the project's choice.
     */
    @Test
    void theHostsCredentialsForARegistryTheProjectNamesAreTheHomeDirectorysToo() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"@corp/a\":\"^1.0.0\"}}")
          .lock("")
          .packageJsonPath(Paths.get("package.json"))
          .packageManagerFile(Paths.get(".npmrc"), "@corp:registry=https://corp.example.com/\n")
          .build();
        NodeRegistries user = new NodeRegistries(registry("https://registry.npmjs.org/"), new LinkedHashMap<>(),
          null, null, null);
        Map<String, NodeRegistry> scopes = new LinkedHashMap<>();
        scopes.put("@corp", new NodeRegistry("@corp", "https://corp.example.com/", "corp-token", null, null, null,
          false, null, true, false));
        NodeRegistries credentials = new NodeRegistries(registry("https://registry.npmjs.org/"), scopes, null, null,
          null);

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN, user, credentials);

        assertThat(inputs.getHomeFiles().get(".npmrc"))
          .isEqualTo("//corp.example.com/:_authToken=\"corp-token\"\n//corp.example.com/:always-auth=true\n");
        assertThat(asked(inputs)).containsExactly(entry("https://corp.example.com/@corp/a", "Bearer corp-token"));
    }

    /**
     * yarn 1.22.22 run in packages/a asks the registry the home .npmrc names over the root's
     * ({@code user-npmrc-member-root-npmrc}): the marker's user npmrc is the home one, its project npmrc the root's.
     */
    @Test
    void aMembersHomeNpmrcIsTheMarkersUserNpmrcNotTheRootsProjectNpmrc(@TempDir Path home) {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"ms\":\"2.1.3\"}}")
          .lock("# yarn lockfile v1\n\n\n")
          .packageJsonPath(Paths.get("packages/a/package.json"))
          .manifest(Paths.get("package.json"), "{\"name\":\"root\",\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .manifest(Paths.get("packages/a/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .marker(nodeResolutionResult(PackageManager.YarnClassic).withNpmrcConfigs(Arrays.asList(
            new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://registry.npmjs.org/")),
            new Npmrc(NpmrcScope.User, singletonMap("registry", "https://registry.npmmirror.com/")))))
          .build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        NodeRegistries user = YarnClassicInputs.userConfig(ctx, request.getMarker(), scratch(home));
        NodeRegistries all = RegistryDiscovery.discover(ctx, request.getMarker(), scratch(home));

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN, user, all);

        assertThat(inputs.getHomeFiles()).containsEntry(".npmrc", "registry=https://registry.npmmirror.com/\n");
        assertThat(inputs.getConfigFiles()).containsEntry(".npmrc", "registry=https://registry.npmjs.org/\n");
        assertThat(asked(inputs)).containsOnlyKeys("https://registry.npmmirror.com/ms");
    }

    // a scratch HOME without a .netrc, and no variables
    private static Environment scratch(Path home) {
        return new Environment() {
            @Override
            public @Nullable String getenv(String name) {
                return null;
            }

            @Override
            public Path userHome() {
                return home;
            }

            @Override
            public String osName() {
                return "Mac OS X";
            }
        };
    }

    private static NodeRegistry registry(String url) {
        return new NodeRegistry(null, url, null, null, null, null, false, null, true, false);
    }

    /**
     * Without corepack, yarn 1.22.22 stops on the nearest packageManager from the project's directory up
     * ({@code open-parent-package-manager-pnpm}).
     */
    @Test
    void theDirectoriesAboveTheProjectGiveTheirPackageJson() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\"}")
          .lock("# yarn lockfile v1\n\n\n")
          .packageJsonPath(Paths.get("repo/web/app/package.json"))
          .packageManagerFile(Paths.get("repo/web/package.json"), "{\"packageManager\":\"pnpm@9.15.0\"}")
          .packageManagerFile(Paths.get("package.json"), "{\"name\":\"top\"}")
          .packageManagerFile(Paths.get("repo/other/package.json"), "{\"packageManager\":\"yarn@4.10.3\"}")
          .build();

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN, null, null);

        assertThat(inputs.getConfigFiles()).containsOnlyKeys("../package.json", "../../../package.json");
        assertThat(new YarnClassicLockRegenerator(inputs, NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: yarn rejects this edit: This project's package.json defines " +
                     "\"packageManager\": \"yarn@pnpm@9.15.0\". However the current global version of Yarn is " +
                     "1.22.22.");
    }
}

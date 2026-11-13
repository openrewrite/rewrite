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
import org.openrewrite.javascript.NodeRegistryCredentials;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class YarnClassicInputsTest {

    private static final RegistrySource NO_REGISTRY = new RegistrySource() {
        @Override
        public @Nullable String packument(String registry, String name, boolean full, @Nullable String authorization) {
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

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(builder.build()), HostRegistries.NONE);

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
          .packageJson("{\"private\":true,\"workspaces\":[\"packages/*\"]}").build()), HostRegistries.NONE);
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

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), HostRegistries.NONE);

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

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), HostRegistries.NONE);

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

        Inputs listed = YarnClassicInputs.from(LockDirectory.of(request.build()), HostRegistries.NONE);

        assertThat(listed.getDirectory()).isEqualTo("web/app");
        assertThat(new YarnClassicLockRegenerator(listed, NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: the engine cannot know the lock yarn writes for this edit: yarn installs " +
                     "at the workspace root ../../, whose workspaces list web/app, and writes ../../yarn.lock, not " +
                     "this project's");

        Inputs unlisted = YarnClassicInputs.from(
          LockDirectory.of(request.packageJsonPath(Paths.get("tools/app/package.json")).build()), HostRegistries.NONE);

        assertThat(unlisted.getDirectory()).isEqualTo("tools/app");
        assertThat(new YarnClassicLockRegenerator(unlisted, NO_REGISTRY).regenerate().isSuccess())
          .isTrue();
    }

    /**
     * yarn run in a member that a lockless root lists installs at that root and creates its lock, leaving the
     * member's own lock as it is: the walk answers the root.
     */
    @Test
    void aMemberTheRootListsInstallsAtTheRootThoughItHoldsNoLock() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"1.0.0\",\"dependencies\":{\"ms\":\"2.1.3\"}}")
          .lock("# yarn lockfile v1\n\n\n")
          .packageJsonPath(Paths.get("packages/a/package.json"))
          .packageManagerFile(Paths.get("package.json"), "{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .build();

        InstallRoot root = YarnClassicInputs.PORT.installRoot(LockDirectory.of(request));

        assertThat(root.isStopped()).isFalse();
        assertThat(root.getDepth()).isEqualTo(2);
    }

    /**
     * The host's registries are the home directory's .npmrc ({@code user-npmrc-registry-bump}); a default the host
     * names is named though it is npm's own, as yarn 1.22.22 then asks npmjs over a project .yarnrc (recorded).
     */
    @Test
    void theHostsRegistriesAreTheHomeDirectorysNpmrc() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("package.json"))
          .build();
        Map<String, String> npmjs = singletonMap("registry", "https://registry.npmjs.org/");

        Inputs mirrored = YarnClassicInputs.from(LockDirectory.of(request), HostRegistries.NONE.toBuilder()
          .registry(registry(null, "https://registry.npmmirror.com/"))
          .registry(registry("@types", "https://registry.npmmirror.com/")).build());
        Inputs named = YarnClassicInputs.from(LockDirectory.of(request), HostRegistries.NONE.toBuilder()
          .npmrc(new Npmrc(NpmrcScope.User, npmjs)).build());
        Inputs viewed = YarnClassicInputs.from(LockDirectory.of(request), HostRegistries.NONE.toBuilder()
          .registry(registry(null, "https://registry.npmjs.org/")).build());
        Inputs unnamed = YarnClassicInputs.from(LockDirectory.of(request), HostRegistries.NONE.toBuilder()
          .npmrc(new Npmrc(NpmrcScope.Project, npmjs)).build());

        assertThat(mirrored.getHomeFiles()).containsOnlyKeys(".npmrc");
        assertThat(mirrored.getHomeFiles().get(".npmrc"))
          .isEqualTo("registry=https://registry.npmmirror.com/\n@types:registry=https://registry.npmmirror.com/\n");
        assertThat(mirrored.getConfigFiles()).isEmpty();
        assertThat(named.getHomeFiles().get(".npmrc")).isEqualTo("registry=https://registry.npmjs.org/\n");
        assertThat(viewed.getHomeFiles().get(".npmrc")).isEqualTo("registry=https://registry.npmjs.org/\n");
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

        HostInputs host = HostRegistries.NONE.toBuilder().registry(registry).registries(scopes.values()).build();
        assertThat(host.userNpmrc(HostInputs.GLOBAL_AND_USER)).isEqualTo(
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
          .build();
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(scratch(home))
          .npmrc(new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://corp.example/")))
          .credential(new NodeRegistryCredentials("corp.example", "corp-token", null, null))
          .build();

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), host);

        assertThat(inputs.getHomeFiles().get(".npmrc"))
          .isEqualTo("//corp.example/:_authToken=\"corp-token\"\n//corp.example/:always-auth=true\n");
        assertThat(asked(inputs)).containsExactly(entry("https://corp.example/a", "Bearer corp-token"));
    }

    /**
     * The home .npmrc holds the host's credentials only, never the project .npmrc's own, so yarn 1.22.22 sends what
     * it sends over the two files: a project's token for an unscoped package only under always-auth.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "-", value = {
      "-|-|-",
      "//corp.example/:_authToken=project-token|-|-",
      "//corp.example/:_authToken=project-token|corp-token|Bearer project-token",
      "always-auth=false|corp-token|Bearer corp-token",
      "//corp.example/:always-auth=false|corp-token|-",
      "//corp.example/:_authToken=project-token;always-auth=true|-|Bearer project-token"
    })
    void theProjectsOwnCredentialsStayInItsNpmrc(@Nullable String project, @Nullable String hostToken,
                                                 @Nullable String sent, @TempDir Path home) {
        Map<String, String> npmrc = new LinkedHashMap<>();
        npmrc.put("registry", "https://corp.example/");
        StringBuilder lines = new StringBuilder("registry=https://corp.example/\n");
        if (project != null) {
            for (String line : project.split(";")) {
                npmrc.put(line.substring(0, line.indexOf('=')), line.substring(line.indexOf('=') + 1));
                lines.append(line).append('\n');
            }
        }
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"^1.0.0\"}}")
          .lock("# yarn lockfile v1\n\n\n")
          .packageJsonPath(Paths.get("package.json"))
          .packageManagerFile(Paths.get(".npmrc"), lines.toString())
          .build();
        HostInputs.HostInputsBuilder host = HostInputs.builder().now(Instant.EPOCH).system(scratch(home))
          .npmrc(new Npmrc(NpmrcScope.Project, npmrc));
        if (hostToken != null) {
            host.credential(new NodeRegistryCredentials("corp.example", hostToken, null, null));
        }

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), host.build());

        assertThat(inputs.getHomeFiles().get(".npmrc")).isEqualTo(hostToken == null ? "" :
          "//corp.example/:_authToken=\"" + hostToken + "\"\n//corp.example/:always-auth=true\n");
        assertThat(asked(inputs)).containsExactly(entry("https://corp.example/a", sent));
    }

    /**
     * A user's global always-auth stays global in the home .npmrc, so a project's {@code always-auth=false} turns it
     * off for the project's own token, as yarn 1.22.22 sends it (recorded with an auth-logging registry).
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "-", value = {
      "always-auth=false|-",
      "-|Bearer project-token"
    })
    void aUsersGlobalAlwaysAuthStaysGlobal(@Nullable String projectAlwaysAuth, @Nullable String sent,
                                          @TempDir Path home) {
        Map<String, String> project = new LinkedHashMap<>();
        project.put("registry", "https://corp.example/");
        project.put("//corp.example/:_authToken", "project-token");
        StringBuilder lines = new StringBuilder("registry=https://corp.example/\n" +
                                                "//corp.example/:_authToken=project-token\n");
        if (projectAlwaysAuth != null) {
            project.put("always-auth", "false");
            lines.append(projectAlwaysAuth).append('\n');
        }
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"^1.0.0\"}}")
          .lock("# yarn lockfile v1\n\n\n")
          .packageJsonPath(Paths.get("package.json"))
          .packageManagerFile(Paths.get(".npmrc"), lines.toString())
          .build();
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(scratch(home))
          .npmrc(new Npmrc(NpmrcScope.User, singletonMap("always-auth", "true")))
          .npmrc(new Npmrc(NpmrcScope.Project, project)).build();

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), host);

        assertThat(inputs.getHomeFiles().get(".npmrc")).isEqualTo("always-auth=true\n");
        assertThat(asked(inputs)).containsExactly(entry("https://corp.example/a", sent));
    }

    /**
     * A credential in the URL of a registry the project's .npmrc names is that file's too.
     */
    @Test
    void theHostsCredentialsLeaveTheUserinfoOfTheProjectsRegistry(@TempDir Path home) {
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(scratch(home))
          .npmrc(new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://u:p@corp.example/"))).build();

        assertThat(host.userNpmrc(HostInputs.GLOBAL_AND_USER)).isEmpty();
    }

    private static Map<String, @Nullable String> asked(Inputs inputs) {
        Map<String, @Nullable String> asked = new LinkedHashMap<>();
        new YarnClassicLockRegenerator(inputs, new RegistrySource() {
            @Override
            public @Nullable String packument(String registry, String name, boolean full,
                                              @Nullable String authorization) {
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
        HostInputs host = HostRegistries.NONE.toBuilder()
          .npmrc(new Npmrc(NpmrcScope.Project, singletonMap("@corp:registry", "https://corp.example.com/")))
          .credential(new NodeRegistryCredentials("corp.example.com", "corp-token", null, null))
          .build();

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), host);

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
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(scratch(home))
          .npmrc(requireNonNull(request.getMarker().getNpmrcConfigs())).build();

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), host);

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

    private static NodeRegistry registry(@Nullable String scope, String url) {
        return new NodeRegistry(scope, url, null, null, null, null, false, null, true, false);
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

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getConfigFiles()).containsOnlyKeys("../package.json", "../../../package.json");
        assertThat(new YarnClassicLockRegenerator(inputs, NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: yarn rejects this edit: This project's package.json defines " +
                     "\"packageManager\": \"yarn@pnpm@9.15.0\". However the current global version of Yarn is " +
                     "1.22.22.");
    }
}

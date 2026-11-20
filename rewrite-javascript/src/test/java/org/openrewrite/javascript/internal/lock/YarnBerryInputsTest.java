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
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.NodeRegistryCredentials;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class YarnBerryInputsTest {

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

    /**
     * Yarn 4.10.3 run in a member reads every .yarnrc.yml from there up (open-member-yarnrc-run-in-member), not a
     * sibling's.
     */
    @Test
    void memberEditPlacesManifestsAndReadsTheRcFilesFromTheMemberUpToTheRepository() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"2.0.0\"}")
          .lock("__metadata:\n  version: 8\n")
          .packageJsonPath(Paths.get("repo/app/packages/a/package.json"))
          .manifest(Paths.get("repo/app/package.json"), "{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .manifest(Paths.get("repo/app/packages/a/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .packageManagerFile(Paths.get("repo/package.json"), "{\"name\":\"above\"}")
          .packageManagerFile(Paths.get("repo/app/.yarnrc.yml"), "compressionLevel: 0\n")
          .packageManagerFile(Paths.get("repo/.yarnrc.yml"), "enableTelemetry: false\n")
          .packageManagerFile(Paths.get("repo/app/packages/a/.yarnrc.yml"), "npmRegistryServer: x\n")
          .packageManagerFile(Paths.get("repo/app/packages/b/.yarnrc.yml"), "npmRegistryServer: y\n")
          .packageManagerFile(Paths.get("repo/app/.npmrc"), "registry=https://example.com/\n")
          .build();

        Inputs inputs = YarnBerryInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getLock()).isEqualTo("__metadata:\n  version: 8\n");
        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getManifests().get("packages/a")).isEqualTo("{\"name\":\"a\",\"version\":\"2.0.0\"}");
        assertThat(inputs.getRunsIn()).isEqualTo("packages/a");
        assertThat(inputs.getConfigFiles()).containsOnlyKeys(".yarnrc.yml", "../.yarnrc.yml", "packages/a/.yarnrc.yml",
          "../package.json");
        assertThat(inputs.getConfigFiles().get(".yarnrc.yml")).isEqualTo("compressionLevel: 0\n");
        assertThat(inputs.getPaths()).isNull();
        assertThat(inputs.getEnv()).isNull();
        assertThat(inputs.getNow()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void thePathsUnderTheRootAreTheProjectsPaths() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("repo/package.json"))
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("repo/package.json"), Paths.get("repo/.yarnrc.yml"),
            Paths.get("repo/packages/a/package.json"), Paths.get("other/.yarnrc.yml"))))
          .build();

        Inputs inputs = YarnBerryInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getPaths()).containsExactlyInAnyOrder("package.json", ".yarnrc.yml",
          "packages/a/package.json");
        assertThat(inputs.getConfigFiles()).isEmpty();
    }

    /**
     * The host's registries are the home directory's .yarnrc.yml ({@code user-yarnrc-registry-bump},
     * {@code user-yarnrc-scoped-add}); where the host names no default, none is written.
     */
    @Test
    void theHostsRegistriesAreTheHomeDirectorysYarnrc() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("package.json"))
          .build();
        Inputs mirrored = YarnBerryInputs.from(LockDirectory.of(request), HostRegistries.NONE.toBuilder()
          .registry(registry(null, "https://registry.npmmirror.com/"))
          .registry(registry("@types", "https://registry.npmmirror.com/")).build());
        Inputs unnamed = YarnBerryInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(mirrored.getHomeFiles()).containsOnlyKeys(".yarnrc.yml");
        assertThat(mirrored.getHomeFiles().get(".yarnrc.yml")).isEqualTo(
          "npmRegistryServer: \"https://registry.npmmirror.com/\"\n" +
          "npmScopes:\n  \"types\":\n    npmRegistryServer: \"https://registry.npmmirror.com/\"\n");
        assertThat(mirrored.getConfigFiles()).isEmpty();
        assertThat(unnamed.getHomeFiles().get(".yarnrc.yml")).isEmpty();
    }

    /**
     * The host's credentials are their registry's npmRegistries entry, with npmAlwaysAuth, and a scope's own settings,
     * where NpmHttpUtils.getAuthenticationHeader finds them (NpmHttpUtilsTest.readsTheRegistriesEntriesAsYarnDoes).
     */
    @Test
    void theHostsCredentialsAreTheHomeDirectorysYarnrc() {
        Map<String, NodeRegistry> scopes = new LinkedHashMap<>();
        scopes.put("@corp", new NodeRegistry("@corp", "https://corp.example.com/", "corp-token", null, null, null,
          false, null, true, false));
        NodeRegistry registry = new NodeRegistry(null, "https://r.example/npm/", null, "u", "p", null, false, null,
          true, false);

        HostInputs host = HostRegistries.NONE.toBuilder().registry(registry).registries(scopes.values()).build();
        assertThat(host.userYarnrc()).isEqualTo(
          "npmRegistryServer: \"https://r.example/npm/\"\nnpmRegistries:\n  \"//r.example/npm\":\n" +
          "    npmAuthIdent: \"u:p\"\n    npmAlwaysAuth: true\n  \"//corp.example.com\":\n" +
          "    npmAuthToken: \"corp-token\"\n    npmAlwaysAuth: true\nnpmScopes:\n  \"corp\":\n" +
          "    npmRegistryServer: \"https://corp.example.com/\"\n    npmAuthToken: \"corp-token\"\n");
    }

    /**
     * Yarn 4.10.3 reads no .npmrc (recorded), so a user npmrc's always-auth without credentials names nothing; as an
     * npmAlwaysAuth entry it would fail a project naming that registry with YN0033.
     */
    @Test
    void alwaysAuthWithoutCredentialsIsNoSetting() {
        Map<String, String> npmrc = new LinkedHashMap<>();
        npmrc.put("always-auth", "true");
        npmrc.put("@corp:registry", "https://corp.example.com/");
        HostInputs host = HostRegistries.NONE.toBuilder().npmrc(new Npmrc(NpmrcScope.User, npmrc)).build();

        assertThat(host.userYarnrc()).isEqualTo(
          "npmScopes:\n  \"corp\":\n    npmRegistryServer: \"https://corp.example.com/\"\n");
    }

    /**
     * The host's token for the registry a project's .yarnrc.yml names goes with an unscoped package's request, as Yarn
     * 4.10.3 sends a home entry's whatever the project's top-level npmAlwaysAuth says (getAuthConfiguration).
     */
    @Test
    void theHostsCredentialsGoWithEveryRequestToTheirRegistry(@TempDir Path home) {
        for (String alwaysAuth : new String[]{"", "npmAlwaysAuth: true\n", "npmAlwaysAuth: false\n"}) {
            Request request = Request.builder()
              .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"packageManager\":\"yarn@4.10.3\"," +
                           "\"dependencies\":{\"a\":\"^1.0.0\"}}")
              .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n" +
                    "  version: 0.0.0-use.local\n  resolution: \"p@workspace:.\"\n  languageName: unknown\n" +
                    "  linkType: soft\n")
              .packageJsonPath(Paths.get("package.json"))
              .packageManagerFile(Paths.get(".yarnrc.yml"), "npmRegistryServer: \"https://corp.example\"\n" +
                                                              alwaysAuth)
              .build();
            HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(scratch(home))
              .npmrc(new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://corp.example/")))
              .credential(new NodeRegistryCredentials("corp.example", "corp-token", null, null))
              .build();

            Inputs inputs = YarnBerryInputs.from(LockDirectory.of(request), host);

            assertThat(inputs.getHomeFiles().get(".yarnrc.yml")).isEqualTo(
              "npmRegistries:\n  \"//corp.example\":\n    npmAuthToken: \"corp-token\"\n    npmAlwaysAuth: true\n");
            assertThat(asked(inputs)).as(alwaysAuth)
              .containsExactly(entry("https://corp.example a", "Bearer corp-token"));
        }
    }

    /**
     * Yarn 4.10.3 never reads the project's .npmrc, so its token stays out of the home file and no request carries it;
     * the host's own credentials for that registry still go.
     */
    @Test
    void theProjectsNpmrcCredentialsNeverReachTheHomeYarnrc(@TempDir Path home) {
        Map<String, String> npmrc = new LinkedHashMap<>();
        npmrc.put("registry", "https://corp.example/");
        npmrc.put("//corp.example/:_authToken", "project-token");
        npmrc.put("always-auth", "true");
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"packageManager\":\"yarn@4.10.3\"," +
                       "\"dependencies\":{\"a\":\"^1.0.0\"}}")
          .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n  version: 0.0.0-use.local\n" +
                "  resolution: \"p@workspace:.\"\n  languageName: unknown\n  linkType: soft\n")
          .packageJsonPath(Paths.get("package.json"))
          .packageManagerFile(Paths.get(".yarnrc.yml"), "npmRegistryServer: \"https://corp.example\"\n")
          .build();
        for (String hostToken : new String[]{null, "corp-token"}) {
            HostInputs.HostInputsBuilder host = HostInputs.builder().now(Instant.EPOCH).system(scratch(home))
              .npmrc(new Npmrc(NpmrcScope.Project, npmrc));
            if (hostToken != null) {
                host.credential(new NodeRegistryCredentials("corp.example", hostToken, null, null));
            }

            Inputs inputs = YarnBerryInputs.from(LockDirectory.of(request), host.build());

            assertThat(inputs.getHomeFiles().get(".yarnrc.yml")).isEqualTo(hostToken == null ? "" :
              "npmRegistries:\n  \"//corp.example\":\n    npmAuthToken: \"corp-token\"\n    npmAlwaysAuth: true\n");
            assertThat(asked(inputs)).containsExactly(entry("https://corp.example a",
              hostToken == null ? null : "Bearer corp-token"));
        }
    }

    /**
     * corepack 0.35.0 reads the env file beside the package.json it settles on, above the project too, so a held one
     * there decides whether that package.json's URL runs.
     */
    @Test
    void theDirectoriesAboveTheProjectGiveTheirPaths() {
        String url = "https://repo.yarnpkg.com/4.10.3/packages/yarnpkg-cli/bin/yarn.js";
        Request.RequestBuilder request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\"}")
          .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n  version: 0.0.0-use.local\n" +
                "  resolution: \"p@workspace:.\"\n  languageName: unknown\n  linkType: soft\n")
          .packageJsonPath(Paths.get("app/package.json"))
          .packageManagerFile(Paths.get("package.json"), "{\"name\":\"root\",\"packageManager\":\"yarn@" + url + "\"}");

        Inputs without = YarnBerryInputs.from(LockDirectory.of(request.projectPaths(new HashSet<>(Arrays.asList(
          Paths.get("app/package.json"), Paths.get("package.json")))).build()), HostRegistries.NONE);
        Inputs with = YarnBerryInputs.from(LockDirectory.of(request.projectPaths(new HashSet<>(Arrays.asList(
          Paths.get("app/package.json"), Paths.get("package.json"), Paths.get(".corepack.env")))).build()),
          HostRegistries.NONE);

        assertThat(with.getPaths()).containsExactlyInAnyOrder("package.json", "../package.json", "../.corepack.env");
        assertThat(new YarnBerryLockRegenerator(without, NO_REGISTRY).regenerate().getErrorMessage())
          .contains("Illegal use of URL for known package manager.");
        assertThat(new YarnBerryLockRegenerator(with, NO_REGISTRY).regenerate().getErrorMessage())
          .endsWith("the engine cannot know ../.corepack.env (its contents were not read)");
    }

    /**
     * A registry only the project's .yarnrc.yml names keeps the credentials the host holds for it as an npmRegistries
     * entry, while the registry the package comes from stays the project's choice.
     */
    @Test
    void theHostsCredentialsForARegistryTheProjectNamesAreTheHomeDirectorysToo() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"packageManager\":\"yarn@4.10.3\"," +
                       "\"dependencies\":{\"@corp/a\":\"^1.0.0\"}}")
          .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n  version: 0.0.0-use.local\n" +
                "  resolution: \"p@workspace:.\"\n  languageName: unknown\n  linkType: soft\n")
          .packageJsonPath(Paths.get("package.json"))
          .packageManagerFile(Paths.get(".yarnrc.yml"),
            "npmScopes:\n  corp:\n    npmRegistryServer: \"https://corp.example.com\"\n")
          .build();
        HostInputs host = HostRegistries.NONE.toBuilder()
          .npmrc(new Npmrc(NpmrcScope.Project, singletonMap("@corp:registry", "https://corp.example.com/")))
          .credential(new NodeRegistryCredentials("corp.example.com", "corp-token", null, null))
          .build();

        Inputs inputs = YarnBerryInputs.from(LockDirectory.of(request), host);

        assertThat(inputs.getHomeFiles().get(".yarnrc.yml")).isEqualTo(
          "npmRegistries:\n  \"//corp.example.com\":\n    npmAuthToken: \"corp-token\"\n    npmAlwaysAuth: true\n");
        assertThat(asked(inputs)).containsExactly(entry("https://corp.example.com @corp/a", "Bearer corp-token"));
    }

    // the registry and name of each packument the regeneration asks for, with the Authorization header it sends
    private static Map<String, @Nullable String> asked(Inputs inputs) {
        Map<String, @Nullable String> asked = new LinkedHashMap<>();
        String error = new YarnBerryLockRegenerator(inputs, new RegistrySource() {
            @Override
            public @Nullable String packument(String registry, String name, boolean full,
                                              @Nullable String authorization) {
                asked.put(registry + " " + name, authorization);
                return null;
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version,
                                             @Nullable String authorization) {
                return null;
            }
        }).regenerate().getErrorMessage();
        assertThat(asked).as(String.valueOf(error)).isNotEmpty();
        return asked;
    }

    /**
     * Yarn 4.10.3 reads neither a project's nor the home directory's .npmrc ({@code yarn config get} answers
     * registry.yarnpkg.com beside both): the marker's project npmrc, the repository root's, stays out of the home file.
     */
    @Test
    void theHomeYarnrcIsTheMarkersUserNpmrcNotTheRootsProjectNpmrc(@TempDir Path home) {
        Npmrc root = new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://registry.npmmirror.com/"));
        Npmrc user = new Npmrc(NpmrcScope.User, singletonMap("registry", "https://registry.yarnpkg.com/"));
        Npmrc global = new Npmrc(NpmrcScope.Global, singletonMap("@types:registry", "https://types.example/"));

        assertThat(homeYarnrc(home, root)).isEmpty();
        assertThat(homeYarnrc(home, root, user, global)).isEqualTo("npmRegistryServer: " +
          "\"https://registry.yarnpkg.com/\"\nnpmScopes:\n  \"types\":\n" +
          "    npmRegistryServer: \"https://types.example/\"\n");
    }

    private static String homeYarnrc(Path home, Npmrc... marker) {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("app/package.json"))
          .marker(nodeResolutionResult(PackageManager.YarnBerry).withNpmrcConfigs(Arrays.asList(marker)))
          .build();
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(scratch(home)).npmrc(Arrays.asList(marker))
          .build();
        return YarnBerryInputs.from(LockDirectory.of(request), host).getHomeFiles().get(".yarnrc.yml");
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
}

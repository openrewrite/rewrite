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
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.bun.BunOptions;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class BunInputsTest {

    @Test
    void memberEditPlacesManifestsAndReadsTheRootConfig() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"2.0.0\"}")
          .lock("{}")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/package.json"), "{\"workspaces\":[\"packages/*\"]}")
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .packageManagerFile(Paths.get("repo/bunfig.toml"), "[install]\nlinkWorkspacePackages = false\n")
          .packageManagerFile(Paths.get("repo/.npmrc"), "registry=https://registry.npmmirror.com/\n")
          .packageManagerFile(Paths.get("repo/packages/a/.npmrc"), "registry=https://example.com/\n")
          .build();

        Inputs inputs = BunInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getManifests().get("packages/a")).isEqualTo("{\"name\":\"a\",\"version\":\"2.0.0\"}");
        assertThat(inputs.getBunfig()).isEqualTo("[install]\nlinkWorkspacePackages = false\n");
        assertThat(inputs.getNpmrc()).isEqualTo("registry=https://registry.npmmirror.com/\n");
        assertThat(inputs.getNow()).isEqualTo(Instant.EPOCH);
    }

    // bun 1.3.10 reads a patch file wherever patchedDependencies names it (corpus open-patched-above-lock)
    @Test
    void patchFilesWhereverTheyLieAreTheProjectsFiles() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"patchedDependencies\":{\"ms@2.1.3\":\"patches/ms@2.1.3.patch\"}}")
          .lock("{}")
          .packageJsonPath(Paths.get("repo/package.json"))
          .packageManagerFile(Paths.get("repo/patches/ms@2.1.3.patch"), "--- a/index.js\n+++ b/index.js\n")
          .packageManagerFile(Paths.get("repo/bunfig.toml"), "[install]\n")
          .packageManagerFile(Paths.get("repo/.env"), "NPM_CONFIG_REGISTRY=https://registry.npmmirror.com\n")
          .packageManagerFile(Paths.get("other/patches/x.patch"), "")
          .build();

        Inputs inputs = BunInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getFiles()).containsOnlyKeys("patches/ms@2.1.3.patch", ".env", "../other/patches/x.patch");
        assertThat(inputs.getFiles()).containsEntry("patches/ms@2.1.3.patch", "--- a/index.js\n+++ b/index.js\n");
        assertThat(inputs.getPaths()).isNull();
    }

    @Test
    void thePathsUnderTheRootAreTheProjectsPaths() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("{}")
          .packageJsonPath(Paths.get("repo/package.json"))
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("repo/package.json"), Paths.get("repo/bun.lock"),
            Paths.get("repo/patches/./ms@2.1.3.patch"), Paths.get("other/patches/x.patch"))))
          .build();

        Inputs inputs = BunInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getPaths()).containsExactlyInAnyOrder("package.json", "bun.lock", "patches/ms@2.1.3.patch");
    }

    @Test
    void theHostsRegistriesAreBunsUserNpmrc() {
        Request request = Request.builder().packageJson("{}").lock("{}").build();

        Inputs inputs = BunInputs.from(LockDirectory.of(request),
          HostRegistries.NONE.toBuilder().registry(registry(null, "https://registry.npmmirror.com/")).build());

        assertThat(inputs.getUserNpmrc()).isEqualTo("registry=https://registry.npmmirror.com/\n");
    }

    // bun 1.3.10 under a user .npmrc naming registry.npmjs.org asks it over bunfig.toml's registry and writes ""
    // (lock/bun/open-user-registry-npmjs-bunfig-add); naming only @types, bunfig's (-scope-bunfig-mirror-add)
    @Test
    void aDefaultTheHostNamesIsNamedEvenWhenItIsNpmsOwn(@TempDir Path home) {
        String bunfig = "[install]\nregistry = \"https://registry.yarnpkg.com/\"\n";
        Npmrc npmjs = new Npmrc(NpmrcScope.User, singletonMap("registry", "https://registry.npmjs.org/"));
        Npmrc types = new Npmrc(NpmrcScope.User, singletonMap("@types:registry", "https://registry.npmmirror.com/"));

        assertThat(userRegistry(home, "", bunfig, npmjs)).isEqualTo("https://registry.npmjs.org/");
        assertThat(userRegistry(home, "", bunfig, types)).isEqualTo("https://registry.yarnpkg.com/");
        assertThat(userRegistry(home, "", bunfig)).isEqualTo("https://registry.yarnpkg.com/");
    }

    @Test
    void theViewNamesTheDefaultOnlyWhenItListsOne() {
        NodeRegistry npmjs = registry(null, "https://registry.npmjs.org/");
        NodeRegistry types = registry("@types", "https://registry.npmmirror.com/");

        assertThat(viewed(npmjs, types)).isEqualTo("registry=https://registry.npmjs.org/\n" +
                                                  "@types:registry=https://registry.npmmirror.com/\n");
        assertThat(viewed(types)).isEqualTo("@types:registry=https://registry.npmmirror.com/\n");
    }

    @Test
    void withoutTheNpmrcFileTheMarkersProjectNpmrcStandsIn() {
        Request request = Request.builder()
          .packageJson("{}")
          .lock("{}")
          .marker(nodeResolutionResult(PackageManager.Bun).withNpmrcConfigs(Arrays.asList(
            new Npmrc(NpmrcScope.User, singletonMap("registry", "https://user.example/")),
            new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://project.example/")))))
          .build();

        Inputs inputs = BunInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getNpmrc()).contains("registry=https://project.example/");
        assertThat(inputs.getBunfig()).isNull();
    }

    // bun 1.3.10 run in repo/mid/app under a repo/.npmrc naming a mirror asks npmjs, or the registry its user .npmrc
    // or its own bunfig.toml names: the marker's project npmrc is that root's file, which bun does not read there
    @Test
    void aNestedProjectsUserNpmrcIsTheMarkersUserNpmrcNotTheRootsProjectNpmrc(@TempDir Path home) {
        Npmrc root = new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://registry.npmmirror.com/"));
        Npmrc user = new Npmrc(NpmrcScope.User, singletonMap("registry", "https://registry.yarnpkg.com/"));
        String bunfig = "[install]\nregistry = \"https://registry.yarnpkg.com/\"\n";

        assertThat(userRegistry(home, "mid/app", null, root)).isEqualTo("https://registry.npmjs.org/");
        assertThat(userRegistry(home, "mid/app", null, root, user)).isEqualTo("https://registry.yarnpkg.com/");
        assertThat(userRegistry(home, "mid/app", bunfig, root)).isEqualTo("https://registry.yarnpkg.com/");
    }

    // bun 1.3.10 reads neither NPM_CONFIG_GLOBALCONFIG nor $PREFIX/etc/npmrc naming yarnpkg: it asks npmjs, writes ""
    @Test
    void theMarkersGlobalNpmrcIsNoConfigOfBuns(@TempDir Path home) {
        Npmrc global = new Npmrc(NpmrcScope.Global, singletonMap("registry", "https://registry.yarnpkg.com/"));

        assertThat(userRegistry(home, "", null, global)).isEqualTo("https://registry.npmjs.org/");
        assertThat(userRegistry(home, "mid/app", null, global)).isEqualTo("https://registry.npmjs.org/");
    }

    private static String userRegistry(Path home, String dir, @Nullable String bunfig, Npmrc... marker) {
        Request.RequestBuilder request = Request.builder()
          .packageJson("{\"name\":\"app\"}")
          .lock("{}")
          .packageJsonPath(Paths.get(dir, "package.json"))
          .marker(nodeResolutionResult(PackageManager.Bun).withNpmrcConfigs(Arrays.asList(marker)));
        if (bunfig != null) {
            request.packageManagerFile(Paths.get(dir, "bunfig.toml"), bunfig);
        }
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).npmrc(Arrays.asList(marker))
          .system(HostRegistries.environment(home)).build();
        Inputs inputs = BunInputs.from(LockDirectory.of(request.build()), host);
        return BunOptions.load(inputs.getBunfig(), inputs.getUserNpmrc(), inputs.getNpmrc(), HostEnvironment.UNKNOWN)
          .getRegistry();
    }

    // the view's registries as given, the marker's user npmrc beside them read for nothing
    private static String viewed(NodeRegistry... registries) {
        return HostRegistries.NONE.toBuilder().registries(Arrays.asList(registries))
          .npmrc(new Npmrc(NpmrcScope.User, singletonMap("registry", "https://registry.yarnpkg.com/"))).build()
          .userNpmrc(HostInputs.USER);
    }

    private static NodeRegistry registry(@Nullable String scope, String url) {
        return new NodeRegistry(scope, url, null, null, null, null, false, null, true, false);
    }
}

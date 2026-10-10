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
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.yarnclassic.RegistrySource;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class YarnClassicInputsTest {

    private static final RegistrySource NO_REGISTRY = new RegistrySource() {
        @Override
        public @Nullable String packument(String registry, String name) {
            return null;
        }

        @Override
        public byte @Nullable [] tarball(String url, String name, String version) {
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

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(builder.build()), Host.UNKNOWN);

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
          .packageJson("{\"private\":true,\"workspaces\":[\"packages/*\"]}").build()), Host.UNKNOWN);
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

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN);

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

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN);

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

        Inputs listed = YarnClassicInputs.from(LockDirectory.of(request.build()), Host.UNKNOWN);

        assertThat(listed.getDirectory()).isEqualTo("web/app");
        assertThat(new YarnClassicLockRegenerator(listed, NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: the engine cannot know the lock yarn writes for this edit: yarn installs " +
                     "at the workspace root ../../, whose workspaces list web/app, and writes ../../yarn.lock, not " +
                     "this project's");

        Inputs unlisted = YarnClassicInputs.from(
          LockDirectory.of(request.packageJsonPath(Paths.get("tools/app/package.json")).build()), Host.UNKNOWN);

        assertThat(unlisted.getDirectory()).isEqualTo("tools/app");
        assertThat(new YarnClassicLockRegenerator(unlisted, NO_REGISTRY).regenerate().isSuccess())
          .isTrue();
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

        Inputs inputs = YarnClassicInputs.from(LockDirectory.of(request), Host.UNKNOWN);

        assertThat(inputs.getConfigFiles()).containsOnlyKeys("../package.json", "../../../package.json");
        assertThat(new YarnClassicLockRegenerator(inputs, NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: yarn rejects this edit: This project's package.json defines " +
                     "\"packageManager\": \"yarn@pnpm@9.15.0\". However the current global version of Yarn is " +
                     "1.22.22.");
    }
}

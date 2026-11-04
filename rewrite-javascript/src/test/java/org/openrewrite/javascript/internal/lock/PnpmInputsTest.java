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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmLockRegenerator.Inputs;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class PnpmInputsTest {

    @Test
    void memberEditPlacesManifestsAndTheFilesUnderTheLocksDirectory() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"dependencies\":{\"ms\":\"2.1.3\"}}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/package.json"), "{\"name\":\"root\",\"private\":true}")
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\"}")
          .packageManagerFile(Paths.get("repo/pnpm-workspace.yaml"), "packages:\n  - packages/*\n")
          .packageManagerFile(Paths.get("repo/.npmrc"), "registry=https://example.com/\n")
          .packageManagerFile(Paths.get("repo/patches/ms@2.1.3.patch"), "diff\n")
          .packageManagerFile(Paths.get(".npmrc"), "registry=https://above.example/\n")
          .packageManagerFile(Paths.get("package.json"), "{\"name\":\"above\"}")
          .build();

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getLock()).isEqualTo("lockfileVersion: '9.0'\n");
        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getManifests().get("packages/a"))
          .isEqualTo("{\"name\":\"a\",\"dependencies\":{\"ms\":\"2.1.3\"}}");
        assertThat(inputs.getFiles()).containsOnlyKeys("pnpm-workspace.yaml", ".npmrc", "patches/ms@2.1.3.patch");
        assertThat(inputs.getFiles().get(".npmrc")).isEqualTo("registry=https://example.com/\n");
        assertThat(inputs.getPaths()).isNull();
        assertThat(inputs.getEnv()).isNull();
        assertThat(inputs.getNow()).isEqualTo(Instant.EPOCH);
    }

    /**
     * pnpm finds its workspace from the lock's directory up; a member's own lock is regenerated with the workspace's
     * manifests, pnpm-workspace.yaml and .npmrc, at the member's place in it.
     */
    @Test
    void aMembersOwnLockIsPlacedInTheWorkspaceAboveIt() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"b\",\"dependencies\":{\"ms\":\"2.1.3\"}}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get("repo/packages/b/package.json"))
          .manifest(Paths.get("repo/packages/b/package.json"), "{\"name\":\"b\"}")
          .packageManagerFile(Paths.get("repo/pnpm-workspace.yaml"), "packages:\n  - packages/*\n")
          .packageManagerFile(Paths.get("repo/.npmrc"), "registry=https://example.com/\n")
          .packageManagerFile(Paths.get("repo/package.json"), "{\"name\":\"root\",\"private\":true}")
          .packageManagerFile(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\"}")
          .packageManagerFile(Paths.get("package.json"), "{\"name\":\"above\"}")
          .marker(nodeResolutionResult(PackageManager.Pnpm).withNpmrcConfigs(Collections.singletonList(
            new Npmrc(NpmrcScope.Project, Collections.singletonMap("registry", "https://member.example/")))))
          .build();

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getLockDir()).isEqualTo("packages/b");
        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a", "packages/b");
        assertThat(inputs.getManifests().get("packages/b"))
          .isEqualTo("{\"name\":\"b\",\"dependencies\":{\"ms\":\"2.1.3\"}}");
        assertThat(inputs.getFiles()).containsOnlyKeys("pnpm-workspace.yaml", ".npmrc");
        assertThat(inputs.getFiles().get(".npmrc")).isEqualTo("registry=https://example.com/\n");
    }

    @Test
    void withoutAWorkspaceAboveTheLockIsTheRoot() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"b\"}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get("repo/packages/b/package.json"))
          .packageManagerFile(Paths.get("repo/package.json"), "{\"name\":\"root\",\"workspaces\":[\"packages/*\"]}")
          .packageManagerFile(Paths.get("repo/.npmrc"), "registry=https://example.com/\n")
          .build();

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getLockDir()).isEmpty();
        assertThat(inputs.getManifests()).containsOnlyKeys("");
        assertThat(inputs.getFiles()).isEmpty();
    }

    @Test
    void thePathsUnderTheRootAreTheProjectsPaths() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get("repo/package.json"))
          .projectPaths(new LinkedHashSet<>(Arrays.asList(Paths.get("repo/package.json"),
            Paths.get("repo/pnpm-lock.yaml"), Paths.get("repo/local/package.json"), Paths.get("other/package.json"))))
          .build();

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getPaths()).containsExactlyInAnyOrder("package.json", "pnpm-lock.yaml",
          "local/package.json");
        assertThat(inputs.getFiles()).isEmpty();
    }

    @Test
    void withoutTheNpmrcFileTheMarkersProjectNpmrcStandsIn() {
        Request request = Request.builder()
          .packageJson("{}")
          .lock("lockfileVersion: '9.0'\n")
          .marker(nodeResolutionResult(PackageManager.Pnpm).withNpmrcConfigs(Arrays.asList(
            new Npmrc(NpmrcScope.User, Collections.singletonMap("registry", "https://user.example/")),
            new Npmrc(NpmrcScope.Project, Collections.singletonMap("registry", "https://project.example/")))))
          .build();

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getFiles().get(".npmrc")).contains("registry=https://project.example/");
    }

    /**
     * The marker's .npmrc is read from the repository root, so it stands in only where pnpm reads the .npmrc of
     * that directory: a workspace or project there, for the walk from a member as for the root's lock.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "packages/a/package.json      | pnpm-workspace.yaml      | true",
      "package.json                 | pnpm-workspace.yaml      | true",
      "repo/packages/a/package.json | repo/pnpm-workspace.yaml | false",
      "repo/package.json            | none                     | false"
    })
    void theMarkersNpmrcStandsInForTheRepositoryRootsOnly(String packageJsonPath, String workspaceManifest,
                                                         boolean standsIn) {
        Request.RequestBuilder request = Request.builder()
          .packageJson("{\"name\":\"a\"}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get(packageJsonPath))
          .marker(nodeResolutionResult(PackageManager.Pnpm).withNpmrcConfigs(Collections.singletonList(
            new Npmrc(NpmrcScope.Project, Collections.singletonMap("registry", "https://project.example/")))));
        if (!"none".equals(workspaceManifest)) {
            request.packageManagerFile(Paths.get(workspaceManifest), "packages:\n  - packages/*\n");
        }

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request.build()), HostRegistries.NONE);

        assertThat(inputs.getFiles().containsKey(".npmrc")).isEqualTo(standsIn);
    }

    /**
     * A member's edit regenerates the workspace root's lock with pnpm run in the member's directory.
     */
    @Test
    void pnpmRunsInTheEditedManifestsDirectory() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"dependencies\":{\"ms\":\"2.1.3\"}}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/package.json"), "{\"name\":\"root\",\"private\":true}")
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\"}")
          .packageManagerFile(Paths.get("repo/pnpm-workspace.yaml"), "packages:\n  - packages/*\n")
          .build();

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request), HostRegistries.NONE);

        assertThat(inputs.getLockDir()).isEmpty();
        assertThat(inputs.getDir()).isEqualTo("packages/a");
    }

    /**
     * pnpm stops at the nearest workspace manifest by any of the names it looks for; a misspelt one below a
     * pnpm-workspace.yaml stays under the root, where the regeneration finds it.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "pnpm-workspace.yml                 | | packages/a",
      "packages/a/.pnpm-workspace.yaml    | pnpm-workspace.yaml | packages/a",
      "packages/a/pnpm-workspaces.yml     | | ''"
    })
    void aMisspeltWorkspaceManifestIsWherePnpmStops(String misspelt, String workspaceManifest, String lockDir) {
        Request.RequestBuilder request = Request.builder()
          .packageJson("{\"name\":\"a\"}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get("packages/a/package.json"))
          .projectPaths(new LinkedHashSet<>(Arrays.asList(Paths.get("packages/a/package.json"),
            Paths.get(misspelt))));
        if (workspaceManifest != null) {
            request.packageManagerFile(Paths.get(workspaceManifest), "packages:\n  - packages/*\n");
        }

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request.build()), HostRegistries.NONE);

        assertThat(inputs.getLockDir()).isEqualTo(lockDir);
        assertThat(inputs.getPaths()).contains(misspelt.startsWith("packages/a/") && lockDir.isEmpty() ?
          misspelt.substring("packages/a/".length()) : misspelt);
    }

    /**
     * pnpm 11.2.2 fails on a user .npmrc naming a closed port, and installs from npmjs when the same file is
     * NPM_CONFIG_GLOBALCONFIG or $PREFIX/etc/npmrc: the host's user npmrc is its user layer, its global one is not.
     */
    @Test
    void theHostsUserNpmrcIsPnpmsUserNpmrc(@TempDir Path home) {
        Map<String, String> user = new LinkedHashMap<>();
        user.put("registry", "https://registry.npmmirror.com/");
        user.put("@types:registry", "https://types.example/");
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get("app/package.json"))
          .build();
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(HostRegistries.environment(home))
          .npmrc(new Npmrc(NpmrcScope.Global, Collections.singletonMap("registry", "https://registry.yarnpkg.com/")))
          .npmrc(new Npmrc(NpmrcScope.User, user))
          .npmrc(new Npmrc(NpmrcScope.Project, Collections.singletonMap("registry", "https://root.example/")))
          .build();

        Inputs inputs = PnpmInputs.from(LockDirectory.of(request), host);

        assertThat(inputs.getUserNpmrc()).isEqualTo("registry=https://registry.npmmirror.com/\n" +
                                                   "@types:registry=https://types.example/\n");
        assertThat(inputs.getFiles()).doesNotContainKey(".npmrc");
    }
}

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
import org.openrewrite.javascript.internal.lock.npm.Host;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.pnpm.RegistryMetadata;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class PnpmInputsTest {

    private static final RegistryMetadata NO_REGISTRY = new RegistryMetadata() {
        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return null;
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return null;
        }

        @Override
        public @Nullable String document(String url) {
            return null;
        }
    };

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

        Inputs inputs = PnpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH, NO_REGISTRY);

        assertThat(inputs.getLock()).isEqualTo("lockfileVersion: '9.0'\n");
        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getManifests().get("packages/a"))
          .isEqualTo("{\"name\":\"a\",\"dependencies\":{\"ms\":\"2.1.3\"}}");
        assertThat(inputs.getFiles()).containsOnlyKeys("pnpm-workspace.yaml", ".npmrc", "patches/ms@2.1.3.patch");
        assertThat(inputs.getFiles().get(".npmrc")).isEqualTo("registry=https://example.com/\n");
        assertThat(inputs.getPaths()).isNull();
        assertThat(inputs.getEnv()).isNull();
        assertThat(inputs.getNow()).isEqualTo(Instant.EPOCH);
        assertThat(inputs.getRegistry()).isSameAs(NO_REGISTRY);
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

        Inputs inputs = PnpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH, NO_REGISTRY);

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

        Inputs inputs = PnpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH, NO_REGISTRY);

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

        Inputs inputs = PnpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH, NO_REGISTRY);

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

        Inputs inputs = PnpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH, NO_REGISTRY);

        assertThat(inputs.getFiles().get(".npmrc")).contains("registry=https://project.example/");
    }
}

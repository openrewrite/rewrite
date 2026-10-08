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
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Test
    void thePathsUnderTheRootAreTheProjectsPaths() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("lockfileVersion: '9.0'\n")
          .packageJsonPath(Paths.get("repo/package.json"))
          .projectPaths(Set.of(Paths.get("repo/package.json"), Paths.get("repo/pnpm-lock.yaml"),
            Paths.get("repo/local/package.json"), Paths.get("other/package.json")))
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
          .marker(nodeResolutionResult(PackageManager.Pnpm).withNpmrcConfigs(List.of(
            new Npmrc(NpmrcScope.User, Map.of("registry", "https://user.example/")),
            new Npmrc(NpmrcScope.Project, Map.of("registry", "https://project.example/")))))
          .build();

        Inputs inputs = PnpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH, NO_REGISTRY);

        assertThat(inputs.getFiles().get(".npmrc")).contains("registry=https://project.example/");
    }
}

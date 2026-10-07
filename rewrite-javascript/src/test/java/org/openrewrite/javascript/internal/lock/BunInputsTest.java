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
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.npm.Host;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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

        Inputs inputs = BunInputs.from(request, Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getManifests().get("packages/a")).isEqualTo("{\"name\":\"a\",\"version\":\"2.0.0\"}");
        assertThat(inputs.getBunfig()).isEqualTo("[install]\nlinkWorkspacePackages = false\n");
        assertThat(inputs.getNpmrc()).isEqualTo("registry=https://registry.npmmirror.com/\n");
        assertThat(inputs.getNow()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void withoutTheNpmrcFileTheMarkersProjectNpmrcStandsIn() {
        Request request = Request.builder()
          .packageJson("{}")
          .lock("{}")
          .marker(nodeResolutionResult(PackageManager.Bun).withNpmrcConfigs(List.of(
            new Npmrc(NpmrcScope.User, Map.of("registry", "https://user.example/")),
            new Npmrc(NpmrcScope.Project, Map.of("registry", "https://project.example/")))))
          .build();

        Inputs inputs = BunInputs.from(request, Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getNpmrc()).contains("registry=https://project.example/");
        assertThat(inputs.getBunfig()).isNull();
    }
}

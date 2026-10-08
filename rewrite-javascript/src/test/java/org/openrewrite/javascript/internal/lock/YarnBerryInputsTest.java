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
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.npm.Host;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class YarnBerryInputsTest {

    @Test
    void memberEditPlacesManifestsAndReadsTheRcFilesUpToTheRepository() {
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
          .packageManagerFile(Paths.get("repo/app/.npmrc"), "registry=https://example.com/\n")
          .build();

        Inputs inputs = YarnBerryInputs.from(request, Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getLock()).isEqualTo("__metadata:\n  version: 8\n");
        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getManifests().get("packages/a")).isEqualTo("{\"name\":\"a\",\"version\":\"2.0.0\"}");
        assertThat(inputs.getConfigFiles()).containsOnlyKeys(".yarnrc.yml", "../.yarnrc.yml");
        assertThat(inputs.getConfigFiles().get(".yarnrc.yml")).isEqualTo("compressionLevel: 0\n");
        assertThat(inputs.getPaths()).isNull();
        assertThat(inputs.getEnv()).isEmpty();
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

        Inputs inputs = YarnBerryInputs.from(request, Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getPaths()).containsExactlyInAnyOrder("package.json", ".yarnrc.yml",
          "packages/a/package.json");
        assertThat(inputs.getConfigFiles()).isEmpty();
    }
}

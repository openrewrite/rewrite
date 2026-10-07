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
import org.openrewrite.javascript.internal.lock.npm.Host;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class YarnClassicInputsTest {

    @Test
    void memberEditPlacesManifestsAndReadsTheRootConfig() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"2.0.0\"}")
          .lock("# yarn lockfile v1\n")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/package.json"), "{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .manifest(Paths.get("repo/local/package.json"), "{\"name\":\"local\",\"version\":\"1.0.0\"}")
          .packageManagerFile(Paths.get("repo/.yarnrc"), "registry \"https://registry.npmjs.org\"\n")
          .packageManagerFile(Paths.get("repo/.npmrc"), "registry=https://registry.npmmirror.com/\n")
          .packageManagerFile(Paths.get("repo/packages/a/.yarnrc"), "registry \"https://example.com\"\n")
          .build();

        Inputs inputs = YarnClassicInputs.from(request, Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getLock()).isEqualTo("# yarn lockfile v1\n");
        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a", "local");
        assertThat(inputs.getManifests().get("packages/a")).isEqualTo("{\"name\":\"a\",\"version\":\"2.0.0\"}");
        assertThat(inputs.getYarnrc()).isEqualTo("registry \"https://registry.npmjs.org\"\n");
        assertThat(inputs.getNpmrc()).isEqualTo("registry=https://registry.npmmirror.com/\n");
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
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("repo/package.json"), Paths.get("repo/.yarnrc"),
            Paths.get("other/.npmrc"))))
          .build();

        Inputs inputs = YarnClassicInputs.from(request, Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getPaths()).containsExactlyInAnyOrder("package.json", ".yarnrc");
        assertThat(inputs.getYarnrc()).isNull();
        assertThat(new YarnClassicLockRegenerator(inputs, (registry, name) -> null).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: the engine cannot know .yarnrc (its contents were not read)");
    }
}

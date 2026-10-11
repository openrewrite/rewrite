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
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;
import java.util.Collections;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class LockDirectoryTest {

    private static Request.RequestBuilder inMidApp(PackageManager pm) {
        return Request.builder()
          .packageJson("{\"name\":\"app\"}")
          .lock("{}")
          .packageJsonPath(Paths.get("mid/app/package.json"))
          .marker(nodeResolutionResult(pm).withNpmrcConfigs(Collections.singletonList(
            new Npmrc(NpmrcScope.Project, singletonMap("registry", "https://registry.npmmirror.com/")))));
    }

    @Test
    void placesFilesByPathFromTheLocksDirectoryAndRunsInTheEditedManifests() {
        LockDirectory dir = LockDirectory.of(Request.builder()
          .packageJson("{\"name\":\"a\"}")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/package.json"), "{}")
          .packageManagerFile(Paths.get("repo/.npmrc"), "x=y")
          .packageManagerFile(Paths.get(".yarnrc"), "--offline true")
          .projectPaths(Collections.singleton(Paths.get("repo/packages/a/index.js")))
          .build());

        assertThat(dir.getPath()).isEqualTo("repo");
        assertThat(dir.name()).isEqualTo("repo");
        assertThat(dir.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(dir.getFiles()).containsOnlyKeys(".npmrc", "../.yarnrc");
        assertThat(dir.getPaths()).containsExactly("packages/a/index.js");
        assertThat(dir.getRunsIn()).isEqualTo("packages/a");
        assertThat(dir.filesUnder("..")).containsOnlyKeys("repo/.npmrc", ".yarnrc");
        assertThat(dir.holds("packages/a/index.js")).isTrue();
        assertThat(dir.holds("../.yarnrc")).isTrue();
        assertThat(dir.holds("packages/a/yarn.lock")).isFalse();
        assertThat(dir.aboveRunsIn(0)).isEqualTo("packages/a");
        assertThat(dir.aboveRunsIn(2)).isEqualTo("");
        assertThat(dir.aboveRunsIn(3)).isEqualTo("..");
    }

    /**
     * The marker read its project npmrc from the repository's root: it stands in for that file only.
     */
    @Test
    void theMarkersNpmrcIsTheRepositoryRootsDotNpmrc() {
        LockDirectory dir = LockDirectory.of(inMidApp(PackageManager.Bun).build());
        assertThat(dir.getFiles()).containsOnlyKeys("../../.npmrc");

        assertThat(NpmInputs.from(dir, HostRegistries.NONE).getNpmrc()).isNull();
        assertThat(BunInputs.from(dir, HostRegistries.NONE).getNpmrc()).isNull();
        assertThat(YarnClassicInputs.from(dir, HostRegistries.NONE).getConfigFiles())
          .containsOnlyKeys("../../.npmrc");

        LockDirectory held = LockDirectory.of(inMidApp(PackageManager.Npm)
          .packageManagerFile(Paths.get(".npmrc"), "registry=https://example.com/\n")
          .packageManagerFile(Paths.get("mid/app/.npmrc"), "registry=https://own.example/\n")
          .build());
        assertThat(held.file("../../.npmrc")).isEqualTo("registry=https://example.com/\n");
        assertThat(NpmInputs.from(held, HostRegistries.NONE).getNpmrc())
          .isEqualTo("registry=https://own.example/\n");
    }
}

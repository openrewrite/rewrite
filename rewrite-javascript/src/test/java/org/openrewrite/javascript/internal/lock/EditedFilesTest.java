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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The package-manager files an edit changes or creates are what every port's inputs read, in place of the scan's.
 */
class EditedFilesTest {

    private static final Path MEMBER = Paths.get("packages/a/package.json");

    private static final Request REQUEST = Request.builder()
      .packageJson("{\"name\":\"root\",\"private\":true}")
      .lock("lock")
      .packageJsonPath(Paths.get("package.json"))
      .manifest(Paths.get("package.json"), "{\"name\":\"root\"}")
      .manifest(MEMBER, "{\"name\":\"a\"}")
      .packageManagerFile(Paths.get("pnpm-workspace.yaml"), "packages:\n  - packages/*\n")
      .packageManagerFile(Paths.get(".yarnrc.yml"), "nodeLinker: pnp\n")
      .packageManagerFile(Paths.get(".npmrc"), "registry=https://scanned.example/\n")
      .editedFile(Paths.get("pnpm-workspace.yaml"), "packages:\n  - packages/*\ncatalog:\n  ms: 2.1.3\n")
      .editedFile(Paths.get(".yarnrc.yml"), "nodeLinker: node-modules\n")
      .editedFile(Paths.get(".npmrc"), "registry=https://edited.example/\n")
      .editedFile(Paths.get("bunfig.toml"), "[install]\n")
      .editedFile(Paths.get(".yarnrc"), "--install.frozen-lockfile false\n")
      .editedFile(MEMBER, "{\"name\":\"a\",\"dependencies\":{\"ms\":\"catalog:\"}}")
      .editedFile(Paths.get("package.json"), "{\"ignored\":true}")
      .projectPaths(new LinkedHashSet<>(Arrays.asList(Paths.get("package.json"), MEMBER, Paths.get("pnpm-lock.yaml"))))
      .build();

    @Test
    void theRequestHoldsTheEditedFilesInPlaceOfTheScannedOnes() {
        assertThat(REQUEST.getManifests()).containsOnlyKeys(Paths.get("package.json"), MEMBER);
        assertThat(REQUEST.getManifests().get(MEMBER)).contains("catalog:");
        assertThat(REQUEST.getManifests().get(Paths.get("package.json"))).isEqualTo("{\"name\":\"root\"}");
        assertThat(REQUEST.getPackageManagerFiles().get(Paths.get(".npmrc")))
          .isEqualTo("registry=https://edited.example/\n");
        assertThat(REQUEST.getPackageManagerFiles()).containsKeys(Paths.get("bunfig.toml"), Paths.get(".yarnrc"));
        assertThat(REQUEST.getProjectPaths()).contains(Paths.get("bunfig.toml"), Paths.get(".yarnrc.yml"));
        assertThat(LockDirectory.of(REQUEST).getManifests().get("")).isEqualTo("{\"name\":\"root\",\"private\":true}");
    }

    @Test
    void everyPortReadsTheEditedVersion() {
        LockDirectory dir = LockDirectory.of(REQUEST);
        assertThat(PnpmInputs.from(dir, HostRegistries.NONE).getFiles().get("pnpm-workspace.yaml"))
          .endsWith("catalog:\n  ms: 2.1.3\n");
        assertThat(YarnBerryInputs.from(dir, HostRegistries.NONE).getConfigFiles()
          .get(".yarnrc.yml")).isEqualTo("nodeLinker: node-modules\n");
        assertThat(YarnClassicInputs.from(dir, HostRegistries.NONE).getConfigFiles().get(".yarnrc"))
          .isEqualTo("--install.frozen-lockfile false\n");
        assertThat(NpmInputs.from(dir, HostRegistries.NONE).getNpmrc())
          .isEqualTo("registry=https://edited.example/\n");
        assertThat(NpmInputs.from(dir, HostRegistries.NONE).getManifests().get("packages/a"))
          .contains("catalog:");
        assertThat(BunInputs.from(dir, HostRegistries.NONE).getBunfig()).isEqualTo("[install]\n");
    }
}

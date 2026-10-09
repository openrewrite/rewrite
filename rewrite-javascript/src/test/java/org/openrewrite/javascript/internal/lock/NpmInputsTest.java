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
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator.Inputs;

import java.nio.file.Paths;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class NpmInputsTest {

    @Test
    void anEditOfTheLocksOwnManifestWalksThePackageJsonAboveItNearestFirst() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\"}")
          .lock("{}")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\"}")
          .packageManagerFile(Paths.get("package.json"), "{\"name\":\"top\"}")
          .packageManagerFile(Paths.get("repo/package.json"), "{\"name\":\"repo\"}")
          .packageManagerFile(Paths.get("repo/.npmrc"), "x=y")
          .packageManagerFile(Paths.get("repo/other/package.json"), "{\"name\":\"other\"}")
          .build();

        Inputs inputs = NpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getParentManifests()).containsExactly(
          entry("packages/a", "{\"name\":\"repo\"}"), entry("repo/packages/a", "{\"name\":\"top\"}"));
    }

    /**
     * npm run in a member stops at the first directory above whose workspaces list it, the root the host sent the
     * edit to.
     */
    @Test
    void aMemberEditWalksNothingAboveTheRootItWasSentTo() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\"}")
          .lock("{}")
          .packageJsonPath(Paths.get("repo/packages/a/package.json"))
          .manifest(Paths.get("repo/package.json"), "{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .manifest(Paths.get("repo/packages/a/package.json"), "{\"name\":\"a\"}")
          .packageManagerFile(Paths.get("package.json"), "{\"workspaces\":[\"repo\"]}")
          .build();

        Inputs inputs = NpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getParentManifests()).isEmpty();
    }
}

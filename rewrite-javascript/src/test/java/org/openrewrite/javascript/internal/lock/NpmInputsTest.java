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
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.npm.PackumentSource;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class NpmInputsTest {

    private static final PackumentSource NO_REGISTRY = new PackumentSource() {
        @Override
        public Map<String, Object> packument(String name, boolean fullMetadata) {
            throw new AssertionError("no packument is needed: " + name);
        }

        @Override
        public Map<String, Object> manifest(String name, String version) {
            throw new AssertionError("no manifest is needed: " + name);
        }
    };

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

        Inputs inputs = NpmInputs.from(LockDirectory.of(request), Host.UNKNOWN, Instant.EPOCH);

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

        Inputs inputs = NpmInputs.from(LockDirectory.of(request), Host.UNKNOWN, Instant.EPOCH);

        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getParentManifests()).isEmpty();
    }

    /**
     * npm reads the .npmrc of the directory it installs in: one the host holds without its contents is unknown, as
     * for the other package managers.
     */
    @Test
    void anNpmrcTheHostHoldsButDidNotReadIsCannotKnow() {
        Request.RequestBuilder request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .lock("{\"name\":\"a\",\"version\":\"1.0.0\",\"lockfileVersion\":3,\"requires\":true,\"packages\":" +
                "{\"\":{\"name\":\"a\",\"version\":\"1.0.0\"}}}\n")
          .packageJsonPath(Paths.get("app/package.json"))
          .manifest(Paths.get("app/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/.npmrc"))));

        Inputs unread = NpmInputs.from(LockDirectory.of(request.build()), Host.UNKNOWN, Instant.EPOCH);
        Result result = new NpmLockRegenerator(unread, NO_REGISTRY).regenerate();

        assertThat(unread.getPaths()).contains(".npmrc");
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).isEqualTo("the engine cannot know .npmrc (its contents were not " +
                                                              "read)");

        Inputs read = NpmInputs.from(LockDirectory.of(request.packageManagerFile(Paths.get("app/.npmrc"),
          "legacy-peer-deps=true\n").build()), Host.UNKNOWN, Instant.EPOCH);
        assertThat(new NpmLockRegenerator(read, NO_REGISTRY).regenerate().isSuccess()).isTrue();
    }
}

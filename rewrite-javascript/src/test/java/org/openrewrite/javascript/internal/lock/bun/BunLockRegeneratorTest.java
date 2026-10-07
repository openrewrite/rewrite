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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BunLockRegeneratorTest {

    private static final String SCENARIO = "/lock/bun/lock-without-config-version-noop/";

    /**
     * Which bun.lock bun 1.3.10 writes when run in packages/a under these package.json files: the root's at the top
     * and, when given, that of packages/ in between. A broken one stops bun with its ParserError.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "{\"private\":true,\"workspaces\":[\"packages/*\"]}||../..",
      "{\"private\":true,\"workspaces\":{\"packages\":[\"packages/*\"]}}||../..",
      "{\"private\":true,\"workspaces\":[\"packages/a\"]}||../..",
      "{\"private\":true,\"workspaces\":[\"packages/*\"]}|{\"name\":\"mid\"}|../..",
      "{\"private\":true,\"workspaces\":[\"packages/*\"]}|{\"name\":\"mid\",\"workspaces\":[\"a\"]}|..",
      "{\"private\":true,\"workspaces\":[\"packages/*\"]}|{\"name\":\"mid\",\"workspaces\":[\"x/*\"]}|",
      "{\"private\":true,\"workspaces\":[\"libs/*\"]}||",
      "{\"private\":true,\"workspaces\":\"packages/*\"}||",
      "{\"private\":true,\"workspaces\":[\"packages/*\",\"!packages/a\"]}||",
      "{\"workspaces\":[\"packages/*\"]||ParserError"
    })
    void bunInstallsAtTheFirstDirectoryAboveWithWorkspacesWhenTheyListTheProject(String root, String mid,
                                                                                 String installsAt) {
        BunLockRegenerator.Inputs.InputsBuilder inputs = BunLockRegenerator.Inputs.builder()
          .lock(BunTestData.resource(SCENARIO + "before"))
          .manifest("", BunTestData.resource(SCENARIO + "pkg"))
          .now(Instant.EPOCH);
        if (mid != null) {
            inputs.parentManifest("a", mid);
        }
        inputs.parentManifest("packages/a", root);

        Result result = new BunLockRegenerator(inputs.build(), (registry, name, extended) -> {
            throw new AssertionError("no packument is needed: " + name);
        }).regenerate();

        if (installsAt == null) {
            assertThat(result.getLockFileContent()).isEqualTo(BunTestData.resource(SCENARIO + "after"));
        } else if ("ParserError".equals(installsAt)) {
            assertThat(result.getErrorMessage()).startsWith("MALFORMED_MANIFEST: bun rejects this edit: ParserError");
        } else {
            assertThat(result.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine cannot know the lock bun " +
              "writes: it installs at the workspace root " + installsAt + ", whose workspaces list this project, and " +
              "writes that root's bun.lock");
        }
    }
}

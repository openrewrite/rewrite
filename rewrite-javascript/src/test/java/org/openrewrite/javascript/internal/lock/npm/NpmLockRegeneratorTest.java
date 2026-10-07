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
package org.openrewrite.javascript.internal.lock.npm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NpmLockRegeneratorTest {

    private static final String MANIFEST = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\"\n}\n";
    private static final String LOCK = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": 3,\n" +
                                       "  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": \"a\",\n" +
                                       "      \"version\": \"1.0.0\"\n    }\n  }\n}\n";

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

    /**
     * {@code npm prefix} (npm 11.17.0) run in packages/a under these package.json files: the root's at the top and,
     * when given, that of packages/ in between. npm writes the lock of the directory it prints.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "{\"private\":true,\"workspaces\":[\"packages/*\"]}||../..",
      "{\"private\":true,\"workspaces\":{\"packages\":[\"packages/*\"]}}||../..",
      "{\"workspaces\":[\"packages/*\"]}||../..",
      "{\"private\":true,\"workspaces\":[\"packages/*\"]}|{\"name\":\"mid\"}|../..",
      "{\"private\":true,\"workspaces\":[\"packages/*\"]}|{\"name\":\"mid\",\"workspaces\":[\"x/*\"]}|../..",
      "{\"private\":true,\"workspaces\":[\"packages/*\"]}|{\"name\":\"mid\",\"workspaces\":[\"a\"]}|..",
      "{\"private\":true,\"workspaces\":[\"libs/*\"]}||",
      "{\"private\":true,\"workspaces\":[\"packages/*\",\"!packages/a\"]}||",
      "{\"workspaces\":[\"packages/*\"]||"
    })
    void npmInstallsAtTheNearestDirectoryAboveWhoseWorkspacesListTheProject(String root, String mid, String installsAt) {
        NpmLockRegenerator.Inputs.InputsBuilder inputs = NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", MANIFEST)
          .now(Instant.EPOCH);
        if (mid != null) {
            inputs.parentManifest("a", mid);
        }
        inputs.parentManifest("packages/a", root);

        Result result = new NpmLockRegenerator(inputs.build(), NO_REGISTRY).regenerate();

        if (installsAt == null) {
            assertThat(result.getLockFileContent()).isEqualTo(LOCK);
        } else {
            assertThat(result.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine cannot know the lock npm " +
              "writes: it installs at the workspace root " + installsAt + ", whose workspaces list this project, " +
              "and writes that root's lock");
        }
    }
}

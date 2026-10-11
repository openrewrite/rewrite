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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

class NpmLockRegeneratorTest {

    private static final String MANIFEST = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\"\n}\n";
    private static final String LOCK = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": " +
                                       "3,\n  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": " +
                                       "\"a\",\n      \"version\": \"1.0.0\"\n    }\n  }\n}\n";

    private static final PackumentSource NO_REGISTRY = new PackumentSource() {
        @Override
        public Map<String, Object> packument(String registry, String name, boolean fullMetadata) {
            throw new AssertionError("no packument is needed: " + name);
        }

        @Override
        public Map<String, Object> manifest(String registry, String name, String version) {
            throw new AssertionError("no manifest is needed: " + name);
        }
    };

    /**
     * {@code npm prefix} (npm 11.17.0) run in packages/a under the root's package.json and that of packages/ when
     * given ({@code npm-arborist/generate/npm-prefix-cases.js}); npm writes the lock of the directory it prints.
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
    void npmInstallsAtTheNearestDirectoryAboveWhoseWorkspacesListTheProject(String root, String mid,
            String installsAt) {
        NpmLockRegenerator.Inputs.InputsBuilder inputs = NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", MANIFEST)
          .now(Instant.EPOCH);
        Map<String, String> rootTree = new LinkedHashMap<>();
        rootTree.put("", root);
        if (mid != null) {
            inputs.parentTree("a", singletonMap("", mid));
            rootTree.put("packages", mid);
        }
        rootTree.put("packages/a", MANIFEST);
        inputs.parentTree("packages/a", rootTree);

        Result result = new NpmLockRegenerator(inputs.build(), NO_REGISTRY).regenerate();

        if (installsAt == null) {
            assertThat(result.getLockFileContent()).isEqualTo(LOCK);
        } else {
            assertThat(result.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine cannot know the lock npm " +
              "writes: it installs at the workspace root " + installsAt + ", whose workspaces list this project, " +
              "and writes that root's lock");
        }
    }

    // npm 11.17.0 rejects the first edit on darwin-arm64 (EBADPLATFORM) and writes its lock on linux-x64. It rejects
    // the second under engine-strict on node 18 (EBADENGINE) and writes its lock on node 24. The recipes leave the
    // host unknown.
    @Test
    void anEditWhoseOutcomeFollowsTheUnknownHostCannotBeKnown() {
        Result platform = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                        "    \"linux-only\": \"1.0.0\"\n  }\n}\n")
          .now(Instant.EPOCH)
          .build(), oneVersion("linux-only", "\"os\": [\"linux\"], \"cpu\": [\"x64\"]")).regenerate();
        Result engine = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                        "    \"node-20-only\": \"1.0.0\"\n  }\n}\n")
          .npmrc("engine-strict=true\n")
          .now(Instant.EPOCH)
          .build(), oneVersion("node-20-only", "\"engines\": {\"node\": \">=20.18.1\"}")).regenerate();

        assertThat(Arrays.asList(kind(platform), kind(engine))).containsExactly(Kind.CANNOT_KNOW, Kind.CANNOT_KNOW);
    }

    // npm locks an optional package the same on a host it skips there (lock/npm/arb-platform-optional)
    @Test
    void anOptionalPackageForSomeHostsRegeneratesOnAnUnknownHost() {
        Result result = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"optionalDependencies\": {\n" +
                        "    \"linux-only\": \"1.0.0\"\n  }\n}\n")
          .now(Instant.EPOCH)
          .build(), oneVersion("linux-only", "\"os\": [\"linux\"], \"engines\": {\"node\": \">=99\"}")).regenerate();

        assertThat(result.getLockFileContent()).contains("\"node_modules/linux-only\": {\n      \"version\": " +
                                                         "\"1.0.0\"").contains("\"optional\": true");
    }

    private static @Nullable Kind kind(Result result) {
        return result.getFailure() == null ? null : result.getFailure().getKind();
    }

    private static PackumentSource oneVersion(String name, String fields) {
        String manifest = "{\"name\": \"" + name + "\", \"version\": \"1.0.0\", " + fields + ", \"dist\": {" +
                          "\"tarball\": \"https://registry.npmjs.org/" + name + "/-/" + name + "-1.0.0.tgz\", " +
                          "\"integrity\": \"sha512-vbutsFqQ+foy3wSSbmjBXXIJ6PL3scghJoM8zCL142cGaZKAdCZHyf+Bpu/" +
                          "MmX9zT9Q0zFBVKb36Ma5Fzfa8xA==\"}}";
        return new PackumentSource() {
            @Override
            @SuppressWarnings("unchecked")
            public Map<String, Object> packument(String registry, String n, boolean fullMetadata) {
                return (Map<String, Object>) JsonParseEvenBetterErrors.jsonParse("{\"name\": \"" + name +
                  "\", \"dist-tags\": {\"latest\": \"1.0.0\"}, \"versions\": {\"1.0.0\": " + manifest + "}}");
            }

            @Override
            @SuppressWarnings("unchecked")
            public Map<String, Object> manifest(String registry, String n, String version) {
                return (Map<String, Object>) JsonParseEvenBetterErrors.jsonParse(manifest);
            }
        };
    }

    // npm 11.17.0 writes an empty lock over this manifest (lock/npm/open-root-bin-dir-number-added): normalize's
    // TypeError, which the failure names with the field normalize read
    @Test
    void aRootDirectoriesBinThatIsNoStringIsMalformedAtThatField() {
        String manifest = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"directories\": {\n    \"bin\": 1\n" +
                          "  }\n}\n";
        Result result = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", manifest)
          .now(Instant.EPOCH)
          .build(), NO_REGISTRY).regenerate();

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_FAILS);
        assertThat(result.getErrorMessage()).isEqualTo("MALFORMED_MANIFEST: npm fails on this edit: it cannot apply " +
          "the manifest: directories.bin: ref.replace is not a function");
    }
}

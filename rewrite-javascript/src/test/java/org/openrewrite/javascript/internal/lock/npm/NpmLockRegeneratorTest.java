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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static final Host NODE_24 = Host.builder().os("darwin").cpu("arm64").nodeVersion("v24.21.0").build();
    private static final String DEV_ENGINES = "The developer of this package has specified the following through " +
                                              "devEngines\n";

    private static String devEngines(String devEngines) {
        return "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"devEngines\": " + devEngines + "\n}\n";
    }

    private static NpmOptions npm(String release, String npmrc) {
        return NpmOptions.fromNpmrc(NpmRelease.of(release), null, npmrc);
    }

    // base-cmd.js checkDevEngines (npm 11.17.0): the first error is thrown, with genMsg's lines
    @Test
    void theFirstDevEngineInErrorIsNpmsRejection() {
        String manifest = devEngines("{\"os\":{\"name\":\"linux\",\"onFail\":\"warn\"}," +
                                     "\"runtime\":[{\"name\":\"bun\"},{\"name\":\"node\",\"version\":\"^18\"}]," +
                                     "\"packageManager\":{\"name\":\"pnpm\",\"onFail\":\"download\"}}");

        assertThatThrownBy(() -> NpmLockRegenerator.checkDevEngines(manifest, NpmOptions.DEFAULT, NODE_24))
          .isInstanceOfSatisfying(NpmLockRegenerator.BadDevEngines.class, e -> {
              assertThat(e.getCode()).isEqualTo("EBADDEVENGINES");
              assertThat(e.getEngine()).isEqualTo("runtime");
              assertThat(e.getCurrent()).isEqualTo(NpmTreeSupport.json("{\"name\":\"node\",\"version\":\"v24.21.0\"}"));
              assertThat(e.getRequired()).isEqualTo(NpmTreeSupport.json("[{\"name\":\"bun\"},{\"name\":\"node\"," +
                                                                        "\"version\":\"^18\"}]"));
          })
          .hasMessage(DEV_ENGINES + "Invalid devEngines.runtime\n" +
                      "Invalid name \"bun\" does not match \"node\" for \"runtime\"\n" +
                      "Invalid semver version \"^18\" does not match \"v24.21.0\" for \"runtime\"");
    }

    // only the first message npm logs opens with the line that names devEngines
    @Test
    void devEnginesThatWarnAreLoggedAndUnderForceTheErrorsToo() {
        String manifest = devEngines("{\"os\":{\"name\":\"linux\",\"onFail\":\"warn\"}," +
                                     "\"cpu\":{\"name\":\"x64\",\"onFail\":\"ignore\"}," +
                                     "\"runtime\":{\"name\":\"node\",\"version\":\"^18\",\"onFail\":\"warn\"}," +
                                     "\"packageManager\":{\"name\":\"pnpm\"}}");
        String os = "Invalid devEngines.os\nInvalid name \"linux\" does not match \"darwin\" for \"os\"";
        String runtime = "Invalid devEngines.runtime\n" +
                         "Invalid semver version \"^18\" does not match \"v24.21.0\" for \"runtime\"";

        assertThat(NpmLockRegenerator.checkDevEngines(manifest, npm("11.17.0", "force=true"), NODE_24))
          .containsExactly(DEV_ENGINES + os, runtime, "Invalid devEngines.packageManager\n" +
                                                      "Invalid name \"pnpm\" does not match \"npm\" for " +
                                                      "\"packageManager\"");
        assertThat(NpmLockRegenerator.checkDevEngines(manifest.replace(",\"packageManager\":{\"name\":\"pnpm\"}", ""),
          NpmOptions.DEFAULT, NODE_24)).containsExactly(DEV_ENGINES + os, runtime);
        assertThatThrownBy(() -> NpmLockRegenerator.checkDevEngines(manifest, NpmOptions.DEFAULT, NODE_24))
          .hasMessage(DEV_ENGINES + "Invalid devEngines.packageManager\n" +
                      "Invalid name \"pnpm\" does not match \"npm\" for \"packageManager\"");
    }

    // .catch(() => ({})) and typeof devEngines === 'undefined': null is not undefined
    @Test
    void aManifestThatDoesNotParseHasNoDevEngines() {
        assertThat(NpmLockRegenerator.checkDevEngines(null, NpmOptions.DEFAULT, NODE_24)).isEmpty();
        assertThat(NpmLockRegenerator.checkDevEngines("{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"}}",
          NpmOptions.DEFAULT, NODE_24)).isEmpty();
        assertThat(NpmLockRegenerator.checkDevEngines("{\"name\":\"a\"}", NpmOptions.DEFAULT, NODE_24)).isEmpty();
        assertThatThrownBy(() -> NpmLockRegenerator.checkDevEngines(devEngines("null"), NpmOptions.DEFAULT, NODE_24))
          .isInstanceOf(JsError.class).hasMessage("Invalid non-object value for \"devEngines\"");
    }

    // npm.js runs the check from 10.9.0; npm-install-checks 7.1.2 rewords it in 10.9.5 and 11.6.1
    @Test
    void aReleaseBeforeTheCheckInstallsUnchecked() {
        String manifest = devEngines("{\"packageManager\":{\"name\":\"pnpm\"}}");

        assertThat(NpmLockRegenerator.checkDevEngines(manifest, npm("10.8.3", null), NODE_24)).isEmpty();
        assertThat(NpmLockRegenerator.checkDevEngines(devEngines("7"), npm("10.8.3", null), NODE_24)).isEmpty();
        assertThatThrownBy(() -> NpmLockRegenerator.checkDevEngines(manifest, npm("10.9.0", null), NODE_24))
          .hasMessage(DEV_ENGINES + "Invalid engine \"packageManager\"\n" +
                      "Invalid name \"pnpm\" does not match \"npm\" for \"packageManager\"");
        assertThatThrownBy(() -> NpmLockRegenerator.checkDevEngines(manifest, npm("10.9.5", null), NODE_24))
          .hasMessage(DEV_ENGINES + "Invalid devEngines.packageManager\n" +
                      "Invalid name \"pnpm\" does not match \"npm\" for \"packageManager\"");
    }

    // npm's own version is the release's
    @Test
    void devEnginesHoldThePackageManagerToTheReleaseThatInstalls() {
        String manifest = devEngines("{\"packageManager\":{\"name\":\"npm\",\"version\":\"^11.18.0\"}}");

        assertThat(NpmLockRegenerator.checkDevEngines(manifest, npm("11.21.0", null), NODE_24)).isEmpty();
        assertThatThrownBy(() -> NpmLockRegenerator.checkDevEngines(manifest, npm("11.17.0", null), NODE_24))
          .hasMessage(DEV_ENGINES + "Invalid devEngines.packageManager\n" +
                      "Invalid semver version \"^11.18.0\" does not match \"11.17.0\" for \"packageManager\"");
        assertThatThrownBy(() -> NpmLockRegenerator.checkDevEngines(manifest, npm("12.0.0", null), NODE_24))
          .hasMessageEndingWith("does not match \"12.0.0\" for \"packageManager\"");
    }

    private static NpmLockRegenerator.Inputs.InputsBuilder project(String manifest) {
        return NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", manifest)
          .host(NODE_24)
          .now(Instant.EPOCH);
    }

    private static final String ENGINES_NPM_11 = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"engines\": {\n" +
                                                 "    \"npm\": \"^11.17.0\"\n  }\n}\n";
    private static final String ENGINES_NPM_11_LOCK = LOCK.replace("\"version\": \"1.0.0\"\n    }",
      "\"version\": \"1.0.0\",\n      \"engines\": {\n        \"npm\": \"^11.17.0\"\n      }\n    }");

    // npm.js flatOptions.npmVersion: under engine-strict npm 11.17.0 rejects a root whose engines.npm it does not
    // satisfy (lock/npm/open-root-engines-npm-strict-unsatisfied), and so does every other release by its own version
    @ParameterizedTest
    @CsvSource({"11.16.0,false", "11.17.0,true", "11.21.0,true", "12.0.0,false"})
    void enginesNpmIsHeldToTheReleaseThatInstalls(String release, boolean satisfies) {
        Result result = new NpmLockRegenerator(project(ENGINES_NPM_11)
          .release(NpmRelease.of(release))
          .npmrc("engine-strict=true\n")
          .build(), NO_REGISTRY).regenerate();

        if (satisfies) {
            assertThat(result.getLockFileContent()).isEqualTo(ENGINES_NPM_11_LOCK);
        } else {
            assertThat(result.getErrorMessage()).isEqualTo("UNSUPPORTED_ENTRY_TYPE: npm rejects this edit " +
              "(EBADENGINE): Unsupported engine");
        }
    }

    @Test
    void devEnginesAreCheckedBeforeTheInstall() {
        String manifest = devEngines("{\"runtime\":{\"name\":\"bun\"}}");

        Result rejected = new NpmLockRegenerator(project(manifest).build(), NO_REGISTRY).regenerate();
        assertThat(rejected.getFailure()).isNotNull();
        assertThat(rejected.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
        assertThat(rejected.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: npm rejects this edit " +
          "(EBADDEVENGINES): " + DEV_ENGINES + "Invalid devEngines.runtime\n" +
          "Invalid name \"bun\" does not match \"node\" for \"runtime\"");

        // an Error without a code
        Result fails = new NpmLockRegenerator(project(devEngines("{\"engine\":{\"name\":\"v8\"}}")).build(),
          NO_REGISTRY).regenerate();
        assertThat(fails.getFailure()).isNotNull();
        assertThat(fails.getFailure().getKind()).isEqualTo(Kind.PM_FAILS);
        assertThat(fails.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: npm fails on this edit: Invalid " +
          "property \"devEngines.engine\"");

        // npm.js: commandInstance.checkDevEngines && !this.global
        Result global = new NpmLockRegenerator(project(manifest).npmrc("global=true\n").build(), NO_REGISTRY)
          .regenerate();
        assertThat(global.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: npm rejects this edit " +
          "(ESHRINKWRAPGLOBAL): cannot generate lockfile for global packages");
    }
}

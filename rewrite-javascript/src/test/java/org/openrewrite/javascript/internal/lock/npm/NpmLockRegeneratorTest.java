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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Host;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    // A registry answers a version without its fields, as a proxy does for one it has not cached. Until npm 11.14.1
    // arborist places what it was given and the lock gets an entry without fields (the entries npm 9.7.2 to 10.3.0
    // wrote in closure-basic and its like, from abbreviated packuments); from 11.15.0 it fails the node.
    @ParameterizedTest
    @CsvSource({"11.0.0,true", "11.6.2,true", "11.14.1,true", "11.15.0,false", "11.17.0,false"})
    void aRegistryManifestWithoutVersion(String release, boolean writes) {
        String manifest = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                          "    \"ms\": \"^2.0.0\"\n  }\n}\n";
        PackumentSource registry = new PackumentSource() {
            @Override
            public Map<String, Object> packument(String registry, String name, boolean fullMetadata) {
                return NpmTreeSupport.json("{\"name\":\"ms\",\"dist-tags\":{\"latest\":\"2.1.3\"}," +
                                           "\"versions\":{\"2.1.3\":{}}}");
            }

            @Override
            public Map<String, Object> manifest(String registry, String name, String version) {
                return new LinkedHashMap<>();
            }
        };
        Result result = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .release(NpmRelease.of(release))
          .lock(LOCK)
          .manifest("", manifest)
          .now(Instant.EPOCH)
          .build(), registry).regenerate();

        if (writes) {
            assertThat(result.getLockFileContent()).isEqualTo(LOCK.replace("\"version\": \"1.0.0\"\n    }",
              "\"version\": \"1.0.0\",\n      \"dependencies\": {\n        \"ms\": \"^2.0.0\"\n      }\n    },\n" +
              "    \"node_modules/ms\": {}"));
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
            assertThat(result.getErrorMessage()).contains("(EINCOMPLETEMANIFEST)");
        }
    }

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
          .hasMessage("Invalid non-object value for \"devEngines\"");
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

    private static final String ENGINES_NPM_11 = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n" +
                                                 "  \"engines\": {\n    \"npm\": \"^11.17.0\"\n  }\n}\n";
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
        NpmRelease npm = NpmRelease.PORTED;

        Result rejected = new NpmLockRegenerator(project(manifest).release(npm).build(), NO_REGISTRY).regenerate();
        assertThat(rejected.getFailure()).isNotNull();
        assertThat(rejected.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
        assertThat(rejected.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: npm rejects this edit " +
          "(EBADDEVENGINES): " + DEV_ENGINES + "Invalid devEngines.runtime\n" +
          "Invalid name \"bun\" does not match \"node\" for \"runtime\"");

        // an Error without a code
        Result fails = new NpmLockRegenerator(project(devEngines("{\"engine\":{\"name\":\"v8\"}}")).release(npm)
          .build(), NO_REGISTRY).regenerate();
        assertThat(fails.getFailure()).isNotNull();
        assertThat(fails.getFailure().getKind()).isEqualTo(Kind.PM_FAILS);
        assertThat(fails.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: npm fails on this edit: Invalid " +
          "property \"devEngines.engine\"");

        // npm.js: commandInstance.checkDevEngines && !this.global
        Result global = new NpmLockRegenerator(project(manifest).release(npm).npmrc("global=true\n").build(),
          NO_REGISTRY).regenerate();
        assertThat(global.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: npm rejects this edit " +
          "(ESHRINKWRAPGLOBAL): cannot generate lockfile for global packages");
    }

    // npm reads packageManager for nothing: a project is installed as the release of its inputs, 11.17.0 by default
    @Test
    void aProjectIsInstalledAsTheReleaseOfItsInputs() {
        String named = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"packageManager\": \"npm@10.9.2\"\n}\n";

        assertThat(project(named).build().getRelease()).isEqualTo(NpmRelease.PORTED);
        assertThat(new NpmLockRegenerator(project(named).build(), NO_REGISTRY).regenerate().getLockFileContent())
          .isEqualTo(LOCK);

        // npm 10.9.2 holds the root's devEngines to its own version, as the npm that built the LST
        String devEngines = devEngines("{\"packageManager\":{\"name\":\"npm\",\"version\":\"^10.9.0\"}}");
        assertThat(new NpmLockRegenerator(project(devEngines).release(NpmRelease.of("10.9.2")).build(), NO_REGISTRY)
          .regenerate().isSuccess()).isTrue();
        assertThat(new NpmLockRegenerator(project(devEngines).build(), NO_REGISTRY).regenerate().getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: npm rejects this edit (EBADDEVENGINES): " + DEV_ENGINES +
                     "Invalid devEngines.packageManager\n" +
                     "Invalid semver version \"^10.9.0\" does not match \"11.17.0\" for \"packageManager\"");
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

    /**
     * A root package.json the host holds without its contents is not one npm fails on: the lock's root entry does not
     * stand in for it, and the engine cannot know what npm reads there.
     */
    @Test
    void aRootPackageJsonTheHostHoldsUnreadIsCannotKnow() {
        Result result = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .paths(new LinkedHashSet<>(Arrays.asList("package.json", "package-lock.json")))
          .now(Instant.EPOCH)
          .build(), NO_REGISTRY).regenerate();

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail())
          .isEqualTo("the engine cannot know package.json (its contents were not read)");
    }

    // run with engine-strict under Node 24, npm 10.8.3 rejects this manifest (EBADENGINE) and 10.9.0, 10.9.9 and
    // 11.17.0 write the lock: from 10.9.0 a root with devEngines has its engines left unchecked
    @ParameterizedTest
    @CsvSource({"10.8.3,false", "10.9.0,true", "11.17.0,true"})
    void devEnginesReplaceTheRootsEngines(String release, boolean writes) {
        String manifest = "{\"name\":\"a\",\"version\":\"1.0.0\",\"engines\":{\"node\":\">=99\"}," +
                          "\"devEngines\":{\"runtime\":{\"name\":\"node\"}}}\n";
        Result result = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .release(NpmRelease.of(release))
          .lock(LOCK)
          .manifest("", manifest)
          .npmrc("engine-strict=true\n")
          .host(Host.builder().os("darwin").cpu("arm64").nodeVersion("v24.21.0").build())
          .now(Instant.EPOCH)
          .build(), NO_REGISTRY).regenerate();

        if (writes) {
            assertThat(result.getLockFileContent()).isEqualTo(LOCK.replace("\"version\": \"1.0.0\"\n    }",
              "\"version\": \"1.0.0\",\n      \"engines\": {\n        \"node\": \">=99\"\n      }\n    }"));
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
            assertThat(result.getErrorMessage()).contains("(EBADENGINE)");
        }
    }

    // lock/npm/arb-link-satisfies-peer's lock without its file:v2 link, whose target's folder is not named after
    // its package. npm 10.7.0 and older wrote the target placed from scratch without its name and added the name
    // once they loaded it from that lock (the scenario's before and after as 9.7.2 to 10.7.0 wrote them); from
    // 10.8.2 the name is written at once.
    @ParameterizedTest
    @CsvSource({"10.8.1,false", "10.8.2,true", "11.17.0,true"})
    void aLinkTargetJustPlacedIsNamedByItsFolder(String release, boolean named) {
        NpmScenarios.Edit edit = ScenarioReplay.edit("arb-link-satisfies-peer");
        NpmScenarios.Scenario scenario = edit.getScenario();
        String whole = scenario.before();
        String link = whole.substring(
          whole.indexOf(",\n    \"node_modules/@isaacs/testing-peer-dep-conflict-chain-v\": {"),
          whole.indexOf("\n  }\n}"));
        Result result = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .release(NpmRelease.of(release))
          .lock(whole.replace(link, ""))
          .manifests(edit.getStart().manifests())
          .host(ScenarioReplay.host(scenario))
          .now(ScenarioReplay.now(scenario))
          .build(), ScenarioReplay.registry(new ScenarioReplay.ScenarioRegistry(scenario))).regenerate();

        if (named) {
            assertThat(result.getLockFileContent()).isEqualTo(whole);
        } else {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.SECOND_INSTALL);
            assertThat(result.getErrorMessage()).contains("would rewrite it: line 64 \"\"version\": \"2.0.0\"\" " +
              "becomes \"\"name\": \"@isaacs/testing-peer-dep-conflict-chain-v\",\"");
        }
    }

    // lock/npm/gap-orphan-prune-duplicate-name's lock without is-buffer, which the kind-of nested under is-number
    // requires and npm places at the top. Run over it, npm 10.4.0 and 10.7.0 never exit (killed after three
    // minutes); 10.3.0, 10.8.0 and 11.17.0 write the lock: their DepsQueue sorts by depth when it is popped, where
    // the one of 10.4.0 to 10.7.x files a node under its depth and never looks above the depth it has reached.
    @ParameterizedTest
    @CsvSource({"10.3.0,false,false", "10.4.0,true,false", "10.7.0,true,false", "10.8.0,false,true",
      "11.17.0,false,true"})
    void aPlacementAboveTheDepthBeingBuiltIsNeverReached(String release, boolean hangs, boolean license) {
        NpmScenarios.Scenario scenario = ScenarioReplay.edit("gap-orphan-prune-duplicate-name").getScenario();
        String whole = scenario.before();
        String isBuffer = whole.substring(whole.indexOf("    \"node_modules/is-buffer\": {"),
          whole.indexOf("    \"node_modules/is-number\": {"));
        Result result = new NpmLockRegenerator(NpmLockRegenerator.Inputs.builder()
          .release(NpmRelease.of(release))
          .lock(whole.replace(isBuffer, ""))
          .manifest("", scenario.pkgBefore())
          .host(ScenarioReplay.host(scenario))
          .now(ScenarioReplay.now(scenario))
          .build(), ScenarioReplay.registry(new ScenarioReplay.ScenarioRegistry(scenario))).regenerate();

        if (hangs) {
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_FAILS);
            assertThat(result.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: npm fails on this edit: it never " +
              "finishes: its queue of dependencies to build no longer reaches node_modules/is-buffer");
        } else {
            assertThat(result.getLockFileContent()).isEqualTo(license ? whole :
              whole.replace(isBuffer, isBuffer.replace(",\n      \"license\": \"MIT\"", "")));
        }
    }

    private static final String EMPTY_RULES_HASH =
      "sha512-J8dGcK23UHX60FjVzq97IMTneGyDuuijL2Jvl4KvNMmjPCBG72D9Knh403jin+yFGAa72aZ4ePOp8c2kgwdj/Q==";
    private static final String IS_ODD_RULE_HASH =
      "sha512-h32ouO4a7osRa5vCtTHIhZJpFFdMwv+bf2ausl7cJUTBlfnzQ4bGMBVx6YuYFz3eHrroKgJyt0r/p+zjKT+M7w==";

    private static String manifest(String version, String... fields) {
        StringBuilder manifest = new StringBuilder("{\n  \"name\": \"a\",\n  \"version\": \"" + version + "\"");
        for (String field : fields) {
            manifest.append(",\n  ").append(field);
        }
        return manifest.append("\n}\n").toString();
    }

    private static String lock(String version, int lockfileVersion, String... rootFields) {
        StringBuilder root = new StringBuilder();
        for (String field : rootFields) {
            root.append(",\n      ").append(field);
        }
        return "{\n  \"name\": \"a\",\n  \"version\": \"" + version + "\",\n  \"lockfileVersion\": " + lockfileVersion +
               ",\n  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": \"a\",\n      \"version\": " +
               "\"" + version + "\"" + root + "\n    }\n  }\n}\n";
    }

    private static Result regenerate(String release, String lock, String manifest) {
        return regenerate(NpmLockRegenerator.Inputs.builder().release(NpmRelease.of(release)).lock(lock)
          .manifest("", manifest));
    }

    private static Result regenerate(NpmLockRegenerator.Inputs.InputsBuilder inputs) {
        return new NpmLockRegenerator(inputs.now(Instant.EPOCH).build(), NO_REGISTRY).regenerate();
    }

    // npm 12.2.0 over each of these projects, which have no dependency: the root's packageExtensions go into the
    // lock as their digest, and a lock that holds one is lockfileVersion 4 whatever lockfile-version says ("manifest
    // extensions require lockfileVersion 4; upgrading the lockfile from version 3"). npm 11.21.0 leaves the lock as
    // it is, and writes lockfile-version's 3 over a 4.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "{} | " + EMPTY_RULES_HASH,
      "{\"is-odd@3\":{\"dependencies\":{\"isarray\":\"2.0.5\"}}} | " + IS_ODD_RULE_HASH
    })
    void theRootsPackageExtensionsAreRecordedInALockfileVersion4From12(String rules, String hash) {
        String manifest = manifest("1.0.0", "\"packageExtensions\": " + rules);
        String extended = lock("1.0.0", 4, "\"packageExtensionsHash\": \"" + hash + "\"");
        for (String release : new String[]{"12.0.0", "12.2.0"}) {
            assertThat(regenerate(release, LOCK, manifest).getLockFileContent()).isEqualTo(extended);
            assertThat(regenerate(release, extended, manifest).getLockFileContent()).isEqualTo(extended);
            assertThat(regenerate(NpmLockRegenerator.Inputs.builder().release(NpmRelease.of(release))
              .lock(lock("1.0.0", 4)).manifest("", manifest).npmrc("lockfile-version=3\n")).getLockFileContent())
              .isEqualTo(extended);
        }
        assertThat(regenerate("11.21.0", LOCK, manifest).getLockFileContent()).isEqualTo(LOCK);
        assertThat(regenerate(NpmLockRegenerator.Inputs.builder().release(NpmRelease.of("11.21.0"))
          .lock(lock("1.0.0", 4)).manifest("", manifest).npmrc("lockfile-version=3\n")).getLockFileContent())
          .isEqualTo(LOCK);
    }

    // npm 12.2.0's own words over each manifest; npm 11.21.0 reads neither field and writes the lock
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "\"packageExtensions\": [] | EEXTENSIONROOT | packageExtensions must be an object",
      "\"packageExtensions\": {\"is-odd\": {\"scripts\": {}}} | EEXTENSIONFIELD | packageExtensions[\"is-odd\"] has " +
      "unsupported field \"scripts\". Supported fields: dependencies, optionalDependencies, peerDependencies, " +
      "peerDependenciesMeta.",
      "\"patchedDependencies\": {\"left-pad@1.3.0\": \"patches/left-pad.patch\"} | EPATCHUNUSED | The following " +
      "patches were registered but matched no installed package:~  left-pad@1.3.0 -> patches/left-pad.patch~" +
      "Use --allow-unused-patches to install anyway."
    })
    void npm12RefusesExtensionsAndPatchesItCannotApply(String field, String code, String message) {
        for (String release : new String[]{"12.0.0", "12.2.0"}) {
            Result result = regenerate(release, LOCK, manifest("1.0.0", field));
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
            assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_MANIFEST);
            assertThat(result.getFailure().getDetail()).contains(code).contains(message.replace('~', '\n'));
        }
        assertThat(regenerate("11.21.0", LOCK, manifest("1.0.0", field)).getLockFileContent()).isEqualTo(LOCK);
    }

    // npm 12.2.0: "Unsupported lockfileVersion 5. This npm only supports up to 4. Please upgrade npm."; it keeps a 4
    // nothing needs, and an empty patchedDependencies changes nothing. npm 11.21.0 writes the 5 and the 4 back.
    @Test
    void npm12RefusesALockNewerThanLockfileVersion4() {
        for (String release : new String[]{"12.0.0", "12.2.0"}) {
            Result result = regenerate(release, lock("1.0.0", 5), MANIFEST);
            assertThat(result.getFailure()).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
            assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_LOCKFILE_VERSION);
            assertThat(result.getFailure().getDetail()).contains("ELOCKFILEVERSION")
              .contains("Unsupported lockfileVersion 5. This npm only supports up to 4. Please upgrade npm.");
            assertThat(regenerate(release, lock("1.0.0", 4), MANIFEST).getLockFileContent())
              .isEqualTo(lock("1.0.0", 4));
            assertThat(regenerate(release, LOCK, manifest("1.0.0", "\"patchedDependencies\": {}"))
              .getLockFileContent()).isEqualTo(LOCK);
        }
        assertThat(regenerate("11.21.0", lock("1.0.0", 5), MANIFEST).getLockFileContent())
          .isEqualTo(lock("1.0.0", 5));
        assertThat(regenerate("11.21.0", lock("1.0.0", 4), MANIFEST).getLockFileContent())
          .isEqualTo(lock("1.0.0", 4));
    }

    // npm 12.2.0 over a project whose patchedDependencies names its directory dependency lib, with no patch file:
    // "patch file not found: patches/lib.patch". The directory itself, which no edge names, is asked before the link
    // to it, so npm reads the patch first; with the file there it goes on to the link, which is no registry
    // dependency to patch. npm 11.21.0 writes the lock back.
    @Test
    void npm12ReadsThePatchOfADirectoryDependency() {
        String lock = "{\n  \"name\": \"p\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": 3,\n  \"requires\": " +
                      "true,\n  \"packages\": {\n    \"\": {\n      \"name\": \"p\",\n      \"version\": \"1.0.0\",\n" +
                      "      \"dependencies\": {\n        \"lib\": \"file:lib\"\n      }\n    },\n    \"lib\": {\n" +
                      "      \"version\": \"1.0.0\"\n    },\n    \"node_modules/lib\": {\n      \"resolved\": " +
                      "\"lib\",\n      \"link\": true\n    }\n  }\n}\n";
        NpmLockRegenerator.Inputs.InputsBuilder inputs = NpmLockRegenerator.Inputs.builder()
          .lock(lock)
          .manifest("", "{\n  \"name\": \"p\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n    \"lib\": " +
                        "\"file:lib\"\n  },\n  \"patchedDependencies\": {\n    \"lib\": \"patches/lib.patch\"\n  }\n" +
                        "}\n")
          .manifest("lib", "{\n  \"name\": \"lib\",\n  \"version\": \"1.0.0\"\n}\n")
          .paths(new LinkedHashSet<>(Arrays.asList("package.json", "package-lock.json", "lib/package.json")));

        Result npm12 = regenerate(inputs.release(NpmRelease.of("12.2.0")));
        assertThat(npm12.getFailure()).isNotNull();
        assertThat(npm12.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
        assertThat(npm12.getFailure().getDetail()).contains("(EPATCHNOTFOUND)")
          .contains("patch file not found: patches/lib.patch");

        assertThat(regenerate(inputs.release(NpmRelease.of("11.21.0"))).getLockFileContent()).isEqualTo(lock);

        // with the patch file there, npm 12.2.0 reads it for the directory and then refuses the link
        Result held = regenerate(inputs.release(NpmRelease.of("12.2.0")).paths(new LinkedHashSet<>(Arrays.asList(
          "package.json", "package-lock.json", "lib/package.json", "patches/lib.patch"))));
        assertThat(held.getFailure()).isNotNull();
        assertThat(held.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
        assertThat(held.getFailure().getDetail()).contains("(EPATCHNONREGISTRY)")
          .contains("Cannot patch non-registry dependency lib@1.0.0 (selector \"lib\"). Only registry dependencies " +
                    "can be patched.");
    }

    // a project that holds an npm-shrinkwrap.json beside its package-lock.json: npm 11.21.0 reads and writes the
    // shrinkwrap in the lock's place, npm 12.2.0 leaves it alone and writes the package-lock.json
    @Test
    void npm12NoLongerReadsTheProjectsNpmShrinkwrap() {
        NpmLockRegenerator.Inputs.InputsBuilder inputs = NpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", manifest("1.0.1"))
          .paths(new LinkedHashSet<>(Arrays.asList("package.json", "package-lock.json", "npm-shrinkwrap.json")));

        Result npm11 = regenerate(inputs.release(NpmRelease.of("11.21.0")));
        assertThat(npm11.getFailure()).isNotNull();
        assertThat(npm11.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(npm11.getFailure().getDetail()).contains("npm-shrinkwrap.json");

        assertThat(regenerate(inputs.release(NpmRelease.of("12.0.0"))).getLockFileContent())
          .isEqualTo(lock("1.0.1", 3));
    }
}

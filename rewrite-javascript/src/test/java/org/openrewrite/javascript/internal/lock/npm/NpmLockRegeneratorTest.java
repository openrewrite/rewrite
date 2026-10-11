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

import java.time.Instant;
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
}

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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
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

    private static final String NAMES_NO_RELEASE = "; package.json names no release in packageManager or " +
                                                   "devEngines.packageManager";
    private static final String INTEGRITY = "sha512-6/mh1E2u2YgEsCHdY0Yx5oW+61gZU+1vXaoiHHrpKeuRNNgFvS+/jrwHiQhB5apA" +
                                            "f5oB7UB7E19ol2R2LKH8hQ==";

    // a depends on b 1.0.0, which holds npm to ^11.17.0
    private static final String DEPENDENT = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n" +
                                            "  \"dependencies\": {\n    \"b\": \"1.0.0\"\n  }\n}\n";
    private static final String DEPENDENT_LOCK = LOCK.replace("\"version\": \"1.0.0\"\n    }",
      "\"version\": \"1.0.0\",\n      \"dependencies\": {\n        \"b\": \"1.0.0\"\n      }\n    },\n" +
      "    \"node_modules/b\": {\n      \"version\": \"1.0.0\",\n" +
      "      \"resolved\": \"https://registry.npmjs.org/b/-/b-1.0.0.tgz\",\n" +
      "      \"integrity\": \"" + INTEGRITY + "\",\n" +
      "      \"engines\": {\n        \"npm\": \"^11.17.0\"\n      }\n    }");

    private static List<NpmRelease> releases(String... versions) {
        List<NpmRelease> releases = new ArrayList<>();
        for (String version : versions) {
            releases.add(NpmRelease.of(version));
        }
        return releases;
    }

    // every release from one to another, newest first
    private static List<NpmRelease> releasesBetween(String oldest, String newest) {
        List<NpmRelease> all = NpmReleases.all();
        List<NpmRelease> releases = new ArrayList<>(all.subList(all.indexOf(NpmRelease.of(oldest)),
          all.indexOf(NpmRelease.of(newest)) + 1));
        Collections.reverse(releases);
        return releases;
    }

    private static NpmLockRegenerator regenerator(NpmLockRegenerator.Inputs.InputsBuilder project) {
        return new NpmLockRegenerator(project.build(), NO_REGISTRY);
    }

    // without a release from the caller, the releases come from the package.json where npm installs
    @Test
    void aProjectIsInstalledAsTheReleasesItsPackageJsonAllows() {
        String named = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"packageManager\": \"npm@%s\"\n}\n";

        assertThat(regenerator(project(MANIFEST)).regenerate().getLockFileContent()).isEqualTo(LOCK);
        assertThat(regenerator(project(String.format(named, NpmRelease.PORTED))).regenerate().getLockFileContent())
          .isEqualTo(LOCK);

        Result unknown = regenerator(project(String.format(named, "6.14.18"))).regenerate();
        assertThat(unknown.getFailure()).isNotNull();
        assertThat(unknown.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(unknown.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine cannot know what npm " +
          "6.14.18 writes, since the engine reproduces npm " + NpmReleases.ranges(NpmReleases.proven()));

        // the release the caller names installs it whatever its package.json says
        assertThat(regenerator(project(String.format(named, "6.14.18")).release(NpmRelease.PORTED)).regenerate()
          .getLockFileContent()).isEqualTo(LOCK);
    }

    private static List<String> standsFor(NpmLockRegenerator.Install install) {
        return install.getCandidates().stream().map(NpmRelease::toString).collect(toList());
    }

    @Test
    void anInstallThatAsksNothingOfItsReleaseStandsForEveryCandidate() {
        NpmLockRegenerator regenerator = regenerator(project(MANIFEST));
        List<NpmRelease> candidates = releasesBetween("7.0.0", "12.2.0");

        List<NpmLockRegenerator.Install> installs = regenerator.installs(candidates);

        assertThat(installs).hasSize(1);
        assertThat(installs.get(0).getRelease()).hasToString("12.2.0");
        assertThat(installs.get(0).getCandidates()).isEqualTo(candidates);
        assertThat(regenerator.regenerate(candidates).getLockFileContent()).isEqualTo(LOCK);
    }

    // npm checks devEngines from 10.9.0 and words what it finds another way from 10.9.5: three ways to install
    @Test
    void releasesThatAnswerAnInstallAnotherWayAreInstalledAgain() {
        NpmLockRegenerator regenerator = regenerator(project(devEngines("{\"runtime\":{\"name\":\"bun\"}}")));
        List<NpmRelease> candidates = releasesBetween("10.8.0", "10.9.9");

        List<NpmLockRegenerator.Install> installs = regenerator.installs(candidates);

        assertThat(installs).extracting(NpmLockRegeneratorTest::standsFor).containsExactly(
          asList("10.9.9", "10.9.8", "10.9.7", "10.9.6", "10.9.5"),
          asList("10.9.4", "10.9.3", "10.9.2", "10.9.1", "10.9.0"),
          asList("10.8.3", "10.8.2", "10.8.1", "10.8.0"));
        assertThat(installs).extracting(install -> install.getResult().getErrorMessage()).containsExactly(
          "RESOLUTION_REQUIRED: npm rejects this edit (EBADDEVENGINES): " + DEV_ENGINES +
          "Invalid devEngines.runtime\nInvalid name \"bun\" does not match \"node\" for \"runtime\"",
          "RESOLUTION_REQUIRED: npm rejects this edit (EBADDEVENGINES): " + DEV_ENGINES +
          "Invalid engine \"runtime\"\nInvalid name \"bun\" does not match \"node\" for \"runtime\"",
          null);
        assertThat(installs).extracting(NpmLockRegenerator.Install::isRefusing).containsOnly(false);

        Result result = regenerator.regenerate(candidates);
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine cannot know the npm release " +
          "that installs the project: npm 10.9.0 - 10.9.9 rejects it (EBADDEVENGINES) and npm 10.8.0 - 10.8.3 " +
          "installs it" + NAMES_NO_RELEASE);
        // where every install fails, the newest one's failure
        assertThat(regenerator.regenerate(releasesBetween("10.9.0", "10.9.9"))).isEqualTo(installs.get(0).getResult());
    }

    // an npm that refuses the project for its own version is not the npm that installs it
    @Test
    void anNpmThatDevEnginesRefuseIsLeftOut() {
        NpmLockRegenerator regenerator = regenerator(project(devEngines(
          "{\"packageManager\":{\"name\":\"npm\",\"version\":\"^11.17.0\"}}")));
        List<NpmRelease> candidates = releases("12.2.0", "12.0.0", "11.21.0", "11.17.0", "11.16.0");

        List<NpmLockRegenerator.Install> installs = regenerator.installs(candidates);

        assertThat(installs).extracting(NpmLockRegeneratorTest::standsFor).containsExactly(
          asList("12.2.0", "12.0.0", "11.16.0"), asList("11.21.0", "11.17.0"));
        assertThat(installs).extracting(NpmLockRegenerator.Install::isRefusing).containsExactly(true, false);
        assertThat(installs).extracting(NpmLockRegenerator.Install::getCode).containsExactly("EBADDEVENGINES", null);
        assertThat(regenerator.regenerate(candidates).getLockFileContent()).isEqualTo(LOCK);
    }

    @Test
    void whereEveryNpmRefusesTheNewestOnesRejectionIsTheResult() {
        NpmLockRegenerator regenerator = regenerator(project(devEngines(
          "{\"packageManager\":{\"name\":\"npm\",\"version\":\"^9\"}}")));

        // 11.6.0 words it as npm-install-checks 7.1.1 does
        List<NpmLockRegenerator.Install> installs = regenerator.installs(releases("11.17.0", "11.6.1", "11.6.0"));
        assertThat(installs).extracting(NpmLockRegeneratorTest::standsFor)
          .containsExactly(asList("11.17.0", "11.6.1"), singletonList("11.6.0"));
        assertThat(installs).extracting(NpmLockRegenerator.Install::isRefusing).containsOnly(true);

        assertThat(regenerator.regenerate(releases("11.17.0", "11.6.1", "11.6.0")).getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: npm rejects this edit (EBADDEVENGINES): " + DEV_ENGINES +
                     "Invalid devEngines.packageManager\n" +
                     "Invalid semver version \"^9\" does not match \"11.17.0\" for \"packageManager\"");
    }

    // lock/npm/open-root-engines-npm-strict-unsatisfied: under engine-strict npm rejects a root it does not satisfy
    @Test
    void anNpmTheRootsEnginesRefuseUnderEngineStrictIsLeftOut() {
        NpmLockRegenerator regenerator = regenerator(project(ENGINES_NPM_11).npmrc("engine-strict=true\n"));
        List<NpmRelease> candidates = releases("12.2.0", "12.0.0", "11.21.0", "11.17.0");

        List<NpmLockRegenerator.Install> installs = regenerator.installs(candidates);

        assertThat(installs).extracting(NpmLockRegeneratorTest::standsFor).containsExactly(
          asList("12.2.0", "12.0.0"), asList("11.21.0", "11.17.0"));
        assertThat(installs).extracting(NpmLockRegenerator.Install::isRefusing).containsExactly(true, false);
        assertThat(installs.get(0).getCode()).isEqualTo("EBADENGINE");
        assertThat(regenerator.regenerate(candidates).getLockFileContent()).isEqualTo(ENGINES_NPM_11_LOCK);
    }

    // the range is a dependency's, not the project's own: no release is the one the project names
    @Test
    void anNpmADependencysEnginesRejectIsNotLeftOut() {
        NpmLockRegenerator regenerator = regenerator(project(DEPENDENT).lock(DEPENDENT_LOCK)
          .npmrc("engine-strict=true\n"));
        List<NpmRelease> candidates = releases("12.2.0", "12.0.0", "11.21.0", "11.17.0");

        List<NpmLockRegenerator.Install> installs = regenerator.installs(candidates);

        assertThat(installs).extracting(NpmLockRegenerator.Install::isRefusing).containsExactly(false, false);
        assertThat(installs.get(1).getResult().getLockFileContent()).isEqualTo(DEPENDENT_LOCK);
        assertThat(regenerator.regenerate(candidates).getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine " +
          "cannot know the npm release that installs the project: npm 12.0.0, 12.2.0 rejects it (EBADENGINE) and " +
          "npm 11.17.0, 11.21.0 installs it" + NAMES_NO_RELEASE);
    }

    // without engine-strict npm only warns, and the install is still held to the range its lock names
    @Test
    void anInstallStandsOnlyForReleasesThatSatisfyTheRangesItReadAlike() {
        NpmLockRegenerator regenerator = regenerator(project(DEPENDENT).lock(DEPENDENT_LOCK));
        List<NpmRelease> candidates = releases("11.17.0", "11.21.0", "12.2.0", "10.9.9");

        List<NpmLockRegenerator.Install> installs = regenerator.installs(candidates);

        assertThat(installs).extracting(NpmLockRegeneratorTest::standsFor)
          .containsExactly(asList("11.17.0", "11.21.0"), asList("12.2.0", "10.9.9"));
        assertThat(regenerator.regenerate(candidates).getLockFileContent()).isEqualTo(DEPENDENT_LOCK);
    }

    /**
     * b's latest, 1.1.0, holds npm to {@code >=12}: npm-pick-manifest gives a release it excludes 1.0.0. The same
     * documents are handed to every install, as {@code NpmRegistryPackuments} does.
     */
    private static final class Registry implements PackumentSource {
        private final Map<String, Object> packument = NpmTreeSupport.json("{\"name\":\"b\"," +
          "\"dist-tags\":{\"latest\":\"1.1.0\"},\"versions\":{" +
          "\"1.0.0\":{\"name\":\"b\",\"version\":\"1.0.0\",\"dist\":{\"tarball\":" +
          "\"https://registry.npmjs.org/b/-/b-1.0.0.tgz\",\"integrity\":\"" + INTEGRITY + "\"}}," +
          "\"1.1.0\":{\"name\":\"b\",\"version\":\"1.1.0\",\"engines\":{\"npm\":\">=12\"},\"dist\":{\"tarball\":" +
          "\"https://registry.npmjs.org/b/-/b-1.1.0.tgz\",\"integrity\":\"" + INTEGRITY + "\"}}}}");
        private final String pristine = packument.toString();

        @Override
        public Map<String, Object> packument(String registry, String name, boolean fullMetadata) {
            return packument;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> manifest(String registry, String name, String version) {
            return (Map<String, Object>) ((Map<String, Object>) packument.get("versions")).get(version);
        }
    }

    @Test
    void releasesThatWriteAnotherLockMakeTheReleaseThatInstallsCannotKnow() {
        Registry registry = new Registry();
        NpmLockRegenerator regenerator = new NpmLockRegenerator(project(DEPENDENT.replace("\"1.0.0\"\n  }",
          "\"^1.0.0\"\n  }")).build(), registry);
        List<NpmRelease> candidates = releasesBetween("11.17.0", "12.2.0");

        List<NpmLockRegenerator.Install> installs = regenerator.installs(candidates);

        // the range comes from the registry: the install as 12.2.0 read it, and stands for the releases it admits
        assertThat(installs).extracting(install -> NpmReleases.ranges(install.getCandidates()))
          .containsExactly("12.0.0 - 12.2.0", "11.17.0 - 11.21.0");
        assertThat(installs.get(0).getResult().getLockFileContent()).contains("/b/-/b-1.1.0.tgz");
        assertThat(installs.get(1).getResult().getLockFileContent()).contains("/b/-/b-1.0.0.tgz");
        // each install normalized its own copy of the documents
        assertThat(registry.packument).hasToString(registry.pristine);

        assertThat(regenerator.regenerate(candidates).getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine " +
          "cannot know the npm release that installs the project: npm 12.0.0 - 12.2.0 writes one lock and npm " +
          "11.17.0 - 11.21.0 another" + NAMES_NO_RELEASE);
        assertThat(regenerator.regenerate(releasesBetween("11.17.0", "11.21.0")).getLockFileContent())
          .isEqualTo(installs.get(1).getResult().getLockFileContent());
    }

    private static NpmLockRegenerator.Install installed(Result result, boolean refusing, @Nullable String code,
                                                         String... standsFor) {
        NpmLockRegenerator.Install install = new NpmLockRegenerator.Install(NpmRelease.of(standsFor[0]).followed(),
          result, refusing, code, emptySet(), null);
        install.getCandidates().addAll(releases(standsFor));
        return install;
    }

    private static Result failed(Kind kind, String detail) {
        return Result.failure(new Failure(kind, Reason.RESOLUTION_REQUIRED, null, detail));
    }

    @Test
    void theNewestInstallIsTheResultWhereEveryOneRefusesOrFailsOrWritesTheSame() {
        Result lock = Result.success("lock", singletonMap("yarn.lock", "yarn"));
        Result refused = failed(Kind.PM_REJECTS, "npm rejects this edit (EBADDEVENGINES): 12");
        Result refusedToo = failed(Kind.PM_REJECTS, "npm rejects this edit (EBADENGINE): 11");
        Result rejected = failed(Kind.PM_REJECTS, "npm rejects this edit (EALLOWGIT): no");
        Result unknown = failed(Kind.CANNOT_KNOW, "the engine cannot know git manifest");

        assertThat(NpmLockRegenerator.result(asList(installed(refused, true, "EBADDEVENGINES", "12.2.0"),
          installed(refusedToo, true, "EBADENGINE", "11.17.0")))).isSameAs(refused);
        assertThat(NpmLockRegenerator.result(asList(installed(refused, true, "EBADDEVENGINES", "12.2.0"),
          installed(lock, false, null, "11.17.0")))).isSameAs(lock);
        assertThat(NpmLockRegenerator.result(asList(installed(refused, true, "EBADDEVENGINES", "12.2.0"),
          installed(rejected, false, "EALLOWGIT", "12.0.0"), installed(unknown, false, null, "11.17.0"))))
          .isSameAs(rejected);
        assertThat(NpmLockRegenerator.result(asList(installed(lock, false, null, "12.2.0"),
          installed(Result.success("lock", singletonMap("yarn.lock", "yarn")), false, null, "11.17.0"))))
          .isSameAs(lock);
        assertThatThrownBy(() -> NpmLockRegenerator.result(emptyList())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void installsThatDifferNameTheReleasesOfEachOutcome() {
        Result lock = Result.success("lock");
        Result rejected = failed(Kind.PM_REJECTS, "npm rejects this edit (EALLOWGIT): no");

        assertThat(differ(installed(lock, false, null, "12.2.0", "12.1.0"),
          installed(Result.success("other"), false, null, "12.0.2", "12.0.1", "12.0.0"),
          installed(Result.success("lock"), false, null, "11.21.0"),
          installed(Result.success("lock", singletonMap("yarn.lock", "yarn")), false, null, "11.20.0")))
          .isEqualTo("npm 11.21.0, 12.1.0 - 12.2.0 writes one lock, npm 12.0.0 - 12.0.2 another and npm 11.20.0 " +
                     "another");
        assertThat(differ(installed(rejected, false, "EALLOWGIT", "12.2.0", "12.1.0", "12.0.2", "12.0.1", "12.0.0"),
          installed(lock, false, null, "11.21.0", "11.20.0")))
          .isEqualTo("npm 12.0.0 - 12.2.0 rejects it (EALLOWGIT) and npm 11.20.0 - 11.21.0 installs it");
        // an npm that refuses its own version is said nothing of
        assertThat(differ(installed(failed(Kind.PM_REJECTS, "refused"), true, "EBADDEVENGINES", "12.2.0"),
          installed(failed(Kind.PM_REJECTS, "npm would discard the lock"), false, null, "12.1.0"),
          installed(failed(Kind.PM_FAILS, "npm fails on this edit"), false, null, "12.0.2"),
          installed(failed(Kind.CANNOT_KNOW, "the engine cannot know"), false, null, "12.0.1"),
          installed(failed(Kind.SECOND_INSTALL, "a second npm install"), false, null, "12.0.0"),
          installed(rejected, false, "EALLOWGIT", "11.21.0"),
          installed(failed(Kind.PM_REJECTS, "npm rejects this edit (EALLOWGIT): again"), false, "EALLOWGIT",
            "11.20.0"),
          installed(lock, false, null, "11.19.1"),
          installed(Result.success("other"), false, null, "11.19.0")))
          .isEqualTo("npm 12.1.0 rejects it, npm 12.0.2 fails on it, what npm 12.0.1 writes cannot be known, " +
                     "npm 12.0.0 writes a lock its second install does not keep, npm 11.20.0 - 11.21.0 rejects it " +
                     "(EALLOWGIT), npm 11.19.1 installs it and npm 11.19.0 installs it with another lock");
    }

    private static String differ(NpmLockRegenerator.Install... installs) {
        Result result = NpmLockRegenerator.result(asList(installs));
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        String detail = result.getFailure().getDetail();
        assertThat(detail).startsWith("the engine cannot know the npm release that installs the project: ")
          .endsWith(NAMES_NO_RELEASE);
        return detail.substring("the engine cannot know the npm release that installs the project: ".length(),
          detail.length() - NAMES_NO_RELEASE.length());
    }

    @Test
    void anInstallStandsForTheReleasesThatAnswerWhatItAskedAsItsOwnDid() {
        NpmRelease release = NpmRelease.of("11.17.0").followed();
        NpmLockRegenerator.Install install = new NpmLockRegenerator.Install(release, Result.success("lock"), false,
          null, new LinkedHashSet<>(asList(">=11.5", "<13")),
          NpmTreeSupport.json("{\"packageManager\":{\"name\":\"npm\",\"version\":\"<12.2\"}}"));

        assertThat(install.standsFor(NpmRelease.of("11.17.0"))).isTrue();
        assertThat(install.standsFor(NpmRelease.of("12.1.0"))).isTrue();
        // a range of engines.npm one satisfies and the other does not
        assertThat(install.standsFor(NpmRelease.of("11.4.2"))).isFalse();
        // devEngines.packageManager
        assertThat(install.standsFor(NpmRelease.of("12.2.0"))).isFalse();
        // a threshold the install asked about
        assertThat(release.atLeast(12, 0, 0)).isFalse();
        assertThat(install.standsFor(NpmRelease.of("11.21.0"))).isTrue();
        assertThat(install.standsFor(NpmRelease.of("12.1.0"))).isFalse();
    }

    private static NpmLockRegenerator recorded(NpmScenarios.Edit edit) {
        return new NpmLockRegenerator(ScenarioReplay.inputs(edit),
          ScenarioReplay.registry(new ScenarioReplay.ScenarioRegistry(edit.getScenario())));
    }

    // lock/npm/add-leaf: every release from 10.8.3 was recorded writing the lock 11.17.0 writes
    @Test
    void releasesRecordedWritingTheSameLockComeToThatLock() {
        NpmScenarios.Edit edit = ScenarioReplay.edit("add-leaf");

        Result result = recorded(edit).regenerate(releases("12.2.0", "12.0.2", "11.21.0", "11.17.0", "10.9.9",
          "10.8.3"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result).isEqualTo(recorded(edit).regenerate(singletonList(NpmRelease.PORTED)));
    }

    // lock/npm/gap-remote-tarball-add: npm 12 rejects the tarball URL that the engine cannot read for npm 11.17.0
    @Test
    void releasesRecordedFailingEachTheirOwnWayComeToTheNewestOnesFailure() {
        NpmLockRegenerator regenerator = recorded(ScenarioReplay.edit("gap-remote-tarball-add"));

        Result result = regenerator.regenerate(releases("12.2.0", "12.0.2", "11.17.0"));

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
        assertThat(result.getErrorMessage()).isEqualTo("UNSUPPORTED_ENTRY_TYPE " +
          "[kind-of@https://registry.npmjs.org/kind-of/-/kind-of-6.0.3.tgz]: npm rejects this edit (EALLOWREMOTE): " +
          "Fetching packages of type \"remote\" have been disabled");
        assertThat(regenerator.regenerate(singletonList(NpmRelease.PORTED)).getErrorMessage())
          .isEqualTo("RESOLUTION_REQUIRED: the engine cannot know remote tarball " +
                     "https://registry.npmjs.org/kind-of/-/kind-of-6.0.3.tgz");
    }

    // lock/npm/open-git-spec-change: the install asks for the default of allow-git, which npm 12.0.0 changed
    @Test
    void anInstallAsTheReleasePortedStandsForTheReleasesThatAnswerAlike() {
        NpmLockRegenerator regenerator = recorded(ScenarioReplay.edit("open-git-spec-change"));

        NpmLockRegenerator.Install install = regenerator.installs(singletonList(NpmRelease.PORTED)).get(0);

        assertThat(install.getResult().getErrorMessage()).isEqualTo("RESOLUTION_REQUIRED: the engine cannot know git " +
          "manifest github:jonschlinkert/is-number#7.0.0");
        assertThat(install.standsFor(NpmRelease.PORTED)).isTrue();
        assertThat(install.standsFor(NpmRelease.of("12.0.0"))).isFalse();
        assertThat(install.standsFor(NpmRelease.of("12.2.0"))).isFalse();
        // nothing in the project holds npm to a range, so the answers alone decide
        for (NpmRelease release : NpmReleases.all()) {
            assertThat(install.standsFor(release)).as(release.toString())
              .isEqualTo(release.answersAs(install.getRelease()));
        }
        assertThat(regenerator.regenerate(releases("12.2.0", "12.0.2", "11.17.0")).getErrorMessage())
          .isEqualTo("UNSUPPORTED_ENTRY_TYPE [is-number@github:jonschlinkert/is-number#7.0.0]: npm rejects this " +
                     "edit (EALLOWGIT): Fetching packages of type \"git\" have been disabled");
    }

    // lock/npm/small-git-dep-beside: npm 12 rejects the git dependency only where it must fetch it, which it need
    // not over the lock that holds it (release/12.0.2/cross is the lock 11.17.0 writes)
    @Test
    void aGitDependencyTheLockHoldsIsInstalledAlikeByNpm12() {
        NpmScenarios.Edit edit = ScenarioReplay.edit("small-git-dep-beside");

        Result result = recorded(edit).regenerate(releases("12.2.0", "12.0.2", "11.21.0", "11.17.0"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result).isEqualTo(recorded(edit).regenerate(singletonList(NpmRelease.PORTED)));
    }

    // lock/npm/add-leaf, its root holding npm to >=12 without engine-strict: npm 11 only warns, and is installed apart
    @Test
    void theInstallsOfSeveralReleasesFetchEachDocumentOnce() {
        NpmScenarios.Edit edit = ScenarioReplay.edit("add-leaf");
        NpmLockRegenerator.Inputs.InputsBuilder inputs = NpmLockRegenerator.Inputs.builder()
          .lock(edit.getStart().getText())
          .host(ScenarioReplay.host(edit.getScenario()))
          .now(ScenarioReplay.now(edit.getScenario()));
        edit.manifests().forEach((location, manifest) -> inputs.manifest(location, location.isEmpty() ?
          manifest.replaceFirst("\\{", "{\"engines\":{\"npm\":\">=12\"},") : manifest));
        ScenarioReplay.ScenarioRegistry http = new ScenarioReplay.ScenarioRegistry(edit.getScenario());
        NpmLockRegenerator regenerator = new NpmLockRegenerator(inputs.build(), ScenarioReplay.registry(http));

        List<NpmLockRegenerator.Install> installs = regenerator.installs(releases("12.2.0", "12.0.2", "11.17.0"));

        assertThat(installs).extracting(NpmLockRegeneratorTest::standsFor)
          .containsExactly(asList("12.2.0", "12.0.2"), singletonList("11.17.0"));
        assertThat(installs.get(0).getResult().isSuccess()).isTrue();
        assertThat(installs.get(1).getResult()).isEqualTo(installs.get(0).getResult());
        assertThat(http.requests).isNotEmpty().doesNotHaveDuplicates();
    }

    static Stream<Arguments> edits() {
        return ScenarioReplay.edits();
    }

    /**
     * Where several releases are candidates each install reads its own copy of the registry's documents; a lone one
     * reads the registry's own, as every scenario replay does.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void anInstallOverItsOwnCopyOfTheDocumentsComesToWhatALoneOneDoes(String name, NpmScenarios.Edit edit) {
        NpmRelease release = edit.getScenario().release();

        Result alone = recorded(edit).regenerate(singletonList(release));
        Result amongOthers = recorded(edit).regenerate(asList(release, release));

        assertThat(amongOthers).isEqualTo(alone);
    }
}

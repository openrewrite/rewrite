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

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    // to it, so npm reads the patch where the link alone would be no registry dependency to patch. npm 11.21.0
    // writes the lock back.
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
